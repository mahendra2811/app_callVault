package com.callvault.app.data.repository

import com.callvault.app.data.local.dao.CallDao
import com.callvault.app.data.local.mapper.toDomain
import com.callvault.app.data.local.mapper.toEntity
import com.callvault.app.domain.model.Call
import com.callvault.app.domain.model.FilterState
import com.callvault.app.domain.repository.CallRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Singleton
class CallRepositoryImpl @Inject constructor(
    private val dao: CallDao
) : CallRepository {

    override fun observeRecent(limit: Int, offset: Int): Flow<List<Call>> =
        dao.observeRecent(limit, offset).map { it.map { e -> e.toDomain() } }

    override fun observeForNumber(number: String): Flow<List<Call>> =
        dao.observeForNumber(number).map { it.map { e -> e.toDomain() } }

    override fun observeBookmarked(): Flow<List<Call>> =
        dao.observeBookmarked().map { it.map { e -> e.toDomain() } }

    override fun observePendingFollowUps(): Flow<List<Call>> =
        dao.observePendingFollowUps().map { it.map { e -> e.toDomain() } }

    override fun observeGroupedByNumber(): Flow<List<Call>> =
        dao.observeGroupedByNumber().map { it.map { e -> e.toDomain() } }

    override fun observeByLeadScore(limit: Int): Flow<List<Call>> =
        dao.observeByLeadScore(limit).map { it.map { e -> e.toDomain() } }

    override fun observeFiltered(filter: FilterState): Flow<List<Call>> =
        dao.observeRaw(FilterQueryBuilder.build(filter))
            .map { it.map { e -> e.toDomain() } }

    override fun observeUnsavedLast7Days(): Flow<List<Call>> {
        val cutoff = System.currentTimeMillis() - SEVEN_DAYS_MS
        return dao.observeUnsavedSince(cutoff).map { it.map { e -> e.toDomain() } }
    }

    override suspend fun getById(id: Long): Call? = dao.getById(id)?.toDomain()

    override suspend fun maxSystemId(): Long = dao.maxSystemId()

    override suspend fun upsert(call: Call) = dao.upsert(call.toEntity())

    override suspend fun upsertAll(calls: List<Call>) = dao.upsertAll(calls.map { it.toEntity() })

    override suspend fun setBookmarked(id: Long, flag: Boolean, reason: String?) {
        dao.setBookmarked(id, flag, reason)
    }

    override suspend fun toggleBookmark(callId: Long, reason: String?) {
        val current = dao.getById(callId) ?: return
        dao.setBookmarked(callId, !current.isBookmarked, reason ?: current.bookmarkReason)
    }

    override suspend fun setArchived(callId: Long, archived: Boolean) {
        dao.setArchived(callId, archived)
    }

    override suspend fun softDelete(id: Long) = dao.softDelete(id)

    override suspend fun deleteForNumber(normalizedNumber: String) {
        dao.deleteForNumber(normalizedNumber)
    }

    override suspend fun setFollowUp(
        callId: Long,
        triggerMs: Long?,
        minuteOfDay: Int?,
        note: String?
    ) {
        dao.setFollowUp(
            id = callId,
            triggerMs = triggerMs,
            minuteOfDay = minuteOfDay,
            note = note
        )
    }

    override suspend fun markFollowUpDone(callId: Long) {
        dao.markFollowUpDone(callId)
    }

    override suspend fun activeFollowUpForNumber(normalizedNumber: String): Call? =
        dao.activeFollowUpForNumber(normalizedNumber)?.toDomain()

    override suspend fun search(query: String): List<Call> {
        val fts = buildFtsQuery(query)
        if (fts.isBlank()) return emptyList()
        // Union of call FTS + note FTS, re-fetched in date-desc order.
        val ids = dao.searchUnionIds(fts).distinct()
        if (ids.isEmpty()) return emptyList()
        return dao.getByIdsOrdered(ids).map { it.toDomain() }
    }

    override fun searchFts(query: String): Flow<List<Call>> {
        val fts = buildFtsQuery(query)
        return if (fts.isBlank()) flow { emit(emptyList()) }
        else dao.observeSearchFts(fts).map { it.map { e -> e.toDomain() } }
    }

    /** Spec §8.6 — token-prefix MATCH. */
    private fun buildFtsQuery(input: String): String =
        input.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                "${token.replace("\"", "\"\"")}*"
            }

    private companion object {
        const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }
}
