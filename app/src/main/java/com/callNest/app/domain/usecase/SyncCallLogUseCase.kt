package com.callNest.app.domain.usecase

import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.dao.NoteDao
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.data.system.CallLogReader
import com.callNest.app.data.system.ContactsReader
import com.callNest.app.data.system.PhoneNumberNormalizer
import com.callNest.app.data.system.RawCallRow
import com.callNest.app.data.system.SimSlotResolver
import com.callNest.app.data.local.mapper.toDomain
import com.callNest.app.domain.model.ContactMeta
import com.callNest.app.domain.model.SyncResult
import com.callNest.app.domain.repository.CallRepository
import com.callNest.app.domain.repository.ContactRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import timber.log.Timber

/**
 * Implements the bulk of spec §8.1.
 *
 * Steps performed in Sprint 1:
 *   1. Read `lastSyncCallId`.
 *   2. Pull all rows with `_ID > lastId`.
 *   3. Normalize, resolve display name + SIM slot, upsert.
 *   4. Recompute `ContactMetaEntity` aggregates for every touched number.
 *   5. Persist new `lastSyncCallId` + `lastSyncAt`.
 *   6. Emit on [events] for UI consumers.
 *
 * Steps deferred:
 *   - Auto-tag rule application — Sprint 6 ([applyAutoTagRules]).
 *   - Auto-save unsaved numbers — Sprint 5.
 *   - Detect renamed auto-saved contacts — Sprint 5.
 *   - Lead-score re-compute integrated — Sprint 6.
 */
class SyncCallLogUseCase @Inject constructor(
    private val settings: SettingsDataStore,
    private val callLogReader: CallLogReader,
    private val normalizer: PhoneNumberNormalizer,
    private val contactsReader: ContactsReader,
    private val simSlotResolver: SimSlotResolver,
    private val callRepository: CallRepository,
    private val contactRepository: ContactRepository,
    private val callDao: CallDao,
    private val noteDao: NoteDao,
    private val progressBus: SyncProgressBus,
    private val autoSaveContactUseCase: AutoSaveContactUseCase,
    private val detectAutoSavedRenameUseCase: DetectAutoSavedRenameUseCase,
    private val applyAutoTagRulesUseCase: ApplyAutoTagRulesUseCase,
    private val computeLeadScoreUseCase: ComputeLeadScoreUseCase
) {

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Notification stream that fires after a successful sync. */
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    /**
     * Executes one sync pass.
     *
     * @return outcome — never throws; mapping caught exceptions to
     *         user-friendly [SyncResult.Failure] reasons (spec §13).
     */
    suspend operator fun invoke(): SyncResult {
        progressBus.publish(SyncProgress.Started)
        val lastId = settings.lastSyncCallId.first()
        val region = settings.defaultRegion.first()

        val raws = try {
            callLogReader.readSince(lastId)
        } catch (_: SecurityException) {
            val msg = "Couldn't read your call log. Tap to grant permission."
            progressBus.publish(SyncProgress.Error(msg))
            return SyncResult.Failure(msg)
        } catch (t: Throwable) {
            Timber.e(t, "Sync failed reading call log")
            val msg = "Couldn't read your call log right now. Try again."
            progressBus.publish(SyncProgress.Error(msg))
            return SyncResult.Failure(msg)
        }

        if (raws.isEmpty()) {
            progressBus.publish(SyncProgress.Done(insertedCount = 0, totalCount = 0))
            return SyncResult.Success(insertedCount = 0, totalCount = 0)
        }
        progressBus.publish(SyncProgress.Progress(current = 0, total = raws.size))

        var inserted = 0
        var skipped = 0
        var firstErr: String? = null
        val touchedNumbers = HashSet<String>()
        val newCalls = ArrayList<com.callNest.app.domain.model.Call>()
        var maxId = lastId

        for (raw in raws) {
            try {
                val enriched = enrich(raw, region) ?: run { skipped++; return@run null } ?: continue
                val call = enriched.toCall()
                callRepository.upsert(call)
                newCalls += call
                touchedNumbers += enriched.normalizedNumber
                // Auto-clear follow-up: an outgoing call to a number that has an
                // active reminder is treated as the follow-up being honoured.
                if (raw.type == com.callNest.app.domain.model.CallType.OUTGOING.raw &&
                    enriched.normalizedNumber.isNotBlank()
                ) {
                    runCatching {
                        val pending = callRepository.activeFollowUpForNumber(enriched.normalizedNumber)
                        if (pending != null && raw.date >= pending.date.toEpochMilliseconds()) {
                            callRepository.markFollowUpDone(pending.systemId)
                        }
                    }
                }
                inserted++
                if (raw.systemId > maxId) maxId = raw.systemId
                progressBus.publish(
                    SyncProgress.Progress(current = inserted + skipped, total = raws.size)
                )
            } catch (t: Throwable) {
                if (firstErr == null) firstErr = t.message
                Timber.w(t, "Skipping call row systemId=${raw.systemId}")
                skipped++
            }
        }

        // Sprint 7 — attach orphan bubble notes (callSystemId IS NULL) to their call.
        for (call in newCalls) {
            if (call.normalizedNumber.isBlank()) continue
            val callMs = call.date.toEpochMilliseconds()
            runCatching {
                noteDao.attachOrphans(
                    normalizedNumber = call.normalizedNumber,
                    callSystemId = call.systemId,
                    fromMs = callMs - 60_000,
                    toMs = callMs + 60_000
                )
            }.onFailure { Timber.w(it, "attachOrphans failed for ${call.normalizedNumber}") }
        }

        // Recompute aggregates for every touched number.
        recomputeAggregates(touchedNumbers)

        // Sprint 6 — apply active auto-tag rules to the newly synced calls
        // before we recompute lead scores so any rule-emitted score boosts
        // are visible to the lead-score formula.
        runCatching { applyAutoTagRulesUseCase(newCalls) }
            .onFailure { Timber.w(it, "ApplyAutoTagRulesUseCase failed") }

        // Sprint 6 — recompute lead score for every touched number.
        for (number in touchedNumbers) {
            if (number.isBlank()) continue
            runCatching {
                val meta = contactRepository.getByNumber(number) ?: return@runCatching
                val ls = computeLeadScoreUseCase(meta)
                contactRepository.upsert(meta.copy(computedLeadScore = ls.total))
            }.onFailure { Timber.w(it, "Lead-score recompute failed for $number") }
        }

        // Sprint 5 step 8 — auto-save unsaved numbers (one Call per number is
        // enough; AutoSaveContactUseCase short-circuits subsequent attempts
        // because ContactMeta is patched on success).
        for (number in touchedNumbers) {
            if (number.isBlank()) continue
            try {
                val meta = contactRepository.getByNumber(number)
                if (meta != null && !meta.isInSystemContacts && !meta.isAutoSaved) {
                    val sample = callDao.latestForNumber(number)?.toDomain() ?: continue
                    autoSaveContactUseCase(sample)
                }
            } catch (t: Throwable) {
                Timber.w(t, "Auto-save loop: failed for $number")
            }
        }

        // Sprint 5 step 9 — detect renamed auto-saved contacts.
        runCatching { detectAutoSavedRenameUseCase() }
            .onFailure { Timber.w(it, "DetectAutoSavedRename failed") }

        // Persist watermark.
        settings.setLastSyncCallId(maxId)
        settings.setLastSyncAt(System.currentTimeMillis())
        _events.tryEmit(Unit)

        val outcome: SyncResult = when {
            inserted == 0 && skipped > 0 ->
                SyncResult.Failure(firstErr ?: "Couldn't import any calls.")
            skipped > 0 -> SyncResult.PartialSuccess(
                insertedCount = inserted,
                skippedCount = skipped,
                totalCount = raws.size,
                firstErrorMessage = firstErr
            )
            else -> SyncResult.Success(insertedCount = inserted, totalCount = raws.size)
        }
        when (outcome) {
            is SyncResult.Failure -> progressBus.publish(SyncProgress.Error(outcome.reason))
            is SyncResult.Success -> progressBus.publish(
                SyncProgress.Done(insertedCount = outcome.insertedCount, totalCount = outcome.totalCount)
            )
            is SyncResult.PartialSuccess -> progressBus.publish(
                SyncProgress.Done(insertedCount = outcome.insertedCount, totalCount = outcome.totalCount)
            )
        }
        return outcome
    }

    private suspend fun enrich(raw: RawCallRow, region: String): EnrichedRow {
        val norm = normalizer.normalize(raw.rawNumber, region)
        val normalizedNumber = norm.e164 ?: if (norm.isPrivate) "" else raw.rawNumber
        val countryIso = norm.countryIso ?: raw.countryIso
        val displayName = if (normalizedNumber.isNotBlank()) {
            contactsReader.resolveDisplayName(normalizedNumber) ?: raw.cachedName
        } else raw.cachedName
        val simSlot = simSlotResolver.resolveSimSlot(raw.phoneAccountId)
        val carrier = simSlotResolver.resolveCarrierName(raw.phoneAccountId)
        return EnrichedRow(
            raw = raw,
            normalizedNumber = normalizedNumber,
            displayName = displayName,
            countryIso = countryIso,
            simSlot = simSlot,
            carrier = carrier
        )
    }

    private suspend fun recomputeAggregates(numbers: Set<String>) {
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        for (number in numbers) {
            if (number.isBlank()) continue
            val agg = callDao.aggregatesFor(number) ?: continue
            val existing = contactRepository.getByNumber(number)
            val resolvedName = contactsReader.resolveDisplayName(number)
            val isInContacts = resolvedName != null
            val merged = ContactMeta(
                normalizedNumber = number,
                displayName = resolvedName ?: existing?.displayName,
                isInSystemContacts = isInContacts,
                systemContactId = existing?.systemContactId,
                systemRawContactId = existing?.systemRawContactId,
                isAutoSaved = existing?.isAutoSaved ?: false,
                autoSavedAt = existing?.autoSavedAt,
                autoSavedFormat = existing?.autoSavedFormat,
                firstCallDate = Instant.fromEpochMilliseconds(agg.firstCallDate),
                lastCallDate = Instant.fromEpochMilliseconds(agg.lastCallDate),
                totalCalls = agg.totalCalls,
                totalDuration = agg.totalDuration,
                incomingCount = agg.incomingCount,
                outgoingCount = agg.outgoingCount,
                missedCount = agg.missedCount,
                computedLeadScore = existing?.computedLeadScore ?: 0,
                source = existing?.source,
                updatedAt = now
            )
            contactRepository.upsert(merged)
        }
    }

    private data class EnrichedRow(
        val raw: RawCallRow,
        val normalizedNumber: String,
        val displayName: String?,
        val countryIso: String?,
        val simSlot: Int?,
        val carrier: String?
    ) {
        fun toCall(): com.callNest.app.domain.model.Call =
            com.callNest.app.domain.model.Call(
                systemId = raw.systemId,
                rawNumber = raw.rawNumber,
                normalizedNumber = normalizedNumber,
                date = Instant.fromEpochMilliseconds(raw.date),
                durationSec = raw.durationSec,
                type = com.callNest.app.domain.model.CallType.fromRaw(raw.type),
                cachedName = displayName,
                phoneAccountId = raw.phoneAccountId,
                simSlot = simSlot,
                carrierName = carrier,
                geocodedLocation = raw.geocodedLocation,
                countryIso = countryIso,
                isNew = raw.isNew,
                isBookmarked = false,
                bookmarkReason = null,
                followUpAt = null,
                followUpMinuteOfDay = null,
                followUpNote = null,
                followUpDoneAt = null,
                leadScore = 0,
                leadScoreManualOverride = null,
                isArchived = false,
                deletedAt = null,
                createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis())
            )
    }
}
