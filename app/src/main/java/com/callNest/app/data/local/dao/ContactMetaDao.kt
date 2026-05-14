package com.callNest.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.callNest.app.data.local.entity.ContactMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactMetaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: ContactMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(metas: List<ContactMetaEntity>)

    @Update
    suspend fun update(meta: ContactMetaEntity)

    @Query("SELECT * FROM contact_meta WHERE normalizedNumber = :number")
    suspend fun getByNumber(number: String): ContactMetaEntity?

    @Query("DELETE FROM contact_meta WHERE normalizedNumber = :number")
    suspend fun deleteByNumber(number: String)

    @Query("DELETE FROM contact_meta")
    suspend fun deleteAll()

    @Query("SELECT * FROM contact_meta WHERE normalizedNumber = :number")
    fun observeByNumber(number: String): Flow<ContactMetaEntity?>

    @Query("SELECT * FROM contact_meta ORDER BY computedLeadScore DESC, lastCallDate DESC")
    fun observeAll(): Flow<List<ContactMetaEntity>>

    /** Numbers with at least one call but no entry in the device's contacts. */
    @Query("SELECT * FROM contact_meta WHERE isInSystemContacts = 0 ORDER BY lastCallDate DESC")
    fun observeUnsaved(): Flow<List<ContactMetaEntity>>

    @Query("SELECT * FROM contact_meta WHERE isInSystemContacts = 1 ORDER BY displayName ASC")
    fun observeMyContacts(): Flow<List<ContactMetaEntity>>

    @Query("SELECT * FROM contact_meta WHERE isAutoSaved = 1 ORDER BY autoSavedAt DESC")
    fun observeAutoSaved(): Flow<List<ContactMetaEntity>>

    @Query("UPDATE contact_meta SET isAutoSaved = :flag, autoSavedAt = :ts, autoSavedFormat = :format WHERE normalizedNumber = :number")
    suspend fun setAutoSaved(number: String, flag: Boolean, ts: Long?, format: String?)

    // --- Sprint 8 (Stats) -------------------------------------------------

    /** Lead scores for contacts whose lastCallDate falls in the range. */
    @Query(
        "SELECT computedLeadScore as score FROM contact_meta " +
            "WHERE lastCallDate BETWEEN :from AND :to"
    )
    suspend fun scoresInRange(from: Long, to: Long): List<LeadScoreRow>

    /** Count of unsaved (not-in-contacts and not auto-saved) numbers in range. */
    @Query(
        "SELECT COUNT(*) FROM contact_meta " +
            "WHERE lastCallDate BETWEEN :from AND :to " +
            "AND isInSystemContacts = 0 AND isAutoSaved = 0"
    )
    suspend fun unsavedCountInRange(from: Long, to: Long): Int
}

/** Projection: a single lead score value, for `LeadBucket` distribution. */
data class LeadScoreRow(val score: Int)
