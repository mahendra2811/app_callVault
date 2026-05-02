package com.callvault.app.domain.usecase

import com.callvault.app.domain.model.Call
import com.callvault.app.domain.model.ContactMeta
import com.callvault.app.domain.model.RuleCondition
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.LocalDateTime

/**
 * Snapshot of every state a [RuleCondition] may inspect (spec §3.7 / §8.3).
 *
 * The evaluator is deliberately a pure function of this context — that makes
 * the live preview on [com.callvault.app.ui.screen.autotagrules.RuleEditorScreen]
 * trivial to drive against the latest 200 calls without going back to Room.
 *
 * @property call the call being matched
 * @property contactMeta cached meta for the call's number, or null if missing
 * @property appliedTagIds tag ids already attached to the call
 * @property localTime the call's date converted to the device's default zone
 * @property callCount running total calls for this number (fed in by the caller)
 */
data class RuleEvalContext(
    val call: Call,
    val contactMeta: ContactMeta?,
    val appliedTagIds: Set<Long>,
    val localTime: LocalDateTime,
    val callCount: Int
)

/**
 * Pure evaluator for [RuleCondition] variants. Defensive on every side:
 * - Bad regexes return `false`.
 * - Missing optional fields (`countryIso`, `geocodedLocation`, …) return `false`.
 * - Numeric comparisons against null fields return `false`.
 *
 * Conditions inside a single rule are AND-ed by [ApplyAutoTagRulesUseCase].
 */
@Singleton
class RuleConditionEvaluator @Inject constructor() {

    /** Returns `true` if [condition] matches [ctx]. Never throws. */
    fun evaluate(condition: RuleCondition, ctx: RuleEvalContext): Boolean = try {
        when (condition) {
            is RuleCondition.PrefixMatches ->
                ctx.call.rawNumber.startsWith(condition.prefix) ||
                    ctx.call.normalizedNumber.startsWith(condition.prefix)

            is RuleCondition.RegexMatches -> {
                val regex = runCatching { Regex(condition.pattern) }.getOrNull()
                    ?: return false
                regex.containsMatchIn(ctx.call.rawNumber) ||
                    regex.containsMatchIn(ctx.call.normalizedNumber)
            }

            is RuleCondition.CountryEquals -> {
                val iso = ctx.call.countryIso ?: return false
                iso.equals(condition.iso, ignoreCase = true)
            }

            is RuleCondition.IsInContacts -> {
                val saved = ctx.contactMeta?.isInSystemContacts ?: false
                saved == condition.expected
            }

            is RuleCondition.CallTypeIn ->
                ctx.call.type.raw in condition.types

            is RuleCondition.DurationCompare -> {
                val sec = ctx.call.durationSec
                when (condition.op) {
                    RuleCondition.CompareOp.LT -> sec < condition.seconds
                    RuleCondition.CompareOp.LTE -> sec <= condition.seconds
                    RuleCondition.CompareOp.EQ -> sec == condition.seconds
                    RuleCondition.CompareOp.GTE -> sec >= condition.seconds
                    RuleCondition.CompareOp.GT -> sec > condition.seconds
                }
            }

            is RuleCondition.TimeOfDayBetween -> {
                val mod = ctx.localTime.hour * 60 + ctx.localTime.minute
                val start = condition.startMinute.coerceIn(0, 1440)
                val end = condition.endMinute.coerceIn(0, 1440)
                if (start <= end) mod in start..end
                else mod >= start || mod <= end // wraps midnight
            }

            is RuleCondition.DayOfWeekIn -> {
                // kotlinx-datetime DayOfWeek: MONDAY=1 .. SUNDAY=7 (ISO).
                val dow = ctx.localTime.dayOfWeek.isoDayNumber
                dow in condition.days
            }

            is RuleCondition.SimSlotEquals -> {
                val slot = ctx.call.simSlot ?: return false
                slot == condition.slot
            }

            is RuleCondition.TagApplied -> condition.tagId in ctx.appliedTagIds
            is RuleCondition.TagNotApplied -> condition.tagId !in ctx.appliedTagIds

            is RuleCondition.GeoContains -> {
                val geo = ctx.call.geocodedLocation ?: return false
                geo.contains(condition.needle, ignoreCase = true)
            }

            is RuleCondition.CallCountGreaterThan ->
                ctx.callCount > condition.count
        }
    } catch (_: Throwable) {
        false
    }

    /** Returns true iff every condition in [conditions] matches. */
    fun evaluateAll(conditions: List<RuleCondition>, ctx: RuleEvalContext): Boolean =
        conditions.all { evaluate(it, ctx) }
}

/** kotlinx.datetime helper — ISO Mon=1 … Sun=7. */
private val kotlinx.datetime.DayOfWeek.isoDayNumber: Int
    get() = isoDayNumber()

private fun kotlinx.datetime.DayOfWeek.isoDayNumber(): Int = when (this) {
    kotlinx.datetime.DayOfWeek.MONDAY -> 1
    kotlinx.datetime.DayOfWeek.TUESDAY -> 2
    kotlinx.datetime.DayOfWeek.WEDNESDAY -> 3
    kotlinx.datetime.DayOfWeek.THURSDAY -> 4
    kotlinx.datetime.DayOfWeek.FRIDAY -> 5
    kotlinx.datetime.DayOfWeek.SATURDAY -> 6
    kotlinx.datetime.DayOfWeek.SUNDAY -> 7
}
