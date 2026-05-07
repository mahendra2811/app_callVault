package com.callNest.app.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.analytics.AnalyticsTracker
import com.callNest.app.domain.model.AuthState
import com.callNest.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-shot events emitted to the active screen. */
sealed interface AuthEvent {
    data object SignedIn : AuthEvent
    data object SignedUp : AuthEvent
    data object PasswordResetSent : AuthEvent
    data object PasswordUpdated : AuthEvent
    data object VerificationResent : AuthEvent
    data object SignedOut : AuthEvent
    data object AccountDeleted : AuthEvent
    data class Error(val message: String) : AuthEvent
}

/** Operation-based VM. Each screen owns its own form state and just calls these methods. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val eventChannel = Channel<AuthEvent>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = eventChannel.receiveAsFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val authState: StateFlow<AuthState> = authRepository.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    fun signIn(email: String, password: String) = launchOp {
        authRepository.signInWithEmail(email.trim(), password)
            .onSuccess {
                analytics.identifyOnSignIn(authState.value)
                analytics.track("auth_sign_in_success")
                eventChannel.trySend(AuthEvent.SignedIn)
            }.onFailure { fail("auth_sign_in_failure", it) }
    }

    fun signUp(email: String, password: String, displayName: String?) = launchOp {
        authRepository.signUpWithEmail(email.trim(), password, displayName?.trim())
            .onSuccess {
                analytics.track("auth_sign_up_success")
                eventChannel.trySend(AuthEvent.SignedUp)
            }.onFailure { fail("auth_sign_up_failure", it) }
    }

    fun sendPasswordReset(email: String) = launchOp {
        authRepository.sendPasswordReset(email.trim())
            .onSuccess {
                analytics.track("auth_password_reset_sent")
                eventChannel.trySend(AuthEvent.PasswordResetSent)
            }.onFailure { fail("auth_password_reset_failure", it) }
    }

    fun updatePassword(newPassword: String) = launchOp {
        authRepository.updatePassword(newPassword)
            .onSuccess {
                analytics.track("auth_password_updated")
                eventChannel.trySend(AuthEvent.PasswordUpdated)
            }.onFailure { fail("auth_password_update_failure", it) }
    }

    fun resendVerificationEmail(email: String) = launchOp {
        authRepository.resendVerificationEmail(email.trim())
            .onSuccess {
                analytics.track("auth_resend_verification")
                eventChannel.trySend(AuthEvent.VerificationResent)
            }.onFailure { fail("auth_resend_verification_failure", it) }
    }

    fun refreshSession() = launchOp {
        authRepository.refreshSession()
            .onFailure { fail("auth_refresh_failure", it) }
    }

    fun deleteAccount(password: String) = launchOp {
        authRepository.deleteAccount(password)
            .onSuccess {
                analytics.track("auth_account_deleted")
                analytics.reset()
                eventChannel.trySend(AuthEvent.AccountDeleted)
            }.onFailure { fail("auth_account_delete_failure", it) }
    }

    fun signOut() = launchOp {
        try {
            authRepository.signOut()
                .onSuccess {
                    analytics.track("auth_sign_out")
                    eventChannel.trySend(AuthEvent.SignedOut)
                }.onFailure { fail("auth_sign_out_failure", it) }
        } finally {
            analytics.reset()
        }
    }

    private fun launchOp(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try { block() } finally { _busy.value = false }
        }
    }

    private fun fail(tag: String, t: Throwable) {
        analytics.track(tag)
        val msg = t.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Try again."
        eventChannel.trySend(AuthEvent.Error(msg))
    }
}
