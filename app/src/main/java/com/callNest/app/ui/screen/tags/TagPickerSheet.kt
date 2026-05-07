package com.callNest.app.ui.screen.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.R
import com.callNest.app.domain.model.Tag
import com.callNest.app.ui.components.neo.NeoBottomSheet
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoButtonVariant
import com.callNest.app.ui.components.neo.NeoChip
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors

/**
 * Modal bottom sheet that lets the user toggle which tags are applied to a
 * call (or batch of calls).
 *
 * Used from [com.callNest.app.ui.screen.calldetail.sections.TagsSection] to
 * replace the Sprint 3 stub, and from the bulk-action bar on the Calls
 * screen. Hosts an inline [TagEditorDialog] when "Create new tag" is tapped.
 *
 * @param allTags every tag in the database — drives the togglable chip grid
 * @param currentlyAppliedTagIds set of tag ids already applied (rendered as selected)
 * @param onApply emitted with the final selected tag-id set when the user taps Apply
 * @param onCreateTag emitted when the user saves a brand-new tag from the inline editor
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPickerSheet(
    allTags: List<Tag>,
    currentlyAppliedTagIds: Set<Long>,
    onDismiss: () -> Unit,
    onApply: (Set<Long>) -> Unit,
    onCreateTag: (Tag) -> Unit
) {
    var selected by rememberSaveable(currentlyAppliedTagIds) {
        mutableStateOf(currentlyAppliedTagIds)
    }
    var editorOpen by remember { mutableStateOf(false) }

    NeoBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.tag_picker_title),
                color = SageColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            if (allTags.isEmpty()) {
                Text(
                    text = stringResource(R.string.tag_picker_empty),
                    color = SageColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allTags.forEach { tag ->
                        val on = tag.id in selected
                        NeoChip(
                            text = "${tag.emoji ?: ""} ${tag.name}".trim(),
                            selected = on,
                            onClick = {
                                selected = if (on) selected - tag.id else selected + tag.id
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoButton(
                    text = stringResource(R.string.tag_picker_create_new),
                    onClick = { editorOpen = true },
                    icon = Icons.Filled.Add,
                    variant = NeoButtonVariant.Tertiary
                )
                Spacer(Modifier.weight(1f))
                NeoButton(
                    text = stringResource(R.string.tag_picker_apply),
                    onClick = { onApply(selected); onDismiss() },
                    variant = NeoButtonVariant.Primary
                )
            }
        }
    }

    if (editorOpen) {
        TagEditorDialog(
            initial = null,
            onDismiss = { editorOpen = false },
            onSave = { newTag ->
                editorOpen = false
                onCreateTag(newTag)
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 600)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPickerSheetPreview() {
    CallNestTheme {
        // Bottom sheets cannot be previewed directly; render a simulated panel.
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Apply tags",
                color = SageColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Customer", "Vendor", "Spam", "Quoted").forEach {
                    NeoChip(text = it, selected = it == "Customer", onClick = {})
                }
            }
        }
    }
}
