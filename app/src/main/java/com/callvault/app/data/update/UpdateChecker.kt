package com.callvault.app.data.update

import android.content.Context
import com.callvault.app.BuildConfig
import com.callvault.app.data.local.dao.SkippedUpdateDao
import com.callvault.app.data.prefs.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result of a single self-update check.
 */
sealed interface UpdateCheckResult {
    /** Local build is at or ahead of the manifest. */
    data object NoUpdate : UpdateCheckResult

    /** A newer build is published on the user's selected channel. */
    data class UpdateAvailable(
        val channel: String,
        val manifest: ChannelManifest,
        val isSkipped: Boolean
    ) : UpdateCheckResult

    /** Network, parse, or other failure — [reason] is user-friendly. */
    data class Error(val reason: String) : UpdateCheckResult
}

/**
 * Fetches the JSON manifest for the user's update channel, parses it, and
 * compares against [BuildConfig.VERSION_CODE].
 *
 * Networking uses raw [HttpURLConnection] (10s timeouts) so we don't drag in
 * OkHttp solely for two GETs a week.
 */
@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val json: Json,
    private val settings: SettingsDataStore,
    private val skippedDao: SkippedUpdateDao
) {

    /**
     * Runs a check now. Updates `lastUpdateCheck` regardless of outcome so the
     * UI can show "Last checked …" honestly.
     */
    suspend fun checkNow(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val channel = runCatching { settings.updateChannel.first() }.getOrDefault("stable")
        val url = if (channel == "beta") {
            BuildConfig.UPDATE_MANIFEST_BETA_URL
        } else {
            BuildConfig.UPDATE_MANIFEST_STABLE_URL
        }
        try {
            val body = fetchBody(url)
            val manifest = json.decodeFromString(UpdateManifest.serializer(), body)
            val channelManifest = if (channel == "beta") manifest.beta ?: manifest.stable
            else manifest.stable
            settings.setLastUpdateCheck(System.currentTimeMillis())
            if (channelManifest == null) return@withContext UpdateCheckResult.NoUpdate
            if (channelManifest.versionCode <= BuildConfig.VERSION_CODE) {
                return@withContext UpdateCheckResult.NoUpdate
            }
            val isSkipped = skippedDao.isSkipped(channelManifest.versionCode)
            UpdateCheckResult.UpdateAvailable(
                channel = channel,
                manifest = channelManifest,
                isSkipped = isSkipped
            )
        } catch (t: Throwable) {
            Timber.w(t, "Update check failed")
            settings.setLastUpdateCheck(System.currentTimeMillis())
            UpdateCheckResult.Error(
                reason = "Couldn't reach update server. Tap to retry."
            )
        }
    }

    private fun fetchBody(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            useCaches = false
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw RuntimeException("HTTP $code")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
