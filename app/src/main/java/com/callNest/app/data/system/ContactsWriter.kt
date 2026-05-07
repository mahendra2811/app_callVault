package com.callNest.app.data.system

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.content.OperationApplicationException
import android.content.pm.PackageManager
import android.os.RemoteException
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Writes contacts via [android.content.ContentResolver.applyBatch] (spec §3.11).
 *
 * The batch always inserts:
 *  1. A `RawContacts` row (with optional ACCOUNT_TYPE/ACCOUNT_NAME).
 *  2. A `StructuredName` data row (DISPLAY_NAME).
 *  3. A `Phone` data row (NUMBER + TYPE; LABEL when `TYPE_CUSTOM`).
 *  4. A `GroupMembership` data row when [InsertParams.groupId] is non-null and > 0.
 *
 * Failures (`SecurityException`, `OperationApplicationException`,
 * `RemoteException`) are caught and surfaced via [InsertResult.Failure] with
 * a user-friendly reason — never thrown.
 */
@Singleton
class ContactsWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Result of an insert batch.
     */
    sealed interface InsertResult {
        data class Success(val rawContactId: Long, val contactId: Long?) : InsertResult
        data class Failure(val reason: String) : InsertResult
    }

    /**
     * Inserts a new auto-saved contact in a single applyBatch.
     *
     * @param displayName user-visible name (already built via AutoSaveNameBuilder).
     * @param normalizedNumber E.164 number; passed verbatim to the Phone row.
     * @param phoneLabel one of `Mobile`, `Work`, `Home`, `Other`, `Custom`.
     * @param phoneLabelCustom required (and only used) when [phoneLabel] is `Custom`.
     * @param groupId optional Groups._ID to add the contact to; null/<=0 skipped.
     * @param accountType optional account hint; null → phone-only device contact.
     * @param accountName optional account hint; must be paired with [accountType].
     */
    suspend fun insertAutoSavedContact(
        displayName: String,
        normalizedNumber: String,
        phoneLabel: String,
        phoneLabelCustom: String?,
        groupId: Long?,
        accountType: String?,
        accountName: String?
    ): InsertResult = withContext(Dispatchers.IO) {
        if (!hasWritePermission()) {
            return@withContext InsertResult.Failure(
                "Couldn't auto-save: please grant callNest permission to write contacts."
            )
        }

        val ops = ArrayList<ContentProviderOperation>(4)

        // 0 — RawContacts
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                .build()
        )

        // 1 — StructuredName
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                .build()
        )

        // 2 — Phone
        val typeInt = phoneTypeFor(phoneLabel)
        val phoneOp = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(
                ContactsContract.Data.MIMETYPE,
                CommonDataKinds.Phone.CONTENT_ITEM_TYPE
            )
            .withValue(CommonDataKinds.Phone.NUMBER, normalizedNumber)
            .withValue(CommonDataKinds.Phone.TYPE, typeInt)
        if (typeInt == CommonDataKinds.Phone.TYPE_CUSTOM) {
            phoneOp.withValue(
                CommonDataKinds.Phone.LABEL,
                phoneLabelCustom?.takeIf { it.isNotBlank() } ?: phoneLabel
            )
        }
        ops.add(phoneOp.build())

        // 3 — GroupMembership (optional)
        if (groupId != null && groupId > 0L) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
                    )
                    .withValue(CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId)
                    .build()
            )
        }

        try {
            val results = context.contentResolver.applyBatch(
                ContactsContract.AUTHORITY,
                ops
            )
            val rawUri = results.firstOrNull()?.uri
                ?: return@withContext InsertResult.Failure(
                    "Couldn't auto-save this contact. Try again."
                )
            val rawContactId = ContentUris.parseId(rawUri)
            val contactId = lookupContactIdForRawContact(rawContactId)
            InsertResult.Success(rawContactId = rawContactId, contactId = contactId)
        } catch (se: SecurityException) {
            Timber.w(se, "insertAutoSavedContact: SecurityException")
            InsertResult.Failure(
                "Couldn't auto-save: please grant callNest permission to write contacts."
            )
        } catch (oae: OperationApplicationException) {
            Timber.w(oae, "insertAutoSavedContact: OperationApplicationException")
            InsertResult.Failure("Couldn't auto-save this contact. Try again.")
        } catch (re: RemoteException) {
            Timber.w(re, "insertAutoSavedContact: RemoteException")
            InsertResult.Failure("Couldn't auto-save: contacts service is busy. Try again.")
        } catch (t: Throwable) {
            Timber.e(t, "insertAutoSavedContact: unexpected failure")
            InsertResult.Failure("Couldn't auto-save this contact. Try again.")
        }
    }

    /**
     * Updates the StructuredName.DISPLAY_NAME of the given raw contact (used
     * by ConvertToMyContactUseCase). Returns true on success.
     */
    suspend fun updateDisplayName(rawContactId: Long, newDisplayName: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!hasWritePermission()) return@withContext false
            try {
                val ops = arrayListOf(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND " +
                                "${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                rawContactId.toString(),
                                CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                            )
                        )
                        .withValue(
                            CommonDataKinds.StructuredName.DISPLAY_NAME,
                            newDisplayName
                        )
                        .build()
                )
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                true
            } catch (se: SecurityException) {
                Timber.w(se, "updateDisplayName: SecurityException"); false
            } catch (oae: OperationApplicationException) {
                Timber.w(oae, "updateDisplayName failed"); false
            } catch (re: RemoteException) {
                Timber.w(re, "updateDisplayName failed"); false
            } catch (t: Throwable) {
                Timber.e(t, "updateDisplayName: unexpected"); false
            }
        }

    private fun lookupContactIdForRawContact(rawContactId: Long): Long? {
        return context.contentResolver.query(
            ContentUris.withAppendedId(
                ContactsContract.RawContacts.CONTENT_URI,
                rawContactId
            ),
            arrayOf(ContactsContract.RawContacts.CONTACT_ID),
            null,
            null,
            null
        )?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else null
        }
    }

    private fun phoneTypeFor(label: String): Int = when (label.trim().lowercase()) {
        "mobile" -> CommonDataKinds.Phone.TYPE_MOBILE
        "work" -> CommonDataKinds.Phone.TYPE_WORK
        "home" -> CommonDataKinds.Phone.TYPE_HOME
        "other" -> CommonDataKinds.Phone.TYPE_OTHER
        else -> CommonDataKinds.Phone.TYPE_CUSTOM
    }

    private fun hasWritePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
}
