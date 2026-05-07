package com.callNest.app.ui.screen.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.update.ChannelManifest
import com.callNest.app.domain.repository.UpdateRepository
import com.callNest.app.domain.repository.UpdateState
import com.callNest.app.domain.usecase.CheckForUpdateUseCase
import com.callNest.app.domain.usecase.DownloadAndInstallUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Backs the [UpdateAvailableScreen] with repo state + intent handlers. */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repo: UpdateRepository,
    private val checkUseCase: CheckForUpdateUseCase,
    private val downloadUseCase: DownloadAndInstallUpdateUseCase
) : ViewModel() {

    val state: StateFlow<UpdateState> = repo.state

    fun onCheck() = viewModelScope.launch { checkUseCase() }

    fun onUpdate(manifest: ChannelManifest) = viewModelScope.launch {
        downloadUseCase(manifest)
    }

    fun onSkip(versionCode: Int) = viewModelScope.launch { repo.skip(versionCode) }

    fun onInstall(file: File) = repo.install(file)

    fun onDismiss() = repo.dismissBanner()
}
