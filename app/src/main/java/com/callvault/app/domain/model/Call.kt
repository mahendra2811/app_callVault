package com.callvault.app.domain.model

import kotlinx.datetime.Instant

/**
 * Pure-Kotlin domain projection of a call. UI and use cases work with this
 * type; the Room layer converts to/from `CallEntity` at its boundary.
 */
data class Call(
    val systemId: Long,
    val rawNumber: String,
    val normalizedNumber: String,
    val date: Instant,
    val durationSec: Int,
    val type: CallType,
    val cachedName: String?,
    val phoneAccountId: String?,
    val simSlot: Int?,
    val carrierName: String?,
    val geocodedLocation: String?,
    val countryIso: String?,
    val isNew: Boolean,
    val isBookmarked: Boolean,
    val bookmarkReason: String?,
    val followUpAt: Instant?,
    val followUpMinuteOfDay: Int?,
    val followUpNote: String?,
    val followUpDoneAt: Instant?,
    val leadScore: Int,
    val leadScoreManualOverride: Int?,
    val isArchived: Boolean,
    val deletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

/** Mirrors `android.provider.CallLog.Calls.TYPE_*`. */
enum class CallType(val raw: Int) {
    INCOMING(1),
    OUTGOING(2),
    MISSED(3),
    VOICEMAIL(4),
    REJECTED(5),
    BLOCKED(6),
    ANSWERED_EXTERNALLY(7),
    UNKNOWN(0);

    companion object {
        fun fromRaw(raw: Int): CallType = entries.firstOrNull { it.raw == raw } ?: UNKNOWN
    }
}
