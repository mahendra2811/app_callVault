package com.callvault.app.data.repository

import com.callvault.app.data.local.dao.CallDao
import com.callvault.app.data.local.dao.TagDao
import com.callvault.app.data.local.entity.CallTagCrossRef
import com.callvault.app.data.local.mapper.toDomain
import com.callvault.app.data.local.mapper.toEntity
import com.callvault.app.domain.model.Tag
import com.callvault.app.domain.repository.TagRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of [TagRepository].
 *
 * The "applied tags by call id" rollup is computed by joining the in-memory
 * stream of `call_tag_cross_ref` with the in-memory tag list — both are
 * already cached as Room flows, so the cost is negligible compared to a
 * per-call DAO query.
 */
@Singleton
class TagRepositoryImpl @Inject constructor(
    private val dao: TagDao,
    private val callDao: CallDao
) : TagRepository {

    override fun observeAll(): Flow<List<Tag>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeForCall(callId: Long): Flow<List<Tag>> =
        dao.observeTagsForCall(callId).map { it.map { e -> e.toDomain() } }

    override fun observeForNumber(normalizedNumber: String): Flow<List<Tag>> =
        dao.observeTagsForNumber(normalizedNumber).map { it.map { e -> e.toDomain() } }

    override fun observeUsageCounts(): Flow<Map<Long, Int>> =
        dao.observeUsageCounts().map { rows -> rows.associate { it.tagId to it.count } }

    override fun observeAllAppliedTags(): Flow<Map<Long, List<Tag>>> =
        combine(dao.observeAllCrossRefs(), dao.observeAll()) { refs, tagEntities ->
            val tagsById = tagEntities.associate { it.id to it.toDomain() }
            refs.groupBy { it.callSystemId }
                .mapValues { (_, list) -> list.mapNotNull { tagsById[it.tagId] } }
        }

    override suspend fun upsert(tag: Tag): Long = dao.insert(tag.toEntity())

    override suspend fun delete(tag: Tag) = dao.delete(tag.toEntity())

    override suspend fun applyTag(callId: Long, tagId: Long, appliedBy: String) {
        dao.applyTag(CallTagCrossRef(callSystemId = callId, tagId = tagId, appliedBy = appliedBy))
    }

    override suspend fun removeTag(callId: Long, tagId: Long) = dao.removeTag(callId, tagId)

    override suspend fun removeAllAppliedBy(appliedBy: String) =
        dao.removeAllAppliedBy(appliedBy)

    override suspend fun bulkApplyTag(callIds: Collection<Long>, tagId: Long, appliedBy: String) {
        callIds.forEach { id ->
            dao.applyTag(CallTagCrossRef(callSystemId = id, tagId = tagId, appliedBy = appliedBy))
        }
    }

    override suspend fun setTagsForCall(callId: Long, tagIds: Set<Long>, appliedBy: String) {
        val existing = dao.observeTagsForCall(callId).first().map { it.id }.toSet()
        val toRemove = existing - tagIds
        val toAdd = tagIds - existing
        toRemove.forEach { dao.removeTag(callId, it) }
        toAdd.forEach { tid ->
            dao.applyTag(CallTagCrossRef(callSystemId = callId, tagId = tid, appliedBy = appliedBy))
        }
    }

    override suspend fun setTagsForNumber(
        normalizedNumber: String,
        tagIds: Set<Long>,
        appliedBy: String
    ) {
        val callIds = callDao.observeForNumber(normalizedNumber).first().map { it.systemId }
        callIds.forEach { setTagsForCall(it, tagIds, appliedBy) }
    }

    override suspend fun mergeInto(sourceTagId: Long, targetTagId: Long) {
        if (sourceTagId == targetTagId) return
        dao.mergeCrossRefs(sourceTagId = sourceTagId, targetTagId = targetTagId)
        dao.deleteById(sourceTagId)
    }

    override suspend fun usageCount(tagId: Long): Int = dao.countForTag(tagId)
}
