package com.callvault.app.data.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-scoped flag: did the user pass biometric/credential auth this session? */
@Singleton
class AppLockState @Inject constructor() {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun markUnlocked() { _unlocked.value = true }
    fun lock() { _unlocked.value = false }
}
