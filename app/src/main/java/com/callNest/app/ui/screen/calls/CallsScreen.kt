package com.callNest.app.ui.screen.calls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.domain.model.FilterState
import com.callNest.app.ui.components.neo.NeoEmptyState
import com.callNest.app.ui.components.neo.NeoIconButton
import com.callNest.app.ui.components.neo.NeoPageHeader
import com.callNest.app.ui.components.neo.NeoTopBar
import com.callNest.app.ui.screen.shared.NeoScaffold
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.util.DateFormatter

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
    onOpenSettings: () -> Unit = {},
    onOpenDocs: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CallsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filterSheetOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    NeoScaffold(
        modifier = modifier,
        // Phase III — page top bar (title + Search/Filter/View-mode actions) hidden.
        // Restore by uncommenting the topBar block below.
        /*
        topBar = {
            NeoTopBar(
                title = androidx.compose.ui.res.stringResource(com.callNest.app.R.string.cv_calls_title),
                actions = {
                    NeoIconButton(
                        icon = Icons.Filled.Search,
                        onClick = onOpenSearch,
                        contentDescription = "Search calls",
                        size = 40.dp
                    )
                    NeoIconButton(
                        icon = Icons.Filled.FilterList,
                        onClick = { filterSheetOpen = !filterSheetOpen },
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
        */
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
        // Phase III — page header (emoji + title + description) hidden. Restore by uncommenting.
        /*
        NeoPageHeader(
            title = androidx.compose.ui.res.stringResource(com.callNest.app.R.string.cv_calls_title),
            description = androidx.compose.ui.res.stringResource(com.callNest.app.R.string.cv_calls_description),
            emoji = "📞"
        )
        */
        // Phase III — inline action subsection: Search · Filter · View-mode toggle.
        // These three actions are Calls-specific so they live on this page (not in More).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoIconButton(
                icon = Icons.Filled.Search,
                onClick = onOpenSearch,
                contentDescription = "Search calls",
                size = 36.dp,
            )
            Spacer(Modifier.width(8.dp))
            NeoIconButton(
                icon = Icons.Filled.FilterList,
                // Toggle the inline filter panel instead of always opening
                // a modal — tap again to collapse.
                onClick = { filterSheetOpen = !filterSheetOpen },
                contentDescription = "Filter calls",
                size = 36.dp,
            )
            Spacer(Modifier.width(8.dp))
            NeoIconButton(
                icon = if (state.viewMode == CallsViewMode.Grouped)
                    Icons.Filled.ViewAgenda else Icons.AutoMirrored.Filled.ViewList,
                onClick = viewModel::toggleViewMode,
                contentDescription = "Toggle view mode",
                size = 36.dp,
            )
        }
        // Quick-filter chip bar — phone-app style. Replaces hunting through
        // the deep filter sheet for the four filters real users actually use.
        QuickFilterChips(
            current = state.filter,
            onApply = viewModel::setFilter
        )
        // Inline horizontal filter panel — opens under the chip strip when
        // the toolbar FilterList button is tapped. Replaces the old modal
        // bottom sheet that sometimes failed to open on Samsung One UI.
        CallsFilterPanel(
            visible = filterSheetOpen,
            initial = state.filter,
            onDismiss = { filterSheetOpen = false },
            onApply = { newFilter ->
                viewModel.setFilter(newFilter)
                filterSheetOpen = false
            }
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
                        // Update banner removed — users now download new
                        // versions from https://callnest.pooniya.com.
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

    // Old modal bottom sheet removed — replaced by inline CallsFilterPanel
    // earlier in this Column (see comment near the QuickFilterChips row).

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

/**
 * Quick-filter chip strip shown above the call list. Each chip toggles one
 * preset slice of [FilterState] — no deep dialog, no hunting through tabs.
 * Tapping a chip a second time clears its filter.
 */
@Composable
private fun QuickFilterChips(
    current: com.callNest.app.domain.model.FilterState,
    onApply: (com.callNest.app.domain.model.FilterState) -> Unit
) {
    val today = remember {
        val zone = java.time.ZoneId.systemDefault()
        val start = java.time.LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = start + 24L * 60 * 60 * 1000 - 1
        start to end
    }
    val isAll = current == com.callNest.app.domain.model.FilterState()
    val isSaved = current.onlySaved
    val isUnsaved = current.onlyUnsaved
    val isMissed = current.callTypes == setOf(3)
    val isFollowUp = current.onlyWithFollowUp
    val isToday = current.dateFrom == today.first && current.dateTo == today.second
    val isHot = (current.minLeadScore ?: 0) >= 70

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickChip("All", isAll) { onApply(com.callNest.app.domain.model.FilterState()) }
        QuickChip("Saved", isSaved) {
            onApply(if (isSaved) com.callNest.app.domain.model.FilterState() else current.copy(onlySaved = true, onlyUnsaved = false))
        }
        QuickChip("Unsaved", isUnsaved) {
            onApply(if (isUnsaved) com.callNest.app.domain.model.FilterState() else current.copy(onlyUnsaved = true, onlySaved = false))
        }
        QuickChip("Missed", isMissed) {
            onApply(if (isMissed) current.copy(callTypes = emptySet()) else current.copy(callTypes = setOf(3)))
        }
        QuickChip("Today", isToday) {
            onApply(if (isToday) current.copy(dateFrom = null, dateTo = null) else current.copy(dateFrom = today.first, dateTo = today.second))
        }
        QuickChip("Follow-ups", isFollowUp) {
            onApply(current.copy(onlyWithFollowUp = !isFollowUp))
        }
        QuickChip("Hot leads", isHot) {
            onApply(current.copy(minLeadScore = if (isHot) null else 70))
        }
    }
}

@Composable
private fun QuickChip(label: String, selected: Boolean, onTap: () -> Unit) {
    val bg = if (selected) com.callNest.app.ui.theme.NeoColors.AccentBlue
             else com.callNest.app.ui.theme.SageColors.SurfaceAlt
    val fg = if (selected) androidx.compose.ui.graphics.Color.White
             else com.callNest.app.ui.theme.SageColors.TextPrimary
    com.callNest.app.ui.components.neo.NeoSurface(
        modifier = Modifier
            .height(34.dp)
            .clickable(onClick = onTap),
        elevation = com.callNest.app.ui.theme.NeoElevation.Flat,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp),
        color = bg
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = label,
                color = fg,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
