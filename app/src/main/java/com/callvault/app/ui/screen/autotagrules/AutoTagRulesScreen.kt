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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.domain.model.AutoTagRule
import com.callvault.app.ui.components.neo.NeoEmptyState
import com.callvault.app.ui.components.neo.NeoFAB
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.components.neo.NeoToggle
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.ui.theme.NeoElevation
import kotlinx.datetime.Clock

/**
 * Sprint 6 — Auto-tag rules manager. Lists user-defined rules with an active
 * toggle, reorder buttons, and a live match-count value. Tap a row or the
 * floating "+" to open [com.callvault.app.ui.screen.autotagrules.RuleEditorScreen].
 *
 * @param onBack pops back to the calling screen
 * @param onOpenEditor invoked with `-1L` for new, or the rule's id to edit
 */
@Composable
fun AutoTagRulesScreen(
    onBack: () -> Unit,
    onOpenEditor: (Long) -> Unit,
    viewModel: AutoTagRulesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StandardPage(
        title = stringResource(R.string.cv_rules_title),
        description = stringResource(R.string.cv_rules_description),
        emoji = "🪄",
        onBack = onBack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.rules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NeoEmptyState(
                        icon = Icons.Filled.Inbox,
                        title = stringResource(R.string.auto_tag_rules_empty_title),
                        message = stringResource(R.string.auto_tag_rules_empty_body)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.rules, key = { it.rule.id }) { row ->
                        RuleListItem(
                            row = row,
                            onClick = { onOpenEditor(row.rule.id) },
                            onToggle = { viewModel.setActive(row.rule, it) },
                            onUp = { viewModel.moveUp(row.rule) },
                            onDown = { viewModel.moveDown(row.rule) },
                            onDelete = { viewModel.delete(row.rule) }
                        )
                    }
                }
            }
            NeoFAB(
                icon = Icons.Filled.Add,
                onClick = { onOpenEditor(-1L) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )
        }
    }
}

@Composable
private fun RuleListItem(
    row: RuleRow,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit
) {
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        elevation = NeoElevation.ConvexSmall,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.rule.name.ifBlank { stringResource(R.string.auto_tag_rules_unnamed) },
                        color = SageColors.TextPrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.auto_tag_rules_summary,
                            row.rule.conditions.size,
                            row.rule.actions.size,
                            row.matchCount
                        ),
                        color = SageColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                NeoToggle(checked = row.rule.isActive, onChange = onToggle)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeoIconButton(
                    icon = Icons.Filled.ArrowUpward,
                    onClick = onUp,
                    contentDescription = "Move up",
                    size = 36.dp
                )
                Spacer(Modifier.width(4.dp))
                NeoIconButton(
                    icon = Icons.Filled.ArrowDownward,
                    onClick = onDown,
                    contentDescription = "Move down",
                    size = 36.dp
                )
                Spacer(Modifier.weight(1f))
                NeoIconButton(
                    icon = Icons.Filled.Delete,
                    onClick = onDelete,
                    contentDescription = "Delete rule",
                    size = 36.dp
                )
                Spacer(Modifier.width(4.dp))
                androidx.compose.material3.TextButton(onClick = onClick) {
                    Text(stringResource(R.string.auto_tag_rules_edit))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 640)
@Composable
private fun AutoTagRulesEmptyPreview() {
    CallVaultTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NeoEmptyState(
                icon = Icons.Filled.Inbox,
                title = "No rules yet",
                message = "Tap + to add your first auto-tag rule."
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 640)
@Composable
private fun AutoTagRulesPopulatedPreview() {
    CallVaultTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RuleListItem(
                row = RuleRow(
                    rule = AutoTagRule(
                        id = 1, name = "Tag missed calls",
                        isActive = true, sortOrder = 0,
                        conditions = emptyList(), actions = emptyList(),
                        createdAt = Clock.System.now()
                    ),
                    matchCount = 12
                ),
                onClick = {}, onToggle = {}, onUp = {}, onDown = {}, onDelete = {}
            )
        }
    }
}
