package com.callvault.app.domain.repository

import com.callvault.app.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/** Authentication backend abstraction; pure Kotlin so domain stays Android-free. */
interface AuthRepository {
    val state: Flow<AuthState>

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String? = null,
    ): Result<Unit>

    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun resendVerificationEmail(email: String): Result<Unit>
    suspend fun refreshSession(): Result<Unit>
    suspend fun signOut(): Result<Unit>

    /**
     * Deletes the calling user's account. The server-side RPC verifies [password] before deletion,
     * so callers don't need a separate verify step (and we avoid rotating the session).
     */
    suspend fun deleteAccount(password: String): Result<Unit>

    // Google OAuth: intentionally absent until activated. See AuthRepositoryImpl
    // for the activation comment block — adding the method back will require
    // GOOGLE_OAUTH_WEB_CLIENT_ID + Credential Manager wiring in MainActivity.
}
