package com.callNest.app.ui.screen.stats.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.domain.model.CallTypeSlice
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors

/**
 * Pure-Canvas donut chart for call-type distribution.
 *
 * NOTE (Sprint 8 deviation): the donut is non-interactive — tapping does not
 * filter the underlying snapshot. Slice click hit-testing lands in v2.
 */
@Composable
fun TypeDonut(
    slices: List<CallTypeSlice>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.count }.coerceAtLeast(1)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                if (slices.isEmpty()) return@Canvas
                var start = -90f
                val stroke = 28f
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                slices.forEach { s ->
                    val sweep = (s.count.toFloat() / total) * 360f
                    drawArc(
                        color = Color(s.color),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = SageColors.TextPrimary
                )
                Text(
                    text = "calls",
                    style = MaterialTheme.typography.bodySmall,
                    color = SageColors.TextSecondary
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            slices.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .height(10.dp)
                            .width(10.dp)
                    ) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(Color(s.color))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${s.label} · ${s.count}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SageColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360)
@Composable
private fun TypeDonutPreview() {
    val slices = listOf(
        CallTypeSlice("Incoming", 42, 0xFF34A853L),
        CallTypeSlice("Outgoing", 31, 0xFF4F7CFFL),
        CallTypeSlice("Missed", 12, 0xFFE5536BL),
        CallTypeSlice("Rejected", 5, 0xFFE0A82EL)
    )
    CallNestTheme { TypeDonut(slices = slices) }
}
