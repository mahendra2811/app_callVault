package com.callvault.app.data.work

import android.content.Context
import android.os.PowerManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callvault.app.domain.model.SyncResult
import com.callvault.app.domain.usecase.SyncCallLogUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background worker wrapping [SyncCallLogUseCase].
 *
 * Holds a partial wakelock for at most 30s (spec §8.1 step 1). Returns
 * `Result.success` on Success/PartialSuccess and `Result.retry` only on
 * fatal failure so transient permission flips don't burn through retries.
 */
@HiltWorker
class CallSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncUseCase: SyncCallLogUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_TAG)
        try {
            wl.acquire(WAKE_TIMEOUT_MS)
            return when (val out = syncUseCase()) {
                is SyncResult.Success -> {
                    Timber.i("Sync OK: imported=${out.insertedCount}/${out.totalCount}")
                    Result.success()
                }
                is SyncResult.PartialSuccess -> {
                    Timber.w(
                        "Sync partial: imported=${out.insertedCount}, " +
                            "skipped=${out.skippedCount}, total=${out.totalCount}"
                    )
                    Result.success()
                }
                is SyncResult.Failure -> {
                    Timber.w("Sync failed: ${out.reason}")
                    Result.retry()
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Sync worker crashed")
            return Result.retry()
        } finally {
            if (wl.isHeld) wl.release()
        }
    }

    companion object {
        const val UNIQUE_PERIODIC_NAME = "callvault.sync.periodic"
        const val UNIQUE_ONESHOT_NAME = "callvault.sync.oneshot"
        const val UNIQUE_DAILY_NAME = "callvault.sync.daily2am"
        private const val WAKE_TAG = "CallVault:CallSyncWorker"
        private const val WAKE_TIMEOUT_MS = 30_000L
    }
}
