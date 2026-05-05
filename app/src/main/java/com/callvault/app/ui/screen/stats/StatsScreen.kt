package com.callvault.app.ui.screen.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.domain.model.CallTypeSlice
import com.callvault.app.domain.model.DateRange
import com.callvault.app.domain.model.HourlyHeatmapCell
import com.callvault.app.domain.model.Insight
import com.callvault.app.domain.model.LeadBucket
import com.callvault.app.domain.model.LeaderboardEntry
import com.callvault.app.domain.model.OverviewMetrics
import com.callvault.app.domain.model.Severity
import com.callvault.app.domain.model.StatsSnapshot
import com.callvault.app.domain.model.TrendPoint
import com.callvault.app.ui.components.neo.NeoBottomSheet
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoChip
import com.callvault.app.ui.components.neo.NeoEmptyState
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoProgressBar
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.screen.stats.charts.DailyVolumeChart
import com.callvault.app.ui.screen.stats.charts.HourlyHeatmap
import com.callvault.app.ui.screen.stats.charts.TopNumbersList
import com.callvault.app.ui.screen.stats.charts.TypeDonut
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.ui.util.DurationFormatter
import kotlin.math.roundToInt

/**
 * Stats dashboard screen. Renders an overview row, lead distribution,
 * insights, four chart sections, and an Export PDF button (Sprint 9 stub).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }
    val snackHost = remember { SnackbarHostState() }

    LaunchedEffect(state.exportToast) {
        state.exportToast?.let {
            snackHost.showSnackbar(it)
            viewModel.consumeExportToast()
        }
    }

    StandardPage(
        title = stringResource(R.string.cv_stats_title),
        description = stringResource(R.string.cv_stats_description),
        emoji = "📊",
        onBack = onBack,
        loading = state.loading,
        backgroundColor = com.callvault.app.ui.theme.TabBgStats,
        headerGradient = com.callvault.app.ui.theme.HeaderGradStatsStart to com.callvault.app.ui.theme.HeaderGradStatsEnd,
        actions = {
            NeoIconButton(
                icon = Icons.Filled.FilterList,
                onClick = { sheetOpen = true },
                contentDescription = stringResource(R.string.stats_change_range),
                size = 40.dp
            )
            NeoIconButton(
                icon = Icons.Filled.Refresh,
                onClick = viewModel::refresh,
                contentDescription = stringResource(R.string.stats_refresh),
                size = 40.dp
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading && state.snapshot == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NeoProgressBar(progress = 0.6f, modifier = Modifier.fillMaxWidth())
                    }
                }
                state.error != null && state.snapshot == null -> {
                    NeoEmptyState(
                        icon = Icons.Filled.BarChart,
                        title = stringResource(R.string.stats_error_title),
                        message = state.error ?: stringResource(R.string.stats_error_body),
                        action = {
                            Button(onClick = viewModel::refresh) {
                                Text(stringResource(R.string.stats_retry))
                            }
                        }
                    )
                }
                else -> {
                    state.snapshot?.let { snap ->
                        StatsBody(
                            snapshot = snap,
                            sortByDuration = state.sortByDuration,
                            onToggleSort = viewModel::toggleSortByDuration,
                            onExport = viewModel::exportPdf
                        )
                    }
                }
            }
            SnackbarHost(
                hostState = snackHost,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }
    }

    if (sheetOpen) {
        DateRangeSheet(
            currentPreset = state.presetIndex,
            onDismiss = { sheetOpen = false },
            onPreset = { idx ->
                viewModel.setPreset(idx)
                sheetOpen = false
            },
            onCustom = { from, to ->
                viewModel.setCustomRange(from, to)
                sheetOpen = false
            }
        )
    }
}

/** Main scrollable body — assumes a non-null [snapshot]. */
@Composable
private fun StatsBody(
    snapshot: StatsSnapshot,
    sortByDuration: Boolean,
    onToggleSort: () -> Unit,
    onExport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { OverviewRow(snapshot.overview) }
        item { LeadDistributionBar(snapshot.overview.leadDistribution) }
        if (snapshot.insights.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.stats_section_insights)) }
            items(snapshot.insights)
        }
        item { SectionTitle(stringResource(R.string.stats_chart_daily_volume)) }
        item {
            DailyVolumeChart(
                points = snapshot.dailyVolume,
                movingAverage = snapshot.dailyMovingAverage
            )
        }
        item { SectionTitle(stringResource(R.string.stats_chart_call_types)) }
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                TypeDonut(slices = snapshot.callTypes)
            }
        }
        item { SectionTitle(stringResource(R.string.stats_chart_heatmap)) }
        item { HourlyHeatmap(cells = snapshot.heatmap) }
        item { SectionTitle(stringResource(R.string.stats_chart_top_numbers)) }
        item {
            TopNumbersList(
                entries = snapshot.topByCount,
                sortByDuration = sortByDuration,
                onToggleSort = onToggleSort
            )
        }
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.stats_export_pdf))
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun LazyColumnItemsScopeShim() = Unit

private fun androidx.compose.foundation.lazy.LazyListScope.items(insights: List<Insight>) {
    items(insights.size) { i -> InsightCard(insights[i]) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = SageColors.TextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun OverviewRow(o: OverviewMetrics) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverviewCard(
            label = stringResource(R.string.stats_overview_total_calls),
            value = o.totalCalls.toString()
        )
        OverviewCard(
            label = stringResource(R.string.stats_overview_talk_time),
            value = DurationFormatter.short(o.totalTalkTimeSec.toInt())
        )
        OverviewCard(
            label = stringResource(R.string.stats_overview_avg_duration),
            value = DurationFormatter.short(o.avgDurationSec)
        )
        OverviewCard(
            label = stringResource(R.string.stats_overview_missed),
            value = "${(o.missedRate * 100).roundToInt()}%"
        )
    }
}

@Composable
private fun OverviewCard(label: String, value: String) {
    NeoCard(modifier = Modifier.width(140.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SageColors.TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = SageColors.TextPrimary
            )
        }
    }
}

@Composable
private fun LeadDistributionBar(dist: Map<LeadBucket, Int>) {
    val cold = dist[LeadBucket.Cold] ?: 0
    val warm = dist[LeadBucket.Warm] ?: 0
    val hot = dist[LeadBucket.Hot] ?: 0
    val total = (cold + warm + hot).coerceAtLeast(1)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.stats_section_leads),
            style = MaterialTheme.typography.titleSmall,
            color = SageColors.TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(999.dp))
        ) {
            if (cold > 0) Box(
                Modifier.weight(cold.toFloat() / total).fillMaxWidth().background(NeoColors.AccentBlue)
            )
            if (warm > 0) Box(
                Modifier.weight(warm.toFloat() / total).fillMaxWidth().background(NeoColors.AccentAmber)
            )
            if (hot > 0) Box(
                Modifier.weight(hot.toFloat() / total).fillMaxWidth().background(NeoColors.AccentRose)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(NeoColors.AccentBlue, "Cold $cold")
            LegendDot(NeoColors.AccentAmber, "Warm $warm")
            LegendDot(NeoColors.AccentRose, "Hot $hot")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = SageColors.TextSecondary)
    }
}

@Composable
private fun InsightCard(insight: Insight) {
    val border = when (insight.severity) {
        Severity.Info -> NeoColors.AccentBlue
        Severity.Warn -> NeoColors.AccentAmber
        Severity.Critical -> NeoColors.AccentRose
    }
    NeoCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxWidth()
                    .background(border)
            )
            Text(
                text = insight.text,
                color = SageColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/** Modal sheet with preset chips + a custom-range option using M3 DatePickerDialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeSheet(
    currentPreset: Int,
    onDismiss: () -> Unit,
    onPreset: (Int) -> Unit,
    onCustom: (Long, Long) -> Unit
) {
    var pickFromOpen by remember { mutableStateOf(false) }
    var pickToOpen by remember { mutableStateOf(false) }
    var fromMs by remember { mutableStateOf<Long?>(null) }
    val labels = listOf(
        stringResource(R.string.stats_preset_today),
        stringResource(R.string.stats_preset_7d),
        stringResource(R.string.stats_preset_30d),
        stringResource(R.string.stats_preset_this_month),
        stringResource(R.string.stats_preset_last_month),
        stringResource(R.string.stats_preset_90d)
    )
    NeoBottomSheet(onDismiss = onDismiss) {
        Column {
            Text(
                stringResource(R.string.stats_change_range),
                style = MaterialTheme.typography.titleMedium,
                color = SageColors.TextPrimary
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                labels.forEachIndexed { i, l ->
                    NeoChip(text = l, selected = currentPreset == i, onClick = { onPreset(i) })
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { pickFromOpen = true }) {
                Text(stringResource(R.string.stats_preset_custom))
            }
        }
    }
    if (pickFromOpen) {
        val s = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickFromOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    fromMs = s.selectedDateMillis
                    pickFromOpen = false
                    if (fromMs != null) pickToOpen = true
                }) { Text("OK") }
            }
        ) { DatePicker(state = s) }
    }
    if (pickToOpen) {
        val s = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickToOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val to = s.selectedDateMillis
                    val from = fromMs
                    pickToOpen = false
                    if (from != null && to != null && to >= from) onCustom(from, to)
                }) { Text("OK") }
            }
        ) { DatePicker(state = s) }
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

private fun sampleSnapshot(): StatsSnapshot {
    val days = (0 until 14).map {
        TrendPoint("Apr ${it + 1}", (3 + it % 6).toDouble(), it.toLong())
    }
    val ma = days.mapIndexed { i, p ->
        p.copy(value = days.take(i + 1).map { it.value }.average())
    }
    val types = listOf(
        CallTypeSlice("Incoming", 42, 0xFF34A853L),
        CallTypeSlice("Outgoing", 31, 0xFF4F7CFFL),
        CallTypeSlice("Missed", 12, 0xFFE5536BL)
    )
    val heat = buildList {
        for (d in 0..6) for (h in 0..23) add(HourlyHeatmapCell(d, h, (d + h) % 8))
    }
    val tops = listOf(
        LeaderboardEntry("+919876500001", "Alice", 24, 3600),
        LeaderboardEntry("+919876500002", "Bob", 19, 2200)
    )
    val insights = listOf(
        Insight("You missed 22% of calls in this range — a lot of inquiries are slipping through.", Severity.Warn),
        Insight("Peak inquiry hour: 11:00–12:00.", Severity.Info)
    )
    return StatsSnapshot(
        range = DateRange.last30Days(),
        overview = OverviewMetrics(
            totalCalls = 85,
            totalTalkTimeSec = 7200,
            avgDurationSec = 84,
            missedRate = 0.22,
            unsavedRate = 0.31,
            leadDistribution = mapOf(
                LeadBucket.Cold to 30,
                LeadBucket.Warm to 12,
                LeadBucket.Hot to 4
            )
        ),
        dailyVolume = days,
        dailyMovingAverage = ma,
        callTypes = types,
        heatmap = heat,
        topByCount = tops,
        insights = insights
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 1200)
@Composable
private fun StatsScreenPreview() {
    CallVaultTheme {
        StatsBody(
            snapshot = sampleSnapshot(),
            sortByDuration = false,
            onToggleSort = {},
            onExport = {}
        )
    }
}
