# Changelog

All notable changes to callNest are documented in this file.

## [Unreleased] — All four deferrals shipped (2026-05-07)

### Added

- **Pipeline as a 5th bottom-nav tab.** New `MainTabRoute.Pipeline` registered in `MainScaffold`'s inner NavHost; tab bar shows 5 tabs with `BarChart` icon. `PipelineScreen.onBack` is now nullable so tab-root usage hides the back arrow.
- **Compact Pipeline mode for narrow screens** (`< 600dp` width). Vertical `LazyColumn` of collapsible stage sections (Lost collapsed by default). Each section is a drag-and-drop target. Drag-and-drop, multi-select, and bulk-blast all continue to work in compact layout.
- **Onboarding demo data seed.** New `data/demo/DemoSeeder` inserts 6 demo calls + 4 contacts + 2 pipeline stages (Priya · Qualified, Neha · Contacted) on first run after onboarding. New `demoSeedActive` + `demoSeedDismissedOnce` prefs gate the seed; once cleared, never re-seeded. Pipeline screen shows a tertiary-container banner with a "Clear demo" button. Demo numbers use names like "Priya (demo)" so they're visually flagged.
- New `ContactMetaDao.deleteByNumber` (used by demo cleanup).
- 6 new strings + Hindi translations (demo banner + Pipeline tab label).

### Changed

- **Palette policy documented** in `docs/palette-policy.md`. Source of truth: Sage/Neo tokens.
  - `M3 ColorScheme.tertiaryContainer` / `onTertiaryContainer` were unset and falling back to default M3 pinks. Now mapped to `SageColors.SurfaceAlt` / `TextPrimary`. Affects: digest AI card, demo banner, Why-Score sheet.
  - `M3 ColorScheme.errorContainer` / `onErrorContainer` now sage-tinted. Affects: DeleteAccountDialog destructive surface.
  - Rule: M3 widgets first, `NeoSurface` for bespoke brand cards, no raw hex outside `theme/Color.kt`.

## [Unreleased] — Audit pass 3 fixes (2026-05-07)

### P0 (correctness / security) fixes

- **Weekly digest miscounted every call type.** `CallEntity.type` stores Android `CallLog.Calls.TYPE_*` raw ints (`INCOMING=1, OUTGOING=2, MISSED=3, REJECTED=5, BLOCKED=6`). Previously assumed enum ordinals — every digest reported `incoming=0` and shifted all other counts. Fixed via `CallType.fromRaw(c.type)`.
- **Anthropic API key now encrypted at rest.** New `data/secrets/SecretStore` backed by `EncryptedSharedPreferences` (AES256-GCM). Plaintext value in DataStore is auto-migrated then cleared on first run after upgrade. The app no longer reads/writes the key in plaintext.
- **Hot-lead alert now uses the live lead score (with bonuses).** `HotLeadNotifier` now invokes `ComputeLeadScoreUseCase` with `hasFollowUp` + `customerTagApplied` so a contact whose score crosses 70 only via bonuses correctly triggers the alert. Notification body shows the same effective score the Why-Score sheet shows.
- **Anthropic model id is now a dated snapshot.** Was bare `claude-haiku-4-5` (likely 404). Now `claude-haiku-4-5-20251001`. Failure path returns a typed `AnthropicClient.Result.Failure` with HTTP code; `WeeklyDigestViewModel` exposes `aiError: StateFlow<String?>`.
- **`MainActivity` deep-link Channel.** Replaced `MutableStateFlow` with `Channel<String>(BUFFERED, DROP_OLDEST)` + a `produceState` adapter, so consecutive same-route deep links re-fire instead of being silently coalesced.
- **`WeeklyDigestWorker` now `PeriodicWorkRequest`.** 7-day period + 6-hour flex window, `KEEP` policy, `setRequiresStorageNotLow`. Added a `weeklyDigestLastFiredMs` pref-backed re-fire guard (skip if last fire < 6 days). Replaces fragile `OneTimeWork` self-chain.

### P1 fixes

- **CSV phone normalizer hardened.** Now requires Indian local mobile prefix `[6-9]` and strict length matching: `+E.164` (8-15 digits), bare 10-digit, `0`-prefixed 11-digit, or 12-digit `91XXXXXXXXXX`. Rejects garbage 11- and 13-digit strings.
- **Top tags heading made honest.** "Top concerns this week" → "Top concerns you flagged this week" (since the SQL filters by `appliedAt`, not call date). Both EN + HI updated.
- **Templates corrupt-JSON guard.** `MessageTemplateRepositoryImpl` refuses to overwrite when parse fails; previously a single bad write could wipe all user templates on the next save.
- **`{name}` substitution fallback.** `TemplateInterpolator.interpolate(body, name, fallback)` accepts a fallback string; both quick-reply and bulk-blast pass `R.string.template_fallback_name` ("there" / "साथी") so unsaved contacts get a polite greeting instead of "Hi , …".
- **Pipeline drag/select race.** `DragAndDropTarget.onStarted/onEnded` now toggle `dragInProgress`; the toolbar select-mode entry button is disabled while a drag is in flight.

### UI/UX improvements (audit Part B)

- **Empty states** for Pipeline, Templates, CSV Import via new shared `ui/components/EmptyState` (emoji + title + body).
- **Welcome tagline** rewritten with brand voice: "Every call, a possibility." / "हर कॉल — एक मौका।".
- **Animated lead-score badge** when score ≥ 70 — alpha pulses 0.18 → 0.42 with a 1.2s reverse-tween.
- **Live preview of `{name}` rendering** under the body field in the templates Add-template dialog (sample name "Rajesh").
- **Why-Score sheet** got a "How can I improve this score?" button (placeholder action; routes to docs in next pass).
- **Lock screen branding** — small "callNest" wordmark in primary color above the headline.
- **Drag haptics** — long-press to start a card drag now triggers `HapticFeedbackType.LongPress`.
- **Digest breakdown bar** — lightweight 3-segment proportional bar (incoming / outgoing / missed) replaces text-only rendering. No Vico dependency.
- **Hot-lead notification ringtone** — channel now uses the device's ringtone (`USAGE_NOTIFICATION_RINGTONE`) + custom vibration pattern. Distinctly louder than a regular notification.

### Deferred (rationale recorded)

- **Pipeline compact-mode on phones** (#2) — needs a separate vertical UI; non-trivial. Tracked.
- **Onboarding demo data** (#3) — needs a seed system with dismiss + cleanup. Tracked.
- **Bottom-nav reshuffle to promote Pipeline** (#7) — requires touching `MainScaffold` + 5-tab UI; risky to do without dedicated regression pass. Pipeline remains the first row on More, ~1 tap.
- **Sage palette consistency audit** (#9) — codebase-wide refactor.
- **Pull-to-refresh on Calls/Inquiries** (#6) — already wired in `CallsScreen` (`PullToRefreshBox`); confirmed in audit.

## [Unreleased] — Top-tag analysis + AI digest (2026-05-07)

### Added

- **Top concerns this week** — `TagDao.topTagsBetween(fromMs, toMs)` JOINs `call_tag_cross_ref` × `tags` filtered by `appliedAt`. Up to 5 tag-name × count rows. Surfaces in the digest screen as "Top concerns this week".
- **AI summary on the weekly digest** (BYOK Claude Haiku). Opt-in via `Settings → Privacy → AI summary on weekly digest`. User pastes their Anthropic API key (`sk-ant-…`), stored locally in DataStore, never in the binary. `AnthropicClient` POSTs only aggregate counts + first names + tag names — **no phone numbers, no notes, no raw call records** ever leave the device. Returns 2-3 sentence narrative rendered in a tertiary-container card on the digest screen with a Regenerate button.
- 14 new strings + Hindi translations (`digest_top_tags_*`, `digest_ai_*`, `settings_ai_digest_*`, `settings_anthropic_key_*`).

### Notes

- The privacy posture is intentionally narrow: even with AI enabled, the model never sees notes or numbers. If a future iteration wants notes-aware summaries it must surface that as a separate, named opt-in.
- `claude-haiku-4-5` is the cheapest Claude model that returns useful narrative; switch the model id in `AnthropicClient` if a newer/cheaper one ships.
- Network errors fall back silently — the digest still renders without the AI section.

## [Unreleased] — Weekly digest (2026-05-07)

### Added

- **Weekly digest** screen + Monday 9 AM notification + share. New: `WeeklyDigest` domain model, `ComputeWeeklyDigestUseCase` (last 7 days: total calls, in/out/missed split, unique contacts, hot-lead count, top-5 callers), `WeeklyDigestWorker` (self-chains weekly), `WeeklyDigestScreen` + ViewModel, More tab entry, Settings toggle (default ON).
- Notification reuses `CHANNEL_DAILY_SUMMARY`. Share button on the screen builds a plain-text rollup via `Intent.ACTION_SEND` (works with WhatsApp / Email / any chooser target).
- 14 new strings + Hindi translations (`digest_*`, `settings_weekly_digest_*`, `more_weekly_digest`).

### Notes

- Implementation is **local-first / no LLM** — the "AI" framing is reserved for a follow-up that opts into a small LLM call (e.g. Gemini Flash / Claude Haiku) for narrative summarization of notes. Hook point: post-process `WeeklyDigest` into a paragraph in `WeeklyDigestViewModel.refresh`.
- Top-tag analysis (e.g. "top concern: pricing") is deferred — needs a JOIN over `CallTagCrossRef` × time-window. The current "top callers" list is a useful proxy for "who to focus on this week."
- Worker is wired to `callNestApp.onCreate` after onboarding completes.

## [Unreleased] — Hot-lead call alerts (2026-05-07)

### Added

- **Hot-lead inbound alert** — when a call rings from a known top lead (computed lead score ≥ 70 OR pipeline stage Qualified/Won), the app posts a high-priority "🔥 Hot lead is calling" notification on the new `hot_lead` channel. Tapping opens the contact's Call Detail.
- New notification channel `CHANNEL_HOT_LEAD` (IMPORTANCE_HIGH, vibration on) registered in `callNestApp`.
- New `HotLeadNotifier` class injected into `CallEnrichmentService.observeStates()` Ringing handler.
- New setting `hotLeadAlertsEnabled` (default ON) with a toggle in Settings → Privacy.
- Hindi translations for the channel + notification body + setting label.

### Notes

- This is a **local** notification; no Supabase / FCM round trip. Works fully offline.
- Multi-device push fan-out (cross-device alert when one device detects the call) is the next layer — needs a Supabase Edge Function reading from `device_tokens`. Deferred.
- The realtime check requires `CallEnrichmentService` to be running, which means the user must have at least one of the realtime toggles (floating bubble or post-call popup) enabled. If those are off, the service stops itself and hot-lead alerts won't fire.

## [Unreleased] — Drag-to-move on Pipeline (2026-05-07)

### Added

- **Drag-and-drop on Pipeline cards.** Long-press a card and drag it to any column — drop releases into that stage. Uses `Modifier.dragAndDropSource` (long-press → `startTransfer`) on cards and `Modifier.dragAndDropTarget` on columns. Hover-highlight: column background tints to `primaryContainer` while a card is over it. ClipData mime is `text/plain` carrying the normalized number.
- Bottom-sheet stage picker (`StagePickerSheet`) is no longer wired to a card gesture — drag handles all stage moves. Sheet code retained for now (no callers).

### Notes

- Selection mode (multi-select for bulk WhatsApp) disables drag on cards: tap toggles, long-press toggles, drag is reserved for non-selection mode.
- Auto-scroll of the LazyRow while dragging is left to the platform default; not all devices scroll edges automatically.

## [Unreleased] — Sales workflow polish (2026-05-07)

### Added

- **`{name}` and `{firstName}` template tokens** — substituted with the contact's display name when sending. Helper: `util/TemplateInterpolator`. Applied in both single Quick Reply (`WhatsAppQuickReplySheet`) and Bulk Blast progress sheet.
- **"Move overdue → Lost" toolbar action** on the Pipeline screen. Confirms via dialog; snackbar shows the count moved.
- **CSV contact import** — new screen at More tab → "Import contacts (CSV)". File picker (SAF), header-aware parser tolerant of either order ("name,phone" or "phone,name"), Indian-number normalization (`+91` for 10-digit), preview of first 50 rows, batch insert via `ContactsContract.applyBatch`. New util: `CsvContactParser`. Manifest already had `WRITE_CONTACTS`.
- **Hindi for ~30 most-visible existing strings** (tab labels, Calls / Inquiries / Follow-ups headings, common buttons). Pre-existing English strings outside auth are still untranslated; this is a focused first sweep.

### Notes

- Drag-to-move on Pipeline cards intentionally **not** implemented — long-press → bottom sheet covers the same outcome with much less custom-gesture code. Tracked as a P3 polish item.
- CSV import inserts new contacts; it does NOT dedupe against existing ones (sufficient for the import-from-Excel use case).

## [Unreleased] — Lead-score "Why?" tooltip (2026-05-07)

### Added

- **Why-score sheet on Call Detail.** Tapping the lead-score badge on `HeroCard` now opens a bottom sheet that breaks the 0–100 score into its components: frequency, total talk time, recency, follow-up bonus, customer-tag bonus, saved-contact bonus, rule boosts. Each row shows points contributed.
- `LeadScoreBadge` is now clickable (was non-interactive); CallDetailViewModel exposes `breakdown: StateFlow<LeadScore?>` and `loadBreakdown()` lazily.
- 14 new strings + Hindi translations (`why_score_*`).

### Notes

- Customer-tag detection uses a name-match (`tag.name.equals("customer", ignoreCase=true)`) since `Tag` doesn't carry a customer flag. Matches existing convention.
- Total in the sheet is recomputed live so it includes follow-up + customer-tag bonuses even if the persisted `computedLeadScore` (set by the background worker) didn't include them — gives users a more honest breakdown.

## [Unreleased] — Bulk WhatsApp blast (2026-05-07)

### Added

- **Multi-select on Pipeline screen**: tap the toolbar select icon to enter selection mode, tap cards to add/remove (selected cards highlight with primary container color), back gesture exits.
- **Bulk WhatsApp blast** flow: with N selected, tap the WhatsApp action → template picker bottom sheet → progress sheet listing each contact with a per-row "Open" button. Tapping Open fires `wa.me/<digits>?text=<encoded body>`; the row marks as "Sent".
- New composables: `BulkBlastTemplatePickerSheet`, `BulkBlastProgressSheet`.
- 11 new strings + Hindi translations (pipeline*selected*_, pipeline*bulk*_, bulk*blast*\*).

### Notes

- WhatsApp doesn't support a true zero-tap blast — the user still taps Send in WA per chat. The progress sheet keeps the workflow tight: open → send → return → tap next Open.
- Reuses `TemplatesViewModel` (one shared list across single Quick Reply and bulk Blast surfaces).

## [Unreleased] — Lead pipeline Kanban (2026-05-07)

### Added

- **Lead pipeline view** — 5-column horizontal Kanban (New → Contacted → Qualified → Won → Lost) under More tab. Tap a card → opens Call Detail; long-press → bottom sheet to move stage.
- New entity `pipeline_stage`, DAO `PipelineStageDao`, migration `MIGRATION_3_4`, DB version 3 → 4.
- `PipelineStage` enum, `PipelineRepository` interface + impl, Hilt binding.
- `PipelineScreen` + `PipelineViewModel`. Each card shows: name (or number), call count, lead-score badge.
- 10 new strings + Hindi translations (pipeline\_\*).
- Nav route `Destinations.Pipeline` wired into `callNestNavHost`.

### Notes

- A contact without an explicit row defaults to "New". Setting back to New deletes the row to keep the table sparse.
- ContactMeta is auto-recomputed at sync end; the pipeline_stage table is independent so user-set stages survive sync.

## [Unreleased] — WhatsApp quick-reply templates (2026-05-07)

### Added

- **Quick-reply bottom sheet on Call Detail** — tapping the WhatsApp action now opens a sheet with all templates; tap a template to open `wa.me/<digits>?text=<body>`.
- **Templates manager screen** at `Settings → Manage templates`. Shows built-ins (read-only, "Built-in" chip) + user-added templates with swipe-free delete. FAB for adding via dialog (label + body).
- **5 built-in templates** (English): "Thanks for your inquiry", "Send catalog", "Follow-up", "Quote / pricing", "Busy — call you back".
- New domain model `MessageTemplate`, repository interface + DataStore-backed impl. Storage: `messageTemplatesJson` pref key (user-added only; built-ins are constants).
- 13 new strings extracted (templates*\*, quickreply*\*).

## [Unreleased] — Hindi i18n (auth + lock) (2026-05-07)

### Added

- `app/src/main/res/values-hi/strings.xml` — Hindi translations for all 60+ auth, lock, and new settings strings. Pre-existing English strings (Sprints 0–12) are not translated yet; this is a focused i18n landing for the cloud-pivot surface.

### Notes

- Test by switching system language to **हिन्दी (भारत)** in Android Settings → System → Languages. The Welcome → Login → Profile flow + Lock screen + new Settings rows render in Hindi.
- App brand "callNest" is intentionally untranslated; the welcome tagline and all body strings are localized.

## [Unreleased] — Sprint 14: Pass-2 P0/P1 fixes (2026-05-07)

### Fixed

- **Account deletion was silent on RPC failure.** Collapsed verify+delete into one Postgres RPC `delete_current_user(p_password)` that verifies the password (via `pgcrypto.crypt`) and deletes in a single transaction. Now throws on wrong password / missing function — caller sees a real error. (P0-1, P0-3)
- **Re-auth dialog no longer closes during the call.** Dialog stays open with the spinner; closes only on `AccountDeleted` or stays open after `Error` so the user can retry. (P0-2)
- **No more session rotation during account deletion.** Removed the `verifyCurrentPassword` step that was re-signing-in (and killing the refresh token if user cancelled). (P0-3)
- **LockScreen no longer auto-unlocks if the device has no biometric/PIN.** When `canAuthenticate != SUCCESS`, shows an explainer with two actions: open Security Settings, or disable App lock. Never bypasses. (P0-4)
- **Recovery deep-link race fixed.** `ResetPasswordScreen` now waits for `AuthState.SignedIn` before enabling the submit button; shows "Waiting…" while the recovery session is verifying, "Link expired" if no session arrives. (P0-5)
- **`onNewIntent` now updates the NavHost** via a `deepLinkFlow: MutableStateFlow<String?>`. Recovery email tap on a backgrounded app now actually navigates to ResetPassword. (P1)
- **`displayName` no longer rendered as a JSON-quoted string.** Switched to `jsonPrimitive.contentOrNull`. (P1)
- **`PushTokenSync` no longer upserts on every Authenticated re-emission.** `distinctUntilChanged` on user id keeps writes to one per actual session change. (P1)
- **`LockScreen.prompted` is now `rememberSaveable`** — no double-prompt on rotation. (P1)
- `BIOMETRIC` cancel/negative-button paths no longer surface as red error text — they're soft, expected. (P1-9)

### Notes

- Re-run `app/src/main/assets/db/delete_user.sql` in Supabase — schema changed to take a password parameter.

## [Unreleased] — Settings rows + account deletion + re-auth (2026-05-07)

### Added

- Settings rows under Privacy: **App lock** (biometric/PIN) and **Help improve callNest (anonymous analytics)**. Both flags previously only existed in DataStore. (audit v2 #7, follow-up to biometric pass)
- **Account deletion** flow on `ProfileScreen` — re-auth dialog (`DeleteAccountDialog`) verifies password, then calls `delete_current_user()` Postgres RPC. SQL committed to `app/src/main/assets/db/delete_user.sql`. (audit v2 #10)
- `AuthRepository.verifyCurrentPassword(password)` and `deleteAccount()`. New `AuthEvent.PasswordVerified` / `AuthEvent.AccountDeleted` events.

### Notes

- Account deletion requires running `delete_user.sql` in Supabase before it works in the app.
- Re-auth gate: typing the password and tapping Permanently delete chains `verifyPassword → onSuccess → deleteAccount`. Wrong password surfaces a snackbar and aborts.

## [Unreleased] — Strings extraction + biometric lock (2026-05-07)

### Added

- **Biometric app lock** — `androidx.biometric:biometric` dependency + `LockScreen` + `AppLockState` (process-scoped unlock flag) + `SettingsDataStore.biometricLockEnabled` flag. When enabled, a full-screen lock gate is shown before the NavHost; uses `BIOMETRIC_WEAK | DEVICE_CREDENTIAL` so the user can fall back to PIN. Auto-locks in `MainActivity.onStop`. (audit v2 #8)
- `MainActivity` now extends `FragmentActivity` (required by `BiometricPrompt`).

### Changed

- All ~50 inline strings across the 8 auth screens extracted to `strings.xml` keys (`auth_*`, `lock_*`). `EmailField`/`PasswordField` content descriptions also localized. (P1-4, P2-4)

## [Unreleased] — Cloud-pivot P1 + P2 polish (2026-05-07)

### Fixed

- `RefreshFailure` no longer drops the user back to auth — keeps the last known `SignedIn` state until truly de-authenticated. (P1-2)
- `signInWithEmail` and signUp errors now demoted to `Timber.w` (was `e` for expected validation failures). (P3-3)
- `SUPABASE_ANON_KEY` is now `require`-validated alongside the URL. (P1-11)

### Added

- Password strength meter under the Sign-up password field (4-segment bar + label). (P1-10)
- `PrimaryAuthButton` reusable composable — spinner + label render together while busy (no more "button collapses to spinner" flash). (P2-1)
- IME actions on email/password fields — pressing Done on the password submits Login. (P2-2)
- `ProfileScreen` `onBack` parameter and back arrow in the TopAppBar. (P2-5)
- App icon (`mipmap/ic_launcher`) above the title on the Welcome screen. (P2-9)
- KDoc on `EmailField`, `PasswordField`, `isValidEmail`, `AuthDestinations`, `authGraph`. (P1-12)

### Changed

- `EmailField`: `autoCorrectEnabled = false`, `KeyboardCapitalization.None`, `imeAction = Next`. (P2-8)

## [Unreleased] — Cloud-pivot P0 fixes (2026-05-06)

### Fixed

- Login spinner no longer stuck after success/error: `AuthViewModel` now exposes `busy: StateFlow<Boolean>`; events delivered via `Channel.receiveAsFlow()` (was a non-replaying SharedFlow with a startup race).
- Sign-up flow auto-routes to home when Supabase project has email confirmation disabled (was always navigating to verify-email screen).
- `VerifyEmailScreen` observes auth state and auto-routes to home on confirmation; the action button now polls `refreshSession()` instead of dead-end navigating.
- FCM `device_tokens` upserts now use `@SerialName("user_id")` / `@SerialName("fcm_token")` (was sending camelCase, every upsert silently failed against snake_case columns).
- FCM token persisted on auth-state transition to `Authenticated` (was attempted at app start before Supabase rehydrated session).
- Password reset email now passes `redirectUrl = "callNest://auth/recovery"`; `MainActivity` handles the deep link via Supabase + routes to `ResetPasswordScreen`.
- `signUpWithEmail` now persists `displayName` to Supabase user metadata as `full_name`.
- Removed `signInWithGoogle` from `AuthRepository` interface (was throwing `IllegalStateException` to users when scaffolded). Activation steps documented as a comment in `AuthRepositoryImpl`.

### Changed

- **PostHog analytics is consent-gated.** New `SettingsDataStore.analyticsConsent` flag, default off. `PostHogTracker.init()` no longer auto-starts; `setConsent(true)` triggers `PostHogAndroid.setup`. Email and full name are no longer sent as PostHog properties — only the Supabase `userId` as `distinctId`.
- `AuthRepository` extended with `updatePassword`, `resendVerificationEmail`, `refreshSession`. `AuthViewModel` no longer imports from `data/` (architecture violation closed).
- Auth nav graph wired into `callNestNavHost` — sign-up, forgot, verify, reset routes are reachable. Splash redirects to `AuthDestinations.GRAPH` instead of the old `Destinations.Login`.
- Minimum password length raised from 6 → 8 in sign-up and reset flows.
- `RefreshFailure` no longer treated as `SignedOut` (kept as transient).

### Added

- `app/src/main/assets/db/device_tokens.sql` — committed migration for the FCM table.
- `<intent-filter>` for `callNest://auth/*` deep links on `MainActivity`.

## [1.0.0] — 2026-04-30

Initial release — full feature set delivered across 13 sprints (Sprint 0–12).

### Added

**Foundation & UI**

- Single-module Android project, Kotlin 2.0.21, Compose BOM 2024.12.01, Material 3.
- Neumorphic design system: `NeoColors`, dual-shadow `Modifier.neoShadow`, 18 reusable Neo\* components (Surface, Card, Button, IconButton, Chip, Toggle, Slider, SearchBar, FAB, TabBar, TopBar, BottomSheet, TextField, ProgressBar, Badge, Avatar, Divider, EmptyState, HelpIcon, LeadScoreBadge).
- `callNestTheme` with Inter/system fallback typography, locked light scheme.

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

- `ContactGroupManager` ensures "callNest Inquiries" group; `ContactsWriter` uses `ContentResolver.applyBatch` for atomic insert.
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
- 5-tab `NeoTabBar` MainScaffold is wired but the nested-graph integration into `callNestNavHost` is a small follow-up; current entry points are via `CallsScreen` overflow + dedicated routes.
- `NeoHelpIcon` exists but is not yet placed in every screen's app bar (component is drop-in).
- Voice-to-text on notes is intentionally deferred to v2 per spec §3.9.
- `ResetAllDataUseCase` wipes notes/search/skipped only; calls/contacts/tags retain (re-sync from OS log resets calls).
- Build verification (`./gradlew lint assembleDebug`) is the operator's responsibility — agents in this run are scoped to file generation only.

### Tech debt

- `LazyColumnItemsScopeShim` dead helper in `StatsScreen.kt` (harmless).
- A handful of `@Preview` blocks were dropped for time; non-functional gap.
- `UpdateAvailableScreen` / `UpdateSettingsScreen` use Material 3 primitives directly in some places (Neo\* swap is a cosmetic follow-up).
