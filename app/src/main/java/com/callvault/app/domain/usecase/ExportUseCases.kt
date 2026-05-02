package com.callvault.app.domain.usecase

import com.callvault.app.data.export.CsvExporter
import com.callvault.app.data.export.ExcelExporter
import com.callvault.app.data.export.ExportColumns
import com.callvault.app.data.export.ExportDestination
import com.callvault.app.data.export.ExportFilter
import com.callvault.app.data.export.ExportResult
import com.callvault.app.data.export.JsonExporter
import com.callvault.app.data.export.PdfExporter
import com.callvault.app.data.export.VcardExporter
import javax.inject.Inject

/** CSV export use case (Sprint 9). */
class ExportToCsvUseCase @Inject constructor(private val exporter: CsvExporter) {
    suspend operator fun invoke(
        filter: ExportFilter,
        columns: ExportColumns,
        destination: ExportDestination
    ): ExportResult = exporter.export(filter, columns, destination)
}

/** Excel (.xlsx) export use case (Sprint 9). */
class ExportToExcelUseCase @Inject constructor(private val exporter: ExcelExporter) {
    suspend operator fun invoke(
        filter: ExportFilter,
        columns: ExportColumns,
        destination: ExportDestination
    ): ExportResult = exporter.export(filter, columns, destination)
}

/** PDF export use case (Sprint 9). */
class ExportToPdfUseCase @Inject constructor(private val exporter: PdfExporter) {
    suspend operator fun invoke(
        filter: ExportFilter,
        columns: ExportColumns,
        destination: ExportDestination
    ): ExportResult = exporter.export(filter, columns, destination)
}

/** Full-database JSON export use case (Sprint 9). */
class ExportToJsonUseCase @Inject constructor(private val exporter: JsonExporter) {
    suspend operator fun invoke(destination: ExportDestination): ExportResult =
        exporter.export(destination)
}

/** vCard export use case (Sprint 9). */
class ExportToVcardUseCase @Inject constructor(private val exporter: VcardExporter) {
    suspend operator fun invoke(
        filter: ExportFilter,
        destination: ExportDestination
    ): ExportResult = exporter.export(filter, destination)
}
