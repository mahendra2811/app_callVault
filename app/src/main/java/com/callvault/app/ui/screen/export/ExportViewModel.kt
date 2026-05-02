package com.callvault.app.ui.screen.export

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.export.ExportColumns
import com.callvault.app.data.export.ExportDestination
import com.callvault.app.data.export.ExportFilter
import com.callvault.app.data.export.ExportResult
import com.callvault.app.domain.model.DateRange
import com.callvault.app.domain.usecase.ExportToCsvUseCase
import com.callvault.app.domain.usecase.ExportToExcelUseCase
import com.callvault.app.domain.usecase.ExportToJsonUseCase
import com.callvault.app.domain.usecase.ExportToPdfUseCase
import com.callvault.app.domain.usecase.ExportToVcardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Format options surfaced in the format step. */
enum class ExportFormat(val display: String, val ext: String) {
    Excel("Excel (.xlsx)", "xlsx"),
    Csv("CSV", "csv"),
    Pdf("PDF report", "pdf"),
    Json("JSON (full DB)", "json"),
    Vcard("vCard contacts", "vcf")
}

/** Predefined date-range chip + custom range. */
sealed class DateRangeChoice {
    data object AllTime : DateRangeChoice()
    data object Last7 : DateRangeChoice()
    data object Last30 : DateRangeChoice()
    data object Last90 : DateRangeChoice()
    data object ThisMonth : DateRangeChoice()
    data class Custom(val from: Long, val to: Long) : DateRangeChoice()

    /** Materialize to a domain [DateRange], or `null` for AllTime. */
    fun toRange(): DateRange? = when (this) {
        is AllTime -> null
        is Last7 -> DateRange.last7Days()
        is Last30 -> DateRange.last30Days()
        is Last90 -> DateRange.last90Days()
        is ThisMonth -> DateRange.thisMonth()
        is Custom -> DateRange(from, to)
    }
}

/** What scope of data the wizard targets. */
enum class ExportScope { CurrentFilter, AllData }

/** Where the file should land. */
sealed class DestinationChoice {
    data object Downloads : DestinationChoice()
    data class PickedUri(val uri: Uri) : DestinationChoice()
}

/** Aggregated UI state for the export wizard. */
data class ExportUiState(
    val step: Int = 0,
    val format: ExportFormat = ExportFormat.Excel,
    val dateRange: DateRangeChoice = DateRangeChoice.Last30,
    val scope: ExportScope = ExportScope.AllData,
    val columns: ExportColumns = ExportColumns(),
    val destination: DestinationChoice = DestinationChoice.Downloads,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val result: ExportResult? = null
)

/** ViewModel for the export wizard. Manages step state and runs the chosen exporter. */
@HiltViewModel
class ExportViewModel @Inject constructor(
    application: Application,
    private val csv: ExportToCsvUseCase,
    private val excel: ExportToExcelUseCase,
    private val pdf: ExportToPdfUseCase,
    private val json: ExportToJsonUseCase,
    private val vcard: ExportToVcardUseCase
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    fun setFormat(f: ExportFormat) { _state.update { it.copy(format = f) } }
    fun setDateRange(c: DateRangeChoice) { _state.update { it.copy(dateRange = c) } }
    fun setScope(s: ExportScope) { _state.update { it.copy(scope = s) } }
    fun setColumns(c: ExportColumns) { _state.update { it.copy(columns = c) } }
    fun setDestination(d: DestinationChoice) { _state.update { it.copy(destination = d) } }
    fun next() { _state.update { it.copy(step = (it.step + 1).coerceAtMost(LAST_STEP)) } }
    fun back() { _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) } }
    fun consumeError() { _state.update { it.copy(error = null) } }
    fun consumeResult() { _state.update { it.copy(result = null) } }

    /** Final "Generate" step — runs the chosen exporter off the main thread. */
    fun generate() {
        val s = _state.value
        if (s.isGenerating) return
        _state.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch {
            try {
                val filter = ExportFilter(range = s.dateRange.toRange())
                val dest: ExportDestination = when (val d = s.destination) {
                    is DestinationChoice.Downloads -> ExportDestination.Downloads(
                        "callvault-${System.currentTimeMillis()}.${s.format.ext}"
                    )
                    is DestinationChoice.PickedUri -> ExportDestination.PickedUri(d.uri)
                }
                val result = when (s.format) {
                    ExportFormat.Csv -> csv(filter, s.columns, dest)
                    ExportFormat.Excel -> excel(filter, s.columns, dest)
                    ExportFormat.Pdf -> pdf(filter, s.columns, dest)
                    ExportFormat.Json -> json(dest)
                    ExportFormat.Vcard -> vcard(filter, dest)
                }
                _state.update { it.copy(isGenerating = false, result = result) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(isGenerating = false, error = "Couldn't export. ${t.message ?: "Unknown error"}")
                }
            }
        }
    }

    companion object {
        /** Last visible step index in the wizard. */
        const val LAST_STEP = 4
    }
}
