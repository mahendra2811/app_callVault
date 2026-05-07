package com.callNest.app.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callNest.app.MainActivity
import com.callNest.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the "Update available" system notification on channel `app_updates`.
 * Tapping the notification deep-links into MainActivity with a route extra.
 */
@Singleton
class UpdateNotifier @Inject constructor(
    @ApplicationContext private val ctx: Context
) {

    /** Show the update notification for [manifest] on [channel]. */
    fun show(manifest: ChannelManifest, channel: String) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, ROUTE_UPDATE_AVAILABLE)
            putExtra(EXTRA_CHANNEL, channel)
        }
        val pending = PendingIntent.getActivity(
            ctx,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = manifest.releaseNotes.take(80).ifBlank { "A new version is ready to install." }
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Update available — v${manifest.version}")
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching {
            NotificationManagerCompat.from(ctx).notify(NOTIF_ID, notif)
        }
    }

    companion object {
        const val CHANNEL_ID = "app_updates"
        const val NOTIF_ID = 0xC4_11
        const val REQUEST_CODE = 0xC4_12
        const val EXTRA_ROUTE = "route"
        const val EXTRA_CHANNEL = "channel"
        const val ROUTE_UPDATE_AVAILABLE = "update_available"
    }
}
