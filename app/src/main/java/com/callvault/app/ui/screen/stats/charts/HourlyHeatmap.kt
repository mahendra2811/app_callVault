package com.callvault.app.ui.screen.stats.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.domain.model.HourlyHeatmapCell
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors

/**
 * 24×7 day-of-week × hour heatmap.
 *
 * Cell color is interpolated between the neumorphic base and the accent
 * color in proportion to that cell's count over the global maximum.
 */
@Composable
fun HourlyHeatmap(
    cells: List<HourlyHeatmapCell>,
    modifier: Modifier = Modifier
) {
    val accent = NeoColors.AccentBlue
    val base = NeoColors.BasePressed
    val labelColor = SageColors.TextSecondary
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            if (cells.isEmpty()) return@Canvas
            val max = (cells.maxOf { it.count }.coerceAtLeast(1)).toFloat()
            val labelW = 56f
            val labelH = 24f
            val gridW = size.width - labelW
            val gridH = size.height - labelH
            val cellW = gridW / 24f
            val cellH = gridH / 7f

            // Day labels
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (labelColor.alpha * 255).toInt(),
                    (labelColor.red * 255).toInt(),
                    (labelColor.green * 255).toInt(),
                    (labelColor.blue * 255).toInt()
                )
                textSize = 22f
                isAntiAlias = true
            }
            val nc = drawContext.canvas.nativeCanvas
            days.forEachIndexed { i, d ->
                nc.drawText(d, 4f, i * cellH + cellH * 0.65f, paint)
            }
            // Hour labels (0/6/12/18)
            listOf(0, 6, 12, 18).forEach { h ->
                val x = labelW + h * cellW
                nc.drawText(h.toString(), x, size.height - 4f, paint)
            }

            cells.forEach { c ->
                val intensity = c.count / max
                val color: Color = lerp(base, accent, intensity)
                drawRect(
                    color = color,
                    topLeft = Offset(labelW + c.hour * cellW + 1f, c.dayOfWeek * cellH + 1f),
                    size = Size(cellW - 2f, cellH - 2f)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360)
@Composable
private fun HourlyHeatmapPreview() {
    val cells = buildList {
        for (d in 0..6) for (h in 0..23) {
            add(HourlyHeatmapCell(d, h, ((d + h) % 8)))
        }
    }
    CallVaultTheme { HourlyHeatmap(cells = cells) }
}
