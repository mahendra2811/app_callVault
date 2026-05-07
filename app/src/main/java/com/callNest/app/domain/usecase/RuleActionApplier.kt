package com.callNest.app.domain.usecase

import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.dao.RuleScoreBoostDao
import com.callNest.app.data.local.dao.TagDao
import com.callNest.app.data.local.entity.CallTagCrossRef
import com.callNest.app.data.local.entity.RuleScoreBoostEntity
import com.callNest.app.domain.model.Call
import com.callNest.app.domain.model.RuleAction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber

/**
 * Applies a single [RuleAction] to a [Call]. Stateless and side-effect-only;
 * orchestration / loop logic lives in [ApplyAutoTagRulesUseCase].
 *
 * Each variant is best-effort: errors are logged and swallowed so a single
 * malformed rule never breaks the rest of the sync pipeline (spec §13).
 */
@Singleton
class RuleActionApplier @Inject constructor(
    private val tagDao: TagDao,
    private val callDao: CallDao,
    private val ruleScoreBoostDao: RuleScoreBoostDao,
    private val scheduleFollowUpUseCase: ScheduleFollowUpUseCase
) {

    /**
     * Apply [action] to [call] under the rule [ruleId].
     *
     * @return true if a change was made, false otherwise.
     */
    suspend fun apply(action: RuleAction, call: Call, ruleId: Long): Boolean = try {
        when (action) {
            is RuleAction.ApplyTag -> {
                tagDao.applyTag(
                    CallTagCrossRef(
                        callSystemId = call.systemId,
                        tagId = action.tagId,
                        appliedBy = "rule:$ruleId"
                    )
                )
                true
            }

            is RuleAction.LeadScoreBoost -> {
                ruleScoreBoostDao.upsert(
                    RuleScoreBoostEntity(
                        callSystemId = call.systemId,
                        ruleId = ruleId,
                        delta = action.delta.coerceIn(-100, 100)
                    )
                )
                true
            }

            is RuleAction.AutoBookmark -> {
                if (!call.isBookmarked) {
                    callDao.setBookmarked(
                        id = call.systemId,
                        flag = true,
                        reason = call.bookmarkReason ?: action.reason
                    )
                    true
                } else false
            }

            is RuleAction.MarkFollowUp -> {
                if (call.followUpAt != null && call.followUpDoneAt == null) {
                    // Already has an open follow-up; do not overwrite.
                    false
                } else {
                    val tz = TimeZone.currentSystemDefault()
                    val daysFromCall = action.hoursFromNow / 24 // spec spelling re-uses field
                    val callLdt = call.date.toLocalDateTime(tz)
                    val triggerInstant: Instant = call.date.plusDays(
                        if (daysFromCall <= 0) 1 else daysFromCall
                    )
                    val triggerLdt = triggerInstant.toLocalDateTime(tz)
                    scheduleFollowUpUseCase(
                        callSystemId = call.systemId,
                        normalizedNumber = call.normalizedNumber,
                        date = triggerLdt.date,
                        time = LocalTime(9, 0),
                        note = "Auto follow-up from rule"
                    )
                    // suppress unused-var linter
                    @Suppress("UNUSED_VARIABLE") val anchor = callLdt
                    true
                }
            }
        }
    } catch (t: Throwable) {
        Timber.w(t, "RuleActionApplier failed for action=${action::class.simpleName} call=${call.systemId}")
        false
    }
}

/** Add [days] whole days to an [Instant]. */
private fun Instant.plusDays(days: Int): Instant =
    Instant.fromEpochMilliseconds(toEpochMilliseconds() + days * 24L * 60 * 60 * 1000)
