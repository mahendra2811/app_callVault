package com.callvault.app.domain.usecase

import com.callvault.app.data.event.UiEvent
import com.callvault.app.data.event.UiEventBus
import com.callvault.app.data.local.dao.ContactMetaDao
import com.callvault.app.data.system.ContactsWriter
import javax.inject.Inject
import timber.log.Timber

/**
 * Sprint 5 — promotes an auto-saved entry into "My Contacts" (spec §3.12).
 *
 * Updates the system contact's `StructuredName.DISPLAY_NAME` to [newDisplayName],
 * then triggers [DetectAutoSavedRenameUseCase] for the single number, which
 * flips `isAutoSaved=false` because the new name no longer matches the
 * configured auto-save pattern.
 *
 * On any failure a snackbar is emitted; the row is left in the Inquiries
 * bucket so the user can retry.
 */
class ConvertToMyContactUseCase @Inject constructor(
    private val contactMetaDao: ContactMetaDao,
    private val writer: ContactsWriter,
    private val detectRename: DetectAutoSavedRenameUseCase,
    private val uiEventBus: UiEventBus
) {

    suspend operator fun invoke(normalizedNumber: String, newDisplayName: String): Boolean {
        val meta = contactMetaDao.getByNumber(normalizedNumber)
        val rawId = meta?.systemRawContactId
        if (rawId == null || rawId <= 0L) {
            uiEventBus.emit(
                UiEvent.Snackbar(
                    "Couldn't find this contact in your phone. Try syncing first."
                )
            )
            return false
        }
        val ok = try {
            writer.updateDisplayName(rawId, newDisplayName)
        } catch (t: Throwable) {
            Timber.e(t, "ConvertToMyContact: updateDisplayName threw")
            false
        }
        if (!ok) {
            uiEventBus.emit(UiEvent.Snackbar("Couldn't rename this contact. Try again."))
            return false
        }
        // Optimistically patch the local row so the UI updates without
        // waiting for the next sync.
        contactMetaDao.upsert(
            meta.copy(
                displayName = newDisplayName,
                isAutoSaved = false,
                updatedAt = System.currentTimeMillis()
            )
        )
        // Run rename detection for this single number too — keeps the
        // autoSavedFormat snapshot honest.
        runCatching { detectRename(normalizedNumber) }
        return true
    }
}
