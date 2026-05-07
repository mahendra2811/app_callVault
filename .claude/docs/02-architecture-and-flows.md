# Architecture And Flows

## Layering

callNest uses one Android module: `app`.

Layer rules:

- `ui/` contains Compose screens, ViewModels, navigation, reusable Neo components, theme, and UI formatters.
- `domain/` contains pure Kotlin domain models, repository interfaces, and use cases.
- `data/` contains Room, DataStore, system APIs, repository implementations, workers, services, exporters, backup, and update plumbing.
- `util/` contains shared Android helpers such as permissions, docs loading, battery guide, pattern matching, and real-time service control.
- `di/` contains Hilt modules.

Important rule from `CLAUDE.md`: `ui/` should not import `data/` directly. Prefer `domain` interfaces/use cases. Some bootstrap entry points expose Android-bound singletons to navigation; do not spread that pattern casually.

## Startup Flow

1. `MainActivity.onCreate`
2. `enableEdgeToEdge`
3. `callNestTheme`
4. `callNestNavHost(initialDeepLink)`
5. `callNestNavHost` reads:
   - `SettingsDataStore.onboardingComplete`
   - `PermissionManager.state`
6. Start route:
   - onboarding incomplete -> `onboarding`
   - critical permissions missing -> `permission_rationale`
   - otherwise -> `calls`

`callNestApp.onCreate` plants Timber in debug builds, creates notification channels, configures Hilt WorkManager, and starts/schedules real-time, update, and daily-summary work after onboarding is complete.

## Navigation Flow

Routes are centralized in `ui/navigation/Destinations.kt`.

Main implemented routes:

- `onboarding`
- `permission_rationale`
- `permission_denied`
- `calls`
- `call_detail/{normalizedNumber}`
- `search`
- `my_contacts`
- `inquiries`
- `settings/auto_save`
- `auto_tag_rules`
- `auto_tag_rules/edit/{ruleId}`
- `settings/lead_scoring`
- `settings/real_time`
- `export`
- `backup`
- `update`
- `settings/updates`
- `settings`
- `docs`
- `docs/{articleId}`
- `home`
- `more`

`filter_presets` currently renders a placeholder screen.

## Data Storage

Room database: `callNestDatabase`, name `callNest.db`, schema version `2`.

Entities:

- `CallEntity`
- `TagEntity`
- `CallTagCrossRef`
- `ContactMetaEntity`
- `NoteEntity`
- `NoteHistoryEntity`
- `FilterPresetEntity`
- `AutoTagRuleEntity`
- `RuleScoreBoostEntity`
- `SearchHistoryEntity`
- `DocFeedbackEntity`
- `SkippedUpdateEntity`
- `CallFts`
- `NoteFts`

DAOs:

- `CallDao`
- `TagDao`
- `NoteDao`
- `ContactMetaDao`
- `FilterPresetDao`
- `AutoTagRuleDao`
- `SearchHistoryDao`
- `DocFeedbackDao`
- `SkippedUpdateDao`
- `RuleScoreBoostDao`

Preferences:

- `SettingsDataStore` stores sync, auto-save, region, real-time, notifications, lead scoring, backup, display, privacy, updates, onboarding, tags seeded, exact-alarm fallback, pinned bookmarks, call sort mode, and stats range keys.
- `SecurePrefs` stores backup passphrase using EncryptedSharedPreferences.

## Sync Flow

Main entry points:

- `CallSyncWorker`
- `SyncScheduler.triggerOnce`
- scheduled WorkManager/AlarmManager paths
- `CallEnrichmentService` after real-time calls

Main use case: `SyncCallLogUseCase`.

Flow:

1. Publish `SyncProgress.Started`.
2. Read `lastSyncCallId` and default region from DataStore.
3. `CallLogReader.readSince(lastId)`.
4. For each row:
   - normalize via `PhoneNumberNormalizer`
   - resolve display name via `ContactsReader`
   - resolve SIM/carrier via `SimSlotResolver`
   - map to domain `Call`
   - upsert through `CallRepository`
   - auto-clear active follow-up when an outgoing call satisfies it
5. Attach orphan bubble notes to matching calls with `NoteDao.attachOrphans`.
6. Recompute `ContactMeta` aggregates for touched numbers.
7. Apply auto-tag rules through `ApplyAutoTagRulesUseCase`.
8. Recompute lead score through `ComputeLeadScoreUseCase`.
9. Auto-save unsaved numbers through `AutoSaveContactUseCase`.
10. Detect renamed auto-saved contacts through `DetectAutoSavedRenameUseCase`.
11. Persist `lastSyncCallId` and `lastSyncAt`.
12. Emit sync completion or error through `SyncProgressBus`.

## Auto-Save Flow

Core files:

- `AutoSaveContactUseCase`
- `AutoSaveNameBuilder`
- `BulkSaveContactsUseCase`
- `DetectAutoSavedRenameUseCase`
- `ContactGroupManager`
- `ContactsWriter`
- `ContactsReader`
- `AutoSavePatternMatcher`
- `AutoSaveSettingsScreen/ViewModel`
- `MyContactsScreen/ViewModel`
- `InquiriesScreen/ViewModel`

Behavior:

- Unsaved numbers are written to ContactsContract when auto-save is enabled.
- Names use `{prefix}{simTag} {phone}{suffix}`, for example `callNest-s1 +91...`.
- Contacts go into the configured group, default `callNest Inquiries`.
- Conversion to "My Contact" renames the system contact and flips metadata.
- Rename detection compares live Contacts display names against the current auto-save pattern.

## Tags, Notes, Bookmarks, Follow-Ups

Tags:

- `TagRepository`, `TagDao`, `TagEntity`
- `TagsManagerScreen/ViewModel`
- `TagPickerSheet`, `TagEditorDialog`
- `DefaultTagsSeeder`, `SeedDefaultTagsWorker`

Notes:

- `NoteRepository`, `NoteDao`, `NoteEntity`, `NoteHistoryEntity`, `NoteFts`
- detail screen sections: `NotesJournal`, `NoteEditorDialog`
- markdown rendering: `MarkdownRenderer`

Bookmarks:

- `CallEntity.isBookmarked`
- `BookmarksScreen/ViewModel`
- `BookmarkReasonDialog`
- pinned bookmark numbers in DataStore JSON

Follow-ups:

- `ScheduleFollowUpUseCase`
- `ExactAlarmScheduler`
- `FollowUpAlarmReceiver`
- `FollowUpsScreen/ViewModel`
- `FollowUpSection`, `FollowUpDateTimeDialog`
- exact-alarm fallback state in DataStore

## Auto-Tag Rules And Lead Scoring

Auto-tag files:

- `AutoTagRule`
- `RuleCondition`
- `RuleAction`
- `AutoTagRuleRepository`
- `AutoTagRuleRepositoryImpl`
- `RuleConditionEvaluator`
- `RuleActionApplier`
- `ApplyAutoTagRulesUseCase`
- `AutoTagRulesScreen/ViewModel`
- `RuleEditorScreen/ViewModel`
- `ActionRow`, `ConditionRow`, `LivePreviewBox`

Lead-score files:

- `LeadScore`, `LeadScoreWeights`
- `ComputeLeadScoreUseCase`
- `LeadScoreRecomputeWorker`
- `LeadScoringSettingsScreen/ViewModel`
- `LeadScoreBadge`
- `RuleScoreBoostEntity/Dao`

Formula summary:

- frequency contribution
- duration contribution
- recency contribution
- follow-up bonus
- customer tag bonus
- saved-contact bonus
- rule-applied score boosts
- manual override short-circuits computed score

Buckets: Cold `<30`, Warm `30..70`, Hot `>70`.

## Real-Time Flow

Core files:

- `PhoneStateMonitor`
- `CallEnrichmentService`
- `RealTimeServiceController`
- `OverlayManager`
- `FloatingBubbleView`
- `PostCallPopupView`
- `CallContextResolver`
- `UiEventBus`
- `RealTimeSettingsScreen/ViewModel`

Flow:

1. `PhoneStateMonitor` watches call state using `TelephonyCallback` on API 31+ and `PhoneStateListener` on older supported versions.
2. `CallEnrichmentService` runs as a foreground service when real-time toggles and overlay permission allow it.
3. During active calls, `OverlayManager` shows the floating bubble.
4. After calls end, a debounced idle event shows the post-call popup.
5. Mid-call notes may be saved before the call row exists, so they are stored as orphan notes and attached during sync.

Overlay UI is programmatic Android views, not Compose, by an explicit decision in `DECISIONS.md`.

## Export Flow

Core files:

- `ExportScreen/ViewModel`
- `FormatStep`
- `DateRangeStep`
- `ScopeStep`
- `ColumnsStep`
- `DestinationStep`
- `ExportUseCases`
- `ExportConfig`
- `ExportShared`
- `CsvExporter`
- `ExcelExporter`
- `PdfExporter`
- `JsonExporter`
- `VcardExporter`

Formats:

- CSV with UTF-8 BOM
- Excel via Apache POI
- PDF via iText
- JSON full dump via kotlinx.serialization
- vCard 3.0

Exports write to public Downloads through MediaStore on API 29+.

## Backup Flow

Core files:

- `BackupScreen/ViewModel`
- `BackupUseCases`
- `BackupManager`
- `EncryptionHelper`
- `JsonExporter`
- `DailyBackupWorker`
- `SecurePrefs`

Backup file:

- extension `.cvb`
- magic header `CVB1`
- PBKDF2-HMAC-SHA256 with 120k iterations
- AES-256-GCM
- restore is destructive but wrapped in one Room transaction

## Update Flow

Core files:

- `UpdateChecker`
- `UpdateDownloader`
- `UpdateInstaller`
- `UpdateNotifier`
- `UpdateManifest`
- `UpdateRepository/Impl`
- `UpdateUseCases`
- `UpdateCheckWorker`
- `UpdateAvailableScreen/ViewModel`
- `UpdateSettingsScreen/ViewModel`
- `UpdateBanner`

Flow:

1. Weekly or manual check reads stable/beta manifest URL from BuildConfig.
2. Manifest is parsed and compared against current `versionCode`.
3. User can skip or download.
4. APK downloads with DownloadManager.
5. SHA-256 is verified.
6. FileProvider hands APK to system installer.
7. Unknown-source permission is handled on API 26+.

## In-App Docs Flow

Core files:

- `AssetDocsLoader`
- `DocsListScreen`
- `DocsArticleScreen`
- `DocsViewModel`
- `MarkdownRenderer`
- `DocFeedbackEntity/Dao`
- 15 markdown articles in `app/src/main/assets/docs`
