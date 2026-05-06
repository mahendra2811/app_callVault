package com.callvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User- or system-defined label that can be attached to many calls via
 * [CallTagCrossRef]. System tags (`isSystem = true`) cannot be deleted.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Hex color, e.g. `#FF6F61`. */
    val colorHex: String,
    val emoji: String? = null,
    val isSystem: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** Optional WhatsApp Business message template surfaced when this tag is applied. */
    val whatsappTemplate: String? = null
)
