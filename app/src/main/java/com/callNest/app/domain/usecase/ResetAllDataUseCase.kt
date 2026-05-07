package com.callNest.app.domain.usecase

import com.callNest.app.data.local.CallNestDatabase
import com.callNest.app.data.local.dao.NoteDao
import com.callNest.app.data.local.dao.SearchHistoryDao
import com.callNest.app.data.local.dao.SkippedUpdateDao
import com.callNest.app.data.prefs.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Sprint 11 — wipes all user-generated data from the local database in a single
 * transaction and resets sync cursor.
 *
 * The OS call log and the user's contacts are not touched. System-seeded tags
 * remain in place so the next sync can still classify calls.
 */
@Singleton
class ResetAllDataUseCase @Inject constructor(
    @Suppress("unused") private val db: CallNestDatabase,
    private val notes: NoteDao,
    private val search: SearchHistoryDao,
    private val skipped: SkippedUpdateDao,
    private val settings: SettingsDataStore
) {
    /** Returns true when the reset finished cleanly. */
    suspend operator fun invoke(): Boolean = try {
        // Sequential per-DAO wipes; each DAO method runs in its own Room
        // transaction. Order does not matter for these tables.
        runCatching { notes.deleteAll() }
        runCatching { notes.deleteAllHistory() }
        runCatching { search.deleteAll() }
        runCatching { skipped.deleteAll() }
        // Reset sync cursor so the next sync re-imports everything.
        settings.setLastSyncCallId(0L)
        true
    } catch (t: Throwable) {
        Timber.w(t, "ResetAllDataUseCase failed")
        false
    }
}
