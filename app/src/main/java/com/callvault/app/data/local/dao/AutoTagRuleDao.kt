package com.callvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.callvault.app.data.local.entity.AutoTagRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoTagRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AutoTagRuleEntity): Long

    @Update
    suspend fun update(rule: AutoTagRuleEntity)

    @Delete
    suspend fun delete(rule: AutoTagRuleEntity)

    @Query("SELECT * FROM auto_tag_rules ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<AutoTagRuleEntity>>

    @Query("SELECT * FROM auto_tag_rules WHERE isActive = 1 ORDER BY sortOrder ASC")
    suspend fun activeRules(): List<AutoTagRuleEntity>

    @Query("SELECT * FROM auto_tag_rules WHERE id = :id")
    suspend fun getById(id: Long): AutoTagRuleEntity?
}
