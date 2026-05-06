package com.callvault.app.data.system

import com.callvault.app.domain.model.Call
import com.callvault.app.domain.model.ContactMeta
import com.callvault.app.domain.model.Tag
import com.callvault.app.domain.repository.CallRepository
import com.callvault.app.domain.repository.ContactRepository
import com.callvault.app.domain.repository.TagRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Read-only facade used by overlay views to resolve display data without
 * touching DAOs directly. Keeps view code thin and testable.
 */
@Singleton
class CallContextResolver @Inject constructor(
    private val callRepository: CallRepository,
    private val contactRepository: ContactRepository,
    private val contactsReader: ContactsReader,
    private val tagRepository: TagRepository
) {
    /** Latest [Call] for the given normalized number, or null. */
    suspend fun latestCallForNumber(normalizedNumber: String): Call? {
        if (normalizedNumber.isBlank()) return null
        return callRepository.observeForNumber(normalizedNumber).first().firstOrNull()
    }

    /** Snapshot of [ContactMeta] for the given number. */
    suspend fun metaForNumber(normalizedNumber: String): ContactMeta? =
        contactRepository.getByNumber(normalizedNumber)

    /** Best display name (live PhoneLookup → Room fallback). */
    suspend fun displayName(normalizedNumber: String): String? =
        contactsReader.resolveDisplayName(normalizedNumber)
            ?: contactRepository.getByNumber(normalizedNumber)?.displayName

    /** Top [n] tags by usage count, used by post-call popup quick chips. */
    suspend fun topTags(n: Int = 3): List<Tag> {
        val all = tagRepository.observeAll().first()
        val usage = tagRepository.observeUsageCounts().first()
        return all.sortedByDescending { usage[it.id] ?: 0 }.take(n)
    }

    /**
     * Tags this number was previously tagged with (across all prior calls).
     * Returns at most [n] entries, ordered by frequency.
     */
    suspend fun suggestedTagsForNumber(normalizedNumber: String, n: Int = 3): List<Tag> {
        val historyTags = tagRepository.observeForNumber(normalizedNumber).first()
        if (historyTags.isEmpty()) return topTags(n)
        return historyTags
            .groupingBy { it.id }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .mapNotNull { e -> historyTags.firstOrNull { it.id == e.key } }
            .distinctBy { it.id }
            .take(n)
            .ifEmpty { topTags(n) }
    }

    /** True if the number is not in the system contacts and not auto-saved. */
    suspend fun isUnsaved(normalizedNumber: String): Boolean {
        val meta = contactRepository.getByNumber(normalizedNumber)
        return meta == null || (!meta.isInSystemContacts && !meta.isAutoSaved)
    }
}
