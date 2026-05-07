package com.callNest.app.data.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads APK files described by a [ChannelManifest] using Android's
 * built-in [DownloadManager], verifies SHA-256, and surfaces progress.
 *
 * The downloader writes into the app's external Downloads dir so the
 * package installer can read it via FileProvider.
 */
@Singleton
class UpdateDownloader @Inject constructor(
    @ApplicationContext private val ctx: Context
) {

    /**
     * Enqueue a download for [manifest] and suspend until it finishes.
     *
     * @param onProgress invoked on the calling dispatcher with 0..100 ints.
     * @return [Result.success] with the verified file, or [Result.failure]
     *         with a user-friendly message.
     */
    suspend fun download(
        manifest: ChannelManifest,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return@withContext Result.failure(
                Exception("Couldn't access download storage. Please free up space and try again.")
            )
        val target = File(dir, "callNest-${manifest.version}.apk")
        if (target.exists()) target.delete()

        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(manifest.apkUrl))
            .setMimeType("application/vnd.android.package-archive")
            .setDestinationUri(Uri.fromFile(target))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setTitle("callNest v${manifest.version}")

        val id = dm.enqueue(request)
        try {
            while (true) {
                val cursor = dm.query(DownloadManager.Query().setFilterById(id))
                cursor.use { c ->
                    if (!c.moveToFirst()) {
                        return@withContext Result.failure(Exception("Update download was cancelled."))
                    }
                    val statusCol = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val totalCol = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val soFarCol = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val reasonCol = c.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val status = c.getInt(statusCol)
                    val total = c.getLong(totalCol)
                    val soFar = c.getLong(soFarCol)
                    if (total > 0) {
                        onProgress((soFar * 100 / total).toInt().coerceIn(0, 100))
                    }
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            onProgress(100)
                            return@withContext verify(target, manifest)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = c.getInt(reasonCol)
                            target.delete()
                            return@withContext Result.failure(
                                Exception(mapFailureReason(reason))
                            )
                        }
                    }
                }
                delay(1000)
            }
            @Suppress("UNREACHABLE_CODE")
            Result.failure(Exception("Update download stopped unexpectedly."))
        } catch (t: Throwable) {
            Timber.w(t, "Download failed")
            runCatching { dm.remove(id) }
            Result.failure(Exception("Couldn't download the update. Please try again."))
        }
    }

    private fun verify(file: File, manifest: ChannelManifest): Result<File> {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buf = ByteArray(8192)
                while (true) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    digest.update(buf, 0, read)
                }
            }
            val hex = digest.digest().joinToString("") { "%02x".format(it) }
            if (hex != manifest.sha256.lowercase()) {
                file.delete()
                Result.failure(Exception("Update file is corrupted. Please try again."))
            } else {
                Result.success(file)
            }
        } catch (t: Throwable) {
            Timber.w(t, "Hash verify failed")
            file.delete()
            Result.failure(Exception("Couldn't verify the update. Please try again."))
        }
    }

    private fun mapFailureReason(reason: Int): String = when (reason) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE ->
            "Not enough space to download the update."
        DownloadManager.ERROR_HTTP_DATA_ERROR,
        DownloadManager.ERROR_TOO_MANY_REDIRECTS,
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE ->
            "Couldn't reach update server. Tap to retry."
        DownloadManager.ERROR_CANNOT_RESUME -> "Download was interrupted. Please try again."
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Storage unavailable. Please try again."
        else -> "Couldn't download the update. Please try again."
    }
}
