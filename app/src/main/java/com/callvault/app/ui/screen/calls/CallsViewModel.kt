package com.callvault.app.ui.screen.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.data.work.SyncScheduler
import com.callvault.app.domain.model.Call
import com.callvault.app.domain.model.ContactMeta
import com.callvault.app.domain.model.FilterState
import com.callvault.app.domain.model.Tag
import com.callvault.app.domain.repository.CallRepository
import com.callvault.app.domain.repository.ContactRepository
import com.callvault.app.domain.repository.TagRepository
import com.callvault.app.domain.repository.UpdateRepository
import com.callvault.app.domain.repository.UpdateState
import com.callvault.app.domain.usecase.SyncProgress
import com.callvault.app.domain.usecase.SyncProgressBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/** Display row representing one call in the list. */
data class CallRow(
    val call: Call,
    val displayName: String?,
    val isUnsaved: Boolean,
    val tags: List<Tag>,
    val tagOverflowCount: Int
)

/** When grouping is on, calls are bucketed by normalized number. */
data class GroupedRow(
    val normalizedNumber: String,
    val displayName: String?,
    val latestCall: Call,
    val totalCalls: Int,
    val isUnsaved: Boolean,
    val tags: List<Tag>,
    val tagOverflowCount: Int
)

/** What grouping the list should render. */
enum class CallsViewMode { Flat, Grouped }

/** Full UI state for the Calls screen. */
data class CallsUiState(
    val flatList: List<CallRow> = emptyList(),
    val groupedByNumber: List<GroupedRow> = emptyList(),
    val pinnedUnsaved: List<CallRow> = emptyList(),
    val filter: FilterState = FilterState(),
    val selectedIds: Set<Long> = emptySet(),
    val bulkMode: Boolean = false,
    val viewMode: CallsViewMode = CallsViewMode.Flat,
    val pinnedSectionVisible: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Drives the Calls list screen.
 *
 * Reactively combines:
 * - filtered calls,
 * - all tags (for chip rendering and bulk apply),
 * - contact metadata (for unsaved flag + display name),
 * - "group by number" + "show unsaved pinned" display preferences.
 *
 * Listens to [SyncProgressBus] to flip [CallsUiState.isRefreshing].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CallsViewModel @Inject constructor(
    private val callRepo: CallRepository,
    private val tagRepo: TagRepository,
    private val contactRepo: ContactRepository,
    private val settings: SettingsDataStore,
    private val syncScheduler: SyncScheduler,
    private val syncProgressBus: SyncProgressBus,
    updateRepo: UpdateRepository
) : ViewModel() {

    /** Sprint 10/11 — surface update lifecycle so the Calls screen can show a banner. */
    val updateState: StateFlow<UpdateState> = updateRepo.state

    private val _filter = MutableStateFlow(FilterState())
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _bulkMode = MutableStateFlow(false)
    private val _viewMode = MutableStateFlow(CallsViewMode.Flat)
    private val _isRefreshing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val callsFlow = _filter.flatMapLatest { callRepo.observeFiltered(it) }
    private val tagsFlow = tagRepo.observeAll()
    private val contactsFlow = contactRepo.observeAll()
    private val pinnedFlow = callRepo.observeUnsavedLast7Days()

    /**
     * Per-call tag rollup. Sprint 4 wires the real cross-ref stream via
     * [TagRepository.observeAllAppliedTags]; Sprint 3 had this stubbed empty.
     */
    private val tagsByCallId: StateFlow<Map<Long, List<Tag>>> =
        tagRepo.observeAllAppliedTags().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyMap()
        )

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<CallsUiState> = combine(
        listOf(
            callsFlow,
            tagsFlow,
            contactsFlow,
            pinnedFlow,
            _filter,
            _selectedIds,
            _bulkMode,
            _viewMode,
            settings.displayGroupedByNumber,
            settings.displayShowUnsavedPinned,
            _isRefreshing,
            _error
        ).map { it as kotlinx.coroutines.flow.Flow<Any?> }
    ) { values: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val calls = values[0] as List<Call>
        @Suppress("UNCHECKED_CAST")
        val tags = values[1] as List<Tag>
        @Suppress("UNCHECKED_CAST")
        val contacts = values[2] as List<ContactMeta>
        @Suppress("UNCHECKED_CAST")
        val pinned = values[3] as List<Call>
        val filter = values[4] as FilterState
        @Suppress("UNCHECKED_CAST")
        val selected = values[5] as Set<Long>
        val bulk = values[6] as Boolean
        val viewMode = values[7] as CallsViewMode
        val grouped = values[8] as Boolean
        val showPinned = values[9] as Boolean
        val refreshing = values[10] as Boolean
        val err = values[11] as String?

        val byNumber = contacts.associateBy { it.normalizedNumber }
        val rows = calls.map { call -> toRow(call, byNumber) }
        val groupedRows = if (grouped) buildGrouped(calls, byNumber) else emptyList()
        val pinnedRows = if (showPinned) pinned.map { toRow(it, byNumber) } else emptyList()
        CallsUiState(
            flatList = rows,
            groupedByNumber = groupedRows,
            pinnedUnsaved = pinnedRows,
            filter = filter,
            selectedIds = selected,
            bulkMode = bulk,
            viewMode = if (grouped) CallsViewMode.Grouped else viewMode,
            pinnedSectionVisible = showPinned,
            isRefreshing = refreshing,
            errorMessage = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CallsUiState())

    init {
        // Mirror sync lifecycle into refreshing flag.
        syncProgressBus.events
            .onEach { ev ->
                _isRefreshing.value = when (ev) {
                    SyncProgress.Started, is SyncProgress.Progress -> true
                    is SyncProgress.Done, is SyncProgress.Error -> false
                }
                if (ev is SyncProgress.Error) _error.value = ev.message
            }
            .launchIn(viewModelScope)
    }

    private fun toRow(call: Call, byNumber: Map<String, ContactMeta>): CallRow {
        val meta = byNumber[call.normalizedNumber]
        val unsaved = meta?.let { !it.isInSystemContacts && !it.isAutoSaved } ?: true
        val tags = tagsByCallId.value[call.systemId].orEmpty()
        return CallRow(
            call = call,
            displayName = meta?.displayName ?: call.cachedName,
            isUnsaved = unsaved,
            tags = tags.take(MAX_TAGS_INLINE),
            tagOverflowCount = (tags.size - MAX_TAGS_INLINE).coerceAtLeast(0)
        )
    }

    private fun buildGrouped(
        calls: List<Call>,
        byNumber: Map<String, ContactMeta>
    ): List<GroupedRow> = calls
        .groupBy { it.normalizedNumber }
        .map { (number, group) ->
            val latest = group.maxByOrNull { it.date.toEpochMilliseconds() }!!
            val meta = byNumber[number]
            val unsaved = meta?.let { !it.isInSystemContacts && !it.isAutoSaved } ?: true
            GroupedRow(
                normalizedNumber = number,
                displayName = meta?.displayName ?: latest.cachedName,
                latestCall = latest,
                totalCalls = group.size,
                isUnsaved = unsaved,
                tags = emptyList(),
                tagOverflowCount = 0
            )
        }
        .sortedByDescending { it.latestCall.date.toEpochMilliseconds() }

    // ---------- Actions ----------

    fun setFilter(filter: FilterState) { _filter.value = filter }

    fun togglePin() {
        viewModelScope.launch {
            val current = settings.displayShowUnsavedPinned
            // toggle by writing the inverse. Read once via flow.first().
            val now = current.first()
            settings.setDisplayShowUnsavedPinned(!now)
        }
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val current = settings.displayGroupedByNumber.first()
            settings.setDisplayGroupedByNumber(!current)
        }
    }

    fun setBulkMode(enabled: Boolean) {
        _bulkMode.value = enabled
        if (!enabled) _selectedIds.value = emptySet()
    }

    fun toggleSelect(callId: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(callId)) remove(callId)
        }
        if (_selectedIds.value.isEmpty()) _bulkMode.value = false
    }

    fun bulkClear() {
        _selectedIds.value = emptySet()
        _bulkMode.value = false
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { syncScheduler.triggerOnce() }
                .onFailure { _error.value = "We couldn't start the sync. Try again in a moment." }
        }
    }

    fun swipeRight(callId: Long) {
        viewModelScope.launch {
            runCatching { callRepo.toggleBookmark(callId) }
                .onFailure { _error.value = "We couldn't update the bookmark." }
        }
    }

    fun swipeLeft(callId: Long) {
        viewModelScope.launch {
            runCatching { callRepo.setArchived(callId, true) }
                .onFailure { _error.value = "We couldn't archive that call." }
        }
    }

    fun consumeError() { _error.value = null }

    private companion object {
        const val MAX_TAGS_INLINE = 3
    }
}
