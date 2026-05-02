package com.callvault.app.data.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Per-channel manifest entry as published at the well-known versions URL.
 *
 * @property version human-readable version (e.g. "1.4.0")
 * @property versionCode monotonically-increasing build number
 * @property apkUrl absolute URL to the signed APK
 * @property sha256 hex digest (case-insensitive) of the APK bytes
 * @property minSupported minimum [versionCode] still allowed to run; below this
 *           the user must update
 * @property releaseNotes Markdown-formatted notes shown in the update sheet
 */
@Serializable
data class ChannelManifest(
    val version: String,
    val versionCode: Int,
    val apkUrl: String,
    val sha256: String,
    val minSupported: Int = 0,
    val releaseNotes: String = ""
)

/**
 * Top-level container served by `versions-{stable,beta}.json`. Either channel
 * may be absent on a given environment so both fields are nullable.
 */
@Serializable
data class UpdateManifest(
    val stable: ChannelManifest? = null,
    val beta: ChannelManifest? = null
)

/**
 * Lenient JSON parser used by the update pipeline. Unknown keys are ignored so
 * the server can add fields without forcing a coordinated client release.
 */
val UpdateJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
