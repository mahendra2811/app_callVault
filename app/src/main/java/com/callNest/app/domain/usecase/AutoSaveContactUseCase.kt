package com.callNest.app.domain.usecase

import com.callNest.app.data.event.UiEvent
import com.callNest.app.data.event.UiEventBus
import com.callNest.app.data.local.dao.ContactMetaDao
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.data.system.ContactGroupManager
import com.callNest.app.data.system.ContactsWriter
import com.callNest.app.domain.model.Call
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Sprint 5 — Auto-save (spec §3.11 / §8.4).
 *
 * Given a single [Call], decide whether the caller's number deserves to be
 * auto-saved as a new system contact and, if so, perform the insert.
 *
 * Skip rules (no-op):
 *  - `autoSaveEnabled` is `false`.
 *  - The number is private / short / empty.
 *  - The number is already in system contacts (`isInSystemContacts=true`)
 *    OR was already auto-saved by us (`isAutoSaved=true`).
 *
 * On success the matching `ContactMetaEntity` is patched in place:
 *  - `isAutoSaved=true`, `autoSavedAt=now`, `autoSavedFormat=<snapshot>`.
 *  - `isInSystemContacts=true`, `systemRawContactId=<new id>`.
 *  - `displayName=<built name>`.
 *
 * On failure a snackbar event is broadcast via [UiEventBus] and the entity
 * is left untouched so the next sync can retry.
 */
class AutoSaveContactUseCase @Inject constructor(
    private val settings: SettingsDataStore,
    private val groupManager: ContactGroupManager,
    private val writer: ContactsWriter,
    private val contactMetaDao: ContactMetaDao,
    private val uiEventBus: UiEventBus
) {

    /** Outcome of one auto-save attempt — exposed for tests + bulk progress. */
    sealed interface Result {
        data object SkippedDisabled : Result
        data object SkippedInvalidNumber : Result
        data object SkippedAlreadySaved : Result
        data class Saved(val rawContactId: Long, val displayName: String) : Result
        data class Failed(val reason: String) : Result
    }

    suspend operator fun invoke(call: Call): Result {
        if (!settings.autoSaveEnabled.first()) return Result.SkippedDisabled
        val number = call.normalizedNumber
        if (!isValidForAutoSave(number)) return Result.SkippedInvalidNumber

        val existing = contactMetaDao.getByNumber(number)
        if (existing != null && (existing.isInSystemContacts || existing.isAutoSaved)) {
            return Result.SkippedAlreadySaved
        }

        val prefix = settings.autoSavePrefix.first()
        val includeSimTag = settings.autoSaveIncludeSimTag.first()
        // Brand-locked suffix; user-editable suffix UI was retired. Stored value is ignored.
        val suffix = BRAND_SUFFIX
        val phoneLabel = settings.autoSavePhoneLabel.first()
        val phoneLabelCustom = settings.autoSavePhoneLabelCustom.first()
        val groupName = settings.autoSaveContactGroupName.first()

        val displayName = AutoSaveNameBuilder.build(
            prefix = prefix,
            includeSimTag = includeSimTag,
            simSlot = call.simSlot,
            suffix = suffix,
            normalizedNumber = number
        )
        val formatSnapshot = AutoSaveNameBuilder.formatSnapshot(prefix, includeSimTag, suffix)

        // Resolve group (may return -1L on perm/insert failure — we still try to
        // insert the contact without the group membership).
        var groupId = settings.autoSaveContactGroupId.first()
        if (groupId <= 0L) {
            groupId = groupManager.ensureGroup(groupName)
        }
        val account = groupManager.resolveWritableAccount()

        val result = writer.insertAutoSavedContact(
            displayName = displayName,
            normalizedNumber = number,
            phoneLabel = phoneLabel,
            phoneLabelCustom = phoneLabelCustom.takeIf { it.isNotBlank() },
            groupId = if (groupId > 0L) groupId else null,
            accountType = account?.type,
            accountName = account?.name
        )

        return when (result) {
            is ContactsWriter.InsertResult.Success -> {
                try {
                    val now = System.currentTimeMillis()
                    val patched = (existing ?: blankMeta(number, now)).copy(
                        displayName = displayName,
                        isInSystemContacts = true,
                        systemContactId = result.contactId,
                        systemRawContactId = result.rawContactId,
                        isAutoSaved = true,
                        autoSavedAt = now,
                        autoSavedFormat = formatSnapshot,
                        updatedAt = now
                    )
                    contactMetaDao.upsert(patched)
                } catch (t: Throwable) {
                    Timber.e(t, "AutoSave: succeeded in CP but failed to patch ContactMeta")
                }
                Result.Saved(result.rawContactId, displayName)
            }
            is ContactsWriter.InsertResult.Failure -> {
                uiEventBus.emit(UiEvent.Snackbar(result.reason))
                Result.Failed(result.reason)
            }
        }
    }

    private fun isValidForAutoSave(number: String): Boolean {
        if (number.isBlank()) return false
        if (number.length < 4) return false
        // Reject "Private/Unknown" placeholders.
        if (!number.startsWith("+") && number.any { !it.isDigit() }) return false
        return true
    }

    private fun blankMeta(
        number: String,
        now: Long
    ): com.callNest.app.data.local.entity.ContactMetaEntity =
        com.callNest.app.data.local.entity.ContactMetaEntity(
            normalizedNumber = number,
            displayName = null,
            isInSystemContacts = false,
            systemContactId = null,
            systemRawContactId = null,
            isAutoSaved = false,
            autoSavedAt = null,
            autoSavedFormat = null,
            firstCallDate = now,
            lastCallDate = now,
            totalCalls = 0,
            totalDuration = 0,
            incomingCount = 0,
            outgoingCount = 0,
            missedCount = 0,
            computedLeadScore = 0,
            source = null,
            updatedAt = now
        )

    companion object {
        /** Suffix appended to every auto-saved contact name. Locked to the app brand. */
        const val BRAND_SUFFIX = "callNest"
    }
}
