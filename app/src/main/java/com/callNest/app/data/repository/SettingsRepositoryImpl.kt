package com.callNest.app.data.repository

import com.callNest.app.data.prefs.SecurePrefs
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: SettingsDataStore,
    private val secure: SecurePrefs
) : SettingsRepository {

    override val syncEnabled: Flow<Boolean> get() = store.syncEnabled
    override suspend fun setSyncEnabled(value: Boolean) = store.setSyncEnabled(value)

    override val syncIntervalMinutes: Flow<Int> get() = store.syncIntervalMinutes
    override suspend fun setSyncIntervalMinutes(value: Int) = store.setSyncIntervalMinutes(value)

    override val syncWifiOnly: Flow<Boolean> get() = store.syncWifiOnly
    override suspend fun setSyncWifiOnly(value: Boolean) = store.setSyncWifiOnly(value)

    override val syncWhenChargingOnly: Flow<Boolean> get() = store.syncWhenChargingOnly
    override suspend fun setSyncWhenChargingOnly(value: Boolean) =
        store.setSyncWhenChargingOnly(value)

    override val syncOnAppOpen: Flow<Boolean> get() = store.syncOnAppOpen
    override suspend fun setSyncOnAppOpen(value: Boolean) = store.setSyncOnAppOpen(value)

    override val syncOnReboot: Flow<Boolean> get() = store.syncOnReboot
    override suspend fun setSyncOnReboot(value: Boolean) = store.setSyncOnReboot(value)

    override val lastSyncCallId: Flow<Long> get() = store.lastSyncCallId
    override suspend fun setLastSyncCallId(value: Long) = store.setLastSyncCallId(value)

    override val lastSyncAt: Flow<Long> get() = store.lastSyncAt
    override suspend fun setLastSyncAt(value: Long) = store.setLastSyncAt(value)

    override val autoSaveEnabled: Flow<Boolean> get() = store.autoSaveEnabled
    override suspend fun setAutoSaveEnabled(value: Boolean) = store.setAutoSaveEnabled(value)

    override val autoSavePrefix: Flow<String> get() = store.autoSavePrefix
    override suspend fun setAutoSavePrefix(value: String) = store.setAutoSavePrefix(value)

    override val autoSaveIncludeSimTag: Flow<Boolean> get() = store.autoSaveIncludeSimTag
    override suspend fun setAutoSaveIncludeSimTag(value: Boolean) =
        store.setAutoSaveIncludeSimTag(value)

    override val autoSaveSuffix: Flow<String> get() = store.autoSaveSuffix
    override suspend fun setAutoSaveSuffix(value: String) = store.setAutoSaveSuffix(value)

    override val autoSaveContactGroupName: Flow<String> get() = store.autoSaveContactGroupName
    override suspend fun setAutoSaveContactGroupName(value: String) =
        store.setAutoSaveContactGroupName(value)

    override val autoSaveContactGroupId: Flow<Long> get() = store.autoSaveContactGroupId
    override suspend fun setAutoSaveContactGroupId(value: Long) =
        store.setAutoSaveContactGroupId(value)

    override val autoSavePhoneLabel: Flow<String> get() = store.autoSavePhoneLabel
    override suspend fun setAutoSavePhoneLabel(value: String) = store.setAutoSavePhoneLabel(value)

    override val autoSavePhoneLabelCustom: Flow<String> get() = store.autoSavePhoneLabelCustom
    override suspend fun setAutoSavePhoneLabelCustom(value: String) =
        store.setAutoSavePhoneLabelCustom(value)

    override val defaultRegion: Flow<String> get() = store.defaultRegion
    override suspend fun setDefaultRegion(value: String) = store.setDefaultRegion(value)

    override val floatingBubbleEnabled: Flow<Boolean> get() = store.floatingBubbleEnabled
    override suspend fun setFloatingBubbleEnabled(value: Boolean) =
        store.setFloatingBubbleEnabled(value)

    override val postCallPopupEnabled: Flow<Boolean> get() = store.postCallPopupEnabled
    override suspend fun setPostCallPopupEnabled(value: Boolean) =
        store.setPostCallPopupEnabled(value)

    override val postCallPopupTimeoutSeconds: Flow<Int> get() = store.postCallPopupTimeoutSeconds
    override suspend fun setPostCallPopupTimeoutSeconds(value: Int) =
        store.setPostCallPopupTimeoutSeconds(value)

    override val postCallPopupUnsavedOnly: Flow<Boolean> get() = store.postCallPopupUnsavedOnly
    override suspend fun setPostCallPopupUnsavedOnly(value: Boolean) =
        store.setPostCallPopupUnsavedOnly(value)

    override val followUpRemindersEnabled: Flow<Boolean> get() = store.followUpRemindersEnabled
    override suspend fun setFollowUpRemindersEnabled(value: Boolean) =
        store.setFollowUpRemindersEnabled(value)

    override val dailySummaryEnabled: Flow<Boolean> get() = store.dailySummaryEnabled
    override suspend fun setDailySummaryEnabled(value: Boolean) =
        store.setDailySummaryEnabled(value)

    override val updateAlertsEnabled: Flow<Boolean> get() = store.updateAlertsEnabled
    override suspend fun setUpdateAlertsEnabled(value: Boolean) =
        store.setUpdateAlertsEnabled(value)

    override val leadScoreEnabled: Flow<Boolean> get() = store.leadScoreEnabled
    override suspend fun setLeadScoreEnabled(value: Boolean) = store.setLeadScoreEnabled(value)

    override val leadScoreWeights: Flow<String> get() = store.leadScoreWeights
    override suspend fun setLeadScoreWeights(json: String) = store.setLeadScoreWeights(json)

    override val autoBackupEnabled: Flow<Boolean> get() = store.autoBackupEnabled
    override suspend fun setAutoBackupEnabled(value: Boolean) =
        store.setAutoBackupEnabled(value)

    override val autoBackupRetention: Flow<Int> get() = store.autoBackupRetention
    override suspend fun setAutoBackupRetention(value: Int) =
        store.setAutoBackupRetention(value)

    override val displayShowUnsavedPinned: Flow<Boolean> get() = store.displayShowUnsavedPinned
    override suspend fun setDisplayShowUnsavedPinned(value: Boolean) =
        store.setDisplayShowUnsavedPinned(value)

    override val displayGroupedByNumber: Flow<Boolean> get() = store.displayGroupedByNumber
    override suspend fun setDisplayGroupedByNumber(value: Boolean) =
        store.setDisplayGroupedByNumber(value)

    override val privacyBlockHidden: Flow<Boolean> get() = store.privacyBlockHidden
    override suspend fun setPrivacyBlockHidden(value: Boolean) =
        store.setPrivacyBlockHidden(value)

    override val privacyHideBlocked: Flow<Boolean> get() = store.privacyHideBlocked
    override suspend fun setPrivacyHideBlocked(value: Boolean) =
        store.setPrivacyHideBlocked(value)

    override val updateChannel: Flow<String> get() = store.updateChannel
    override suspend fun setUpdateChannel(value: String) = store.setUpdateChannel(value)

    override val updateAutoCheck: Flow<Boolean> get() = store.updateAutoCheck
    override suspend fun setUpdateAutoCheck(value: Boolean) = store.setUpdateAutoCheck(value)

    override val lastUpdateCheck: Flow<Long> get() = store.lastUpdateCheck
    override suspend fun setLastUpdateCheck(value: Long) = store.setLastUpdateCheck(value)

    override val onboardingComplete: Flow<Boolean> get() = store.onboardingComplete
    override suspend fun setOnboardingComplete(value: Boolean) =
        store.setOnboardingComplete(value)

    override val oemBatteryGuideShown: Flow<Boolean> get() = store.oemBatteryGuideShown
    override suspend fun setOemBatteryGuideShown(value: Boolean) =
        store.setOemBatteryGuideShown(value)

    override suspend fun getBackupPassphrase(): String? =
        withContext(Dispatchers.IO) { secure.getBackupPassphrase() }

    override suspend fun setBackupPassphrase(value: String?) =
        withContext(Dispatchers.IO) { secure.setBackupPassphrase(value) }

    // ---------- Sprint 4 ----------

    override val tagsSeeded: Flow<Boolean> get() = store.tagsSeeded
    override suspend fun setTagsSeeded(value: Boolean) = store.setTagsSeeded(value)

    override val exactAlarmFallbackActive: Flow<Boolean> get() = store.exactAlarmFallbackActive
    override suspend fun setExactAlarmFallbackActive(value: Boolean) =
        store.setExactAlarmFallbackActive(value)

    override fun observePinnedBookmarks(): Flow<List<String>> =
        store.pinnedBookmarkNumbersJson.map { decodePinned(it) }

    override suspend fun setPinnedBookmarks(numbers: List<String>) {
        val capped = numbers.distinct().take(MAX_PINNED_BOOKMARKS)
        store.setPinnedBookmarkNumbersJson(encodePinned(capped))
    }

    private companion object {
        const val MAX_PINNED_BOOKMARKS = 5
        val pinJson: Json = Json { ignoreUnknownKeys = true }
        private val pinSerializer = ListSerializer(String.serializer())
        fun decodePinned(raw: String): List<String> =
            runCatching { pinJson.decodeFromString(pinSerializer, raw) }.getOrDefault(emptyList())
        fun encodePinned(list: List<String>): String =
            pinJson.encodeToString(pinSerializer, list)
    }
}
