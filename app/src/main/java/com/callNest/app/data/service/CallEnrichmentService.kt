package com.callNest.app.data.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.callNest.app.CallNestApp
import com.callNest.app.MainActivity
import com.callNest.app.R
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.data.service.overlay.OverlayManager
import com.callNest.app.data.service.overlay.PostCallPayload
import com.callNest.app.data.system.CallContextResolver
import com.callNest.app.data.system.PhoneStateMonitor
import com.callNest.app.data.work.SyncScheduler
import com.callNest.app.domain.model.CallType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Foreground service that observes call-state transitions and surfaces the
 * floating bubble (during a call) and the post-call popup (after IDLE).
 *
 * Started/stopped by [com.callNest.app.util.RealTimeServiceController]
 * based on user toggles. Uses a low-priority sticky notification on the
 * `realtime_call` channel.
 */
@AndroidEntryPoint
class CallEnrichmentService : LifecycleService() {

    @Inject lateinit var phoneStateMonitor: PhoneStateMonitor
    @Inject lateinit var settings: SettingsDataStore
    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var contextResolver: CallContextResolver
    @Inject lateinit var hotLeadNotifier: HotLeadNotifier
    @Inject lateinit var syncScheduler: SyncScheduler

    private var monitorJob: Job? = null
    private var lastOffhookNumber: String? = null
    private var sawOffhook: Boolean = false

    override fun onCreate() {
        super.onCreate()
        startInForegroundCompat()
        monitorJob = lifecycleScope.launch { observeStates() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            overlayManager.hideAll()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlayManager.hideAll()
        monitorJob?.cancel()
        super.onDestroy()
    }

    private suspend fun observeStates() {
        // Combine call state with relevant toggles for reactive behaviour.
        phoneStateMonitor.observe().collect { state ->
            val bubbleOn = settings.floatingBubbleEnabled.first()
            val popupOn = settings.postCallPopupEnabled.first()
            val unsavedOnly = settings.postCallPopupUnsavedOnly.first()
            val timeout = settings.postCallPopupTimeoutSeconds.first()

            when (state) {
                is PhoneStateMonitor.CallState.Offhook -> {
                    sawOffhook = true
                    lastOffhookNumber = state.number
                    if (bubbleOn) {
                        val payload = buildPayload(state.number, durationSec = 0, callType = "Active")
                        overlayManager.showBubble(payload)
                    }
                }
                is PhoneStateMonitor.CallState.Ringing -> {
                    lastOffhookNumber = state.number ?: lastOffhookNumber
                    hotLeadNotifier.maybeAlert(state.number)
                }
                PhoneStateMonitor.CallState.Idle -> {
                    overlayManager.hideBubble()
                    // The call just ended — kick off a sync so the new
                    // CallLog row gets imported AND the auto-save loop runs
                    // immediately, instead of waiting for the next periodic
                    // tick (default 15 min).
                    if (sawOffhook) {
                        runCatching { syncScheduler.triggerOnce() }
                            .onFailure { Timber.w(it, "post-call sync trigger failed") }
                    }
                    if (sawOffhook && popupOn) {
                        val number = lastOffhookNumber
                        sawOffhook = false
                        // Debounce 2s.
                        kotlinx.coroutines.delay(2000)
                        if (number != null && number.isNotBlank()) {
                            val unsaved = runCatching { contextResolver.isUnsaved(number) }.getOrDefault(true)
                            if (!unsavedOnly || unsaved) {
                                val call = runCatching { contextResolver.latestCallForNumber(number) }.getOrNull()
                                val payload = PostCallPayload(
                                    normalizedNumber = number,
                                    displayName = runCatching { contextResolver.displayName(number) }.getOrNull(),
                                    durationSec = call?.durationSec ?: 0,
                                    callType = (call?.type ?: CallType.INCOMING).name,
                                    isUnsaved = unsaved
                                )
                                overlayManager.showPostCallPopup(payload, timeout)
                            }
                        }
                    }
                    // Auto-stop if both toggles disabled and no overlay visible.
                    if (!bubbleOn && !popupOn && !overlayManager.hasAnyOverlayVisible()) {
                        Timber.i("Real-time toggles off — stopping CallEnrichmentService")
                        stopSelf()
                    }
                }
            }
        }
    }

    private suspend fun buildPayload(number: String?, durationSec: Int, callType: String): PostCallPayload {
        val n = number.orEmpty()
        val name = if (n.isNotBlank()) runCatching { contextResolver.displayName(n) }.getOrNull() else null
        val unsaved = if (n.isNotBlank()) runCatching { contextResolver.isUnsaved(n) }.getOrDefault(true) else true
        return PostCallPayload(n, name, durationSec, callType, unsaved)
    }

    private fun startInForegroundCompat() {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, CallNestApp.CHANNEL_REALTIME_CALL)
            .setContentTitle(getString(R.string.realtime_service_title))
            .setContentText(getString(R.string.realtime_service_body))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (t: Throwable) {
            Timber.w(t, "startForeground failed")
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 4711
        const val ACTION_STOP = "com.callNest.app.action.STOP_REALTIME"

        /** Start the service. Caller must ensure overlay permission if it intends overlays. */
        fun start(ctx: Context) {
            val intent = Intent(ctx, CallEnrichmentService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(intent)
                } else {
                    ctx.startService(intent)
                }
            } catch (t: Throwable) {
                Timber.w(t, "Failed to start CallEnrichmentService")
            }
        }

        /** Stop the service via an explicit STOP intent. */
        fun stop(ctx: Context) {
            try {
                ctx.startService(
                    Intent(ctx, CallEnrichmentService::class.java).setAction(ACTION_STOP)
                )
            } catch (_: Throwable) {
                runCatching { ctx.stopService(Intent(ctx, CallEnrichmentService::class.java)) }
            }
        }
    }
}
