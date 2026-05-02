package com.callvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 shadow table over [CallEntity] indexing the columns users search by
 * name. `docid` matches `CallEntity.systemId` (Room links them via
 * `contentEntity`).
 */
@Fts4(contentEntity = CallEntity::class)
@Entity(tableName = "call_fts")
data class CallFts(
    val rawNumber: String,
    val cachedName: String?,
    val geocodedLocation: String?
)
