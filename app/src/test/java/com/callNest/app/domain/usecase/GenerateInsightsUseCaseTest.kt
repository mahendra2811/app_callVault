package com.callNest.app.domain.usecase

import com.callNest.app.domain.model.HeatmapCell
import com.callNest.app.domain.model.Insight
import com.callNest.app.domain.model.LeadBucket
import com.callNest.app.domain.model.OverviewMetrics
import com.callNest.app.domain.model.Severity
import com.callNest.app.domain.model.StatsSnapshot
import com.callNest.app.domain.model.TopNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GenerateInsightsUseCaseTest {

    private val useCase = GenerateInsightsUseCase()

    private fun snapshot(
        missedRate: Double = 0.0,
        unsavedRate: Double = 0.0,
        totalCalls: Int = 100,
        leadDistribution: Map<LeadBucket, Int> = emptyMap(),
        heatmap: List<HeatmapCell> = emptyList(),
        topByCount: List<TopNumber> = emptyList()
    ) = StatsSnapshot(
        overview = OverviewMetrics(
            totalCalls = totalCalls,
            totalDurationSec = 0L,
            missedRate = missedRate,
            unsavedRate = unsavedRate,
            leadDistribution = leadDistribution
        ),
        daily = emptyList(),
        typeBreakdown = emptyList(),
        heatmap = heatmap,
        topByCount = topByCount
    )

    @Test fun highMissedRate_emitsMissedInsight() {
        val out = useCase(snapshot(missedRate = 0.5))
        assertTrue(out.any { it.message.contains("missed", ignoreCase = true) })
    }

    @Test fun highUnsavedRate_emitsUnsavedInsight() {
        val out = useCase(snapshot(unsavedRate = 0.5))
        assertTrue(out.any { it.message.contains("unsaved", ignoreCase = true) })
    }

    @Test fun hotLeads_emitsCriticalInsight() {
        val out = useCase(snapshot(leadDistribution = mapOf(LeadBucket.Hot to 5)))
        val hot = out.firstOrNull { it.message.contains("hot leads", ignoreCase = true) }
        assertTrue(hot != null && hot.severity == Severity.Critical)
    }

    @Test fun peakHour_emittedFromHeatmap() {
        val cells = listOf(
            HeatmapCell(dayOfWeek = 1, hour = 14, count = 9),
            HeatmapCell(dayOfWeek = 2, hour = 9, count = 1)
        )
        val out = useCase(snapshot(heatmap = cells))
        assertTrue(out.any { it.message.contains("14:00") })
    }

    @Test fun topNumber_emitted() {
        val out = useCase(snapshot(topByCount = listOf(TopNumber("+919876543210", "Alice", 12, 0L))))
        assertTrue(out.any { it.message.contains("Alice") })
    }

    @Test fun max5Returned() {
        val out = useCase(snapshot(
            missedRate = 0.5,
            unsavedRate = 0.5,
            leadDistribution = mapOf(LeadBucket.Hot to 5),
            heatmap = listOf(HeatmapCell(1, 10, 9)),
            topByCount = listOf(TopNumber("+1", "n", 9, 0L))
        ))
        assertTrue(out.size <= 5)
    }
}
