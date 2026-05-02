package com.callvault.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Result of a lead-score computation, carrying the breakdown so the UI can
 * show "why this score?".
 *
 * `total` is clamped to 0..100. The breakdown fields are the raw component
 * contributions (already weighted) so callers can render a "score breakdown"
 * tooltip without re-deriving anything.
 */
data class LeadScore(
    val total: Int,
    val frequency: Double,
    val duration: Double,
    val recency: Double,
    val followUpBonus: Int,
    val customerTagBonus: Int,
    val savedContactBonus: Int,
    val ruleBoosts: Int,
    /** When the user has manually overridden the score, the override value
     *  is propagated here so the UI can show a "Manual" badge. */
    val manualOverride: Int? = null
) {
    init {
        require(total in 0..100) { "Lead score must be clamped to 0..100" }
    }
}

/**
 * Configurable weights and bonuses for the lead-score formula (spec §8.2).
 *
 * The three weight values are fractions in `0..1` that scale the
 * frequency / duration / recency normalised components (each already 0..100).
 * The three bonus values are flat point bumps added when the matching
 * predicate holds.
 *
 * Defaults match the spec verbatim.
 */
@Serializable
data class LeadScoreWeights(
    /** Weight applied to the normalised call-count component (0..1). */
    val weightFreq: Double = 0.25,
    /** Weight applied to the normalised total-duration component (0..1). */
    val weightDuration: Double = 0.20,
    /** Weight applied to the recency-decay component (0..1). */
    val weightRecency: Double = 0.25,
    /** Bonus points when the contact has an open follow-up. */
    val bonusFollowUp: Int = 10,
    /** Bonus points when at least one "customer" tag is applied. */
    val bonusCustomerTag: Int = 20,
    /** Bonus points when the number is saved in the device contacts. */
    val bonusSavedContact: Int = 15
) {
    companion object {
        /** Spec default weights — see §8.2 / §3.13. */
        val DEFAULT = LeadScoreWeights()
    }

    // Backward-compat aliases for existing call-sites that read the trio.
    val frequency: Double get() = weightFreq
    val duration: Double get() = weightDuration
    val recency: Double get() = weightRecency
}
