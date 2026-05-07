package com.callNest.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot gate for the system splash. Flipped to `true` after the first 800ms
 * or after the first SyncProgressBus emission — whichever earlier. Once true,
 * never flips back.
 */
@Singleton
class SplashGate @Inject constructor() {
    private val _isReady = MutableStateFlow(false)

    /** Hot stream the system splash's keep-on-screen condition observes. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /** Idempotent — flips the gate on; no-op if already ready. */
    fun markReady() {
        _isReady.value = true
    }
}
