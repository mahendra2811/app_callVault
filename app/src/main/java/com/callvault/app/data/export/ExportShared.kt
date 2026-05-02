package com.callvault.app.data.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.callvault.app.data.local.dao.CallDao
import com.callvault.app.data.local.dao.ContactMetaDao
import com.callvault.app.data.local.dao.NoteDao
import com.callvault.app.data.local.dao.TagDao
import com.callvault.app.data.local.mapper.toDomain
import com.callvault.app.domain.model.Call
import com.callvault.app.domain.model.ContactMeta
import com.callvault.app.domain.model.DateRange
import com.callvault.app.domain.model.Note
import com.callvault.app.domain.model.Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Filter applied to an export job. `null` fields disable that dimension.
 *
 * @property range optional inclusive epoch-ms window
 * @property callTypes set of `CallLog.Calls.TYPE_*` values
 * @property tagsAnyOf calls matching any of these tag ids
 * @property bookmarkedOnly limit to bookmarked rows
 * @property includeArchived include soft-archived rows
 */
data class ExportFilter(
    val range: DateRange? = null,
    val callTypes: Set<Int>? = null,
    val tagsAnyOf: Set<Long>? = null,
    val bookmarkedOnly: Boolean = false,
    val includeArchived: Boolean = false
)

/**
 * Boolean flags controlling which CSV/Excel columns to emit.
 */
data class ExportColumns(
    val date: Boolean = true,
    val number: Boolean = true,
    val name: Boolean = true,
    val type: Boolean = true,
    val duration: Boolean = true,
    val simSlot: Boolean = true,
    val tags: Boolean = true,
    val notes: Boolean = true,
    val leadScore: Boolean = true,
    val geocodedLocation: Boolean = true,
    val isBookmarked: Boolean = true,
    val isArchived: Boolean = false
)

/** Resolved output of an export job. */
data class ExportResult(
    val uri: Uri,
    val fileName: String,
    val sizeBytes: Long,
    val format: String
)

/** A single call row plus all related meta needed by every exporter. */
data class CallWithRelations(
    val call: Call,
    val contactMeta: ContactMeta?,
    val tags: List<Tag>,
    val notes: List<Note>
)

/** Where the exporter should write the resulting blob. */
sealed class ExportDestination {
    /** Stream into `Downloads/<filename>` via MediaStore. */
    data class Downloads(val fileName: String) : ExportDestination()

    /** Stream into a user-picked URI from SAF. */
    data class PickedUri(val uri: Uri) : ExportDestination()
}

/**
 * Helper that resolves common export plumbing: relation queries against the
 * DAOs, MIME-aware MediaStore inserts, and SAF-uri OutputStreams.
 */
@Singleton
class ExportShared @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callDao: CallDao,
    private val tagDao: TagDao,
    private val noteDao: NoteDao,
    private val contactMetaDao: ContactMetaDao
) {

    /** Run the [filter] and join in tags / notes / contact meta per call. */
    suspend fun queryCalls(filter: ExportFilter): List<CallWithRelations> {
        val from = filter.range?.from ?: 0L
        val to = filter.range?.to ?: Long.MAX_VALUE
        val typeSet = filter.callTypes
        // Pull the broad date window then do in-memory filtering for everything
        // else — keeps the query simple and avoids RawQuery for v1.
        val rows = callDao.observeBetween(from, to).first()
        val filtered = rows
            .asSequence()
            .filter { typeSet == null || it.type in typeSet }
            .filter { !filter.bookmarkedOnly || it.isBookmarked }
            .filter { filter.includeArchived || !it.isArchived }
            .toList()

        return filtered.map { entity ->
            val call = entity.toDomain()
            val tags = tagDao.tagIdsForCall(call.systemId).let { ids ->
                ids.mapNotNull { tagDao.getById(it)?.toDomain() }
            }
            if (filter.tagsAnyOf != null && tags.none { it.id in filter.tagsAnyOf }) {
                return@map null
            }
            val notes = noteDao.observeForCall(call.systemId).first().map { it.toDomain() }
            val meta = contactMetaDao.getByNumber(call.normalizedNumber)?.toDomain()
            CallWithRelations(call, meta, tags, notes)
        }.filterNotNull()
    }

    /**
     * Open an OutputStream for [destination] with the given MIME type.
     * Returns the resolved [Uri] alongside the stream — caller closes both.
     */
    fun openOutputStream(
        destination: ExportDestination,
        mimeType: String
    ): Pair<Uri, OutputStream> {
        return when (destination) {
            is ExportDestination.PickedUri -> {
                val os = context.contentResolver.openOutputStream(destination.uri, "w")
                    ?: error("Couldn't open the picked location for writing.")
                destination.uri to os
            }
            is ExportDestination.Downloads -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val cv = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, destination.fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv
                    ) ?: error("Couldn't create the file in Downloads.")
                    uri to (context.contentResolver.openOutputStream(uri, "w")
                        ?: error("Couldn't open Downloads stream."))
                } else {
                    val downloads = Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloads.exists()) downloads.mkdirs()
                    val file = File(downloads, destination.fileName)
                    Uri.fromFile(file) to FileOutputStream(file)
                }
            }
        }
    }

    /** Probe the byte size for the resulting URI (best-effort). */
    fun sizeOf(uri: Uri): Long = try {
        context.contentResolver.openFileDescriptor(uri, "r")
            ?.use { it.statSize } ?: 0L
    } catch (_: Throwable) {
        0L
    }
}
