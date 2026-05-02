package com.callvault.app.util

import android.content.Context
import android.provider.Settings
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.data.service.CallEnrichmentService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Coordinator that starts/stops [CallEnrichmentService] based on user
 * toggles in [SettingsDataStore] and overlay-permission state.
 *
 * Call [evaluateAndApply] from app startup and after each toggle change.
 */
@Singleton
class RealTimeServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsDataStore
) {

    /**
     * Reads the current toggles and starts or stops the service.
     * Safe to call repeatedly. Must run on the main thread.
     */
    suspend fun evaluateAndApply() {
        val bubble = settings.floatingBubbleEnabled.first()
        val popup = settings.postCallPopupEnabled.first()
        val want = bubble || popup
        val canOverlay = Settings.canDrawOverlays(context)
        if (want && canOverlay) {
            CallEnrichmentService.start(context)
        } else {
            CallEnrichmentService.stop(context)
        }
    }
}
