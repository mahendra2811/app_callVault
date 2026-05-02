package com.callvault.app.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoBadge
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoChip
import com.callvault.app.ui.screen.home.HomeViewModel.HomeUiState
import com.callvault.app.ui.screen.home.HomeViewModel.RecentUnsavedItem
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.BorderAccent
import com.callvault.app.ui.theme.BorderSoft
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.Spacing

/**
 * Home tab — sparse, scannable landing surface.
 *
 * Three cards only: today's snapshot, recent unsaved inquiries, and quick
 * actions. No carousels, no greetings, no rotating insights.
 */
@Composable
fun HomeScreen(
    onNavigateCalls: () -> Unit,
    onNavigateInquiries: () -> Unit,
    onNavigateStats: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateQuickExport: () -> Unit,
    onNavigateFollowUps: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onNavigateCalls = onNavigateCalls,
        onNavigateInquiries = onNavigateInquiries,
        onNavigateStats = onNavigateStats,
        onNavigateBackup = onNavigateBackup,
        onNavigateQuickExport = onNavigateQuickExport,
        onNavigateFollowUps = onNavigateFollowUps,
        modifier = modifier,
    )
}

/** Stateless body — used by both [HomeScreen] and previews. */
@Suppress("UNUSED_PARAMETER")
@Composable
private fun HomeContent(
    state: HomeUiState,
    onNavigateCalls: () -> Unit,
    onNavigateInquiries: () -> Unit,
    onNavigateStats: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateQuickExport: () -> Unit,
    onNavigateFollowUps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardPage(
        title = stringResource(R.string.cv_home_title),
        description = stringResource(R.string.cv_home_description),
        emoji = "🏠",
    ) {
        TodayCard(
            state = state,
            modifier = modifier.fillMaxWidth(),
        )
        RecentUnsavedCard(
            items = state.recentUnsaved,
            total = state.unsavedTotal,
            onSaveAll = onNavigateInquiries,
            onTapItem = { onNavigateInquiries() },
        )
        QuickActionsCard(
            onCalls = onNavigateCalls,
            onStats = onNavigateStats,
            onBackup = onNavigateBackup,
            onQuickExport = onNavigateQuickExport,
        )
    }
}

@Composable
private fun TodayCard(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    NeoCard(modifier = modifier, border = NeoColors.BorderAccent) {
        Column {
            Text(
                text = stringResource(R.string.cv_home_card_today),
                style = MaterialTheme.typography.titleMedium,
                color = NeoColors.OnBaseMuted,
            )
            Spacer(Modifier.height(Spacing.Md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
            ) {
                StatTile(
                    emoji = "📞",
                    value = state.callsToday,
                    label = stringResource(R.string.cv_home_metric_calls),
                    tint = NeoColors.AccentBlue,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    emoji = "❌",
                    value = state.missedToday,
                    label = stringResource(R.string.cv_home_metric_missed),
                    tint = NeoColors.AccentRose,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    emoji = "📥",
                    value = state.unsavedTotal,
                    label = stringResource(R.string.cv_home_metric_unsaved),
                    tint = NeoColors.AccentViolet,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    emoji = "🔔",
                    value = state.followUpsDue,
                    label = stringResource(R.string.cv_home_metric_followups),
                    tint = NeoColors.AccentTeal,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    emoji: String,
    value: Int,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Spacing.Xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.Xs))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = tint,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NeoColors.OnBaseMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RecentUnsavedCard(
    items: List<RecentUnsavedItem>,
    total: Int,
    onSaveAll: () -> Unit,
    onTapItem: (RecentUnsavedItem) -> Unit,
) {
    NeoCard(modifier = Modifier.fillMaxWidth(), border = NeoColors.BorderSoft) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.cv_home_card_recent_unsaved),
                    style = MaterialTheme.typography.titleMedium,
                    color = NeoColors.OnBase,
                    modifier = Modifier.weight(1f),
                )
                if (total > 0) NeoBadge(count = total)
            }
            Spacer(Modifier.height(Spacing.Md))
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.cv_home_unsaved_empty) + " ✨",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeoColors.OnBaseMuted,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Sm)) {
                    items.forEach { item ->
                        UnsavedRow(item = item, onClick = { onTapItem(item) })
                    }
                }
                Spacer(Modifier.height(Spacing.Md))
                NeoButton(
                    text = stringResource(R.string.cv_home_save_all),
                    onClick = onSaveAll,
                    variant = NeoButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun UnsavedRow(item: RecentUnsavedItem, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Xs),
    ) {
        Text(
            text = item.normalizedNumber,
            style = MaterialTheme.typography.bodyLarge,
            color = NeoColors.OnBase,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = relativeTime(item.lastCallEpochMs),
            style = MaterialTheme.typography.labelSmall,
            color = NeoColors.OnBaseMuted,
        )
        Spacer(Modifier.width(Spacing.Sm))
        NeoButton(
            text = "Open",
            onClick = onClick,
            variant = NeoButtonVariant.Tertiary,
        )
    }
}

private fun relativeTime(epochMs: Long): String {
    val deltaMin = ((System.currentTimeMillis() - epochMs) / 60_000L).coerceAtLeast(0)
    return when {
        deltaMin < 1 -> "just now"
        deltaMin < 60 -> "${deltaMin}m ago"
        deltaMin < 60 * 24 -> "${deltaMin / 60}h ago"
        else -> "${deltaMin / (60 * 24)}d ago"
    }
}

@Composable
private fun QuickActionsCard(
    onCalls: () -> Unit,
    onStats: () -> Unit,
    onBackup: () -> Unit,
    onQuickExport: () -> Unit,
) {
    NeoCard(modifier = Modifier.fillMaxWidth(), border = NeoColors.BorderSoft) {
        Column {
            Text(
                text = stringResource(R.string.cv_home_card_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                color = NeoColors.OnBase,
            )
            Spacer(Modifier.height(Spacing.Md))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                NeoChip(
                    text = "📞 " + stringResource(R.string.cv_home_action_calls),
                    selected = false,
                    onClick = onCalls,
                )
                NeoChip(
                    text = "📊 " + stringResource(R.string.cv_home_action_stats),
                    selected = false,
                    onClick = onStats,
                )
                NeoChip(
                    text = "💾 " + stringResource(R.string.cv_home_action_backup),
                    selected = false,
                    onClick = onBackup,
                )
                NeoChip(
                    text = "📥 " + stringResource(R.string.cv_home_action_quick_export),
                    selected = false,
                    onClick = onQuickExport,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 400, heightDp = 800, name = "PopulatedHome")
@Composable
private fun PopulatedHomePreview() {
    CallVaultTheme {
        HomeContent(
            state = HomeUiState(
                callsToday = 12,
                missedToday = 2,
                unsavedTotal = 5,
                followUpsDue = 1,
                recentUnsaved = listOf(
                    RecentUnsavedItem("+919812345678", System.currentTimeMillis() - 5 * 60_000L),
                    RecentUnsavedItem("+919812345679", System.currentTimeMillis() - 45 * 60_000L),
                    RecentUnsavedItem("+919812345670", System.currentTimeMillis() - 3 * 60 * 60_000L),
                ),
                loading = false,
            ),
            onNavigateCalls = {},
            onNavigateInquiries = {},
            onNavigateStats = {},
            onNavigateBackup = {},
            onNavigateQuickExport = {},
            onNavigateFollowUps = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 400, heightDp = 800, name = "EmptyHome")
@Composable
private fun EmptyHomePreview() {
    CallVaultTheme {
        HomeContent(
            state = HomeUiState(loading = false),
            onNavigateCalls = {},
            onNavigateInquiries = {},
            onNavigateStats = {},
            onNavigateBackup = {},
            onNavigateQuickExport = {},
            onNavigateFollowUps = {},
        )
    }
}
