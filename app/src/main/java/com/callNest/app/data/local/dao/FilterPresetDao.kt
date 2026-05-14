package com.callNest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.callNest.app.data.local.entity.FilterPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterPresetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: FilterPresetEntity): Long

    @Update
    suspend fun update(preset: FilterPresetEntity)

    @Delete
    suspend fun delete(preset: FilterPresetEntity)

    @Query("SELECT * FROM filter_presets ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<FilterPresetEntity>>

    @Query("SELECT * FROM filter_presets WHERE id = :id")
    suspend fun getById(id: Long): FilterPresetEntity?

    @Query("DELETE FROM filter_presets")
    suspend fun deleteAll()
}
