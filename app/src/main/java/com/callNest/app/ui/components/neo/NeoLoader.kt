package com.callNest.app.ui.components.neo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation

/**
 * Indeterminate circular spinner.
 *
 * Concave outer ring with a rotating 90° accent arc inside; one full rotation
 * per 1200ms. Use as the global loading indicator (replaces Material3
 * CircularProgressIndicator on neumorphic backgrounds).
 *
 * @param size diameter of the loader; defaults to 48.dp.
 */
@Composable
fun NeoLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val transition = rememberInfiniteTransition(label = "neo-loader")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neo-loader-angle"
    )

    NeoSurface(
        modifier = modifier.size(size),
        elevation = NeoElevation.ConcaveSmall,
        shape = CircleShape
    ) {
        Box(modifier = Modifier.size(size).padding(6.dp)) {
            Canvas(modifier = Modifier.size(size).rotate(angle)) {
                drawArc(
                    color = NeoColors.AccentBlue,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoLoaderPreview() {
    CallNestTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            NeoLoader()
        }
    }
}
