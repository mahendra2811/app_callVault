package com.callvault.app.ui.screen.stats.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callvault.app.domain.model.TrendPoint
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors

/**
 * Pure-Compose line chart for daily call volume.
 *
 * Plots [points] as a solid line and [movingAverage] as a dotted overlay.
 * The Y-axis auto-scales; X-axis labels show the first/middle/last bucket.
 *
 * @param height total chart height (default 180dp)
 */
@Composable
fun DailyVolumeChart(
    points: List<TrendPoint>,
    movingAverage: List<TrendPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp
) {
    val volumeColor = NeoColors.AccentBlue
    val maColor = NeoColors.AccentTeal
    val gridColor = SageColors.TextTertiary
    val labelColor = SageColors.TextSecondary

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            if (points.isEmpty()) return@Canvas
            val maxV = (points.maxOf { it.value }.coerceAtLeast(1.0)).toFloat()
            val padTop = 12f
            val padBottom = 28f
            val padX = 8f
            val w = size.width - padX * 2
            val h = size.height - padTop - padBottom
            val stepX = if (points.size > 1) w / (points.size - 1) else 0f

            // Baseline.
            drawLine(
                color = gridColor.copy(alpha = 0.4f),
                start = Offset(padX, padTop + h),
                end = Offset(padX + w, padTop + h),
                strokeWidth = 1f
            )

            fun mapPoint(i: Int, v: Double): Offset {
                val x = padX + stepX * i
                val y = padTop + h - (v.toFloat() / maxV) * h
                return Offset(x, y)
            }

            val volumePath = Path().apply {
                points.forEachIndexed { i, p ->
                    val o = mapPoint(i, p.value)
                    if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                }
            }
            drawPath(
                path = volumePath,
                color = volumeColor,
                style = Stroke(width = 4f)
            )

            if (movingAverage.size == points.size) {
                val maPath = Path().apply {
                    movingAverage.forEachIndexed { i, p ->
                        val o = mapPoint(i, p.value)
                        if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                    }
                }
                drawPath(
                    path = maPath,
                    color = maColor,
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                )
            }

            // X labels start/mid/end.
            val labels = listOf(0, points.size / 2, points.size - 1)
                .distinct()
                .filter { it in points.indices }
            val nativeCanvas = drawContext.canvas.nativeCanvas
            val paint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = 26f
                isAntiAlias = true
            }
            labels.forEach { i ->
                val o = mapPoint(i, points[i].value)
                val txt = points[i].label
                val tx = (o.x - paint.measureText(txt) / 2).coerceIn(0f, size.width - paint.measureText(txt))
                nativeCanvas.drawText(txt, tx, size.height - 4f, paint)
            }
        }
    }
}

private fun Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360)
@Composable
private fun DailyVolumeChartPreview() {
    val pts = (0 until 14).map {
        TrendPoint(label = "Apr ${it + 1}", value = (3 + it % 6).toDouble(), ts = it.toLong())
    }
    val ma = pts.mapIndexed { i, p ->
        p.copy(value = pts.take(i + 1).map { it.value }.average())
    }
    CallVaultTheme {
        DailyVolumeChart(points = pts, movingAverage = ma)
    }
}
