package com.callvault.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.callvault.app.data.analytics.AnalyticsTracker
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.data.push.PushTokenSync
import com.callvault.app.data.work.UpdateCheckWorker
import com.callvault.app.data.work.DailySummaryWorker
import com.callvault.app.util.RealTimeServiceController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Application entry point.
 *
 * Responsibilities:
 * - Wire up Hilt via [HiltAndroidApp].
 * - Plant a Timber debug tree in debug builds (no telemetry — spec §13).
 * - Register all notification channels the app will ever post to so the
 *   user sees them in System Settings before the first notification fires.
 * - Expose [HiltWorkerFactory] so `@HiltWorker`-annotated workers can be
 *   instantiated by WorkManager (Sprint 1 onwards).
 */
@HiltAndroidApp
class CallVaultApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var realTimeServiceController: RealTimeServiceController
    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var analytics: AnalyticsTracker
    @Inject lateinit var pushTokenSync: PushTokenSync

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        registerNotificationChannels(this)
        analytics.init(this)
        pushTokenSync.registerCurrentToken()
        // Sprint 7 — start real-time service if user has finished onboarding & enabled toggles.
        MainScope().launch {
            try {
                if (settingsDataStore.onboardingComplete.first()) {
                    realTimeServiceController.evaluateAndApply()
                    // Sprint 10/11 — background workers gated on onboarding completion.
                    runCatching { UpdateCheckWorker.schedule(this@CallVaultApp) }
                        .onFailure { Timber.w(it, "UpdateCheckWorker.schedule failed") }
                    if (settingsDataStore.dailySummaryEnabled.first()) {
                        runCatching { DailySummaryWorker.schedule(this@CallVaultApp) }
                            .onFailure { Timber.w(it, "DailySummaryWorker.schedule failed") }
                    }
                }
            } catch (t: Throwable) {
                Timber.w(t, "RealTime service evaluate failed at startup")
            }
        }
    }

    private fun registerNotificationChannels(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        val channels = listOf(
            NotificationChannel(
                CHANNEL_APP_UPDATES,
                context.getString(R.string.channel_app_updates_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_app_updates_desc)
            },
            NotificationChannel(
                CHANNEL_FOLLOW_UPS,
                context.getString(R.string.channel_follow_ups_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_follow_ups_desc)
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_SYNC,
                context.getString(R.string.channel_sync_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_sync_desc)
            },
            NotificationChannel(
                CHANNEL_REALTIME_CALL,
                context.getString(R.string.channel_realtime_call_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_realtime_call_desc)
            },
            NotificationChannel(
                CHANNEL_DAILY_SUMMARY,
                context.getString(R.string.channel_daily_summary_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_daily_summary_desc)
            }
        )
        nm.createNotificationChannels(channels)
    }

    companion object {
        const val CHANNEL_APP_UPDATES = "app_updates"
        const val CHANNEL_FOLLOW_UPS = "follow_ups"
        const val CHANNEL_SYNC = "sync"
        const val CHANNEL_REALTIME_CALL = "realtime_call"
        const val CHANNEL_DAILY_SUMMARY = "daily_summary"
    }
}
