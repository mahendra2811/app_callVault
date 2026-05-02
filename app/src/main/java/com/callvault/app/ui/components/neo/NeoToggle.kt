package com.callvault.app.ui.components.neo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoElevation

/**
 * Neumorphic switch / toggle.
 *
 * The track is concave; the thumb is convex and slides between the two ends
 * with a spring animation matching the global press spec.
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

    NeoSurface(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clickable { onChange(!checked) },
        elevation = NeoElevation.ConcaveSmall,
        shape = RoundedCornerShape(999.dp)
    ) {
        Box(modifier = Modifier.fillMaxHeight().padding(vertical = pad)) {
            NeoSurface(
                modifier = Modifier
                    .offset(x = animOffset)
                    .size(thumbSize),
                elevation = NeoElevation.ConvexSmall,
                shape = CircleShape
            ) { }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoTogglePreview() {
    CallVaultTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            NeoToggle(checked = true, onChange = {})
        }
    }
}
