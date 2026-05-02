package com.callvault.app.ui.components.neo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.Spacing

/**
 * Header block that lives directly under [NeoTopBar] on every page.
 *
 * Renders an ~80dp tall row: optional [emoji] · [title] (headlineSmall, bold)
 * with [description] beneath it · optional [trailingChip]. Provides the
 * "leave space at top to tell about the page" pattern requested by the user.
 *
 * @param title concise page name.
 * @param description one-line page summary, rendered muted.
 * @param emoji optional decorative leading mark; rendered titleLarge.
 * @param trailingChip optional composable shown right-aligned (e.g. a badge).
 */
@Composable
fun NeoPageHeader(
    title: String,
    description: String,
    emoji: String? = null,
    trailingChip: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.PageHorizontal,
                vertical = Spacing.PageTopHeader
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (emoji != null) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(Spacing.Md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = NeoColors.OnBase
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = NeoColors.OnBaseMuted
            )
        }
        if (trailingChip != null) {
            Spacer(modifier = Modifier.width(Spacing.Md))
            trailingChip()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "with emoji + chip")
@Composable
private fun NeoPageHeaderPreview() {
    CallVaultTheme {
        NeoPageHeader(
            title = "Calls",
            description = "Every call this week, sorted by date.",
            emoji = "📞",
            trailingChip = { Text("12", color = NeoColors.AccentBlue) }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "no emoji")
@Composable
private fun NeoPageHeaderPlainPreview() {
    CallVaultTheme {
        NeoPageHeader(
            title = "Settings",
            description = "Sync, real-time, lead score, privacy."
        )
    }
}
