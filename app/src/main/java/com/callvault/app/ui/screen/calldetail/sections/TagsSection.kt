package com.callvault.app.ui.screen.calldetail.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callvault.app.domain.model.Tag
import com.callvault.app.ui.components.neo.NeoChip
import com.callvault.app.ui.theme.NeoColors

/**
 * Renders tags applied to this number plus an "Add tag" affordance that opens
 * a stub Sprint-4 sheet via [onAddTag].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSection(
    tags: List<Tag>,
    onAddTag: () -> Unit,
    onRemoveTag: (Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "Tags",
            color = NeoColors.OnBaseMuted,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tags.forEach { tag ->
                NeoChip(
                    text = "${tag.emoji ?: ""} ${tag.name} ×".trim(),
                    selected = true,
                    onClick = { onRemoveTag(tag) }
                )
            }
            NeoChip(text = "+ Add tag", selected = false, onClick = onAddTag)
        }
    }
}
