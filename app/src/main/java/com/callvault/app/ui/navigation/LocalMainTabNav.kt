package com.callvault.app.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

/**
 * Root NavController used for full-screen flows (Settings, CallDetail, Search, ...).
 * Provided once at the top of [CallVaultNavHost].
 */
val LocalRootNav = staticCompositionLocalOf<NavController?> { null }

/**
 * Inner tab NavController used by [MainScaffold] for Home/Calls/Inquiries/More.
 * Null outside the MainScaffold composition (e.g. from deep screens).
 */
val LocalMainTabNav = staticCompositionLocalOf<NavController?> { null }

/**
 * Pop the entire deep-screen back-stack and return to the Home tab.
 *
 * Called from [BackHandler]s on deep screens so a single back press unwinds
 * straight to Home rather than walking back through every intermediate route.
 */
fun popToHome(rootNav: NavController, innerNav: NavController?) {
    rootNav.navigate(Destinations.Main.route) {
        popUpTo(Destinations.Main.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    innerNav?.navigate(MainTabRoute.Home.route) {
        popUpTo(innerNav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
