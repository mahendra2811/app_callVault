package com.callvault.app.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.local.dao.SearchHistoryDao
import com.callvault.app.data.local.entity.SearchHistoryEntity
import com.callvault.app.domain.model.Call
import com.callvault.app.domain.model.ContactMeta
import com.callvault.app.domain.repository.CallRepository
import com.callvault.app.domain.repository.ContactRepository
import com.callvault.app.ui.screen.calls.CallRow
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** UI state for the Search screen. */
data class SearchUiState(
    val query: String = "",
    val results: List<CallRow> = emptyList(),
    val recent: List<String> = emptyList()
)

/**
 * Drives the full-screen Search overlay.
 *
 * Debounces the query at 300ms and runs [CallRepository.searchFts]; the
 * result is decorated with display-name + unsaved metadata for the same
 * row component used by the Calls list.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val callRepo: CallRepository,
    private val contactRepo: ContactRepository,
    private val historyDao: SearchHistoryDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val resultsFlow = _query
        .debounce(300L)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList<Call>())
            else callRepo.searchFts(q)
        }

    val state: StateFlow<SearchUiState> = combine(
        _query,
        resultsFlow,
        contactRepo.observeAll(),
        historyDao.observeRecent(10)
    ) { q, calls, contacts, recent ->
        val byNumber: Map<String, ContactMeta> = contacts.associateBy { it.normalizedNumber }
        val rows = calls.map { call ->
            val meta = byNumber[call.normalizedNumber]
            CallRow(
                call = call,
                displayName = meta?.displayName ?: call.cachedName,
                isUnsaved = meta?.let { !it.isInSystemContacts && !it.isAutoSaved } ?: true,
                tags = emptyList(),
                tagOverflowCount = 0
            )
        }
        SearchUiState(
            query = q,
            results = rows,
            recent = recent.map { it.query }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(q: String) { _query.value = q }

    fun selectRecent(q: String) {
        _query.value = q
        viewModelScope.launch { runCatching { historyDao.insert(SearchHistoryEntity(query = q)) } }
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
