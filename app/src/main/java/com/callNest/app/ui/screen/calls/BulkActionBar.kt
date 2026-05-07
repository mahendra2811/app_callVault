package com.callNest.app.ui.screen.calls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.components.neo.NeoIconButton
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation

/**
 * Bottom action bar shown when the Calls screen is in bulk-edit mode.
 *
 * Save shows only when [allUnsaved] is true (no point offering save when
 * everything is already in contacts).
 */
@Composable
fun BulkActionBar(
    selectedCount: Int,
    allUnsaved: Boolean,
    onDone: () -> Unit,
    onTag: () -> Unit,
    onBookmark: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeoSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = NeoElevation.ConvexLarge,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Done",
                color = NeoColors.AccentBlue,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onDone)
                    .padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$selectedCount selected",
                color = NeoColors.OnBase,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NeoIconButton(
                    icon = Icons.AutoMirrored.Filled.Label,
                    onClick = onTag,
                    contentDescription = "Tag selected",
                    size = 40.dp
                )
                NeoIconButton(
                    icon = Icons.Filled.Star,
                    onClick = onBookmark,
                    contentDescription = "Bookmark selected",
                    size = 40.dp
                )
                if (allUnsaved) {
                    NeoIconButton(
                        icon = Icons.Filled.PersonAdd,
                        onClick = onSave,
                        contentDescription = "Save to contacts",
                        size = 40.dp
                    )
                }
                NeoIconButton(
                    icon = Icons.Filled.IosShare,
                    onClick = onExport,
                    contentDescription = "Export selected",
                    size = 40.dp
                )
                NeoIconButton(
                    icon = Icons.Filled.Delete,
                    onClick = onDelete,
                    contentDescription = "Delete selected",
                    size = 40.dp
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun BulkActionBarPreview() {
    CallNestTheme {
        BulkActionBar(
            selectedCount = 3,
            allUnsaved = true,
            onDone = {}, onTag = {}, onBookmark = {}, onSave = {}, onExport = {}, onDelete = {}
        )
    }
}
