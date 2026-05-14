package com.callNest.app.domain.usecase

import com.callNest.app.data.local.CallNestDatabase
import com.callNest.app.data.local.dao.AutoTagRuleDao
import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.dao.ContactMetaDao
import com.callNest.app.data.local.dao.DocFeedbackDao
import com.callNest.app.data.local.dao.FilterPresetDao
import com.callNest.app.data.local.dao.NoteDao
import com.callNest.app.data.local.dao.PipelineStageDao
import com.callNest.app.data.local.dao.RuleScoreBoostDao
import com.callNest.app.data.local.dao.SearchHistoryDao
import com.callNest.app.data.local.dao.SkippedUpdateDao
import com.callNest.app.data.local.dao.TagDao
import com.callNest.app.data.prefs.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Wipes every user-generated row in the local database, then resets sync
 * cursors so the next sync re-imports from scratch.
 *
 * What stays:
 *  - OS call log + OS contacts (we never own these)
 *  - System-seeded tags (`isSystem = true`)
 *  - User auth + settings (cleared via separate sign-out)
 */
@Singleton
class ResetAllDataUseCase @Inject constructor(
    @Suppress("unused") private val db: CallNestDatabase,
    private val calls: CallDao,
    private val contactMeta: ContactMetaDao,
    private val tags: TagDao,
    private val notes: NoteDao,
    private val filterPresets: FilterPresetDao,
    private val autoTagRules: AutoTagRuleDao,
    private val ruleScoreBoosts: RuleScoreBoostDao,
    private val docFeedback: DocFeedbackDao,
    private val pipelineStages: PipelineStageDao,
    private val search: SearchHistoryDao,
    private val skipped: SkippedUpdateDao,
    private val settings: SettingsDataStore
) {
    /** Returns true when the reset finished cleanly. */
    suspend operator fun invoke(): Boolean = try {
        // Drop cross-refs first (no FK constraints, but keeps the trace
        // tidy when this races against a sync).
        runCatching { tags.deleteAllCrossRefs() }
        runCatching { ruleScoreBoosts.deleteAll() }
        runCatching { notes.deleteAll() }
        runCatching { notes.deleteAllHistory() }
        runCatching { calls.deleteAll() }
        runCatching { contactMeta.deleteAll() }
        runCatching { tags.deleteAllUserCreated() }
        runCatching { filterPresets.deleteAll() }
        runCatching { autoTagRules.deleteAll() }
        runCatching { docFeedback.deleteAll() }
        runCatching { pipelineStages.deleteAll() }
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
