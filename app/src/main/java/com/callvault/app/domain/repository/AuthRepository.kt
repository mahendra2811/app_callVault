package com.callvault.app.domain.repository

import com.callvault.app.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/** Authentication backend abstraction; pure Kotlin so domain stays Android-free. */
interface AuthRepository {
    val state: Flow<AuthState>

    suspend fun signUpWithEmail(email: String, password: String): Result<Unit>
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun signOut(): Result<Unit>

    // Google OAuth — scaffolded but disabled until GOOGLE_OAUTH_WEB_CLIENT_ID is configured.
    // Uncomment the body in AuthRepositoryImpl to activate.
    suspend fun signInWithGoogle(idToken: String, rawNonce: String? = null): Result<Unit>
}
