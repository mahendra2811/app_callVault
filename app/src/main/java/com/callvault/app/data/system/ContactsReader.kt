package com.callvault.app.data.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read-only ContactsContract wrapper.
 *
 * - `resolveDisplayName` consults the LIVE contacts database via PhoneLookup
 *   (spec §8.1 step 4c — never trust `CallLog.CACHED_NAME`).
 * - `findRawContactId` is a stub returning `null` until Sprint 5 wires in
 *   contact writing; the API is exposed here so use cases can compile.
 */
@Singleton
class ContactsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Looks up the system display name for the given E.164 number.
     * Returns `null` if no contact matches OR if READ_CONTACTS is missing.
     */
    suspend fun resolveDisplayName(normalizedNumber: String): String? =
        withContext(Dispatchers.IO) {
            if (normalizedNumber.isBlank()) return@withContext null
            if (!hasContactsPermission()) return@withContext null
            val uri: Uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(normalizedNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }

    /**
     * Returns the system `raw_contact_id` for [normalizedNumber] (Sprint 5
     * impl). Looks up via PhoneLookup → joins back to RawContactsEntity.
     * Returns `null` when no match or permission missing.
     */
    suspend fun findRawContactId(normalizedNumber: String): Long? =
        withContext(Dispatchers.IO) {
            if (normalizedNumber.isBlank()) return@withContext null
            if (!hasContactsPermission()) return@withContext null
            val uri: Uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(normalizedNumber)
            )
            val contactId = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null } ?: return@withContext null
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId.toString()),
                null
            )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
        }

    /** True if the user has granted READ_CONTACTS at runtime. */
    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
}
