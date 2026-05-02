package com.callvault.app.ui.components.neo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Selectable filter chip in neumorphic style.
 *
 * Selected state reads as concave (pressed-in); unselected reads as a small
 * convex card. Used in filter rows and tag pickers.
 */
@Composable
fun NeoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elevation: NeoElevation =
        if (selected) NeoElevation.ConcaveSmall else NeoElevation.ConvexSmall
    NeoSurface(
        modifier = modifier.clickable(onClick = onClick),
        elevation = elevation,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = if (selected) NeoColors.AccentBlue else NeoColors.OnBase,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoChipPreview() {
    CallVaultTheme {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeoChip(text = "Missed", selected = true, onClick = {})
            NeoChip(text = "Outgoing", selected = false, onClick = {})
        }
    }
}
