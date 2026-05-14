package com.callNest.app.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import com.callNest.app.R
import com.callNest.app.domain.repository.CallRepository
import com.callNest.app.ui.components.neo.NeoIconButton
import com.callNest.app.ui.components.neo.NeoTab
import com.callNest.app.ui.components.neo.NeoTabBar
import com.callNest.app.ui.components.neo.NeoTopBar
import com.callNest.app.ui.screen.calls.CallsScreen
import com.callNest.app.ui.screen.export.QuickExportSheet
import com.callNest.app.ui.screen.inquiries.InquiriesScreen
import com.callNest.app.ui.screen.auth.AuthViewModel
import com.callNest.app.ui.screen.more.MoreScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.callNest.app.ui.theme.IconCallsTint
import com.callNest.app.ui.theme.IconInquiriesTint
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.theme.Spacing
import com.callNest.app.ui.theme.TabBgCalls
import com.callNest.app.ui.theme.TabBgInquiries
import com.callNest.app.ui.theme.TabBgMore
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

    val unsavedFlow = remember(callRepo) { callRepo.observeUnsavedLast7Days().map { it.size } }
    val unsavedCount by unsavedFlow.collectAsStateWithLifecycle(initialValue = 0)

    // Update badge removed — in-app update flow deleted; users get new
    // builds from https://callnest.pooniya.com instead.
    val updateBadge: Int? = null

    val innerNav = rememberNavController()
    val backStack by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: MainTabRoute.Calls.route

    val tabs = listOf(
        MainTabRoute.Calls,
        MainTabRoute.Insights,
        MainTabRoute.Inquiries,
        MainTabRoute.More
    )
    val selectedIndex = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    val callsLabel = stringResource(R.string.cv_tab_calls)
    val insightsLabel = "Insights"
    val inquiriesLabel = stringResource(R.string.cv_tab_inquiries)
    val moreLabel = stringResource(R.string.cv_tab_more)
    val brand = stringResource(R.string.cv_main_brand)

    val neoTabs = listOf(
        NeoTab(callsLabel, Icons.Filled.Call, activeTint = IconCallsTint),
        NeoTab(insightsLabel, Icons.Filled.BarChart, activeTint = NeoColors.AccentBlue),
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

    // popToHome callers now land on Calls (Home tab removed from bottom nav).
    LaunchedEffect(Unit) {
        HomeNavRequest.pending.collect { wantsHome ->
            if (wantsHome) {
                innerNav.navigate(MainTabRoute.Calls.route) {
                    popUpTo(innerNav.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                HomeNavRequest.pending.value = false
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var lastBackPressMs by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val backExitMessage = stringResource(R.string.cv_back_press_exit)
    val canPopInner = innerNav.previousBackStackEntry != null

    BackHandler(enabled = !canPopInner) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressMs < 2_000L) {
            (ctx as? Activity)?.finish()
        } else {
            lastBackPressMs = now
            coroutineScope.launch { snackbarHostState.showSnackbar(backExitMessage) }
        }
    }

    val currentTabBg = when (tabs[selectedIndex]) {
        MainTabRoute.Calls -> TabBgCalls
        MainTabRoute.Insights -> TabBgCalls
        MainTabRoute.Inquiries -> TabBgInquiries
        MainTabRoute.More -> TabBgMore
    }

    Scaffold(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        containerColor = currentTabBg,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Phase III — brand top bar hidden; restore by uncommenting the topBar block below.
        // The page-header strip inside each StandardPage already shows the page title +
        // description, so the persistent "callNest [logo] 🔍 ⋮" bar duplicates branding.
        // Search route stays registered in CallNestNavHost; re-add an entry point when needed.
        // Sign out of Drive remains reachable from Backup → Cloud section.
        // Profile (placeholder) remains reachable from More → Settings.
        /*
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
                            // TODO Phase I.B: render only when DriveAuthManager reports signed-in.
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cv_overflow_signout)) },
                                onClick = {
                                    overflowOpen = false
                                    rootNavController.navigate(Destinations.Backup.route)
                                }
                            )
                        }
                    }
                }
            )
        },
        */
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SageColors.Canvas)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp).padding(top = 0.dp, bottom = 2.dp)
            ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(currentTabBg)
        ) {
            CompositionLocalProvider(LocalMainTabNav provides innerNav) {
            NavHost(
                navController = innerNav,
                startDestination = MainTabRoute.Calls.route
            ) {
                composable(MainTabRoute.Calls.route) {
                    saveableHolder.SaveableStateProvider(MainTabRoute.Calls.route) {
                        CallsScreen(
                            onOpenDetail = { number ->
                                rootNavController.navigate(Destinations.CallDetail.routeFor(number))
                            },
                            onOpenSearch = {
                                rootNavController.navigate(Destinations.Search.route)
                            },
                            onOpenFilterPresets = { /* removed: filter presets not implemented */ },
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
                            onOpenRealTimeSettings = {
                                rootNavController.navigate(Destinations.RealTimeSettings.route)
                            },
                            onOpenExport = {
                                rootNavController.navigate(Destinations.Export.route)
                            },
                            onOpenBackup = {
                                rootNavController.navigate(Destinations.Backup.route)
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
                composable(MainTabRoute.Insights.route) {
                    saveableHolder.SaveableStateProvider(MainTabRoute.Insights.route) {
                        com.callNest.app.ui.screen.insights.InsightsScreen(
                            onOpenStats = {
                                rootNavController.navigate(Destinations.Stats.route)
                            },
                            onOpenWeeklyDigest = {
                                rootNavController.navigate(Destinations.WeeklyDigest.route)
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
                            },
                            onOpenAutoSaveSettings = {
                                rootNavController.navigate(Destinations.AutoSaveSettings.route)
                            }
                        )
                    }
                }
                composable(MainTabRoute.More.route) {
                    saveableHolder.SaveableStateProvider(MainTabRoute.More.route) {
                        val authViewModel: AuthViewModel = hiltViewModel()
                        MoreScreen(
                            navController = rootNavController,
                            onSignOut = {
                                // Fire VM (clears Supabase session, resets analytics, etc.)
                                authViewModel.signOut()
                                // Optimistically pop everything and land on auth.
                                // The outer LaunchedEffect on authState would do this too,
                                // but doing it here avoids the brief flash where the user
                                // still sees "Main" while sessionStatus propagates.
                                rootNavController.navigate(
                                    com.callNest.app.ui.screen.auth.AuthDestinations.GRAPH
                                ) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
            }
            if (quickExportOpen) {
                QuickExportSheet(onDismiss = { quickExportOpen = false })
            }
        }
    }
}
