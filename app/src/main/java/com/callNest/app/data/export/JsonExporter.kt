package com.callNest.app.data.export

import com.callNest.app.data.local.dao.AutoTagRuleDao
import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.dao.ContactMetaDao
import com.callNest.app.data.local.dao.FilterPresetDao
import com.callNest.app.data.local.dao.NoteDao
import com.callNest.app.data.local.dao.TagDao
import com.callNest.app.data.local.entity.AutoTagRuleEntity
import com.callNest.app.data.local.entity.CallEntity
import com.callNest.app.data.local.entity.CallTagCrossRef
import com.callNest.app.data.local.entity.ContactMetaEntity
import com.callNest.app.data.local.entity.FilterPresetEntity
import com.callNest.app.data.local.entity.NoteEntity
import com.callNest.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full-database JSON exporter / serializer used by both manual JSON exports
 * and the encrypted backup pipeline.
 *
 * Output is compact (no pretty-print) so backups stay small.
 */
@Singleton
class JsonExporter @Inject constructor(
    private val shared: ExportShared,
    private val callDao: CallDao,
    private val tagDao: TagDao,
    private val noteDao: NoteDao,
    private val contactMetaDao: ContactMetaDao,
    private val filterPresetDao: FilterPresetDao,
    private val autoTagRuleDao: AutoTagRuleDao
) {
    /** Public JSON config — kotlinx.serialization configured for backup safety. */
    val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /** Build the dump and write it to [destination]. */
    suspend fun export(destination: ExportDestination): ExportResult {
        val payload = buildDump()
        val bytes = json.encodeToString(payload).encodeToByteArray()
        val fileName = (destination as? ExportDestination.Downloads)?.fileName
            ?: "callNest-${stamp()}.json"
        val target = if (destination is ExportDestination.PickedUri) destination
        else ExportDestination.Downloads(fileName)
        val handle = shared.openOutputStream(target, "application/json")
        shared.writeAndCommit(handle) { it.write(bytes) }
        return ExportResult(handle.uri, fileName, bytes.size.toLong(), "json")
    }

    /** Build the full dump as raw bytes (used by the backup pipeline). */
    suspend fun encodeFullDump(): ByteArray {
        return json.encodeToString(buildDump()).encodeToByteArray()
    }

    /** Decode a dump from raw bytes. */
    fun decode(bytes: ByteArray): BackupDump =
        json.decodeFromString(BackupDump.serializer(), bytes.decodeToString())

    private suspend fun buildDump(): BackupDump {
        val calls = callDao.observeRecent(limit = Int.MAX_VALUE).first()
        val tags = tagDao.observeAll().first()
        val crossRefs = tagDao.observeAllCrossRefs().first()
        val metas = contactMetaDao.observeAll().first()
        val presets = filterPresetDao.observeAll().first()
        val rules = autoTagRuleDao.observeAll().first()

        // Notes: flat-traverse via per-call queries — N+1 but simple. The
        // Note table itself has no observeAll so we union across calls.
        val notes = mutableListOf<NoteEntity>()
        val seen = hashSetOf<Long>()
        calls.forEach { call ->
            noteDao.observeForCall(call.systemId).first().forEach { note ->
                if (seen.add(note.id)) notes += note
            }
        }
        return BackupDump(
            version = SCHEMA_VERSION,
            exportedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date()),
            calls = calls.map { it.toSerializable() },
            tags = tags.map { it.toSerializable() },
            crossRefs = crossRefs.map { it.toSerializable() },
            contactMeta = metas.map { it.toSerializable() },
            notes = notes.map { it.toSerializable() },
            filterPresets = presets.map { it.toSerializable() },
            autoTagRules = rules.map { it.toSerializable() }
        )
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())

    companion object {
        /** Bumped on any schema change to entities. */
        const val SCHEMA_VERSION = 2
    }
}

// ===== Serializable mirrors =====

/** Top-level backup payload. */
@Serializable
data class BackupDump(
    val version: Int,
    val exportedAt: String,
    val calls: List<SCall>,
    val tags: List<STag>,
    val crossRefs: List<SCrossRef>,
    val contactMeta: List<SContactMeta>,
    val notes: List<SNote>,
    val filterPresets: List<SFilterPreset>,
    val autoTagRules: List<SAutoTagRule>
)

@Serializable
data class SCall(
    val systemId: Long,
    val rawNumber: String,
    val normalizedNumber: String,
    val date: Long,
    val duration: Int,
    val type: Int,
    val cachedName: String? = null,
    val phoneAccountId: String? = null,
    val simSlot: Int? = null,
    val carrierName: String? = null,
    val geocodedLocation: String? = null,
    val countryIso: String? = null,
    val isNew: Boolean = false,
    val isBookmarked: Boolean = false,
    val bookmarkReason: String? = null,
    val followUpDate: Long? = null,
    val followUpTime: Int? = null,
    val followUpNote: String? = null,
    val followUpDoneAt: Long? = null,
    val leadScore: Int = 0,
    val leadScoreManualOverride: Int? = null,
    val isArchived: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class STag(
    val id: Long, val name: String, val colorHex: String, val emoji: String? = null,
    val isSystem: Boolean = false, val sortOrder: Int = 0, val createdAt: Long
)

@Serializable
data class SCrossRef(
    val callSystemId: Long, val tagId: Long, val appliedAt: Long, val appliedBy: String
)

@Serializable
data class SContactMeta(
    val normalizedNumber: String,
    val displayName: String? = null,
    val isInSystemContacts: Boolean,
    val systemContactId: Long? = null,
    val systemRawContactId: Long? = null,
    val isAutoSaved: Boolean = false,
    val autoSavedAt: Long? = null,
    val autoSavedFormat: String? = null,
    val firstCallDate: Long,
    val lastCallDate: Long,
    val totalCalls: Int,
    val totalDuration: Int,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int,
    val computedLeadScore: Int,
    val source: String? = null,
    val updatedAt: Long
)

@Serializable
data class SNote(
    val id: Long, val callSystemId: Long? = null, val normalizedNumber: String,
    val content: String, val createdAt: Long, val updatedAt: Long
)

@Serializable
data class SFilterPreset(
    val id: Long, val name: String, val filterJson: String, val sortOrder: Int, val createdAt: Long
)

@Serializable
data class SAutoTagRule(
    val id: Long, val name: String, val isActive: Boolean, val sortOrder: Int,
    val conditionsJson: String, val actionsJson: String, val createdAt: Long
)

// ===== Mappers =====

internal fun CallEntity.toSerializable() = SCall(
    systemId, rawNumber, normalizedNumber, date, duration, type, cachedName, phoneAccountId,
    simSlot, carrierName, geocodedLocation, countryIso, isNew, isBookmarked, bookmarkReason,
    followUpDate, followUpTime, followUpNote, followUpDoneAt, leadScore, leadScoreManualOverride,
    isArchived, deletedAt, createdAt, updatedAt
)

internal fun SCall.toEntity() = CallEntity(
    systemId, rawNumber, normalizedNumber, date, duration, type, cachedName, phoneAccountId,
    simSlot, carrierName, geocodedLocation, countryIso, isNew, isBookmarked, bookmarkReason,
    followUpDate, followUpTime, followUpNote, followUpDoneAt, leadScore, leadScoreManualOverride,
    isArchived, deletedAt, createdAt, updatedAt
)

internal fun TagEntity.toSerializable() = STag(id, name, colorHex, emoji, isSystem, sortOrder, createdAt)
internal fun STag.toEntity() = TagEntity(id, name, colorHex, emoji, isSystem, sortOrder, createdAt)

internal fun CallTagCrossRef.toSerializable() = SCrossRef(callSystemId, tagId, appliedAt, appliedBy)
internal fun SCrossRef.toEntity() = CallTagCrossRef(callSystemId, tagId, appliedAt, appliedBy)

internal fun ContactMetaEntity.toSerializable() = SContactMeta(
    normalizedNumber, displayName, isInSystemContacts, systemContactId, systemRawContactId,
    isAutoSaved, autoSavedAt, autoSavedFormat, firstCallDate, lastCallDate, totalCalls,
    totalDuration, incomingCount, outgoingCount, missedCount, computedLeadScore, source, updatedAt
)
internal fun SContactMeta.toEntity() = ContactMetaEntity(
    normalizedNumber, displayName, isInSystemContacts, systemContactId, systemRawContactId,
    isAutoSaved, autoSavedAt, autoSavedFormat, firstCallDate, lastCallDate, totalCalls,
    totalDuration, incomingCount, outgoingCount, missedCount, computedLeadScore, source, updatedAt
)

internal fun NoteEntity.toSerializable() = SNote(id, callSystemId, normalizedNumber, content, createdAt, updatedAt)
internal fun SNote.toEntity() = NoteEntity(id, callSystemId, normalizedNumber, content, createdAt, updatedAt)

internal fun FilterPresetEntity.toSerializable() = SFilterPreset(id, name, filterJson, sortOrder, createdAt)
internal fun SFilterPreset.toEntity() = FilterPresetEntity(id, name, filterJson, sortOrder, createdAt)

internal fun AutoTagRuleEntity.toSerializable() = SAutoTagRule(
    id, name, isActive, sortOrder, conditionsJson, actionsJson, createdAt
)
internal fun SAutoTagRule.toEntity() = AutoTagRuleEntity(
    id, name, isActive, sortOrder, conditionsJson, actionsJson, createdAt
)
