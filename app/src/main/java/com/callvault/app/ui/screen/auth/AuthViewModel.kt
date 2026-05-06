package com.callvault.app.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.analytics.AnalyticsTracker
import com.callvault.app.data.auth.SupabaseClientProvider
import com.callvault.app.domain.model.AuthState
import com.callvault.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface AuthEvent {
    data object SignedIn : AuthEvent
    data object SignedUp : AuthEvent
    data object PasswordResetSent : AuthEvent
    data object PasswordUpdated : AuthEvent
    data object SignedOut : AuthEvent
    data class Error(val message: String) : AuthEvent
}

/** Operation-based VM. Each screen owns its own form state and just calls these methods. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val supabase: SupabaseClientProvider,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    val authState: StateFlow<AuthState> = authRepository.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    fun signIn(email: String, password: String) = launchOp("sign_in") {
        authRepository.signInWithEmail(email.trim(), password).onSuccess {
            analytics.identifyOnSignIn(authState.value)
            analytics.track("auth_sign_in_success")
            _events.tryEmit(AuthEvent.SignedIn)
        }.onFailure { fail("auth_sign_in_failure", it) }
    }

    fun signUp(email: String, password: String, displayName: String?) = launchOp("sign_up") {
        authRepository.signUpWithEmail(email.trim(), password).onSuccess {
            analytics.track(
                "auth_sign_up_success",
                buildMap { displayName?.takeIf { it.isNotBlank() }?.let { put("name", it) } }
            )
            _events.tryEmit(AuthEvent.SignedUp)
        }.onFailure { fail("auth_sign_up_failure", it) }
    }

    fun sendPasswordReset(email: String) = launchOp("send_reset") {
        authRepository.sendPasswordReset(email.trim()).onSuccess {
            analytics.track("auth_password_reset_sent")
            _events.tryEmit(AuthEvent.PasswordResetSent)
        }.onFailure { fail("auth_password_reset_failure", it) }
    }

    fun updatePassword(newPassword: String) = launchOp("update_password") {
        runCatching {
            supabase.client.auth.updateUser { password = newPassword }
        }.onSuccess {
            analytics.track("auth_password_updated")
            _events.tryEmit(AuthEvent.PasswordUpdated)
        }.onFailure { fail("auth_password_update_failure", it) }
    }

    fun resendVerificationEmail(email: String) = launchOp("resend_verification") {
        runCatching {
            supabase.client.auth.resendEmail(
                type = io.github.jan.supabase.auth.OtpType.Email.SIGNUP,
                email = email.trim(),
            )
        }.onSuccess {
            analytics.track("auth_resend_verification")
            _events.tryEmit(AuthEvent.PasswordResetSent)
        }.onFailure { fail("auth_resend_verification_failure", it) }
    }

    fun signOut() = launchOp("sign_out") {
        authRepository.signOut().onSuccess {
            analytics.track("auth_sign_out")
            analytics.reset()
            _events.tryEmit(AuthEvent.SignedOut)
        }.onFailure { fail("auth_sign_out_failure", it) }
    }

    private fun launchOp(tag: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (t: Throwable) {
                Timber.e(t, "Auth op failed: %s", tag)
                fail(tag, t)
            }
        }
    }

    private fun fail(tag: String, t: Throwable) {
        analytics.track(tag)
        _events.tryEmit(AuthEvent.Error(t.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Try again."))
    }
}
