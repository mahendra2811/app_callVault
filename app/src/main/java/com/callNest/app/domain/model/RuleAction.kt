package com.callNest.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Effects applied when a rule's conditions match. Persisted alongside the
 * rule as JSON; therefore every variant is `@Serializable`.
 */
@Serializable
sealed interface RuleAction {

    /** Apply [tagId] to the matching call. */
    @Serializable
    data class ApplyTag(val tagId: Long) : RuleAction

    /** Add [delta] points to the call's lead score. May be negative. */
    @Serializable
    data class LeadScoreBoost(val delta: Int) : RuleAction

    /** Bookmark the call automatically with optional [reason]. */
    @Serializable
    data class AutoBookmark(val reason: String? = null) : RuleAction

    /** Schedule a follow-up [hoursFromNow] hours after the call. */
    @Serializable
    data class MarkFollowUp(val hoursFromNow: Int) : RuleAction
}
