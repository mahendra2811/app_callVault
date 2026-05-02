package com.callvault.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted wrapper used solely to persist [KEY_BACKUP_PASSPHRASE].
 *
 * The passphrase is the user-entered key for the encrypted backup feature
 * (Sprint 9). We don't put it in DataStore because we never want it to land
 * in plaintext on disk.
 */
@Singleton
class SecurePrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Returns the stored passphrase, or `null` if the user has not set one. */
    fun getBackupPassphrase(): String? = prefs.getString(KEY_BACKUP_PASSPHRASE, null)

    /** Pass `null` to clear the stored passphrase. */
    fun setBackupPassphrase(value: String?) {
        prefs.edit().apply {
            if (value == null) remove(KEY_BACKUP_PASSPHRASE) else putString(KEY_BACKUP_PASSPHRASE, value)
        }.apply()
    }

    /** Persisted AppAuth `AuthState` (JSON), or `null`. */
    fun getDriveAuthStateJson(): String? = prefs.getString(KEY_DRIVE_AUTH_STATE, null)

    /** Pass `null` to clear the persisted Drive auth. */
    fun setDriveAuthStateJson(value: String?) {
        prefs.edit().apply {
            if (value == null) remove(KEY_DRIVE_AUTH_STATE) else putString(KEY_DRIVE_AUTH_STATE, value)
        }.apply()
    }

    private companion object {
        const val FILE_NAME = "callvault_secure"
        const val KEY_BACKUP_PASSPHRASE = "backupPassphrase"
        const val KEY_DRIVE_AUTH_STATE = "drive_auth_state"
    }
}
