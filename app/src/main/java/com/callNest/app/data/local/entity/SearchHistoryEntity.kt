package com.callNest.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Recent search queries shown as suggestions on the Search screen. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val executedAt: Long = System.currentTimeMillis()
)
