package com.callNest.app.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.local.dao.SearchHistoryDao
import com.callNest.app.data.local.entity.SearchHistoryEntity
import com.callNest.app.data.system.ContactsReader
import com.callNest.app.domain.model.Call
import com.callNest.app.domain.model.Note
import com.callNest.app.domain.repository.CallRepository
import com.callNest.app.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the Spotlight-style search overlay.
 */
data class SearchUiState(
    val query: String = "",
    val contacts: List<ContactsReader.ContactMatch> = emptyList(),
    val calls: List<Call> = emptyList(),
    val notes: List<Note> = emptyList(),
    val recent: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = contacts.isEmpty() && calls.isEmpty() && notes.isEmpty()
    val total: Int get() = contacts.size + calls.size + notes.size
}

/**
 * Drives the macOS-Spotlight-style search overlay. Three live result
 * sections — OS Contacts (top), recent Calls (middle), Notes (bottom).
 * Each section is debounced 200ms and capped to its own limit.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val callRepo: CallRepository,
    private val noteRepo: NoteRepository,
    private val contactsReader: ContactsReader,
    private val historyDao: SearchHistoryDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val callsFlow = _query
        .debounce(200L)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList<Call>()) else callRepo.searchFts(q)
        }

    private val contactsFlow = _query
        .debounce(200L)
        .flatMapLatest { q ->
            flow { emit(if (q.isBlank()) emptyList() else contactsReader.searchContacts(q, limit = 6)) }
        }

    private val notesFlow = _query
        .debounce(200L)
        .flatMapLatest { q ->
            flow { emit(if (q.isBlank()) emptyList() else noteRepo.search(q).take(6)) }
        }

    val state: StateFlow<SearchUiState> = combine(
        _query,
        contactsFlow,
        callsFlow,
        notesFlow,
        historyDao.observeRecent(10)
    ) { q, contacts, calls, notes, recent ->
        SearchUiState(
            query = q,
            contacts = contacts,
            calls = calls.take(8),
            notes = notes,
            recent = recent.map { it.query }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(q: String) { _query.value = q }
    fun clearQuery() { _query.value = "" }

    fun selectRecent(q: String) {
        _query.value = q
        saveToHistory()
    }

    fun saveToHistory() {
        val q = _query.value.trim()
        if (q.isBlank()) return
        viewModelScope.launch { runCatching { historyDao.insert(SearchHistoryEntity(query = q)) } }
    }

    fun clearHistory() {
        viewModelScope.launch { runCatching { historyDao.clear() } }
    }
}
