package com.callNest.app.domain.model

import kotlinx.datetime.Instant

/** Aggregated, per-number summary used by the calls list and stats screens. */
data class ContactMeta(
    val normalizedNumber: String,
    val displayName: String?,
    val isInSystemContacts: Boolean,
    val systemContactId: Long?,
    val systemRawContactId: Long?,
    val isAutoSaved: Boolean,
    val autoSavedAt: Instant?,
    val autoSavedFormat: String?,
    val firstCallDate: Instant,
    val lastCallDate: Instant,
    val totalCalls: Int,
    val totalDuration: Int,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int,
    val computedLeadScore: Int,
    val source: String?,
    val updatedAt: Instant
)
