package com.callvault.app.ui.screen.more

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.navigation.Destinations
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.HeaderGradMoreEnd
import com.callvault.app.ui.theme.HeaderGradMoreStart
import com.callvault.app.ui.theme.IconBackupTint
import com.callvault.app.ui.theme.IconCallsTint
import com.callvault.app.ui.theme.IconHomeTint
import com.callvault.app.ui.theme.IconInquiriesTint
import com.callvault.app.ui.theme.IconStatsTint
import com.callvault.app.ui.theme.IconTagsTint
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.ui.theme.Spacing
import com.callvault.app.ui.theme.TabBgMore

private data class MoreRow(
    val emoji: String,
    val title: String,
    val tint: Color,
    val onClick: () -> Unit
)

/**
 * Sprint 11 — More tab. Three grouped sections (Data, Automation, App) of
 * secondary surfaces, each rendered inside a [NeoCard] with colored emoji
 * leading icons and a chevron trailing affordance.
 *
 * @param onOpenQuickExport opens the parent-controlled QuickExport sheet.
 */
@Composable
fun MoreScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    onOpenQuickExport: () -> Unit = {}
) {
    val data = listOf(
        MoreRow("📤", "Export", IconBackupTint) { navController.navigate(Destinations.Export.route) },
        MoreRow("⚡", "Quick Export", IconBackupTint) { onOpenQuickExport() },
        MoreRow("💾", "Backup & restore", IconBackupTint) { navController.navigate(Destinations.Backup.route) },
        MoreRow("🏷️", "Tags", IconTagsTint) { navController.navigate(Destinations.Settings.route) }
    )
    val automation = listOf(
        MoreRow("🪄", "Auto-tag rules", IconTagsTint) { navController.navigate(Destinations.AutoTagRules.route) },
        MoreRow("🎯", "Lead scoring", IconStatsTint) { navController.navigate(Destinations.LeadScoringSettings.route) },
        MoreRow("✨", "Real-time features", IconCallsTint) { navController.navigate(Destinations.RealTimeSettings.route) },
        MoreRow("💡", "Auto-save", IconInquiriesTint) { navController.navigate(Destinations.AutoSaveSettings.route) }
    )
    val app = listOf(
        MoreRow("📊", "Stats", IconStatsTint) { navController.navigate(Destinations.Settings.route) },
        MoreRow("🆙", "App updates", IconCallsTint) { navController.navigate(Destinations.UpdateSettings.route) },
        MoreRow("📚", "Help & docs", IconHomeTint) { navController.navigate(Destinations.DocsList.route) },
        MoreRow("⚙️", "Settings", NeoColors.OnBaseMuted) { navController.navigate(Destinations.Settings.route) }
    )

    StandardPage(
        title = stringResource(R.string.more_title),
        description = "Advanced features and settings",
        emoji = "⚙️",
        backgroundColor = TabBgMore,
        headerGradient = HeaderGradMoreStart to HeaderGradMoreEnd,
        chromeless = true, // Phase III — hide page top bar + header on main tabs
        scrollable = true,
    ) {
        MoreGroup(stringResource(R.string.cv_more_group_data), data)
        MoreGroup(stringResource(R.string.cv_more_group_automation), automation)
        MoreGroup(stringResource(R.string.cv_more_group_app), app)
    }
}

@Composable
private fun MoreGroup(title: String, rows: List<MoreRow>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = SageColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            rows.forEach { row -> MoreRowView(row) }
        }
    }
}

@Composable
private fun MoreRowView(row: MoreRow) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = row.onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoSurface(
                elevation = NeoElevation.ConcaveSmall,
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = row.emoji, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = row.title,
                style = MaterialTheme.typography.bodyLarge,
                color = SageColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SageColors.TextTertiary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF3E5, widthDp = 360, heightDp = 720)
@Composable
private fun MorePreview() {
    CallVaultTheme {
        MoreScreen(navController = rememberNavController())
    }
}
