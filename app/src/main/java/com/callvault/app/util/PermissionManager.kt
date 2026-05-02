package com.callvault.app.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Granted/denied/permanently-denied state for a single runtime permission.
 *
 * - [Granted] — the user has granted this permission.
 * - [Denied] — the user has not granted it but a rationale dialog can still be shown.
 * - [PermanentlyDenied] — the user has selected "Don't ask again"; the only path
 *   forward is `openAppSettings`.
 * - [NotApplicable] — the permission isn't relevant on this OS version (e.g.
 *   `POST_NOTIFICATIONS` below API 33).
 */
enum class PermissionStatus { Granted, Denied, PermanentlyDenied, NotApplicable }

/**
 * Snapshot of every runtime permission CallVault cares about, plus the two
 * "special" settings-based gates (overlay + exact alarm).
 *
 * Special gates use [PermissionStatus.Granted] / [PermissionStatus.Denied]
 * only — they don't have a "permanently denied" concept because the user
 * always toggles them in Settings.
 */
data class PermissionState(
    val readCallLog: PermissionStatus = PermissionStatus.Denied,
    val readContacts: PermissionStatus = PermissionStatus.Denied,
    val writeContacts: PermissionStatus = PermissionStatus.Denied,
    val readPhoneState: PermissionStatus = PermissionStatus.Denied,
    val postNotifications: PermissionStatus = PermissionStatus.Denied,
    val scheduleExactAlarm: PermissionStatus = PermissionStatus.Denied,
    val systemAlertWindow: PermissionStatus = PermissionStatus.Denied
)

/**
 * Hilt-scoped singleton that tracks the permission state of every runtime
 * permission + special setting required by CallVault.
 *
 * Call [recheckAll] from `onResume` of any screen that just returned from
 * Settings or from a permission rationale prompt.
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    private val _state = MutableStateFlow(snapshot(appContext, activity = null))

    /** Reactive stream of the current permission state. */
    val state: StateFlow<PermissionState> = _state.asStateFlow()

    /**
     * Re-read every permission. When called with an [activity], the manager
     * can additionally distinguish "denied" from "permanently denied" via
     * [ActivityCompat.shouldShowRequestPermissionRationale].
     */
    fun recheckAll(activity: Activity? = null) {
        _state.value = snapshot(appContext, activity)
    }

    /**
     * @return `true` when the three permissions required to read & display
     * inquiry calls are all granted.
     */
    fun isCriticalGranted(): Boolean {
        val s = _state.value
        return s.readCallLog == PermissionStatus.Granted &&
            s.readContacts == PermissionStatus.Granted &&
            s.readPhoneState == PermissionStatus.Granted
    }

    /** Names of permissions required for the basic call-tracking flow. */
    fun missingCriticalPermissions(): List<String> {
        val s = _state.value
        return buildList {
            if (s.readCallLog != PermissionStatus.Granted) add(Manifest.permission.READ_CALL_LOG)
            if (s.readContacts != PermissionStatus.Granted) add(Manifest.permission.READ_CONTACTS)
            if (s.readPhoneState != PermissionStatus.Granted) add(Manifest.permission.READ_PHONE_STATE)
        }
    }

    /** Open the app's Settings page so the user can toggle permissions manually. */
    fun openAppSettings(ctx: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", ctx.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(intent)
    }

    /** Open the system overlay-permission screen (`SYSTEM_ALERT_WINDOW`). */
    fun openOverlaySettings(ctx: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${ctx.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    /** Open the system "exact alarms" screen on API 31+, or app settings otherwise. */
    fun openExactAlarmSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${ctx.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } else {
            openAppSettings(ctx)
        }
    }

    private fun snapshot(ctx: Context, activity: Activity?): PermissionState {
        return PermissionState(
            readCallLog = check(ctx, activity, Manifest.permission.READ_CALL_LOG),
            readContacts = check(ctx, activity, Manifest.permission.READ_CONTACTS),
            writeContacts = check(ctx, activity, Manifest.permission.WRITE_CONTACTS),
            readPhoneState = check(ctx, activity, Manifest.permission.READ_PHONE_STATE),
            postNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                check(ctx, activity, Manifest.permission.POST_NOTIFICATIONS)
            } else PermissionStatus.NotApplicable,
            scheduleExactAlarm = exactAlarmStatus(ctx),
            systemAlertWindow = if (Settings.canDrawOverlays(ctx)) {
                PermissionStatus.Granted
            } else PermissionStatus.Denied
        )
    }

    private fun check(ctx: Context, activity: Activity?, permission: String): PermissionStatus {
        val granted = ContextCompat.checkSelfPermission(ctx, permission) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return PermissionStatus.Granted
        if (activity != null) {
            val canShowRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            return if (canShowRationale) PermissionStatus.Denied
            else PermissionStatus.PermanentlyDenied
        }
        return PermissionStatus.Denied
    }

    private fun exactAlarmStatus(ctx: Context): PermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return PermissionStatus.NotApplicable
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        return if (am.canScheduleExactAlarms()) PermissionStatus.Granted
        else PermissionStatus.Denied
    }

    /**
     * Receives the result of a [RequestMultiplePermissions] launcher and
     * updates [state]. Forward-compatible with future permissions.
     */
    fun onRequestResult(result: Map<String, Boolean>, activity: Activity) {
        // Result granted/denied flag is folded into the next snapshot — we
        // re-read everything via [PackageManager] so partial denials and
        // OS-version mismatches are handled uniformly.
        recheckAll(activity)
    }
}

/**
 * Convenience Compose wrapper around [RequestMultiplePermissions].
 *
 * On result, refreshes [PermissionManager.state] and invokes [onResult] with
 * the raw permission → granted map.
 */
@Composable
fun rememberPermissionLauncher(
    permissionManager: PermissionManager,
    activity: Activity?,
    onResult: (Map<String, Boolean>) -> Unit = {}
): ActivityResultLauncher<Array<String>> {
    return rememberLauncherForActivityResult(RequestMultiplePermissions()) { result ->
        if (activity != null) permissionManager.onRequestResult(result, activity)
        else permissionManager.recheckAll(null)
        onResult(result)
    }
}
