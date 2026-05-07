package com.callNest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.callNest.app.data.local.entity.CallTagCrossRef
import com.callNest.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun applyTag(crossRef: CallTagCrossRef)

    @Query("DELETE FROM call_tag_cross_ref WHERE callSystemId = :callId AND tagId = :tagId")
    suspend fun removeTag(callId: Long, tagId: Long)

    @Query("DELETE FROM call_tag_cross_ref WHERE appliedBy = :appliedBy")
    suspend fun removeAllAppliedBy(appliedBy: String)

    /** Tag ids currently applied to [callId]. Used by Sprint 6 rule eval. */
    @Query("SELECT tagId FROM call_tag_cross_ref WHERE callSystemId = :callId")
    suspend fun tagIdsForCall(callId: Long): List<Long>

    @Query("""
        SELECT t.* FROM tags t
        JOIN call_tag_cross_ref x ON x.tagId = t.id
        WHERE x.callSystemId = :callId
        ORDER BY t.sortOrder
    """)
    fun observeTagsForCall(callId: Long): Flow<List<TagEntity>>

    @Query("""
        SELECT x.callSystemId FROM call_tag_cross_ref x
        WHERE x.tagId = :tagId
    """)
    fun observeCallIdsForTag(tagId: Long): Flow<List<Long>>

    /** All cross-refs in the database — used by [com.callNest.app.ui.screen.calls.CallsViewModel]
     *  to compute the per-call tag map without N+1 queries. */
    @Query("SELECT * FROM call_tag_cross_ref")
    fun observeAllCrossRefs(): Flow<List<CallTagCrossRef>>

    /** Tag-name → application count, for cross-refs whose [CallTagCrossRef.appliedAt] is in the window. */
    @Query("""
        SELECT t.name AS name, COUNT(*) AS count
        FROM call_tag_cross_ref x
        JOIN tags t ON t.id = x.tagId
        WHERE x.appliedAt BETWEEN :fromMs AND :toMs
        GROUP BY t.id
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun topTagsBetween(fromMs: Long, toMs: Long, limit: Int = 5): List<TagCount>

    /** Count of calls each tag is applied to. Drives the badge on
     *  [com.callNest.app.ui.screen.tags.TagsManagerScreen]. */
    @Query("SELECT tagId AS tagId, COUNT(*) AS count FROM call_tag_cross_ref GROUP BY tagId")
    fun observeUsageCounts(): Flow<List<TagUsageCount>>

    /** Aggregated tag rollup for every call attached to [normalizedNumber] —
     *  used by the call-detail tag chip row. */
    @Query("""
        SELECT DISTINCT t.* FROM tags t
        JOIN call_tag_cross_ref x ON x.tagId = t.id
        JOIN calls c ON c.systemId = x.callSystemId
        WHERE c.normalizedNumber = :number AND c.deletedAt IS NULL
        ORDER BY t.sortOrder
    """)
    fun observeTagsForNumber(number: String): Flow<List<TagEntity>>

    /** Re-points every cross-ref of [sourceTagId] to [targetTagId] then deletes
     *  the source. Used by the "Merge into…" flow in the Tags Manager. */
    @androidx.room.Transaction
    @Query("""
        UPDATE OR REPLACE call_tag_cross_ref
        SET tagId = :targetTagId
        WHERE tagId = :sourceTagId
    """)
    suspend fun mergeCrossRefs(sourceTagId: Long, targetTagId: Long)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM call_tag_cross_ref WHERE tagId = :tagId")
    suspend fun countForTag(tagId: Long): Int
}

/** Projection row for [TagDao.observeUsageCounts]. */
data class TagUsageCount(val tagId: Long, val count: Int)
