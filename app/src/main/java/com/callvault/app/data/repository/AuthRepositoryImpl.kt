package com.callvault.app.data.repository

import com.callvault.app.data.auth.SupabaseClientProvider
import com.callvault.app.domain.model.AuthSession
import com.callvault.app.domain.model.AuthState
import com.callvault.app.domain.repository.AuthRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClientProvider,
) : AuthRepository {

    override val state: Flow<AuthState> =
        supabase.client.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Initializing -> AuthState.Loading
                is SessionStatus.Authenticated -> AuthState.SignedIn(
                    AuthSession(
                        userId = status.session.user?.id.orEmpty(),
                        email = status.session.user?.email,
                        displayName = status.session.user?.userMetadata
                            ?.get("full_name")?.toString(),
                    )
                )
                else -> AuthState.SignedOut
            }
        }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> =
        runAuth("signUp") {
            supabase.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runAuth("signIn") {
            supabase.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        runAuth("resetPassword") {
            supabase.client.auth.resetPasswordForEmail(email)
        }

    override suspend fun signOut(): Result<Unit> =
        runAuth("signOut") { supabase.client.auth.signOut() }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String?): Result<Unit> =
        runAuth("signInGoogle") {
            // Activate when GOOGLE_OAUTH_WEB_CLIENT_ID is populated and Credential Manager flow is wired.
            // supabase.client.auth.signInWith(IDToken) {
            //     this.idToken = idToken
            //     this.provider = Google
            //     this.nonce = rawNonce
            // }
            error("Google sign-in is scaffolded but disabled. Set GOOGLE_OAUTH_WEB_CLIENT_ID and uncomment the body.")
        }

    private suspend inline fun runAuth(op: String, block: () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (t: Throwable) {
        Timber.e(t, "Auth op failed: %s", op)
        Result.failure(t)
    }
}
