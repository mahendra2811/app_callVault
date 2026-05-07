package com.callNest.app.domain.usecase

import com.callNest.app.data.update.ChannelManifest
import com.callNest.app.domain.repository.UpdateRepository
import javax.inject.Inject

/**
 * Sprint 10 — kicks the manifest fetch + version comparison via the
 * shared [UpdateRepository]. The repo's `state` flow surfaces the result.
 */
class CheckForUpdateUseCase @Inject constructor(
    private val repo: UpdateRepository
) {
    suspend operator fun invoke() = repo.checkNow()
}

/**
 * Sprint 10 — initiates download + verification for an Available manifest.
 * Final state transitions land back on [UpdateRepository.state].
 */
class DownloadAndInstallUpdateUseCase @Inject constructor(
    private val repo: UpdateRepository
) {
    suspend operator fun invoke(manifest: ChannelManifest) = repo.startDownload(manifest)
}
