package com.callvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-saved filter preset. `filterJson` is the serialized form of
 * `domain.model.FilterState`.
 */
@Entity(tableName = "filter_presets")
data class FilterPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filterJson: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
