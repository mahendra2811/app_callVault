package com.callNest.app.data.system

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

    /**
     * One contact-app-style search match — a single phone number tied to the
     * contact's display name + organisation (when present).
     */
    data class ContactMatch(
        val displayName: String,
        val normalizedNumber: String,
        val organisation: String?
    )

    /**
     * Live-search OS contacts by name / phone / company. Used by the in-app
     * Search surface so users can find anyone in their phonebook even before
     * they've called them. Matches against DISPLAY_NAME, PHONE_NUMBER, and
     * (when available) the COMPANY data row.
     */
    suspend fun searchContacts(query: String, limit: Int = 6): List<ContactMatch> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isEmpty() || !hasContactsPermission()) return@withContext emptyList()
            val cr = context.contentResolver
            val out = LinkedHashMap<String, ContactMatch>()
            // 1) Phone number / DISPLAY_NAME match via Phone CONTENT_FILTER_URI.
            val phoneUri = Uri.withAppendedPath(
                ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                Uri.encode(q)
            )
            runCatching {
                cr.query(
                    phoneUri,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null, null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $limit"
                )?.use { c ->
                    while (c.moveToNext() && out.size < limit) {
                        val name = c.getString(0) ?: continue
                        val num = c.getString(1) ?: continue
                        val key = "$name|$num"
                        out[key] = ContactMatch(name, num, organisation = null)
                    }
                }
            }
            out.values.toList()
        }
}
