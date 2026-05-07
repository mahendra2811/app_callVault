package com.callNest.app.util

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/** Lightweight CSV parser tuned for two-column "name, phone" input. No external deps. */
object CsvContactParser {

    data class Row(val name: String?, val phoneRaw: String, val normalized: String)
    data class Parsed(val rows: List<Row>, val skipped: Int)

    private val PHONE_RE = Regex("[+0-9]")

    fun parse(stream: InputStream): Parsed {
        val rows = mutableListOf<Row>()
        var skipped = 0
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { r ->
            r.lineSequence().forEachIndexed { idx, raw ->
                val line = raw.trim()
                if (line.isEmpty()) return@forEachIndexed
                val parts = splitCsv(line)
                if (parts.isEmpty()) return@forEachIndexed
                val (nameCol, phoneCol) = pickColumns(parts) ?: run {
                    skipped++
                    return@forEachIndexed
                }
                if (idx == 0 && looksLikeHeader(nameCol, phoneCol)) return@forEachIndexed
                val normalized = normalizePhone(phoneCol)
                if (normalized == null) {
                    skipped++
                    return@forEachIndexed
                }
                rows += Row(name = nameCol.takeIf { it.isNotBlank() }, phoneRaw = phoneCol, normalized = normalized)
            }
        }
        return Parsed(rows, skipped)
    }

    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> { out += sb.toString().trim(); sb.clear() }
                else -> sb.append(ch)
            }
        }
        out += sb.toString().trim()
        return out
    }

    /** Returns (name, phone) or null if neither column looks like a phone number. */
    private fun pickColumns(parts: List<String>): Pair<String, String>? {
        if (parts.size == 1) {
            return if (looksLikePhone(parts[0])) "" to parts[0] else null
        }
        val phoneIdx = parts.indexOfFirst { looksLikePhone(it) }
        if (phoneIdx == -1) return null
        val name = parts.filterIndexed { i, _ -> i != phoneIdx }.joinToString(" ").trim()
        return name to parts[phoneIdx]
    }

    private fun looksLikePhone(s: String): Boolean {
        val digits = s.count { it.isDigit() }
        return digits in 7..15 && s.all { PHONE_RE.matches(it.toString()) || it.isWhitespace() || it == '-' || it == '(' || it == ')' }
    }

    private fun looksLikeHeader(name: String, phone: String): Boolean =
        name.equals("name", ignoreCase = true) ||
            phone.equals("phone", ignoreCase = true) ||
            phone.equals("number", ignoreCase = true) ||
            phone.equals("mobile", ignoreCase = true)

    /** Returns E.164-ish India default: +91 prefix when 10 local digits, else null on bad input. */
    private fun normalizePhone(raw: String): String? {
        val cleaned = raw.filter { it.isDigit() || it == '+' }
        return when {
            // Already E.164: +<country><7-13 digits>
            cleaned.startsWith("+") && cleaned.drop(1).length in 8..15 -> cleaned
            // Bare 10-digit Indian local
            cleaned.length == 10 && cleaned.first() in '6'..'9' -> "+91$cleaned"
            // Leading "0" + 10-digit Indian local
            cleaned.length == 11 && cleaned.startsWith("0") &&
                cleaned[1] in '6'..'9' -> "+91${cleaned.drop(1)}"
            // 12-digit "91XXXXXXXXXX" — country code + 10-digit local, no plus
            cleaned.length == 12 && cleaned.startsWith("91") &&
                cleaned[2] in '6'..'9' -> "+$cleaned"
            else -> null
        }
    }
}
