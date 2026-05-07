package com.callNest.app.ui.screen.autotagrules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.callNest.app.domain.model.RuleCondition.CompareOp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.domain.model.RuleAction
import com.callNest.app.domain.model.RuleCondition
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoIconButton
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.components.neo.NeoTextField
import com.callNest.app.ui.components.neo.NeoToggle
import com.callNest.app.ui.components.neo.NeoTopBar
import com.callNest.app.ui.screen.autotagrules.components.ActionRow
import com.callNest.app.ui.screen.autotagrules.components.ConditionRow
import com.callNest.app.ui.screen.autotagrules.components.LivePreviewBox
import com.callNest.app.ui.screen.shared.NeoScaffold
import com.callNest.app.ui.screen.shared.StandardPage
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.theme.NeoElevation

/**
 * Sprint 6 — Rule editor: define `name`, conditions (AND-ed), and actions.
 * A live "matches X / 200 recent calls" preview re-runs 400 ms after the
 * user stops editing.
 *
 * @param onBack pops back to the rules list
 */
@Composable
fun RuleEditorScreen(
    onBack: () -> Unit,
    viewModel: RuleEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var showConditionPicker by remember { mutableStateOf(false) }
    var showActionPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    StandardPage(
        title = state.draft.name.ifBlank { stringResource(R.string.cv_rule_editor_title_default) },
        description = stringResource(R.string.cv_rule_editor_description),
        emoji = "⚙️",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeoTextField(
                value = state.draft.name,
                onChange = viewModel::setName,
                label = stringResource(R.string.rule_editor_name_label),
                placeholder = stringResource(R.string.rule_editor_name_placeholder)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.rule_editor_active),
                    color = SageColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                NeoToggle(checked = state.draft.isActive, onChange = viewModel::setActive)
            }

            LivePreviewBox(matchCount = state.previewMatchCount)

            // Conditions
            SectionHeader(title = stringResource(R.string.rule_editor_when_all_true))
            state.draft.conditions.forEachIndexed { idx, c ->
                ConditionRow(
                    condition = c,
                    tags = tags,
                    onUpdate = { viewModel.updateCondition(idx, it) },
                    onRemove = { viewModel.removeCondition(idx) }
                )
            }
            NeoButton(
                text = stringResource(R.string.rule_editor_add_condition),
                icon = Icons.Filled.Add,
                onClick = { showConditionPicker = true },
                modifier = Modifier.fillMaxWidth()
            )

            // Actions
            SectionHeader(title = stringResource(R.string.rule_editor_then))
            state.draft.actions.forEachIndexed { idx, a ->
                ActionRow(
                    action = a,
                    tags = tags,
                    onUpdate = { viewModel.updateAction(idx, it) },
                    onRemove = { viewModel.removeAction(idx) }
                )
            }
            NeoButton(
                text = stringResource(R.string.rule_editor_add_action),
                icon = Icons.Filled.Add,
                onClick = { showActionPicker = true },
                modifier = Modifier.fillMaxWidth()
            )

            state.saveError?.let { err ->
                Text(text = err, color = NeoColors.AccentRose)
            }

            Spacer(Modifier.height(8.dp))

            NeoButton(
                text = stringResource(R.string.rule_editor_save),
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showConditionPicker) {
        TypePickerDialog(
            title = "Add condition",
            options = conditionTypes(),
            onDismiss = { showConditionPicker = false },
            onPick = { make ->
                viewModel.addCondition(make())
                showConditionPicker = false
            }
        )
    }
    if (showActionPicker) {
        TypePickerDialog(
            title = "Add action",
            options = actionTypes(),
            onDismiss = { showActionPicker = false },
            onPick = { make ->
                viewModel.addAction(make())
                showActionPicker = false
            }
        )
    }
}

private fun conditionTypes(): List<Pair<String, () -> RuleCondition>> = listOf(
    "Number starts with" to { RuleCondition.PrefixMatches("") },
    "Number matches regex" to { RuleCondition.RegexMatches("") },
    "Country code" to { RuleCondition.CountryEquals("IN") },
    "In contacts" to { RuleCondition.IsInContacts(true) },
    "Call type" to { RuleCondition.CallTypeIn(emptySet()) },
    "Duration" to { RuleCondition.DurationCompare(CompareOp.GT, 60) },
    "Time of day" to { RuleCondition.TimeOfDayBetween(9 * 60, 18 * 60) },
    "Day of week" to { RuleCondition.DayOfWeekIn(emptySet()) },
    "SIM slot" to { RuleCondition.SimSlotEquals(0) },
    "Has tag" to { RuleCondition.TagApplied(0L) },
    "Missing tag" to { RuleCondition.TagNotApplied(0L) },
    "Location contains" to { RuleCondition.GeoContains("") },
    "Total calls greater than" to { RuleCondition.CallCountGreaterThan(5) },
)

private fun actionTypes(): List<Pair<String, () -> RuleAction>> = listOf(
    "Apply tag" to { RuleAction.ApplyTag(0L) },
    "Boost lead score" to { RuleAction.LeadScoreBoost(10) },
    "Auto-bookmark" to { RuleAction.AutoBookmark(null) },
    "Schedule follow-up" to { RuleAction.MarkFollowUp(24) },
)

@Composable
private fun <T> TypePickerDialog(
    title: String,
    options: List<Pair<String, () -> T>>,
    onDismiss: () -> Unit,
    onPick: (() -> T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (label, factory) ->
                    TextButton(
                        onClick = { onPick(factory) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(label, modifier = Modifier.fillMaxWidth()) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = SageColors.TextPrimary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 640)
@Composable
private fun RuleEditorPreview() {
    CallNestTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NeoSurface(
                modifier = Modifier.fillMaxWidth(),
                elevation = NeoElevation.ConcaveSmall,
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text("Rule editor preview", color = SageColors.TextPrimary)
                }
            }
        }
    }
}
