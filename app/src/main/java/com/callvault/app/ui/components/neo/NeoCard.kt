package com.callvault.app.ui.components.neo

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.BorderSoft
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors

/**
 * A tappable raised card.
 *
 * Behaves as a passive container when [onClick] is null, otherwise plays the
 * neumorphic press animation: scale to 0.97 and invert the shadow on press,
 * then spring back (stiffness 700, damping 0.8 — spec §3.23).
 *
 * @param border optional 1.dp outline drawn over the neumorphic shadow. When
 *   non-null, the card reads with a soft accent edge — used for primary cards
 *   on Home / Stats / Settings to match the colorful-accents direction.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: Color? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier
    val borderModifier =
        if (border != null) Modifier.border(1.dp, border, shape) else Modifier

    NeoSurface(
        modifier = modifier
            .clip(shape)
            .then(clickableModifier)
            .then(borderModifier),
        shape = shape,
        color = SageColors.Surface
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoCardPreview() {
    CallVaultTheme {
        NeoCard(modifier = Modifier.padding(24.dp), onClick = {}) {
            Text("Tap me")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "with border")
@Composable
private fun NeoCardBorderedPreview() {
    CallVaultTheme {
        NeoCard(
            modifier = Modifier.padding(24.dp),
            onClick = {},
            border = NeoColors.BorderSoft
        ) {
            Text("Bordered card")
        }
    }
}
