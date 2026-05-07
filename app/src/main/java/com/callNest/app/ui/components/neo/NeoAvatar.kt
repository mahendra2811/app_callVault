package com.callNest.app.ui.components.neo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation
import kotlin.math.abs

/**
 * Initial-letter avatar in neumorphic style.
 *
 * Picks a deterministic accent tint from [name] when [color] is null so the
 * same person always renders with the same hue.
 */
@Composable
fun NeoAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color? = null
) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val resolved = color ?: pickAccent(name)
    NeoSurface(
        modifier = modifier.size(size),
        elevation = NeoElevation.ConvexSmall,
        shape = CircleShape,
        color = resolved
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = NeoColors.Light,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun pickAccent(name: String): Color {
    val palette = listOf(
        NeoColors.AccentBlue,
        NeoColors.AccentTeal,
        NeoColors.AccentAmber,
        NeoColors.AccentRose,
        NeoColors.AccentViolet,
        NeoColors.AccentGreen
    )
    val idx = abs(name.hashCode()) % palette.size
    return palette[idx]
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoAvatarPreview() {
    CallNestTheme {
        NeoAvatar(name = "Asha Kapoor", modifier = Modifier.padding(24.dp))
    }
}
