package com.callNest.app.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.export.ExportColumns
import com.callNest.app.data.export.ExportDestination
import com.callNest.app.data.export.ExportFilter
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.domain.model.DateRange
import com.callNest.app.domain.model.StatsSnapshot
import com.callNest.app.domain.usecase.ComputeStatsUseCase
import com.callNest.app.domain.usecase.ExportToPdfUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/** UI state for the Stats screen. */
data class StatsUiState(
    val range: DateRange = DateRange.last30Days(),
    val presetIndex: Int = 2,
    val snapshot: StatsSnapshot? = null,
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
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
    private val settings: SettingsDataStore,
    private val exportToPdf: ExportToPdfUseCase
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

    /**
     * User-initiated pull-to-refresh. Drives [StatsUiState.isRefreshing] so
     * the PTR spinner is distinct from the cold-start `loading` flag.
     */
    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            try {
                recompute()
            } finally {
                _state.value = _state.value.copy(isRefreshing = false)
            }
        }
    }

    /** Toggle the Top Numbers sort order between count- and duration-desc. */
    fun toggleSortByDuration() {
        _state.value = _state.value.copy(sortByDuration = !_state.value.sortByDuration)
    }

    /** Generate the Stats PDF for the currently selected range. */
    fun exportPdf() {
        val range = _state.value.range
        viewModelScope.launch {
            _state.value = _state.value.copy(exportToast = "Generating PDF…")
            try {
                val result = exportToPdf(
                    filter = ExportFilter(range = range),
                    columns = ExportColumns(),
                    destination = ExportDestination.Downloads("callNest-stats-${System.currentTimeMillis()}.pdf")
                )
                _state.value = _state.value.copy(
                    exportToast = "Saved to Downloads · ${result.fileName}"
                )
            } catch (t: Throwable) {
                Timber.w(t, "Stats PDF export failed")
                _state.value = _state.value.copy(
                    exportToast = "Couldn't export PDF. ${t.message ?: "Unknown error"}"
                )
            }
        }
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
