package com.callvault.app.data.push

import com.callvault.app.data.auth.SupabaseClientProvider
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber

/** Persists the FCM token against the signed-in Supabase user. Re-syncs on auth and token rotation. */
@Singleton
class PushTokenSync @Inject constructor(
    private val supabase: SupabaseClientProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Begins observing auth state; persists current FCM token whenever the user becomes authenticated. */
    fun start() {
        scope.launch {
            try {
                supabase.client.auth.sessionStatus
                    .map { status ->
                        (status as? SessionStatus.Authenticated)?.session?.user?.id
                    }
                    .distinctUntilChanged()
                    .collect { userId ->
                        if (userId != null) upsertCurrentToken(userId)
                    }
            } catch (t: Throwable) {
                Timber.w(t, "Auth state observation failed in PushTokenSync")
            }
        }
    }

    fun onTokenRefreshed(token: String) {
        scope.launch {
            val userId = supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            upsertRow(userId, token)
        }
    }

    private suspend fun upsertCurrentToken(userId: String) {
        val token = try {
            fetchFcmToken()
        } catch (t: Throwable) {
            Timber.w(t, "FCM token fetch failed (Firebase not initialized?)")
            return
        }
        upsertRow(userId, token)
    }

    private suspend fun fetchFcmToken(): String =
        suspendCancellableCoroutine { cont ->
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { cont.resumeWith(Result.success(it)) }
                    .addOnFailureListener { cont.resumeWith(Result.failure(it)) }
            } catch (t: IllegalStateException) {
                cont.resumeWith(Result.failure(t))
            }
        }

    private suspend fun upsertRow(userId: String, token: String) {
        try {
            supabase.client.postgrest.from(TABLE)
                .upsert(DeviceTokenRow(userId = userId, fcmToken = token))
        } catch (t: Throwable) {
            Timber.w(t, "Persist FCM token failed")
        }
    }

    @Serializable
    private data class DeviceTokenRow(
        @SerialName("user_id") val userId: String,
        @SerialName("fcm_token") val fcmToken: String,
    )

    companion object {
        private const val TABLE = "device_tokens"
    }
}
