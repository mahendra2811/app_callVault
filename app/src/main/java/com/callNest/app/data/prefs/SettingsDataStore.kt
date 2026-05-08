package com.callNest.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "callNest_settings")

/**
 * Typed wrapper around Preferences DataStore for every key listed in spec
 * §4 "DataStore Preferences". Each setting exposes a [Flow] for observation
 * plus a `suspend` setter.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.dataStore

    private fun <T> Flow<Preferences>.read(key: Preferences.Key<T>, default: T): Flow<T> =
        map { it[key] ?: default }

    private suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        ds.edit { it[key] = value }
    }

    // ---------- Sync ----------
    val syncEnabled: Flow<Boolean> = ds.data.read(K_SYNC_ENABLED, true)
    suspend fun setSyncEnabled(v: Boolean) = write(K_SYNC_ENABLED, v)

    val syncIntervalMinutes: Flow<Int> = ds.data.read(K_SYNC_INTERVAL_MIN, 15)
    suspend fun setSyncIntervalMinutes(v: Int) = write(K_SYNC_INTERVAL_MIN, v)

    val syncWifiOnly: Flow<Boolean> = ds.data.read(K_SYNC_WIFI_ONLY, false)
    suspend fun setSyncWifiOnly(v: Boolean) = write(K_SYNC_WIFI_ONLY, v)

    val syncWhenChargingOnly: Flow<Boolean> = ds.data.read(K_SYNC_CHARGING_ONLY, false)
    suspend fun setSyncWhenChargingOnly(v: Boolean) = write(K_SYNC_CHARGING_ONLY, v)

    val syncOnAppOpen: Flow<Boolean> = ds.data.read(K_SYNC_ON_APP_OPEN, true)
    suspend fun setSyncOnAppOpen(v: Boolean) = write(K_SYNC_ON_APP_OPEN, v)

    val syncOnReboot: Flow<Boolean> = ds.data.read(K_SYNC_ON_REBOOT, true)
    suspend fun setSyncOnReboot(v: Boolean) = write(K_SYNC_ON_REBOOT, v)

    val lastSyncCallId: Flow<Long> = ds.data.read(K_LAST_SYNC_CALL_ID, 0L)
    suspend fun setLastSyncCallId(v: Long) = write(K_LAST_SYNC_CALL_ID, v)

    val lastSyncAt: Flow<Long> = ds.data.read(K_LAST_SYNC_AT, 0L)
    suspend fun setLastSyncAt(v: Long) = write(K_LAST_SYNC_AT, v)

    // ---------- Auto-save ----------
    val autoSaveEnabled: Flow<Boolean> = ds.data.read(K_AUTO_SAVE_ENABLED, true)
    suspend fun setAutoSaveEnabled(v: Boolean) = write(K_AUTO_SAVE_ENABLED, v)

    val autoSavePrefix: Flow<String> = ds.data.read(K_AUTO_SAVE_PREFIX, "callNest")
    suspend fun setAutoSavePrefix(v: String) = write(K_AUTO_SAVE_PREFIX, v)

    val autoSaveIncludeSimTag: Flow<Boolean> = ds.data.read(K_AUTO_SAVE_INCLUDE_SIM_TAG, true)
    suspend fun setAutoSaveIncludeSimTag(v: Boolean) = write(K_AUTO_SAVE_INCLUDE_SIM_TAG, v)

    val autoSaveSuffix: Flow<String> = ds.data.read(K_AUTO_SAVE_SUFFIX, "")
    suspend fun setAutoSaveSuffix(v: String) = write(K_AUTO_SAVE_SUFFIX, v)

    val autoSaveContactGroupName: Flow<String> =
        ds.data.read(K_AUTO_SAVE_GROUP_NAME, "callNest Inquiries")
    suspend fun setAutoSaveContactGroupName(v: String) = write(K_AUTO_SAVE_GROUP_NAME, v)

    val autoSaveContactGroupId: Flow<Long> = ds.data.read(K_AUTO_SAVE_GROUP_ID, -1L)
    suspend fun setAutoSaveContactGroupId(v: Long) = write(K_AUTO_SAVE_GROUP_ID, v)

    val autoSavePhoneLabel: Flow<String> = ds.data.read(K_AUTO_SAVE_PHONE_LABEL, "Mobile")
    suspend fun setAutoSavePhoneLabel(v: String) = write(K_AUTO_SAVE_PHONE_LABEL, v)

    val autoSavePhoneLabelCustom: Flow<String> =
        ds.data.read(K_AUTO_SAVE_PHONE_LABEL_CUSTOM, "")
    suspend fun setAutoSavePhoneLabelCustom(v: String) = write(K_AUTO_SAVE_PHONE_LABEL_CUSTOM, v)

    // ---------- Region ----------
    val defaultRegion: Flow<String> = ds.data.read(K_DEFAULT_REGION, "IN")
    suspend fun setDefaultRegion(v: String) = write(K_DEFAULT_REGION, v)

    // ---------- Real-time / overlays ----------
    val floatingBubbleEnabled: Flow<Boolean> = ds.data.read(K_FLOATING_BUBBLE, true)
    suspend fun setFloatingBubbleEnabled(v: Boolean) = write(K_FLOATING_BUBBLE, v)

    val postCallPopupEnabled: Flow<Boolean> = ds.data.read(K_POST_CALL_POPUP, true)
    suspend fun setPostCallPopupEnabled(v: Boolean) = write(K_POST_CALL_POPUP, v)

    val postCallPopupTimeoutSeconds: Flow<Int> = ds.data.read(K_POST_CALL_POPUP_TIMEOUT, 8)
    suspend fun setPostCallPopupTimeoutSeconds(v: Int) = write(K_POST_CALL_POPUP_TIMEOUT, v)

    val postCallPopupUnsavedOnly: Flow<Boolean> =
        ds.data.read(K_POST_CALL_POPUP_UNSAVED_ONLY, false)
    suspend fun setPostCallPopupUnsavedOnly(v: Boolean) = write(K_POST_CALL_POPUP_UNSAVED_ONLY, v)

    // ---------- Notifications ----------
    val followUpRemindersEnabled: Flow<Boolean> =
        ds.data.read(K_FOLLOW_UP_REMINDERS, true)
    suspend fun setFollowUpRemindersEnabled(v: Boolean) = write(K_FOLLOW_UP_REMINDERS, v)

    val dailySummaryEnabled: Flow<Boolean> = ds.data.read(K_DAILY_SUMMARY, false)
    suspend fun setDailySummaryEnabled(v: Boolean) = write(K_DAILY_SUMMARY, v)

    val updateAlertsEnabled: Flow<Boolean> = ds.data.read(K_UPDATE_ALERTS, true)
    suspend fun setUpdateAlertsEnabled(v: Boolean) = write(K_UPDATE_ALERTS, v)

    // ---------- Lead score ----------
    val leadScoreEnabled: Flow<Boolean> = ds.data.read(K_LEAD_SCORE_ENABLED, true)
    suspend fun setLeadScoreEnabled(v: Boolean) = write(K_LEAD_SCORE_ENABLED, v)

    val leadScoreWeights: Flow<String> = ds.data.read(K_LEAD_SCORE_WEIGHTS, "")
    suspend fun setLeadScoreWeights(json: String) = write(K_LEAD_SCORE_WEIGHTS, json)

    // ---------- Backup ----------
    val autoBackupEnabled: Flow<Boolean> = ds.data.read(K_AUTO_BACKUP_ENABLED, true)
    suspend fun setAutoBackupEnabled(v: Boolean) = write(K_AUTO_BACKUP_ENABLED, v)

    val autoBackupRetention: Flow<Int> = ds.data.read(K_AUTO_BACKUP_RETENTION, 7)
    suspend fun setAutoBackupRetention(v: Int) = write(K_AUTO_BACKUP_RETENTION, v)

    /** Master toggle for "save encrypted backup to Google Drive". */
    val backupDriveEnabled: Flow<Boolean> = ds.data.read(K_BACKUP_DRIVE_ENABLED, false)
    suspend fun setBackupDriveEnabled(enabled: Boolean) = write(K_BACKUP_DRIVE_ENABLED, enabled)

    /** Auto-upload after each local backup; only meaningful when [backupDriveEnabled] is true. */
    val driveAutoUpload: Flow<Boolean> = ds.data.read(K_DRIVE_AUTO_UPLOAD, true)
    suspend fun setDriveAutoUpload(enabled: Boolean) = write(K_DRIVE_AUTO_UPLOAD, enabled)

    // ---------- Display ----------
    val displayShowUnsavedPinned: Flow<Boolean> =
        ds.data.read(K_DISPLAY_UNSAVED_PINNED, true)
    suspend fun setDisplayShowUnsavedPinned(v: Boolean) = write(K_DISPLAY_UNSAVED_PINNED, v)

    val displayGroupedByNumber: Flow<Boolean> = ds.data.read(K_DISPLAY_GROUPED_BY_NUMBER, false)
    suspend fun setDisplayGroupedByNumber(v: Boolean) = write(K_DISPLAY_GROUPED_BY_NUMBER, v)

    // ---------- Privacy ----------
    val privacyBlockHidden: Flow<Boolean> = ds.data.read(K_PRIVACY_BLOCK_HIDDEN, false)
    suspend fun setPrivacyBlockHidden(v: Boolean) = write(K_PRIVACY_BLOCK_HIDDEN, v)

    val privacyHideBlocked: Flow<Boolean> = ds.data.read(K_PRIVACY_HIDE_BLOCKED, false)
    suspend fun setPrivacyHideBlocked(v: Boolean) = write(K_PRIVACY_HIDE_BLOCKED, v)

    // ---------- Updates ----------
    val updateChannel: Flow<String> = ds.data.read(K_UPDATE_CHANNEL, "stable")
    suspend fun setUpdateChannel(v: String) = write(K_UPDATE_CHANNEL, v)

    val updateAutoCheck: Flow<Boolean> = ds.data.read(K_UPDATE_AUTO_CHECK, true)
    suspend fun setUpdateAutoCheck(v: Boolean) = write(K_UPDATE_AUTO_CHECK, v)

    val lastUpdateCheck: Flow<Long> = ds.data.read(K_LAST_UPDATE_CHECK, 0L)
    suspend fun setLastUpdateCheck(v: Long) = write(K_LAST_UPDATE_CHECK, v)

    // ---------- Onboarding / OEM ----------
    val onboardingComplete: Flow<Boolean> = ds.data.read(K_ONBOARDING_COMPLETE, false)
    suspend fun setOnboardingComplete(v: Boolean) = write(K_ONBOARDING_COMPLETE, v)

    val oemBatteryGuideShown: Flow<Boolean> = ds.data.read(K_OEM_BATTERY_GUIDE_SHOWN, false)
    suspend fun setOemBatteryGuideShown(v: Boolean) = write(K_OEM_BATTERY_GUIDE_SHOWN, v)

    // ---------- Sprint 4 ----------
    /** True after [com.callNest.app.data.local.seed.DefaultTagsSeeder] has populated the system tags. */
    val tagsSeeded: Flow<Boolean> = ds.data.read(K_TAGS_SEEDED, false)
    suspend fun setTagsSeeded(v: Boolean) = write(K_TAGS_SEEDED, v)

    /** Banner flag flipped on by [com.callNest.app.data.service.alarm.ExactAlarmScheduler]
     *  when the OS denies `SCHEDULE_EXACT_ALARM`; the UI surfaces a "Reminders may be late"
     *  banner with a deep link to system settings. */
    val exactAlarmFallbackActive: Flow<Boolean> = ds.data.read(K_EXACT_ALARM_FALLBACK, false)
    suspend fun setExactAlarmFallbackActive(v: Boolean) = write(K_EXACT_ALARM_FALLBACK, v)

    /** JSON `List<String>` of normalized numbers the user has pinned to the top of the bookmarks list. */
    val pinnedBookmarkNumbersJson: Flow<String> = ds.data.read(K_PINNED_BOOKMARKS, "[]")
    suspend fun setPinnedBookmarkNumbersJson(v: String) = write(K_PINNED_BOOKMARKS, v)

    /**
     * Sprint 6 — Calls list sort mode (DateDesc | LeadScoreDesc | GroupedByNumber).
     * Stored as the enum name string for forward compatibility.
     */
    val callsSortMode: Flow<String> = ds.data.read(K_CALLS_SORT_MODE, "DateDesc")
    suspend fun setCallsSortMode(v: String) = write(K_CALLS_SORT_MODE, v)

    // ---------- Sprint 8 (Stats) ----------
    /** Last selected date-range preset (0=today, 1=7d, 2=30d, 3=thisMonth,
     *  4=lastMonth, 5=last90, 6=custom). */
    val statsLastRangePresetIndex: Flow<Int> =
        ds.data.read(K_STATS_LAST_RANGE_PRESET, 2)
    suspend fun setStatsLastRangePresetIndex(v: Int) =
        write(K_STATS_LAST_RANGE_PRESET, v)

    val statsCustomFrom: Flow<Long> = ds.data.read(K_STATS_CUSTOM_FROM, 0L)
    suspend fun setStatsCustomFrom(v: Long) = write(K_STATS_CUSTOM_FROM, v)

    val statsCustomTo: Flow<Long> = ds.data.read(K_STATS_CUSTOM_TO, 0L)
    suspend fun setStatsCustomTo(v: Long) = write(K_STATS_CUSTOM_TO, v)

    @Suppress("MemberVisibilityCanBePrivate")
    companion object Keys {
        // Sync
        val K_SYNC_ENABLED = booleanPreferencesKey("syncEnabled")
        val K_SYNC_INTERVAL_MIN = intPreferencesKey("syncIntervalMinutes")
        val K_SYNC_WIFI_ONLY = booleanPreferencesKey("syncWifiOnly")
        val K_SYNC_CHARGING_ONLY = booleanPreferencesKey("syncWhenChargingOnly")
        val K_SYNC_ON_APP_OPEN = booleanPreferencesKey("syncOnAppOpen")
        val K_SYNC_ON_REBOOT = booleanPreferencesKey("syncOnReboot")
        val K_LAST_SYNC_CALL_ID = longPreferencesKey("lastSyncCallId")
        val K_LAST_SYNC_AT = longPreferencesKey("lastSyncAt")

        // Auto-save
        val K_AUTO_SAVE_ENABLED = booleanPreferencesKey("autoSaveEnabled")
        val K_AUTO_SAVE_PREFIX = stringPreferencesKey("autoSavePrefix")
        val K_AUTO_SAVE_INCLUDE_SIM_TAG = booleanPreferencesKey("autoSaveIncludeSimTag")
        val K_AUTO_SAVE_SUFFIX = stringPreferencesKey("autoSaveSuffix")
        val K_AUTO_SAVE_GROUP_NAME = stringPreferencesKey("autoSaveContactGroupName")
        val K_AUTO_SAVE_GROUP_ID = longPreferencesKey("autoSaveContactGroupId")
        val K_AUTO_SAVE_PHONE_LABEL = stringPreferencesKey("autoSavePhoneLabel")
        val K_AUTO_SAVE_PHONE_LABEL_CUSTOM = stringPreferencesKey("autoSavePhoneLabelCustom")

        // Region
        val K_DEFAULT_REGION = stringPreferencesKey("defaultRegion")

        // Real-time
        val K_FLOATING_BUBBLE = booleanPreferencesKey("floatingBubbleEnabled")
        val K_POST_CALL_POPUP = booleanPreferencesKey("postCallPopupEnabled")
        val K_POST_CALL_POPUP_TIMEOUT = intPreferencesKey("postCallPopupTimeoutSeconds")
        val K_POST_CALL_POPUP_UNSAVED_ONLY = booleanPreferencesKey("postCallPopupUnsavedOnly")

        // Notifications
        val K_FOLLOW_UP_REMINDERS = booleanPreferencesKey("followUpRemindersEnabled")
        val K_DAILY_SUMMARY = booleanPreferencesKey("dailySummaryEnabled")
        val K_UPDATE_ALERTS = booleanPreferencesKey("updateAlertsEnabled")

        // Lead score
        val K_LEAD_SCORE_ENABLED = booleanPreferencesKey("leadScoreEnabled")
        val K_LEAD_SCORE_WEIGHTS = stringPreferencesKey("leadScoreWeights")

        // Backup
        val K_AUTO_BACKUP_ENABLED = booleanPreferencesKey("autoBackupEnabled")
        val K_AUTO_BACKUP_RETENTION = intPreferencesKey("autoBackupRetention")
        val K_BACKUP_DRIVE_ENABLED = booleanPreferencesKey("backupDriveEnabled")
        val K_DRIVE_AUTO_UPLOAD = booleanPreferencesKey("driveAutoUpload")

        // Display
        val K_DISPLAY_UNSAVED_PINNED = booleanPreferencesKey("displayShowUnsavedPinned")
        val K_DISPLAY_GROUPED_BY_NUMBER = booleanPreferencesKey("displayGroupedByNumber")

        // Privacy
        val K_PRIVACY_BLOCK_HIDDEN = booleanPreferencesKey("privacyBlockHidden")
        val K_PRIVACY_HIDE_BLOCKED = booleanPreferencesKey("privacyHideBlocked")

        // Updates
        val K_UPDATE_CHANNEL = stringPreferencesKey("updateChannel")
        val K_UPDATE_AUTO_CHECK = booleanPreferencesKey("updateAutoCheck")
        val K_LAST_UPDATE_CHECK = longPreferencesKey("lastUpdateCheck")

        // Onboarding / OEM
        val K_ONBOARDING_COMPLETE = booleanPreferencesKey("onboardingComplete")
        val K_OEM_BATTERY_GUIDE_SHOWN = booleanPreferencesKey("oemBatteryGuideShown")

        // Sprint 4
        val K_TAGS_SEEDED = booleanPreferencesKey("tagsSeeded")
        val K_EXACT_ALARM_FALLBACK = booleanPreferencesKey("exactAlarmFallbackActive")
        val K_PINNED_BOOKMARKS = stringPreferencesKey("pinnedBookmarkNumbersJson")

        // Sprint 6
        val K_CALLS_SORT_MODE = stringPreferencesKey("callsSortMode")

        // Sprint 8 (Stats)
        val K_STATS_LAST_RANGE_PRESET = intPreferencesKey("statsLastRangePresetIndex")
        val K_STATS_CUSTOM_FROM = longPreferencesKey("statsCustomFrom")
        val K_STATS_CUSTOM_TO = longPreferencesKey("statsCustomTo")
        val K_ANALYTICS_CONSENT = booleanPreferencesKey("analyticsConsent")
        val K_BIOMETRIC_LOCK = booleanPreferencesKey("biometricLockEnabled")
        val K_MESSAGE_TEMPLATES_JSON = stringPreferencesKey("messageTemplatesJson")
        val K_HOT_LEAD_ALERTS = booleanPreferencesKey("hotLeadAlertsEnabled")
        val K_WEEKLY_DIGEST = booleanPreferencesKey("weeklyDigestEnabled")
        val K_WEEKLY_DIGEST_LAST_FIRED = longPreferencesKey("weeklyDigestLastFiredMs")
        val K_DEMO_SEED_ACTIVE = booleanPreferencesKey("demoSeedActive")
        val K_DEMO_SEED_DISMISSED = booleanPreferencesKey("demoSeedDismissedOnce")
        val K_AI_DIGEST = booleanPreferencesKey("aiDigestEnabled")
        val K_ANTHROPIC_API_KEY = stringPreferencesKey("anthropicApiKey")
        val K_HAS_USED_ACCOUNT = booleanPreferencesKey("hasUsedAccount")
    }

    /** Set true after the first successful sign-in/sign-up. Returning signed-out users skip
     *  the Welcome picker and go straight to Login. */
    val hasUsedAccount: Flow<Boolean> = ds.data.read(K_HAS_USED_ACCOUNT, false)
    suspend fun setHasUsedAccount(v: Boolean) = write(K_HAS_USED_ACCOUNT, v)

    /** User-added message templates as a JSON array of `{id,label,body}`. Built-ins are NOT stored here. */
    val messageTemplatesJson: Flow<String> = ds.data.read(K_MESSAGE_TEMPLATES_JSON, "[]")
    suspend fun setMessageTemplatesJson(v: String) = write(K_MESSAGE_TEMPLATES_JSON, v)

    /** Whether the user has opted in to anonymous PostHog analytics. Default: off. */
    val analyticsConsent: Flow<Boolean> = ds.data.read(K_ANALYTICS_CONSENT, false)
    suspend fun setAnalyticsConsent(v: Boolean) = write(K_ANALYTICS_CONSENT, v)

    /** Whether the app should require biometric/credential auth on cold start + after backgrounding. */
    val biometricLockEnabled: Flow<Boolean> = ds.data.read(K_BIOMETRIC_LOCK, false)
    suspend fun setBiometricLockEnabled(v: Boolean) = write(K_BIOMETRIC_LOCK, v)

    /** Notify the user when a "hot lead" calls (score ≥ 70 or pipeline stage Qualified/Won). */
    val hotLeadAlertsEnabled: Flow<Boolean> = ds.data.read(K_HOT_LEAD_ALERTS, true)
    suspend fun setHotLeadAlertsEnabled(v: Boolean) = write(K_HOT_LEAD_ALERTS, v)

    /** Post a weekly summary every Monday morning. */
    val weeklyDigestEnabled: Flow<Boolean> = ds.data.read(K_WEEKLY_DIGEST, true)
    suspend fun setWeeklyDigestEnabled(v: Boolean) = write(K_WEEKLY_DIGEST, v)

    /** Last successful weekly-digest fire — used to dedupe repeated WorkManager runs in the flex window. */
    val weeklyDigestLastFiredMs: Flow<Long> = ds.data.read(K_WEEKLY_DIGEST_LAST_FIRED, 0L)
    suspend fun setWeeklyDigestLastFiredMs(v: Long) = write(K_WEEKLY_DIGEST_LAST_FIRED, v)

    /** Whether the demo-data seed has been written. Set true after seeding; false after user clears it. */
    val demoSeedActive: Flow<Boolean> = ds.data.read(K_DEMO_SEED_ACTIVE, false)
    suspend fun setDemoSeedActive(v: Boolean) = write(K_DEMO_SEED_ACTIVE, v)
    val demoSeedDismissedOnce: Flow<Boolean> = ds.data.read(K_DEMO_SEED_DISMISSED, false)
    suspend fun setDemoSeedDismissedOnce(v: Boolean) = write(K_DEMO_SEED_DISMISSED, v)

    /** Opt-in to send the weekly digest stats (no notes / phone numbers) to a chosen LLM provider. */
    val aiDigestEnabled: Flow<Boolean> = ds.data.read(K_AI_DIGEST, false)
    suspend fun setAiDigestEnabled(v: Boolean) = write(K_AI_DIGEST, v)

    // Anthropic API key moved to SecretStore (EncryptedSharedPreferences). The plaintext key
    // here is retained for one-time migration only; production reads/writes go through SecretStore.
    @Deprecated("Migrated to SecretStore", ReplaceWith("secretStore.get(SecretStore.K_ANTHROPIC_KEY)"))
    val anthropicApiKeyPlaintextLegacy: Flow<String> = ds.data.read(K_ANTHROPIC_API_KEY, "")
    suspend fun clearLegacyAnthropicKey() = write(K_ANTHROPIC_API_KEY, "")
}
