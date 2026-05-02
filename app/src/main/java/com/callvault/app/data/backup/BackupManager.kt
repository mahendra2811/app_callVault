package com.callvault.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.callvault.app.data.export.ExportDestination
import com.callvault.app.data.export.ExportResult
import com.callvault.app.data.export.ExportShared
import com.callvault.app.data.export.JsonExporter
import com.callvault.app.data.export.toEntity
import com.callvault.app.data.local.CallVaultDatabase
import com.callvault.app.data.local.dao.AutoTagRuleDao
import com.callvault.app.data.local.dao.CallDao
import com.callvault.app.data.local.dao.ContactMetaDao
import com.callvault.app.data.local.dao.FilterPresetDao
import com.callvault.app.data.local.dao.NoteDao
import com.callvault.app.data.local.dao.TagDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of a [BackupManager.restore] call, surfaced to the UI. */
data class RestoreResult(
    val callsRestored: Int,
    val tagsRestored: Int,
    val notesRestored: Int,
    val schemaVersion: Int
)

/**
 * Orchestrates encrypted backup writes and verified restores.
 *
 * - **Backup**: encode the full DB → encrypt → write `.cvb`.
 * - **Restore**: read URI → decrypt → verify schema → wipe + insert in a
 *   single Room transaction.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: CallVaultDatabase,
    private val jsonExporter: JsonExporter,
    private val encryption: EncryptionHelper,
    private val shared: ExportShared,
    private val callDao: CallDao,
    private val tagDao: TagDao,
    private val noteDao: NoteDao,
    private val contactMetaDao: ContactMetaDao,
    private val filterPresetDao: FilterPresetDao,
    private val autoTagRuleDao: AutoTagRuleDao
) {
    /** Backup the database to [destination] using [passphrase]. */
    suspend fun backup(passphrase: String, destination: ExportDestination): ExportResult {
        encryption.init(passphrase)
        val plain = jsonExporter.encodeFullDump()
        val cipher = encryption.encrypt(plain)
        val fileName = (destination as? ExportDestination.Downloads)?.fileName
            ?: defaultBackupName()
        val target = if (destination is ExportDestination.PickedUri) destination
        else ExportDestination.Downloads(fileName)
        val (uri, stream) = shared.openOutputStream(target, "application/octet-stream")
        stream.use { it.write(cipher) }
        return ExportResult(uri, fileName, cipher.size.toLong(), "cvb")
    }

    /** Restore the database from [uri] using [passphrase]. */
    suspend fun restore(uri: Uri, passphrase: String): RestoreResult {
        encryption.init(passphrase)
        val cipherBytes = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes() }
            ?: error("Couldn't open the backup file.")
        val plain = try {
            encryption.decrypt(cipherBytes)
        } catch (t: Throwable) {
            throw IllegalStateException("Couldn't decrypt — wrong passphrase or corrupted file.")
        }
        val dump = jsonExporter.decode(plain)
        require(dump.version <= JsonExporter.SCHEMA_VERSION) {
            "Backup is from a newer app version (${dump.version}). Please update CallVault."
        }
        db.withTransaction {
            wipeAll()
            tagDao.run { dump.tags.forEach { insert(it.toEntity()) } }
            dump.calls.forEach { callDao.upsert(it.toEntity()) }
            dump.crossRefs.forEach { tagDao.applyTag(it.toEntity()) }
            dump.contactMeta.forEach { contactMetaDao.upsert(it.toEntity()) }
            dump.notes.forEach { noteDao.insert(it.toEntity()) }
            dump.filterPresets.forEach { filterPresetDao.insert(it.toEntity()) }
            dump.autoTagRules.forEach { autoTagRuleDao.insert(it.toEntity()) }
        }
        return RestoreResult(
            callsRestored = dump.calls.size,
            tagsRestored = dump.tags.size,
            notesRestored = dump.notes.size,
            schemaVersion = dump.version
        )
    }

    /** Truncate every user-data table — runs inside the restore transaction. */
    private fun wipeAll() {
        val tables = listOf(
            "call_tag_cross_ref", "note_history", "notes", "calls", "tags",
            "contact_meta", "filter_presets", "auto_tag_rules"
        )
        tables.forEach { t ->
            db.openHelper.writableDatabase.execSQL("DELETE FROM $t")
        }
        // Reset autoincrement counters where applicable.
        db.openHelper.writableDatabase.execSQL("DELETE FROM sqlite_sequence WHERE 1=1")
    }

    /** Filename pattern: `callvault-backup-YYYYMMDD-HHmm.cvb`. */
    fun defaultBackupName(): String =
        "callvault-backup-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.cvb"

    /** Suppress unused — kept for future raw SQL hooks. */
    @Suppress("unused") private fun rawQuery(sql: String) = SimpleSQLiteQuery(sql)
}
