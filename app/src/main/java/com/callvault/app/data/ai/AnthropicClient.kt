package com.callvault.app.data.ai

import com.callvault.app.domain.model.WeeklyDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

/**
 * Tiny Claude Messages API client. BYOK — no key in the binary.
 *
 * **Privacy posture:** the only payload sent is the *aggregate* digest (counts + tag names + first
 * names of top callers). No phone numbers, no notes, no raw call records. Caller must verify
 * `BuildConfig.DEBUG`-equivalent consent before invoking.
 */
@Singleton
class AnthropicClient @Inject constructor() {

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
    private val json = Json { ignoreUnknownKeys = true }

    sealed interface Result {
        data class Ok(val text: String) : Result
        data class Failure(val message: String) : Result
    }

    /** Sends only aggregate counts + first names + tag names. Returns explicit Ok/Failure. */
    suspend fun summarizeDigest(apiKey: String, digest: WeeklyDigest): Result {
        if (apiKey.isBlank()) return Result.Failure("API key not set")
        val prompt = buildPrompt(digest)
        val body = json.encodeToString(
            MessagesRequest.serializer(),
            MessagesRequest(
                model = MODEL_ID,
                maxTokens = 200,
                messages = listOf(MessagesRequest.Message(role = "user", content = prompt)),
            ),
        ).toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(body)
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    Timber.w("Anthropic API HTTP %d: %s", resp.code, raw.take(200))
                    return Result.Failure("HTTP ${resp.code}")
                }
                val payload = json.decodeFromString(MessagesResponse.serializer(), raw)
                val text = payload.content.firstOrNull { it.type == "text" }?.text?.trim()
                if (text.isNullOrBlank()) Result.Failure("Empty response")
                else Result.Ok(text)
            }
        } catch (t: Throwable) {
            Timber.w(t, "Anthropic API exception")
            Result.Failure(t.message ?: "Network error")
        }
    }

    companion object {
        // Public dated snapshot per Anthropic's model cards. Bare `claude-haiku-4-5` is not guaranteed.
        private const val MODEL_ID = "claude-haiku-4-5-20251001"
    }

    private fun buildPrompt(d: WeeklyDigest): String = buildString {
        append("You are a concise sales coach for an Indian small-business owner. ")
        append("Given the last-7-day phone-call rollup below, write 2-3 sentences in plain English ")
        append("highlighting what the user should focus on this week. No bullet points. No numbers ")
        append("repeated verbatim — interpret them. Be encouraging.\n\n")
        append("Total calls: ${d.totalCalls} (${d.incoming} in, ${d.outgoing} out, ${d.missed} missed)\n")
        append("Unique contacts: ${d.uniqueContacts}\n")
        append("Hot leads (score ≥ 70): ${d.hotLeads}\n")
        if (d.topCallers.isNotEmpty()) {
            append("Top callers (first-name only):\n")
            d.topCallers.forEach { c ->
                val firstName = c.displayName?.split(' ')?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "anonymous"
                append("  - $firstName: ${c.callCount} calls, score ${c.leadScore}\n")
            }
        }
        if (d.topTags.isNotEmpty()) {
            append("Most-used tags this week: ")
            append(d.topTags.joinToString(", ") { "${it.name} (${it.count})" })
            append('\n')
        }
    }

    @Serializable
    private data class MessagesRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val messages: List<Message>,
    ) {
        @Serializable
        data class Message(val role: String, val content: String)
    }

    @Serializable
    private data class MessagesResponse(
        val content: List<Block> = emptyList(),
    ) {
        @Serializable
        data class Block(val type: String = "", val text: String = "")
    }
}
