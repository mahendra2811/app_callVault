# callNest

Local-first Android call-log enrichment app. Sideloaded only — no Play Store,
no Google Play Services, no telemetry.

## Tech stack

- Kotlin 2.0.21, Android Gradle Plugin 8.7.3, Gradle 8.10.2
- compileSdk / targetSdk 35, minSdk 26, JVM target 17
- Jetpack Compose (BOM 2024.12.01) + Material 3
- Compose Navigation 2.8.5
- Hilt 2.53.1, KSP
- Room 2.6.1, DataStore Preferences 1.1.1, WorkManager 2.10.0
- kotlinx-coroutines 1.9.0, serialization 1.7.3, datetime 0.6.1
- libphonenumber-android 8.13.50
- Apache POI ooxml-lite 5.2.5, iText core 8.0.5
- Coil 3.0.4, Vico 2.0.0-beta.4
- Tink 1.15.0, Timber 5.0.1
- Tests: JUnit5 5.11.4, Turbine 1.2.0, MockK 1.13.13

## Build

This repository ships without `gradle/wrapper/gradle-wrapper.jar`. Generate it
once with a system Gradle installation:

```bash
gradle wrapper --gradle-version 8.10.2 --distribution-type bin
```

Then build a debug APK:

```bash
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

For a release build, drop a `keystore.properties` file at the repo root with
`storeFile`, `storePassword`, `keyAlias`, `keyPassword`, then:

```bash
./gradlew assembleRelease
```

Without `keystore.properties`, the release variant builds unsigned.

## Sprint progress

- **Sprint 0 — Foundation**: Gradle, manifest, Hilt app, theme, Neo\* component
  library. Stub services/receivers ship empty so the manifest resolves.
- **Sprint 1 — Data Layer & Sync**: Room v1 schema (13 entities incl. FTS),
  9 DAOs, all repositories, `SettingsDataStore` covering every key from
  spec §4, libphonenumber-backed `PhoneNumberNormalizer`, `CallLogReader`,
  `ContactsReader`, `SimSlotResolver`, `SyncCallLogUseCase` implementing the
  spec §8.1 algorithm (auto-save / rule engine deferred to later sprints),
  `CallSyncWorker` + `SyncScheduler` + reboot rearm via
  `BootCompletedReceiver`. Hilt `Configuration.Provider` wired up.
- **Sprint 2 — Permissions & Onboarding**: `PermissionManager` (Hilt
  singleton with `StateFlow<PermissionState>`), `OemBatteryGuide` (vendor
  detection + intent fallback chain + per-vendor instruction copy), 5-page
  `OnboardingScreen` (Welcome / Features / Permissions / OEM battery / First
  sync) with swipe-disabled `HorizontalPager` and progress dots,
  `OnboardingViewModel` driving the flow, `SyncProgressBus` (singleton)
  publishing `SyncProgress.Started/Progress/Done/Error` from
  `SyncCallLogUseCase`, `PermissionRationaleScreen` + `PermissionDeniedScreen`
  guards, `callNestNavHost` routing onboarding → rationale → calls based on
  DataStore + live permission state. `MainActivity` now hosts the NavHost.
- **Sprint 3 — Calls List, Detail, Filters & Search**: full Calls screen
  (`CallsScreen` + `CallsViewModel`) with sticky date headers, optional
  group-by-number, collapsible "Unsaved inquiries — last 7 days" pinned
  section, pull-to-refresh wired to `SyncScheduler.triggerOnce()`, bulk
  selection mode with action bar (Tag / Bookmark / Save / Export / Delete).
  Filter sheet (`CallsFilterSheet`) covers every §3.4 axis driven by a
  `FilterQueryBuilder` → `SimpleSQLiteQuery` → `CallDao.observeRaw`. Per-
  number detail screen (`CallDetailScreen`) with hero/lead-score badge,
  action bar (Call/Message/WhatsApp/Save/Block — real intents), Stats card,
  Tags stub, Notes journal with minimal-markdown render, Follow-up controls,
  History timeline, share-vCard top action, "Clear all data" destructive
  flow. Search overlay (`SearchScreen` + `SearchViewModel`) with 300ms
  debounced FTS, recent searches via `SearchHistoryDao`. UI utilities
  added in `ui/util/`: `DateFormatter`, `PhoneNumberFormatter`,
  `DurationFormatter`, `MarkdownRenderer`. Navigation extended with
  `CallDetail(normalizedNumber)`, `Search`, `FilterPresets`.
- **Sprint 4 — Tags, Notes, Bookmarks, Follow-Ups** (delivered): tag CRUD,
  multi-select tag picker, default tag seeding, notes journal with markdown,
  bookmarks UI with pinned numbers, follow-up reminders backed by exact alarms
  with a fallback banner when permission is missing.
- **Sprint 5 — Auto-Save & Contact Segregation**: real `ContactGroupManager`
  (Groups CRUD via ContactsContract + AccountManager fallback to phone-only),
  `ContactsWriter.applyBatch` insert (RawContacts + StructuredName + Phone +
  GroupMembership), `AutoSaveContactUseCase` driven by `AutoSaveNameBuilder`
  (`{prefix}( -s1| -s2)? +<E164>{suffix}` per spec §8.4),
  `BulkSaveContactsUseCase` + `BulkSaveProgressBus`,
  `DetectAutoSavedRenameUseCase` (lenient bucketing per §8.5 backed by
  `AutoSavePatternMatcher`), `ConvertToMyContactUseCase` (StructuredName
  rename + auto-flip). `SyncCallLogUseCase` now performs steps 8–9 of §8.1.
  New screens: `MyContactsScreen`, `InquiriesScreen` (long-press bulk mode,
  Convert / Bulk save), `AutoSaveSettingsScreen` with live preview and
  debounced (400ms) DataStore writes, plus `BulkSaveProgressDialog`.
  `UiEventBus` carries permission-aware snackbar messages from background
  pipelines. Navigation extended with `MyContacts`, `Inquiries`, and
  `AutoSaveSettings` reachable from the Calls overflow menu.
- Upcoming: Sprint 6 (auto-tag rules + lead-scoring UI).

### Sprint 6 — Auto-Tag Rules & Lead Scoring

- Room schema bumped to v2; `rule_score_boosts` table tracks per-(call, rule)
  score deltas with cascading delete from `calls`. `MIGRATION_1_2` adds the
  table + index for upgrades from 1.0.x.
- `ApplyAutoTagRulesUseCase` evaluates every active rule against the freshly
  synced batch in `SyncCallLogUseCase`, applies matched actions through
  `RuleActionApplier`, and the in-line lead-score recompute picks up the new
  boosts before sync returns.
- New screens: `AutoTagRulesScreen`, `RuleEditorScreen`,
  `LeadScoringSettingsScreen`. `LeadScoreRecomputeWorker` handles bulk score
  refreshes when weights change. `LeadScoreBadge` lights up call rows in
  three buckets (cold / warm / hot).
- Calls overflow menu gains "Auto-tag rules" and "Lead scoring" entries; new
  navigation routes are `auto_tag_rules`, `auto_tag_rules/edit/{ruleId}` (use
  `-1L` for new), and `settings/lead_scoring`.

### Sprint 7 — Real-Time Features

- `PhoneStateMonitor` is now a real `TelephonyCallback`/`PhoneStateListener`
  bridge exposing a `Flow<CallState>`. Number resolution on API 31+ uses a
  secondary `ACTION_PHONE_STATE_CHANGED` receiver gated by `READ_CALL_LOG`.
- `CallEnrichmentService` is a `LifecycleService` foreground service on the
  `realtime_call` channel; coordinates the floating bubble (during a call)
  and the post-call popup (debounced 2 s after `Idle`).
- `OverlayManager` adds/removes `WindowManager` overlay views safely; emits
  a snackbar via `UiEventBus` when overlay permission is missing. Views are
  programmatic Android views (not Compose) — see `DECISIONS.md`.
- `RealTimeSettingsScreen` exposes the four toggles + timeout slider +
  overlay-permission status row; toggles route through
  `RealTimeServiceController` to start/stop the service. Reachable from the
  Calls overflow menu via the new route `settings/real_time`.
- App startup (`callNestApp.onCreate`) and `BootCompletedReceiver` both
  invoke `RealTimeServiceController.evaluateAndApply()` once onboarding has
  been completed.
- `NoteDao.attachOrphans` re-parents in-call bubble notes
  (`callSystemId IS NULL`) onto the matching `CallEntity` once sync inserts
  it — the binding window is `±60 s` around the call date.

## Sprint 9 — Export & Backup

- Five real exporters under `data/export/`: `CsvExporter` (UTF-8 BOM,
  RFC-4180 escaping), `ExcelExporter` (Apache POI XSSF, multi-sheet),
  `PdfExporter` (iText 8, cover + totals + paginated calls table),
  `JsonExporter` (full DB dump via `kotlinx.serialization`, schema v2),
  `VcardExporter` (vCard 3.0 plain text).
- `BackupManager` (data/backup) wraps `JsonExporter` with `EncryptionHelper`
  (AES-256-GCM, PBKDF2-derived key, 120k iterations) and writes
  `callNest-backup-YYYYMMDD-HHmm.cvb`. Restore is a single Room
  transaction that wipes seven user-data tables before re-inserting.
- `DailyBackupWorker` (`@HiltWorker`) runs daily, reads passphrase from
  `SecurePrefs`, and rotates older `.cvb` files in Downloads beyond the
  retention slider (3..14, default 7).
- Five domain use cases (`ExportToCsv/Excel/Pdf/Json/VcardUseCase`) plus
  `BackupDatabaseUseCase` and `RestoreDatabaseUseCase` replace the Sprint-9
  stubs in `DeferredUseCases.kt`.
- New screens: `ui/screen/export/ExportScreen.kt` (5-step wizard with
  `FormatStep`, `DateRangeStep`, `ScopeStep`, `ColumnsStep`,
  `DestinationStep`) and `ui/screen/backup/BackupScreen.kt` (manual
  backup, restore, passphrase status, auto-backup toggle, retention
  slider). New routes `Destinations.Export` and `Destinations.Backup`
  added to `callNestNavHost`. Calls overflow menu gains "Export" and
  "Backup & restore" entries.
