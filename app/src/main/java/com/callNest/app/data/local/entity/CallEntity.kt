package com.callNest.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent record of a single call log row, enriched with callNest metadata.
 *
 * Mirrors spec §4 Data Model exactly. Column names, indices and defaults must
 * stay byte-identical with the spec because Sprint 9 export and Sprint 10
 * backup/restore depend on the schema being stable across versions.
 */
@Entity(
    tableName = "calls",
    indices = [
        Index("normalizedNumber"),
        Index("date"),
        Index("type"),
        Index("isBookmarked"),
        Index("followUpDate")
    ]
)
data class CallEntity(
    /** Mirrors `CallLog.Calls._ID` for the underlying row. Stable per-device. */
    @PrimaryKey val systemId: Long,
    val rawNumber: String,
    /** E.164 normalized number; empty string for private/withheld numbers. */
    val normalizedNumber: String,
    /** Epoch millis at which the call started. */
    val date: Long,
    /** Duration in seconds. */
    val duration: Int,
    /** One of `CallLog.Calls.TYPE_*` (1..7). */
    val type: Int,
    val cachedName: String?,
    val phoneAccountId: String?,
    val simSlot: Int?,
    val carrierName: String?,
    val geocodedLocation: String?,
    val countryIso: String?,
    val isNew: Boolean,
    val isBookmarked: Boolean = false,
    val bookmarkReason: String? = null,
    val followUpDate: Long? = null,
    /** Minutes-of-day for the follow-up. `null` means use 09:00 default. */
    val followUpTime: Int? = null,
    val followUpNote: String? = null,
    val followUpDoneAt: Long? = null,
    val leadScore: Int = 0,
    val leadScoreManualOverride: Int? = null,
    val isArchived: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
