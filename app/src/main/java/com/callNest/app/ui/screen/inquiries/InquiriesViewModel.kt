package com.callNest.app.ui.screen.inquiries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.mapper.toDomain
import com.callNest.app.domain.model.ContactMeta
import com.callNest.app.domain.repository.ContactRepository
import com.callNest.app.domain.usecase.BulkSaveContactsUseCase
import com.callNest.app.domain.usecase.BulkSaveProgress
import com.callNest.app.domain.usecase.BulkSaveProgressBus
import com.callNest.app.domain.usecase.ConvertToMyContactUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State + intents for [InquiriesScreen].
 */
@HiltViewModel
class InquiriesViewModel @Inject constructor(
    contactRepository: ContactRepository,
    private val callDao: CallDao,
    private val convertToMyContact: ConvertToMyContactUseCase,
    private val bulkSave: BulkSaveContactsUseCase,
    bulkProgressBus: BulkSaveProgressBus
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected

    private val _bulkMode = MutableStateFlow(false)
    val bulkMode: StateFlow<Boolean> = _bulkMode

    val bulkProgress: StateFlow<BulkSaveProgress> = bulkProgressBus.events.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BulkSaveProgress.Idle
    )

    val state: StateFlow<InquiriesUiState> =
        combine(contactRepository.observeAutoSaved(), _query, _selected, _bulkMode) { rows, q, sel, mode ->
            val filtered = if (q.isBlank()) rows else {
                val needle = q.trim().lowercase()
                rows.filter {
                    (it.displayName?.lowercase()?.contains(needle) == true) ||
                        it.normalizedNumber.contains(needle)
                }
            }
            InquiriesUiState(
                inquiries = filtered,
                selected = sel,
                bulkMode = mode
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InquiriesUiState()
        )

    fun setQuery(q: String) { _query.value = q }

    fun toggleSelect(number: String) {
        _selected.value = _selected.value.toMutableSet().also {
            if (!it.add(number)) it.remove(number)
        }
        if (_selected.value.isEmpty()) _bulkMode.value = false
    }

    fun enterBulkMode(initial: String) {
        _bulkMode.value = true
        _selected.value = setOf(initial)
    }

    fun clearSelection() {
        _selected.value = emptySet()
        _bulkMode.value = false
    }

    /** Promote a single inquiry — caller passes the chosen new display name. */
    fun convert(number: String, newDisplayName: String) {
        viewModelScope.launch {
            convertToMyContact(number, newDisplayName)
        }
    }

    /**
     * "Convert all" — re-uses the selection set; promotes each row using its
     * current display name minus the auto-save chrome (callers may prefer to
     * surface a bulk-rename UX in a later iteration).
     */
    fun convertAllSelected() {
        val targets = _selected.value.toList()
        viewModelScope.launch {
            for (number in targets) {
                val current = state.value.inquiries.firstOrNull { it.normalizedNumber == number }
                val fallback = current?.displayName ?: number
                convertToMyContact(number, fallback)
            }
            clearSelection()
        }
    }

    /**
     * "Bulk save" — these rows are already auto-saved, but the user may have
     * deleted contacts in their phone after the fact. Re-runs the auto-save
     * pipeline against the latest call for each number to refill any holes.
     */
    fun bulkSaveSelected() {
        val targets = _selected.value.toList()
        viewModelScope.launch {
            val calls = targets.mapNotNull { callDao.latestForNumber(it)?.toDomain() }
            bulkSave(calls)
            clearSelection()
        }
    }
}

/** Immutable state surface for [InquiriesScreen]. */
data class InquiriesUiState(
    val inquiries: List<ContactMeta> = emptyList(),
    val selected: Set<String> = emptySet(),
    val bulkMode: Boolean = false
)
