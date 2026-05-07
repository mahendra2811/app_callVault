package com.callvault.app.domain.repository

import com.callvault.app.domain.model.MessageTemplate
import kotlinx.coroutines.flow.Flow

/** Quick-reply templates: built-ins are immutable; user-added templates are CRUD-able. */
interface MessageTemplateRepository {
    /** Combined list (built-ins first, then user-added). */
    val templates: Flow<List<MessageTemplate>>

    suspend fun add(label: String, body: String)
    suspend fun delete(id: String)
}
