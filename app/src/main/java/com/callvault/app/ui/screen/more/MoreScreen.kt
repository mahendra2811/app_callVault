package com.callvault.app.ui.screen.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.navigation.Destinations
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors

/**
 * Sprint 11 — More tab.
 *
 * Single column of secondary surfaces accessible from the bottom nav. Mirrors
 * the overflow on the Calls top-bar but presented as a discoverable list.
 */
@Composable
fun MoreScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "Auto-tag rules" to Destinations.AutoTagRules.route,
        "Lead scoring" to Destinations.LeadScoringSettings.route,
        "Real-time" to Destinations.RealTimeSettings.route,
        "Auto-save" to Destinations.AutoSaveSettings.route,
        "Export" to Destinations.Export.route,
        "Backup & restore" to Destinations.Backup.route,
        "Help & docs" to Destinations.DocsList.route,
        "App updates" to Destinations.UpdateSettings.route,
        "Settings" to Destinations.Settings.route
    )
    NeoScaffold(
        modifier = modifier,
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.more_title),
                navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavClick = { navController.popBackStack() }
            )
        }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.first }) { (label, route) ->
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate(route) }
                ) {
                    Text(label, color = NeoColors.OnBase, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun MorePreview() { CallVaultTheme { MoreScreen(navController = rememberNavController()) } }
