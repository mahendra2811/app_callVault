package com.callvault.app.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import timber.log.Timber

/**
 * Recognised OEM Android skins that ship aggressive task-killers on top of
 * stock Android battery management.
 */
sealed class OemVendor(val displayName: String) {
    data object Xiaomi : OemVendor("Xiaomi / Redmi / POCO")
    data object Oppo : OemVendor("OPPO")
    data object Vivo : OemVendor("vivo")
    data object Realme : OemVendor("realme")
    data object Samsung : OemVendor("Samsung")
    data object OnePlus : OemVendor("OnePlus")
    data object Honor : OemVendor("Honor")
    data object Huawei : OemVendor("Huawei")
    data object Other : OemVendor("Your device")
}

/**
 * Detects the current device's OEM and opens its autostart / background
 * activity / battery-saver page. Falls back to the system "ignore battery
 * optimizations" screen when the OEM screen is unavailable.
 *
 * The component names below come from the spec §3.22 catalogue and have
 * been the canonical entry points across MIUI 10–14, ColorOS 11–14, FuntouchOS
 * 11–14, OneUI 4–6, OxygenOS 11–14 and Magic OS 6–8.
 */
object OemBatteryGuide {

    /** Determine the running device's vendor from [Build.MANUFACTURER]. */
    fun detect(): OemVendor {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return when {
            "xiaomi" in m || "redmi" in b || "poco" in b -> OemVendor.Xiaomi
            "oppo" in m -> OemVendor.Oppo
            "vivo" in m -> OemVendor.Vivo
            "realme" in m || "realme" in b -> OemVendor.Realme
            "samsung" in m -> OemVendor.Samsung
            "oneplus" in m -> OemVendor.OnePlus
            "honor" in m -> OemVendor.Honor
            "huawei" in m -> OemVendor.Huawei
            else -> OemVendor.Other
        }
    }

    /**
     * Try, in order, every [Intent] candidate registered for [vendor]. On
     * the first one that resolves, launch it and return. Falls through to
     * `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` and finally to the
     * generic battery-saver screen.
     */
    fun openAutostartSettings(ctx: Context, vendor: OemVendor = detect()) {
        val candidates = candidatesFor(vendor)
        for (intent in candidates) {
            if (tryLaunch(ctx, intent)) return
        }
        // Fallback 1 — Android-stock "ignore battery optimizations" prompt.
        val ignoreOpt = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${ctx.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (tryLaunch(ctx, ignoreOpt)) return
        // Fallback 2 — generic battery saver list.
        tryLaunch(ctx, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Step-by-step instruction strings shown above the "Open settings" button. */
    fun stepsFor(vendor: OemVendor): List<String> = when (vendor) {
        OemVendor.Xiaomi -> listOf(
            "Tap Open settings to jump straight to MIUI's Autostart list.",
            "Find CallVault and turn the Autostart switch on.",
            "Go back, then open Battery saver and set CallVault to No restrictions.",
            "Optional: lock CallVault in Recents so MIUI never wipes it from memory."
        )
        OemVendor.Oppo -> listOf(
            "Tap Open settings to land on ColorOS' App startup manager.",
            "Find CallVault and enable Allow auto-launch and Allow background activity.",
            "Open Battery > High background power consumption and add CallVault.",
            "Lock CallVault in Recents to keep ColorOS from killing it."
        )
        OemVendor.Vivo -> listOf(
            "Tap Open settings to open vivo's High background power consumption list.",
            "Find CallVault and enable Allow background activity.",
            "Go back to Battery > Background power consumption management and allow CallVault.",
            "Open i Manager > Autostart and turn CallVault on."
        )
        OemVendor.Realme -> listOf(
            "Tap Open settings to open realme's App startup manager.",
            "Switch on Auto-launch and Allow background activity for CallVault.",
            "Open Battery > Power saving and set CallVault to Allow background running.",
            "Lock CallVault in Recents so realme's task killer leaves it alone."
        )
        OemVendor.Samsung -> listOf(
            "Tap Open settings to land on Samsung's Battery > Background usage limits.",
            "Make sure CallVault is NOT in Sleeping apps or Deep sleeping apps.",
            "Open Apps > CallVault > Battery and pick Unrestricted.",
            "Optional: turn off Adaptive battery if reminders still arrive late."
        )
        OemVendor.OnePlus -> listOf(
            "Tap Open settings to open OxygenOS' Battery optimization screen.",
            "Pick CallVault, then choose Don't optimise.",
            "Open Battery > More battery settings and turn off Sleep standby optimisation.",
            "Lock CallVault in Recents so OxygenOS keeps it warm."
        )
        OemVendor.Honor -> listOf(
            "Tap Open settings to open Honor's Startup manager.",
            "Switch CallVault to Manage manually and enable all three toggles.",
            "Go back to Battery > More battery settings and turn off Power-intensive prompt.",
            "Lock CallVault in Recents to survive Magic OS task cleanup."
        )
        OemVendor.Huawei -> listOf(
            "Tap Open settings to open Huawei's App launch screen.",
            "Switch CallVault to Manage manually, then enable Auto-launch, Secondary launch, and Run in background.",
            "Open Battery > App launch settings and confirm CallVault is allowed.",
            "Lock CallVault in Recents so EMUI does not freeze it."
        )
        OemVendor.Other -> listOf(
            "Tap Open settings to open the system battery optimization screen.",
            "Pick CallVault and choose Don't optimise so background sync keeps running.",
            "If your device has an Autostart or App launch screen, allow CallVault there too.",
            "Lock CallVault in Recents to keep it out of the system's task killer."
        )
    }

    private fun candidatesFor(vendor: OemVendor): List<Intent> {
        val newTask = Intent.FLAG_ACTIVITY_NEW_TASK
        return when (vendor) {
            OemVendor.Xiaomi -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                ).addFlags(newTask),
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.powercenter.PowerSettings"
                    )
                ).addFlags(newTask)
            )
            OemVendor.Oppo -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                ).addFlags(newTask),
                Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.startupapp.StartupAppListActivity"
                    )
                ).addFlags(newTask),
                Intent().setComponent(
                    ComponentName(
                        "com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity"
                    )
                ).addFlags(newTask)
            )
            OemVendor.Vivo -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                    )
                ).addFlags(newTask),
                Intent().setComponent(
                    ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                ).addFlags(newTask)
            )
            OemVendor.Realme -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                ).addFlags(newTask),
                Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.privacypermissionsentry.PermissionTopActivity"
                    )
                ).addFlags(newTask)
            )
            OemVendor.Samsung -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                ).addFlags(newTask),
                Intent().setComponent(
                    ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.BatteryActivity"
                    )
                ).addFlags(newTask)
            )
            OemVendor.OnePlus -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    )
                ).addFlags(newTask)
            )
            OemVendor.Honor, OemVendor.Huawei -> listOf(
                Intent().setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                ).addFlags(newTask),
                Intent().setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                ).addFlags(newTask)
            )
            OemVendor.Other -> emptyList()
        }
    }

    private fun tryLaunch(ctx: Context, intent: Intent): Boolean {
        return try {
            ctx.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (t: Throwable) {
            Timber.w(t, "Failed to launch OEM intent $intent")
            false
        }
    }
}
