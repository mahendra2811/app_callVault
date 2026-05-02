package com.callvault.app.domain.usecase

import android.os.Bundle
import com.callvault.app.data.service.alarm.ExactAlarmScheduler
import com.callvault.app.domain.repository.CallRepository
import com.callvault.app.domain.repository.ContactRepository
import com.callvault.app.domain.repository.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Persists a follow-up to the database and schedules an exact-alarm reminder.
 *
 * If [time] is null the reminder defaults to 09:00 local time. The use case
 * is fully idempotent — calling it again for the same call replaces both the
 * persisted row and the scheduled alarm.
 */
class ScheduleFollowUpUseCase @Inject constructor(
    private val callRepo: CallRepository,
    private val noteRepo: NoteRepository,
    private val contactRepo: ContactRepository,
    private val scheduler: ExactAlarmScheduler
) {

    /**
     * @param callSystemId target call row
     * @param normalizedNumber number associated with the call (extras for the broadcast)
     * @param date local date the reminder should fire
     * @param time local time of day, or null to default to 09:00
     * @param note optional reminder note shown in the notification body
     */
    suspend operator fun invoke(
        callSystemId: Long,
        normalizedNumber: String,
        date: LocalDate,
        time: LocalTime?,
        note: String?
    ) {
        val resolvedTime = time ?: LocalTime(9, 0)
        val ldt = LocalDateTime(date, resolvedTime)
        val triggerMs = ldt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val minuteOfDay = resolvedTime.hour * 60 + resolvedTime.minute

        callRepo.setFollowUp(
            callId = callSystemId,
            triggerMs = triggerMs,
            minuteOfDay = minuteOfDay,
            note = note
        )

        // Resolve display name + last note preview for richer notification copy.
        val displayName = contactRepo.observeByNumber(normalizedNumber).first()?.displayName
        val notePreview = note?.takeIf { it.isNotBlank() }
            ?: noteRepo.observeForNumber(normalizedNumber).first()
                .firstOrNull()
                ?.content

        val extras = Bundle().apply {
            putLong(ExactAlarmScheduler.EXTRA_CALL_SYSTEM_ID, callSystemId)
            putString(ExactAlarmScheduler.EXTRA_NORMALIZED_NUMBER, normalizedNumber)
            putString(ExactAlarmScheduler.EXTRA_DISPLAY_NAME, displayName)
            putString(ExactAlarmScheduler.EXTRA_NOTE_PREVIEW, notePreview)
            putLong(ExactAlarmScheduler.EXTRA_TRIGGER_MS, triggerMs)
        }

        scheduler.schedule(
            triggerAtMs = triggerMs,
            requestCode = ExactAlarmScheduler.requestCodeFor(callSystemId),
            extras = extras
        )
    }

    /** Cancel a previously scheduled follow-up and clear the persisted fields. */
    suspend fun cancel(callSystemId: Long) {
        scheduler.cancel(ExactAlarmScheduler.requestCodeFor(callSystemId))
        callRepo.setFollowUp(callId = callSystemId, triggerMs = null, minuteOfDay = null, note = null)
    }

    /** Mark the follow-up done without notifying. */
    suspend fun markDone(callSystemId: Long) {
        scheduler.cancel(ExactAlarmScheduler.requestCodeFor(callSystemId))
        callRepo.markFollowUpDone(callSystemId)
    }
}
