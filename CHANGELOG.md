# Changelog

All notable changes to CallVault are documented in this file.

## [1.0.0] — 2026-04-30

Initial release — full feature set delivered across 13 sprints (Sprint 0–12).

### Added

**Foundation & UI**
- Single-module Android project, Kotlin 2.0.21, Compose BOM 2024.12.01, Material 3.
- Neumorphic design system: `NeoColors`, dual-shadow `Modifier.neoShadow`, 18 reusable Neo* components (Surface, Card, Button, IconButton, Chip, Toggle, Slider, SearchBar, FAB, TabBar, TopBar, BottomSheet, TextField, ProgressBar, Badge, Avatar, Divider, EmptyState, HelpIcon, LeadScoreBadge).
- `CallVaultTheme` with Inter/system fallback typography, locked light scheme.

**Data layer**
- Room v2 with 14 entities + 2 FTS4 tables (`call_fts`, `note_fts`) and full DAO surface.
- Migration v1 → v2 adding `rule_score_boosts`.
- DataStore Preferences for ~40 typed settings; `EncryptedSharedPreferences` for backup passphrase via `SecurePrefs`.
- Repository layer (Call, Tag, Note, Contact, AutoTagRule, Settings, Update) with mappers.

**Sync & call extraction**
- `CallLogReader` + `PhoneNumberNormalizer` (libphonenumber-android) + `SimSlotResolver`.
- `SyncCallLogUseCase` implementing the full §8.1 pipeline: read since `lastSyncCallId` → normalize → resolve display name → resolve SIM → upsert → recompute ContactMeta → apply auto-tag rules → recompute LeadScore → auto-save unsaved → detect renamed auto-saved (lenient bucketing) → attach orphan bubble notes.
- `CallSyncWorker` + `SyncScheduler` (PeriodicWorkRequest for 15m–24h, AlarmManager exact for 5m, OneTimeWork chained for daily 2 AM).
- `BootCompletedReceiver` re-schedules sync + real-time service after device reboot.

**Onboarding & permissions**
- 5-page onboarding (Welcome / Features / Permissions / OEM battery / First sync).
- `PermissionManager` singleton tracking call log, contacts, phone state, post-notifications, exact alarms, overlay.
- OEM autostart deep-link registry for Xiaomi, Oppo, Vivo, Realme, Samsung, OnePlus, Honor, Huawei with fallback to `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

**Calls & detail**
- Calls list with pinned "Unsaved inquiries — last 7 days" section, sticky date headers, swipe gestures (bookmark/archive), bulk-select mode + BulkActionBar.
- `CallsFilterSheet` with all §3.4 filter axes; SimpleSQLiteQuery filter builder; saved presets.
- `CallDetailScreen`: Hero, ActionBar (call/message/whatsapp/save/block via Intents), Stats, Tags, Notes journal with markdown, Follow-up section, Call history timeline.
- `SearchScreen` with FTS4 over rawNumber/cachedName/geocodedLocation + notes; recent searches.

**Tags, notes, bookmarks, follow-ups**
- 9 default system tags seeded via Room callback worker; tag manager with rename/recolor/merge/delete.
- TagPickerSheet for detail + bulk-select.
- Notes journal with edit history (last 5 versions), markdown templates, full editor dialog.
- Bookmarks screen with reason prompt and pinned-top-5 (DataStore-backed reorder).
- Follow-ups: AlarmManager exact (`SCHEDULE_EXACT_ALARM` permission-aware fallback to inexact), `FollowUpAlarmReceiver` notifications with Call back / Snooze 1h / Snooze 1d / Mark done actions; tabs Today / Overdue / Upcoming / Completed.

**Auto-save & contact segregation**
- `ContactGroupManager` ensures "CallVault Inquiries" group; `ContactsWriter` uses `ContentResolver.applyBatch` for atomic insert.
- `AutoSaveNameBuilder` produces the locked `{prefix}{simTag} {phone}{suffix}` format.
- `AutoSaveSettingsScreen` with live preview, debounced 400ms persist.
- Two-bucket UX: `MyContactsScreen` and `InquiriesScreen` with Convert / Bulk save.
- `DetectAutoSavedRenameUseCase` runs every sync (lenient bucketing per §8.5).

**Auto-tag rule engine & lead scoring**
- Sealed `RuleCondition` (12 variants) and `RuleAction` (4 variants) with kotlinx.serialization (class-discriminator "type").
- `RuleConditionEvaluator` + `RuleActionApplier`; `ApplyAutoTagRulesUseCase` wired into sync.
- `RuleScoreBoost` table; rules' tag applications cascade-cleaned on rule delete.
- `RuleEditorScreen` stepper with live "applies to N calls" preview (latest-200 evaluation).
- Lead score formula per §8.2 with configurable weights via `LeadScoringSettingsScreen`; `LeadScoreRecomputeWorker` reflows scores after weight changes.
- `LeadScoreBadge` (Cold/Warm/Hot) shown in CallRow + HeroCard.

**Real-time features**
- `CallEnrichmentService` (`foregroundServiceType="specialUse"`, `RealTimeCallEnrichment` subtype).
- `PhoneStateMonitor` using `TelephonyCallback` (API 31+) / `PhoneStateListener` (26–30) plus a secondary `PHONE_STATE` receiver for number resolution.
- `OverlayManager` + `FloatingBubbleView` (drag with edge snap, mid-call note + tag chip) + `PostCallPopupView` (configurable timeout, top-3 tag chips, Save contact, More options deep-link).
- Bubble notes attach to call entity post-sync via orphan-attachment window.
- `RealTimeServiceController` reconciles service state with settings + permissions.

**Stats dashboard**
- Date-range picker (Today / 7d / 30d / This month / Last month / Last 90 / Custom) with persistence.
- `ComputeStatsUseCase` runs aggregates concurrently via `coroutineScope { async … }`.
- Overview cards: total calls, talk time, avg duration, missed rate, lead distribution mini-bar.
- Charts (4 of 10 from spec): Daily volume + 7-day moving average (Compose Canvas), Type donut (Canvas), Hourly heatmap (Canvas grid), Top numbers leaderboard.
- `GenerateInsightsUseCase` with 5 rule-based insights, severity-coloured cards.

**Export & backup**
- 5 exporters: Excel (Apache POI multi-sheet), CSV (UTF-8 BOM), PDF (iText cover + totals + paginated calls table), JSON (full DB dump, optional encryption), vCard 3.0.
- 5-step export wizard: Format → Date range → Scope → Columns → Destination.
- `BackupManager` with PBKDF2-HMAC-SHA256 (120k iter) + AES-256-GCM, single `.cvb` blob with `CVB1` magic header.
- `DailyBackupWorker` 2 AM daily, FIFO retention.
- Restore wraps replace in a single Room transaction.

**Self-update**
- `UpdateChecker` polls `BuildConfig.UPDATE_MANIFEST_STABLE_URL` / `UPDATE_MANIFEST_BETA_URL`.
- `UpdateDownloader` uses `DownloadManager`, verifies SHA-256, deletes corrupt files.
- `UpdateInstaller` with API 26+ `canRequestPackageInstalls()` gate and unknown-sources fallback intent.
- `UpdateCheckWorker` weekly periodic; `UpdateNotifier` posts on `app_updates` channel with deep-link.
- `UpdateAvailableScreen` (Available / Downloading / ReadyToInstall / Installing / Error states) + `UpdateBanner` on Calls + `UpdateSettingsScreen` (channel, auto-check, last-checked, clear skipped).

**Settings, docs & polish**
- Master `SettingsScreen` with progressive disclosure across all §3.19 sections; sub-screen links to dedicated settings (Auto-Save, Real-Time, Lead Scoring, Auto-Tag, Backup, Updates).
- Daily summary worker on `daily_summary` notification channel.
- `ResetAllDataUseCase` with double-confirm dialog.
- 15 in-app docs articles bundled in `assets/docs/`, rendered via `MarkdownRenderer`; "Was this helpful?" feedback persisted.
- `NeoHelpIcon` component (caller-side wiring per screen is a follow-up).
- Home + More + 5-tab MainScaffold scaffolded; full nested-graph integration is a follow-up.

**Testing**
- Unit tests for `AutoSaveNameBuilder`, `AutoSavePatternMatcher`, `GenerateInsightsUseCase`.
- DAO instrumentation tests scaffolded (extend in follow-up).

### Known limitations

- 6 of 10 stats charts from §3.16 deferred to v1.1 (SimUtilizationBar, TagDistribution, SavedUnsavedTrend, ConversionFunnel, GeoBars, DayOfWeekBars).
- PDF export does not embed chart images yet (Compose `captureToBitmap` deferred).
- 5-tab `NeoTabBar` MainScaffold is wired but the nested-graph integration into `CallVaultNavHost` is a small follow-up; current entry points are via `CallsScreen` overflow + dedicated routes.
- `NeoHelpIcon` exists but is not yet placed in every screen's app bar (component is drop-in).
- Voice-to-text on notes is intentionally deferred to v2 per spec §3.9.
- `ResetAllDataUseCase` wipes notes/search/skipped only; calls/contacts/tags retain (re-sync from OS log resets calls).
- Build verification (`./gradlew lint assembleDebug`) is the operator's responsibility — agents in this run are scoped to file generation only.

### Tech debt
- `LazyColumnItemsScopeShim` dead helper in `StatsScreen.kt` (harmless).
- A handful of `@Preview` blocks were dropped for time; non-functional gap.
- `UpdateAvailableScreen` / `UpdateSettingsScreen` use Material 3 primitives directly in some places (Neo* swap is a cosmetic follow-up).
