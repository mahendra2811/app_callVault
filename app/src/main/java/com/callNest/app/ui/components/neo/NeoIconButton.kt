package com.callNest.app.ui.components.neo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.SageColors

/**
 * Round neumorphic icon button.
 *
 * Used heavily in Call Detail action bars and the top app bar. Tap is
 * indication-free — no ripple, no scale animation — for a flat, calm feel.
 *
 * @param contentDescription accessibility label — required, never null
 */
@Composable
fun NeoIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    NeoSurface(
        modifier = modifier
            .size(size)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = SageColors.Surface
    ) {
        Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) SageColors.TextPrimary else SageColors.TextTertiary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F1EA)
@Composable
private fun NeoIconButtonPreview() {
    CallNestTheme {
        NeoIconButton(
            icon = Icons.Filled.Phone,
            onClick = {},
            contentDescription = "Call back",
            modifier = Modifier.padding(24.dp)
        )
    }
}
