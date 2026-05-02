package com.callvault.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.domain.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State + actions for the Update settings screen. */
@HiltViewModel
class UpdateSettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    private val repo: UpdateRepository
) : ViewModel() {

    val channel: StateFlow<String> =
        settings.updateChannel.stateIn(viewModelScope, SharingStarted.Eagerly, "stable")
    val autoCheck: StateFlow<Boolean> =
        settings.updateAutoCheck.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val lastChecked: StateFlow<Long> =
        settings.lastUpdateCheck.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val state = repo.state

    fun setChannel(c: String) = viewModelScope.launch { settings.setUpdateChannel(c) }
    fun setAutoCheck(v: Boolean) = viewModelScope.launch { settings.setUpdateAutoCheck(v) }
    fun checkNow() = viewModelScope.launch { repo.checkNow() }
    fun clearSkipped() = viewModelScope.launch { repo.clearSkipped() }
}
