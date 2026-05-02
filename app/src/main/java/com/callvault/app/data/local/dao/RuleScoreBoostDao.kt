package com.callvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callvault.app.data.local.entity.RuleScoreBoostEntity

/**
 * DAO for the lead-score boost ledger. Boosts are summed per normalized
 * number and added on top of the base lead-score formula (spec §8.3).
 */
@Dao
interface RuleScoreBoostDao {

    /** Idempotent on (callSystemId, ruleId). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(boost: RuleScoreBoostEntity)

    @Query("DELETE FROM rule_score_boosts WHERE ruleId = :ruleId")
    suspend fun deleteForRule(ruleId: Long)

    @Query("DELETE FROM rule_score_boosts WHERE callSystemId = :callId")
    suspend fun deleteForCall(callId: Long)

    /**
     * Sum of boosts for every call belonging to [normalizedNumber]. Joins
     * `calls` so deleted calls are excluded automatically.
     */
    @Query("""
        SELECT IFNULL(SUM(b.delta), 0) FROM rule_score_boosts b
        JOIN calls c ON c.systemId = b.callSystemId
        WHERE c.normalizedNumber = :normalizedNumber
          AND c.deletedAt IS NULL
    """)
    suspend fun totalForNumber(normalizedNumber: String): Int

    @Query("SELECT IFNULL(SUM(delta), 0) FROM rule_score_boosts WHERE callSystemId = :callId")
    suspend fun totalForCall(callId: Long): Int
}
