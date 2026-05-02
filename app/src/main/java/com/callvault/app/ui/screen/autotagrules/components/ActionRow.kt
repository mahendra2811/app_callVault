package com.callvault.app.ui.screen.autotagrules.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.callvault.app.domain.model.RuleAction
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Summary row for one [RuleAction]. Mirrors [ConditionRow] visually so the
 * "When all of these are true… / Then…" sections feel symmetrical.
 *
 * @param action the action to render
 * @param onRemove fires when the trailing X is tapped
 */
@Composable
fun ActionRow(
    action: RuleAction,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeoSurface(
        modifier = modifier.fillMaxWidth(),
        elevation = NeoElevation.ConcaveSmall,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = describe(action),
                color = NeoColors.OnBase,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            NeoIconButton(
                icon = Icons.Filled.Close,
                onClick = onRemove,
                contentDescription = "Remove action",
                size = 32.dp
            )
        }
    }
}

private fun describe(a: RuleAction): String = when (a) {
    is RuleAction.ApplyTag -> "Apply tag #${a.tagId}"
    is RuleAction.LeadScoreBoost ->
        if (a.delta >= 0) "Boost lead score +${a.delta}" else "Reduce lead score ${a.delta}"
    is RuleAction.AutoBookmark -> "Auto-bookmark${a.reason?.let { ": $it" } ?: ""}"
    is RuleAction.MarkFollowUp -> "Schedule follow-up in ${a.hoursFromNow}h"
}
