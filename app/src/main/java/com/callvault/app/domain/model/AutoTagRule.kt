package com.callvault.app.domain.model

import kotlinx.datetime.Instant

/**
 * Domain representation of an auto-tag rule. Conditions are AND-ed; actions
 * are applied sequentially in the order given.
 */
data class AutoTagRule(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val sortOrder: Int,
    val conditions: List<RuleCondition>,
    val actions: List<RuleAction>,
    val createdAt: Instant
)
