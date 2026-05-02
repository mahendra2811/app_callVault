package com.callvault.app.domain.usecase

/**
 * Pure function that builds the display name used when auto-saving an
 * unknown caller (spec §8.4).
 *
 * Format: `{prefix}( -s1| -s2)? +<E164>{suffix}`.
 *
 * Examples (prefix=`callVault`, includeSimTag=true, suffix=`""`):
 * - SIM 1, +919876543210 → `callVault-s1 +919876543210`
 * - No SIM info, +447700900000 → `callVault +447700900000`
 *
 * Stays a top-level object so it can be unit-tested with zero Android
 * dependencies.
 */
object AutoSaveNameBuilder {

    /**
     * @param prefix free-form user prefix; trimmed.
     * @param includeSimTag whether to append `-s1` / `-s2` when known.
     * @param simSlot 0/1 → `-s1`/`-s2`; null → omit.
     * @param suffix free-form trailing text; trimmed (kept verbatim if empty).
     * @param normalizedNumber an E.164 number (e.g. `+919876543210`).
     */
    fun build(
        prefix: String,
        includeSimTag: Boolean,
        simSlot: Int?,
        suffix: String,
        normalizedNumber: String
    ): String {
        val safePrefix = prefix.trim().ifEmpty { "callVault" }
        val safeSuffix = suffix.trim()
        val tag = when {
            includeSimTag && simSlot == 0 -> "-s1"
            includeSimTag && simSlot == 1 -> "-s2"
            else -> ""
        }
        val number = normalizedNumber.trim()
        return buildString {
            append(safePrefix)
            append(tag)
            append(' ')
            append(number)
            if (safeSuffix.isNotEmpty()) append(safeSuffix)
        }
    }

    /**
     * Snapshot string used to populate `ContactMetaEntity.autoSavedFormat`.
     * Persisting it lets us re-detect renames even after settings change.
     */
    fun formatSnapshot(prefix: String, includeSimTag: Boolean, suffix: String): String =
        "${prefix.trim()}|sim=$includeSimTag|${suffix.trim()}"
}
