package com.callNest.app.data.export

import com.callNest.app.domain.model.CallType
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real CSV exporter. UTF-8 with BOM, RFC-4180 escaping.
 *
 * Each row is one call. Fields are quoted when they contain `"`, `,` or
 * line breaks; embedded quotes are doubled. The resulting file opens
 * cleanly in Excel, Numbers, and Sheets.
 */
@Singleton
class CsvExporter @Inject constructor(
    private val shared: ExportShared
) {
    /** Write the CSV blob to [destination] and return its descriptor. */
    suspend fun export(
        filter: ExportFilter,
        columns: ExportColumns,
        destination: ExportDestination
    ): ExportResult {
        val rows = shared.queryCalls(filter)
        val fileName = (destination as? ExportDestination.Downloads)?.fileName
            ?: "callNest-${stamp()}.csv"
        val resolved = if (destination is ExportDestination.Downloads) destination
        else ExportDestination.Downloads(fileName)
        val target = if (destination is ExportDestination.PickedUri) destination else resolved
        val (uri, stream) = shared.openOutputStream(target, "text/csv")
        OutputStreamWriter(stream, Charsets.UTF_8).use { w ->
            // UTF-8 BOM so Excel detects the encoding.
            w.write("﻿")
            w.write(headerLine(columns))
            w.write("\n")
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            rows.forEach { row ->
                val cells = mutableListOf<String>()
                if (columns.date) cells += df.format(Date(row.call.date.toEpochMilliseconds()))
                if (columns.number) cells += row.call.normalizedNumber
                if (columns.name) cells += (row.contactMeta?.displayName ?: row.call.cachedName ?: "")
                if (columns.type) cells += row.call.type.name
                if (columns.duration) cells += row.call.durationSec.toString()
                if (columns.simSlot) cells += (row.call.simSlot?.toString() ?: "")
                if (columns.tags) cells += row.tags.joinToString(", ") { it.name }
                if (columns.notes) cells += row.notes.joinToString(" | ") { it.content }
                if (columns.leadScore) cells += row.call.leadScore.toString()
                if (columns.geocodedLocation) cells += (row.call.geocodedLocation ?: "")
                if (columns.isBookmarked) cells += if (row.call.isBookmarked) "1" else "0"
                if (columns.isArchived) cells += if (row.call.isArchived) "1" else "0"
                w.write(cells.joinToString(",") { escape(it) })
                w.write("\n")
            }
            w.flush()
        }
        return ExportResult(uri, fileName, shared.sizeOf(uri), "csv")
    }

    private fun headerLine(c: ExportColumns): String {
        val h = mutableListOf<String>()
        if (c.date) h += "Date"
        if (c.number) h += "Number"
        if (c.name) h += "Name"
        if (c.type) h += "Type"
        if (c.duration) h += "Duration (s)"
        if (c.simSlot) h += "SIM Slot"
        if (c.tags) h += "Tags"
        if (c.notes) h += "Notes"
        if (c.leadScore) h += "Lead Score"
        if (c.geocodedLocation) h += "Location"
        if (c.isBookmarked) h += "Bookmarked"
        if (c.isArchived) h += "Archived"
        return h.joinToString(",") { escape(it) }
    }

    private fun escape(value: String): String {
        val needs = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        if (!needs) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    @Suppress("unused") private fun unusedReference(): CallType = CallType.UNKNOWN
    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
}
