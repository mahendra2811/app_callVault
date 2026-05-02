package com.callvault.app.ui.screen.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.callvault.app.ui.screen.onboarding.pages.FeaturesPage
import com.callvault.app.ui.screen.onboarding.pages.FirstSyncPage
import com.callvault.app.ui.screen.onboarding.pages.OemBatteryPage
import com.callvault.app.ui.screen.onboarding.pages.PermissionsPage
import com.callvault.app.ui.screen.onboarding.pages.WelcomePage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.util.PermissionManager
import com.callvault.app.util.rememberPermissionLauncher

/**
 * Top-level onboarding host.
 *
 * Renders a swipe-disabled [HorizontalPager] (advance only via Continue),
 * a row of progress dots above it, and the 5 onboarding pages.
 *
 * @param onFinished invoked once the user completes the final page or skips
 *   the first-sync error — the navigator should pop onboarding off the back
 *   stack and route to the calls home.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { viewModel.pageCount }
    )
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }

    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() }
    val permissionManager = rememberPermissionManager()
    val launcher = rememberPermissionLauncher(
        permissionManager = permissionManager,
        activity = activity,
        onResult = {
            viewModel.markPermissionsRequested()
            viewModel.next()
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoColors.Base)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProgressDots(
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp)
            )
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> WelcomePage(onContinue = viewModel::next)
                    1 -> FeaturesPage(onContinue = viewModel::next)
                    2 -> PermissionsPage(
                        onContinue = viewModel::next,
                        launcher = launcher
                    )
                    3 -> OemBatteryPage(onContinue = viewModel::next)
                    4 -> FirstSyncPage(
                        progress = state.firstSyncProgress,
                        total = state.firstSyncTotal,
                        done = state.firstSyncDone,
                        error = state.firstSyncError,
                        onStart = viewModel::startFirstSync,
                        onRetry = viewModel::startFirstSync,
                        onSkip = {
                            viewModel.complete()
                            onFinished()
                        },
                        onCompleted = {
                            viewModel.complete()
                            onFinished()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressDots(pagerState: PagerState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { idx ->
            val active = idx == pagerState.currentPage
            Box(
                modifier = Modifier
                    .size(if (active) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (active) NeoColors.AccentBlue else NeoColors.Inset)
            )
            if (idx != pagerState.pageCount - 1) Spacer(Modifier.size(8.dp))
        }
    }
}

/**
 * Hilt-aware accessor for the singleton [PermissionManager]. Lives here
 * because Compose can't `@Inject` directly into composables.
 */
@Composable
private fun rememberPermissionManager(): PermissionManager {
    val ctx = LocalContext.current
    return remember(ctx) {
        dagger.hilt.android.EntryPointAccessors
            .fromApplication(ctx.applicationContext, OnboardingEntryPoint::class.java)
            .permissionManager()
    }
}

/** Hilt entry point for non-DI-aware Compose helpers. */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface OnboardingEntryPoint {
    fun permissionManager(): PermissionManager
}

internal fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingScreenPreview() {
    CallVaultTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NeoColors.Base)
        ) {
            // Render the welcome page directly; full screen needs Hilt.
            com.callvault.app.ui.screen.onboarding.pages.WelcomePage(onContinue = {})
        }
    }
}
