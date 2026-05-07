package com.callNest.app.ui.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.local.dao.NoteDao
import com.callNest.app.data.local.dao.SearchHistoryDao
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.data.work.DailySummaryWorker
import com.callNest.app.domain.usecase.ResetAllDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Snapshot of every setting the master Settings screen renders/edits. */
data class SettingsUiState(
    val syncEnabled: Boolean = true,
    val syncInterval: Int = 15,
    val syncWifiOnly: Boolean = false,
    val syncChargingOnly: Boolean = false,
    val syncOnAppOpen: Boolean = true,
    val syncOnReboot: Boolean = true,

    val autoSaveEnabled: Boolean = true,
    val bubbleEnabled: Boolean = true,
    val popupEnabled: Boolean = true,

    val followUpReminders: Boolean = true,
    val dailySummary: Boolean = false,
    val updateAlerts: Boolean = true,

    val leadScoringEnabled: Boolean = true,

    val autoBackupEnabled: Boolean = true,
    val autoBackupRetention: Int = 7,

    val pinnedAtTop: Boolean = true,
    val groupedByNumber: Boolean = false,

    val blockHidden: Boolean = false,
    val hideBlocked: Boolean = false,

    val biometricLock: Boolean = false,
    val analyticsConsent: Boolean = false,
    val hotLeadAlerts: Boolean = true,
    val weeklyDigest: Boolean = true,
    val aiDigest: Boolean = false,
    val anthropicKeySet: Boolean = false,
)

/**
 * Sprint 11 — drives the master Settings screen. All setters debounce-write
 * to [SettingsDataStore]; privacy actions are delegated to dedicated DAOs and
 * use cases.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    app: Application,
    private val settings: SettingsDataStore,
    private val notes: NoteDao,
    private val search: SearchHistoryDao,
    private val resetAll: ResetAllDataUseCase,
    private val secrets: com.callNest.app.data.secrets.SecretStore,
) : AndroidViewModel(app) {

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<SettingsUiState> = combine(
        listOf(
            settings.syncEnabled, settings.syncIntervalMinutes,
            settings.syncWifiOnly, settings.syncWhenChargingOnly,
            settings.syncOnAppOpen, settings.syncOnReboot,
            settings.autoSaveEnabled,
            settings.floatingBubbleEnabled, settings.postCallPopupEnabled,
            settings.followUpRemindersEnabled, settings.dailySummaryEnabled, settings.updateAlertsEnabled,
            settings.leadScoreEnabled,
            settings.autoBackupEnabled, settings.autoBackupRetention,
            settings.displayShowUnsavedPinned, settings.displayGroupedByNumber,
            settings.privacyBlockHidden, settings.privacyHideBlocked,
            settings.biometricLockEnabled, settings.analyticsConsent,
            settings.hotLeadAlertsEnabled, settings.weeklyDigestEnabled,
            settings.aiDigestEnabled
        ).map { it as kotlinx.coroutines.flow.Flow<Any?> }
    ) { values: Array<Any?> ->
        SettingsUiState(
            syncEnabled = values[0] as Boolean,
            syncInterval = values[1] as Int,
            syncWifiOnly = values[2] as Boolean,
            syncChargingOnly = values[3] as Boolean,
            syncOnAppOpen = values[4] as Boolean,
            syncOnReboot = values[5] as Boolean,
            autoSaveEnabled = values[6] as Boolean,
            bubbleEnabled = values[7] as Boolean,
            popupEnabled = values[8] as Boolean,
            followUpReminders = values[9] as Boolean,
            dailySummary = values[10] as Boolean,
            updateAlerts = values[11] as Boolean,
            leadScoringEnabled = values[12] as Boolean,
            autoBackupEnabled = values[13] as Boolean,
            autoBackupRetention = values[14] as Int,
            pinnedAtTop = values[15] as Boolean,
            groupedByNumber = values[16] as Boolean,
            blockHidden = values[17] as Boolean,
            hideBlocked = values[18] as Boolean,
            biometricLock = values[19] as Boolean,
            analyticsConsent = values[20] as Boolean,
            hotLeadAlerts = values[21] as Boolean,
            weeklyDigest = values[22] as Boolean,
            aiDigest = values[23] as Boolean,
            anthropicKeySet = secrets.get(com.callNest.app.data.secrets.SecretStore.K_ANTHROPIC_KEY).isNotBlank(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    // -------- setters --------
    fun setSyncEnabled(v: Boolean) = launchSet { settings.setSyncEnabled(v) }
    fun setSyncWifiOnly(v: Boolean) = launchSet { settings.setSyncWifiOnly(v) }
    fun setSyncChargingOnly(v: Boolean) = launchSet { settings.setSyncWhenChargingOnly(v) }
    fun setSyncOnAppOpen(v: Boolean) = launchSet { settings.setSyncOnAppOpen(v) }
    fun setSyncOnReboot(v: Boolean) = launchSet { settings.setSyncOnReboot(v) }
    fun setSyncInterval(v: Int) = launchSet { settings.setSyncIntervalMinutes(v) }

    fun setAutoSaveEnabled(v: Boolean) = launchSet { settings.setAutoSaveEnabled(v) }
    fun setBubbleEnabled(v: Boolean) = launchSet { settings.setFloatingBubbleEnabled(v) }
    fun setPopupEnabled(v: Boolean) = launchSet { settings.setPostCallPopupEnabled(v) }

    fun setFollowUpReminders(v: Boolean) = launchSet { settings.setFollowUpRemindersEnabled(v) }
    fun setUpdateAlerts(v: Boolean) = launchSet { settings.setUpdateAlertsEnabled(v) }
    fun setDailySummary(v: Boolean) {
        launchSet {
            settings.setDailySummaryEnabled(v)
            if (v) DailySummaryWorker.schedule(getApplication())
            else DailySummaryWorker.cancel(getApplication())
        }
    }

    fun setLeadScoringEnabled(v: Boolean) = launchSet { settings.setLeadScoreEnabled(v) }

    fun setAutoBackupEnabled(v: Boolean) = launchSet { settings.setAutoBackupEnabled(v) }
    fun setAutoBackupRetention(v: Int) = launchSet { settings.setAutoBackupRetention(v) }

    fun setPinnedAtTop(v: Boolean) = launchSet { settings.setDisplayShowUnsavedPinned(v) }
    fun setGroupedByNumber(v: Boolean) = launchSet { settings.setDisplayGroupedByNumber(v) }

    fun setBlockHidden(v: Boolean) = launchSet { settings.setPrivacyBlockHidden(v) }
    fun setHideBlocked(v: Boolean) = launchSet { settings.setPrivacyHideBlocked(v) }
    fun setBiometricLock(v: Boolean) = launchSet { settings.setBiometricLockEnabled(v) }
    fun setAnalyticsConsent(v: Boolean) = launchSet { settings.setAnalyticsConsent(v) }
    fun setHotLeadAlerts(v: Boolean) = launchSet { settings.setHotLeadAlertsEnabled(v) }
    fun setAiDigest(v: Boolean) = launchSet { settings.setAiDigestEnabled(v) }
    fun setAnthropicKey(v: String) = launchSet {
        secrets.set(com.callNest.app.data.secrets.SecretStore.K_ANTHROPIC_KEY, v.trim())
    }
    fun setWeeklyDigest(v: Boolean) {
        launchSet {
            settings.setWeeklyDigestEnabled(v)
            if (v) com.callNest.app.data.work.WeeklyDigestWorker.schedule(getApplication())
            else com.callNest.app.data.work.WeeklyDigestWorker.cancel(getApplication())
        }
    }

    // -------- privacy actions --------
    fun clearSearchHistory() = launchSet { search.deleteAll() }
    fun clearAllNotes() = launchSet {
        notes.deleteAll(); notes.deleteAllHistory()
    }
    suspend fun resetAllData(): Boolean = resetAll()

    private inline fun launchSet(crossinline block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() } }
    }
}
