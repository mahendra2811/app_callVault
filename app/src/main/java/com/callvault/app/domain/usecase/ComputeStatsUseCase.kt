package com.callvault.app.domain.usecase

import com.callvault.app.data.local.dao.CallDao
import com.callvault.app.data.local.dao.ContactMetaDao
import com.callvault.app.domain.model.CallTypeSlice
import com.callvault.app.domain.model.DateRange
import com.callvault.app.domain.model.HourlyHeatmapCell
import com.callvault.app.domain.model.LeadBucket
import com.callvault.app.domain.model.LeaderboardEntry
import com.callvault.app.domain.model.OverviewMetrics
import com.callvault.app.domain.model.StatsSnapshot
import com.callvault.app.domain.model.TrendPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Computes a [StatsSnapshot] for a [DateRange] by fanning out to several
 * Room aggregates in parallel and reducing the results client-side.
 *
 * Heatmap aggregation runs in Kotlin (not SQL) because Room doesn't ship
 * a portable `strftime`-with-timezone story — the cost is one extra pass
 * over the in-range timestamps which stays cheap at < 100k rows.
 */
class ComputeStatsUseCase @Inject constructor(
    private val callDao: CallDao,
    private val contactMetaDao: ContactMetaDao,
    private val generateInsights: GenerateInsightsUseCase
) {
    /** Run all stats aggregates and bundle them into a [StatsSnapshot]. */
    suspend operator fun invoke(range: DateRange): StatsSnapshot = coroutineScope {
        val totalCallsD = async { callDao.totalCount(range.from, range.to) }
        val totalDurationD = async { callDao.totalDuration(range.from, range.to) }
        val missedD = async { callDao.missedCount(range.from, range.to) }
        val dailyD = async { callDao.dailyCounts(range.from, range.to) }
        val typeD = async { callDao.typeCounts(range.from, range.to) }
        val rawDatesD = async { callDao.rawDates(range.from, range.to) }
        val topD = async { callDao.topByCount(range.from, range.to, 10) }
        val scoresD = async { contactMetaDao.scoresInRange(range.from, range.to) }
        val unsavedCountD = async { contactMetaDao.unsavedCountInRange(range.from, range.to) }

        val totalCalls = totalCallsD.await()
        val totalDuration = totalDurationD.await()
        val missed = missedD.await()
        val unsavedCount = unsavedCountD.await()
        val dailyRows = dailyD.await()
        val typeRows = typeD.await()
        val rawDates = rawDatesD.await()
        val topRows = topD.await()
        val scores = scoresD.await()

        val avg = if (totalCalls > 0) (totalDuration / totalCalls).toInt() else 0
        val missedRate = if (totalCalls > 0) missed.toDouble() / totalCalls else 0.0
        val unsavedRate = if (totalCalls > 0) unsavedCount.toDouble() / totalCalls else 0.0

        val leadDist = mutableMapOf(
            LeadBucket.Cold to 0,
            LeadBucket.Warm to 0,
            LeadBucket.Hot to 0
        )
        scores.forEach { row ->
            val b = when {
                row.score < 34 -> LeadBucket.Cold
                row.score <= 66 -> LeadBucket.Warm
                else -> LeadBucket.Hot
            }
            leadDist[b] = (leadDist[b] ?: 0) + 1
        }

        val labelFmt = SimpleDateFormat("MMM d", Locale.getDefault())
        val dailyVolume = dailyRows.map {
            TrendPoint(labelFmt.format(Date(it.day)), it.count.toDouble(), it.day)
        }
        val ma = movingAverage(dailyVolume.map { it.value }, 7)
        val dailyMA = dailyVolume.mapIndexed { i, p -> p.copy(value = ma[i]) }

        val callTypes = typeRows.map { mapTypeRow(it.type, it.count) }

        val heatmap = buildHeatmap(rawDates)

        val top = topRows.map {
            LeaderboardEntry(
                normalizedNumber = it.normalizedNumber,
                displayName = null,
                callCount = it.callCount,
                totalDurationSec = it.totalDuration
            )
        }

        val overview = OverviewMetrics(
            totalCalls = totalCalls,
            totalTalkTimeSec = totalDuration,
            avgDurationSec = avg,
            missedRate = missedRate,
            unsavedRate = unsavedRate,
            leadDistribution = leadDist
        )

        val snapshotNoInsights = StatsSnapshot(
            range = range,
            overview = overview,
            dailyVolume = dailyVolume,
            dailyMovingAverage = dailyMA,
            callTypes = callTypes,
            heatmap = heatmap,
            topByCount = top,
            insights = emptyList()
        )
        snapshotNoInsights.copy(insights = generateInsights(snapshotNoInsights))
    }

    private fun mapTypeRow(type: Int, count: Int): CallTypeSlice = when (type) {
        1 -> CallTypeSlice("Incoming", count, 0xFF34A853L)
        2 -> CallTypeSlice("Outgoing", count, 0xFF4F7CFFL)
        3 -> CallTypeSlice("Missed", count, 0xFFE5536BL)
        4 -> CallTypeSlice("Voicemail", count, 0xFF8266E5L)
        5 -> CallTypeSlice("Rejected", count, 0xFFE0A82EL)
        6 -> CallTypeSlice("Blocked", count, 0xFF5C6A7AL)
        7 -> CallTypeSlice("External", count, 0xFF1FB5A8L)
        else -> CallTypeSlice("Other", count, 0xFF8492A3L)
    }

    private fun movingAverage(values: List<Double>, window: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        val out = DoubleArray(values.size)
        var sum = 0.0
        for (i in values.indices) {
            sum += values[i]
            if (i >= window) sum -= values[i - window]
            val n = (i + 1).coerceAtMost(window)
            out[i] = sum / n
        }
        return out.toList()
    }

    private fun buildHeatmap(dates: List<Long>): List<HourlyHeatmapCell> {
        val grid = Array(7) { IntArray(24) }
        val cal = Calendar.getInstance()
        for (ts in dates) {
            cal.timeInMillis = ts
            // Calendar.DAY_OF_WEEK: SUN=1..SAT=7 → normalize to MON=0..SUN=6.
            val dow = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            grid[dow][hour]++
        }
        val cells = mutableListOf<HourlyHeatmapCell>()
        for (d in 0..6) for (h in 0..23) {
            cells += HourlyHeatmapCell(d, h, grid[d][h])
        }
        return cells
    }
}
