package com.callvault.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.callvault.app.R
import com.callvault.app.domain.repository.CallRepository
import com.callvault.app.domain.repository.UpdateRepository
import com.callvault.app.domain.repository.UpdateState
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoTab
import com.callvault.app.ui.components.neo.NeoTabBar
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.calls.CallsScreen
import com.callvault.app.ui.screen.export.QuickExportSheet
import com.callvault.app.ui.screen.home.HomeScreen
import com.callvault.app.ui.screen.inquiries.InquiriesScreen
import com.callvault.app.ui.screen.more.MoreScreen
import com.callvault.app.ui.theme.IconCallsTint
import com.callvault.app.ui.theme.IconHomeTint
import com.callvault.app.ui.theme.IconInquiriesTint
import com.callvault.app.ui.theme.NeoColors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.map

/**
 * Hilt entry-point exposing the repositories needed for top-bar badges.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MainScaffoldEntryPoint {
    fun callRepository(): CallRepository
    fun updateRepository(): UpdateRepository
}

/**
 * Tabbed root surface that hosts Home / Calls / Inquiries / More inside a
 * single [Scaffold] with a shared [NeoTopBar] and [NeoTabBar].
 *
 * Full-screen flows (Search, Settings, CallDetail, etc.) live on the
 * [rootNavController] and replace this surface entirely. Tab switches happen
 * on a nested NavController and preserve scroll/state via
 * [rememberSaveableStateHolder].
 *
 * @param rootNavController top-level NavController used for full-screen flows.
 */
@Composable
fun MainScaffold(
    rootNavController: NavController,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val entry = remember(ctx) {
        EntryPointAccessors.fromApplication(
            ctx.applicationContext,
            MainScaffoldEntryPoint::class.java
        )
    }
    val callRepo = entry.callRepository()
    val updateRepo = entry.updateRepository()

    val unsavedFlow = remember(callRepo) { callRepo.observeUnsavedLast7Days().map { it.size } }
    val unsavedCount by unsavedFlow.collectAsStateWithLifecycle(initialValue = 0)
    val updateState by updateRepo.state.collectAsStateWithLifecycle()

    val updateBadge = when (val s = updateState) {
        is UpdateState.Available -> if (!s.isSkipped) 1 else null
        else -> null
    }

    val innerNav = rememberNavController()
    val backStack by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: MainTabRoute.Calls.route

    val tabs = listOf(
        MainTabRoute.Home,
        MainTabRoute.Calls,
        MainTabRoute.Inquiries,
        MainTabRoute.More
    )
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val homeLabel = stringResource(R.string.cv_tab_home)
    val callsLabel = stringResource(R.string.cv_tab_calls)
    val inquiriesLabel = stringResource(R.string.cv_tab_inquiries)
    val moreLabel = stringResource(R.string.cv_tab_more)
    val brand = stringResource(R.string.cv_main_brand)

    val neoTabs = listOf(
        NeoTab(homeLabel, Icons.Filled.Home, activeTint = IconHomeTint),
        NeoTab(callsLabel, Icons.Filled.Call, activeTint = IconCallsTint),
        NeoTab(
            inquiriesLabel,
            Icons.Filled.Inbox,
            badge = unsavedCount.takeIf { it > 0 },
            activeTint = IconInquiriesTint
        ),
        NeoTab(moreLabel, Icons.Filled.MoreHoriz, badge = updateBadge, activeTint = NeoColors.OnBase)
    )

    var overflowOpen by remember { mutableStateOf(false) }
    var quickExportOpen by rememberSaveable { mutableStateOf(false) }
    val saveableHolder = rememberSaveableStateHolder()

    Scaffold(
        modifier = modifier,
        containerColor = NeoColors.Base,
        topBar = {
            NeoTopBar(
                title = brand,
                showBrand = true,
                actions = {
                    NeoIconButton(
                        icon = Icons.Filled.Search,
                        onClick = { rootNavController.navigate(Destinations.Search.route) },
                        contentDescription = stringResource(R.string.cv_topbar_search_cd)
                    )
                    Box {
                        NeoIconButton(
                            icon = Icons.Filled.MoreVert,
                            onClick = { overflowOpen = true },
                            contentDescription = stringResource(R.string.cv_topbar_more_cd)
                        )
                        DropdownMenu(
                            expanded = overflowOpen,
                            onDismissRequest = { overflowOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cv_overflow_profile)) },
                                onClick = {
                                    overflowOpen = false
                                    // Placeholder — Profile screen not built; route to Settings.
                                    rootNavController.navigate(Destinations.Settings.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cv_overflow_settings)) },
                                onClick = {
                                    overflowOpen = false
                                    rootNavController.navigate(Destinations.Settings.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cv_overflow_backup)) },
                                onClick = {
                                    overflowOpen = false
                                    rootNavController.navigate(Destinations.Backup.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cv_overflow_quick_export)) },
                                onClick = {
                                    overflowOpen = false
                                    quickExportOpen = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cv_overflow_stats)) },
                                onClick = {
                                    overflowOpen = false
                                    rootNavController.navigate(Destinations.Settings.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cv_overflow_tags)) },
                                onClick = {
                                    overflowOpen = false
                                    rootNavController.navigate(Destinations.AutoTagRules.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cv_overflow_rules)) },
                                onClick = {
                                    overflowOpen = false
                                    rootNavController.navigate(Destinations.AutoTagRules.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cv_overflow_updates)) },
                                onClick = {
                                    overflowOpen = false
                                    rootNavController.navigate(Destinations.UpdateSettings.route)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cv_overflow_help)) },
                                onClick = {
                                    overflowOpen = false
                                    rootNavController.navigate(Destinations.DocsList.route)
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NeoTabBar(
                tabs = neoTabs,
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    val target = tabs[index].route
                    if (target != currentRoute) {
                        innerNav.navigate(target) {
                            popUpTo(innerNav.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NeoColors.Base)
        ) {
            NavHost(
                navController = innerNav,
                startDestination = MainTabRoute.Calls.route
            ) {
                composable(MainTabRoute.Home.route) {
                    saveableHolder.SaveableStateProvider(MainTabRoute.Home.route) {
                        val switchTab: (String) -> Unit = { route ->
                            if (route != currentRoute) {
                                innerNav.navigate(route) {
                                    popUpTo(innerNav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        HomeScreen(
                            onNavigateCalls = { switchTab(MainTabRoute.Calls.route) },
                            onNavigateInquiries = { switchTab(MainTabRoute.Inquiries.route) },
                            onNavigateStats = {
                                rootNavController.navigate(Destinations.Settings.route)
                            },
                            onNavigateBackup = {
                                rootNavController.navigate(Destinations.Backup.route)
                            },
                            onNavigateQuickExport = {
                                quickExportOpen = true
                            },
                            onNavigateFollowUps = {
                                switchTab(MainTabRoute.Calls.route)
                            },
                        )
                    }
                }
                composable(MainTabRoute.Calls.route) {
                    saveableHolder.SaveableStateProvider(MainTabRoute.Calls.route) {
                        CallsScreen(
                            onOpenDetail = { number ->
                                rootNavController.navigate(Destinations.CallDetail.routeFor(number))
                            },
                            onOpenSearch = {
                                rootNavController.navigate(Destinations.Search.route)
                            },
                            onOpenFilterPresets = {
                                rootNavController.navigate(Destinations.FilterPresets.route)
                            },
                            onPermissionMissing = {
                                rootNavController.navigate(Destinations.PermissionRationale.route)
                            },
                            onOpenMyContacts = {
                                rootNavController.navigate(Destinations.MyContacts.route)
                            },
                            onOpenInquiries = {
                                rootNavController.navigate(Destinations.Inquiries.route)
                            },
                            onOpenAutoSaveSettings = {
                                rootNavController.navigate(Destinations.AutoSaveSettings.route)
                            },
                            onOpenAutoTagRules = {
                                rootNavController.navigate(Destinations.AutoTagRules.route)
                            },
                            onOpenLeadScoringSettings = {
                                rootNavController.navigate(Destinations.LeadScoringSettings.route)
                            },
                            onOpenRealTimeSettings = {
                                rootNavController.navigate(Destinations.RealTimeSettings.route)
                            },
                            onOpenExport = {
                                rootNavController.navigate(Destinations.Export.route)
                            },
                            onOpenBackup = {
                                rootNavController.navigate(Destinations.Backup.route)
                            },
                            onOpenUpdateAvailable = {
                                rootNavController.navigate(Destinations.UpdateAvailable.route)
                            },
                            onOpenUpdateSettings = {
                                rootNavController.navigate(Destinations.UpdateSettings.route)
                            },
                            onOpenSettings = {
                                rootNavController.navigate(Destinations.Settings.route)
                            },
                            onOpenDocs = {
                                rootNavController.navigate(Destinations.DocsList.route)
                            }
                        )
                    }
                }
                composable(MainTabRoute.Inquiries.route) {
                    saveableHolder.SaveableStateProvider(MainTabRoute.Inquiries.route) {
                        InquiriesScreen(
                            onBack = { /* no-op — tab root has no back */ },
                            onOpenDetail = { number ->
                                rootNavController.navigate(Destinations.CallDetail.routeFor(number))
                            }
                        )
                    }
                }
                composable(MainTabRoute.More.route) {
                    saveableHolder.SaveableStateProvider(MainTabRoute.More.route) {
                        MoreScreen(navController = rootNavController)
                    }
                }
            }
            if (quickExportOpen) {
                QuickExportSheet(onDismiss = { quickExportOpen = false })
            }
        }
    }
}
