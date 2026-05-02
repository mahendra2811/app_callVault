package com.callvault.app.domain.repository

import com.callvault.app.data.update.ChannelManifest
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Self-update facade observed by the UI layer. Implementations wrap
 * `UpdateChecker` + `UpdateDownloader` + `UpdateInstaller` and expose a
 * single [state] StateFlow.
 */
interface UpdateRepository {

    /** Current lifecycle state for the update flow. */
    val state: StateFlow<UpdateState>

    /** Run a manifest check now. Updates [state]. */
    suspend fun checkNow()

    /** Begin downloading [manifest]; progress is reported via [state]. */
    suspend fun startDownload(manifest: ChannelManifest)

    /** Hand off the downloaded [file] to the system installer. */
    fun install(file: File)

    /** Persist a "skip this version" flag for [versionCode]. */
    suspend fun skip(versionCode: Int)

    /** Forget every previously-skipped version. */
    suspend fun clearSkipped()

    /** Reset [state] to [UpdateState.Idle] for the current session. */
    fun dismissBanner()
}

/** Lifecycle states surfaced to the UI. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState

    data class Available(
        val channel: String,
        val manifest: ChannelManifest,
        val isSkipped: Boolean
    ) : UpdateState

    data class Downloading(val progress: Int) : UpdateState

    data class ReadyToInstall(
        val file: File,
        val manifest: ChannelManifest
    ) : UpdateState

    data object Installing : UpdateState

    data class Error(val reason: String) : UpdateState
}
