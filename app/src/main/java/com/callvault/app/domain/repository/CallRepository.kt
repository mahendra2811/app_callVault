package com.callvault.app.domain.repository

import com.callvault.app.domain.model.Call
import com.callvault.app.domain.model.FilterState
import kotlinx.coroutines.flow.Flow

/** Read/write surface for call rows. */
interface CallRepository {

    fun observeRecent(limit: Int = 200, offset: Int = 0): Flow<List<Call>>
    fun observeForNumber(number: String): Flow<List<Call>>
    fun observeBookmarked(): Flow<List<Call>>
    fun observePendingFollowUps(): Flow<List<Call>>
    fun observeGroupedByNumber(): Flow<List<Call>>
    fun observeByLeadScore(limit: Int = 100): Flow<List<Call>>
    fun observeFiltered(filter: FilterState): Flow<List<Call>>

    /**
     * Calls from numbers that aren't in the device contacts and weren't
     * auto-saved yet, restricted to the last 7 days. Drives the
     * "Unsaved inquiries" pinned section on the Calls screen (spec §3.2).
     */
    fun observeUnsavedLast7Days(): Flow<List<Call>>

    suspend fun getById(id: Long): Call?
    suspend fun maxSystemId(): Long
    suspend fun upsert(call: Call)
    suspend fun upsertAll(calls: List<Call>)
    suspend fun setBookmarked(id: Long, flag: Boolean, reason: String?)
    suspend fun toggleBookmark(callId: Long, reason: String? = null)
    suspend fun setArchived(callId: Long, archived: Boolean)
    suspend fun softDelete(id: Long)
    suspend fun deleteForNumber(normalizedNumber: String)

    /** Persist follow-up fields on a single call row. */
    suspend fun setFollowUp(
        callId: Long,
        triggerMs: Long?,
        minuteOfDay: Int?,
        note: String?
    )

    /** Mark the active follow-up on this call as done (no notification). */
    suspend fun markFollowUpDone(callId: Long)

    /** Latest non-done follow-up on a number, or null if there's none. */
    suspend fun activeFollowUpForNumber(normalizedNumber: String): Call?

    /** FTS prefix-matching search joined with the calls table (spec §8.6). */
    suspend fun search(query: String): List<Call>

    /** Reactive variant — debounced searches on the Search screen subscribe here. */
    fun searchFts(query: String): Flow<List<Call>>
}
