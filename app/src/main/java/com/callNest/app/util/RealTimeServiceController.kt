package com.callNest.app.util

import android.content.Context
import android.provider.Settings
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.data.service.CallEnrichmentService
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
        // Auto-save needs the same phone-state stream to trigger an immediate
        // sync after a call ends, so the service must run even when both
        // overlay toggles are off (overlays still require canDrawOverlays).
        val autoSave = settings.autoSaveEnabled.first()
        val wantOverlay = bubble || popup
        val canOverlay = Settings.canDrawOverlays(context)
        val want = (wantOverlay && canOverlay) || autoSave
        if (want) {
            CallEnrichmentService.start(context)
        } else {
            CallEnrichmentService.stop(context)
        }
    }
}
