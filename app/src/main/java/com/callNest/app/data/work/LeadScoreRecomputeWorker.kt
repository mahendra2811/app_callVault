package com.callNest.app.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callNest.app.domain.repository.ContactRepository
import com.callNest.app.domain.usecase.ComputeLeadScoreUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Sprint 6 — recomputes the lead score for every contact using the latest
 * [com.callNest.app.domain.model.LeadScoreWeights]. Enqueued whenever the
 * user changes a slider on `LeadScoringSettingsScreen` or toggles the master
 * switch.
 *
 * The work is unique-named so rapid changes coalesce: the latest enqueue wins.
 */
@HiltWorker
class LeadScoreRecomputeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val contactRepo: ContactRepository,
    private val computeLeadScoreUseCase: ComputeLeadScoreUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val all = contactRepo.observeAll().first()
        for (meta in all) {
            runCatching {
                val ls = computeLeadScoreUseCase(meta)
                contactRepo.upsert(meta.copy(computedLeadScore = ls.total))
            }.onFailure { Timber.w(it, "LeadScoreRecomputeWorker: failed for ${meta.normalizedNumber}") }
        }
        Result.success()
    } catch (t: Throwable) {
        Timber.w(t, "LeadScoreRecomputeWorker failed")
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "callNest.leadscore.recompute"

        /** Enqueue a one-shot recompute, replacing any previously queued one. */
        fun enqueue(context: Context) {
            val req = OneTimeWorkRequestBuilder<LeadScoreRecomputeWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                req
            )
        }
    }
}
