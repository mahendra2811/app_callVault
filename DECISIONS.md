# Sprint 0 — Decisions & Deviations

## Build verification

- **Skipped `./gradlew lint assembleDebug`** — the caller will run the build
  locally. A previous automated run timed out trying to download Gradle and
  Android dependencies, so all build / wrapper / network operations were
  intentionally avoided in this pass.
- `gradle/wrapper/gradle-wrapper.jar` is **not** committed. Run
  `gradle wrapper --gradle-version 8.10.2` once locally (with a system Gradle
  installed) to materialize it, then `./gradlew assembleDebug`.

## Dependencies

- `libphonenumber-android` resolves to `io.michaelrocks:libphonenumber-android`
  (the standard Android port of Google's libphonenumber). Spec lists the bare
  name only.
- `iText core 8.0.5` is included as `com.itextpdf:itext-core`. The fallback
  `pdfbox-android` mentioned in spec §2 is **not** wired up — Sprint 9 (Export)
  decides whether to swap based on rendering needs.
- `coil` is on the v3 line (`io.coil-kt.coil3:coil-compose` 3.0.4 per spec).
- `vico` is `com.patrykandpatrick.vico:compose-m3` plus `compose` and `core`
  modules at `2.0.0-beta.4`.
- `kotlinx-serialization` plugin is declared in the version catalog and applied
  at the module level.
- `desugar_jdk_libs` 2.1.3 is enabled because Compose + Room on minSdk 26
  benefits from `java.time` desugaring; spec doesn't forbid it.

## Architecture / scope

- `NeoShadows` (legacy spec name) and `NeoColors` are both provided. They
  expose the same canonical tokens; new code should prefer `NeoColors`.
- `NeoBottomSheet` is built on Material 3 `ModalBottomSheet`, restyled per
  spec §3.23 ("Material 3 sheet but restyled"). Its preview only renders the
  inner card because `ModalBottomSheet` cannot meaningfully appear in
  `@Preview`.
- The two stub `BroadcastReceiver`s and `CallEnrichmentService` ship empty so
  the manifest resolves; their behavior is added in later sprints.
- `keystore.properties` is read only if present; release variant signing
  silently no-ops otherwise so debug builds keep working out of the box.
- `data_extraction_rules.xml` excludes every domain (cloud + device transfer)
  so user data never leaves the device, matching spec §13 "no telemetry".

## Sprint 1 — Data Layer & Sync

### Dependencies added

- `androidx.security:security-crypto:1.1.0-alpha06` — used by
  `data/prefs/SecurePrefs.kt` to wrap the single sensitive setting
  (`backupPassphrase`) in EncryptedSharedPreferences. The `1.1.0-alpha06`
  line is the latest published artifact at the time of writing; spec §13
  forbids Play-Services-bound auth so this is the only viable option.

### Schema export

- `room.schemaLocation` is wired through KSP arg pointing at
  `app/schemas/`. The directory is created on first build; commit the
  resulting JSON files alongside any future migration PRs.

### Sync algorithm (spec §8.1)

`SyncCallLogUseCase` implements steps 1–5 + 10–11 of the §8.1 pipeline.
Steps explicitly NOT implemented yet (per Sprint 1 scope):

- Step 6 — auto-tag rule application → Sprint 6.
- Step 7 — full lead-score recompute integration → Sprint 6 (the
  `ComputeLeadScoreUseCase` formula itself is implemented now).
- Step 8 — auto-save unsaved contacts → Sprint 5.
- Step 9 — detect renamed auto-saved contacts → Sprint 5.

The deferred use cases throw `NotImplementedError("...— Sprint N")` so any
accidental invocation surfaces immediately during tests.

### `syncIntervalMinutes = 5`

The 5-minute interval is below WorkManager's periodic minimum (15 min), so
`SyncScheduler` falls back to `AlarmManager.setExactAndAllowWhileIdle`.
This burns more battery and requires `SCHEDULE_EXACT_ALARM`. Sprint 11 docs
will warn the user; for Sprint 1 we just no-op when the OS denies exact
alarm scheduling.

### `SkippedUpdateEntity` lives in v1

Although the self-update flow lands in Sprint 10, the entity is included
in v1 so we don't need a migration when Sprint 10 starts populating it.

### `UpdateRepositoryImpl` is a skeleton

Implements the [`UpdateRepository`] interface with `UpdateState.Idle` only;
Sprint 10 fills in the HTTP / DownloadManager / PackageInstaller flow.

## Not in scope for Sprint 0 (per spec §11)

- Room database, DAOs, repositories — Sprint 1.
- Permission manager, onboarding flow — Sprint 2.
- Calls list / detail / search screens — Sprints 3+.
- Real navigation graph (`CallVaultNavHost`, `Destinations`) — added when the
  first real screens land in Sprint 2/3. `MainActivity` currently shows
  `PlaceholderScreen` directly.

## Sprint 2 — Permissions & Onboarding

### `SyncProgressBus` — sync progress wiring

The existing `SyncCallLogUseCase` only emitted a `SharedFlow<Unit>` post-sync
sentinel — no progress, no totals. Two options were considered:

1. Bolt a `MutableSharedFlow<SyncProgress>` onto the use case directly.
2. Introduce a Hilt-singleton bus the use case writes to.

We picked **option 2** (`domain/usecase/SyncProgressBus.kt`). Rationale:

- Keeps the use case focused on the §8.1 algorithm.
- Survives use-case re-creation (it's `@Singleton`).
- Lets future consumers (settings sync chip, calls list pull-to-refresh)
  subscribe without coupling to the use case.
- `replay = 1` so a consumer that arrives mid-sync immediately sees the
  latest event.

`SyncCallLogUseCase` was edited surgically: one new constructor parameter,
five `progressBus.publish(...)` lines bracketing the existing flow. The
original `_events: SharedFlow<Unit>` is left intact for backward-compat.

### Onboarding pager

`HorizontalPager.userScrollEnabled = false` per spec — the user must press
Continue to advance. Page index is owned by `OnboardingViewModel`; the pager
state is animated via `LaunchedEffect(currentPage)`. Progress dots above the
pager track the current page in real time.

### Permission Manager

`PermissionManager` lives in `util/` (not `data/`) because it's a thin Hilt
wrapper over `ContextCompat.checkSelfPermission` + `Settings.canDrawOverlays`
+ `AlarmManager.canScheduleExactAlarms`. The "permanently denied" axis only
materialises after a real permission request — so unless an `Activity` is
passed to `recheckAll`, every non-granted permission shows up as `Denied`
(safe default).

The Compose helper `rememberPermissionLauncher` wraps
`RequestMultiplePermissions` and re-invokes `recheckAll(activity)` on
result so all consumers see the updated `StateFlow` immediately.

### OEM battery handling

`OemBatteryGuide` ships hard-coded component names per spec §3.22. When all
candidates fail, it falls back to
`Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` and finally to
`ACTION_BATTERY_SAVER_SETTINGS`. Each vendor has 3–4 step-by-step bullets
written for end-users (no placeholders).

### NavHost routing

`CallVaultNavHost` reads `onboardingComplete` from DataStore and the live
permission state from `PermissionManager` to choose the start destination.
After onboarding finishes, it explicitly checks `isCriticalGranted()` and
either jumps to Calls or to PermissionRationale. The launcher in the
rationale screen routes to PermissionDenied when *any* critical permission
returns as `PermanentlyDenied`.

### Hilt entry points in Compose

Two `@EntryPoint` interfaces (`OnboardingEntryPoint`, `NavHostEntryPoint`)
expose the singletons the navigation layer needs before any view-model has
been resolved. This avoids forcing `MainActivity` to thread dependencies
manually through Compose params.

## Sprint 3 — Calls List, Detail, Filters & Search

### Filter SQL composition — `SimpleSQLiteQuery`

The §3.4 filter sheet exposes 12+ axes (date range, call types, SIM slots,
include/exclude tags, only-bookmarked, only-with-followup, saved/unsaved,
duration min/max, lead-score min, country ISOs, free-text, sort). A static
Room `@Query` would have to enumerate every combination, so we ship a small
`FilterQueryBuilder` (in `data/repository/`) that composes a parameterised
`SELECT` and feeds it through `CallDao.observeRaw(SupportSQLiteQuery)`.

`@RawQuery(observedEntities = [CallEntity::class])` keeps Room's invalidation
tracker hooked up — the UI still gets live updates when calls change.

### FTS join strategy (§8.6)

`CallRepository.searchFts(query)` exposes a reactive prefix-match search via
`call_fts` only — fast and observable through Room's invalidation. The
one-shot `search(query)` does the spec's union with `note_fts`:

1. Build a token-prefix MATCH string (`token1* token2* ...`).
2. `SELECT systemId FROM calls JOIN call_fts ... UNION SELECT callSystemId
   FROM notes JOIN note_fts ...`
3. Re-fetch matching rows in date-desc order via `getByIdsOrdered`.

Notes-only matches surface only via the one-shot path because Room can't
reactively observe a query that joins two FTS tables without listing both
content entities.

### Swipe gestures

Sprint 3 ships swipe-right (bookmark) and swipe-left (archive) as VM
commands; the row composable will be wrapped in Material 3
`SwipeToDismissBox` in Sprint 3.1 once the row visuals settle. The current
`CallRow` exposes `onToggleBookmark` directly so the trailing star always
works regardless of swipe support.

### Tag picker — Sprint-4 stub

Tag application UI lives in Sprint 4; the detail screen renders a
`NeoBottomSheet` containing the literal copy "Tags coming in Sprint 4" so
nothing throws.

### Filter presets persistence

The `Save preset` button in the filter sheet is wired to a no-op in
Sprint 3 (the `FilterPresetEntity` row is created in Sprint 3.1 once we
finalize the JSON shape for `FilterState`).

### Detail screen "Save to contacts"

Fires `ContactsContract.Intents.Insert.ACTION` so the user picks the
contacts app. The full one-tap auto-save lands in Sprint 5.

### Markdown renderer scope

Notes use a hand-rolled minimal markdown — bold / italic / bullet / link.
A full library (commonmark, etc.) was avoided so notes remain lossless and
predictable; anything we don't recognise renders as literal text.

## Sprint 5

### Account resolution for ContactsContract writes

Spec §3.11 leaves the storage account ambiguous. We resolve in this order:
1. First Google sync account (`com.google`) on the device — the most likely
   candidate to round-trip to the user's primary contact list.
2. Otherwise, the first account `AccountManager.getAccounts()` returns.
3. Otherwise (or if `GET_ACCOUNTS` is denied / no accounts installed), we
   write `ACCOUNT_TYPE`/`ACCOUNT_NAME = null` so the row lands in the
   device-local "Phone" storage. Phone-only contacts behave identically for
   our use-case (display + dial) and survive without any sync agent.

This trades perfect cross-device sync for resilience on bare phones; users
who later add a Google account will see the auto-saved rows stay put rather
than silently re-targeting.

### Lenient bucketing (§8.5)

`DetectAutoSavedRenameUseCase` re-reads the live system display name and
compares it against a regex compiled from the *current* settings — not the
snapshot stored in `autoSavedFormat`. This means changing the prefix in
settings will instantly reclassify every previously auto-saved row whose
name doesn't fit the new pattern, which matches the spec's "bucketing
detection" intent. The historical `autoSavedFormat` snapshot is still
persisted for forensic / debugging purposes.

### Convert UX

"Convert to My Contact" opens a rename dialog so the user can choose a
human-friendly name in one go; the StructuredName update + auto-flip
happen in a single use-case call. "Convert all" inside the bulk-select
mode promotes every selected row using its existing display name —
intentionally minimal because the common case is a single rename.

### Bottom navigation deferred

The 5-tab bottom-nav (Calls / My Contacts / Inquiries / Stats / Settings)
lands in Sprint 11. For Sprint 5 the new screens are reachable from the
CallsScreen overflow menu (`MoreVert`); routes are stable so the bottom
nav can swap in without churn.

### applyBatch over individual inserts

`ContactsWriter.insertAutoSavedContact` always uses `applyBatch` with a
single back-referenced batch — this is atomic (no orphan RawContacts on
partial failure) and lets Android route the operation through one provider
transaction rather than four.

## Sprint 6 — Auto-Tag Rules & Lead Scoring

- **Cascade-deletes are application-level, not foreign-key.** When a rule is
  removed we explicitly clear `call_tag_cross_ref WHERE appliedBy =
  "rule:<id>"` and the rule's rows in `rule_score_boosts` before deleting the
  rule entity. Schema-level `ON DELETE CASCADE` would couple the rule entity
  to a string column that Room cannot index efficiently — handling this in
  `AutoTagRuleRepositoryImpl.delete` keeps the schema simple while still
  honouring spec §8.3.
- **`rule_score_boosts` PK = (callSystemId, ruleId).** A single boost per
  (call, rule) makes the action idempotent and lets the lead-score formula
  sum a stable per-number total. Sums are taken via JOIN to `calls` so
  soft-deleted calls are excluded automatically.
- **Match-count preview uses a 200-call snapshot.** `previewMatchCount`
  iterates the most recent 200 calls instead of the entire history. This
  keeps the live-preview banner under 100 ms for typical user databases and
  matches the spec §3.7 "200 latest calls" cap.
- **Lead-score weights are debounced 400 ms before persisting.** Sliders feed
  an in-memory snapshot first; only after the user pauses do we serialise the
  new `LeadScoreWeights` to DataStore and enqueue
  `LeadScoreRecomputeWorker`. This avoids hammering the worker queue while
  the user drags the slider.
- **Sync recomputes lead scores synchronously.** Per spec §8.3, the per-sync
  recompute runs inline after rules are applied so the new boosts feed the
  score formula in the same pass. The full-population recompute is reserved
  for the worker triggered from settings changes.

## Sprint 7 — Real-Time Features

- **Compose-in-overlay deferred — overlays use plain Android views.** The
  spec called for `ComposeView` inside the WindowManager overlay with custom
  `ViewTree*Owner` fakes. Doing that correctly across configuration changes
  and back-press handling is fragile, especially without an `Activity`.
  Floating bubble + post-call popup are therefore implemented as
  programmatic `FrameLayout`/`LinearLayout` trees. The visual surface is
  small (a bubble + a card) and Compose adds no value; this also keeps the
  service free of any Activity-lifecycle plumbing.
- **TelephonyCallback number resolution via secondary BroadcastReceiver.**
  `TelephonyCallback.CallStateListener` does NOT include the phone number
  in its callback (privacy guarded). To recover it on API 31+, the monitor
  also registers a runtime receiver for `ACTION_PHONE_STATE_CHANGED` which
  carries `EXTRA_INCOMING_NUMBER` — but only when `READ_CALL_LOG` is held.
  When the permission is missing the flow still emits state transitions
  with a `null` number; overlays handle that gracefully by falling back to
  the most recent known number or skipping the popup.
- **Lifecycle-service dependency added.** The service uses `LifecycleService`
  for its `lifecycleScope`. Added `androidx.lifecycle:lifecycle-service` and
  `androidx.savedstate:savedstate` to the catalog.
- **Bubble notes are number-level (callSystemId = null).** Mid-call notes
  are persisted before the call entity exists. `NoteDao.attachOrphans` is
  invoked from `SyncCallLogUseCase` once a sync inserts the matching call,
  using a `±60s` window around the call's date to associate orphan notes.
- **Service start gating.** `RealTimeServiceController.evaluateAndApply`
  starts the foreground service only when at least one of the two toggles
  is enabled AND `Settings.canDrawOverlays` is true. Without overlay
  permission the service would have nothing useful to do, so we don't burn
  the foreground slot.

## Sprint 9 — Export & Backup

- **PDF charts deferred.** Spec §3.15 calls for chart images (line / donut /
  heatmap) embedded in the PDF, captured from Compose. iText 8 supports
  `Image` from PNG bytes, but capturing Compose canvases off-screen requires
  spinning up an `AndroidView` host and a `PixelCopy` round-trip — a real
  Activity-bound subgraph. For v1 the PDF emits Cover → Totals →
  paginated calls table only. Charts land alongside the Sprint-10 self-update
  polish work.
- **Encryption: PBKDF2 + AES/GCM/NoPadding instead of Tink keysets.**
  Tink is in the `libs` catalog and AeadConfig works fine on Android, but
  Tink's `KeysetHandle` story for *passphrase-derived* keys requires either
  shipping a separate keyset file or using `KeysetHandle.read` with an
  `Aead` wrapper — extra moving parts that don't add security beyond a
  proper KDF. The chosen path (`PBKDF2WithHmacSHA256`, 120k iterations,
  16-byte salt embedded in every blob, 12-byte IV, AES-256-GCM 128-bit
  tag) gives the same guarantees with one self-contained file per backup.
  Magic header `CVB1` lets us detect format version on restore.
- **Backup file format `.cvb`.** Layout
  `MAGIC(4) | VERSION(1) | SALT(16) | IV(12) | CIPHERTEXT+TAG(rest)`.
  Plaintext is the JSON dump produced by `JsonExporter.encodeFullDump()`.
- **Restore is destructive in a single transaction.** `BackupManager.restore`
  decrypts → parses → calls `db.withTransaction { wipeAll(); insert(...) }`.
  On any throw the transaction rolls back, so partial restores are
  impossible. `wipeAll` truncates the seven user-data tables and
  `sqlite_sequence` so autoincrement counters don't carry over.
- **Custom date pickers deferred.** The date-range step ships with chip
  presets (All time / 7 / 30 / 90 / This month). Custom-range UI uses the
  existing `DateRange.last30Days()` etc. — a Material date-range picker
  arrives in a follow-up so we don't bloat Sprint 9 UI.
- **MediaStore-only writes.** All exports go to the public Downloads
  collection via `MediaStore.Downloads` on API ≥29. Pre-Q falls back to
  `Environment.getExternalStoragePublicDirectory(DOWNLOADS)` without
  requesting `MANAGE_EXTERNAL_STORAGE`.
- **Auto-backup retention via MediaStore query.** `DailyBackupWorker`
  globs `callvault-backup-%.cvb` in Downloads, sorted by `DATE_ADDED DESC`,
  and deletes everything past index `keep`. Pre-Q devices skip rotation.

### 2026-05-02 — Drive backup deviates from spec §13
Optional cloud backup added per user request. Gated behind `backupDriveEnabled`
setting (default OFF). Uses AppAuth (NOT play-services-auth) to remain GMS-free
per spec §1. Encrypted local backup runs first; the encrypted .cvb is uploaded
unchanged. Documented in-app at assets/docs/16-google-drive-backup.md and in
docs/locale/06-google-cloud-setup.md.
