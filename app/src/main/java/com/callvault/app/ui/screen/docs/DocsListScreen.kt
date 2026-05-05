package com.callvault.app.ui.screen.docs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoEmptyState
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.navigation.Destinations
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors

/**
 * Sprint 11 — top-level Help & Docs screen listing every article bundled with
 * the app under `assets/docs/`.
 */
@Composable
fun DocsListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    vm: DocsViewModel = hiltViewModel()
) {
    val articles by vm.articles.collectAsStateWithLifecycle()

    StandardPage(
        title = stringResource(R.string.cv_docs_title),
        description = stringResource(R.string.cv_docs_description),
        emoji = "📚",
        onBack = { navController.popBackStack() }
    ) {
        if (articles.isEmpty()) {
            NeoEmptyState(
                icon = Icons.Filled.Description,
                title = "No help articles yet",
                message = "We're packaging the help library. Try again after the next update."
            )
            return@StandardPage
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(articles, key = { it.id }) { meta ->
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        navController.navigate(Destinations.DocsArticle.routeFor(meta.id))
                    }
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = meta.title,
                            color = SageColors.TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (meta.excerpt.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = meta.excerpt,
                                color = SageColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun DocsListPreview() {
    CallVaultTheme {
        DocsListScreen(navController = rememberNavController())
    }
}
