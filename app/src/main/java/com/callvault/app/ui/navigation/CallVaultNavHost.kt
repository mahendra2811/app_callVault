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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.callvault.app.ui.screen.calldetail.CallDetailScreen
import com.callvault.app.ui.screen.calls.CallsScreen
import com.callvault.app.ui.screen.inquiries.InquiriesScreen
import com.callvault.app.ui.screen.mycontacts.MyContactsScreen
import com.callvault.app.ui.screen.search.SearchScreen
import com.callvault.app.ui.screen.autotagrules.AutoTagRulesScreen
import com.callvault.app.ui.screen.autotagrules.RuleEditorScreen
import com.callvault.app.ui.screen.backup.BackupScreen
import com.callvault.app.ui.screen.export.ExportScreen
import com.callvault.app.ui.screen.settings.AutoSaveSettingsScreen
import com.callvault.app.ui.screen.settings.LeadScoringSettingsScreen
import com.callvault.app.ui.screen.settings.RealTimeSettingsScreen
import com.callvault.app.ui.screen.settings.SettingsScreen
import com.callvault.app.ui.screen.settings.UpdateSettingsScreen
import com.callvault.app.ui.screen.update.UpdateAvailableScreen
import com.callvault.app.ui.screen.docs.DocsListScreen
import com.callvault.app.ui.screen.docs.DocsArticleScreen
import com.callvault.app.ui.screen.home.HomeScreen
import com.callvault.app.ui.screen.more.MoreScreen
import androidx.compose.runtime.LaunchedEffect
import com.callvault.app.R
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.ui.screen.onboarding.OnboardingScreen
import com.callvault.app.ui.screen.onboarding.findActivity
import com.callvault.app.ui.screen.permission.PermissionDeniedScreen
import com.callvault.app.ui.screen.permission.PermissionRationaleScreen
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
}

/**
 * Top-level navigation graph for CallVault.
 *
 * Picks the start destination at runtime:
 * - If [SettingsDataStore.onboardingComplete] is `false` → [Destinations.Onboarding].
 * - Else if [PermissionManager.isCriticalGranted] is `false` → [Destinations.PermissionRationale].
 * - Else → [Destinations.Calls] (the placeholder Calls home until Sprint 3).
 *
 * The graph re-evaluates the start destination only on first composition;
 * later transitions happen via explicit nav events.
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

    val onboardingComplete by settings.onboardingComplete
        .collectAsStateWithLifecycle(initialValue = false)
    val permState by permissionManager.state.collectAsState()

    val navController = rememberNavController()

    val startRoute = remember(onboardingComplete, permState) {
        when {
            !onboardingComplete -> Destinations.Onboarding.route
            !permissionManager.isCriticalGranted() -> Destinations.PermissionRationale.route
            else -> Destinations.Calls.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        modifier = modifier
    ) {
        composable(Destinations.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    val target = if (permissionManager.isCriticalGranted()) {
                        Destinations.Calls.route
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
                        navController.navigate(Destinations.Calls.route) {
                            popUpTo(Destinations.PermissionRationale.route) { inclusive = true }
                        }
                    } else {
                        // If the user picked "Don't ask again", route to denied.
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
        composable(Destinations.Calls.route) {
            CallsScreen(
                onOpenDetail = { number ->
                    navController.navigate(Destinations.CallDetail.routeFor(number))
                },
                onOpenSearch = { navController.navigate(Destinations.Search.route) },
                onOpenFilterPresets = {
                    navController.navigate(Destinations.FilterPresets.route)
                },
                onPermissionMissing = {
                    navController.navigate(Destinations.PermissionRationale.route)
                },
                onOpenMyContacts = { navController.navigate(Destinations.MyContacts.route) },
                onOpenInquiries = { navController.navigate(Destinations.Inquiries.route) },
                onOpenAutoSaveSettings = {
                    navController.navigate(Destinations.AutoSaveSettings.route)
                },
                onOpenAutoTagRules = {
                    navController.navigate(Destinations.AutoTagRules.route)
                },
                onOpenLeadScoringSettings = {
                    navController.navigate(Destinations.LeadScoringSettings.route)
                },
                onOpenRealTimeSettings = {
                    navController.navigate(Destinations.RealTimeSettings.route)
                },
                onOpenExport = { navController.navigate(Destinations.Export.route) },
                onOpenBackup = { navController.navigate(Destinations.Backup.route) },
                onOpenUpdateAvailable = { navController.navigate(Destinations.UpdateAvailable.route) },
                onOpenUpdateSettings = { navController.navigate(Destinations.UpdateSettings.route) },
                onOpenSettings = { navController.navigate(Destinations.Settings.route) },
                onOpenDocs = { navController.navigate(Destinations.DocsList.route) }
            )
        }
        composable(Destinations.Export.route) {
            ExportScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Backup.route) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.AutoTagRules.route) {
            AutoTagRulesScreen(
                onBack = { navController.popBackStack() },
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
            RuleEditorScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.LeadScoringSettings.route) {
            LeadScoringSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.RealTimeSettings.route) {
            RealTimeSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.MyContacts.route) {
            MyContactsScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { number ->
                    navController.navigate(Destinations.CallDetail.routeFor(number))
                }
            )
        }
        composable(Destinations.Inquiries.route) {
            InquiriesScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { number ->
                    navController.navigate(Destinations.CallDetail.routeFor(number))
                }
            )
        }
        composable(Destinations.AutoSaveSettings.route) {
            AutoSaveSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Destinations.CallDetail.route,
            arguments = listOf(
                navArgument(Destinations.CallDetail.ARG_NUMBER) { type = NavType.StringType }
            )
        ) {
            CallDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Destinations.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { number ->
                    navController.navigate(Destinations.CallDetail.routeFor(number))
                }
            )
        }
        composable(Destinations.FilterPresets.route) {
            CallsPlaceholderScreen()
        }
        composable(Destinations.UpdateAvailable.route) {
            UpdateAvailableScreen(onClose = { navController.popBackStack() })
        }
        composable(Destinations.UpdateSettings.route) {
            UpdateSettingsScreen()
        }
        composable(Destinations.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Destinations.DocsList.route) {
            DocsListScreen(navController = navController)
        }
        composable(
            route = Destinations.DocsArticle.route,
            arguments = listOf(
                navArgument(Destinations.DocsArticle.ARG_ARTICLE_ID) { type = NavType.StringType }
            )
        ) {
            DocsArticleScreen(navController = navController)
        }
        composable(Destinations.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Destinations.More.route) {
            MoreScreen(navController = navController)
        }
    }

    // Deep-link from notification (e.g. "update_available" → UpdateAvailable route).
    LaunchedEffect(initialDeepLink) {
        when (initialDeepLink) {
            "update_available" -> navController.navigate(Destinations.UpdateAvailable.route)
            "daily_summary" -> navController.navigate(Destinations.Calls.route)
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

/**
 * Placeholder destination for the Calls home tab — replaces Sprint 0's
 * `PlaceholderScreen`. The real list lands in Sprint 3.
 */
@Composable
private fun CallsPlaceholderScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeoColors.Base)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.calls_placeholder_title),
                color = NeoColors.OnBase,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.calls_placeholder_body),
                color = NeoColors.OnBaseMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun CallsPlaceholderPreview() {
    CallVaultTheme { CallsPlaceholderScreen() }
}
