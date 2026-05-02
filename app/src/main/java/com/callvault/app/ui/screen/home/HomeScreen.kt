package com.callvault.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * Sprint 11 — Home tab landing.
 *
 * Greeting strip plus quick-link cards. The richer "today's snapshot" charts
 * arrive later — for now we surface stable shortcuts so the user can reach
 * frequently-needed views in one tap.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    NeoScaffold(
        modifier = modifier,
        topBar = { NeoTopBar(title = stringResource(R.string.home_title)) }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "Welcome back",
                        color = NeoColors.OnBase,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.home_today_snapshot),
                        color = NeoColors.OnBaseMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            QuickLinkCard("View calls") { navController.navigate(Destinations.Calls.route) }
            QuickLinkCard("My Contacts") { navController.navigate(Destinations.MyContacts.route) }
            QuickLinkCard("Inquiries") { navController.navigate(Destinations.Inquiries.route) }
            QuickLinkCard("Auto-tag rules") { navController.navigate(Destinations.AutoTagRules.route) }
            QuickLinkCard("Backup & restore") { navController.navigate(Destinations.Backup.route) }
            QuickLinkCard("Help & docs") { navController.navigate(Destinations.DocsList.route) }
        }
    }
}

@Composable
private fun QuickLinkCard(label: String, onClick: () -> Unit) {
    NeoCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(label, color = NeoColors.OnBase, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun HomePreview() { CallVaultTheme { HomeScreen(navController = rememberNavController()) } }
