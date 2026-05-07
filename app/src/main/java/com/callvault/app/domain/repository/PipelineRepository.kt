package com.callvault.app.domain.repository

import com.callvault.app.domain.model.PipelineStage
import kotlinx.coroutines.flow.Flow

/** Per-contact pipeline stage. Defaults to [PipelineStage.New] when no row exists. */
interface PipelineRepository {
    /** Map of normalizedNumber → explicitly-set stage. Numbers absent are implicitly [PipelineStage.New]. */
    val stages: Flow<Map<String, PipelineStage>>

    suspend fun setStage(normalizedNumber: String, stage: PipelineStage)
    suspend fun clearStage(normalizedNumber: String)
}
