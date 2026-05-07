package com.callNest.app.ui.components.neo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.theme.BorderSoft
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.theme.ToggleOff
import com.callNest.app.ui.theme.ToggleOn

/**
 * Neumorphic switch / toggle.
 *
 * The 32×56dp track tints between [ToggleOff] (gray) and [ToggleOn] (green)
 * with a 1.dp [NeoColors.BorderSoft] outline; the convex thumb slides between
 * the two ends with a press-spring scale animation.
 */
@Composable
fun NeoToggle(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackWidth = 56.dp
    val trackHeight = 32.dp
    val thumbSize = 24.dp
    val pad = 4.dp
    val targetOffset = if (checked) trackWidth - thumbSize - pad else pad

    val animOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(stiffness = 700f, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "neo-toggle-thumb"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) ToggleOn else ToggleOff,
        animationSpec = spring(stiffness = 700f),
        label = "neo-toggle-track"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val thumbScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 700f, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "neo-toggle-thumb-scale"
    )

    val trackShape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(trackShape)
            .background(trackColor)
            .border(1.dp, NeoColors.BorderSoft, trackShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onChange(!checked) }
    ) {
        Box(modifier = Modifier.fillMaxHeight().padding(vertical = pad)) {
            NeoSurface(
                modifier = Modifier
                    .offset(x = animOffset)
                    .size(thumbSize)
                    .scale(thumbScale),
                elevation = NeoElevation.ConvexSmall,
                shape = CircleShape
            ) { }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "on")
@Composable
private fun NeoToggleOnPreview() {
    CallNestTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            NeoToggle(checked = true, onChange = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "off")
@Composable
private fun NeoToggleOffPreview() {
    CallNestTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            NeoToggle(checked = false, onChange = {})
        }
    }
}
