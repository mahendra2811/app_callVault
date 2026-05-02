package com.callvault.app.data.service.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.callvault.app.data.prefs.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Thin wrapper around [AlarmManager] that owns the exact-alarm permission
 * decision tree.
 *
 * On API 31+ Android requires the user to grant `SCHEDULE_EXACT_ALARM` from
 * system settings. If the permission is missing we silently fall back to
 * `setAndAllowWhileIdle` (inexact, but the OS is far more permissive about
 * scheduling it) and flip [SettingsDataStore.exactAlarmFallbackActive] so the
 * UI can surface a banner with a deep link to the system settings screen.
 */
@Singleton
class ExactAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsDataStore
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Schedules a wake-up at [triggerAtMs] millis since epoch that broadcasts
     * to [FollowUpAlarmReceiver].
     *
     * @param requestCode unique per follow-up so subsequent schedule/cancel
     *  calls overwrite rather than stack.
     * @param extras attached to the broadcast intent so the receiver can
     *  rebuild the notification without an extra DB hit.
     */
    fun schedule(triggerAtMs: Long, requestCode: Int, extras: Bundle) {
        val intent = Intent(context, FollowUpAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtras(extras)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true

        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pi
                )
                ioScope.launch { settings.setExactAlarmFallbackActive(false) }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
                ioScope.launch { settings.setExactAlarmFallbackActive(true) }
                Timber.w("Exact alarms unavailable — using inexact fallback at $triggerAtMs")
            }
        } catch (se: SecurityException) {
            // OEM revoked the permission between check and call.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            ioScope.launch { settings.setExactAlarmFallbackActive(true) }
            Timber.w(se, "SecurityException scheduling exact alarm; fell back to inexact")
        }
    }

    /** Cancels a previously scheduled alarm by [requestCode]. */
    fun cancel(requestCode: Int) {
        val intent = Intent(context, FollowUpAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
        }
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, flags) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    companion object {
        const val ACTION_FIRE = "com.callvault.app.action.FOLLOW_UP_FIRE"
        const val EXTRA_CALL_SYSTEM_ID = "callSystemId"
        const val EXTRA_NORMALIZED_NUMBER = "normalizedNumber"
        const val EXTRA_DISPLAY_NAME = "displayName"
        const val EXTRA_NOTE_PREVIEW = "notePreview"
        const val EXTRA_TRIGGER_MS = "triggerMs"

        /** Stable per-call request code derivation. */
        fun requestCodeFor(callSystemId: Long): Int =
            (callSystemId xor (callSystemId ushr 32)).toInt() and 0x7FFFFFFF
    }
}
