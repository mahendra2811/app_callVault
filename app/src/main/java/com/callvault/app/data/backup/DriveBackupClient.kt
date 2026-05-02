package com.callvault.app.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Single Drive file metadata returned by [DriveBackupClient.listBackups]. */
data class DriveFile(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdTime: String
)

/**
 * Thin Drive REST v3 wrapper. Auth tokens are pulled from [DriveAuthManager]
 * for every call so refreshes happen transparently.
 */
@Singleton
class DriveBackupClient @Inject constructor(
    private val auth: DriveAuthManager
) {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Resolve (or create) the "CallVault Backups" folder; returns its file id. */
    suspend fun ensureBackupFolder(): String = withContext(Dispatchers.IO) {
        val token = auth.freshAccessToken()
        val q = "name='$FOLDER_NAME' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val listReq = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(q, "UTF-8")}&fields=files(id,name)")
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(listReq).execute().use { resp ->
            ensureSuccess(resp)
            val files = JSONObject(resp.body!!.string()).optJSONArray("files") ?: JSONArray()
            if (files.length() > 0) return@withContext files.getJSONObject(0).getString("id")
        }
        val body = JSONObject().apply {
            put("name", FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }.toString().toRequestBody(JSON)
        val createReq = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?fields=id")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        http.newCall(createReq).execute().use { resp ->
            ensureSuccess(resp)
            JSONObject(resp.body!!.string()).getString("id")
        }
    }

    /** Multipart-upload [file] into the "CallVault Backups" folder. Returns the new file id. */
    suspend fun uploadBackup(file: File): String = withContext(Dispatchers.IO) {
        val token = auth.freshAccessToken()
        val folderId = ensureBackupFolder()
        val metadata = JSONObject().apply {
            put("name", file.name)
            put("parents", JSONArray().put(folderId))
        }.toString().toRequestBody(JSON)
        val multipart = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata)
            .addPart(file.asRequestBody(OCTET))
            .build()
        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id")
            .header("Authorization", "Bearer $token")
            .post(multipart)
            .build()
        http.newCall(req).execute().use { resp ->
            ensureSuccess(resp)
            JSONObject(resp.body!!.string()).getString("id")
        }
    }

    /** List `.cvb` backups inside the CallVault Backups folder. */
    suspend fun listBackups(): List<DriveFile> = withContext(Dispatchers.IO) {
        val token = auth.freshAccessToken()
        val folderId = ensureBackupFolder()
        val q = "'$folderId' in parents and trashed=false"
        val req = Request.Builder()
            .url(
                "https://www.googleapis.com/drive/v3/files" +
                    "?q=${java.net.URLEncoder.encode(q, "UTF-8")}" +
                    "&fields=files(id,name,size,createdTime)" +
                    "&orderBy=createdTime desc"
            )
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { resp ->
            ensureSuccess(resp)
            val arr = JSONObject(resp.body!!.string()).optJSONArray("files") ?: JSONArray()
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DriveFile(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    sizeBytes = o.optString("size", "0").toLongOrNull() ?: 0L,
                    createdTime = o.optString("createdTime", "")
                )
            }
        }
    }

    /** Stream the file at [id] into [dest]. */
    suspend fun downloadBackup(id: String, dest: File) = withContext(Dispatchers.IO) {
        val token = auth.freshAccessToken()
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$id?alt=media")
            .header("Authorization", "Bearer $token")
            .build()
        http.newCall(req).execute().use { resp ->
            ensureSuccess(resp)
            dest.outputStream().use { out -> resp.body!!.byteStream().copyTo(out) }
        }
    }

    private fun ensureSuccess(resp: Response) {
        if (!resp.isSuccessful) {
            throw IOException("Drive request failed: ${resp.code} ${resp.message}")
        }
    }

    private companion object {
        const val FOLDER_NAME = "CallVault Backups"
        val JSON = "application/json; charset=utf-8".toMediaType()
        val OCTET = "application/octet-stream".toMediaType()
    }
}
