package com.callNest.app.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.data.update.UpdateChecker
import com.callNest.app.data.update.UpdateCheckResult
import com.callNest.app.data.update.UpdateNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Weekly background worker that re-runs the update check and posts a
 * notification when a non-skipped newer build exists.
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsDataStore,
    private val checker: UpdateChecker,
    private val notifier: UpdateNotifier
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val auto = runCatching { settings.updateAutoCheck.first() }.getOrDefault(true)
        if (!auto) return Result.success()
        return try {
            when (val r = checker.checkNow()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    if (!r.isSkipped) notifier.show(r.manifest, r.channel)
                    Result.success()
                }
                is UpdateCheckResult.NoUpdate -> Result.success()
                is UpdateCheckResult.Error -> Result.retry()
            }
        } catch (t: Throwable) {
            Timber.w(t, "UpdateCheckWorker failed")
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE = "UpdateCheckWorker_periodic"

        /** Enqueue the weekly check (KEEP existing schedule if present). */
        fun schedule(ctx: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Cancel all future runs. */
        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE)
        }
    }
}
