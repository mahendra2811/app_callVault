package com.callNest.app.data.system

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.provider.CallLog
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps [CallLog.Calls] reads. The output is a list of plain rows the sync
 * pipeline then enriches and persists.
 *
 * Permission check is deliberately defensive: callers may invoke this before
 * the user has granted READ_CALL_LOG, in which case we throw a typed
 * [SecurityException] so the use case can map it to a user-friendly error.
 */
@Singleton
class CallLogReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val resolver: ContentResolver = context.contentResolver

    /**
     * Reads all rows where `_ID > [lastId]`, ordered ascending so the caller
     * can persist `lastSyncCallId = max(_ID)` deterministically.
     */
    suspend fun readSince(lastId: Long): List<RawCallRow> = withContext(Dispatchers.IO) {
        ensurePermission()
        val cursor: Cursor? = resolver.query(
            CallLog.Calls.CONTENT_URI,
            PROJECTION,
            "${CallLog.Calls._ID} > ?",
            arrayOf(lastId.toString()),
            "${CallLog.Calls._ID} ASC"
        )
        cursor?.use { it.toRows() } ?: emptyList()
    }

    private fun ensurePermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            throw SecurityException("READ_CALL_LOG permission not granted")
        }
    }

    private fun Cursor.toRows(): List<RawCallRow> {
        val out = ArrayList<RawCallRow>(count)
        val idIdx = getColumnIndexOrThrow(CallLog.Calls._ID)
        val numIdx = getColumnIndexOrThrow(CallLog.Calls.NUMBER)
        val dateIdx = getColumnIndexOrThrow(CallLog.Calls.DATE)
        val durIdx = getColumnIndexOrThrow(CallLog.Calls.DURATION)
        val typeIdx = getColumnIndexOrThrow(CallLog.Calls.TYPE)
        val nameIdx = getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
        val paIdx = getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID)
        val countryIdx = getColumnIndexOrThrow(CallLog.Calls.COUNTRY_ISO)
        val geoIdx = getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION)
        val newIdx = getColumnIndexOrThrow(CallLog.Calls.NEW)

        while (moveToNext()) {
            out += RawCallRow(
                systemId = getLong(idIdx),
                rawNumber = getString(numIdx) ?: "",
                date = getLong(dateIdx),
                durationSec = getInt(durIdx),
                type = getInt(typeIdx),
                cachedName = getString(nameIdx),
                phoneAccountId = getString(paIdx),
                countryIso = getString(countryIdx),
                geocodedLocation = getString(geoIdx),
                isNew = getInt(newIdx) != 0
            )
        }
        return out
    }

    private companion object {
        val PROJECTION: Array<String> = buildList {
            add(CallLog.Calls._ID)
            add(CallLog.Calls.NUMBER)
            add(CallLog.Calls.DATE)
            add(CallLog.Calls.DURATION)
            add(CallLog.Calls.TYPE)
            add(CallLog.Calls.CACHED_NAME)
            add(CallLog.Calls.PHONE_ACCOUNT_ID)
            add(CallLog.Calls.COUNTRY_ISO)
            add(CallLog.Calls.GEOCODED_LOCATION)
            add(CallLog.Calls.NEW)
            // future API columns can be appended guarded by Build.VERSION.SDK_INT
            @Suppress("UNUSED_VARIABLE") val sdk = Build.VERSION.SDK_INT
        }.toTypedArray()
    }
}

/**
 * Raw, unenriched row pulled from the system call log.
 */
data class RawCallRow(
    val systemId: Long,
    val rawNumber: String,
    val date: Long,
    val durationSec: Int,
    val type: Int,
    val cachedName: String?,
    val phoneAccountId: String?,
    val countryIso: String?,
    val geocodedLocation: String?,
    val isNew: Boolean
)
