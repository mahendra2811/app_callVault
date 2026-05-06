package com.callvault.app.ui.screen.autotagrules.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.callvault.app.domain.model.RuleAction
import com.callvault.app.domain.model.Tag
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.ui.theme.NeoElevation

/** Inline-editable card for one [RuleAction]. */
@Composable
fun ActionRow(
    action: RuleAction,
    tags: List<Tag>,
    onUpdate: (RuleAction) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeoSurface(
        modifier = modifier.fillMaxWidth(),
        elevation = NeoElevation.ConcaveSmall,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label(action),
                    color = SageColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                NeoIconButton(
                    icon = Icons.Filled.Close,
                    onClick = onRemove,
                    contentDescription = "Remove action",
                    size = 32.dp
                )
            }
            Spacer(Modifier.height(8.dp))
            ActionEditor(action, tags, onUpdate)
        }
    }
}

private fun label(a: RuleAction): String = when (a) {
    is RuleAction.ApplyTag -> "Apply tag"
    is RuleAction.LeadScoreBoost -> "Adjust lead score"
    is RuleAction.AutoBookmark -> "Auto-bookmark"
    is RuleAction.MarkFollowUp -> "Schedule follow-up"
}

@Composable
private fun ActionEditor(a: RuleAction, tags: List<Tag>, onUpdate: (RuleAction) -> Unit) {
    when (a) {
        is RuleAction.ApplyTag -> TagDropdown(tags, a.tagId) { onUpdate(RuleAction.ApplyTag(it)) }
        is RuleAction.LeadScoreBoost -> InlineTextField(
            value = a.delta.toString(),
            onChange = { onUpdate(RuleAction.LeadScoreBoost(it.toIntOrNull() ?: 0)) },
            placeholder = "+10 / -5",
            keyboardType = KeyboardType.Number,
        )
        is RuleAction.AutoBookmark -> InlineTextField(
            value = a.reason.orEmpty(),
            onChange = { onUpdate(RuleAction.AutoBookmark(it.ifBlank { null })) },
            placeholder = "Reason (optional)",
        )
        is RuleAction.MarkFollowUp -> InlineTextField(
            value = a.hoursFromNow.toString(),
            onChange = { onUpdate(RuleAction.MarkFollowUp(it.toIntOrNull() ?: 0)) },
            placeholder = "hours from now",
            keyboardType = KeyboardType.Number,
        )
    }
}
