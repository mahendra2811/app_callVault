package com.callvault.app.ui.components.neo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Visual variants supported by [NeoButton].
 *
 * - [Primary] — accent-tinted label and icon, raised convex card.
 * - [Secondary] — neutral label, raised convex card (default).
 * - [Tertiary] — flat surface, used for low-emphasis actions in dense rows.
 */
enum class NeoButtonVariant { Primary, Secondary, Tertiary }

/**
 * Standard neumorphic action button.
 *
 * On press it scales to 0.97 and inverts to a concave surface. Both halves of
 * the animation share a single spring (stiffness 700, damping 0.8) per spec.
 */
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    variant: NeoButtonVariant = NeoButtonVariant.Secondary,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = 700f, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "neo-button-scale"
    )
    val elevation: NeoElevation = when {
        !enabled -> NeoElevation.Flat
        variant == NeoButtonVariant.Tertiary -> NeoElevation.Flat
        pressed -> NeoElevation.ConcaveSmall
        else -> NeoElevation.ConvexMedium
    }
    val labelColor: Color = when (variant) {
        NeoButtonVariant.Primary -> NeoColors.AccentBlue
        NeoButtonVariant.Secondary -> NeoColors.OnBase
        NeoButtonVariant.Tertiary -> NeoColors.OnBaseMuted
    }

    NeoSurface(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        elevation = elevation,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = labelColor)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) labelColor else NeoColors.OnBaseSubtle,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoButtonPreview() {
    CallVaultTheme {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeoButton(text = "Save", onClick = {}, variant = NeoButtonVariant.Primary)
            NeoButton(text = "Cancel", onClick = {}, variant = NeoButtonVariant.Secondary)
            NeoButton(text = "Skip", onClick = {}, variant = NeoButtonVariant.Tertiary)
        }
    }
}
