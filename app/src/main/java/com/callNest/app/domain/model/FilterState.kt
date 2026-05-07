package com.callNest.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * The full set of filter knobs exposed by the Calls filter sheet (spec §3.4).
 * Defaults mean "no filter applied".
 */
@Serializable
data class FilterState(
    val dateFrom: Long? = null,
    val dateTo: Long? = null,
    val callTypes: Set<Int> = emptySet(),
    val simSlots: Set<Int> = emptySet(),
    val tagIds: Set<Long> = emptySet(),
    val excludedTagIds: Set<Long> = emptySet(),
    val onlyBookmarked: Boolean = false,
    val onlyWithFollowUp: Boolean = false,
    val onlyUnsaved: Boolean = false,
    val onlySaved: Boolean = false,
    val minDurationSec: Int? = null,
    val maxDurationSec: Int? = null,
    val minLeadScore: Int? = null,
    val countryIsos: Set<String> = emptySet(),
    val freeText: String? = null,
    val sort: SortMode = SortMode.DATE_DESC
) {
    /** Available sort orders. */
    @Serializable
    enum class SortMode {
        DATE_DESC,
        DATE_ASC,
        DURATION_DESC,
        LEAD_SCORE_DESC,
        FREQUENCY_DESC
    }

    /** Convenience accessor that promotes the [Long] timestamps to [Instant]. */
    val dateFromInstant: Instant? get() = dateFrom?.let(Instant::fromEpochMilliseconds)
    val dateToInstant: Instant? get() = dateTo?.let(Instant::fromEpochMilliseconds)
}
