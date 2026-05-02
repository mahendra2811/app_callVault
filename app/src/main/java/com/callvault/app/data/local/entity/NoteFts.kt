package com.callvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/** FTS4 shadow table over [NoteEntity.content] for full-text note search. */
@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "note_fts")
data class NoteFts(
    val content: String
)
