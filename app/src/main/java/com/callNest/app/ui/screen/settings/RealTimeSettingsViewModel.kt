package com.callNest.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.util.RealTimeServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Reactive snapshot for [RealTimeSettingsScreen]. */
data class RealTimeSettingsState(
    val floatingBubble: Boolean = true,
    val postCallPopup: Boolean = true,
    val timeoutSec: Int = 8,
    val unsavedOnly: Boolean = false
)

/**
 * ViewModel exposing the four real-time toggles and routing every change
 * through [RealTimeServiceController] so the foreground service is started
 * or stopped to match.
 */
@HiltViewModel
class RealTimeSettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    private val controller: RealTimeServiceController
) : ViewModel() {

    val state: StateFlow<RealTimeSettingsState> = combine(
        settings.floatingBubbleEnabled,
        settings.postCallPopupEnabled,
        settings.postCallPopupTimeoutSeconds,
        settings.postCallPopupUnsavedOnly
    ) { b, p, t, u -> RealTimeSettingsState(b, p, t, u) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RealTimeSettingsState())

    private var pendingTimeout: Job? = null

    fun setFloatingBubble(v: Boolean) = viewModelScope.launch {
        settings.setFloatingBubbleEnabled(v)
        controller.evaluateAndApply()
    }

    fun setPostCallPopup(v: Boolean) = viewModelScope.launch {
        settings.setPostCallPopupEnabled(v)
        controller.evaluateAndApply()
    }

    fun setUnsavedOnly(v: Boolean) = viewModelScope.launch {
        settings.setPostCallPopupUnsavedOnly(v)
    }

    /** Debounced 400ms before persisting the timeout slider. */
    fun setTimeoutSec(v: Int) {
        pendingTimeout?.cancel()
        pendingTimeout = viewModelScope.launch {
            delay(400)
            settings.setPostCallPopupTimeoutSeconds(v)
        }
    }
}
