package com.callNest.app.ui.screen.search

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.ui.components.neo.NeoEmptyState
import com.callNest.app.ui.components.neo.NeoIconButton
import com.callNest.app.ui.components.neo.NeoSearchBar
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.screen.calls.CallRowItem
import com.callNest.app.ui.screen.shared.NeoScaffold
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.theme.NeoElevation

/**
 * Full-screen search overlay with the search bar serving as its own top bar.
 *
 * - Empty query → recent searches list with a Clear button.
 * - Non-empty → debounced FTS search via [SearchViewModel]; rows render as
 *   the same [CallRowItem] used by the Calls list.
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
            NeoSurface(
                modifier = Modifier.fillMaxWidth(),
                elevation = NeoElevation.ConvexSmall,
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeoIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NeoSearchBar(
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                        placeholder = "Try a number, name, or note keyword",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            when {
                state.query.isBlank() -> RecentList(
                    recent = state.recent,
                    onSelect = viewModel::selectRecent,
                    onClear = viewModel::clearHistory
                )
                state.results.isEmpty() && state.contactMatches.isEmpty() -> NeoEmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "No matches",
                    message = "Try a name, number, or company."
                )
                else -> LazyColumn {
                    if (state.contactMatches.isNotEmpty()) {
                        item(key = "contacts-header") {
                            SectionHeader("Contacts")
                        }
                        items(state.contactMatches, key = { "${it.displayName}|${it.normalizedNumber}" }) { match ->
                            ContactMatchRow(
                                match = match,
                                onClick = {
                                    viewModel.saveToHistory()
                                    onOpenDetail(match.normalizedNumber)
                                }
                            )
                        }
                    }
                    if (state.results.isNotEmpty()) {
                        item(key = "calls-header") {
                            SectionHeader("Calls")
                        }
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
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        color = SageColors.TextSecondary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp)
    )
}

/** Single OS-contact match row — name + pretty number, tap to open detail. */
@Composable
private fun ContactMatchRow(
    match: com.callNest.app.data.system.ContactsReader.ContactMatch,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.callNest.app.ui.components.neo.NeoAvatar(name = match.displayName, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = match.displayName,
                color = SageColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            Text(
                text = match.normalizedNumber,
                color = SageColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RecentList(recent: List<String>, onSelect: (String) -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Recent searches",
                color = SageColors.TextSecondary,
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
                color = SageColors.TextTertiary,
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeoIconButton(
                            icon = Icons.Filled.History,
                            contentDescription = "Recent search",
                            onClick = { onSelect(q) },
                            size = 32.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(q, color = SageColors.TextPrimary)
                    }
                }
            }
        }
    }
}
