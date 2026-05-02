package com.callvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted auto-tag rule. `conditionsJson` and `actionsJson` are serialized
 * lists of `domain.model.RuleCondition` / `RuleAction`. JSON is used (rather
 * than child tables) so a rule round-trips through backup/restore as a single
 * blob and so existing rules survive future schema additions to the sealed
 * hierarchies.
 */
@Entity(tableName = "auto_tag_rules")
data class AutoTagRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val conditionsJson: String,
    val actionsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
