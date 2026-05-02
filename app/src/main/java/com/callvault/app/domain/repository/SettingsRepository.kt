package com.callvault.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Reactive settings facade. Mirrors the DataStore keys defined in spec §4.
 *
 * Each setting exposes a `Flow<T>` for observation plus a `suspend` setter.
 * Boolean defaults follow the spec: most are user-facing toggles.
 */
interface SettingsRepository {

    // Sync
    val syncEnabled: Flow<Boolean>
    suspend fun setSyncEnabled(value: Boolean)

    val syncIntervalMinutes: Flow<Int>
    suspend fun setSyncIntervalMinutes(value: Int)

    val syncWifiOnly: Flow<Boolean>
    suspend fun setSyncWifiOnly(value: Boolean)

    val syncWhenChargingOnly: Flow<Boolean>
    suspend fun setSyncWhenChargingOnly(value: Boolean)

    val syncOnAppOpen: Flow<Boolean>
    suspend fun setSyncOnAppOpen(value: Boolean)

    val syncOnReboot: Flow<Boolean>
    suspend fun setSyncOnReboot(value: Boolean)

    val lastSyncCallId: Flow<Long>
    suspend fun setLastSyncCallId(value: Long)

    val lastSyncAt: Flow<Long>
    suspend fun setLastSyncAt(value: Long)

    // Auto-save
    val autoSaveEnabled: Flow<Boolean>
    suspend fun setAutoSaveEnabled(value: Boolean)

    val autoSavePrefix: Flow<String>
    suspend fun setAutoSavePrefix(value: String)

    val autoSaveIncludeSimTag: Flow<Boolean>
    suspend fun setAutoSaveIncludeSimTag(value: Boolean)

    val autoSaveSuffix: Flow<String>
    suspend fun setAutoSaveSuffix(value: String)

    val autoSaveContactGroupName: Flow<String>
    suspend fun setAutoSaveContactGroupName(value: String)

    val autoSaveContactGroupId: Flow<Long>
    suspend fun setAutoSaveContactGroupId(value: Long)

    val autoSavePhoneLabel: Flow<String>
    suspend fun setAutoSavePhoneLabel(value: String)

    val autoSavePhoneLabelCustom: Flow<String>
    suspend fun setAutoSavePhoneLabelCustom(value: String)

    // Region
    val defaultRegion: Flow<String>
    suspend fun setDefaultRegion(value: String)

    // Real-time / overlays
    val floatingBubbleEnabled: Flow<Boolean>
    suspend fun setFloatingBubbleEnabled(value: Boolean)

    val postCallPopupEnabled: Flow<Boolean>
    suspend fun setPostCallPopupEnabled(value: Boolean)

    val postCallPopupTimeoutSeconds: Flow<Int>
    suspend fun setPostCallPopupTimeoutSeconds(value: Int)

    val postCallPopupUnsavedOnly: Flow<Boolean>
    suspend fun setPostCallPopupUnsavedOnly(value: Boolean)

    // Notifications
    val followUpRemindersEnabled: Flow<Boolean>
    suspend fun setFollowUpRemindersEnabled(value: Boolean)

    val dailySummaryEnabled: Flow<Boolean>
    suspend fun setDailySummaryEnabled(value: Boolean)

    val updateAlertsEnabled: Flow<Boolean>
    suspend fun setUpdateAlertsEnabled(value: Boolean)

    // Lead score
    val leadScoreEnabled: Flow<Boolean>
    suspend fun setLeadScoreEnabled(value: Boolean)

    val leadScoreWeights: Flow<String>
    suspend fun setLeadScoreWeights(json: String)

    // Backup
    val autoBackupEnabled: Flow<Boolean>
    suspend fun setAutoBackupEnabled(value: Boolean)

    val autoBackupRetention: Flow<Int>
    suspend fun setAutoBackupRetention(value: Int)

    // Display
    val displayShowUnsavedPinned: Flow<Boolean>
    suspend fun setDisplayShowUnsavedPinned(value: Boolean)

    val displayGroupedByNumber: Flow<Boolean>
    suspend fun setDisplayGroupedByNumber(value: Boolean)

    // Privacy
    val privacyBlockHidden: Flow<Boolean>
    suspend fun setPrivacyBlockHidden(value: Boolean)

    val privacyHideBlocked: Flow<Boolean>
    suspend fun setPrivacyHideBlocked(value: Boolean)

    // Updates
    val updateChannel: Flow<String>
    suspend fun setUpdateChannel(value: String)

    val updateAutoCheck: Flow<Boolean>
    suspend fun setUpdateAutoCheck(value: Boolean)

    val lastUpdateCheck: Flow<Long>
    suspend fun setLastUpdateCheck(value: Long)

    // Onboarding / OEM
    val onboardingComplete: Flow<Boolean>
    suspend fun setOnboardingComplete(value: Boolean)

    val oemBatteryGuideShown: Flow<Boolean>
    suspend fun setOemBatteryGuideShown(value: Boolean)

    // Backup passphrase (encrypted backing store)
    suspend fun getBackupPassphrase(): String?
    suspend fun setBackupPassphrase(value: String?)

    // Sprint 4 — tags / alarms / bookmarks pinning
    val tagsSeeded: Flow<Boolean>
    suspend fun setTagsSeeded(value: Boolean)

    val exactAlarmFallbackActive: Flow<Boolean>
    suspend fun setExactAlarmFallbackActive(value: Boolean)

    /** Reactive list of normalized numbers the user has pinned to the top of the Bookmarks screen. */
    fun observePinnedBookmarks(): Flow<List<String>>

    /** Replaces the pinned bookmarks list (max 5 enforced by callers). */
    suspend fun setPinnedBookmarks(numbers: List<String>)
}
