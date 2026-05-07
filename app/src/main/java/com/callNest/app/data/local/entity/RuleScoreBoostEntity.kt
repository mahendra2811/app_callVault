package com.callNest.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index

/**
 * Persisted lead-score boost emitted by an auto-tag rule action
 * `RuleAction.LeadScoreBoost` (spec §3.7 + §8.3).
 *
 * One row per (call, rule) pair so we can:
 * - Sum the per-number `delta` when [com.callNest.app.domain.usecase.ComputeLeadScoreUseCase]
 *   recomputes a score.
 * - Cascade-clean every boost a rule produced when the rule is deleted
 *   (`AutoTagRuleRepositoryImpl.delete`).
 *
 * Rows reference [CallEntity] with `ON DELETE CASCADE` so soft/hard call
 * deletion automatically retires the matching boost.
 */
@Entity(
    tableName = "rule_score_boosts",
    primaryKeys = ["callSystemId", "ruleId"],
    foreignKeys = [
        ForeignKey(
            entity = CallEntity::class,
            parentColumns = ["systemId"],
            childColumns = ["callSystemId"],
            onDelete = CASCADE
        )
    ],
    indices = [Index("ruleId")]
)
data class RuleScoreBoostEntity(
    val callSystemId: Long,
    val ruleId: Long,
    /** Signed delta in raw score points; clamped to [-100, 100] by the applier. */
    val delta: Int,
    val appliedAt: Long = System.currentTimeMillis()
)
