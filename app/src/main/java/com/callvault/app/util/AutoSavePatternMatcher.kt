package com.callvault.app.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Compiles + caches the regular expression that matches an auto-saved
 * contact display name as produced by
 * [com.callvault.app.domain.usecase.AutoSaveNameBuilder] (spec §8.4 / §8.5).
 *
 * Format: `{prefix}( -s1| -s2)? +<E164>{suffix}` — the SIM tag block is
 * optional, the suffix may be empty.
 *
 * The matcher is intentionally lenient: whitespace runs are collapsed and
 * leading/trailing whitespace is ignored. If the user renames the contact in
 * any way that breaks the pattern, [matches] returns `false` and the
 * `DetectAutoSavedRenameUseCase` flips `isAutoSaved = false` (lenient
 * bucketing per spec §8.5).
 */
object AutoSavePatternMatcher {

    private val cache = ConcurrentHashMap<Key, Regex>()

    private data class Key(
        val prefix: String,
        val includeSimTag: Boolean,
        val suffix: String
    )

    /**
     * Returns a cached, compiled [Regex] for the given settings snapshot.
     * The expression is anchored — callers should pass the entire display
     * name to [Regex.matches].
     */
    fun regexFor(prefix: String, includeSimTag: Boolean, suffix: String): Regex {
        val key = Key(prefix.trim(), includeSimTag, suffix.trim())
        return cache.getOrPut(key) {
            val simBlock = if (includeSimTag) "(?:\\s*-s[12])?" else "(?:\\s*-s[12])?"
            // accept SIM tag even if includeSimTag is false (history)
            val pre = Regex.escape(key.prefix)
            val suf = if (key.suffix.isEmpty()) "" else Regex.escape(key.suffix)
            // The number block: " +<E164>" — `+` followed by 6–15 digits.
            Regex("^\\s*$pre$simBlock\\s+\\+\\d{6,15}$suf\\s*$")
        }
    }

    /** True if [displayName] looks like the configured auto-save pattern. */
    fun matches(
        displayName: String?,
        prefix: String,
        includeSimTag: Boolean,
        suffix: String
    ): Boolean {
        if (displayName.isNullOrBlank()) return false
        return regexFor(prefix, includeSimTag, suffix).matches(displayName.trim())
    }

    /** Test-only: clear the regex cache. */
    fun clearCache() = cache.clear()
}
