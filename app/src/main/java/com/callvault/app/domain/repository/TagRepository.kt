package com.callvault.app.domain.repository

import com.callvault.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeAll(): Flow<List<Tag>>
    fun observeForCall(callId: Long): Flow<List<Tag>>
    /** Distinct tags applied to any call associated with [normalizedNumber]. */
    fun observeForNumber(normalizedNumber: String): Flow<List<Tag>>
    /** Reactive map of `tagId → applied-call-count`. Drives the Tags Manager badges. */
    fun observeUsageCounts(): Flow<Map<Long, Int>>
    /** Stream of every cross-ref so callers can build per-call tag rollups. */
    fun observeAllAppliedTags(): Flow<Map<Long, List<Tag>>>
    suspend fun upsert(tag: Tag): Long
    suspend fun delete(tag: Tag)
    suspend fun applyTag(callId: Long, tagId: Long, appliedBy: String = "user")
    suspend fun removeTag(callId: Long, tagId: Long)
    suspend fun removeAllAppliedBy(appliedBy: String)
    /** Apply a tag to many calls in one transaction (used by bulk-edit on Calls screen). */
    suspend fun bulkApplyTag(callIds: Collection<Long>, tagId: Long, appliedBy: String = "user")
    /** Replace every cross-ref for [callId] with [tagIds]. */
    suspend fun setTagsForCall(callId: Long, tagIds: Set<Long>, appliedBy: String = "user")
    /** Apply [tagIds] to every call belonging to [normalizedNumber]. */
    suspend fun setTagsForNumber(normalizedNumber: String, tagIds: Set<Long>, appliedBy: String = "user")
    /** Repoint cross-refs from [sourceTagId] to [targetTagId] then delete [sourceTagId]. */
    suspend fun mergeInto(sourceTagId: Long, targetTagId: Long)
    /** Count of calls a tag is applied to. */
    suspend fun usageCount(tagId: Long): Int
}
