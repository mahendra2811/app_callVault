package com.callNest.app.data.export

import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * vCard 3.0 exporter for [com.callNest.app.domain.model.ContactMeta]
 * rows that match the export filter (via their linked calls).
 *
 * Produces ASCII-clean records of the form:
 * ```
 * BEGIN:VCARD
 * VERSION:3.0
 * FN:<displayName or number>
 * TEL;TYPE=CELL:<E.164>
 * NOTE:<aggregated notes>
 * END:VCARD
 * ```
 */
@Singleton
class VcardExporter @Inject constructor(
    private val shared: ExportShared
) {
    /** Build the vCard text and write it to [destination]. */
    suspend fun export(
        filter: ExportFilter,
        destination: ExportDestination
    ): ExportResult {
        val rows = shared.queryCalls(filter)
        val byNumber = rows.groupBy { it.call.normalizedNumber }
        val fileName = (destination as? ExportDestination.Downloads)?.fileName
            ?: "callNest-${stamp()}.vcf"
        val target = if (destination is ExportDestination.PickedUri) destination
        else ExportDestination.Downloads(fileName)
        val handle = shared.openOutputStream(target, "text/vcard")
        shared.writeAndCommit(handle) { stream ->
            OutputStreamWriter(stream, Charsets.UTF_8).use { w ->
                byNumber.forEach { (number, group) ->
                    val name = group.firstNotNullOfOrNull { it.contactMeta?.displayName }
                        ?: group.firstNotNullOfOrNull { it.call.cachedName }
                        ?: number
                    val notes = group.flatMap { it.notes }.joinToString(" | ") { it.content }
                    w.write("BEGIN:VCARD\n")
                    w.write("VERSION:3.0\n")
                    w.write("FN:${escape(name)}\n")
                    w.write("TEL;TYPE=CELL:${escape(number)}\n")
                    if (notes.isNotEmpty()) w.write("NOTE:${escape(notes)}\n")
                    w.write("END:VCARD\n")
                }
                w.flush()
            }
        }
        return ExportResult(handle.uri, fileName, shared.sizeOf(handle.uri), "vcf")
    }

    /** vCard escapes: `\` `,` `;` and newlines. */
    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
            .replace("\r", "")

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
}
