package com.callNest.app.data.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.util.RealTimeServiceController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Re-arms periodic sync after a reboot when the user has opted in to
 * `syncOnReboot` (spec §4 DataStore key, default `true`).
 *
 * Uses `goAsync()` so we can hop onto IO and read settings without ANR-ing
 * the broadcast pipeline. Coroutine scope is tied to the receiver's lifetime
 * — `pendingResult.finish()` releases it.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var settings: SettingsDataStore
    @Inject lateinit var scheduler: SyncScheduler
    @Inject lateinit var realTimeServiceController: RealTimeServiceController

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                if (settings.syncOnReboot.first()) {
                    scheduler.schedule()
                    Timber.i("Sync re-armed after boot")
                }
                if (settings.onboardingComplete.first()) {
                    withContext(Main) { realTimeServiceController.evaluateAndApply() }
                }
            } catch (t: Throwable) {
                Timber.e(t, "Boot re-arm failed")
            } finally {
                pending.finish()
            }
        }
    }
}
