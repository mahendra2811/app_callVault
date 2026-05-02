package com.callvault.app.domain.usecase

import android.net.Uri
import com.callvault.app.data.backup.BackupManager
import com.callvault.app.data.backup.RestoreResult
import com.callvault.app.data.export.ExportDestination
import com.callvault.app.data.export.ExportResult
import javax.inject.Inject

/** Run an encrypted backup of the full database (Sprint 9). */
class BackupDatabaseUseCase @Inject constructor(private val manager: BackupManager) {
    suspend operator fun invoke(passphrase: String, destination: ExportDestination): ExportResult =
        manager.backup(passphrase, destination)
}

/** Restore the database from an encrypted backup file (Sprint 9). */
class RestoreDatabaseUseCase @Inject constructor(private val manager: BackupManager) {
    suspend operator fun invoke(uri: Uri, passphrase: String): RestoreResult =
        manager.restore(uri, passphrase)
}
