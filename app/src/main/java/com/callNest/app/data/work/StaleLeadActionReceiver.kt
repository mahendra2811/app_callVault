package com.callNest.app.data.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.callNest.app.domain.repository.NoteRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import timber.log.Timber

/**
 * Handles notification action buttons emitted by [StaleLeadNudgeWorker].
 *
 * Supported actions:
 * - [ACTION_SNOOZE] — silently dismisses the notification (the worker re-evaluates
 *   tomorrow; this is enough to clear today's nag without opening the app).
 * - [ACTION_MARK_CONTACTED] — writes a placeholder note ("Marked contacted from
 *   nudge") for the lead, which keeps the contact warm so it falls out of the
 *   30-day stale window on the next worker run.
 */
@AndroidEntryPoint
class StaleLeadActionReceiver : BroadcastReceiver() {

    @Inject lateinit var noteRepo: NoteRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val number = intent.getStringExtra(EXTRA_NUMBER).orEmpty()
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                when (action) {
                    ACTION_MARK_CONTACTED -> {
                        if (number.isNotBlank()) {
                            runCatching {
                                noteRepo.upsert(
                                    com.callNest.app.domain.model.Note(
                                        id = 0,
                                        callSystemId = null,
                                        normalizedNumber = number,
                                        content = "Marked contacted from nudge",
                                        createdAt = Clock.System.now(),
                                        updatedAt = Clock.System.now()
                                    )
                                )
                            }.onFailure { Timber.w(it, "Mark contacted failed") }
                        }
                    }
                    ACTION_SNOOZE -> { /* dismiss only */ }
                }
                if (notifId > 0) NotificationManagerCompat.from(context).cancel(notifId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.callNest.app.action.STALE_LEAD_SNOOZE"
        const val ACTION_MARK_CONTACTED = "com.callNest.app.action.STALE_LEAD_MARK_CONTACTED"
        const val EXTRA_NUMBER = "number"
        const val EXTRA_NOTIF_ID = "notif_id"
    }
}
