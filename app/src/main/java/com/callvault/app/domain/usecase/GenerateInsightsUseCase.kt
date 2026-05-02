package com.callvault.app.domain.usecase

import com.callvault.app.domain.model.Insight
import com.callvault.app.domain.model.LeadBucket
import com.callvault.app.domain.model.Severity
import com.callvault.app.domain.model.StatsSnapshot
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Pure rule engine that turns a [StatsSnapshot] into up to 5 user-facing
 * recommendations. Stateless and side-effect free — safe to call from a
 * non-suspending context.
 */
class GenerateInsightsUseCase @Inject constructor() {

    /** Apply all enabled heuristics and return the top 5 most relevant insights. */
    operator fun invoke(snapshot: StatsSnapshot): List<Insight> {
        val out = mutableListOf<Insight>()
        val o = snapshot.overview

        if (o.missedRate > 0.2) {
            val pct = (o.missedRate * 100).roundToInt()
            out += Insight(
                "You missed $pct% of calls in this range — a lot of inquiries are slipping through.",
                Severity.Warn
            )
        }
        if (o.unsavedRate > 0.3) {
            val n = (o.unsavedRate * o.totalCalls).roundToInt()
            out += Insight(
                "$n unsaved numbers — bulk save them as inquiries.",
                Severity.Info
            )
        }
        snapshot.heatmap
            .groupBy { it.hour }
            .mapValues { (_, cells) -> cells.sumOf { it.count } }
            .maxByOrNull { it.value }
            ?.takeIf { it.value > 0 }
            ?.let { (hr, _) ->
                val end = (hr + 1) % 24
                out += Insight("Peak inquiry hour: $hr:00–$end:00.", Severity.Info)
            }
        snapshot.topByCount.firstOrNull()?.let { top ->
            val name = top.displayName ?: top.normalizedNumber
            out += Insight(
                "Top number this period: $name — ${top.callCount} calls.",
                Severity.Info
            )
        }
        val hot = o.leadDistribution[LeadBucket.Hot] ?: 0
        if (hot > 0) {
            out += Insight(
                "$hot hot leads in your pipeline — follow up before they go cold.",
                Severity.Critical
            )
        }
        return out.take(5)
    }
}
