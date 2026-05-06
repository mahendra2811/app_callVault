package com.callvault.app.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/** FCM entry point. Stores the device token via [PushTokenSync]; payload handling is intentionally minimal. */
@AndroidEntryPoint
class CallVaultMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushTokenSync: PushTokenSync

    override fun onNewToken(token: String) {
        Timber.d("FCM token refreshed")
        pushTokenSync.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d("FCM message: data=%s notification=%s", message.data, message.notification?.title)
        // Notification payloads are auto-rendered by FCM when the app is backgrounded.
        // Add data-payload handling here if you need silent pushes.
    }
}
