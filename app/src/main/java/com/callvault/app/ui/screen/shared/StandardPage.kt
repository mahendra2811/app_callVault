package com.callvault.app.ui.screen.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoPageHeader
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.Spacing

/**
 * Reusable scaffold for content pages that share the standard CallVault chrome:
 * a [NeoTopBar], a [NeoPageHeader] and a vertically-stacked content column.
 *
 * @param title screen title shown in the top bar and page header.
 * @param description supporting copy rendered under the title in the page header.
 * @param emoji optional decorative emoji rendered to the left of the title.
 * @param onBack when non-null, a back arrow appears in the top bar.
 * @param actions trailing top-bar action slot.
 * @param showBrand renders the "C" brand chip next to the title (use only on
 *   the app's primary surface).
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
    content: @Composable ColumnScope.() -> Unit
) {
    NeoScaffold(
        topBar = {
            NeoTopBar(
                title = title,
                showBrand = showBrand,
                navIcon = if (onBack != null) Icons.AutoMirrored.Filled.ArrowBack else null,
                onNavClick = { onBack?.invoke() },
                actions = actions
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            NeoPageHeader(title = title, description = description, emoji = emoji)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = Spacing.PageHorizontal,
                        vertical = Spacing.SectionGap
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.SectionGap),
                content = content
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun StandardPageWithBackAndActionsPreview() {
    CallVaultTheme {
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

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun StandardPagePlainPreview() {
    CallVaultTheme {
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
