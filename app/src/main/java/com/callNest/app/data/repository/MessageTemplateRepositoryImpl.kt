package com.callNest.app.data.repository

import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.domain.model.MessageTemplate
import com.callNest.app.domain.repository.MessageTemplateRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber

/** Built-ins are hard-coded; user templates persist as a JSON array in DataStore. */
@Singleton
class MessageTemplateRepositoryImpl @Inject constructor(
    private val settings: SettingsDataStore,
) : MessageTemplateRepository {

    override val templates: Flow<List<MessageTemplate>> =
        settings.messageTemplatesJson.map { json ->
            BUILT_INS + parse(json).map {
                MessageTemplate(it.id, it.label, it.body, builtIn = false)
            }
        }

    override suspend fun add(label: String, body: String) {
        if (label.isBlank() || body.isBlank()) return
        val raw = settings.messageTemplatesJson.first()
        val current = parseStrict(raw) ?: run {
            Timber.w("Template JSON corrupt; refusing to overwrite. Raw len=%d", raw.length)
            return
        }
        val next = current + StoredTemplate(
            id = UUID.randomUUID().toString(),
            label = label.trim(),
            body = body.trim(),
        )
        settings.setMessageTemplatesJson(Json.encodeToString(serializer, next))
    }

    override suspend fun delete(id: String) {
        val raw = settings.messageTemplatesJson.first()
        val current = parseStrict(raw) ?: run {
            Timber.w("Template JSON corrupt; refusing to overwrite on delete.")
            return
        }
        val next = current.filterNot { it.id == id }
        settings.setMessageTemplatesJson(Json.encodeToString(serializer, next))
    }

    /** Strict parse: returns null on any decode failure so writes can refuse to overwrite. */
    private fun parseStrict(json: String): List<StoredTemplate>? = try {
        Json.decodeFromString(serializer, json)
    } catch (t: Throwable) {
        null
    }

    private val serializer = ListSerializer(StoredTemplate.serializer())

    private fun parse(json: String): List<StoredTemplate> = try {
        Json.decodeFromString(serializer, json)
    } catch (t: Throwable) {
        Timber.w(t, "Bad message-templates JSON; treating as empty")
        emptyList()
    }

    @Serializable
    private data class StoredTemplate(val id: String, val label: String, val body: String)

    companion object {
        // Built-ins are short, neutral, and language-agnostic. Localize the labels via UI layer if needed.
        private val BUILT_INS = listOf(
            MessageTemplate(
                id = "builtin_thanks",
                label = "Thanks for your inquiry",
                body = "Hi, thanks for reaching out to us. How can I help you today?",
                builtIn = true,
            ),
            MessageTemplate(
                id = "builtin_catalog",
                label = "Send catalog",
                body = "Hi, sharing our latest product catalog with you. Please let me know which item interests you.",
                builtIn = true,
            ),
            MessageTemplate(
                id = "builtin_followup",
                label = "Follow-up",
                body = "Hi, just following up on our earlier call. Are you still interested? Happy to answer any questions.",
                builtIn = true,
            ),
            MessageTemplate(
                id = "builtin_price",
                label = "Quote / pricing",
                body = "Hi, sharing pricing details as discussed. Let me know if you'd like to proceed.",
                builtIn = true,
            ),
            MessageTemplate(
                id = "builtin_busy",
                label = "Busy — call you back",
                body = "Hi, I missed your call. I'll call you back shortly. If urgent, please WhatsApp me here.",
                builtIn = true,
            ),
        )
    }
}
