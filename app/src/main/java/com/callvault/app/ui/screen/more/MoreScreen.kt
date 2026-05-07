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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onSignOut: () -> Unit = {},
) {
    var confirmLogout by remember { mutableStateOf(false) }

    val data = listOf(
        MoreRow("📈", stringResource(R.string.more_weekly_digest), IconStatsTint) {
            navController.navigate(Destinations.WeeklyDigest.route)
        },
        MoreRow("📊", stringResource(R.string.pipeline_screen_title), IconStatsTint) {
            navController.navigate(Destinations.Pipeline.route)
        },
        MoreRow("📥", stringResource(R.string.csv_import_screen_title), IconBackupTint) {
            navController.navigate(Destinations.CsvImport.route)
        },
        MoreRow("📤", "Export", IconBackupTint) { navController.navigate(Destinations.Export.route) },
        MoreRow("🏷️", "Tags", IconTagsTint) { navController.navigate(Destinations.Tags.route) }
    )
    val automation = listOf(
        MoreRow("🪄", "Auto-tag rules", IconTagsTint) { navController.navigate(Destinations.AutoTagRules.route) },
        MoreRow("🎯", "Lead scoring", IconStatsTint) { navController.navigate(Destinations.LeadScoringSettings.route) },
        MoreRow("✨", "Real-time features", IconCallsTint) { navController.navigate(Destinations.RealTimeSettings.route) },
        MoreRow("💡", "Auto-save", IconInquiriesTint) { navController.navigate(Destinations.AutoSaveSettings.route) }
    )
    val account = listOf(
        MoreRow("🚪", stringResource(R.string.more_logout), NeoColors.OnBaseMuted) { confirmLogout = true }
    )
    val app = listOf(
        MoreRow("📊", "Stats", IconStatsTint) { navController.navigate(Destinations.Stats.route) },
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
        MoreGroup(stringResource(R.string.cv_more_group_account), account)
        Spacer(Modifier.height(16.dp))
        MadeWithLoveFooter()
        Spacer(Modifier.height(24.dp))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.more_logout_confirm_title)) },
            text = { Text(stringResource(R.string.more_logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onSignOut()
                }) { Text(stringResource(R.string.more_logout_confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(stringResource(R.string.more_cancel))
                }
            },
        )
    }
}

@Composable
private fun MadeWithLoveFooter() {
    Text(
        text = "Made with ❤️ by Mahendra 🇮🇳",
        style = MaterialTheme.typography.bodyMedium,
        color = SageColors.TextTertiary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
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
