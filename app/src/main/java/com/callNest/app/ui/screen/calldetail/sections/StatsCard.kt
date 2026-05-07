package com.callNest.app.ui.screen.calldetail.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.screen.calldetail.DetailStats
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.util.DateFormatter
import com.callNest.app.ui.util.DurationFormatter

/** Per-number aggregate stats card. */
@Composable
fun StatsCard(stats: DetailStats, modifier: Modifier = Modifier) {
    NeoSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = NeoElevation.ConvexMedium,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Stats",
                color = SageColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Stat(label = "Calls", value = stats.totalCalls.toString())
                Stat(label = "Total time", value = DurationFormatter.short(stats.totalDurationSec))
                Stat(label = "Avg", value = DurationFormatter.short(stats.avgDurationSec))
                Stat(
                    label = "Missed",
                    value = "${(stats.missedRatio * 100).toInt()}%"
                )
            }
            stats.firstDate?.let { first ->
                Text(
                    text = "First: ${DateFormatter.longDate(first)}",
                    color = SageColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            stats.lastDate?.let { last ->
                Text(
                    text = "Last: ${DateFormatter.longDate(last)}",
                    color = SageColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = SageColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(label, color = SageColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun StatsCardPreview() {
    CallNestTheme {
        StatsCard(
            stats = DetailStats(
                totalCalls = 12,
                totalDurationSec = 4_200,
                firstDate = null,
                lastDate = null,
                avgDurationSec = 350,
                missedRatio = 0.25f
            )
        )
    }
}
