# 06 — Glossary

Domain terms used throughout CallVault code, docs, and spec. When in doubt, this file is the source of truth.

## Call & contact

**Call** — a single row from `CallLog.Calls`, mirrored to Room as `CallEntity`. Identified by `systemId` (= `CallLog._ID`). Multiple calls can share a `normalizedNumber`.

**Normalized number** — phone number in E.164 form (e.g. `+919876543210`). Produced by `PhoneNumberNormalizer` via libphonenumber-android with default region `IN`. Calls without a parseable number get an empty string + `isPrivate=true` flag.

**Raw number** — the original string from `CallLog.Calls.NUMBER`, preserved as-is for audit.

**Contact meta** — per-`normalizedNumber` aggregates kept in `ContactMetaEntity`: total calls, total duration, first/last call dates, type counts, lead score, auto-save state. Recomputed on every sync.

**SIM slot** — `0` or `1`, resolved from `CallLog.PHONE_ACCOUNT_ID` via `SubscriptionManager`. Displayed as "SIM 1" / "SIM 2" (1-indexed for users).

## Inquiry & contact segregation

**Inquiry** — an unsaved-number call. The user receives many inquiries per day; CallVault's job is to capture them.

**Auto-saved contact** — an inquiry written to system Contacts by CallVault using a generated name pattern (e.g. `callVault-s1 +919876543210`). Tracked via `ContactMetaEntity.isAutoSaved=true`.

**My Contacts** — top-level UX bucket for "real" contacts (manually saved or auto-saved-then-renamed-by-user). Contains contacts where `isInSystemContacts=true && isAutoSaved=false`.

**Inquiries** (bucket) — top-level UX bucket for auto-saved-but-not-yet-renamed inquiries. Contains contacts where `isAutoSaved=true`.

**Lenient bucketing** — the rule per §8.5: if the user opens system Contacts and renames an auto-saved entry, `isAutoSaved` flips to `false` on next sync (the contact moves from Inquiries to My Contacts automatically).

**Auto-save name format** — `{prefix}{simTag} {fullNormalizedPhone}{suffix}` per §8.4. Configurable in Settings → Auto-Save.

**Auto-save pattern matcher** — regex compiled from current settings used to detect whether a system-contact name still matches the auto-save format.

## Lead, score, rule

**Lead score** — 0–100 integer per `normalizedNumber`, computed by `ComputeLeadScoreUseCase` per §8.2. Components: call frequency, total duration, recency decay, follow-up bonus, customer-tag bonus, saved-contact bonus, rule score boosts.

**Bucket** — the user-facing band for a lead score: `Cold` <30, `Warm` 30–70, `Hot` >70.

**Recency decay** — `100 * exp(-daysSinceLastCall / 14)`. Half-life ~10 days.

**Manual override** — user can pin a score in Call Detail. While `leadScoreManualOverride != null`, automatic recomputation is suppressed.

**Auto-tag rule** — user-defined `(conditions, actions)` pair. Conditions ANDed, rules iterate top-to-bottom by `sortOrder`. JSON-stored in `AutoTagRuleEntity` via kotlinx.serialization (class-discriminator `"type"`).

**Rule condition** — sealed interface variant: `PrefixMatches`, `RegexMatches`, `CountryEquals`, `IsInContacts`, `CallTypeIn`, `DurationCompare`, `TimeOfDayBetween`, `DayOfWeekIn`, `SimSlotEquals`, `TagApplied`, `TagNotApplied`, `GeoContains`, `CallCountGreaterThan`.

**Rule action** — sealed interface variant: `ApplyTag`, `LeadScoreBoost`, `AutoBookmark`, `MarkFollowUp`.

**Rule score boost** — when a `LeadScoreBoost(delta)` action fires, a row goes into `rule_score_boosts` keyed by `(callSystemId, ruleId)`. Lead score sums all boosts per number.

**Applied-by** — every `CallTagCrossRef` carries `appliedBy = "user"` or `"rule:${ruleId}"`. On rule delete, all `appliedBy = "rule:${id}"` rows cascade-clean.

## Tags, notes, bookmarks, follow-ups

**System tag** — one of 9 default tags seeded on first run (`Inquiry`, `Customer`, `Vendor`, `Personal`, `Spam`, `Follow-up`, `Quoted`, `Closed-won`, `Closed-lost`). Renamable, recolorable; not deletable.

**User tag** — any non-system tag. Unlimited.

**Tag merge** — moves all `CallTagCrossRef` rows from source tag to target tag, then deletes source.

**Note** — markdown-rendered text attached to a `normalizedNumber` (and optionally a specific `callSystemId`). Mini-journal pattern: many notes per number, each with timestamp.

**Note history** — last 5 versions of a note kept in `NoteHistoryEntity` for undo / audit.

**Orphan note** — a note created during a call (via floating bubble) before the corresponding `CallEntity` exists. Sync attaches orphans to the new call entity within a ±60s window.

**Bookmark** — `isBookmarked=true` flag on `CallEntity` plus optional `bookmarkReason`. Top-5 pinned bookmarks live in DataStore as a JSON list of normalized numbers.

**Follow-up** — scheduled reminder per call. `followUpDate` (epoch ms) + optional `followUpTime` (minutes-of-day). Time = null → fires at 9 AM local. Notification fires via `AlarmManager.setExactAndAllowWhileIdle`.

**Auto-clear follow-up** — when sync detects an outgoing call to a number with an active follow-up, the follow-up is marked done.

## Sync, real-time, export, backup, update

**Sync pipeline** — the 12-step algorithm in §8.1 implemented by `SyncCallLogUseCase`. Wakelock → query CallLog → normalize → upsert → recompute aggregates → apply rules → recompute scores → auto-save → detect renames → save lastSyncCallId → emit progress → attach orphan notes.

**lastSyncCallId** — `CallLog._ID` of the most-recent call seen. Stored in DataStore. Subsequent syncs query `WHERE _ID > :lastSyncCallId`.

**Sync interval** — user setting: `Manual`, `5 min`, `15 min` (default), `1 hour`, `12 hour`, `24 hour`, `Daily 2 AM`. Implementation maps to `WorkManager` Periodic, `AlarmManager` exact, or `OneTimeWorkRequest` chained.

**Floating bubble** — `WindowManager` overlay (`TYPE_APPLICATION_OVERLAY`) shown during an active call by `CallEnrichmentService`. 56dp circle, drag-to-snap-to-edge, expandable to mini-card with note + tag chip.

**Post-call popup** — overlay shown 2s after `CALL_STATE_IDLE`. Auto-dismisses after `postCallPopupTimeoutSeconds` (default 8s). Quick-tag chips, single-line note, "Save contact" button.

**SpecialUse foreground service** — `CallEnrichmentService` runs with `foregroundServiceType="specialUse"` and `PROPERTY_SPECIAL_USE_FGS_SUBTYPE="RealTimeCallEnrichment"`. Required for Android 14+.

**Export** — one of 5 formats: Excel (POI multi-sheet), CSV (UTF-8 BOM), PDF (iText), JSON (kotlinx.serialization full dump), vCard 3.0. Wizard: format → range → scope → columns → destination.

**Backup** — encrypted JSON file (`.cvb`). Format: `CVB1` magic + 16-byte salt + 12-byte IV + AES-256-GCM ciphertext (128-bit tag). Key derived via PBKDF2-HMAC-SHA256 (120,000 iter) from user passphrase. Auto-backup runs daily at 2 AM via `DailyBackupWorker`.

**Restore** — destructive: wipes user-data tables and reinserts from a `.cvb` file. Wrapped in a single Room transaction.

**Update channel** — `stable` or `beta`. Each polls a separate `versions.json` URL configured in `BuildConfig.UPDATE_MANIFEST_*_URL`. Manifest schema: `{ stable: { version, versionCode, apkUrl, sha256, minSupported, releaseNotes }, beta: {...} }`.

**Update flow** — `UpdateCheckWorker` (weekly) → `UpdateChecker` (HTTP fetch) → `UpdateState.Available` → `UpdateBanner` on Calls / `UpdateNotifier` notification → `UpdateAvailableScreen` → `UpdateDownloader` (DownloadManager) → SHA-256 verify → `UpdateInstaller` (FileProvider + `ACTION_VIEW`) → API 26+ unknown-sources gate.

**Skipped version** — user dismissal of an update for a specific `versionCode`. Stored in `SkippedUpdateEntity`. Reset on next non-skipped versionCode or via "Clear skipped" in settings.

## UI & design system

**Neumorphism** — light source top-left, base `#E8E8EC`. Dual-shadow rendering via `Modifier.neoShadow(elevation, shape)`. Convex (raised), Concave (sunken), Flat.

**Neo* component** — any Compose composable in `ui/components/neo/`. Wraps Material 3 internals with neumorphic styling.

**NeoColors** — token object. `Base`, `BasePressed`, `Light` (top-left highlight), `Dark` (bottom-right shadow).

**4% offset rule** — never put pure white or pure black surfaces against the base; kills depth. All surfaces tint at least 4% from base.

**Lead score badge** — small colored dot (gray/amber/red) + score number, rendered by `LeadScoreBadge.kt`.

**MainScaffold** — top-level Compose scaffold with bottom `NeoTabBar` (5 tabs: Home / Calls / My Contacts / Inquiries / More). Default tab: Calls.

**NeoHelpIcon** — `?` icon component that opens a specific in-app docs article. Goes in screen app bars.

## Workers & schedules

| Worker | Schedule | Purpose |
|--------|----------|---------|
| `CallSyncWorker` | Periodic (15m–24h) or AlarmManager exact (5m) or chained OneTime (Daily 2 AM) | Sync pipeline |
| `DailyBackupWorker` | Daily 2 AM | Encrypted backup |
| `UpdateCheckWorker` | Weekly | Self-update poll |
| `DailySummaryWorker` | Daily 9 AM | Notification with day's stats |
| `LeadScoreRecomputeWorker` | One-shot | Recompute all scores after weight change |
| `SeedDefaultTagsWorker` | One-shot on first DB create | Seed 9 system tags |

## Permissions

**Critical permissions** — call log + contacts + phone state. Without these the app can't function. `PermissionManager.isCriticalGranted()` gates Calls screen.

**Special-grant permissions** — `SYSTEM_ALERT_WINDOW` (overlay), `SCHEDULE_EXACT_ALARM` (exact follow-up alarms). Granted via Settings, not runtime dialog.

**OEM autostart** — non-permission setting on Xiaomi/Oppo/Vivo/Realme/Samsung/OnePlus/Honor/Huawei devices. Without it, the foreground service gets killed by aggressive battery managers. `OemBatteryGuide` deep-links to vendor settings; falls back to `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
