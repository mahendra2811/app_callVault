package com.callNest.app.data.secrets

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/** Encrypted-at-rest store for sensitive strings (API keys, OAuth refresh tokens). */
@Singleton
class SecretStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (t: Throwable) {
            Timber.w(t, "EncryptedSharedPreferences init failed; falling back to in-memory only")
            context.getSharedPreferences("${FILE_NAME}_fallback", Context.MODE_PRIVATE)
        }
    }

    fun get(key: String): String = prefs.getString(key, "") ?: ""
    fun set(key: String, value: String) {
        if (value.isBlank()) prefs.edit().remove(key).apply()
        else prefs.edit().putString(key, value).apply()
    }

    companion object {
        const val FILE_NAME = "callNest_secrets"
        const val K_ANTHROPIC_KEY = "anthropic_api_key"
    }
}
