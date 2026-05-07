package com.callNest.app.ui.components.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors

/**
 * Compact lead-score indicator: a colored dot followed by the numeric score.
 *
 * Bucket colours mirror spec §3.13:
 * - 0..33   → muted grey ("cold")
 * - 34..66  → amber ("warm")
 * - 67..100 → red ("hot")
 *
 * @param score expected to be in 0..100; values outside that range are clamped.
 * @param showNumber when false, renders the dot only — useful for dense lists.
 */
@Composable
fun LeadScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
    showNumber: Boolean = true
) {
    val clamped = score.coerceIn(0, 100)
    val color = bucketColor(clamped)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
        if (showNumber) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = clamped.toString(),
                color = NeoColors.OnBaseMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** Map a 0..100 score to its UI bucket colour. */
private fun bucketColor(score: Int): Color = when {
    score >= 67 -> NeoColors.AccentRose
    score >= 34 -> NeoColors.AccentAmber
    else -> NeoColors.OnBaseSubtle
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun LeadScoreBadgeColdPreview() {
    CallNestTheme { LeadScoreBadge(score = 12) }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun LeadScoreBadgeWarmPreview() {
    CallNestTheme { LeadScoreBadge(score = 50) }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun LeadScoreBadgeHotPreview() {
    CallNestTheme { LeadScoreBadge(score = 88) }
}
