# CallVault — Architecture Overview

## Goals

CallVault is a single-user, offline-first Android app for capturing every inbound/outbound call into a searchable inquiry CRM. It is sideloaded (no Play Store), targets Indian small-business operators handling 20–100 daily inquiry calls, and never leaves the device except to fetch self-update manifests.

## Layering

```
┌──────────────────────────────────────────────────────────┐
│ ui/             Compose screens, ViewModels, Neo* lib     │
│   theme  components/neo  navigation  screen/{...}  util   │
└────────────────────────┬─────────────────────────────────┘
                         │  StateFlow / SharedFlow
┌────────────────────────▼─────────────────────────────────┐
│ domain/         Pure Kotlin: models, use cases, repo IFs  │
│   model  usecase  repository                              │
└────────────────────────┬─────────────────────────────────┘
                         │  interface boundary
┌────────────────────────▼─────────────────────────────────┐
│ data/           Android-bound implementations             │
│   local (Room + FTS)   prefs (DataStore + Encrypted)      │
│   system (CallLog, Contacts, Telephony, libphonenumber)   │
│   repository  work (WorkManager)  service (overlay+alarm) │
│   export  backup  update  insight                         │
└──────────────────────────────────────────────────────────┘
```

DI: Hilt across all three layers. Modules split into `AppModule`, `DatabaseModule`, `RepositoryModule`, `WorkerModule`. ViewModels are `@HiltViewModel`. Workers are `@HiltWorker` with `Configuration.Provider` wired in `CallVaultApp`.

## Data store

- **Room v2** — 14 entities + 2 FTS4 tables (`call_fts`, `note_fts`). Cross-ref tables for `call_tag_cross_ref` and `rule_score_boosts`. Type converters minimal (primitives only). FTS rebuilt automatically by Room triggers; queries use prefix-match (`token*`) per §8.6.
- **DataStore Preferences** — ~40 typed settings (sync, auto-save, real-time, lead scoring, backup, display, privacy, updates, onboarding flags).
- **EncryptedSharedPreferences** — `backupPassphrase` only.
- **Internal files** — backup blobs (`callvault-backup-YYYYMMDD-HHmm.cvb`) and downloaded APKs (`getExternalFilesDir(DOWNLOADS)`).

## Sync pipeline (§8.1)

```
WorkManager / AlarmManager
        ▼
CallSyncWorker.doWork()
        ▼
SyncCallLogUseCase
   1. WakeLock partial (30s)
   2. Read lastSyncCallId
   3. Query CallLog WHERE _ID > lastSyncCallId
   4. For each row → normalize → resolve name → resolve SIM → upsert
   5. Recompute ContactMeta aggregates
   6. ApplyAutoTagRulesUseCase  ──► RuleConditionEvaluator + RuleActionApplier
   7. ComputeLeadScoreUseCase   (sums rule score boosts, honours manual override)
   8. AutoSaveContactUseCase   (per row, gated by settings)
   9. DetectAutoSavedRenameUseCase (lenient bucketing)
  10. Save lastSyncCallId, lastSyncAt
  11. Emit SyncProgressBus events
  12. Attach orphan bubble notes to inserted calls
```

The same use case is invoked inline by `CallEnrichmentService` after a real-time call ends — same pipeline, smaller working set.

## Real-time path

```
TelephonyCallback (API 31+) / PhoneStateListener (26–30)
        ▼
PhoneStateMonitor.flow : Flow<CallState>
        ▼
CallEnrichmentService (foreground, specialUse)
   • Offhook → OverlayManager.showBubble()
   • Idle (debounced 2s) → OverlayManager.showPostCallPopup()
   • Bubble notes → NoteRepository.add(callSystemId=null, normalizedNumber, ...)
   • Popup tag/note → CallContextResolver.latestCallForNumber → attach
```

The bubble's mid-call notes are reconciled to the actual `CallEntity` later by the orphan-attachment pass at step 12 of the sync pipeline.

## Auto-tag rule engine (§8.3)

- Rules are persisted as JSON in `auto_tag_rules.conditionsJson` + `actionsJson`.
- `RuleConditionEvaluator.evaluate(condition, ctx)` is a pure function over a sealed `RuleCondition` hierarchy (12 variants) — easily unit-tested.
- `RuleActionApplier.apply(action, call, ruleId)` mutates state through DAOs; tag applications carry `appliedBy = "rule:${ruleId}"` so a rule delete can cascade-clean.
- Rules iterate top-to-bottom; conditions are AND-joined within a rule.
- Rule edits can flip a `RuleScoreBoost` per (call, rule); `ComputeLeadScoreUseCase` sums those per number.

## Lead score (§8.2)

```
score = w_freq    · normalize(callCount, 0..50)            (default 25%)
      + w_dur     · normalize(totalDurationSec, 0..7200)   (default 20%)
      + w_recency · 100·exp(-daysSince/14)                 (default 25%)
      + (10 if hasFollowUp)
      + (20 if Customer tag applied)
      + (15 if saved in real contacts)
      + Σ rule-applied score boosts
                     clamped to 0..100
```

Manual override: `leadScoreManualOverride` short-circuits the calculation. Buckets: Cold <30, Warm 30–70, Hot >70.

## Update channel

```
UpdateCheckWorker (weekly, NetworkType.CONNECTED)
        ▼
UpdateChecker.checkNow()  ──HTTP──►  versions-{channel}.json
        ▼
UpdateRepository.state : MutableStateFlow<UpdateState>
        ▼
   • UpdateBanner on CallsScreen
   • UpdateNotifier (channel: app_updates)
        ▼
DownloadAndInstallUpdateUseCase
        ▼
UpdateDownloader (DownloadManager) → SHA-256 verify
        ▼
UpdateInstaller (FileProvider + ACTION_VIEW) ─ API 26+ canRequestPackageInstalls gate
```

`SkippedUpdateEntity` records per-versionCode user dismissals; `clearSkipped()` resets.

## Privacy posture

- No analytics SDK. No Firebase. No Play Services.
- Outbound network limited to update manifest URLs declared in `BuildConfig` (allow-listed by code).
- Backup blobs are AES-256-GCM with PBKDF2-HMAC-SHA256 (120,000 iterations, 16-byte salt, 12-byte IV, 128-bit tag) over user passphrase.
- Restore is destructive and wraps the wipe + insert in a single Room transaction.

## Permissions surface

Manifest declares: `READ_CALL_LOG`, `READ_CONTACTS`, `WRITE_CONTACTS`, `READ_PHONE_STATE`, `POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `REQUEST_INSTALL_PACKAGES`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `VIBRATE`, `INTERNET`, `ACCESS_NETWORK_STATE`. Runtime requests routed through `PermissionManager` with rationale + permanently-denied paths to `ACTION_APPLICATION_DETAILS_SETTINGS`.

## Build configuration

- Kotlin 2.0.21, AGP 8.7.3, compileSdk 35, minSdk 26, JVM target 17.
- Compose compiler via `org.jetbrains.kotlin.plugin.compose`.
- KSP for Room (schemas exported to `app/schemas`) and Hilt.
- `packaging.resources.excludes` configured for POI duplicates and netty/META-INF noise.
- Debug-only `Timber.plant`. Release variant ready for keystore via `keystore.properties` (signing config skipped if absent).
