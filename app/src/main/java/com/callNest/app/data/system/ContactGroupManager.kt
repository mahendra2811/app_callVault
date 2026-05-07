package com.callNest.app.data.system

import android.Manifest
import android.accounts.AccountManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.callNest.app.data.prefs.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Wraps [ContactsContract.Groups] so the auto-save pipeline can keep all
 * inquiries inside a single, user-recognisable group (spec §3.11 — default
 * name "callNest Inquiries").
 *
 * Responsibilities:
 *  - Resolve a writable account (sync account → first writable → null/null
 *    "phone-only" fallback).
 *  - Look up an existing group by `TITLE`; create it if missing.
 *  - Persist the resolved id in [SettingsDataStore.autoSaveContactGroupId]
 *    so subsequent calls are O(1).
 *  - Rename a group in place when the user changes the configured name.
 *
 * Every ContactsContract write is wrapped in `try/catch (SecurityException)`
 * so the use-cases stay alive even if the user revokes WRITE_CONTACTS.
 */
@Singleton
class ContactGroupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsDataStore
) {

    /**
     * Returns the group id for [name], creating it if missing. Returns `-1L`
     * when permissions are missing or the provider rejects the insert.
     */
    suspend fun ensureGroup(name: String): Long = withContext(Dispatchers.IO) {
        if (!hasWritePermission()) {
            Timber.w("ensureGroup: WRITE_CONTACTS missing")
            return@withContext -1L
        }
        val cached = runCatching { settings.autoSaveContactGroupId.first() }.getOrDefault(-1L)
        if (cached > 0L && groupExists(cached)) return@withContext cached

        try {
            val existing = findGroupIdByTitle(name)
            if (existing != null) {
                settings.setAutoSaveContactGroupId(existing)
                return@withContext existing
            }
            val account = resolveWritableAccount()
            val values = ContentValues().apply {
                put(ContactsContract.Groups.TITLE, name)
                put(ContactsContract.Groups.GROUP_VISIBLE, 1)
                if (account != null) {
                    put(ContactsContract.Groups.ACCOUNT_TYPE, account.type)
                    put(ContactsContract.Groups.ACCOUNT_NAME, account.name)
                }
            }
            val uri = context.contentResolver.insert(
                ContactsContract.Groups.CONTENT_URI,
                values
            ) ?: return@withContext -1L
            val id = ContentUris.parseId(uri)
            settings.setAutoSaveContactGroupId(id)
            id
        } catch (se: SecurityException) {
            Timber.w(se, "ensureGroup: SecurityException")
            -1L
        } catch (t: Throwable) {
            Timber.e(t, "ensureGroup: unexpected failure")
            -1L
        }
    }

    /**
     * Renames the group at [id] to [newName]. No-op when permissions are
     * missing. Logs and swallows provider errors.
     */
    suspend fun renameGroup(id: Long, newName: String) = withContext(Dispatchers.IO) {
        if (id <= 0L) return@withContext
        if (!hasWritePermission()) return@withContext
        try {
            val values = ContentValues().apply {
                put(ContactsContract.Groups.TITLE, newName)
            }
            context.contentResolver.update(
                ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, id),
                values,
                null,
                null
            )
        } catch (se: SecurityException) {
            Timber.w(se, "renameGroup: SecurityException")
        } catch (t: Throwable) {
            Timber.e(t, "renameGroup: failed")
        }
    }

    /** Resolved account hint for new RawContacts; null when we should write phone-only. */
    data class Account(val type: String, val name: String)

    /**
     * Picks the first account with a Contacts sync adapter. If none is found
     * (permission denied or pure phone-only device), returns null and callers
     * write rows with `ACCOUNT_TYPE`/`ACCOUNT_NAME` left null — Android stores
     * those as device-local "Phone" contacts.
     */
    fun resolveWritableAccount(): Account? = try {
        val mgr = AccountManager.get(context)
        val accounts = mgr.accounts
        if (accounts.isEmpty()) null
        else accounts.firstOrNull {
            it.type.equals("com.google", ignoreCase = true)
        }?.let { Account(it.type, it.name) }
            ?: Account(accounts.first().type, accounts.first().name)
    } catch (se: SecurityException) {
        Timber.w(se, "resolveWritableAccount: SecurityException")
        null
    } catch (t: Throwable) {
        Timber.e(t, "resolveWritableAccount: failed")
        null
    }

    private fun findGroupIdByTitle(title: String): Long? {
        return context.contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups._ID),
            "${ContactsContract.Groups.TITLE} = ? AND ${ContactsContract.Groups.DELETED} = 0",
            arrayOf(title),
            null
        )?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else null
        }
    }

    private fun groupExists(id: Long): Boolean {
        return context.contentResolver.query(
            ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, id),
            arrayOf(ContactsContract.Groups._ID),
            "${ContactsContract.Groups.DELETED} = 0",
            null,
            null
        )?.use { it.moveToFirst() } ?: false
    }

    private fun hasWritePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
}
