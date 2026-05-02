package com.callvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Append-only audit trail of [NoteEntity] edits. One row is written each
 * time the user saves a note over an existing one — the previous content is
 * snapshotted so users can recover lost text.
 */
@Entity(tableName = "note_history")
data class NoteHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val previousContent: String,
    val savedAt: Long = System.currentTimeMillis()
)
