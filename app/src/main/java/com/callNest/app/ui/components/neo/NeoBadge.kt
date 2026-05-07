package com.callNest.app.ui.components.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors

/**
 * Small numeric badge.
 *
 * Capped display at "99+". Renders nothing when [count] is zero or negative.
 */
@Composable
fun NeoBadge(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = NeoColors.AccentRose
) {
    if (count <= 0) return
    val label = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = NeoColors.Light,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoBadgePreview() {
    CallNestTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            NeoBadge(count = 12)
        }
    }
}
