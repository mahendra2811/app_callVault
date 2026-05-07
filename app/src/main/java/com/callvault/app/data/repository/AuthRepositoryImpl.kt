package com.callvault.app.data.repository

import com.callvault.app.data.auth.SupabaseClientProvider
import com.callvault.app.domain.model.AuthSession
import com.callvault.app.domain.model.AuthState
import com.callvault.app.domain.repository.AuthRepository
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

// Google sign-in activation steps (intentionally not in the interface):
//   1. Set GOOGLE_OAUTH_WEB_CLIENT_ID in local.properties.
//   2. In Supabase → Authentication → Providers → Google, add the same Web Client ID + secret.
//   3. Re-add `suspend fun signInWithGoogle(idToken: String, rawNonce: String?): Result<Unit>` to AuthRepository.
//   4. Implement it here using:
//        supabase.client.auth.signInWith(IDToken) {
//            idToken = idToken; provider = Google; nonce = rawNonce
//        }
//   5. Wire Credential Manager in MainActivity to obtain the idToken, pass it through.

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClientProvider,
) : AuthRepository {

    private val lastKnownSignedIn = MutableStateFlow<AuthState.SignedIn?>(null)

    override val state: Flow<AuthState> =
        supabase.client.auth.sessionStatus
            .map { status ->
                when (status) {
                    is SessionStatus.Initializing -> AuthState.Loading
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        if (user == null) {
                            Timber.w("Authenticated session with null user")
                            AuthState.SignedOut
                        } else {
                            AuthState.SignedIn(
                                AuthSession(
                                    userId = user.id,
                                    email = user.email,
                                    displayName = user.userMetadata?.get("full_name")
                                    ?.jsonPrimitive?.contentOrNull,
                                )
                            ).also { lastKnownSignedIn.value = it }
                        }
                    }
                    // Transient — keep the user signed in instead of bouncing them to auth.
                    is SessionStatus.RefreshFailure -> lastKnownSignedIn.value ?: AuthState.SignedOut
                    else -> {
                        lastKnownSignedIn.value = null
                        AuthState.SignedOut
                    }
                }
            }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String?,
    ): Result<Unit> = runAuth("signUp", expected = true) {
        supabase.client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            if (!displayName.isNullOrBlank()) {
                this.data = buildJsonObject {
                    put("full_name", JsonPrimitive(displayName))
                }
            }
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runAuth("signIn", expected = true) {
            supabase.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        runAuth("resetPassword", expected = true) {
            supabase.client.auth.resetPasswordForEmail(
                email = email,
                redirectUrl = PASSWORD_RESET_REDIRECT,
            )
        }

    override suspend fun updatePassword(newPassword: String): Result<Unit> =
        runAuth("updatePassword") {
            supabase.client.auth.updateUser { password = newPassword }
        }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> =
        runAuth("resendVerification") {
            supabase.client.auth.resendEmail(type = OtpType.Email.SIGNUP, email = email)
        }

    override suspend fun refreshSession(): Result<Unit> =
        runAuth("refreshSession") {
            supabase.client.auth.refreshCurrentSession()
        }

    override suspend fun signOut(): Result<Unit> =
        runAuth("signOut") { supabase.client.auth.signOut() }

    override suspend fun deleteAccount(password: String): Result<Unit> =
        runAuth("deleteAccount", expected = true) {
            // Postgres RPC verifies password, deletes auth.users row in one transaction. See
            // assets/db/delete_user.sql. Throws PostgrestRestException on wrong password.
            supabase.client.postgrest.rpc(
                "delete_current_user",
                buildJsonObject { put("p_password", JsonPrimitive(password)) },
            )
            supabase.client.auth.signOut()
        }

    private suspend inline fun runAuth(
        op: String,
        expected: Boolean = false,
        block: () -> Unit,
    ): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (t: Throwable) {
        if (expected) Timber.w(t, "Auth op failed: %s", op) else Timber.e(t, "Auth op failed: %s", op)
        Result.failure(t)
    }

    companion object {
        const val PASSWORD_RESET_REDIRECT = "callvault://auth/recovery"
    }
}
