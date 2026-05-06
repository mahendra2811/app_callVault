package com.callvault.app.domain.model

/** A signed-in user session. */
data class AuthSession(
    val userId: String,
    val email: String?,
    val displayName: String?
)

/** Authentication state surface for the UI. */
sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val session: AuthSession) : AuthState
}
