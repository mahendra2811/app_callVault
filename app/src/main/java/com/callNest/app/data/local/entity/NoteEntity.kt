package com.callNest.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Markdown note attached to a specific call (`callSystemId` non-null) or to a
 * number across all its calls (`callSystemId == null`).
 */
@Entity(
    tableName = "notes",
    indices = [Index("callSystemId"), Index("normalizedNumber")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callSystemId: Long?,
    val normalizedNumber: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
