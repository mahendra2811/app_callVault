package com.callNest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callNest.app.data.local.entity.PipelineStageEntity
import kotlinx.coroutines.flow.Flow

/** Pipeline stage CRUD; absence of a row = "New" stage. */
@Dao
interface PipelineStageDao {

    /** All explicitly-set rows. */
    @Query("SELECT * FROM pipeline_stage")
    fun observeAll(): Flow<List<PipelineStageEntity>>

    @Query("SELECT * FROM pipeline_stage WHERE normalizedNumber = :number LIMIT 1")
    suspend fun get(number: String): PipelineStageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: PipelineStageEntity)

    @Query("DELETE FROM pipeline_stage WHERE normalizedNumber = :number")
    suspend fun delete(number: String)
}
