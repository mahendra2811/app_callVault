package com.callvault.app.domain.model

import kotlinx.serialization.Serializable

/**
 * A single predicate evaluated against a [Call] when an auto-tag rule fires
 * (spec §3.7). All variants are `@Serializable` so a list of them can be
 * persisted as JSON in `AutoTagRuleEntity.conditionsJson`.
 *
 * The polymorphic class discriminator (`type`) is added by kotlinx
 * serialization; the AutoTagRule JSON module configures it project-wide.
 */
@Serializable
sealed interface RuleCondition {

    /** True when the call's `rawNumber` starts with [prefix]. */
    @Serializable
    data class PrefixMatches(val prefix: String) : RuleCondition

    /** True when the call's `rawNumber` matches the given regex. */
    @Serializable
    data class RegexMatches(val pattern: String) : RuleCondition

    /** True when the resolved `countryIso` equals [iso]. */
    @Serializable
    data class CountryEquals(val iso: String) : RuleCondition

    /** True when the call's number is already saved in the device contacts. */
    @Serializable
    data class IsInContacts(val expected: Boolean) : RuleCondition

    /** True when the call type is one of [types] (CallLog.Calls.TYPE_*). */
    @Serializable
    data class CallTypeIn(val types: Set<Int>) : RuleCondition

    /** Compares duration (seconds) against [seconds] using [op]. */
    @Serializable
    data class DurationCompare(val op: CompareOp, val seconds: Int) : RuleCondition

    /** Inclusive minute-of-day window. */
    @Serializable
    data class TimeOfDayBetween(val startMinute: Int, val endMinute: Int) : RuleCondition

    /** Day-of-week filter (1 = Monday, 7 = Sunday — ISO). */
    @Serializable
    data class DayOfWeekIn(val days: Set<Int>) : RuleCondition

    /** Specific SIM slot match (0-based). */
    @Serializable
    data class SimSlotEquals(val slot: Int) : RuleCondition

    /** True when the call already has [tagId] applied. */
    @Serializable
    data class TagApplied(val tagId: Long) : RuleCondition

    /** True when the call does NOT have [tagId] applied. */
    @Serializable
    data class TagNotApplied(val tagId: Long) : RuleCondition

    /** True when `geocodedLocation` contains [needle] (case-insensitive). */
    @Serializable
    data class GeoContains(val needle: String) : RuleCondition

    /** True when the contact has more than [count] calls overall. */
    @Serializable
    data class CallCountGreaterThan(val count: Int) : RuleCondition

    /** Comparison operators for numeric conditions. */
    @Serializable
    enum class CompareOp { LT, LTE, EQ, GTE, GT }
}
