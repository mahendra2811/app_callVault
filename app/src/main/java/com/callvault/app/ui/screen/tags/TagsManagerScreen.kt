package com.callvault.app.ui.screen.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.domain.model.Tag
import com.callvault.app.ui.components.neo.NeoBadge
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoEmptyState
import com.callvault.app.ui.components.neo.NeoFAB
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.ui.theme.NeoElevation
import android.widget.Toast

/**
 * Manages the tag library — list every tag, edit / merge / delete, and create
 * new ones. Used from Settings and from the in-flow [TagPickerSheet].
 */
@Composable
fun TagsManagerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagsManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    var editorTarget by remember { mutableStateOf<Tag?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TagRow?>(null) }
    var mergeTarget by remember { mutableStateOf<Tag?>(null) }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        viewModel.consumeError()
    }

    StandardPage(
        title = stringResource(R.string.cv_tags_title),
        description = stringResource(R.string.cv_tags_description),
        emoji = "🏷️",
        onBack = onBack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.rows.isEmpty()) {
                NeoEmptyState(
                    icon = Icons.AutoMirrored.Filled.Label,
                    title = stringResource(R.string.tags_empty_title),
                    message = stringResource(R.string.tags_empty_body),
                    action = {
                        NeoButton(
                            text = stringResource(R.string.tags_fab_new),
                            onClick = { editorTarget = null; editorOpen = true },
                            icon = Icons.Filled.Add,
                            variant = NeoButtonVariant.Primary
                        )
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.rows, key = { it.tag.id }) { row ->
                        TagRowItem(
                            row = row,
                            onClick = {
                                editorTarget = row.tag
                                editorOpen = true
                            },
                            onLongPress = { mergeTarget = row.tag },
                            onDelete = { deleteTarget = row }
                        )
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }

            NeoFAB(
                icon = Icons.Filled.Add,
                onClick = { editorTarget = null; editorOpen = true },
                expanded = stringResource(R.string.tags_fab_new),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }
    }

    if (editorOpen) {
        TagEditorDialog(
            initial = editorTarget,
            onDismiss = { editorOpen = false },
            onSave = { tag ->
                viewModel.upsert(tag)
                editorOpen = false
            }
        )
    }

    val target = deleteTarget
    if (target != null) {
        com.callvault.app.ui.components.neo.NeoDialog(
            onDismissRequest = { deleteTarget = null },
            header = {
                Text(
                    stringResource(R.string.tags_delete_confirm_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            body = {
                Spacer(Modifier.height(com.callvault.app.ui.theme.Spacing.Sm))
                Text(
                    if (target.usageCount > 0)
                        stringResource(R.string.tags_delete_confirm_with_uses, target.usageCount)
                    else
                        stringResource(R.string.tags_delete_confirm_no_uses),
                    color = SageColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            footer = {
                NeoButton(
                    text = stringResource(R.string.tag_editor_cancel),
                    onClick = { deleteTarget = null },
                    variant = NeoButtonVariant.Tertiary
                )
                Spacer(Modifier.width(com.callvault.app.ui.theme.Spacing.Sm))
                NeoButton(
                    text = stringResource(R.string.tags_delete_confirm_cta),
                    onClick = {
                        viewModel.delete(target.tag)
                        deleteTarget = null
                    },
                    variant = NeoButtonVariant.Primary
                )
            }
        )
    }

    val mergeFrom = mergeTarget
    if (mergeFrom != null) {
        MergeIntoDialog(
            source = mergeFrom,
            others = state.rows.map { it.tag }.filter { it.id != mergeFrom.id },
            onDismiss = { mergeTarget = null },
            onPick = { target ->
                viewModel.merge(mergeFrom, target.id)
                mergeTarget = null
            }
        )
    }
}

@Composable
private fun TagRowItem(
    row: TagRow,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val color = remember(row.tag.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(row.tag.colorHex)) }
            .getOrDefault(NeoColors.AccentBlue)
    }
    NeoSurface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        elevation = NeoElevation.ConvexSmall
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
            Spacer(Modifier.width(10.dp))
            if (!row.tag.emoji.isNullOrBlank()) {
                Text(
                    row.tag.emoji,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.tag.name,
                        color = SageColors.TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (row.tag.isSystem) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.tags_system_badge),
                            color = SageColors.TextTertiary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Text(
                    if (row.usageCount == 1)
                        stringResource(R.string.tags_usage_count, row.usageCount)
                    else
                        stringResource(R.string.tags_usage_count_plural, row.usageCount),
                    color = SageColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (row.usageCount > 0) {
                NeoBadge(count = row.usageCount)
                Spacer(Modifier.width(8.dp))
            }
            // Per-row overflow removed: rename = row tap, merge = long-press,
            // delete = swipe gesture (handled by parent list).
        }
    }
}

@Composable
private fun MergeIntoDialog(
    source: Tag,
    others: List<Tag>,
    onDismiss: () -> Unit,
    onPick: (Tag) -> Unit
) {
    com.callvault.app.ui.components.neo.NeoDialog(
        onDismissRequest = onDismiss,
        header = {
            Text(
                stringResource(R.string.tags_merge_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        body = {
            Spacer(Modifier.height(com.callvault.app.ui.theme.Spacing.Sm))
            Text(
                stringResource(R.string.tags_merge_dialog_body, source.name),
                color = SageColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(com.callvault.app.ui.theme.Spacing.Md))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(others, key = { it.id }) { t ->
                    NeoButton(
                        text = "${t.emoji ?: ""} ${t.name}".trim(),
                        onClick = { onPick(t) },
                        variant = NeoButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        footer = {
            NeoButton(
                text = stringResource(R.string.tag_editor_cancel),
                onClick = onDismiss,
                variant = NeoButtonVariant.Tertiary
            )
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun TagsManagerEmptyPreview() {
    CallVaultTheme {
        NeoEmptyState(
            icon = Icons.AutoMirrored.Filled.Label,
            title = "No tags yet",
            message = "Tags help you sort inquiries from customers, vendors, and follow-ups."
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun TagsManagerWithRowsPreview() {
    CallVaultTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TagRowItem(
                row = TagRow(
                    Tag(1, "Inquiry", "#4F7CFF", "📝", true, 0),
                    usageCount = 12
                ),
                onClick = {}, onLongPress = {}, onDelete = {}
            )
            TagRowItem(
                row = TagRow(
                    Tag(2, "Wholesale", "#1FB5A8", "📦", false, 1),
                    usageCount = 3
                ),
                onClick = {}, onLongPress = {}, onDelete = {}
            )
        }
    }
}
