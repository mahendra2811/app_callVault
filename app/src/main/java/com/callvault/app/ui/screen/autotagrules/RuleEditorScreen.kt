package com.callvault.app.ui.screen.autotagrules

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.domain.model.RuleAction
import com.callvault.app.domain.model.RuleCondition
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.components.neo.NeoTextField
import com.callvault.app.ui.components.neo.NeoToggle
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.autotagrules.components.ActionRow
import com.callvault.app.ui.screen.autotagrules.components.ConditionRow
import com.callvault.app.ui.screen.autotagrules.components.LivePreviewBox
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

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
                    color = NeoColors.OnBase,
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
                    onRemove = { viewModel.removeCondition(idx) }
                )
            }
            NeoButton(
                text = stringResource(R.string.rule_editor_add_condition),
                icon = Icons.Filled.Add,
                onClick = {
                    // Default new condition — a prefix match the user can refine.
                    viewModel.addCondition(RuleCondition.PrefixMatches(""))
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Actions
            SectionHeader(title = stringResource(R.string.rule_editor_then))
            state.draft.actions.forEachIndexed { idx, a ->
                ActionRow(
                    action = a,
                    onRemove = { viewModel.removeAction(idx) }
                )
            }
            NeoButton(
                text = stringResource(R.string.rule_editor_add_action),
                icon = Icons.Filled.Add,
                onClick = { viewModel.addAction(RuleAction.LeadScoreBoost(10)) },
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
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = NeoColors.OnBase,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 640)
@Composable
private fun RuleEditorPreview() {
    CallVaultTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NeoSurface(
                modifier = Modifier.fillMaxWidth(),
                elevation = NeoElevation.ConcaveSmall,
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text("Rule editor preview", color = NeoColors.OnBase)
                }
            }
        }
    }
}
