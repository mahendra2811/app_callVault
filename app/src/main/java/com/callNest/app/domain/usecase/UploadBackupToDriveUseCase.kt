package com.callNest.app.domain.usecase

import android.content.Context
import com.callNest.app.data.backup.BackupManager
import com.callNest.app.data.backup.DriveAuthManager
import com.callNest.app.data.backup.DriveBackupClient
import com.callNest.app.data.export.ExportDestination
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Run a local encrypted backup to a private cache file, then upload it to
 * the user's Drive "callNest Backups" folder.
 */
class UploadBackupToDriveUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val driveAuth: DriveAuthManager,
    private val driveClient: DriveBackupClient
) {
    /** Result envelope for [invoke]. */
    sealed interface Result {
        /** Upload finished — Drive file id and original file name. */
        data class Success(val fileId: String, val fileName: String) : Result

        /** Anything else, with a user-friendly explanation. */
        data class Failure(val reason: String) : Result

        /** No persisted Drive auth — caller should kick off sign-in. */
        data object NotSignedIn : Result
    }

    suspend operator fun invoke(passphrase: String): Result {
        if (!driveAuth.isAuthorized()) return Result.NotSignedIn
        return try {
            val fileName = backupManager.defaultBackupName()
            val cacheTarget = File(context.cacheDir, fileName).apply { delete() }
            // Run the encrypted local backup straight into our cache file.
            backupManager.backup(
                passphrase,
                ExportDestination.PickedUri(android.net.Uri.fromFile(cacheTarget))
            )
            val fileId = driveClient.uploadBackup(cacheTarget)
            cacheTarget.delete()
            Result.Success(fileId, fileName)
        } catch (t: Throwable) {
            Result.Failure(t.message ?: "Unknown error")
        }
    }
}
