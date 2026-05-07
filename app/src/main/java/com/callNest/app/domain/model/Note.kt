package com.callNest.app.domain.model

import kotlinx.datetime.Instant

/** Markdown note attached to a call or to a number. */
data class Note(
    val id: Long,
    val callSystemId: Long?,
    val normalizedNumber: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
