package com.callNest.app.ui.screen.calldetail.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextFieldValue.Companion
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.R
import com.callNest.app.domain.model.Note
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoButtonVariant
import com.callNest.app.ui.components.neo.NeoChip
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.util.MarkdownText

/**
 * Full-screen note editor dialog.
 *
 * Features:
 *  - Multi-line text field with markdown.
 *  - Toggle between edit and preview using [MarkdownText].
 *  - Five quick-template buttons that splice text in at the current cursor.
 *
 * Saving wires to the host's [onSave]; the host is responsible for writing
 * to [com.callNest.app.domain.repository.NoteRepository] which itself
 * snapshots the previous content into `note_history` (last 5 retained).
 *
 * @param initial existing note when editing, or null when creating.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorDialog(
    initial: Note?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var fieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initial?.content.orEmpty()))
    }
    var preview by rememberSaveable { mutableStateOf(false) }

    val templates = listOf(
        stringResource(R.string.note_template_quoted_price),
        stringResource(R.string.note_template_asked_catalog),
        stringResource(R.string.note_template_followup_next_week),
        stringResource(R.string.note_template_signed_up),
        stringResource(R.string.note_template_not_interested)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SageColors.Canvas,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.note_editor_new_title
                    else R.string.note_editor_edit_title
                ),
                color = SageColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.note_editor_templates_label),
                    color = SageColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    templates.forEach { tpl ->
                        NeoChip(
                            text = tpl,
                            selected = false,
                            onClick = { fieldValue = insertAtCursor(fieldValue, tpl) }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeoChip(
                        text = stringResource(R.string.note_editor_preview_hide),
                        selected = !preview,
                        onClick = { preview = false }
                    )
                    NeoChip(
                        text = stringResource(R.string.note_editor_preview_show),
                        selected = preview,
                        onClick = { preview = true }
                    )
                }
                Spacer(Modifier.height(10.dp))
                NeoSurface(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = NeoElevation.ConcaveSmall,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 160.dp, max = 320.dp)
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (preview) {
                            if (fieldValue.text.isBlank()) {
                                Text(
                                    stringResource(R.string.note_editor_placeholder),
                                    color = SageColors.TextTertiary
                                )
                            } else {
                                MarkdownText(source = fieldValue.text)
                            }
                        } else {
                            BasicTextField(
                                value = fieldValue,
                                onValueChange = { fieldValue = it },
                                cursorBrush = SolidColor(NeoColors.AccentBlue),
                                textStyle = LocalTextStyle.current.copy(color = SageColors.TextPrimary),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (fieldValue.text.isEmpty()) {
                                        Text(
                                            stringResource(R.string.note_editor_placeholder),
                                            color = SageColors.TextTertiary
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            NeoButton(
                text = stringResource(R.string.note_editor_save),
                onClick = {
                    val trimmed = fieldValue.text.trim()
                    if (trimmed.isNotEmpty()) onSave(trimmed)
                },
                variant = NeoButtonVariant.Primary,
                enabled = fieldValue.text.isNotBlank()
            )
        },
        dismissButton = {
            NeoButton(
                text = stringResource(R.string.note_editor_cancel),
                onClick = onDismiss,
                variant = NeoButtonVariant.Tertiary
            )
        }
    )
}

/** Inserts [insert] at the current cursor position, leaving the cursor after the inserted text. */
private fun insertAtCursor(value: TextFieldValue, insert: String): TextFieldValue {
    val pre = value.text.substring(0, value.selection.start)
    val post = value.text.substring(value.selection.end)
    val newText = pre + insert + post
    val newCursor = pre.length + insert.length
    return TextFieldValue(
        text = newText,
        selection = androidx.compose.ui.text.TextRange(newCursor)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun NoteEditorDialogPreview() {
    CallNestTheme {
        // AlertDialog can't render in preview; show a representative panel.
        NeoSurface(modifier = Modifier.padding(24.dp)) {
            Box(modifier = Modifier.padding(16.dp)) {
                Text("Note editor preview", color = SageColors.TextPrimary)
            }
        }
    }
}
