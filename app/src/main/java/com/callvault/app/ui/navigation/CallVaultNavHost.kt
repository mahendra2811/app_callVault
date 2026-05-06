package com.callvault.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.callvault.app.R
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.domain.model.AuthState
import com.callvault.app.domain.repository.AuthRepository
import com.callvault.app.ui.screen.auth.LoginScreen
import com.callvault.app.ui.screen.autotagrules.AutoTagRulesScreen
import com.callvault.app.ui.screen.autotagrules.RuleEditorScreen
import com.callvault.app.ui.screen.backup.BackupScreen
import com.callvault.app.ui.screen.calldetail.CallDetailScreen
import com.callvault.app.ui.screen.docs.DocsArticleScreen
import com.callvault.app.ui.screen.docs.DocsListScreen
import com.callvault.app.ui.screen.export.ExportScreen
import com.callvault.app.ui.screen.inquiries.InquiriesScreen
import com.callvault.app.ui.screen.mycontacts.MyContactsScreen
import com.callvault.app.ui.screen.onboarding.OnboardingScreen
import com.callvault.app.ui.screen.onboarding.findActivity
import com.callvault.app.ui.screen.permission.PermissionDeniedScreen
import com.callvault.app.ui.screen.permission.PermissionRationaleScreen
import com.callvault.app.ui.screen.search.SearchScreen
import com.callvault.app.ui.screen.splash.SplashScreen
import com.callvault.app.ui.screen.stats.StatsScreen
import com.callvault.app.ui.screen.tags.TagsManagerScreen
import com.callvault.app.ui.screen.settings.AutoSaveSettingsScreen
import com.callvault.app.ui.screen.settings.LeadScoringSettingsScreen
import com.callvault.app.ui.screen.settings.RealTimeSettingsScreen
import com.callvault.app.ui.screen.settings.SettingsScreen
import com.callvault.app.ui.screen.settings.UpdateSettingsScreen
import com.callvault.app.ui.screen.update.UpdateAvailableScreen
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.util.PermissionManager
import com.callvault.app.util.PermissionStatus
import com.callvault.app.util.rememberPermissionLauncher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry-point that exposes the bootstrap dependencies the NavHost needs
 * before any view-model has been resolved.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NavHostEntryPoint {
    fun settings(): SettingsDataStore
    fun permissionManager(): PermissionManager
    fun authRepository(): AuthRepository
}

/**
 * Top-level navigation graph for CallVault.
 *
 * Picks the start destination at runtime:
 * - If [SettingsDataStore.onboardingComplete] is `false` → [Destinations.Onboarding].
 * - Else if [PermissionManager.isCriticalGranted] is `false` → [Destinations.PermissionRationale].
 * - Else → [Destinations.Main] (the tabbed surface).
 *
 * Phase C will swap the post-permission destination to [Destinations.Splash].
 */
@Composable
fun CallVaultNavHost(
    modifier: Modifier = Modifier,
    initialDeepLink: String? = null
) {
    val ctx = LocalContext.current
    val entry = remember(ctx) {
        EntryPointAccessors.fromApplication(ctx.applicationContext, NavHostEntryPoint::class.java)
    }
    val settings = entry.settings()
    val permissionManager = entry.permissionManager()
    val authRepository = entry.authRepository()

    val onboardingComplete by settings.onboardingComplete
        .collectAsStateWithLifecycle(initialValue = false)
    val permState by permissionManager.state.collectAsState()
    val authState by authRepository.state
        .collectAsStateWithLifecycle(initialValue = AuthState.Loading)

    val navController = rememberNavController()

    fun postLoginRoute(): String = when {
        !onboardingComplete -> Destinations.Onboarding.route
        !permissionManager.isCriticalGranted() -> Destinations.PermissionRationale.route
        else -> Destinations.Main.route
    }

    val toHome: () -> Unit = {
        if (!navController.popBackStack()) popToHome(navController, null)
    }

    CompositionLocalProvider(LocalRootNav provides navController) {
    NavHost(
        navController = navController,
        startDestination = Destinations.Splash.route,
        modifier = modifier
    ) {
        composable(Destinations.Splash.route) {
            var splashFinished by rememberSaveable { mutableStateOf(false) }
            SplashScreen(onFinished = { splashFinished = true })
            LaunchedEffect(splashFinished, authState) {
                if (splashFinished && authState !is AuthState.Loading) {
                    val target = if (authState is AuthState.SignedIn) {
                        postLoginRoute()
                    } else {
                        Destinations.Login.route
                    }
                    navController.navigate(target) {
                        popUpTo(Destinations.Splash.route) { inclusive = true }
                    }
                }
            }
        }
        composable(Destinations.Login.route) {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate(postLoginRoute()) {
                        popUpTo(Destinations.Login.route) { inclusive = true }
                    }
                },
                onForgotPassword = {},
                onCreateAccount = {},
            )
        }
        composable(Destinations.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    val target = if (permissionManager.isCriticalGranted()) {
                        Destinations.Main.route
                    } else Destinations.PermissionRationale.route
                    navController.navigate(target) {
                        popUpTo(Destinations.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Destinations.PermissionRationale.route) {
            val activity = remember(ctx) { ctx.findActivity() }
            val launcher = rememberPermissionLauncher(
                permissionManager = permissionManager,
                activity = activity,
                onResult = {
                    if (permissionManager.isCriticalGranted()) {
                        navController.navigate(Destinations.Main.route) {
                            popUpTo(Destinations.PermissionRationale.route) { inclusive = true }
                        }
                    } else {
                        val anyPerm = permState
                        val anyPermanent = listOf(
                            anyPerm.readCallLog,
                            anyPerm.readContacts,
                            anyPerm.readPhoneState
                        ).any { it == PermissionStatus.PermanentlyDenied }
                        if (anyPermanent) {
                            navController.navigate(Destinations.PermissionDenied.route) {
                                popUpTo(Destinations.PermissionRationale.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
            val missing = friendlyMissing(permState)
            val perms = remember {
                arrayOf(
                    android.Manifest.permission.READ_CALL_LOG,
                    android.Manifest.permission.READ_CONTACTS,
                    android.Manifest.permission.READ_PHONE_STATE
                )
            }
            PermissionRationaleScreen(
                missing = missing,
                permissions = perms,
                launcher = launcher
            )
        }
        composable(Destinations.PermissionDenied.route) {
            PermissionDeniedScreen(permissionManager = permissionManager)
        }
        composable(Destinations.Main.route) {
            MainScaffold(rootNavController = navController)
        }
        composable(Destinations.Export.route) {
            BackHandler { toHome() }
            ExportScreen(onBack = toHome)
        }
        composable(Destinations.Backup.route) {
            BackHandler { toHome() }
            BackupScreen(onBack = toHome)
        }
        composable(Destinations.AutoTagRules.route) {
            BackHandler { toHome() }
            AutoTagRulesScreen(
                onBack = toHome,
                onOpenEditor = { ruleId ->
                    navController.navigate(Destinations.RuleEditor.routeFor(ruleId))
                }
            )
        }
        composable(
            route = Destinations.RuleEditor.route,
            arguments = listOf(
                navArgument(Destinations.RuleEditor.ARG_RULE_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            BackHandler { toHome() }
            RuleEditorScreen(onBack = toHome)
        }
        composable(Destinations.LeadScoringSettings.route) {
            BackHandler { toHome() }
            LeadScoringSettingsScreen(onBack = toHome)
        }
        composable(Destinations.RealTimeSettings.route) {
            BackHandler { toHome() }
            RealTimeSettingsScreen(onBack = toHome)
        }
        composable(Destinations.MyContacts.route) {
            BackHandler { toHome() }
            MyContactsScreen(
                onBack = toHome,
                onOpenDetail = { number ->
                    navController.navigate(Destinations.CallDetail.routeFor(number))
                }
            )
        }
        composable(Destinations.Inquiries.route) {
            BackHandler { toHome() }
            InquiriesScreen(
                onBack = toHome,
                onOpenDetail = { number ->
                    navController.navigate(Destinations.CallDetail.routeFor(number))
                }
            )
        }
        composable(Destinations.AutoSaveSettings.route) {
            BackHandler { toHome() }
            AutoSaveSettingsScreen(onBack = toHome)
        }
        composable(
            route = Destinations.CallDetail.route,
            arguments = listOf(
                navArgument(Destinations.CallDetail.ARG_NUMBER) { type = NavType.StringType }
            )
        ) {
            BackHandler { toHome() }
            CallDetailScreen(onBack = toHome)
        }
        composable(Destinations.Search.route) {
            BackHandler { toHome() }
            SearchScreen(
                onBack = toHome,
                onOpenDetail = { number ->
                    navController.navigate(Destinations.CallDetail.routeFor(number))
                }
            )
        }
        composable(Destinations.UpdateAvailable.route) {
            BackHandler { toHome() }
            UpdateAvailableScreen(onClose = toHome)
        }
        composable(Destinations.UpdateSettings.route) {
            BackHandler { toHome() }
            UpdateSettingsScreen()
        }
        composable(Destinations.Settings.route) {
            BackHandler { toHome() }
            SettingsScreen(navController = navController)
        }
        composable(Destinations.Tags.route) {
            BackHandler { toHome() }
            TagsManagerScreen(onBack = toHome)
        }
        composable(Destinations.Stats.route) {
            BackHandler { toHome() }
            StatsScreen(onBack = toHome)
        }
        composable(Destinations.DocsList.route) {
            BackHandler { toHome() }
            DocsListScreen(navController = navController)
        }
        composable(
            route = Destinations.DocsArticle.route,
            arguments = listOf(
                navArgument(Destinations.DocsArticle.ARG_ARTICLE_ID) { type = NavType.StringType }
            )
        ) {
            BackHandler { toHome() }
            DocsArticleScreen(navController = navController)
        }
    }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.SignedOut) {
            val current = navController.currentBackStackEntry?.destination?.route
            if (current != null &&
                current != Destinations.Login.route &&
                current != Destinations.Splash.route
            ) {
                navController.navigate(Destinations.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(initialDeepLink) {
        when (initialDeepLink) {
            "update_available" -> navController.navigate(Destinations.UpdateAvailable.route)
            "daily_summary" -> navController.navigate(Destinations.Main.route)
            else -> Unit
        }
    }
}

private fun friendlyMissing(state: com.callvault.app.util.PermissionState): List<String> {
    val list = mutableListOf<String>()
    if (state.readCallLog != PermissionStatus.Granted) list += "Read call log"
    if (state.readContacts != PermissionStatus.Granted) list += "Read contacts"
    if (state.readPhoneState != PermissionStatus.Granted) list += "Phone state"
    return list
}

