package com.callNest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.callNest.app.data.local.entity.SkippedUpdateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkippedUpdateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SkippedUpdateEntity)

    @Query("SELECT * FROM skipped_updates")
    fun observeAll(): Flow<List<SkippedUpdateEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM skipped_updates WHERE versionCode = :versionCode)")
    suspend fun isSkipped(versionCode: Int): Boolean

    @Query("DELETE FROM skipped_updates")
    suspend fun clear()

    @Query("DELETE FROM skipped_updates")
    suspend fun deleteAll()
}
