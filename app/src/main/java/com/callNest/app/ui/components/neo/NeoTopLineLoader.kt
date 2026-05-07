package com.callNest.app.ui.components.neo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors

/**
 * Indeterminate top-line loader. 3.dp tall full-width sliver with a 30%-width
 * gradient stripe (transparent → [color] → transparent) that traverses
 * left-to-right over 1.2s, repeating. Render directly under the top bar.
 */
@Composable
fun NeoTopLineLoader(
    modifier: Modifier = Modifier,
    color: Color = NeoColors.AccentBlue,
) {
    val transition = rememberInfiniteTransition(label = "neo-topline-loader")
    val fraction by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neo-topline-stripe"
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val stripeWidth = widthPx * 0.3f
        val startX = fraction * widthPx
        val brush = Brush.linearGradient(
            colors = listOf(
                color.copy(alpha = 0f),
                color,
                color.copy(alpha = 0f)
            ),
            start = Offset(startX, 0f),
            end = Offset(startX + stripeWidth, 0f)
        )
        Box(modifier = Modifier.fillMaxSize().background(brush))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoTopLineLoaderPreview() {
    CallNestTheme {
        NeoTopLineLoader()
    }
}
