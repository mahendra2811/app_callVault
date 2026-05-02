package com.callvault.app.data.work

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.callvault.app.data.backup.BackupManager
import com.callvault.app.data.export.ExportDestination
import com.callvault.app.data.prefs.SecurePrefs
import com.callvault.app.data.prefs.SettingsDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that runs an encrypted backup and rotates older files.
 *
 * Reads `autoBackupEnabled`, `autoBackupRetention`, and the stored
 * passphrase. If any of them is missing, the worker no-ops with `success`.
 */
@HiltWorker
class DailyBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsDataStore,
    private val securePrefs: SecurePrefs,
    private val backupManager: BackupManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val enabled = settings.autoBackupEnabled.first()
            val passphrase = securePrefs.getBackupPassphrase()
            if (!enabled || passphrase.isNullOrBlank()) {
                Timber.i("DailyBackupWorker: skipped (enabled=$enabled, hasPass=${passphrase != null})")
                return Result.success()
            }
            val keep = settings.autoBackupRetention.first().coerceIn(3, 14)
            val name = backupManager.defaultBackupName()
            backupManager.backup(passphrase, ExportDestination.Downloads(name))
            rotateOlder(keep)
            Result.success()
        } catch (t: Throwable) {
            Timber.w(t, "DailyBackupWorker failed")
            Result.retry()
        }
    }

    /** Delete `.cvb` files in Downloads beyond [keep] most recent. */
    private fun rotateOlder(keep: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val resolver = applicationContext.contentResolver
        val proj = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED
        )
        val sel = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("callvault-backup-%.cvb")
        val sort = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, proj, sel, args, sort)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            var i = 0
            while (c.moveToNext()) {
                if (i++ < keep) continue
                val id = c.getLong(idIdx)
                val rowUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                try {
                    resolver.delete(rowUri, null, null)
                } catch (t: Throwable) {
                    Timber.w(t, "Couldn't delete old backup uri=$rowUri")
                }
            }
        }
    }

    companion object {
        const val UNIQUE_NAME = "callvault.daily_backup"

        /** Enqueue the periodic worker (~24h cadence; WorkManager picks the slot). */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            val req = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, req
            )
        }

        /** Cancel any pending periodic backup. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
