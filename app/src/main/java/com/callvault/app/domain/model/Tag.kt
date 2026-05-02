package com.callvault.app.domain.model

/** Pure domain tag. */
data class Tag(
    val id: Long,
    val name: String,
    val colorHex: String,
    val emoji: String?,
    val isSystem: Boolean,
    val sortOrder: Int
)
