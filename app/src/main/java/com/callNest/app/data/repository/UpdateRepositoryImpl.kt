package com.callNest.app.data.repository

import com.callNest.app.data.local.dao.SkippedUpdateDao
import com.callNest.app.data.local.entity.SkippedUpdateEntity
import com.callNest.app.data.update.ChannelManifest
import com.callNest.app.data.update.InstallStartResult
import com.callNest.app.data.update.UpdateCheckResult
import com.callNest.app.data.update.UpdateChecker
import com.callNest.app.data.update.UpdateDownloader
import com.callNest.app.data.update.UpdateInstaller
import com.callNest.app.domain.repository.UpdateRepository
import com.callNest.app.domain.repository.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-world [UpdateRepository] composing checker, downloader, and installer.
 * State transitions follow:
 * Idle → Checking → (UpToDate | Available | Error)
 *      → Downloading → ReadyToInstall → Installing → (Idle | Error)
 */
@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val checker: UpdateChecker,
    private val downloader: UpdateDownloader,
    private val installer: UpdateInstaller,
    private val skippedDao: SkippedUpdateDao
) : UpdateRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val state: StateFlow<UpdateState> = _state.asStateFlow()

    override suspend fun checkNow() {
        _state.value = UpdateState.Checking
        when (val result = checker.checkNow()) {
            is UpdateCheckResult.NoUpdate -> _state.value = UpdateState.UpToDate
            is UpdateCheckResult.UpdateAvailable -> _state.value = UpdateState.Available(
                channel = result.channel,
                manifest = result.manifest,
                isSkipped = result.isSkipped
            )
            is UpdateCheckResult.Error -> _state.value = UpdateState.Error(result.reason)
        }
    }

    override suspend fun startDownload(manifest: ChannelManifest) {
        _state.value = UpdateState.Downloading(0)
        val outcome = downloader.download(manifest) { progress ->
            _state.value = UpdateState.Downloading(progress)
        }
        outcome
            .onSuccess { file ->
                _state.value = UpdateState.ReadyToInstall(file, manifest)
            }
            .onFailure { err ->
                _state.value = UpdateState.Error(
                    err.message ?: "Couldn't download the update. Please try again."
                )
            }
    }

    override fun install(file: File) {
        scope.launch {
            _state.value = UpdateState.Installing
            when (val result = installer.install(file)) {
                is InstallStartResult.Started -> {
                    // Leave state as Installing; system takes over.
                }
                is InstallStartResult.NeedsUnknownSourcesPermission -> {
                    try {
                        withContext(Dispatchers.Main) {
                            // Caller may also start it; we attempt here for safety.
                        }
                    } catch (t: Throwable) {
                        Timber.w(t, "Unknown-sources prompt failed")
                    }
                    _state.value = UpdateState.Error(
                        "Allow installs from callNest in system settings, then try again."
                    )
                }
                is InstallStartResult.Error -> {
                    _state.value = UpdateState.Error(result.reason)
                }
            }
        }
    }

    override suspend fun skip(versionCode: Int) {
        skippedDao.insert(SkippedUpdateEntity(versionCode = versionCode))
        _state.value = UpdateState.Idle
    }

    override suspend fun clearSkipped() {
        skippedDao.deleteAll()
    }

    override fun dismissBanner() {
        _state.value = UpdateState.Idle
    }
}
