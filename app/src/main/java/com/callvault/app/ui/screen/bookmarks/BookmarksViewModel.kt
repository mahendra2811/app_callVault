package com.callvault.app.ui.screen.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.domain.model.Call
import com.callvault.app.domain.model.ContactMeta
import com.callvault.app.domain.model.FilterState
import com.callvault.app.domain.repository.CallRepository
import com.callvault.app.domain.repository.ContactRepository
import com.callvault.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A bookmarked call paired with the rolled-up display name. */
data class BookmarkRow(
    val call: Call,
    val displayName: String?,
    val pinned: Boolean
)

/** UI state for [BookmarksScreen]. */
data class BookmarksUiState(
    /** Pinned section, in user-defined order, capped at 5. */
    val pinned: List<BookmarkRow> = emptyList(),
    /** All other bookmarked calls in date-desc order. */
    val others: List<BookmarkRow> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Drives the Bookmarks screen.
 *
 * Pinned numbers are persisted via [SettingsRepository.observePinnedBookmarks];
 * the live bookmarked-calls flow filters by `isBookmarked = 1` (see
 * [CallRepository.observeFiltered]).
 */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val callRepo: CallRepository,
    private val contactRepo: ContactRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<BookmarksUiState> = combine(
        callRepo.observeFiltered(FilterState(onlyBookmarked = true)),
        contactRepo.observeAll(),
        settingsRepo.observePinnedBookmarks(),
        _error
    ) { calls, contacts, pinnedNumbers, err ->
        val byNumber: Map<String, ContactMeta> = contacts.associateBy { it.normalizedNumber }
        // For each unique number, keep the most recent call.
        val latestByNumber: Map<String, Call> = calls
            .groupBy { it.normalizedNumber }
            .mapValues { (_, list) ->
                list.maxByOrNull { it.date.toEpochMilliseconds() }!!
            }

        val pinSet = pinnedNumbers.toSet()
        val pinnedRows = pinnedNumbers
            .mapNotNull { num -> latestByNumber[num]?.let { num to it } }
            .map { (num, call) ->
                BookmarkRow(
                    call = call,
                    displayName = byNumber[num]?.displayName ?: call.cachedName,
                    pinned = true
                )
            }

        val others = latestByNumber.values
            .filter { it.normalizedNumber !in pinSet }
            .sortedByDescending { it.date.toEpochMilliseconds() }
            .map {
                BookmarkRow(
                    call = it,
                    displayName = byNumber[it.normalizedNumber]?.displayName ?: it.cachedName,
                    pinned = false
                )
            }
        BookmarksUiState(pinned = pinnedRows, others = others, errorMessage = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookmarksUiState())

    fun togglePin(normalizedNumber: String) {
        viewModelScope.launch {
            runCatching {
                val current = settingsRepo.observePinnedBookmarks().first()
                val next = if (normalizedNumber in current) current - normalizedNumber
                else (current + normalizedNumber).take(MAX_PINS)
                settingsRepo.setPinnedBookmarks(next)
            }.onFailure { _error.value = "We couldn't update pinned bookmarks." }
        }
    }

    fun moveUp(normalizedNumber: String) = swap(normalizedNumber, -1)
    fun moveDown(normalizedNumber: String) = swap(normalizedNumber, +1)

    private fun swap(normalizedNumber: String, delta: Int) {
        viewModelScope.launch {
            runCatching {
                val current = settingsRepo.observePinnedBookmarks().first().toMutableList()
                val idx = current.indexOf(normalizedNumber)
                if (idx < 0) return@runCatching
                val target = idx + delta
                if (target !in current.indices) return@runCatching
                val tmp = current[idx]
                current[idx] = current[target]
                current[target] = tmp
                settingsRepo.setPinnedBookmarks(current)
            }.onFailure { _error.value = "We couldn't reorder pinned bookmarks." }
        }
    }

    fun unbookmark(callSystemId: Long) {
        viewModelScope.launch {
            runCatching { callRepo.setBookmarked(callSystemId, flag = false, reason = null) }
                .onFailure { _error.value = "We couldn't update the bookmark." }
        }
    }

    fun consumeError() { _error.value = null }

    private companion object {
        const val MAX_PINS = 5
    }
}
