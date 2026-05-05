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
import com.callvault.app.domain.model.RuleCondition
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * One read-only summary row for a [RuleCondition] inside the rule editor.
 *
 * A full per-variant input form is intentionally simplified for Sprint 6:
 * each row renders a human-readable description of the condition plus a
 * remove button. Editing is performed by removing and re-adding (the
 * "+ Add condition" picker handles type selection).
 *
 * @param condition the condition to render
 * @param onRemove fires when the trailing X is tapped
 */
@Composable
fun ConditionRow(
    condition: RuleCondition,
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
                text = describe(condition),
                color = SageColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            NeoIconButton(
                icon = Icons.Filled.Close,
                onClick = onRemove,
                contentDescription = "Remove condition",
                size = 32.dp
            )
        }
    }
}

private fun describe(c: RuleCondition): String = when (c) {
    is RuleCondition.PrefixMatches -> "Number starts with \"${c.prefix.ifBlank { "…" }}\""
    is RuleCondition.RegexMatches -> "Number matches /${c.pattern}/"
    is RuleCondition.CountryEquals -> "Country = ${c.iso}"
    is RuleCondition.IsInContacts -> if (c.expected) "Saved in contacts" else "Not in contacts"
    is RuleCondition.CallTypeIn -> "Call type in ${c.types}"
    is RuleCondition.DurationCompare -> "Duration ${c.op.name.lowercase()} ${c.seconds}s"
    is RuleCondition.TimeOfDayBetween ->
        "Time of day between ${c.startMinute / 60}:${"%02d".format(c.startMinute % 60)} – " +
            "${c.endMinute / 60}:${"%02d".format(c.endMinute % 60)}"
    is RuleCondition.DayOfWeekIn -> "Day of week in ${c.days}"
    is RuleCondition.SimSlotEquals -> "SIM slot = ${c.slot}"
    is RuleCondition.TagApplied -> "Tag applied #${c.tagId}"
    is RuleCondition.TagNotApplied -> "Tag NOT applied #${c.tagId}"
    is RuleCondition.GeoContains -> "Location contains \"${c.needle}\""
    is RuleCondition.CallCountGreaterThan -> "Total calls > ${c.count}"
}
