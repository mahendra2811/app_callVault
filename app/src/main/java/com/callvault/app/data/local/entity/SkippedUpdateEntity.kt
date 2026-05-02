package com.callvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Versions the user explicitly chose to skip on the in-app update prompt.
 * `versionCode` is the PK so a single row exists per skipped version.
 */
@Entity(tableName = "skipped_updates")
data class SkippedUpdateEntity(
    @PrimaryKey val versionCode: Int,
    val skippedAt: Long = System.currentTimeMillis()
)
