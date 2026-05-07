package com.callNest.app.data.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callNest.app.CallNestApp
import com.callNest.app.MainActivity
import com.callNest.app.R
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.domain.repository.ContactRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Once-a-day nudge for inquiries that haven't been touched in 30+ days.
 *
 * Fires alongside daily summary (10:30 local) — gated on
 * `followUpRemindersEnabled`. Surfaces top-3 stalest auto-saved numbers in a
 * single high-priority notification on the FOLLOW_UPS channel.
 */
@HiltWorker
class StaleLeadNudgeWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsDataStore,
    private val contacts: ContactRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!settings.followUpRemindersEnabled.first()) {
                return Result.success()
            }
            val cutoff = System.currentTimeMillis() - STALE_AGE_MS
            val stale = contacts.observeAutoSaved().first()
                .filter { it.lastCallDate.toEpochMilliseconds() < cutoff }
                .sortedBy { it.lastCallDate.toEpochMilliseconds() }
                .take(3)
            if (stale.isNotEmpty()) {
                val first = stale.first()
                postNotification(
                    count = stale.size,
                    leadingName = first.displayName ?: first.normalizedNumber,
                    leadingNumber = first.normalizedNumber
                )
            }
            schedule(ctx)
            Result.success()
        } catch (t: Throwable) {
            Timber.w(t, "StaleLeadNudgeWorker failed")
            Result.retry()
        }
    }

    private fun postNotification(count: Int, leadingName: String, leadingNumber: String) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            putExtra("route", "inquiries")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, 1201, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markPi = PendingIntent.getBroadcast(
            ctx, 1202,
            Intent(ctx, StaleLeadActionReceiver::class.java).apply {
                action = StaleLeadActionReceiver.ACTION_MARK_CONTACTED
                putExtra(StaleLeadActionReceiver.EXTRA_NUMBER, leadingNumber)
                putExtra(StaleLeadActionReceiver.EXTRA_NOTIF_ID, NOTIF_ID)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getBroadcast(
            ctx, 1203,
            Intent(ctx, StaleLeadActionReceiver::class.java).apply {
                action = StaleLeadActionReceiver.ACTION_SNOOZE
                putExtra(StaleLeadActionReceiver.EXTRA_NOTIF_ID, NOTIF_ID)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (count == 1) "1 stale lead needs a nudge"
            else "$count stale leads need a nudge"
        val body = "Last contact: $leadingName · 30+ days ago. Tap to follow up."
        val n = NotificationCompat.Builder(ctx, CallNestApp.CHANNEL_FOLLOW_UPS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .addAction(0, "Mark contacted", markPi)
            .addAction(0, "Snooze", snoozePi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n)
    }

    companion object {
        private const val UNIQUE_NAME = "stale_lead_nudge_worker"
        private const val NOTIF_ID = 9101
        private const val STALE_AGE_MS = 30L * 24 * 60 * 60 * 1000

        private fun delayUntilNext1030(): Long {
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now(zone)
            var next = now.toLocalDate().atTime(LocalTime.of(10, 30))
            if (!next.isAfter(now)) next = next.plusDays(1)
            val nowMs = now.atZone(zone).toInstant().toEpochMilli()
            val nextMs = next.atZone(zone).toInstant().toEpochMilli()
            return (nextMs - nowMs).coerceAtLeast(60_000L)
        }

        fun schedule(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<StaleLeadNudgeWorker>()
                .setInitialDelay(delayUntilNext1030(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
