package com.callNest.app.data.local.entity

import androidx.room.Entity

/** Per-number pipeline stage. Absence of a row means the contact is in the "New" stage. */
@Entity(tableName = "pipeline_stage", primaryKeys = ["normalizedNumber"])
data class PipelineStageEntity(
    val normalizedNumber: String,
    /** Stored as the enum's `.name` so refactors break loudly via tests. */
    val stageKey: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
