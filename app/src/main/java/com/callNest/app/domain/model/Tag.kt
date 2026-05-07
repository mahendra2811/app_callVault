package com.callNest.app.domain.model

/** Pure domain tag. */
data class Tag(
    val id: Long,
    val name: String,
    val colorHex: String,
    val emoji: String?,
    val isSystem: Boolean,
    val sortOrder: Int,
    val whatsappTemplate: String? = null
)
