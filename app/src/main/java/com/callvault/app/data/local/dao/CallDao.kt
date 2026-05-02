package com.callvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.callvault.app.data.local.entity.CallEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to the `calls` table plus FTS-joined search.
 */
@Dao
interface CallDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(call: CallEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(calls: List<CallEntity>)

    @Update
    suspend fun update(call: CallEntity)

    @Query("DELETE FROM calls WHERE systemId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM calls WHERE normalizedNumber = :number")
    suspend fun deleteForNumber(number: String)

    @Query("SELECT * FROM calls WHERE systemId = :id")
    suspend fun getById(id: Long): CallEntity?

    @Query("SELECT * FROM calls WHERE systemId = :id")
    fun observeById(id: Long): Flow<CallEntity?>

    /** Highest `_ID` seen so far. Used as the watermark for incremental sync. */
    @Query("SELECT IFNULL(MAX(systemId), 0) FROM calls")
    suspend fun maxSystemId(): Long

    @Query("SELECT * FROM calls WHERE deletedAt IS NULL ORDER BY date DESC LIMIT :limit OFFSET :offset")
    fun observeRecent(limit: Int = 200, offset: Int = 0): Flow<List<CallEntity>>

    /** Snapshot used by [com.callvault.app.domain.usecase.ApplyAutoTagRulesUseCase]
     *  for the live "match count" preview in the rule editor. */
    @Query("SELECT * FROM calls WHERE deletedAt IS NULL ORDER BY date DESC LIMIT :limit")
    suspend fun recentCallsForPreview(limit: Int): List<CallEntity>

    @Query("""
        SELECT * FROM calls
        WHERE deletedAt IS NULL
          AND date BETWEEN :fromMs AND :toMs
        ORDER BY date DESC
    """)
    fun observeBetween(fromMs: Long, toMs: Long): Flow<List<CallEntity>>

    @Query("""
        SELECT * FROM calls
        WHERE deletedAt IS NULL
          AND type IN (:types)
          AND date BETWEEN :fromMs AND :toMs
        ORDER BY date DESC
    """)
    fun observeByTypes(types: List<Int>, fromMs: Long, toMs: Long): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE isBookmarked = 1 AND deletedAt IS NULL ORDER BY date DESC")
    fun observeBookmarked(): Flow<List<CallEntity>>

    @Query("""
        SELECT * FROM calls
        WHERE followUpDate IS NOT NULL
          AND followUpDoneAt IS NULL
          AND deletedAt IS NULL
        ORDER BY followUpDate ASC
    """)
    fun observePendingFollowUps(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE normalizedNumber = :number AND deletedAt IS NULL ORDER BY date DESC")
    fun observeForNumber(number: String): Flow<List<CallEntity>>

    /** Most-recent call for [number] (used by Sprint 5 auto-save sampling). */
    @Query("SELECT * FROM calls WHERE normalizedNumber = :number AND deletedAt IS NULL ORDER BY date DESC LIMIT 1")
    suspend fun latestForNumber(number: String): CallEntity?

    @Query("SELECT * FROM calls WHERE deletedAt IS NULL ORDER BY leadScore DESC, date DESC LIMIT :limit")
    fun observeByLeadScore(limit: Int = 100): Flow<List<CallEntity>>

    /**
     * One row per distinct number with the latest call time, used by the
     * "Group by number" toggle on the Calls screen.
     */
    @Query("""
        SELECT * FROM calls c
        WHERE deletedAt IS NULL
          AND date = (
            SELECT MAX(date) FROM calls
            WHERE normalizedNumber = c.normalizedNumber
              AND deletedAt IS NULL
          )
        ORDER BY date DESC
    """)
    fun observeGroupedByNumber(): Flow<List<CallEntity>>

    /**
     * Calls from numbers not in system contacts and not auto-saved, in the
     * last 7 days. Drives the unsaved-pinned section.
     */
    @Query("""
        SELECT c.* FROM calls c
        JOIN contact_meta m ON m.normalizedNumber = c.normalizedNumber
        WHERE c.deletedAt IS NULL
          AND c.isArchived = 0
          AND m.isInSystemContacts = 0
          AND m.isAutoSaved = 0
          AND c.date >= :sinceMs
        ORDER BY c.date DESC
    """)
    fun observeUnsavedSince(sinceMs: Long): Flow<List<CallEntity>>

    /**
     * FTS prefix-match search joined to the parent table (spec §8.6).
     * Caller must build `query` as `token1* token2* ...`.
     */
    @Query("""
        SELECT calls.* FROM calls
        JOIN call_fts ON call_fts.rowid = calls.systemId
        WHERE call_fts MATCH :ftsQuery
          AND calls.deletedAt IS NULL
        ORDER BY calls.date DESC
        LIMIT 200
    """)
    suspend fun searchFts(ftsQuery: String): List<CallEntity>

    /** Reactive variant of [searchFts] for the Search screen. */
    @Query("""
        SELECT calls.* FROM calls
        JOIN call_fts ON call_fts.rowid = calls.systemId
        WHERE call_fts MATCH :ftsQuery
          AND calls.deletedAt IS NULL
        ORDER BY calls.date DESC
        LIMIT 200
    """)
    fun observeSearchFts(ftsQuery: String): Flow<List<CallEntity>>

    /**
     * Joined FTS over both calls + notes. Returned ids are de-duplicated and
     * re-fetched in date-desc order via [getByIdsOrdered].
     */
    @Query("""
        SELECT calls.systemId FROM calls
        JOIN call_fts ON call_fts.rowid = calls.systemId
        WHERE call_fts MATCH :ftsQuery AND calls.deletedAt IS NULL
        UNION
        SELECT notes.callSystemId FROM notes
        JOIN note_fts ON note_fts.rowid = notes.id
        WHERE note_fts MATCH :ftsQuery AND notes.callSystemId IS NOT NULL
    """)
    suspend fun searchUnionIds(ftsQuery: String): List<Long>

    @Query("SELECT * FROM calls WHERE systemId IN (:ids) AND deletedAt IS NULL ORDER BY date DESC")
    suspend fun getByIdsOrdered(ids: List<Long>): List<CallEntity>

    /** Run a dynamically composed filter SELECT (Sprint 3 filter sheet). */
    @RawQuery(observedEntities = [CallEntity::class])
    fun observeRaw(query: SupportSQLiteQuery): Flow<List<CallEntity>>

    /** Aggregates needed by [ContactMetaDao.upsert] when recomputing. */
    @Query("""
        SELECT
            MIN(date)                          AS firstCallDate,
            MAX(date)                          AS lastCallDate,
            COUNT(*)                           AS totalCalls,
            COALESCE(SUM(duration), 0)         AS totalDuration,
            SUM(CASE WHEN type = 1 THEN 1 ELSE 0 END) AS incomingCount,
            SUM(CASE WHEN type = 2 THEN 1 ELSE 0 END) AS outgoingCount,
            SUM(CASE WHEN type = 3 THEN 1 ELSE 0 END) AS missedCount
        FROM calls
        WHERE normalizedNumber = :number AND deletedAt IS NULL
    """)
    suspend fun aggregatesFor(number: String): CallAggregates?

    @Query("SELECT DISTINCT normalizedNumber FROM calls WHERE systemId IN (:ids)")
    suspend fun normalizedNumbersFor(ids: List<Long>): List<String>

    @Query("UPDATE calls SET isBookmarked = :flag, bookmarkReason = :reason, updatedAt = :now WHERE systemId = :id")
    suspend fun setBookmarked(id: Long, flag: Boolean, reason: String?, now: Long = System.currentTimeMillis())

    @Query("UPDATE calls SET isArchived = :flag, updatedAt = :now WHERE systemId = :id")
    suspend fun setArchived(id: Long, flag: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE calls SET deletedAt = :now WHERE systemId = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query("""
        UPDATE calls
        SET followUpDate = :triggerMs,
            followUpTime = :minuteOfDay,
            followUpNote = :note,
            followUpDoneAt = NULL,
            updatedAt = :now
        WHERE systemId = :id
    """)
    suspend fun setFollowUp(
        id: Long,
        triggerMs: Long?,
        minuteOfDay: Int?,
        note: String?,
        now: Long = System.currentTimeMillis()
    )

    @Query("UPDATE calls SET followUpDoneAt = :now, updatedAt = :now WHERE systemId = :id")
    suspend fun markFollowUpDone(id: Long, now: Long = System.currentTimeMillis())

    /** Latest active (non-done) follow-up for a number, used by auto-clear in
     *  [com.callvault.app.domain.usecase.SyncCallLogUseCase]. */
    @Query("""
        SELECT * FROM calls
        WHERE normalizedNumber = :number
          AND followUpDate IS NOT NULL
          AND followUpDoneAt IS NULL
          AND deletedAt IS NULL
        ORDER BY followUpDate ASC
        LIMIT 1
    """)
    suspend fun activeFollowUpForNumber(number: String): CallEntity?

    // --- Sprint 8 (Stats) -------------------------------------------------

    /** Per-day call counts in [from, to], midnight-bucketed. */
    @Query(
        "SELECT date/86400000*86400000 as day, COUNT(*) as count " +
            "FROM calls WHERE date BETWEEN :from AND :to " +
            "GROUP BY day ORDER BY day ASC"
    )
    suspend fun dailyCounts(from: Long, to: Long): List<DailyCountRow>

    /** Counts per `CallLog.Calls.TYPE_*`. */
    @Query(
        "SELECT type, COUNT(*) as count FROM calls " +
            "WHERE date BETWEEN :from AND :to GROUP BY type"
    )
    suspend fun typeCounts(from: Long, to: Long): List<TypeCountRow>

    /** Raw timestamps used for client-side hour/day-of-week aggregation. */
    @Query("SELECT date FROM calls WHERE date BETWEEN :from AND :to")
    suspend fun rawDates(from: Long, to: Long): List<Long>

    /** Top numbers by count in range; secondary sort = total duration. */
    @Query(
        "SELECT normalizedNumber, COUNT(*) as callCount, SUM(duration) as totalDuration " +
            "FROM calls WHERE date BETWEEN :from AND :to " +
            "GROUP BY normalizedNumber ORDER BY callCount DESC LIMIT :limit"
    )
    suspend fun topByCount(from: Long, to: Long, limit: Int): List<TopNumberRow>

    @Query("SELECT COUNT(*) FROM calls WHERE date BETWEEN :from AND :to")
    suspend fun totalCount(from: Long, to: Long): Int

    @Query("SELECT COALESCE(SUM(duration),0) FROM calls WHERE date BETWEEN :from AND :to")
    suspend fun totalDuration(from: Long, to: Long): Long

    @Query("SELECT COUNT(*) FROM calls WHERE date BETWEEN :from AND :to AND type = 3")
    suspend fun missedCount(from: Long, to: Long): Int
}

/** Projection: day-bucket epoch-ms → call count. */
data class DailyCountRow(val day: Long, val count: Int)

/** Projection: `CallLog.Calls.TYPE_*` value → call count. */
data class TypeCountRow(val type: Int, val count: Int)

/** Projection: leaderboard row for top-numbers chart. */
data class TopNumberRow(
    val normalizedNumber: String,
    val callCount: Int,
    val totalDuration: Long
)

/** Projection row for [CallDao.aggregatesFor]. */
data class CallAggregates(
    val firstCallDate: Long,
    val lastCallDate: Long,
    val totalCalls: Int,
    val totalDuration: Int,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int
)
