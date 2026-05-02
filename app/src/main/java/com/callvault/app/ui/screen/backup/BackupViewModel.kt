package com.callvault.app.ui.screen.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.export.ExportDestination
import com.callvault.app.data.prefs.SecurePrefs
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.data.work.DailyBackupWorker
import com.callvault.app.domain.usecase.BackupDatabaseUseCase
import com.callvault.app.domain.usecase.RestoreDatabaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI snapshot for the backup landing screen. */
data class BackupUiState(
    val passphraseSet: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val retention: Int = 7,
    val isWorking: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

/** ViewModel for [BackupScreen]. Pulls passphrase status + retention prefs. */
@HiltViewModel
class BackupViewModel @Inject constructor(
    application: Application,
    private val securePrefs: SecurePrefs,
    private val settings: SettingsDataStore,
    private val backup: BackupDatabaseUseCase,
    private val restore: RestoreDatabaseUseCase
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        // Initial sync.
        _state.update { it.copy(passphraseSet = securePrefs.getBackupPassphrase() != null) }
        viewModelScope.launch {
            settings.autoBackupEnabled.collect { v -> _state.update { it.copy(autoBackupEnabled = v) } }
        }
        viewModelScope.launch {
            settings.autoBackupRetention.collect { v -> _state.update { it.copy(retention = v) } }
        }
    }

    /** Persist (or clear) the backup passphrase. */
    fun setPassphrase(value: String?) {
        securePrefs.setBackupPassphrase(value)
        _state.update { it.copy(passphraseSet = !value.isNullOrBlank()) }
    }

    /** Run a manual backup using either the saved passphrase or [override]. */
    fun runBackup(override: String? = null) {
        val pass = override?.takeIf { it.isNotBlank() } ?: securePrefs.getBackupPassphrase()
        if (pass.isNullOrBlank()) {
            _state.update { it.copy(error = "Couldn't back up. Set a passphrase first.") }
            return
        }
        _state.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            try {
                val r = backup(pass, ExportDestination.Downloads(defaultName()))
                _state.update { it.copy(isWorking = false, message = "Backup saved: ${r.fileName}") }
                if (override != null) securePrefs.setBackupPassphrase(override)
            } catch (t: Throwable) {
                _state.update {
                    it.copy(isWorking = false, error = "Couldn't back up. ${t.message ?: "Unknown error"}")
                }
            }
        }
    }

    /** Restore the database from [uri] using [passphrase]. */
    fun runRestore(uri: Uri, passphrase: String) {
        _state.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            try {
                val r = restore(uri, passphrase)
                _state.update {
                    it.copy(
                        isWorking = false,
                        message = "Restored ${r.callsRestored} calls, ${r.tagsRestored} tags."
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(isWorking = false, error = "Couldn't restore. ${t.message ?: "Unknown error"}")
                }
            }
        }
    }

    /** Toggle the daily auto-backup worker. */
    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setAutoBackupEnabled(enabled)
            if (enabled) DailyBackupWorker.enqueue(getApplication())
            else DailyBackupWorker.cancel(getApplication())
        }
    }

    /** Persist the retention slider value (3..14). */
    fun setRetention(n: Int) {
        viewModelScope.launch { settings.setAutoBackupRetention(n.coerceIn(3, 14)) }
    }

    fun consumeMessage() { _state.update { it.copy(message = null) } }
    fun consumeError() { _state.update { it.copy(error = null) } }

    private fun defaultName(): String =
        "callvault-backup-${System.currentTimeMillis()}.cvb"

    /** Unused — keep stateIn import warm for future flows. */
    @Suppress("unused") private val starter = SharingStarted.WhileSubscribed(5_000)
}
