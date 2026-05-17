package com.callNest.app.ui.screen.inquiries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.mapper.toDomain
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.data.work.SyncScheduler
import com.callNest.app.domain.model.ContactMeta
import com.callNest.app.domain.repository.ContactRepository
import com.callNest.app.domain.usecase.AutoSaveContactUseCase
import com.callNest.app.domain.usecase.AutoSaveNameBuilder
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
import kotlinx.coroutines.flow.first
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
    private val autoSave: AutoSaveContactUseCase,
    private val settings: SettingsDataStore,
    private val syncScheduler: SyncScheduler,
    bulkProgressBus: BulkSaveProgressBus
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

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
        // Show every number that isn't in the OS contacts list — that's what
        // "Inquiries" actually means to the user. Previously we filtered to
        // `isAutoSaved=1`, which hid every unknown caller until our own
        // auto-save loop had run successfully (and stayed empty for users
        // who hadn't enabled auto-save at all).
        combine(contactRepository.observeUnsaved(), _query, _selected, _bulkMode) { rows, q, sel, mode ->
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

    /**
     * Compute the exact display name the auto-save pipeline would generate
     * for [number] using the current prefix / SIM-tag / suffix settings.
     * Used by the Save-now confirmation dialog as a preview string.
     */
    suspend fun previewSavedName(number: String): String {
        val call = callDao.latestForNumber(number)?.toDomain()
        return AutoSaveNameBuilder.build(
            prefix = settings.autoSavePrefix.first(),
            includeSimTag = settings.autoSaveIncludeSimTag.first(),
            simSlot = call?.simSlot,
            suffix = AutoSaveContactUseCase.BRAND_SUFFIX,
            normalizedNumber = number
        )
    }

    /**
     * Save one number now — bypasses the autoSaveEnabled flag (the user is
     * explicitly opting in for this single contact via the Inquiries Save-now
     * button).
     */
    fun saveOneNow(number: String) {
        viewModelScope.launch {
            val call = callDao.latestForNumber(number)?.toDomain() ?: return@launch
            // Force-enable for this single invocation; restore prior value
            // immediately so the user's toggle preference is preserved.
            val wasEnabled = settings.autoSaveEnabled.first()
            if (!wasEnabled) settings.setAutoSaveEnabled(true)
            try { autoSave(call) } finally {
                if (!wasEnabled) settings.setAutoSaveEnabled(false)
            }
        }
    }

    /** Save every unsaved inquiry now — runs the bulk pipeline against the full list. */
    fun saveAllNow() {
        viewModelScope.launch {
            val calls = state.value.inquiries
                .mapNotNull { callDao.latestForNumber(it.normalizedNumber)?.toDomain() }
            val wasEnabled = settings.autoSaveEnabled.first()
            if (!wasEnabled) settings.setAutoSaveEnabled(true)
            try { bulkSave(calls) } finally {
                if (!wasEnabled) settings.setAutoSaveEnabled(false)
            }
        }
    }

    /** Pull-to-refresh: re-syncs the call log so brand-new inquiries land. */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { syncScheduler.triggerOnce() }
            // Sync runs on the worker — give it a brief beat so the spinner
            // isn't a flicker. The contact-meta Flow keeps the UI live.
            kotlinx.coroutines.delay(800)
            _isRefreshing.value = false
        }
    }
}

/** Immutable state surface for [InquiriesScreen]. */
data class InquiriesUiState(
    val inquiries: List<ContactMeta> = emptyList(),
    val selected: Set<String> = emptySet(),
    val bulkMode: Boolean = false
)
