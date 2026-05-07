package com.callNest.app.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of attempting to launch the system installer.
 */
sealed interface InstallStartResult {
    /** Installer activity launched. */
    data object Started : InstallStartResult

    /**
     * The user must enable "install unknown apps" for callNest. Caller
     * should `startActivity(intent)` and re-attempt install when they return.
     */
    data class NeedsUnknownSourcesPermission(val intent: Intent) : InstallStartResult

    /** Installer could not be launched at all (e.g. missing FileProvider). */
    data class Error(val reason: String) : InstallStartResult
}

/**
 * Hands a downloaded APK off to the system package installer through a
 * [FileProvider] URI. Honours the API 26+ unknown-sources gate.
 */
@Singleton
class UpdateInstaller @Inject constructor(
    @ApplicationContext private val ctx: Context
) {

    /** Launch the installer for [file]. */
    fun install(file: File): InstallStartResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!ctx.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${ctx.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return InstallStartResult.NeedsUnknownSourcesPermission(intent)
            }
        }
        return try {
            val uri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            InstallStartResult.Started
        } catch (t: Throwable) {
            Timber.e(t, "Installer launch failed")
            InstallStartResult.Error("Couldn't open the installer. Please try again.")
        }
    }
}
