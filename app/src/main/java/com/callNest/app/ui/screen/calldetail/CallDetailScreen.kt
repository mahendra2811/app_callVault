package com.callNest.app.ui.screen.calldetail

import android.content.Intent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
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
import com.callNest.app.R
import com.callNest.app.domain.model.Note
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoButtonVariant
import com.callNest.app.ui.components.neo.NeoIconButton
import com.callNest.app.ui.screen.bookmarks.BookmarkReasonDialog
import com.callNest.app.ui.screen.calldetail.sections.ActionBar
import com.callNest.app.ui.screen.calldetail.sections.FollowUpDateTimeDialog
import com.callNest.app.ui.screen.calldetail.sections.FollowUpSection
import com.callNest.app.ui.screen.calldetail.sections.HeroCard
import com.callNest.app.ui.screen.calldetail.sections.HistoryTimeline
import com.callNest.app.ui.screen.calldetail.sections.NoteEditorDialog
import com.callNest.app.ui.screen.calldetail.sections.NotesJournal
import com.callNest.app.ui.screen.calldetail.sections.SaveStatus
import com.callNest.app.ui.screen.calldetail.sections.StatsCard
import com.callNest.app.ui.screen.calldetail.sections.TagsSection
import com.callNest.app.ui.screen.shared.StandardPage
import com.callNest.app.ui.screen.tags.TagPickerSheet
import com.callNest.app.ui.util.PhoneNumberFormatter
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
    var whyScoreOpen by remember { mutableStateOf(false) }
    val breakdown by viewModel.breakdown.collectAsStateWithLifecycle()

    StandardPage(
        title = state.contact?.displayName
            ?: PhoneNumberFormatter.pretty(state.normalizedNumber),
        description = stringResource(R.string.cv_calldetail_description),
        emoji = "👤",
        onBack = onBack,
        backgroundColor = com.callNest.app.ui.theme.TabBgCalls,
        headerGradient = com.callNest.app.ui.theme.HeaderGradCallsStart to com.callNest.app.ui.theme.HeaderGradCallsEnd,
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
                    onSaveContact = { /* triggers ContactsContract intent in ActionBar */ },
                    onLeadScoreClick = {
                        viewModel.loadBreakdown()
                        whyScoreOpen = true
                    },
                    summary = state.summary
                )
            }
            item("actions") {
                ActionBar(
                    normalizedNumber = state.normalizedNumber,
                    displayName = state.contact?.displayName,
                    onSaveToContacts = {}
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
            item("manage") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cv_calldetail_manage_section),
                        style = MaterialTheme.typography.titleMedium
                    )
                    NeoButton(
                        text = stringResource(R.string.detail_menu_edit_notes),
                        onClick = {
                            noteEditorTarget = null
                            noteEditorOpen = true
                        },
                        variant = NeoButtonVariant.Tertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NeoButton(
                        text = stringResource(R.string.detail_menu_clear_all),
                        onClick = { confirmClear = true },
                        variant = NeoButtonVariant.Tertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    NeoButton(
                        text = stringResource(R.string.detail_menu_report_spam),
                        onClick = { /* existing handler placeholder */ },
                        variant = NeoButtonVariant.Tertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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
        com.callNest.app.ui.components.neo.NeoDialog(
            onDismissRequest = { noteDeleteTarget = null },
            header = {
                Text(
                    stringResource(R.string.note_delete_confirm_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            body = {
                androidx.compose.foundation.layout.Spacer(Modifier.height(com.callNest.app.ui.theme.Spacing.Sm))
                Text(stringResource(R.string.note_delete_confirm_body))
            },
            footer = {
                TextButton(onClick = { noteDeleteTarget = null }) {
                    Text(stringResource(R.string.detail_clear_confirm_dismiss))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.width(com.callNest.app.ui.theme.Spacing.Sm))
                com.callNest.app.ui.components.neo.NeoButton(
                    text = stringResource(R.string.note_delete_confirm_cta),
                    onClick = {
                        viewModel.deleteNote(deleteTarget)
                        noteDeleteTarget = null
                    }
                )
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
        com.callNest.app.ui.components.neo.NeoDialog(
            onDismissRequest = { confirmClear = false },
            header = {
                Text(
                    stringResource(R.string.detail_clear_confirm_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            body = {
                androidx.compose.foundation.layout.Spacer(Modifier.height(com.callNest.app.ui.theme.Spacing.Sm))
                Text(stringResource(R.string.detail_clear_confirm_body))
            },
            footer = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.detail_clear_confirm_dismiss))
                }
                androidx.compose.foundation.layout.Spacer(Modifier.width(com.callNest.app.ui.theme.Spacing.Sm))
                com.callNest.app.ui.components.neo.NeoButton(
                    text = stringResource(R.string.detail_clear_confirm_cta),
                    onClick = {
                        confirmClear = false
                        viewModel.clearAllForThisNumber()
                    }
                )
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

    if (whyScoreOpen) {
        breakdown?.let { b ->
            com.callNest.app.ui.screen.calldetail.sections.WhyScoreSheet(
                breakdown = b,
                onDismiss = { whyScoreOpen = false },
            )
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
