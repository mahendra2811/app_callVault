package com.callvault.app.domain.usecase

import com.callvault.app.data.local.dao.ContactMetaDao
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.data.system.ContactsReader
import com.callvault.app.util.AutoSavePatternMatcher
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

/**
 * Sprint 5 — bucketing detection (spec §8.5).
 *
 * Iterates every `ContactMeta` with `isAutoSaved=true`, re-reads the system
 * display name via [ContactsReader.resolveDisplayName], and if the live name
 * no longer matches the configured auto-save pattern (i.e. the user manually
 * renamed the contact in their phone), flips `isAutoSaved=false`. This is
 * the "lenient" bucketing rule — once promoted out of Inquiries, the contact
 * stays out.
 *
 * Optionally constrained to a single number via [forNumber] (used by
 * [ConvertToMyContactUseCase]).
 *
 * @return number of rows flipped to `isAutoSaved=false`.
 */
class DetectAutoSavedRenameUseCase @Inject constructor(
    private val contactMetaDao: ContactMetaDao,
    private val contactsReader: ContactsReader,
    private val settings: SettingsDataStore
) {

    suspend operator fun invoke(forNumber: String? = null): Int {
        val prefix = settings.autoSavePrefix.first()
        val includeSimTag = settings.autoSaveIncludeSimTag.first()
        val suffix = settings.autoSaveSuffix.first()

        val candidates = if (forNumber != null) {
            listOfNotNull(contactMetaDao.getByNumber(forNumber))
        } else {
            // Snapshot — no Flow collection (we want a one-shot).
            contactMetaDao.observeAutoSaved().firstOrNull().orEmpty()
        }.filter { it.isAutoSaved }

        var flipped = 0
        for (meta in candidates) {
            val liveName = try {
                contactsReader.resolveDisplayName(meta.normalizedNumber)
            } catch (t: Throwable) {
                Timber.w(t, "DetectRename: read failed for ${meta.normalizedNumber}")
                continue
            }
            // If liveName is null the contact is gone from the system; treat as
            // renamed-away (the Inquiries bucket only makes sense if the row
            // still exists in Contacts).
            val stillMatches = AutoSavePatternMatcher.matches(
                displayName = liveName,
                prefix = prefix,
                includeSimTag = includeSimTag,
                suffix = suffix
            )
            if (!stillMatches) {
                contactMetaDao.upsert(
                    meta.copy(
                        isAutoSaved = false,
                        displayName = liveName ?: meta.displayName,
                        // Keep autoSavedAt as a historical marker — used for
                        // the "promoted from inquiry" badge.
                        updatedAt = System.currentTimeMillis()
                    )
                )
                flipped++
            }
        }
        return flipped
    }
}
