package com.callNest.app.ui.util

import java.util.Locale

/**
 * Compact human duration string.
 *
 * Examples:
 * - 0 → `0s`
 * - 45 → `45s`
 * - 90 → `1m 30s`
 * - 3725 → `1h 2m`
 *
 * Locale is honoured for digit grouping; abbreviations stay English because
 * they're recognised across most consumer locales we ship for.
 */
object DurationFormatter {

    /** Returns a short two-unit duration string. */
    fun short(durationSec: Int): String {
        val safe = durationSec.coerceAtLeast(0)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return when {
            h > 0 -> String.format(Locale.getDefault(), "%dh %dm", h, m)
            m > 0 -> String.format(Locale.getDefault(), "%dm %ds", m, s)
            else -> String.format(Locale.getDefault(), "%ds", s)
        }
    }

    /** Long-form, used by Stats card. e.g. "2 hours 14 minutes". */
    fun verbose(durationSec: Int): String {
        val safe = durationSec.coerceAtLeast(0)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val parts = buildList {
            if (h > 0) add(if (h == 1) "1 hour" else "$h hours")
            if (m > 0) add(if (m == 1) "1 minute" else "$m minutes")
            if (h == 0 && m == 0) add("less than a minute")
        }
        return parts.joinToString(" ")
    }
}
