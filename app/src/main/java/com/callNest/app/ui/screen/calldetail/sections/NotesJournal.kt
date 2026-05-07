package com.callNest.app.ui.screen.calldetail.sections

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.domain.model.Note
import com.callNest.app.ui.components.neo.NeoIconButton
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.components.neo.NeoTextField
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.util.DateFormatter
import com.callNest.app.ui.util.MarkdownText
import kotlinx.datetime.Instant

/**
 * Newest-first journal of notes plus an inline composer.
 *
 * @param notes already in created-desc order (the repo flow sorts for us)
 * @param onAddNote called with the trimmed content when the user taps send
 * @param onEditNote opens an edit dialog (host owns it)
 * @param onDeleteNote confirms + deletes
 */
@Composable
fun NotesJournal(
    notes: List<Note>,
    onAddNote: (String) -> Unit,
    onEditNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "Notes",
            color = SageColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeoTextField(
                value = draft,
                onChange = { draft = it },
                label = "",
                placeholder = "Add a note (markdown supported)",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            NeoIconButton(
                icon = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Save note",
                onClick = {
                    if (draft.isNotBlank()) {
                        onAddNote(draft)
                        draft = ""
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            notes.forEach { note ->
                NeoSurface(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = NeoElevation.ConvexSmall,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                DateFormatter.rowTime(note.createdAt),
                                color = SageColors.TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f)
                            )
                            NeoIconButton(
                                icon = Icons.Filled.Edit,
                                contentDescription = "Edit note",
                                onClick = { onEditNote(note) },
                                size = 32.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            NeoIconButton(
                                icon = Icons.Filled.Delete,
                                contentDescription = "Delete note",
                                onClick = { onDeleteNote(note) },
                                size = 32.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        MarkdownText(source = note.content)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NotesJournalPreview() {
    CallNestTheme {
        NotesJournal(
            notes = listOf(
                Note(
                    id = 1,
                    callSystemId = null,
                    normalizedNumber = "+91",
                    content = "Asked about **wholesale** rates for *50 units*.\n- Confirmed delivery slot.\n- Send PI tomorrow.",
                    createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis() - 3600_000),
                    updatedAt = Instant.fromEpochMilliseconds(System.currentTimeMillis() - 3600_000)
                )
            ),
            onAddNote = {}, onEditNote = {}, onDeleteNote = {}
        )
    }
}
