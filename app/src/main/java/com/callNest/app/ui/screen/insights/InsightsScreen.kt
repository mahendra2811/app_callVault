package com.callNest.app.ui.screen.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.domain.model.LeadBucket
import com.callNest.app.domain.model.StatsSnapshot
import com.callNest.app.domain.model.TrendPoint
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.screen.shared.NeoScaffold
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.theme.SageColors

/**
 * Insights — a single-screen dashboard with today's snapshot, 7-day trend,
 * top callers, day-of-week distribution, lead-quality breakdown, and a
 * deep-link to the Weekly Digest. Replaces the old 2-card index that just
 * routed to other screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onOpenStats: () -> Unit,
    onOpenWeeklyDigest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    NeoScaffold(modifier = modifier) {
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeaderRow(onRefresh = viewModel::refresh)
            TodaySection(today = state.today, loading = state.loading)
            state.sevenDay?.let { snap ->
                SevenDayTrendCard(snap.dailyVolume)
                LeadQualityCard(snap)
                DayOfWeekCard(snap)
                TopCallersCard(snap)
            }
            if (state.error != null) {
                NeoSurface(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = NeoElevation.Flat,
                    shape = RoundedCornerShape(12.dp),
                    color = NeoColors.AccentAmber.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = state.error ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SageColors.TextPrimary
                    )
                }
            }
            NavCard(
                emoji = "📅",
                title = "Weekly digest",
                body = "AI-summarised highlights from this week's calls.",
                onClick = onOpenWeeklyDigest
            )
            NavCard(
                emoji = "📊",
                title = "Full stats",
                body = "30-day trends, heatmap, top numbers, more sort options.",
                onClick = onOpenStats
            )
            Spacer(Modifier.height(8.dp))
        }
        }
    }
}

@Composable
private fun HeaderRow(onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = SageColors.TextPrimary
            )
            Text(
                text = "How your calls turn into leads.",
                style = MaterialTheme.typography.bodyMedium,
                color = SageColors.TextSecondary
            )
        }
        NeoButton(text = "Refresh", onClick = onRefresh)
    }
}

@Composable
private fun TodaySection(today: TodayMetrics, loading: Boolean) {
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        elevation = NeoElevation.ConvexSmall,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelLarge,
                color = SageColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (loading) "—" else today.totalCalls.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = NeoColors.AccentBlue,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (today.totalCalls == 1) "call" else "calls",
                style = MaterialTheme.typography.bodyMedium,
                color = SageColors.TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile("Missed", today.missed, Modifier.weight(1f))
                MetricTile("Unsaved", today.unsaved, Modifier.weight(1f))
                MetricTile("Follow-ups", today.followUpsDue, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: Int, modifier: Modifier = Modifier) {
    NeoSurface(
        modifier = modifier,
        elevation = NeoElevation.ConcaveSmall,
        shape = RoundedCornerShape(12.dp),
        color = SageColors.SurfaceAlt
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SageColors.TextPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SageColors.TextSecondary
            )
        }
    }
}

@Composable
private fun SevenDayTrendCard(points: List<TrendPoint>) {
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        elevation = NeoElevation.ConvexSmall,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Last 7 days",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SageColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            TrendBars(points)
        }
    }
}

@Composable
private fun TrendBars(points: List<TrendPoint>) {
    if (points.isEmpty()) {
        Text("No calls in the last 7 days.", style = MaterialTheme.typography.bodyMedium, color = SageColors.TextSecondary)
        return
    }
    val maxV = (points.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0)
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        points.takeLast(7).forEach { p ->
            val fraction = (p.value / maxV).toFloat()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(32.dp)
            ) {
                Text(
                    text = p.value.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SageColors.TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((90f * fraction).coerceAtLeast(4f).dp)
                        .background(
                            NeoColors.AccentBlue.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = p.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = SageColors.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun LeadQualityCard(snap: StatsSnapshot) {
    val cold = snap.overview.leadDistribution[LeadBucket.Cold] ?: 0
    val warm = snap.overview.leadDistribution[LeadBucket.Warm] ?: 0
    val hot = snap.overview.leadDistribution[LeadBucket.Hot] ?: 0
    val total = (cold + warm + hot).coerceAtLeast(1)
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        elevation = NeoElevation.ConvexSmall,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Lead quality (7 days)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SageColors.TextPrimary
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                Box(
                    modifier = Modifier
                        .weight((hot.toFloat() / total).coerceAtLeast(0.01f))
                        .fillMaxSize()
                        .background(NeoColors.AccentBlue, shape = RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp))
                )
                Spacer(Modifier.width(1.dp))
                Box(
                    modifier = Modifier
                        .weight((warm.toFloat() / total).coerceAtLeast(0.01f))
                        .fillMaxSize()
                        .background(NeoColors.AccentAmber)
                )
                Spacer(Modifier.width(1.dp))
                Box(
                    modifier = Modifier
                        .weight((cold.toFloat() / total).coerceAtLeast(0.01f))
                        .fillMaxSize()
                        .background(SageColors.TextTertiary, shape = RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp))
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendDot("Hot", hot, NeoColors.AccentBlue)
                LegendDot("Warm", warm, NeoColors.AccentAmber)
                LegendDot("Cold", cold, SageColors.TextTertiary)
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(10.dp).height(10.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label · $value",
            style = MaterialTheme.typography.labelMedium,
            color = SageColors.TextSecondary
        )
    }
}

@Composable
private fun DayOfWeekCard(snap: StatsSnapshot) {
    val byDay = remember(snap) {
        val agg = IntArray(7)
        snap.heatmap.forEach { c ->
            val idx = ((c.dayOfWeek + 5) % 7).coerceIn(0, 6)
            agg[idx] += c.count
        }
        agg
    }
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxV = (byDay.maxOrNull() ?: 0).coerceAtLeast(1)
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        elevation = NeoElevation.ConvexSmall,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Busiest days (7 days)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SageColors.TextPrimary
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                byDay.forEachIndexed { i, v ->
                    val fraction = v.toFloat() / maxV.toFloat()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((90f * fraction).coerceAtLeast(4f).dp)
                                .background(
                                    NeoColors.AccentBlue.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = labels[i],
                            style = MaterialTheme.typography.labelSmall,
                            color = SageColors.TextSecondary
                        )
                        Text(
                            text = v.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = SageColors.TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopCallersCard(snap: StatsSnapshot) {
    if (snap.topByCount.isEmpty()) return
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        elevation = NeoElevation.ConvexSmall,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top callers",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SageColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            snap.topByCount.take(5).forEachIndexed { i, e ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${i + 1}.",
                        modifier = Modifier.width(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SageColors.TextTertiary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = e.displayName ?: e.normalizedNumber,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = SageColors.TextPrimary
                        )
                        if (e.displayName != null) {
                            Text(
                                text = e.normalizedNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = SageColors.TextTertiary
                            )
                        }
                    }
                    Text(
                        text = "${e.callCount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NeoColors.AccentBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun NavCard(emoji: String, title: String, body: String, onClick: () -> Unit) {
    NeoSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = NeoElevation.ConcaveSmall,
        shape = RoundedCornerShape(14.dp),
        color = SageColors.SurfaceAlt
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SageColors.TextPrimary
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = SageColors.TextSecondary
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = SageColors.TextTertiary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun InsightsPreview() {
    CallNestTheme {
        NeoScaffold {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TodaySection(today = TodayMetrics(24, 7, 4, 2), loading = false)
                NavCard("📅", "Weekly digest", "AI summary of this week.", {})
            }
        }
    }
}
