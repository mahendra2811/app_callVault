package com.callvault.app.ui.screen.calldetail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.domain.model.Call
import com.callvault.app.domain.model.ContactMeta
import com.callvault.app.domain.model.Note
import com.callvault.app.domain.model.Tag
import com.callvault.app.domain.repository.CallRepository
import com.callvault.app.domain.repository.ContactRepository
import com.callvault.app.domain.repository.NoteRepository
import com.callvault.app.domain.repository.TagRepository
import com.callvault.app.domain.usecase.ScheduleFollowUpUseCase
import com.callvault.app.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Aggregated stats shown on the detail "Stats" card. */
data class DetailStats(
    val totalCalls: Int,
    val totalDurationSec: Int,
    val firstDate: Instant?,
    val lastDate: Instant?,
    val avgDurationSec: Int,
    val missedRatio: Float
)

/** Full UI state for the call detail screen. */
data class CallDetailUiState(
    val normalizedNumber: String,
    val contact: ContactMeta? = null,
    val history: List<Call> = emptyList(),
    val stats: DetailStats = DetailStats(0, 0, null, null, 0, 0f),
    val tags: List<Tag> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val notes: List<Note> = emptyList(),
    val followUpAt: Instant? = null,
    val summary: String? = null,
    val errorMessage: String? = null
)

/**
 * Per-number detail screen view-model.
 *
 * Reads `normalizedNumber` from [SavedStateHandle] (set by Compose Navigation)
 * and combines repository streams into a single [CallDetailUiState]. Tag
 * application, follow-up scheduling and bookmark prompts all flow through
 * here so individual section composables stay stateless.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CallDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val callRepo: CallRepository,
    private val contactRepo: ContactRepository,
    private val noteRepo: NoteRepository,
    private val tagRepo: TagRepository,
    private val scheduleFollowUp: ScheduleFollowUpUseCase
) : ViewModel() {

    val normalizedNumber: String =
        Uri.decode(savedState.get<String>(Destinations.CallDetail.ARG_NUMBER) ?: "")

    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<CallDetailUiState> = combine(
        contactRepo.observeByNumber(normalizedNumber),
        callRepo.observeForNumber(normalizedNumber),
        noteRepo.observeForNumber(normalizedNumber),
        tagRepo.observeForNumber(normalizedNumber),
        tagRepo.observeAll(),
        _error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val contact = values[0] as ContactMeta?
        @Suppress("UNCHECKED_CAST")
        val calls = values[1] as List<Call>
        @Suppress("UNCHECKED_CAST")
        val notes = values[2] as List<Note>
        @Suppress("UNCHECKED_CAST")
        val tagsForNumber = values[3] as List<Tag>
        @Suppress("UNCHECKED_CAST")
        val allTags = values[4] as List<Tag>
        val err = values[5] as String?

        val total = calls.size
        val totalDur = calls.sumOf { it.durationSec }
        val avg = if (total == 0) 0 else totalDur / total
        val missedCount = calls.count {
            it.type == com.callvault.app.domain.model.CallType.MISSED ||
                it.type == com.callvault.app.domain.model.CallType.REJECTED
        }
        val ratio = if (total == 0) 0f else missedCount.toFloat() / total
        CallDetailUiState(
            normalizedNumber = normalizedNumber,
            contact = contact,
            history = calls,
            stats = DetailStats(
                totalCalls = total,
                totalDurationSec = totalDur,
                firstDate = calls.minByOrNull { it.date.toEpochMilliseconds() }?.date,
                lastDate = calls.maxByOrNull { it.date.toEpochMilliseconds() }?.date,
                avgDurationSec = avg,
                missedRatio = ratio
            ),
            tags = tagsForNumber,
            allTags = allTags,
            notes = notes,
            followUpAt = calls.firstOrNull { it.followUpAt != null && it.followUpDoneAt == null }
                ?.followUpAt,
            summary = buildSummary(total, tagsForNumber, notes),
            errorMessage = err
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CallDetailUiState(normalizedNumber = normalizedNumber)
    )

    /** Persist a fresh note tied to the latest call for this number. */
    fun addNote(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val latestCallId = state.value.history.firstOrNull()?.systemId
                noteRepo.upsert(
                    Note(
                        id = 0,
                        callSystemId = latestCallId,
                        normalizedNumber = normalizedNumber,
                        content = content.trim(),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                )
            }.onFailure { _error.value = "We couldn't save that note. Please try again." }
        }
    }

    fun editNote(note: Note, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch {
            runCatching {
                noteRepo.upsert(note.copy(content = newContent.trim(), updatedAt = Clock.System.now()))
            }.onFailure { _error.value = "We couldn't update that note." }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            runCatching { noteRepo.delete(note) }
                .onFailure { _error.value = "We couldn't delete that note." }
        }
    }

    /** Apply [tagIds] to every call associated with this number. */
    fun applyTags(tagIds: Set<Long>) {
        viewModelScope.launch {
            runCatching { tagRepo.setTagsForNumber(normalizedNumber, tagIds) }
                .onFailure { _error.value = "We couldn't update tags." }
        }
    }

    /** Persist a brand-new tag and immediately apply it to this number alongside [existing]. */
    fun createAndApplyTag(newTag: Tag, existing: Set<Long>) {
        viewModelScope.launch {
            runCatching {
                val newId = tagRepo.upsert(newTag)
                tagRepo.setTagsForNumber(normalizedNumber, existing + newId)
            }.onFailure { _error.value = "We couldn't save that tag." }
        }
    }

    /** Schedule a follow-up reminder against the latest call for this number. */
    fun setFollowUpAt(date: LocalDate, time: LocalTime?, note: String?) {
        viewModelScope.launch {
            val latest = state.value.history.firstOrNull() ?: return@launch
            runCatching {
                scheduleFollowUp(
                    callSystemId = latest.systemId,
                    normalizedNumber = normalizedNumber,
                    date = date,
                    time = time,
                    note = note
                )
            }.onFailure { _error.value = "Couldn't schedule reminder. Allow exact alarms?" }
        }
    }

    fun clearFollowUp() {
        viewModelScope.launch {
            val target = state.value.history.firstOrNull { it.followUpAt != null && it.followUpDoneAt == null }
                ?: return@launch
            runCatching { scheduleFollowUp.cancel(target.systemId) }
                .onFailure { _error.value = "We couldn't clear the follow-up." }
        }
    }

    fun snoozeFollowUpHours(hours: Long) {
        viewModelScope.launch {
            val target = state.value.history.firstOrNull { it.followUpAt != null && it.followUpDoneAt == null }
                ?: return@launch
            val newTrigger = System.currentTimeMillis() + hours * 60L * 60 * 1000
            runCatching {
                callRepo.setFollowUp(
                    callId = target.systemId,
                    triggerMs = newTrigger,
                    minuteOfDay = null,
                    note = target.followUpNote
                )
            }.onFailure { _error.value = "We couldn't snooze the follow-up." }
        }
    }

    fun toggleBookmarkLatest(reason: String? = null) {
        viewModelScope.launch {
            val latest = state.value.history.firstOrNull() ?: return@launch
            runCatching { callRepo.toggleBookmark(latest.systemId, reason) }
                .onFailure { _error.value = "We couldn't update the bookmark." }
        }
    }

    /** True if no other call for this number is currently bookmarked — used by the
     *  Sprint-4 "first bookmark" reason prompt. */
    suspend fun isFirstBookmarkForNumber(): Boolean {
        val current = callRepo.observeForNumber(normalizedNumber).first()
        return current.none { it.isBookmarked }
    }

    fun clearAllForThisNumber() {
        viewModelScope.launch {
            runCatching {
                callRepo.deleteForNumber(normalizedNumber)
                noteRepo.deleteForNumber(normalizedNumber)
                tagRepo.removeAllAppliedBy("user")
            }.onFailure { _error.value = "We couldn't clear data for this number." }
        }
    }

    fun consumeError() { _error.value = null }
}

private fun buildSummary(total: Int, tagsForNumber: List<Tag>, notes: List<Note>): String? {
    if (total == 0) return null
    val parts = mutableListOf<String>()
    parts += if (total == 1) "1 call" else "$total calls"
    val topTag = tagsForNumber
        .groupingBy { it.name }
        .eachCount()
        .entries
        .maxByOrNull { it.value }
    if (topTag != null) parts += "mostly ${topTag.key}"
    val latestNote = notes.maxByOrNull { it.updatedAt.toEpochMilliseconds() }?.content?.trim()
    if (!latestNote.isNullOrBlank()) {
        val snippet = latestNote.lineSequence().firstOrNull().orEmpty().take(60)
        parts += "last note: “$snippet”"
    }
    return parts.joinToString(" · ")
}
