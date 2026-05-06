package com.callvault.app.ui.screen.autotagrules.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.callvault.app.domain.model.RuleCondition
import com.callvault.app.domain.model.RuleCondition.CompareOp
import com.callvault.app.domain.model.Tag
import com.callvault.app.ui.components.neo.NeoChip
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Inline-editable card for one [RuleCondition]. Renders the type label and a
 * type-specific input form so the user can both pick a value and remove the
 * condition without leaving the editor.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConditionRow(
    condition: RuleCondition,
    tags: List<Tag>,
    onUpdate: (RuleCondition) -> Unit,
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
                    text = label(condition),
                    color = SageColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                NeoIconButton(
                    icon = Icons.Filled.Close,
                    onClick = onRemove,
                    contentDescription = "Remove condition",
                    size = 32.dp
                )
            }
            Spacer(Modifier.height(8.dp))
            ConditionEditor(condition, tags, onUpdate)
        }
    }
}

private fun label(c: RuleCondition): String = when (c) {
    is RuleCondition.PrefixMatches -> "Number starts with"
    is RuleCondition.RegexMatches -> "Number matches regex"
    is RuleCondition.CountryEquals -> "Country code"
    is RuleCondition.IsInContacts -> "In contacts"
    is RuleCondition.CallTypeIn -> "Call type"
    is RuleCondition.DurationCompare -> "Duration"
    is RuleCondition.TimeOfDayBetween -> "Time of day"
    is RuleCondition.DayOfWeekIn -> "Day of week"
    is RuleCondition.SimSlotEquals -> "SIM slot"
    is RuleCondition.TagApplied -> "Has tag"
    is RuleCondition.TagNotApplied -> "Missing tag"
    is RuleCondition.GeoContains -> "Location contains"
    is RuleCondition.CallCountGreaterThan -> "Total calls greater than"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionEditor(c: RuleCondition, tags: List<Tag>, onUpdate: (RuleCondition) -> Unit) {
    when (c) {
        is RuleCondition.PrefixMatches -> InlineTextField(
            value = c.prefix,
            onChange = { onUpdate(RuleCondition.PrefixMatches(it)) },
            placeholder = "+91 / 080 / 1800",
        )
        is RuleCondition.RegexMatches -> InlineTextField(
            value = c.pattern,
            onChange = { onUpdate(RuleCondition.RegexMatches(it)) },
            placeholder = "^\\+91[0-9]{10}$",
        )
        is RuleCondition.CountryEquals -> InlineTextField(
            value = c.iso,
            onChange = { onUpdate(RuleCondition.CountryEquals(it.uppercase())) },
            placeholder = "IN, US, GB…",
        )
        is RuleCondition.IsInContacts -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoChip(text = "Saved", selected = c.expected, onClick = { onUpdate(RuleCondition.IsInContacts(true)) })
                NeoChip(text = "Not saved", selected = !c.expected, onClick = { onUpdate(RuleCondition.IsInContacts(false)) })
            }
        }
        is RuleCondition.CallTypeIn -> {
            // CallLog.Calls types: 1 incoming, 2 outgoing, 3 missed, 5 rejected, 6 blocked
            val labels = listOf(1 to "Incoming", 2 to "Outgoing", 3 to "Missed", 5 to "Rejected", 6 to "Blocked")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEach { (id, name) ->
                    val on = id in c.types
                    NeoChip(text = name, selected = on, onClick = {
                        val next = c.types.toMutableSet().apply { if (on) remove(id) else add(id) }
                        onUpdate(RuleCondition.CallTypeIn(next))
                    })
                }
            }
        }
        is RuleCondition.DurationCompare -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompareOpDropdown(c.op) { onUpdate(c.copy(op = it)) }
                Spacer(Modifier.width(8.dp))
                InlineTextField(
                    value = c.seconds.toString(),
                    onChange = { onUpdate(c.copy(seconds = it.toIntOrNull() ?: 0)) },
                    placeholder = "seconds",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        is RuleCondition.TimeOfDayBetween -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InlineTextField(
                    value = formatHm(c.startMinute),
                    onChange = { onUpdate(c.copy(startMinute = parseHm(it) ?: c.startMinute)) },
                    placeholder = "09:00",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text("→", color = SageColors.TextSecondary)
                Spacer(Modifier.width(8.dp))
                InlineTextField(
                    value = formatHm(c.endMinute),
                    onChange = { onUpdate(c.copy(endMinute = parseHm(it) ?: c.endMinute)) },
                    placeholder = "18:00",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        is RuleCondition.DayOfWeekIn -> {
            val days = listOf(1 to "Mo", 2 to "Tu", 3 to "We", 4 to "Th", 5 to "Fr", 6 to "Sa", 7 to "Su")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEach { (id, name) ->
                    val on = id in c.days
                    NeoChip(text = name, selected = on, onClick = {
                        val next = c.days.toMutableSet().apply { if (on) remove(id) else add(id) }
                        onUpdate(RuleCondition.DayOfWeekIn(next))
                    })
                }
            }
        }
        is RuleCondition.SimSlotEquals -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoChip(text = "SIM 1", selected = c.slot == 0, onClick = { onUpdate(RuleCondition.SimSlotEquals(0)) })
                NeoChip(text = "SIM 2", selected = c.slot == 1, onClick = { onUpdate(RuleCondition.SimSlotEquals(1)) })
            }
        }
        is RuleCondition.TagApplied -> TagDropdown(tags, c.tagId) { onUpdate(RuleCondition.TagApplied(it)) }
        is RuleCondition.TagNotApplied -> TagDropdown(tags, c.tagId) { onUpdate(RuleCondition.TagNotApplied(it)) }
        is RuleCondition.GeoContains -> InlineTextField(
            value = c.needle,
            onChange = { onUpdate(RuleCondition.GeoContains(it)) },
            placeholder = "Mumbai, Delhi, Bengaluru…",
        )
        is RuleCondition.CallCountGreaterThan -> InlineTextField(
            value = c.count.toString(),
            onChange = { onUpdate(RuleCondition.CallCountGreaterThan(it.toIntOrNull() ?: 0)) },
            placeholder = "count",
            keyboardType = KeyboardType.Number,
        )
    }
}

@Composable
private fun CompareOpDropdown(op: CompareOp, onSelect: (CompareOp) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(symbolFor(op)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            CompareOp.entries.forEach { v ->
                DropdownMenuItem(
                    text = { Text("${symbolFor(v)}  ${v.name}") },
                    onClick = { onSelect(v); open = false }
                )
            }
        }
    }
}

private fun symbolFor(op: CompareOp): String = when (op) {
    CompareOp.LT -> "<"
    CompareOp.LTE -> "≤"
    CompareOp.EQ -> "="
    CompareOp.GTE -> "≥"
    CompareOp.GT -> ">"
}

private fun formatHm(min: Int): String = "%02d:%02d".format(min / 60, min % 60)

private fun parseHm(s: String): Int? {
    val parts = s.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

@Composable
internal fun TagDropdown(
    tags: List<Tag>,
    selectedId: Long,
    onSelect: (Long) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    val current = tags.firstOrNull { it.id == selectedId }
    val display = current?.let { "${it.emoji ?: "🏷️"} ${it.name}" }
        ?: if (tags.isEmpty()) "No tags yet — create one in Tags" else "Pick a tag"
    Box {
        NeoSurface(
            modifier = Modifier.fillMaxWidth().padding(0.dp),
            elevation = NeoElevation.ConcaveSmall,
            shape = RoundedCornerShape(10.dp)
        ) {
            TextButton(
                onClick = { if (tags.isNotEmpty()) open = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    display,
                    color = if (current != null) NeoColors.OnBase else NeoColors.OnBaseSubtle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            tags.forEach { tag ->
                DropdownMenuItem(
                    text = { Text("${tag.emoji ?: "🏷️"} ${tag.name}") },
                    onClick = { onSelect(tag.id); open = false }
                )
            }
        }
    }
}

@Composable
internal fun InlineTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    NeoSurface(
        modifier = modifier.fillMaxWidth(),
        elevation = NeoElevation.ConcaveSmall,
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = NeoColors.OnBase),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            placeholder,
                            color = NeoColors.OnBaseSubtle,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    inner()
                }
            )
        }
    }
}
