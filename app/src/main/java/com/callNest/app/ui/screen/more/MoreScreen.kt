package com.callNest.app.ui.screen.more

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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.callNest.app.R
import com.callNest.app.ui.components.neo.NeoCard
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.navigation.Destinations
import com.callNest.app.ui.screen.shared.StandardPage
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.HeaderGradMoreEnd
import com.callNest.app.ui.theme.HeaderGradMoreStart
import com.callNest.app.ui.theme.IconBackupTint
import com.callNest.app.ui.theme.IconCallsTint
import com.callNest.app.ui.theme.IconHomeTint
import com.callNest.app.ui.theme.IconInquiriesTint
import com.callNest.app.ui.theme.IconStatsTint
import com.callNest.app.ui.theme.IconTagsTint
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.theme.Spacing
import com.callNest.app.ui.theme.TabBgMore

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
        // MoreRow("📈", stringResource(R.string.more_weekly_digest), IconStatsTint) {
        //     navController.navigate(Destinations.WeeklyDigest.route)
        // },
        // Pipeline retired for v1.0.0.
        MoreRow("📥", stringResource(R.string.csv_import_screen_title), IconBackupTint) {
            navController.navigate(Destinations.CsvImport.route)
        },
        MoreRow("📤", "Export", IconBackupTint) { navController.navigate(Destinations.Export.route) },
        MoreRow("🏷️", "Tags", IconTagsTint) { navController.navigate(Destinations.Tags.route) }
    )
    val ctx = LocalContext.current
    val openWebsiteForUpdate: () -> Unit = {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://callnest.pooniya.com"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        }
    }
    val automation = listOf(
        MoreRow("💬", stringResource(R.string.quickreply_manage), IconCallsTint) {
            navController.navigate(Destinations.Templates.route)
        },
        MoreRow("🪄", "Auto-tag rules", IconTagsTint) { navController.navigate(Destinations.AutoTagRules.route) },
        // Lead scoring is hidden for v1.0.0 — fixed standard weights are in
        // ComputeLeadScoreUseCase. Restore by uncommenting once the UI is final.
        // MoreRow("🎯", "Lead scoring", IconStatsTint) { navController.navigate(Destinations.LeadScoringSettings.route) },
        MoreRow("✨", "Real-time features", IconCallsTint) { navController.navigate(Destinations.RealTimeSettings.route) },
        MoreRow("💡", "Auto-save", IconInquiriesTint) { navController.navigate(Destinations.AutoSaveSettings.route) }
    )
    val account = listOf(
        MoreRow("🚪", stringResource(R.string.more_logout), NeoColors.OnBaseMuted) { confirmLogout = true }
    )
    val app = listOf(
        // Stats moved into the new Insights bottom-nav tab.
        // MoreRow("📊", "Stats", IconStatsTint) { navController.navigate(Destinations.Stats.route) },
        // App updates now point users to the website where the latest signed
        // APK lives (no in-app update flow for v1.0.0).
        MoreRow("🆙", "Check for updates", IconCallsTint, onClick = openWebsiteForUpdate),
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 0.5.dp),
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
    CallNestTheme {
        MoreScreen(navController = rememberNavController())
    }
}
