package com.callNest.app.data.repository

import com.callNest.app.data.local.dao.AutoTagRuleDao
import com.callNest.app.data.local.dao.RuleScoreBoostDao
import com.callNest.app.data.local.dao.TagDao
import com.callNest.app.data.local.mapper.toDomain
import com.callNest.app.data.local.mapper.toEntity
import com.callNest.app.domain.model.AutoTagRule
import com.callNest.app.domain.repository.AutoTagRuleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Default Room-backed implementation of [AutoTagRuleRepository].
 *
 * Sprint 6: [delete] cascades the rule's side-effects so a removed rule does
 * not leave orphaned rows behind:
 * - `call_tag_cross_ref` rows where `appliedBy = "rule:<id>"` are removed.
 * - `rule_score_boosts` rows for the rule are removed.
 */
@Singleton
class AutoTagRuleRepositoryImpl @Inject constructor(
    private val dao: AutoTagRuleDao,
    private val tagDao: TagDao,
    private val ruleScoreBoostDao: RuleScoreBoostDao
) : AutoTagRuleRepository {

    override fun observeAll(): Flow<List<AutoTagRule>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun activeRules(): List<AutoTagRule> =
        dao.activeRules().map { it.toDomain() }

    override suspend fun upsert(rule: AutoTagRule): Long = dao.insert(rule.toEntity())

    override suspend fun delete(rule: AutoTagRule) {
        runCatching { tagDao.removeAllAppliedBy("rule:${rule.id}") }
            .onFailure { Timber.w(it, "delete: removeAllAppliedBy failed for rule=${rule.id}") }
        runCatching { ruleScoreBoostDao.deleteForRule(rule.id) }
            .onFailure { Timber.w(it, "delete: deleteForRule failed for rule=${rule.id}") }
        dao.delete(rule.toEntity())
    }
}
