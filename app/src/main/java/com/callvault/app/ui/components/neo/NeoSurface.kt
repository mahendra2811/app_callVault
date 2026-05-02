package com.callvault.app.ui.components.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Base neumorphic building block.
 *
 * Paints a tinted surface, applies [neoShadow] for the requested [elevation],
 * and clips its [content] to [shape]. Every other Neo* component composes
 * itself on top of [NeoSurface].
 *
 * @param elevation Convex / Concave / Flat depth token
 * @param shape rounded outline used for both clipping and shadow rendering
 * @param color base tint — defaults to [NeoColors.Base]
 */
@Composable
fun NeoSurface(
    modifier: Modifier = Modifier,
    elevation: NeoElevation = NeoElevation.ConvexMedium,
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = NeoColors.Base,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .neoShadow(elevation = elevation, shape = shape)
            .clip(shape)
            .background(color)
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoSurfacePreview() {
    CallVaultTheme {
        NeoSurface(
            modifier = Modifier.padding(24.dp),
            elevation = NeoElevation.ConvexMedium
        ) {
            Box(modifier = Modifier.padding(32.dp))
        }
    }
}
