package com.callvault.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.callvault.app.data.local.entity.NoteEntity
import com.callvault.app.data.local.entity.NoteHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    /** Sprint 11 — privacy action: wipe all notes (and history) on reset. */
    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Query("DELETE FROM note_history")
    suspend fun deleteAllHistory()

    @Delete
    suspend fun delete(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: NoteHistoryEntity)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE callSystemId = :callId ORDER BY createdAt DESC")
    fun observeForCall(callId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE normalizedNumber = :number ORDER BY createdAt DESC")
    fun observeForNumber(number: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note_history WHERE noteId = :noteId ORDER BY savedAt DESC")
    fun observeHistory(noteId: Long): Flow<List<NoteHistoryEntity>>

    @Query("""
        SELECT notes.* FROM notes
        JOIN note_fts ON note_fts.rowid = notes.id
        WHERE note_fts MATCH :ftsQuery
        ORDER BY notes.createdAt DESC
        LIMIT 200
    """)
    suspend fun searchFts(ftsQuery: String): List<NoteEntity>

    /** Number of [NoteHistoryEntity] rows currently retained for [noteId]. */
    @Query("SELECT COUNT(*) FROM note_history WHERE noteId = :noteId")
    suspend fun historyCount(noteId: Long): Int

    /**
     * Deletes the oldest history rows for [noteId] beyond the most recent
     * [keep] entries. Spec retains the last 5 snapshots per note.
     */
    @Query("""
        DELETE FROM note_history
        WHERE noteId = :noteId
          AND id NOT IN (
            SELECT id FROM note_history
            WHERE noteId = :noteId
            ORDER BY savedAt DESC
            LIMIT :keep
          )
    """)
    suspend fun pruneHistory(noteId: Long, keep: Int)

    @Query("DELETE FROM notes WHERE callSystemId = :callId")
    suspend fun deleteForCall(callId: Long)

    /**
     * Sprint 7 — attach orphan number-level notes (those persisted by the
     * floating bubble during a call when the call entity didn't yet exist)
     * to the call now that sync has inserted it.
     */
    @Query("""
        UPDATE notes
        SET callSystemId = :callSystemId
        WHERE callSystemId IS NULL
          AND normalizedNumber = :normalizedNumber
          AND createdAt BETWEEN :fromMs AND :toMs
    """)
    suspend fun attachOrphans(
        normalizedNumber: String,
        callSystemId: Long,
        fromMs: Long,
        toMs: Long
    ): Int
}
