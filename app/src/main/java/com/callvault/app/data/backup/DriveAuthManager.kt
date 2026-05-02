package com.callvault.app.data.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import com.callvault.app.R
import com.callvault.app.data.prefs.SecurePrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.ResponseTypeValues
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps AppAuth to perform Google OAuth for the Drive `drive.file` scope.
 *
 * The persisted [AuthState] (containing the refresh token) lives inside
 * [SecurePrefs] so it is encrypted at rest.
 */
@Singleton
class DriveAuthManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val securePrefs: SecurePrefs
) {
    private val service: AuthorizationService = AuthorizationService(appContext)
    private var pendingContinuation: ((Result<Unit>) -> Unit)? = null

    private val clientId: String
        get() = appContext.getString(R.string.cv_drive_oauth_client_id)
    private val clientSecret: String
        get() = appContext.getString(R.string.cv_drive_oauth_client_secret)
    private val redirectUri: Uri
        get() = Uri.parse("${appContext.packageName}:/oauth2redirect")

    /** True when a usable [AuthState] is persisted. */
    fun isAuthorized(): Boolean = loadState()?.isAuthorized == true

    /** Email decoded from the persisted id_token, or `null`. */
    fun getEmail(): String? = loadState()?.let { state ->
        val idToken = state.idToken ?: return null
        runCatching {
            val payload = idToken.split(".").getOrNull(1) ?: return null
            val json = String(android.util.Base64.decode(payload, android.util.Base64.URL_SAFE))
            JSONObject(json).optString("email").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /** True when [clientId] / [clientSecret] still hold the placeholder values. */
    fun isConfigured(): Boolean =
        clientId.isNotBlank() && clientId != "REPLACE_ME_CLIENT_ID" &&
            clientSecret.isNotBlank() && clientSecret != "REPLACE_ME_CLIENT_SECRET"

    /** Launch the browser-based consent flow; suspends until the redirect lands. */
    suspend fun signIn(activity: Activity): Unit = suspendCancellableCoroutine { cont ->
        if (!isConfigured()) {
            cont.resumeWithException(
                IllegalStateException("Drive OAuth is not configured. See docs/locale/06-google-cloud-setup.md.")
            )
            return@suspendCancellableCoroutine
        }
        pendingContinuation = { result ->
            result.onSuccess { cont.resume(Unit) }
                .onFailure { cont.resumeWithException(it) }
        }
        val config = AuthorizationServiceConfiguration(
            Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
            Uri.parse("https://oauth2.googleapis.com/token")
        )
        val req = AuthorizationRequest.Builder(
            config, clientId, ResponseTypeValues.CODE, redirectUri
        )
            .setScopes("openid", "email", "https://www.googleapis.com/auth/drive.file")
            .setPrompt("consent")
            .build()
        val intent = service.getAuthorizationRequestIntent(req)
        activity.startActivity(intent)
    }

    /** Pump the redirect intent through AppAuth and finish the token exchange. */
    fun handleAuthResult(intent: Intent) {
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        val cb = pendingContinuation
        if (resp == null) {
            Timber.w(ex, "Drive sign-in cancelled or failed")
            pendingContinuation = null
            cb?.invoke(Result.failure(ex ?: IllegalStateException("Sign-in cancelled.")))
            return
        }
        val state = AuthState(resp, ex)
        service.performTokenRequest(
            resp.createTokenExchangeRequest(),
            ClientSecretBasic(clientSecret)
        ) { tokenResp, tokenEx ->
            state.update(tokenResp, tokenEx)
            saveState(state)
            pendingContinuation = null
            if (tokenEx != null) {
                cb?.invoke(Result.failure(tokenEx))
            } else {
                cb?.invoke(Result.success(Unit))
            }
        }
    }

    /** Forget any persisted [AuthState]. */
    suspend fun signOut() {
        securePrefs.setDriveAuthStateJson(null)
    }

    /** Coroutine bridge to AppAuth's `performActionWithFreshTokens`. */
    suspend fun freshAccessToken(): String = suspendCancellableCoroutine { cont ->
        val state = loadState()
        if (state == null || !state.isAuthorized) {
            cont.resumeWithException(IllegalStateException("Not signed in to Google Drive."))
            return@suspendCancellableCoroutine
        }
        state.performActionWithFreshTokens(service, ClientSecretBasic(clientSecret)) { token, _, ex ->
            saveState(state)
            if (ex != null || token == null) {
                cont.resumeWithException(ex ?: IllegalStateException("Couldn't refresh Drive token."))
            } else {
                cont.resume(token)
            }
        }
    }

    private fun loadState(): AuthState? {
        val json = securePrefs.getDriveAuthStateJson() ?: return null
        return runCatching { AuthState.jsonDeserialize(json) }.getOrNull()
    }

    private fun saveState(state: AuthState) {
        securePrefs.setDriveAuthStateJson(state.jsonSerializeString())
    }
}
