package com.callNest.app.data.work

import android.app.Notification
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
import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.dao.ContactMetaDao
import com.callNest.app.data.prefs.SettingsDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Sprint 11 — emits a once-daily 9 AM digest notification with today's call activity.
 *
 * Self-chains: each successful run schedules the next one for tomorrow at 09:00 local time.
 * Cancelled when the user disables the toggle in Settings.
 */
@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsDataStore,
    private val callDao: CallDao,
    private val contactMetaDao: ContactMetaDao
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!settings.dailySummaryEnabled.first()) {
                return Result.success()
            }
            val (from, to) = todayWindowMs()
            val total = runCatching { callDao.totalCount(from, to) }.getOrDefault(0)
            val missed = runCatching { callDao.missedCount(from, to) }.getOrDefault(0)
            val unsaved = runCatching {
                contactMetaDao.unsavedCountInRange(from, to)
            }.getOrDefault(0)
            val followUps = runCatching {
                callDao.followUpsDueCount(from, to)
            }.getOrDefault(0)
            postNotification(total, missed, unsaved, followUps)
            schedule(ctx) // chain
            Result.success()
        } catch (t: Throwable) {
            Timber.w(t, "DailySummaryWorker failed")
            Result.retry()
        }
    }

    private fun todayWindowMs(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = start + 24L * 60 * 60 * 1000 - 1
        return start to end
    }

    private fun postNotification(total: Int, missed: Int, unsaved: Int, followUps: Int) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            putExtra("route", "daily_summary")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, 1101, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = buildString {
            append("$total calls")
            if (missed > 0) append(" · $missed missed")
            if (unsaved > 0) append(" · $unsaved unsaved")
            if (followUps > 0) append(" · $followUps follow-up${if (followUps == 1) "" else "s"} due")
        }
        val n: Notification = NotificationCompat.Builder(ctx, CallNestApp.CHANNEL_DAILY_SUMMARY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Today on callNest")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n)
    }

    companion object {
        private const val UNIQUE_NAME = "daily_summary_worker"
        private const val NOTIF_ID = 9001

        /** Compute milliseconds until next 09:00 local time. */
        private fun delayUntilNext9AM(): Long {
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now(zone)
            var next = now.toLocalDate().atTime(LocalTime.of(9, 0))
            if (!next.isAfter(now)) next = next.plusDays(1)
            val nowMs = now.atZone(zone).toInstant().toEpochMilli()
            val nextMs = next.atZone(zone).toInstant().toEpochMilli()
            return (nextMs - nowMs).coerceAtLeast(60_000L)
        }

        /** Enqueue the next 9 AM run. */
        fun schedule(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<DailySummaryWorker>()
                .setInitialDelay(delayUntilNext9AM(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }

        /** Cancel any pending run when the user toggles the setting off. */
        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
