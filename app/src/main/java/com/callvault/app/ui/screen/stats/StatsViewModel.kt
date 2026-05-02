package com.callvault.app.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.domain.model.DateRange
import com.callvault.app.domain.model.StatsSnapshot
import com.callvault.app.domain.usecase.ComputeStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** UI state for the Stats screen. */
data class StatsUiState(
    val range: DateRange = DateRange.last30Days(),
    val presetIndex: Int = 2,
    val snapshot: StatsSnapshot? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val sortByDuration: Boolean = false,
    val exportToast: String? = null
)

/**
 * Drives the Stats screen: tracks the current [DateRange], persists the
 * user's preset choice via [SettingsDataStore], and reruns
 * [ComputeStatsUseCase] whenever inputs change.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val computeStats: ComputeStatsUseCase,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val idx = runCatching { settings.statsLastRangePresetIndex.first() }
                .getOrDefault(2)
            val from = runCatching { settings.statsCustomFrom.first() }.getOrDefault(0L)
            val to = runCatching { settings.statsCustomTo.first() }.getOrDefault(0L)
            val range = rangeForPreset(idx, from, to)
            _state.value = _state.value.copy(range = range, presetIndex = idx)
            recompute()
        }
    }

    /** Map a preset index + persisted custom bounds to a [DateRange]. */
    private fun rangeForPreset(idx: Int, customFrom: Long, customTo: Long): DateRange =
        when (idx) {
            0 -> DateRange.today()
            1 -> DateRange.last7Days()
            2 -> DateRange.last30Days()
            3 -> DateRange.thisMonth()
            4 -> DateRange.lastMonth()
            5 -> DateRange.last90Days()
            6 -> if (customFrom > 0 && customTo > 0) DateRange(customFrom, customTo)
            else DateRange.last30Days()
            else -> DateRange.last30Days()
        }

    /** Switch to a preset by index (0..5) and recompute. */
    fun setPreset(index: Int) {
        viewModelScope.launch {
            val range = rangeForPreset(index, 0, 0)
            _state.value = _state.value.copy(range = range, presetIndex = index)
            settings.setStatsLastRangePresetIndex(index)
            recompute()
        }
    }

    /** Apply a user-picked custom range and persist it. */
    fun setCustomRange(from: Long, to: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                range = DateRange(from, to), presetIndex = 6
            )
            settings.setStatsLastRangePresetIndex(6)
            settings.setStatsCustomFrom(from)
            settings.setStatsCustomTo(to)
            recompute()
        }
    }

    /** Re-run aggregates for the current range. */
    fun refresh() {
        viewModelScope.launch { recompute() }
    }

    /** Toggle the Top Numbers sort order between count- and duration-desc. */
    fun toggleSortByDuration() {
        _state.value = _state.value.copy(sortByDuration = !_state.value.sortByDuration)
    }

    /** Stub for the bottom "Export PDF" button — wired in Sprint 9. */
    fun exportPdf() {
        _state.value = _state.value.copy(
            exportToast = "Export available after Sprint 9."
        )
    }

    /** Clear the one-shot export toast after the snackbar consumes it. */
    fun consumeExportToast() {
        _state.value = _state.value.copy(exportToast = null)
    }

    private suspend fun recompute() {
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            val snap = computeStats(_state.value.range)
            _state.value = _state.value.copy(snapshot = snap, loading = false)
        } catch (t: Throwable) {
            _state.value = _state.value.copy(
                loading = false,
                error = t.message ?: "Failed to compute stats."
            )
        }
    }
}
