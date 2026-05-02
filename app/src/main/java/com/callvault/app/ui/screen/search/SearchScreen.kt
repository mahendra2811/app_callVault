package com.callvault.app.ui.screen.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.ui.components.neo.NeoEmptyState
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoSearchBar
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.calls.CallRowItem
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.theme.NeoColors

/**
 * Full-screen search overlay.
 *
 * - Empty query → recent searches list with a Clear button.
 * - Non-empty → debounced FTS search (handled by [SearchViewModel]); results
 *   render as the same [CallRowItem] used by the Calls list.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    NeoScaffold(
        modifier = modifier,
        topBar = {
            NeoTopBar(
                title = "Search",
                navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavClick = onBack
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            NeoSearchBar(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                placeholder = "Try a number, name, or note keyword"
            )
            Spacer(modifier = Modifier.height(8.dp))
            when {
                state.query.isBlank() -> RecentList(
                    recent = state.recent,
                    onSelect = viewModel::selectRecent,
                    onClear = viewModel::clearHistory
                )
                state.results.isEmpty() -> NeoEmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "No matches",
                    message = "Try a number, name, or note keyword."
                )
                else -> LazyColumn {
                    items(state.results, key = { it.call.systemId }) { row ->
                        CallRowItem(
                            row = row,
                            onClick = {
                                viewModel.saveToHistory()
                                onOpenDetail(row.call.normalizedNumber)
                            },
                            onLongPress = {},
                            onToggleBookmark = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentList(recent: List<String>, onSelect: (String) -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                "Recent searches",
                color = NeoColors.OnBaseMuted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            if (recent.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
        if (recent.isEmpty()) {
            Text(
                "Your recent searches will appear here.",
                color = NeoColors.OnBaseSubtle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                recent.forEach { q ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(q) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        NeoIconButton(
                            icon = Icons.Filled.History,
                            contentDescription = "Recent search",
                            onClick = { onSelect(q) },
                            size = 32.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(q, color = NeoColors.OnBase)
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private val ArrangementSpacedBy = Arrangement.spacedBy(4.dp)
