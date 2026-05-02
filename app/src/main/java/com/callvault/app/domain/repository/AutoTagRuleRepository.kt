package com.callvault.app.domain.repository

import com.callvault.app.domain.model.AutoTagRule
import kotlinx.coroutines.flow.Flow

interface AutoTagRuleRepository {
    fun observeAll(): Flow<List<AutoTagRule>>
    suspend fun activeRules(): List<AutoTagRule>
    suspend fun upsert(rule: AutoTagRule): Long
    suspend fun delete(rule: AutoTagRule)
}
