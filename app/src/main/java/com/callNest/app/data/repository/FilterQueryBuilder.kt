package com.callNest.app.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.callNest.app.domain.model.FilterState

/**
 * Compiles a [FilterState] into a parameterised SELECT against the `calls`
 * table. Joins `contact_meta` only when the filter actually needs it
 * (saved/unsaved tabs) so cheap default queries stay cheap.
 *
 * The output is bound through [SimpleSQLiteQuery] so Room's invalidation
 * tracker still observes `CallEntity` for live updates (see
 * `CallDao.observeRaw`).
 */
internal object FilterQueryBuilder {

    fun build(filter: FilterState): SupportSQLiteQuery {
        val args = mutableListOf<Any>()
        val where = mutableListOf<String>()
        val joins = mutableListOf<String>()

        where += "c.deletedAt IS NULL"
        where += "c.isArchived = 0"

        filter.dateFrom?.let {
            where += "c.date >= ?"
            args += it
        }
        filter.dateTo?.let {
            where += "c.date <= ?"
            args += it
        }
        if (filter.callTypes.isNotEmpty()) {
            where += "c.type IN (${placeholders(filter.callTypes.size)})"
            args.addAll(filter.callTypes.map { it as Any })
        }
        if (filter.simSlots.isNotEmpty()) {
            where += "c.simSlot IN (${placeholders(filter.simSlots.size)})"
            args.addAll(filter.simSlots.map { it as Any })
        }
        if (filter.onlyBookmarked) where += "c.isBookmarked = 1"
        if (filter.onlyWithFollowUp) {
            where += "c.followUpDate IS NOT NULL AND c.followUpDoneAt IS NULL"
        }
        if (filter.minDurationSec != null) {
            where += "c.duration >= ?"
            args += filter.minDurationSec
        }
        if (filter.maxDurationSec != null) {
            where += "c.duration <= ?"
            args += filter.maxDurationSec
        }
        if (filter.minLeadScore != null) {
            where += "c.leadScore >= ?"
            args += filter.minLeadScore
        }
        if (filter.countryIsos.isNotEmpty()) {
            where += "c.countryIso IN (${placeholders(filter.countryIsos.size)})"
            args.addAll(filter.countryIsos.map { it as Any })
        }

        if (filter.onlyUnsaved || filter.onlySaved) {
            joins += "JOIN contact_meta m ON m.normalizedNumber = c.normalizedNumber"
            if (filter.onlyUnsaved) where += "m.isInSystemContacts = 0 AND m.isAutoSaved = 0"
            if (filter.onlySaved) where += "(m.isInSystemContacts = 1 OR m.isAutoSaved = 1)"
        }

        if (filter.tagIds.isNotEmpty()) {
            // Use EXISTS so every required tag must be present.
            for (tagId in filter.tagIds) {
                where += "EXISTS (SELECT 1 FROM call_tag_cross_ref x WHERE x.callSystemId = c.systemId AND x.tagId = ?)"
                args += tagId
            }
        }
        if (filter.excludedTagIds.isNotEmpty()) {
            where += "NOT EXISTS (SELECT 1 FROM call_tag_cross_ref x WHERE x.callSystemId = c.systemId AND x.tagId IN (${placeholders(filter.excludedTagIds.size)}))"
            args.addAll(filter.excludedTagIds.map { it as Any })
        }

        if (!filter.freeText.isNullOrBlank()) {
            val like = "%${filter.freeText.trim()}%"
            where += "(c.cachedName LIKE ? OR c.normalizedNumber LIKE ? OR c.rawNumber LIKE ?)"
            args.addAll(listOf(like, like, like))
        }

        val orderBy = when (filter.sort) {
            FilterState.SortMode.DATE_DESC -> "c.date DESC"
            FilterState.SortMode.DATE_ASC -> "c.date ASC"
            FilterState.SortMode.DURATION_DESC -> "c.duration DESC, c.date DESC"
            FilterState.SortMode.LEAD_SCORE_DESC -> "c.leadScore DESC, c.date DESC"
            FilterState.SortMode.FREQUENCY_DESC -> "c.date DESC" // freq fallback
        }

        val sql = buildString {
            append("SELECT c.* FROM calls c ")
            joins.forEach { append(it); append(' ') }
            if (where.isNotEmpty()) {
                append("WHERE ")
                append(where.joinToString(" AND "))
                append(' ')
            }
            append("ORDER BY ")
            append(orderBy)
            append(" LIMIT 1000")
        }
        // Flatten list-of-list arg expansion.
        val flat = args.flatMap { if (it is Collection<*>) it.filterNotNull() else listOf(it) }
        return SimpleSQLiteQuery(sql, flat.toTypedArray())
    }

    private fun placeholders(n: Int): String = List(n) { "?" }.joinToString(", ")
}
