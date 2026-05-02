package com.callvault.app.data.export

import com.callvault.app.domain.model.DateRange
import com.callvault.app.domain.usecase.ComputeStatsUseCase
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * iText 8 PDF exporter. Layout:
 *
 *  - **Cover** — title, generated date, applied filter summary.
 *  - **Totals** — overview metrics from [ComputeStatsUseCase].
 *  - **Calls** — paginated table, max 30 rows per page.
 *
 * Charts are intentionally deferred — see DECISIONS.md.
 */
@Singleton
class PdfExporter @Inject constructor(
    private val shared: ExportShared,
    private val computeStats: ComputeStatsUseCase
) {
    /** Build the PDF and stream it to [destination]. */
    suspend fun export(
        filter: ExportFilter,
        @Suppress("UNUSED_PARAMETER") columns: ExportColumns,
        destination: ExportDestination
    ): ExportResult {
        val rows = shared.queryCalls(filter)
        val fileName = (destination as? ExportDestination.Downloads)?.fileName
            ?: "callvault-${stamp()}.pdf"
        val target = if (destination is ExportDestination.PickedUri) destination
        else ExportDestination.Downloads(fileName)
        val (uri, stream) = shared.openOutputStream(target, "application/pdf")
        PdfWriter(stream).use { pw ->
            PdfDocument(pw).use { pdf ->
                pdf.defaultPageSize = PageSize.A4
                Document(pdf).use { doc ->
                    cover(doc, filter, rows.size)
                    doc.add(AreaBreak())
                    totals(doc, filter)
                    doc.add(AreaBreak())
                    callsTable(doc, rows)
                }
            }
        }
        return ExportResult(uri, fileName, shared.sizeOf(uri), "pdf")
    }

    private fun cover(doc: Document, filter: ExportFilter, count: Int) {
        doc.add(Paragraph("CallVault Export").setFontSize(28f).setBold())
        doc.add(Paragraph("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}"))
        doc.add(Paragraph("Calls in this report: $count"))
        doc.add(Paragraph("Filters applied").setBold().setFontSize(14f).setMarginTop(20f))
        val sb = StringBuilder()
        filter.range?.let {
            sb.append("• Date range: ${Date(it.from)} → ${Date(it.to)}\n")
        }
        filter.callTypes?.takeIf { it.isNotEmpty() }?.let {
            sb.append("• Call types: ${it.joinToString(", ")}\n")
        }
        filter.tagsAnyOf?.takeIf { it.isNotEmpty() }?.let {
            sb.append("• Tags: ${it.size} selected\n")
        }
        if (filter.bookmarkedOnly) sb.append("• Bookmarked only\n")
        if (filter.includeArchived) sb.append("• Includes archived\n")
        if (sb.isEmpty()) sb.append("• None — full database export\n")
        doc.add(Paragraph(sb.toString()))
    }

    private suspend fun totals(doc: Document, filter: ExportFilter) {
        val range = filter.range ?: DateRange.last30Days()
        val snap = computeStats(range)
        doc.add(Paragraph("Overview").setBold().setFontSize(20f))
        val o = snap.overview
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f)))
            .useAllAvailableWidth()
        listOf(
            "Total calls" to o.totalCalls.toString(),
            "Total talk time (s)" to o.totalTalkTimeSec.toString(),
            "Average duration (s)" to o.avgDurationSec.toString(),
            "Missed rate" to String.format(Locale.US, "%.1f%%", o.missedRate * 100),
            "Unsaved rate" to String.format(Locale.US, "%.1f%%", o.unsavedRate * 100)
        ).forEach { (k, v) ->
            table.addCell(Cell().add(Paragraph(k)))
            table.addCell(Cell().add(Paragraph(v).setTextAlignment(TextAlignment.RIGHT)))
        }
        doc.add(table)
    }

    private fun callsTable(doc: Document, rows: List<CallWithRelations>) {
        doc.add(Paragraph("Calls").setBold().setFontSize(20f))
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val pageSize = 30
        val pages = rows.chunked(pageSize)
        pages.forEachIndexed { idx, chunk ->
            val table = Table(UnitValue.createPercentArray(floatArrayOf(3f, 4f, 3f, 2f, 2f)))
                .useAllAvailableWidth()
            listOf("Date", "Number / Name", "Type", "Dur(s)", "Score").forEach {
                table.addHeaderCell(Cell().add(Paragraph(it).setBold()))
            }
            chunk.forEach { row ->
                table.addCell(df.format(Date(row.call.date.toEpochMilliseconds())))
                val label = row.contactMeta?.displayName ?: row.call.cachedName ?: row.call.normalizedNumber
                table.addCell(label)
                table.addCell(row.call.type.name)
                table.addCell(row.call.durationSec.toString())
                table.addCell(row.call.leadScore.toString())
            }
            doc.add(table)
            if (idx < pages.lastIndex) doc.add(AreaBreak())
        }
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
}
