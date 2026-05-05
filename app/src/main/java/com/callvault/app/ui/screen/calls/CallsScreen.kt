package com.callvault.app.ui.screen.calls

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.domain.model.FilterState
import com.callvault.app.ui.components.neo.NeoEmptyState
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoPageHeader
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.ui.util.DateFormatter

/**
 * Top-level screen for the Calls tab. Wires the [CallsViewModel] state into
 * the layout primitives in this package: pinned section, sticky date headers,
 * pull-to-refresh, optional bulk action bar, and the filter sheet.
 *
 * @param onOpenDetail navigate to call detail for a normalized number
 * @param onOpenSearch navigate to the full-screen search overlay
 * @param onOpenFilterPresets navigate to the filter-presets manager
 * @param onPermissionMissing called when the user must (re-)grant permissions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    onOpenDetail: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFilterPresets: () -> Unit,
    onPermissionMissing: () -> Unit,
    onOpenMyContacts: () -> Unit = {},
    onOpenInquiries: () -> Unit = {},
    onOpenAutoSaveSettings: () -> Unit = {},
    onOpenAutoTagRules: () -> Unit = {},
    onOpenLeadScoringSettings: () -> Unit = {},
    onOpenRealTimeSettings: () -> Unit = {},
    onOpenExport: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenUpdateAvailable: () -> Unit = {},
    onOpenUpdateSettings: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenDocs: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CallsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var filterSheetOpen by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    NeoScaffold(
        modifier = modifier,
        topBar = {
            NeoTopBar(
                title = androidx.compose.ui.res.stringResource(com.callvault.app.R.string.cv_calls_title),
                actions = {
                    NeoIconButton(
                        icon = Icons.Filled.Search,
                        onClick = onOpenSearch,
                        contentDescription = "Search calls",
                        size = 40.dp
                    )
                    NeoIconButton(
                        icon = Icons.Filled.FilterList,
                        onClick = { filterSheetOpen = true },
                        contentDescription = "Filter calls",
                        size = 40.dp
                    )
                    NeoIconButton(
                        icon = if (state.viewMode == CallsViewMode.Grouped)
                            Icons.Filled.ViewAgenda else Icons.AutoMirrored.Filled.ViewList,
                        onClick = viewModel::toggleViewMode,
                        contentDescription = "Toggle view mode",
                        size = 40.dp
                    )
                }
            )
        },
        bottomBar = if (state.bulkMode) {
            {
                BulkActionBar(
                    selectedCount = state.selectedIds.size,
                    allUnsaved = state.selectedIds.all { id ->
                        state.flatList.firstOrNull { it.call.systemId == id }?.isUnsaved == true
                    },
                    onDone = viewModel::bulkClear,
                    onTag = { /* opens placeholder tag picker — Sprint 4 */ },
                    onBookmark = {
                        state.selectedIds.forEach { viewModel.swipeRight(it) }
                        viewModel.bulkClear()
                    },
                    onSave = { /* save-to-contacts intent — Sprint 5 */ },
                    onExport = { /* export pipeline — Sprint 9 */ },
                    onDelete = {
                        state.selectedIds.forEach { viewModel.swipeLeft(it) }
                        viewModel.bulkClear()
                    }
                )
            }
        } else null
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
        NeoPageHeader(
            title = androidx.compose.ui.res.stringResource(com.callvault.app.R.string.cv_calls_title),
            description = androidx.compose.ui.res.stringResource(com.callvault.app.R.string.cv_calls_description),
            emoji = "📞"
        )
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.flatList.isEmpty() && state.pinnedUnsaved.isEmpty() &&
                    state.filter == FilterState() -> {
                    EmptyCalls(onOpenSearch = onOpenSearch)
                }
                state.flatList.isEmpty() && state.filter != FilterState() -> {
                    NeoEmptyState(
                        icon = Icons.Filled.Inbox,
                        title = "No calls match these filters",
                        message = "Try clearing one or more filters to see more results.",
                        action = {
                            NeoIconButton(
                                icon = Icons.Filled.FilterList,
                                onClick = { viewModel.setFilter(FilterState()) },
                                contentDescription = "Clear filters"
                            )
                        }
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(top = 4.dp)
                    ) {
                        val u = updateState
                        if (u is com.callvault.app.domain.repository.UpdateState.Available &&
                            !u.isSkipped && !bannerDismissed) {
                            item("update-banner") {
                                com.callvault.app.ui.screen.calls.components.UpdateBanner(
                                    state = u,
                                    onView = onOpenUpdateAvailable,
                                    onDismiss = { bannerDismissed = true }
                                )
                            }
                        }
                        if (state.pinnedSectionVisible && state.pinnedUnsaved.isNotEmpty()) {
                            item("pinned") {
                                UnsavedPinnedSection(
                                    rows = state.pinnedUnsaved,
                                    onRowClick = { onOpenDetail(it.call.normalizedNumber) },
                                    onLongPressRow = {
                                        viewModel.setBulkMode(true)
                                        viewModel.toggleSelect(it.call.systemId)
                                    },
                                    onToggleBookmark = { viewModel.swipeRight(it.call.systemId) },
                                    onHideSection = viewModel::togglePin
                                )
                            }
                        }
                        item("active-filters") {
                            ActiveFiltersRow(
                                filter = state.filter,
                                onClear = { viewModel.setFilter(FilterState()) },
                                onChange = viewModel::setFilter
                            )
                        }
                        if (state.viewMode == CallsViewMode.Flat) {
                            renderGroupedByDate(state.flatList, state, viewModel, onOpenDetail)
                        } else {
                            items(state.groupedByNumber, key = { it.normalizedNumber }) { row ->
                                CallRowItem(
                                    row = CallRow(
                                        call = row.latestCall,
                                        displayName = row.displayName?.let { "$it · ${row.totalCalls}" }
                                            ?: "${row.normalizedNumber} · ${row.totalCalls}",
                                        isUnsaved = row.isUnsaved,
                                        tags = row.tags,
                                        tagOverflowCount = row.tagOverflowCount
                                    ),
                                    onClick = { onOpenDetail(row.normalizedNumber) },
                                    onLongPress = {
                                        viewModel.setBulkMode(true)
                                        viewModel.toggleSelect(row.latestCall.systemId)
                                    },
                                    onToggleBookmark = {
                                        viewModel.swipeRight(row.latestCall.systemId)
                                    },
                                    selected = row.latestCall.systemId in state.selectedIds
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(if (state.bulkMode) 96.dp else 16.dp)) }
                    }
                }
            }
        }
        }
    }

    if (filterSheetOpen) {
        // The sheet ViewModel-less because all state lives in the screen-level VM.
        // We pass repository through ScreenEntryPoint via Hilt — exposed by VM injection.
        // For Sprint 3, we re-use the VM's filter and apply it here.
        CallsFilterSheetHost(
            initial = state.filter,
            onDismiss = { filterSheetOpen = false },
            onApply = {
                viewModel.setFilter(it)
                filterSheetOpen = false
            }
        )
    }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            // Errors are surfaced via Snackbar host in a future iteration; for
            // now we just consume to avoid sticky banners.
            viewModel.consumeError()
        }
    }

    // onPermissionMissing is reserved for future permission revocation handling.
}

/** Builds sticky date-header sections out of the flat list. */
private fun androidx.compose.foundation.lazy.LazyListScope.renderGroupedByDate(
    rows: List<CallRow>,
    state: CallsUiState,
    viewModel: CallsViewModel,
    onOpenDetail: (String) -> Unit
) {
    val grouped = rows.groupBy { DateFormatter.headerKey(it.call.date) }
        .toSortedMap(compareByDescending { it })
    grouped.forEach { (key, list) ->
        item("hdr-$key") {
            Text(
                text = DateFormatter.headerLabel(list.first().call.date),
                style = MaterialTheme.typography.labelMedium,
                color = SageColors.TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, bottom = 4.dp)
            )
        }
        items(list, key = { it.call.systemId }) { row ->
            CallRowItem(
                row = row,
                onClick = {
                    if (state.bulkMode) viewModel.toggleSelect(row.call.systemId)
                    else onOpenDetail(row.call.normalizedNumber)
                },
                onLongPress = {
                    viewModel.setBulkMode(true)
                    viewModel.toggleSelect(row.call.systemId)
                },
                onToggleBookmark = { viewModel.swipeRight(row.call.systemId) },
                selected = row.call.systemId in state.selectedIds
            )
        }
    }
}

@Composable
private fun EmptyCalls(onOpenSearch: () -> Unit) {
    NeoEmptyState(
        icon = Icons.Filled.Inbox,
        title = "No calls yet",
        message = "Pull down to sync your call log.",
        action = {
            NeoIconButton(
                icon = Icons.Filled.Search,
                onClick = onOpenSearch,
                contentDescription = "Search calls"
            )
        }
    )
}
