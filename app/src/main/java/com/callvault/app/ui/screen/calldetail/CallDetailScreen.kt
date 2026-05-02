package com.callvault.app.ui.screen.calldetail

import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.domain.model.Note
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.bookmarks.BookmarkReasonDialog
import com.callvault.app.ui.screen.calldetail.sections.ActionBar
import com.callvault.app.ui.screen.calldetail.sections.FollowUpDateTimeDialog
import com.callvault.app.ui.screen.calldetail.sections.FollowUpSection
import com.callvault.app.ui.screen.calldetail.sections.HeroCard
import com.callvault.app.ui.screen.calldetail.sections.HistoryTimeline
import com.callvault.app.ui.screen.calldetail.sections.NoteEditorDialog
import com.callvault.app.ui.screen.calldetail.sections.NotesJournal
import com.callvault.app.ui.screen.calldetail.sections.SaveStatus
import com.callvault.app.ui.screen.calldetail.sections.StatsCard
import com.callvault.app.ui.screen.calldetail.sections.TagsSection
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.screen.tags.TagPickerSheet
import com.callvault.app.ui.util.PhoneNumberFormatter
import kotlinx.coroutines.launch

/**
 * Per-number call detail screen.
 *
 * Stitches together hero, action bar, stats, tags, notes, follow-up, and
 * history timeline. Sprint 4 wires the real tag picker, full note editor,
 * follow-up scheduling and the first-bookmark reason prompt.
 */
@Composable
fun CallDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CallDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var tagPickerOpen by remember { mutableStateOf(false) }
    var noteEditorTarget by remember { mutableStateOf<Note?>(null) }
    var noteEditorOpen by remember { mutableStateOf(false) }
    var noteDeleteTarget by remember { mutableStateOf<Note?>(null) }
    var followUpPickerOpen by remember { mutableStateOf(false) }
    var bookmarkReasonOpen by remember { mutableStateOf(false) }

    NeoScaffold(
        modifier = modifier,
        topBar = {
            NeoTopBar(
                title = state.contact?.displayName
                    ?: PhoneNumberFormatter.pretty(state.normalizedNumber),
                navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavClick = onBack,
                actions = {
                    NeoIconButton(
                        icon = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.detail_share),
                        onClick = {
                            val text = buildVCard(
                                name = state.contact?.displayName,
                                number = state.normalizedNumber
                            )
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/x-vcard"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            runCatching { ctx.startActivity(Intent.createChooser(send, "Share")) }
                        },
                        size = 40.dp
                    )
                    NeoIconButton(
                        icon = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.calls_action_more),
                        onClick = { menuOpen = true },
                        size = 40.dp
                    )
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.detail_menu_edit_notes)) },
                            onClick = {
                                menuOpen = false
                                noteEditorTarget = null
                                noteEditorOpen = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.detail_menu_clear_all)) },
                            onClick = { menuOpen = false; confirmClear = true }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.detail_menu_report_spam)) },
                            onClick = { menuOpen = false }
                        )
                    }
                }
            )
        }
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item("hero") {
                val status = when {
                    state.contact?.isInSystemContacts == true -> SaveStatus.Saved
                    state.contact?.isAutoSaved == true -> SaveStatus.AutoSaved
                    else -> SaveStatus.Unsaved
                }
                HeroCard(
                    displayName = state.contact?.displayName,
                    normalizedNumber = state.normalizedNumber,
                    saveStatus = status,
                    leadScore = state.contact?.computedLeadScore ?: 0,
                    onSaveContact = { /* triggers ContactsContract intent in ActionBar */ }
                )
            }
            item("actions") {
                ActionBar(
                    normalizedNumber = state.normalizedNumber,
                    onSaveToContacts = {},
                    onBlock = {}
                )
            }
            item("stats") { StatsCard(stats = state.stats) }
            item("tags") {
                TagsSection(
                    tags = state.tags,
                    onAddTag = { tagPickerOpen = true },
                    onRemoveTag = { tag ->
                        viewModel.applyTags(state.tags.map { it.id }.toSet() - tag.id)
                    }
                )
            }
            item("notes") {
                NotesJournal(
                    notes = state.notes,
                    onAddNote = viewModel::addNote,
                    onEditNote = { note ->
                        noteEditorTarget = note
                        noteEditorOpen = true
                    },
                    onDeleteNote = { note -> noteDeleteTarget = note }
                )
            }
            item("followup") {
                FollowUpSection(
                    followUpAt = state.followUpAt,
                    onSet = { followUpPickerOpen = true },
                    onClear = viewModel::clearFollowUp,
                    onSnooze = { viewModel.snoozeFollowUpHours(24) }
                )
            }
            item("history") { HistoryTimeline(history = state.history) }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (tagPickerOpen) {
        TagPickerSheet(
            allTags = state.allTags,
            currentlyAppliedTagIds = state.tags.map { it.id }.toSet(),
            onDismiss = { tagPickerOpen = false },
            onApply = { ids -> viewModel.applyTags(ids) },
            onCreateTag = { newTag ->
                viewModel.createAndApplyTag(newTag, state.tags.map { it.id }.toSet())
            }
        )
    }

    if (noteEditorOpen) {
        NoteEditorDialog(
            initial = noteEditorTarget,
            onDismiss = { noteEditorOpen = false },
            onSave = { content ->
                val target = noteEditorTarget
                if (target != null) viewModel.editNote(target, content)
                else viewModel.addNote(content)
                noteEditorOpen = false
            }
        )
    }

    val deleteTarget = noteDeleteTarget
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { noteDeleteTarget = null },
            title = { Text(stringResource(R.string.note_delete_confirm_title)) },
            text = { Text(stringResource(R.string.note_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(deleteTarget)
                    noteDeleteTarget = null
                }) { Text(stringResource(R.string.note_delete_confirm_cta)) }
            },
            dismissButton = {
                TextButton(onClick = { noteDeleteTarget = null }) {
                    Text(stringResource(R.string.detail_clear_confirm_dismiss))
                }
            }
        )
    }

    if (followUpPickerOpen) {
        FollowUpDateTimeDialog(
            onDismiss = { followUpPickerOpen = false },
            onPicked = { date, time ->
                followUpPickerOpen = false
                viewModel.setFollowUpAt(date, time, null)
            }
        )
    }

    if (bookmarkReasonOpen) {
        BookmarkReasonDialog(
            onDismiss = { bookmarkReasonOpen = false },
            onSubmit = { reason ->
                bookmarkReasonOpen = false
                viewModel.toggleBookmarkLatest(reason)
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.detail_clear_confirm_title)) },
            text = { Text(stringResource(R.string.detail_clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearAllForThisNumber()
                }) { Text(stringResource(R.string.detail_clear_confirm_cta)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.detail_clear_confirm_dismiss))
                }
            }
        )
    }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) viewModel.consumeError()
    }

    // Trigger the first-bookmark reason prompt from external UI hooks.
    val firstBookmarkTrigger = remember { mutableStateOf(false) }
    LaunchedEffect(firstBookmarkTrigger.value) {
        if (firstBookmarkTrigger.value) {
            firstBookmarkTrigger.value = false
            scope.launch {
                if (viewModel.isFirstBookmarkForNumber()) bookmarkReasonOpen = true
                else viewModel.toggleBookmarkLatest(null)
            }
        }
    }
}

private fun buildVCard(name: String?, number: String): String = buildString {
    append("BEGIN:VCARD\n")
    append("VERSION:3.0\n")
    append("FN:${name ?: number}\n")
    append("TEL;TYPE=CELL:$number\n")
    append("END:VCARD")
}
