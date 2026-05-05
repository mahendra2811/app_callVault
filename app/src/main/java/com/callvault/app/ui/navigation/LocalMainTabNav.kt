package com.callvault.app.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow

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
 * Side-channel set by [popToHome] when called from a deep screen where the
 * inner-tab NavController isn't reachable. [MainScaffold] observes this flag
 * on composition and switches its inner nav to Home, then clears it.
 */
object HomeNavRequest {
    val pending = MutableStateFlow(false)
}

/**
 * Pop the entire deep-screen back-stack and return to the Home tab.
 *
 * Called from [BackHandler]s on deep screens so a single back press unwinds
 * straight to Home rather than walking back through every intermediate route.
 *
 * If [innerNav] is non-null, it's switched directly. If null (the common case
 * when called from a deep screen outside MainScaffold), [HomeNavRequest] flags
 * MainScaffold to switch on its next composition.
 */
fun popToHome(rootNav: NavController, innerNav: NavController?) {
    rootNav.navigate(Destinations.Main.route) {
        popUpTo(Destinations.Main.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    if (innerNav != null) {
        innerNav.navigate(MainTabRoute.Home.route) {
            popUpTo(innerNav.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        HomeNavRequest.pending.value = true
    }
}
