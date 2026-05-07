package com.callNest.app.ui.util

/**
 * Lightweight pretty-printer for E.164-style phone numbers used in the UI.
 *
 * The full libphonenumber engine drives the data layer (`PhoneNumberNormalizer`);
 * the UI just needs a readable national rendering. This implementation:
 *
 * - Strips a leading `+91` / `+1` style country code, then groups digits in
 *   3-3-4 (10-digit) or 3-4 / 4-4 fallback chunks.
 * - Returns the input unchanged when it isn't recognisably E.164.
 *
 * Keep it dependency-free so `@Preview` works without a real Android context.
 */
object PhoneNumberFormatter {

    /** Friendly national-format string. Falls back to the input on failure. */
    fun pretty(e164OrRaw: String?): String {
        val input = e164OrRaw?.trim().orEmpty()
        if (input.isEmpty()) return ""
        if (!input.startsWith("+")) return groupDigits(input)

        val digits = input.drop(1).filter { it.isDigit() }
        if (digits.length < 8) return input

        // Best-effort country-code split. Two-digit and three-digit CCs cover
        // most real-world cases; precise ranges are libphonenumber's job.
        val cc = when {
            digits.startsWith("1") && digits.length == 11 -> "1"
            digits.startsWith("44") -> "44"
            digits.startsWith("91") -> "91"
            digits.startsWith("971") || digits.startsWith("972") -> digits.take(3)
            else -> digits.take(2)
        }
        val national = digits.removePrefix(cc)
        return "+$cc " + groupDigits(national)
    }

    private fun groupDigits(d: String): String {
        val s = d.filter { it.isDigit() }
        return when (s.length) {
            10 -> "${s.substring(0, 5)} ${s.substring(5)}"
            7 -> "${s.substring(0, 3)}-${s.substring(3)}"
            8 -> "${s.substring(0, 4)} ${s.substring(4)}"
            in 11..12 -> "${s.substring(0, s.length - 7)} ${s.substring(s.length - 7, s.length - 4)} ${s.substring(s.length - 4)}"
            else -> s.chunked(4).joinToString(" ")
        }
    }
}
