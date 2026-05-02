package com.callvault.app.ui.components.neo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Linear progress bar.
 *
 * Concave track + convex accent fill (spec §3.23). When [indeterminate] is
 * true, a 30%-width gradient stripe slides across the track in a 1.2s loop.
 *
 * @param progress fractional progress in `[0f..1f]`, ignored when [indeterminate]
 * @param indeterminate when true, animates a sliding gradient instead of [progress]
 */
@Composable
fun NeoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    indeterminate: Boolean = false
) {
    NeoSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp),
        elevation = NeoElevation.ConcaveSmall,
        shape = RoundedCornerShape(999.dp)
    ) {
        Box(modifier = Modifier.padding(2.dp).fillMaxHeight()) {
            if (indeterminate) {
                IndeterminateStripe()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(999.dp))
                        .background(NeoColors.AccentBlue)
                )
            }
        }
    }
}

@Composable
private fun IndeterminateStripe() {
    val transition = rememberInfiniteTransition(label = "neo-progress-indeterminate")
    val fraction by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neo-progress-stripe"
    )

    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
    ) {
        BoxWithLocalSize { widthPx ->
            val stripeWidth = widthPx * 0.3f
            val startX = fraction * widthPx
            val brush = Brush.linearGradient(
                colors = listOf(
                    NeoColors.AccentBlue.copy(alpha = 0f),
                    NeoColors.AccentBlue,
                    NeoColors.AccentBlue.copy(alpha = 0f)
                ),
                start = Offset(startX, 0f),
                end = Offset(startX + stripeWidth, 0f)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(brush)
            )
        }
        // density unused but referenced to avoid unused warning chain
        @Suppress("UNUSED_EXPRESSION") density
    }
}

@Composable
private fun BoxWithLocalSize(content: @Composable (Float) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        content(widthPx)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoProgressBarPreview() {
    CallVaultTheme {
        NeoProgressBar(progress = 0.6f, modifier = Modifier.padding(24.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "indeterminate")
@Composable
private fun NeoProgressBarIndeterminatePreview() {
    CallVaultTheme {
        NeoProgressBar(progress = 0f, indeterminate = true, modifier = Modifier.padding(24.dp))
    }
}
