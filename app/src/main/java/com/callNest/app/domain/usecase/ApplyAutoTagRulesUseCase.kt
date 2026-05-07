package com.callNest.app.domain.usecase

import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.dao.TagDao
import com.callNest.app.data.local.mapper.toDomain
import com.callNest.app.domain.model.AutoTagRule
import com.callNest.app.domain.model.Call
import com.callNest.app.domain.repository.AutoTagRuleRepository
import com.callNest.app.domain.repository.ContactRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber

/**
 * Sprint 6 — orchestrates auto-tag rule evaluation against a freshly synced
 * batch of calls. For every active rule (sorted by `sortOrder`) we build a
 * [RuleEvalContext] per call, AND-evaluate the conditions, and apply each
 * action through [RuleActionApplier].
 *
 * Errors per rule are logged and swallowed so a single broken rule never
 * blocks the rest of the sync pipeline (spec §13).
 */
@Singleton
class ApplyAutoTagRulesUseCase @Inject constructor(
    private val ruleRepo: AutoTagRuleRepository,
    private val evaluator: RuleConditionEvaluator,
    private val applier: RuleActionApplier,
    private val tagDao: TagDao,
    private val callDao: CallDao,
    private val contactRepo: ContactRepository
) {

    /**
     * Apply every active rule to [newCalls].
     *
     * @return [RuleApplicationSummary] with per-run counts useful for logging
     *         and the live "match count" badge in the editor.
     */
    suspend operator fun invoke(newCalls: List<Call>): RuleApplicationSummary {
        if (newCalls.isEmpty()) return RuleApplicationSummary(0, 0)
        val rules = runCatching { ruleRepo.activeRules() }
            .onFailure { Timber.w(it, "ApplyAutoTagRulesUseCase: failed to load rules") }
            .getOrDefault(emptyList())
        if (rules.isEmpty()) return RuleApplicationSummary(0, 0)

        val tz = TimeZone.currentSystemDefault()
        var applications = 0

        for (call in newCalls) {
            val ctx = buildContext(call, tz)
            for (rule in rules) {
                if (!rule.isActive) continue
                val matched = runCatching { evaluator.evaluateAll(rule.conditions, ctx) }
                    .onFailure { Timber.w(it, "Rule ${rule.id} evaluation failed") }
                    .getOrDefault(false)
                if (!matched) continue
                for (action in rule.actions) {
                    val ok = applier.apply(action, call, rule.id)
                    if (ok) applications++
                }
            }
        }
        return RuleApplicationSummary(rules.size, applications)
    }

    /** Compute live match count for [rule] against the latest [maxCalls] calls. */
    suspend fun previewMatchCount(rule: AutoTagRule, maxCalls: Int = 200): Int {
        val tz = TimeZone.currentSystemDefault()
        val recent = runCatching { callDao.recentCallsForPreview(maxCalls) }
            .getOrNull()
            ?: return 0
        var count = 0
        for (entity in recent) {
            val call = entity.toDomain()
            val ctx = buildContext(call, tz)
            if (evaluator.evaluateAll(rule.conditions, ctx)) count++
        }
        return count
    }

    private suspend fun buildContext(
        call: Call,
        tz: TimeZone
    ): RuleEvalContext {
        val meta = runCatching { contactRepo.getByNumber(call.normalizedNumber) }.getOrNull()
        val appliedTagIds = runCatching {
            tagDao.tagIdsForCall(call.systemId).toSet()
        }.getOrDefault(emptySet())
        val callCount = meta?.totalCalls ?: 1
        return RuleEvalContext(
            call = call,
            contactMeta = meta,
            appliedTagIds = appliedTagIds,
            localTime = call.date.toLocalDateTime(tz),
            callCount = callCount
        )
    }
}

/** Aggregate summary returned from [ApplyAutoTagRulesUseCase.invoke]. */
data class RuleApplicationSummary(
    /** Number of active rules considered. */
    val totalRules: Int,
    /** Number of action applications that mutated state. */
    val totalApplications: Int
)
