package com.callNest.app.data.service.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callNest.app.CallNestApp
import com.callNest.app.R
import com.callNest.app.domain.repository.CallRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Receives [ExactAlarmScheduler] broadcasts and posts the user-visible
 * follow-up notification.
 *
 * The notification offers four actions:
 *  - Call back (opens the dialer)
 *  - Snooze 1 hour
 *  - Snooze 1 day
 *  - Mark done
 *
 * Snooze re-schedules a new alarm and dismisses the current notification;
 * "Mark done" writes `followUpDoneAt` via [CallRepository] and dismisses.
 */
@AndroidEntryPoint
class FollowUpAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ExactAlarmScheduler
    @Inject lateinit var callRepo: CallRepository

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ExactAlarmScheduler.ACTION_FIRE -> postNotification(context, intent)
            ACTION_SNOOZE_1H -> handleSnooze(context, intent, ONE_HOUR_MS)
            ACTION_SNOOZE_1D -> handleSnooze(context, intent, ONE_DAY_MS)
            ACTION_MARK_DONE -> handleMarkDone(context, intent)
            else -> Timber.w("FollowUpAlarmReceiver: unknown action ${intent.action}")
        }
    }

    private fun postNotification(context: Context, intent: Intent) {
        val callId = intent.getLongExtra(ExactAlarmScheduler.EXTRA_CALL_SYSTEM_ID, -1L)
        val number = intent.getStringExtra(ExactAlarmScheduler.EXTRA_NORMALIZED_NUMBER) ?: return
        val name = intent.getStringExtra(ExactAlarmScheduler.EXTRA_DISPLAY_NAME)
        val notePreview = intent.getStringExtra(ExactAlarmScheduler.EXTRA_NOTE_PREVIEW)

        val title = context.getString(
            R.string.followup_notif_title,
            name?.takeIf { it.isNotBlank() } ?: number
        )
        val body = notePreview
            ?.takeIf { it.isNotBlank() }
            ?.let { if (it.length > 80) it.take(80) + "…" else it }
            ?: context.getString(R.string.followup_notif_default_body)

        val callBackPi = PendingIntent.getActivity(
            context,
            ExactAlarmScheduler.requestCodeFor(callId),
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze1h = pendingSelfAction(context, ACTION_SNOOZE_1H, callId, intent.extras)
        val snooze1d = pendingSelfAction(context, ACTION_SNOOZE_1D, callId, intent.extras)
        val markDone = pendingSelfAction(context, ACTION_MARK_DONE, callId, intent.extras)

        val notification: Notification = NotificationCompat.Builder(
            context,
            CallNestApp.CHANNEL_FOLLOW_UPS
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(callBackPi)
            .addAction(0, context.getString(R.string.followup_action_call_back), callBackPi)
            .addAction(0, context.getString(R.string.followup_action_snooze_1h), snooze1h)
            .addAction(0, context.getString(R.string.followup_action_snooze_1d), snooze1d)
            .addAction(0, context.getString(R.string.followup_action_mark_done), markDone)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context)
                .notify(notifIdFor(callId), notification)
        } else {
            Timber.w("Follow-up reminder skipped — notifications disabled")
        }
    }

    private fun handleSnooze(context: Context, intent: Intent, deltaMs: Long) {
        val callId = intent.getLongExtra(ExactAlarmScheduler.EXTRA_CALL_SYSTEM_ID, -1L)
        if (callId < 0) return
        dismiss(context, callId)
        val newTrigger = System.currentTimeMillis() + deltaMs
        val extras = (intent.extras ?: android.os.Bundle()).apply {
            putLong(ExactAlarmScheduler.EXTRA_TRIGGER_MS, newTrigger)
        }
        scheduler.schedule(
            triggerAtMs = newTrigger,
            requestCode = ExactAlarmScheduler.requestCodeFor(callId),
            extras = extras
        )
        ioScope.launch {
            runCatching {
                callRepo.setFollowUp(
                    callId = callId,
                    triggerMs = newTrigger,
                    minuteOfDay = null,
                    note = intent.getStringExtra(ExactAlarmScheduler.EXTRA_NOTE_PREVIEW)
                )
            }.onFailure { Timber.w(it, "Snooze persist failed") }
        }
    }

    private fun handleMarkDone(context: Context, intent: Intent) {
        val callId = intent.getLongExtra(ExactAlarmScheduler.EXTRA_CALL_SYSTEM_ID, -1L)
        if (callId < 0) return
        dismiss(context, callId)
        ioScope.launch {
            runCatching { callRepo.markFollowUpDone(callId) }
                .onFailure { Timber.w(it, "Mark-done persist failed") }
        }
    }

    private fun pendingSelfAction(
        context: Context,
        action: String,
        callId: Long,
        extras: android.os.Bundle?
    ): PendingIntent {
        val intent = Intent(context, FollowUpAlarmReceiver::class.java).apply {
            this.action = action
            if (extras != null) putExtras(extras)
            putExtra(ExactAlarmScheduler.EXTRA_CALL_SYSTEM_ID, callId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(
            context,
            (ExactAlarmScheduler.requestCodeFor(callId) xor action.hashCode()) and 0x7FFFFFFF,
            intent,
            flags
        )
    }

    private fun dismiss(context: Context, callId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notifIdFor(callId))
    }

    private fun notifIdFor(callId: Long): Int =
        ExactAlarmScheduler.requestCodeFor(callId)

    companion object {
        const val ACTION_SNOOZE_1H = "com.callNest.app.action.FOLLOW_UP_SNOOZE_1H"
        const val ACTION_SNOOZE_1D = "com.callNest.app.action.FOLLOW_UP_SNOOZE_1D"
        const val ACTION_MARK_DONE = "com.callNest.app.action.FOLLOW_UP_MARK_DONE"
        private const val ONE_HOUR_MS = 60L * 60 * 1000
        private const val ONE_DAY_MS = 24L * 60 * 60 * 1000
    }
}
