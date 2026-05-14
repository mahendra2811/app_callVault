package com.callNest.app.ui.screen.shared

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.components.neo.NeoCard
import com.callNest.app.ui.components.neo.NeoIconButton
import com.callNest.app.ui.components.neo.NeoPageHeader
import com.callNest.app.ui.components.neo.NeoTopBar
import com.callNest.app.ui.components.neo.NeoTopLineLoader
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.theme.Spacing

/**
 * Reusable scaffold for content pages that share the standard callNest chrome:
 * a [NeoTopBar], an optional thin [NeoTopLineLoader], a [NeoPageHeader] and a
 * vertically-stacked content column.
 *
 * @param title screen title shown in the top bar and page header.
 * @param description supporting copy rendered under the title in the page header.
 * @param emoji optional decorative emoji rendered to the left of the title.
 * @param onBack when non-null, a back arrow appears in the top bar.
 * @param actions trailing top-bar action slot.
 * @param showBrand renders the "C" brand chip next to the title (use only on
 *   the app's primary surface).
 * @param onBackToHome when non-null, hardware back routes to home instead of popping.
 * @param loading when true, shows a 3.dp top-line indeterminate loader between
 *   the top bar and page header.
 * @param backgroundColor outer container color, painted full-bleed (insets
 *   included) by the underlying [NeoScaffold]. Defaults to [SageColors.Canvas].
 * @param headerGradient optional vertical gradient (top → bottom) painted behind
 *   the page header.
 * @param chromeless when true, suppresses both the [NeoTopBar] and [NeoPageHeader].
 *   Used by the 4 main tabs (Home/Calls/Inquiries/More) where the user has chosen
 *   to hide page chrome — see Phase III in the plan. Default false preserves all
 *   deep-page chrome.
 * @param content section column body. Vertical spacing between children is
 *   [Spacing.SectionGap].
 */
@Composable
fun StandardPage(
    title: String,
    description: String,
    emoji: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    showBrand: Boolean = false,
    onBackToHome: (() -> Unit)? = null,
    loading: Boolean = false,
    backgroundColor: Color = SageColors.Canvas,
    headerGradient: Pair<Color, Color>? = null,
    chromeless: Boolean = false,
    scrollable: Boolean = false,
    /**
     * Optional in-app docs article id (matches `assets/docs/<id>.md`).
     * When set, a small "?" icon appears in the top-bar actions row that
     * deep-links into the matching help article.
     */
    helpArticleId: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val rootNav = com.callNest.app.ui.navigation.LocalRootNav.current
    val scrollState = rememberScrollState()
    if (onBackToHome != null) {
        BackHandler { onBackToHome() }
    }
    val headerBrush = headerGradient?.let {
        Brush.verticalGradient(listOf(it.first, it.second))
    }
    NeoScaffold(
        containerColor = backgroundColor,
        topBar = {
            if (!chromeless) {
                NeoTopBar(
                    title = title,
                    showBrand = showBrand,
                    navIcon = if (onBack != null) Icons.AutoMirrored.Filled.ArrowBack else null,
                    onNavClick = { onBack?.invoke() },
                    actions = {
                        actions()
                        if (helpArticleId != null && rootNav != null) {
                            com.callNest.app.ui.components.neo.NeoHelpIcon(
                                articleId = helpArticleId,
                                navController = rootNav
                            )
                        }
                    }
                )
            }
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            AnimatedVisibility(visible = loading) {
                NeoTopLineLoader()
            }
            if (!chromeless) {
                NeoPageHeader(
                    title = title,
                    description = description,
                    emoji = emoji,
                    backgroundBrush = headerBrush
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
                    .padding(horizontal = Spacing.PageHorizontal)
                    .padding(top = Spacing.Xs, bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.Lg),
                content = content
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F1EA, widthDp = 360, heightDp = 720)
@Composable
private fun StandardPageWithBackAndActionsPreview() {
    CallNestTheme {
        StandardPage(
            title = "Backup",
            description = "Encrypted snapshots of your call history.",
            emoji = "💾",
            onBack = {},
            actions = {
                NeoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = {},
                    contentDescription = "Refresh"
                )
            }
        ) {
            NeoCard {
                Text(text = "Latest backup: 2 hours ago", color = NeoColors.OnBase)
            }
            NeoCard {
                Text(text = "Storage used: 12 MB", color = NeoColors.OnBaseMuted)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F1EA, widthDp = 360, heightDp = 720)
@Composable
private fun StandardPagePlainPreview() {
    CallNestTheme {
        StandardPage(
            title = "Stats",
            description = "Your weekly call activity.",
            emoji = "📊"
        ) {
            NeoCard {
                Text(text = "42 calls this week", color = NeoColors.OnBase)
            }
        }
    }
}
