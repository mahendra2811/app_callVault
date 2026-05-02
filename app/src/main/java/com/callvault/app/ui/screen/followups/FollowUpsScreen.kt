package com.callvault.app.ui.screen.followups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoChip
import com.callvault.app.ui.components.neo.NeoEmptyState
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation
import com.callvault.app.ui.util.DateFormatter
import com.callvault.app.ui.util.PhoneNumberFormatter
import android.widget.Toast
import kotlinx.datetime.Instant

/**
 * Follow-ups dashboard with Today / Overdue / Upcoming / Completed tabs.
 */
@Composable
fun FollowUpsScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FollowUpsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        viewModel.consumeError()
    }

    NeoScaffold(
        modifier = modifier,
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.followups_title),
                navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavClick = onBack
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FollowUpTabsRow(
                selected = state.selectedTab,
                onSelect = viewModel::setTab,
                today = state.today.size,
                overdue = state.overdue.size,
                upcoming = state.upcoming.size,
                done = state.done.size
            )
            val rows = when (state.selectedTab) {
                FollowUpTab.TODAY -> state.today
                FollowUpTab.OVERDUE -> state.overdue
                FollowUpTab.UPCOMING -> state.upcoming
                FollowUpTab.DONE -> state.done
            }
            if (rows.isEmpty()) {
                NeoEmptyState(
                    icon = Icons.Filled.AccessTime,
                    title = "",
                    message = when (state.selectedTab) {
                        FollowUpTab.TODAY -> stringResource(R.string.followups_empty_today)
                        FollowUpTab.OVERDUE -> stringResource(R.string.followups_empty_overdue)
                        FollowUpTab.UPCOMING -> stringResource(R.string.followups_empty_upcoming)
                        FollowUpTab.DONE -> stringResource(R.string.followups_empty_done)
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rows, key = { it.call.systemId }) { row ->
                        FollowUpRowItem(
                            row = row,
                            onClick = { onOpenDetail(row.call.normalizedNumber) },
                            onMarkDone = { viewModel.markDone(row.call.systemId) },
                            onCancel = { viewModel.cancel(row.call.systemId) }
                        )
                    }
                    item { Spacer(Modifier.height(48.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FollowUpTabsRow(
    selected: FollowUpTab,
    onSelect: (FollowUpTab) -> Unit,
    today: Int,
    overdue: Int,
    upcoming: Int,
    done: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TabChip(stringResource(R.string.followups_tab_today), today, selected == FollowUpTab.TODAY) {
            onSelect(FollowUpTab.TODAY)
        }
        TabChip(stringResource(R.string.followups_tab_overdue), overdue, selected == FollowUpTab.OVERDUE) {
            onSelect(FollowUpTab.OVERDUE)
        }
        TabChip(stringResource(R.string.followups_tab_upcoming), upcoming, selected == FollowUpTab.UPCOMING) {
            onSelect(FollowUpTab.UPCOMING)
        }
        TabChip(stringResource(R.string.followups_tab_done), done, selected == FollowUpTab.DONE) {
            onSelect(FollowUpTab.DONE)
        }
    }
}

@Composable
private fun TabChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    NeoChip(
        text = if (count > 0) "$label · $count" else label,
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun FollowUpRowItem(
    row: FollowUpRow,
    onClick: () -> Unit,
    onMarkDone: () -> Unit,
    onCancel: () -> Unit
) {
    NeoSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = NeoElevation.ConvexSmall,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = row.displayName ?: PhoneNumberFormatter.pretty(row.call.normalizedNumber),
                color = NeoColors.OnBase,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            val whenText = DateFormatter.longDate(Instant.fromEpochMilliseconds(row.triggerEpochMs))
            val noteText = row.call.followUpNote
            Text(
                text = if (!noteText.isNullOrBlank()) "$whenText · $noteText" else whenText,
                color = NeoColors.OnBaseMuted,
                style = MaterialTheme.typography.labelMedium
            )
            if (!row.isDone) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NeoButton(
                        text = stringResource(R.string.followups_action_done),
                        onClick = onMarkDone,
                        variant = NeoButtonVariant.Primary
                    )
                    NeoButton(
                        text = stringResource(R.string.followups_action_cancel),
                        onClick = onCancel,
                        variant = NeoButtonVariant.Tertiary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun FollowUpsTodayEmptyPreview() {
    CallVaultTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NeoEmptyState(
                icon = Icons.Filled.AccessTime,
                title = "",
                message = "All caught up. No follow-ups due today."
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun FollowUpsOverduePreview() {
    CallVaultTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NeoEmptyState(
                icon = Icons.Filled.AccessTime,
                title = "",
                message = "No overdue follow-ups. Nice work."
            )
        }
    }
}
