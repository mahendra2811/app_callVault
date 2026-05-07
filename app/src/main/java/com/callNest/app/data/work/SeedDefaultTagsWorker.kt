package com.callNest.app.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callNest.app.data.local.CallNestDatabase
import com.callNest.app.data.local.seed.DefaultTagsSeeder
import com.callNest.app.data.prefs.SettingsDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Off-thread worker that runs [DefaultTagsSeeder] once after database creation.
 *
 * Enqueued from the Room `Callback.onCreate` hook in `DatabaseModule`. Using a
 * worker instead of a synchronous insert keeps `Room.databaseBuilder` from
 * blocking on Hilt's main-thread provisioning — and survives an OS crash mid-seed
 * because the seeder itself is idempotent.
 */
@HiltWorker
class SeedDefaultTagsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val database: CallNestDatabase,
    private val settings: SettingsDataStore
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        if (!settings.tagsSeeded.first()) {
            DefaultTagsSeeder.seed(database.tagDao())
            settings.setTagsSeeded(true)
        }
        Result.success()
    } catch (t: Throwable) {
        Timber.w(t, "SeedDefaultTagsWorker failed")
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "callNest.tags.seed"
    }
}
