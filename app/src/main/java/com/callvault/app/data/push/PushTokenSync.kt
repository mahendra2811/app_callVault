package com.callvault.app.data.push

import com.callvault.app.data.auth.SupabaseClientProvider
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber

/** Fetches the FCM token and persists it against the signed-in Supabase user. */
@Singleton
class PushTokenSync @Inject constructor(
    private val supabase: SupabaseClientProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun registerCurrentToken() {
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> onTokenRefreshed(token) }
                .addOnFailureListener { Timber.w(it, "FCM token fetch failed") }
        } catch (t: IllegalStateException) {
            Timber.w("FCM not initialized — drop google-services.json into app/ to enable push.")
        }
    }

    fun onTokenRefreshed(token: String) {
        scope.launch {
            try {
                val userId = supabase.client.auth.currentUserOrNull()?.id ?: return@launch
                supabase.client.postgrest.from(TABLE)
                    .upsert(DeviceTokenRow(userId = userId, fcmToken = token))
            } catch (t: Throwable) {
                Timber.w(t, "Persist FCM token failed")
            }
        }
    }

    @Serializable
    private data class DeviceTokenRow(
        val userId: String,
        val fcmToken: String,
    )

    companion object {
        // Create this table in Supabase: id uuid pk default gen_random_uuid(),
        // user_id uuid references auth.users on delete cascade, fcm_token text unique,
        // updated_at timestamptz default now(). Add an upsert policy for authenticated users.
        private const val TABLE = "device_tokens"
    }
}
