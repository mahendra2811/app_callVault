package com.callvault.app.data.repository

import com.callvault.app.data.local.dao.PipelineStageDao
import com.callvault.app.data.local.entity.PipelineStageEntity
import com.callvault.app.domain.model.PipelineStage
import com.callvault.app.domain.repository.PipelineRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PipelineRepositoryImpl @Inject constructor(
    private val dao: PipelineStageDao,
) : PipelineRepository {

    override val stages: Flow<Map<String, PipelineStage>> =
        dao.observeAll().map { rows ->
            rows.associate { it.normalizedNumber to PipelineStage.fromKey(it.stageKey) }
        }

    override suspend fun setStage(normalizedNumber: String, stage: PipelineStage) {
        if (stage == PipelineStage.New) {
            dao.delete(normalizedNumber)
        } else {
            dao.upsert(PipelineStageEntity(normalizedNumber, stage.name))
        }
    }

    override suspend fun clearStage(normalizedNumber: String) = dao.delete(normalizedNumber)
}
