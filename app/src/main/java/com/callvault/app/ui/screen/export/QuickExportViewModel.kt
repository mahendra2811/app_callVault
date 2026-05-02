package com.callvault.app.ui.screen.export

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.export.ExportColumns
import com.callvault.app.data.export.ExportDestination
import com.callvault.app.data.export.ExportFilter
import com.callvault.app.domain.usecase.ExportToCsvUseCase
import com.callvault.app.domain.usecase.ExportToExcelUseCase
import com.callvault.app.domain.usecase.ExportToJsonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import javax.inject.Inject

/**
 * Drives the [QuickExportSheet]. One-tap export with the user's current
 * filter (defaulted to [ExportFilter] = "no filter — all calls" because v1
 * does not yet persist a last-used filter).
 */
@HiltViewModel
class QuickExportViewModel @Inject constructor(
    private val exportCsv: ExportToCsvUseCase,
    private val exportExcel: ExportToExcelUseCase,
    private val exportJson: ExportToJsonUseCase,
) : ViewModel() {

    /** UI state for the quick-export sheet. */
    sealed interface QuickExportUiState {
        /** Nothing in flight. */
        data object Idle : QuickExportUiState

        /** An export is running. */
        data object Running : QuickExportUiState

        /** Export finished successfully. */
        data class Success(val uri: Uri, val fileName: String, val sizeBytes: Long) : QuickExportUiState

        /** Export failed; reason is user-friendly. */
        data class Error(val reason: String) : QuickExportUiState
    }

    private val _state = MutableStateFlow<QuickExportUiState>(QuickExportUiState.Idle)
    val state: StateFlow<QuickExportUiState> = _state.asStateFlow()

    private val currentFilter: ExportFilter get() = ExportFilter()
    private val defaultColumns: ExportColumns get() = ExportColumns()

    /** Run a CSV export with the current filter. */
    fun exportCsv() = run("csv") {
        exportCsv(currentFilter, defaultColumns, downloadsDestination("csv"))
    }

    /** Run an Excel export with the current filter. */
    fun exportExcel() = run("xlsx") {
        exportExcel(currentFilter, defaultColumns, downloadsDestination("xlsx"))
    }

    /** Run a full-database JSON dump (no filter). */
    fun exportJson() = run("json") {
        exportJson(downloadsDestination("json"))
    }

    /** Reset to [QuickExportUiState.Idle] (used by Retry). */
    fun reset() {
        _state.value = QuickExportUiState.Idle
    }

    private fun run(
        kind: String,
        block: suspend () -> com.callvault.app.data.export.ExportResult
    ) {
        if (_state.value is QuickExportUiState.Running) return
        _state.value = QuickExportUiState.Running
        viewModelScope.launch {
            try {
                val r = block()
                _state.value = QuickExportUiState.Success(r.uri, r.fileName, r.sizeBytes)
            } catch (t: Throwable) {
                Timber.e(t, "Quick export ($kind) failed")
                _state.value = QuickExportUiState.Error(
                    t.message ?: "Something went wrong while exporting."
                )
            }
        }
    }

    private fun downloadsDestination(ext: String): ExportDestination.Downloads {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val stamp = "%04d%02d%02d-%02d%02d".format(
            now.year, now.monthNumber, now.dayOfMonth, now.hour, now.minute
        )
        return ExportDestination.Downloads(fileName = "callvault-quick-$stamp.$ext")
    }
}
