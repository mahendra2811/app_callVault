package com.callvault.app.domain.model

/** A reusable message template the user can shoot to WhatsApp/SMS in one tap. */
data class MessageTemplate(
    val id: String,
    val label: String,
    val body: String,
    val builtIn: Boolean = false,
)
