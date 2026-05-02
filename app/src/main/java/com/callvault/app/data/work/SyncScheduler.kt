package com.callvault.app.data.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.callvault.app.data.prefs.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Translates the `syncIntervalMinutes` setting into the right WorkManager /
 * AlarmManager primitives.
 *
 * Special values:
 *  -  `0`  → user picked "Manual only" → cancel everything.
 *  - `-1`  → "Daily at 2 AM" → one OneTimeWork chained nightly.
 *  -  `5`  → 5-minute interval. WorkManager periodic minimum is 15 min, so
 *           we use AlarmManager `setExactAndAllowWhileIdle` (note: this is
 *           costly for battery — surfaced as a warning in Sprint 11 docs).
 *  - 15 / 60 / 720 / 1440 → standard PeriodicWorkRequest.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsDataStore
) {

    private val wm: WorkManager get() = WorkManager.getInstance(context)

    /** Reads current settings and re-arms work according to the rules above. */
    suspend fun schedule() {
        if (!settings.syncEnabled.first()) {
            cancelAll()
            return
        }
        val minutes = settings.syncIntervalMinutes.first()
        val constraints = buildConstraints(
            wifiOnly = settings.syncWifiOnly.first(),
            chargingOnly = settings.syncWhenChargingOnly.first()
        )

        // Always start clean — avoids stale schedules sticking around.
        cancelAll()

        when (minutes) {
            0 -> Unit // manual only
            -1 -> scheduleDailyTwoAm(constraints)
            5 -> scheduleExactRepeating(intervalMs = 5L * 60_000L)
            15, 60, 720, 1440 -> schedulePeriodic(minutes.toLong(), constraints)
            else -> schedulePeriodic(15L, constraints) // safe default
        }
    }

    /** Triggers a one-off sync immediately, respecting current constraints. */
    suspend fun triggerOnce() {
        val req = OneTimeWorkRequestBuilder<CallSyncWorker>()
            .setConstraints(
                buildConstraints(
                    wifiOnly = settings.syncWifiOnly.first(),
                    chargingOnly = settings.syncWhenChargingOnly.first()
                )
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniqueWork(
            CallSyncWorker.UNIQUE_ONESHOT_NAME,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun cancelAll() {
        wm.cancelUniqueWork(CallSyncWorker.UNIQUE_PERIODIC_NAME)
        wm.cancelUniqueWork(CallSyncWorker.UNIQUE_ONESHOT_NAME)
        wm.cancelUniqueWork(CallSyncWorker.UNIQUE_DAILY_NAME)
        cancelExactAlarm()
    }

    private fun buildConstraints(wifiOnly: Boolean, chargingOnly: Boolean): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.NOT_REQUIRED)
            .setRequiresCharging(chargingOnly)
            .build()

    private fun schedulePeriodic(minutes: Long, constraints: Constraints) {
        val req = PeriodicWorkRequestBuilder<CallSyncWorker>(minutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(
            CallSyncWorker.UNIQUE_PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    private fun scheduleDailyTwoAm(constraints: Constraints) {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_MONTH, 1)
        }
        val delayMs = next.timeInMillis - now.timeInMillis
        val req = OneTimeWorkRequestBuilder<CallSyncWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()
        wm.enqueueUniqueWork(
            CallSyncWorker.UNIQUE_DAILY_NAME,
            ExistingWorkPolicy.REPLACE,
            req
        )
        // Note: the worker re-enqueues itself for the next day in Sprint 9
        // (DailyTwoAmSyncWorker chaining); for now a single shot is enough
        // to satisfy the Sprint 1 acceptance criteria.
    }

    private fun scheduleExactRepeating(intervalMs: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + intervalMs
        val pi = exactAlarmPendingIntent()
        val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true
        if (!canSchedule) return
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    private fun cancelExactAlarm() {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(exactAlarmPendingIntent())
    }

    private fun exactAlarmPendingIntent(): PendingIntent {
        val intent = Intent(ACTION_EXACT_SYNC).setPackage(context.packageName)
        return PendingIntent.getBroadcast(
            context,
            REQ_EXACT_SYNC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val ACTION_EXACT_SYNC = "com.callvault.app.action.EXACT_SYNC"
        const val REQ_EXACT_SYNC = 1100
    }
}
