package com.callvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** "Was this article helpful?" feedback captured locally (no telemetry). */
@Entity(tableName = "doc_feedback")
data class DocFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleId: String,
    val isHelpful: Boolean,
    val submittedAt: Long = System.currentTimeMillis()
)
