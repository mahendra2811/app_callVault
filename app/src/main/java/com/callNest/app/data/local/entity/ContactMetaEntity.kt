package com.callNest.app.data.local.entity

import androidx.room.Entity

/**
 * Per-number aggregate row, recomputed at the end of every sync (spec §8.1
 * step 5). Acts as a fast index for the calls list, lead score badges and
 * the Stats dashboard.
 */
@Entity(tableName = "contact_meta", primaryKeys = ["normalizedNumber"])
data class ContactMetaEntity(
    val normalizedNumber: String,
    val displayName: String?,
    val isInSystemContacts: Boolean,
    val systemContactId: Long?,
    val systemRawContactId: Long?,
    val isAutoSaved: Boolean = false,
    val autoSavedAt: Long? = null,
    /** Snapshot of the auto-save name pattern used at save time. */
    val autoSavedFormat: String? = null,
    val firstCallDate: Long,
    val lastCallDate: Long,
    val totalCalls: Int,
    val totalDuration: Int,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int,
    val computedLeadScore: Int,
    /** Free-text "where this lead came from", user-set. */
    val source: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
