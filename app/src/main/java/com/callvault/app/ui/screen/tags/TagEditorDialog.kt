package com.callvault.app.ui.screen.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.data.local.seed.DefaultTagsSeeder
import com.callvault.app.domain.model.Tag
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoChip
import com.callvault.app.ui.components.neo.NeoTextField
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors

/** Maximum length of a tag name (spec §3.6). */
private const val MAX_TAG_NAME = 24

/** Curated business-friendly emoji set for the inline picker. */
private val EmojiPalette: List<String> = listOf(
    "📝", "🤝", "🏬", "👤", "🚫", "⏰", "💰", "🏆",
    "❌", "📞", "📦", "🛒", "💼", "✉️", "🔔", "⭐",
    "🚀", "🎯", "🧾", "📊", "🔥", "✅", "⚠️", "❓",
    "💬", "🛠️", "🌐", "📌", "🏷️", "🧑‍💼", "👋", "🎁"
)

/**
 * Dialog for creating or editing a [Tag].
 *
 * Validation:
 *  - Name is required and trimmed.
 *  - Names longer than [MAX_TAG_NAME] characters are truncated as the user types.
 *
 * Color picker uses the locked 6-color palette from
 * [DefaultTagsSeeder.Palette]. Emoji picker is a small curated grid.
 *
 * @param initial existing tag when editing, or null when creating.
 * @param onSave called with the new/updated [Tag] (id preserved when editing).
 */
@Composable
fun TagEditorDialog(
    initial: Tag?,
    onDismiss: () -> Unit,
    onSave: (Tag) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var color by rememberSaveable {
        mutableStateOf(initial?.colorHex ?: DefaultTagsSeeder.Palette.first())
    }
    var emoji by rememberSaveable { mutableStateOf(initial?.emoji.orEmpty()) }

    val trimmed = name.trim()
    val tooLong = name.length > MAX_TAG_NAME
    val valid = trimmed.isNotEmpty() && !tooLong

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeoColors.Base,
        title = {
            Text(
                text = stringResource(
                    if (initial == null) R.string.tag_editor_new_title
                    else R.string.tag_editor_edit_title
                ),
                color = NeoColors.OnBase,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                NeoTextField(
                    value = name,
                    onChange = { if (it.length <= MAX_TAG_NAME + 4) name = it },
                    label = stringResource(R.string.tag_editor_name_label),
                    placeholder = stringResource(R.string.tag_editor_name_placeholder)
                )
                if (tooLong) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.tag_editor_name_too_long),
                        color = NeoColors.AccentRose,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.tag_editor_color_label),
                    color = NeoColors.OnBaseMuted,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DefaultTagsSeeder.Palette.forEach { hex ->
                        ColorSwatch(
                            hex = hex,
                            selected = hex.equals(color, ignoreCase = true),
                            onClick = { color = hex }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.tag_editor_emoji_label),
                    color = NeoColors.OnBaseMuted,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                EmojiGrid(selected = emoji, onSelect = { emoji = if (emoji == it) "" else it })
            }
        },
        confirmButton = {
            NeoButton(
                text = stringResource(R.string.tag_editor_save),
                onClick = {
                    if (!valid) return@NeoButton
                    val out = (initial ?: Tag(0, "", color, null, false, 0))
                        .copy(
                            name = trimmed,
                            colorHex = color,
                            emoji = emoji.takeIf { it.isNotBlank() }
                        )
                    onSave(out)
                },
                variant = NeoButtonVariant.Primary,
                enabled = valid
            )
        },
        dismissButton = {
            NeoButton(
                text = stringResource(R.string.tag_editor_cancel),
                onClick = onDismiss,
                variant = NeoButtonVariant.Tertiary
            )
        }
    )
}

@Composable
private fun ColorSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    val color = remember(hex) { runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(NeoColors.AccentBlue) }
    Box(
        modifier = Modifier
            .size(if (selected) 36.dp else 32.dp)
            .background(color, CircleShape)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) NeoColors.OnBase else NeoColors.OnBaseSubtle,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun EmojiGrid(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        EmojiPalette.chunked(8).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { e ->
                    NeoChip(
                        text = e,
                        selected = e == selected,
                        onClick = { onSelect(e) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun TagEditorDialogPreview() {
    CallVaultTheme {
        TagEditorDialog(initial = null, onDismiss = {}, onSave = {})
    }
}
