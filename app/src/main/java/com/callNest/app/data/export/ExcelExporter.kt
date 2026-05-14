package com.callNest.app.data.export

import com.callNest.app.domain.model.DateRange
import com.callNest.app.domain.usecase.ComputeStatsUseCase
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Multi-sheet Excel exporter built with Apache POI XSSF.
 *
 * Sheets emitted:
 *  1. **Calls** — one row per call honoring [ExportColumns].
 *  2. **Contacts** — `ContactMeta` aggregates for every number in scope.
 *  3. **Tags summary** — tag, applied count.
 *  4. **Stats** — flattened [com.callNest.app.domain.model.StatsSnapshot]
 *     for the same date range.
 */
@Singleton
class ExcelExporter @Inject constructor(
    private val shared: ExportShared,
    private val computeStats: ComputeStatsUseCase
) {
    /** Build the workbook and stream it to [destination]. */
    suspend fun export(
        filter: ExportFilter,
        columns: ExportColumns,
        destination: ExportDestination
    ): ExportResult = withContext(Dispatchers.IO) {
        val rows = shared.queryCalls(filter)
        val fileName = (destination as? ExportDestination.Downloads)?.fileName
            ?: "callNest-${stamp()}.xlsx"
        val target = if (destination is ExportDestination.PickedUri) destination
        else ExportDestination.Downloads(fileName)
        val handle = shared.openOutputStream(
            target,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        // XSSFWorkbook (in-memory). SXSSF would stream to disk and use less
        // RAM, but its internal call chain triggers org.apache.poi.ss.util
        // .SheetUtil, which static-references java.awt and crashes on
        // Android with NoClassDefFoundError. For typical SMB users with
        // <5000 calls, in-memory is fine; the IS_PENDING dance + try/catch
        // still guards against the 0-byte / partial-write issues.
        val wb = XSSFWorkbook()
        try {
            val bold = boldHeaderStyle(wb)
            val dateStyle = wb.createCellStyle().apply {
                dataFormat = wb.creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm")
            }
            writeCallsSheet(wb, rows, columns, bold, dateStyle)
            // Single combined "Contacts" sheet kept for back-compat; split
            // saved vs unsaved on two extra sheets so the user can hand
            // either bucket to a team-mate without filtering manually.
            writeContactsSheet(wb, rows, bold)
            writeContactsBucketSheet(wb, rows, bold, "Saved contacts", savedFilter = true)
            writeContactsBucketSheet(wb, rows, bold, "Unsaved inquiries", savedFilter = false)
            writeTagsSummarySheet(wb, rows, bold)
            writeStatsSheet(wb, filter, bold)
            handle.stream.use { out ->
                wb.write(out)
                out.flush()
            }
            shared.commit(handle)
            ExportResult(handle.uri, fileName, shared.sizeOf(handle.uri), "xlsx")
        } catch (t: Throwable) {
            Timber.e(t, "Excel export failed (${rows.size} rows)")
            runCatching { handle.stream.close() }
            shared.abort(handle)
            throw t
        } finally {
            // XSSFWorkbook has no dispose() — only close().
            runCatching { wb.close() }
        }
    }

    private fun writeCallsSheet(
        wb: Workbook,
        rows: List<CallWithRelations>,
        c: ExportColumns,
        headerStyle: CellStyle,
        dateStyle: CellStyle
    ) {
        val sheet = wb.createSheet("Calls")
        val headers = mutableListOf<String>()
        if (c.date) headers += "Date"
        if (c.number) headers += "Number"
        if (c.name) headers += "Name"
        if (c.type) headers += "Type"
        if (c.duration) headers += "Duration (s)"
        if (c.simSlot) headers += "SIM Slot"
        if (c.tags) headers += "Tags"
        if (c.notes) headers += "Notes"
        if (c.leadScore) headers += "Lead Score"
        if (c.geocodedLocation) headers += "Location"
        if (c.isBookmarked) headers += "Bookmarked"
        if (c.isArchived) headers += "Archived"
        writeHeader(sheet, headers, headerStyle)
        rows.forEachIndexed { i, row ->
            val r = sheet.createRow(i + 1)
            var col = 0
            if (c.date) {
                val cell = r.createCell(col++)
                cell.setCellValue(Date(row.call.date.toEpochMilliseconds()))
                cell.cellStyle = dateStyle
            }
            if (c.number) r.createCell(col++).setCellValue(row.call.normalizedNumber)
            if (c.name) r.createCell(col++).setCellValue(row.contactMeta?.displayName ?: row.call.cachedName ?: "")
            if (c.type) r.createCell(col++).setCellValue(row.call.type.name)
            if (c.duration) r.createCell(col++).setCellValue(row.call.durationSec.toDouble())
            if (c.simSlot) r.createCell(col++).setCellValue(row.call.simSlot?.toString() ?: "")
            if (c.tags) r.createCell(col++).setCellValue(row.tags.joinToString(", ") { it.name })
            if (c.notes) r.createCell(col++).setCellValue(row.notes.joinToString(" | ") { it.content })
            if (c.leadScore) r.createCell(col++).setCellValue(row.call.leadScore.toDouble())
            if (c.geocodedLocation) r.createCell(col++).setCellValue(row.call.geocodedLocation ?: "")
            if (c.isBookmarked) r.createCell(col++).setCellValue(if (row.call.isBookmarked) "Yes" else "No")
            if (c.isArchived) r.createCell(col++).setCellValue(if (row.call.isArchived) "Yes" else "No")
        }
    }

    private fun writeContactsSheet(
        wb: Workbook,
        rows: List<CallWithRelations>,
        headerStyle: CellStyle
    ) {
        val sheet = wb.createSheet("Contacts")
        writeHeader(
            sheet,
            listOf(
                "Number", "Display Name", "In System Contacts", "Total Calls",
                "Total Duration (s)", "Incoming", "Outgoing", "Missed",
                "Lead Score", "First Call", "Last Call"
            ),
            headerStyle
        )
        val unique = rows.mapNotNull { it.contactMeta }.distinctBy { it.normalizedNumber }
        unique.forEachIndexed { i, m ->
            val r = sheet.createRow(i + 1)
            r.createCell(0).setCellValue(m.normalizedNumber)
            r.createCell(1).setCellValue(m.displayName ?: "")
            r.createCell(2).setCellValue(if (m.isInSystemContacts) "Yes" else "No")
            r.createCell(3).setCellValue(m.totalCalls.toDouble())
            r.createCell(4).setCellValue(m.totalDuration.toDouble())
            r.createCell(5).setCellValue(m.incomingCount.toDouble())
            r.createCell(6).setCellValue(m.outgoingCount.toDouble())
            r.createCell(7).setCellValue(m.missedCount.toDouble())
            r.createCell(8).setCellValue(m.computedLeadScore.toDouble())
            r.createCell(9).setCellValue(Date(m.firstCallDate.toEpochMilliseconds()).toString())
            r.createCell(10).setCellValue(Date(m.lastCallDate.toEpochMilliseconds()).toString())
        }
    }

    /**
     * Emit a contacts sheet filtered to either saved (in OS contacts) or
     * unsaved (unknown / unsaved inquiry) numbers. A number that has no
     * ContactMeta row at all is treated as unsaved.
     */
    private fun writeContactsBucketSheet(
        wb: Workbook,
        rows: List<CallWithRelations>,
        headerStyle: CellStyle,
        sheetName: String,
        savedFilter: Boolean
    ) {
        val sheet = wb.createSheet(sheetName)
        writeHeader(
            sheet,
            listOf(
                "Number", "Display Name", "Total Calls", "Incoming",
                "Outgoing", "Missed", "Lead Score", "Last Call", "Tags"
            ),
            headerStyle
        )

        // Group calls by normalized number so we still emit one row per
        // contact even when the meta table is empty.
        val byNumber = rows.groupBy { it.call.normalizedNumber }
        val filtered = byNumber.entries.filter { (_, group) ->
            val meta = group.firstNotNullOfOrNull { it.contactMeta }
            val isSaved = meta?.isInSystemContacts == true
            if (savedFilter) isSaved else !isSaved
        }

        filtered.forEachIndexed { i, (number, group) ->
            val r = sheet.createRow(i + 1)
            val meta = group.firstNotNullOfOrNull { it.contactMeta }
            val displayName = meta?.displayName
                ?: group.firstNotNullOfOrNull { it.call.cachedName }
                ?: ""
            val incoming = group.count { it.call.type.name == "INCOMING" }
            val outgoing = group.count { it.call.type.name == "OUTGOING" }
            val missed = group.count { it.call.type.name == "MISSED" }
            val lastCallMs = group.maxOf { it.call.date.toEpochMilliseconds() }
            val score = meta?.computedLeadScore ?: 0
            val tagsCsv = group.flatMap { it.tags }.map { it.name }.distinct().joinToString(", ")
            r.createCell(0).setCellValue(number)
            r.createCell(1).setCellValue(displayName)
            r.createCell(2).setCellValue(group.size.toDouble())
            r.createCell(3).setCellValue(incoming.toDouble())
            r.createCell(4).setCellValue(outgoing.toDouble())
            r.createCell(5).setCellValue(missed.toDouble())
            r.createCell(6).setCellValue(score.toDouble())
            r.createCell(7).setCellValue(Date(lastCallMs).toString())
            r.createCell(8).setCellValue(tagsCsv)
        }
    }

    private fun writeTagsSummarySheet(
        wb: Workbook,
        rows: List<CallWithRelations>,
        headerStyle: CellStyle
    ) {
        val sheet = wb.createSheet("Tags summary")
        writeHeader(sheet, listOf("Tag", "Calls Tagged"), headerStyle)
        val counts = mutableMapOf<String, Int>()
        rows.forEach { row -> row.tags.forEach { counts[it.name] = (counts[it.name] ?: 0) + 1 } }
        counts.entries.sortedByDescending { it.value }.forEachIndexed { i, e ->
            val r = sheet.createRow(i + 1)
            r.createCell(0).setCellValue(e.key)
            r.createCell(1).setCellValue(e.value.toDouble())
        }
    }

    private suspend fun writeStatsSheet(
        wb: Workbook,
        filter: ExportFilter,
        headerStyle: CellStyle
    ) {
        val sheet = wb.createSheet("Stats")
        val range = filter.range ?: DateRange.last30Days()
        val snap = computeStats(range)
        writeHeader(sheet, listOf("Metric", "Value"), headerStyle)
        var rIdx = 1
        fun put(k: String, v: String) {
            val r = sheet.createRow(rIdx++)
            r.createCell(0).setCellValue(k)
            r.createCell(1).setCellValue(v)
        }
        put("Total Calls", snap.overview.totalCalls.toString())
        put("Total Talk Time (s)", snap.overview.totalTalkTimeSec.toString())
        put("Average Duration (s)", snap.overview.avgDurationSec.toString())
        put("Missed Rate", String.format(Locale.US, "%.2f%%", snap.overview.missedRate * 100))
        put("Unsaved Rate", String.format(Locale.US, "%.2f%%", snap.overview.unsavedRate * 100))
        snap.overview.leadDistribution.forEach { (b, c) -> put("Leads — ${b.name}", c.toString()) }
        snap.callTypes.forEach { put("Type — ${it.label}", it.count.toString()) }
        snap.topByCount.take(10).forEachIndexed { i, e ->
            put("Top #${i + 1}", "${e.normalizedNumber} (${e.callCount} calls)")
        }
    }

    private fun writeHeader(sheet: Sheet, names: List<String>, style: CellStyle) {
        val r: Row = sheet.createRow(0)
        names.forEachIndexed { i, n ->
            val cell = r.createCell(i)
            cell.setCellValue(n)
            cell.cellStyle = style
        }
    }

    private fun boldHeaderStyle(wb: Workbook): CellStyle {
        val style = wb.createCellStyle()
        val font = wb.createFont().apply { bold = true }
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.LEFT
        style.borderBottom = BorderStyle.THIN
        return style
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
}
