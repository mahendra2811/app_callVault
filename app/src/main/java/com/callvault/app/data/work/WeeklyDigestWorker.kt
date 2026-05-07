package com.callvault.app.data.work

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import com.callvault.app.CallVaultApp
import com.callvault.app.MainActivity
import com.callvault.app.R
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.domain.usecase.ComputeWeeklyDigestUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Posts a "your week on CallVault" notification every Monday 09:00 local time.
 * Self-chains: each successful run schedules the next Monday.
 */
@HiltWorker
class WeeklyDigestWorker @AssistedInject constructor(
    @Assisted private val ctx: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsDataStore,
    private val computeDigest: ComputeWeeklyDigestUseCase,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!settings.weeklyDigestEnabled.first()) return Result.success()
            // Cheap re-fire guard: WorkManager periodic flex window can fire twice across days.
            val now = System.currentTimeMillis()
            val lastFired = settings.weeklyDigestLastFiredMs.first()
            if (lastFired != 0L && now - lastFired < MIN_INTERVAL_MS) {
                Timber.d("WeeklyDigestWorker: skip — last fire was %d ms ago", now - lastFired)
                return Result.success()
            }
            val digest = computeDigest()
            postNotification(digest.totalCalls, digest.hotLeads)
            settings.setWeeklyDigestLastFiredMs(now)
            Result.success()
        } catch (t: Throwable) {
            Timber.w(t, "WeeklyDigestWorker failed")
            Result.retry()
        }
    }

    private fun postNotification(totalCalls: Int, hotLeads: Int) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            putExtra("route", "weekly_digest")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            ctx, 1102, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(ctx, CallVaultApp.CHANNEL_DAILY_SUMMARY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(ctx.getString(R.string.digest_notif_title))
            .setContentText(ctx.getString(R.string.digest_notif_body_fmt, totalCalls, hotLeads))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n) }
    }

    companion object {
        private const val UNIQUE_NAME = "weekly_digest_worker"
        private const val NOTIF_ID = 9101
        private const val MIN_INTERVAL_MS = 6L * 24 * 60 * 60 * 1000

        private fun initialDelayUntilMonday9AM(): Long {
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now(zone)
            var next = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                .toLocalDate().atTime(LocalTime.of(9, 0))
            if (!next.isAfter(now)) next = next.plusWeeks(1)
            val nowMs = now.atZone(zone).toInstant().toEpochMilli()
            val nextMs = next.atZone(zone).toInstant().toEpochMilli()
            return (nextMs - nowMs).coerceAtLeast(60_000L)
        }

        fun schedule(ctx: Context) {
            val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
            val req = PeriodicWorkRequestBuilder<WeeklyDigestWorker>(
                7, TimeUnit.DAYS,
                /* flex = */ 6, TimeUnit.HOURS,
            )
                .setInitialDelay(initialDelayUntilMonday9AM(), TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
