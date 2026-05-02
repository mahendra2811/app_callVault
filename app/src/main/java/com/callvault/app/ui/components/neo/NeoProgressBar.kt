package com.callvault.app.ui.components.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Linear progress bar.
 *
 * The track reads as concave (pressed-in) and the fill as a small convex
 * accent surface, matching the spec's "concave track + convex fill" rule.
 *
 * @param progress fractional progress in `[0f..1f]`
 */
@Composable
fun NeoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    NeoSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp),
        elevation = NeoElevation.ConcaveSmall,
        shape = RoundedCornerShape(999.dp)
    ) {
        Box(modifier = Modifier.padding(2.dp).fillMaxHeight()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .clip(RoundedCornerShape(999.dp))
                    .background(NeoColors.AccentBlue)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoProgressBarPreview() {
    CallVaultTheme {
        NeoProgressBar(progress = 0.6f, modifier = Modifier.padding(24.dp))
    }
}
