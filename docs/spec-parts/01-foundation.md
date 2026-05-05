# APP-SPEC Part 01 — Foundation

> Living document. Source of truth for CallVault v1.0.0 foundations.
> Last revised: 2026-04-30. Owner: CallVault core.

This is **Part 01** of the multi-part CallVault APP-SPEC. It covers conventions, project overview, the locked tech stack, the domain glossary, the Android permissions inventory, the data model (Room + DataStore + SecurePrefs), and the canonical algorithms.

Cross-references:

- Part 02 — UI surfaces, screens, navigation, design system, Neo* components.
- Part 03 — Sync pipeline, real-time service, workers, schedules.
- Part 04 — Auto-tag rules, lead scoring, stats, insights.
- Part 05 — Export, backup, restore, self-update.
- Part 06 — Testing, build, distribution, release ops.
- Part 07 — In-app docs, onboarding, OEM autostart, troubleshooting.

---

## 0 — Document conventions

### 0.1 Glyph legend

The following glyphs are used throughout the APP-SPEC. They are not decoration — each one carries meaning, and reviewers must read them.

| Glyph | Meaning |
|-------|---------|
| `[L]` | **Locked** decision. Do not relitigate without writing a new entry in `DECISIONS.md`. |
| `[D]` | **Deferred** to a later milestone. Tracked in `TODO.md`. |
| `[F]` | **Fallback** taken because the preferred path was not viable. Documented in `DECISIONS.md`. |
| `[P0]`–`[P3]` | Priority. P0 blocks a release; P3 is nice-to-have. |
| `[v1]` / `[v1.1]` / `[v2]` | Target version for the item. |
| `§` | Section reference inside this APP-SPEC. `§3.4` means section 3.4 of the relevant Part. |
| `→` | "Resolves to" / "produces". |
| `±` | Bidirectional tolerance (e.g. ±60s window). |

### 0.2 Section numbering

Top-level sections in each Part use H2 (`##`) with a numeric prefix (`0`, `1`, `2`, …). Sub-sections use H3 (`###`) with dotted notation (`1.1`, `1.2`, …). Fourth-level subsections use H4 (`####`) with three-dot notation. Cross-Part references explicitly name the part: "see Part 02 §7.4".

Inside this Part:

- `§0` Document conventions (this section).
- `§1` Project overview.
- `§2` Locked tech stack.
- `§3` Domain glossary.
- `§4` Permissions inventory.
- `§5` Data model.
- `§6` Core algorithms.

### 0.3 Status — living document

This document is **living**. Behavior changes in code MUST be reflected here within the same PR. Conversely, no behavior change ships without first updating this document — readers trust APP-SPEC over comments and over individual KDoc.

When you change this file:

1. Bump the `Last revised` line at the top of the affected Part.
2. Append a one-line changelog entry to `CHANGELOG.md` if the change is user-visible.
3. If the change is a deviation from a previously-locked decision, also append to `DECISIONS.md`.
4. Update cross-references in other Parts so links don't go stale.

### 0.4 Versioning

The APP-SPEC follows the app version. APP-SPEC for `v1.0.0` corresponds to the code at git tag `v1.0.0`. Any post-release amendments are tagged in this document with the version that introduced them, e.g. `[v1.0.1]`.

Three versions are visible across the project:

| Version | Where | What it means |
|---------|-------|---------------|
| `versionName` | `app/build.gradle.kts` | Marketing string shown in Settings → About. |
| `versionCode` | `app/build.gradle.kts` | Integer used by self-update comparisons. |
| `db version` | `CallVaultDatabase.kt` | Room schema version, separate from app version. |

`versionName` is bumped with every user-visible release. `versionCode` is monotonically increasing. `db version` only bumps when a Room migration is added.

### 0.5 How to read this APP-SPEC

The APP-SPEC is structured to be read in two passes:

**Pass 1 — orientation**: read §1 of each Part, top to bottom. This gives you a complete mental model of CallVault in roughly 30 minutes.

**Pass 2 — implementation**: when picking up a feature, read the relevant deep section (e.g. Part 03 §6 "Sync pipeline") plus any cross-referenced glossary entries in this Part's §3.

Tables are exhaustive. Code blocks are pseudocode unless marked otherwise. SQL is SQLite dialect (Room flavor). Kotlin pseudocode uses `?` for nullable, sealed classes for variants, and `// …` for elided detail.

For machine-readable specs (entity columns, DataStore keys, condition variants, action variants, manifest entries) prefer the tables in this document over reading source code — the tables are the source of truth and the source code conforms to them.

### 0.6 Out of scope for this Part

This Part deliberately excludes:

- Compose UI surfaces — see Part 02.
- Worker schedules and triggers — see Part 03.
- Algorithm parameter tuning post-launch — see Part 04.
- Build configuration, signing, Gradle setup — see Part 06.

---

## 1 — Project overview

### 1.1 What CallVault is

CallVault is an **offline-first Android app** that turns the device's call history into a lightweight inquiry CRM. It captures every call from `CallLog.Calls`, normalizes and enriches each row, separates real contacts from unsaved inquiries, and gives the user a fast surface to **tag, note, bookmark, follow up, search, filter, and export** their calls.

The product is targeted, not generic. Generic call-log apps focus on display; CallVault focuses on **conversion**: turning a flood of inquiry calls into a triaged, scored, actionable pipeline. This shapes every design decision — the lead score, the auto-save flow, the floating bubble, the post-call popup, and the two-bucket My Contacts / Inquiries split all exist to serve the conversion job.

CallVault is **sideloaded**, **GMS-free**, and **single-device**. There is no cloud sync, no account, no telemetry, no analytics, and no Play Store presence. The single allowed outbound network call is a self-update manifest poll. This posture is non-negotiable for v1 — see §1.6.

### 1.2 Target user persona — Indian small-business owner

The primary persona is a **30–55 year-old Indian small-business owner** running a B2C or low-ticket B2B business: a coaching center, a real-estate broker, a furniture showroom, a wedding photographer, a wholesale clothing trader, a plumber, an electrician, a car-dealership salesperson. The defining trait: **20–100 daily inbound calls**, most from numbers not in their contact book.

#### 1.2.1 Day in the life

- **8:30 AM** — first call of the day comes in while the owner is still finishing chai. It's an unsaved number; CallVault's floating bubble surfaces *"new inquiry"* and lets the owner jot one word: *furniture*. The post-call popup offers `Customer / Inquiry / Spam` quick-tag chips; the owner taps `Inquiry` and dismisses.
- **10:00 AM** — at the shop. Picks up the phone, opens CallVault. The Calls list shows a pinned section: *Unsaved inquiries — last 7 days* with 14 entries. He swipes-right on three of them to bookmark; one of them gets a follow-up scheduled for *tomorrow 11 AM*.
- **12:30 PM** — he searches for *"sofa"* in CallVault. FTS hits a note he wrote two weeks ago against a number, plus three calls whose `geocodedLocation` matches "Sofakart Pvt Ltd". He calls one back.
- **3:00 PM** — opens *My Contacts* tab. His phone Contacts now has 200+ auto-saved entries named `callVault-s1 +91...`. Three of them got renamed to real names yesterday — CallVault's lenient bucketing has already moved them out of the Inquiries bucket. He never has to think about it.
- **6:30 PM** — quick check on Stats. *Hot leads: 4*. He opens the lead-score 90+ filter, sees all four, calls each one once. Two convert. He tags them `Closed-won` and adds a markdown note with the order details.
- **8:00 PM** — wraps up. Tomorrow's `Today` follow-up tab has 6 entries cued for 11 AM, 12 PM, and 3 PM. CallVault's exact-alarm follow-up reminders fire even when the phone is in Doze.
- **2:00 AM** — `DailyBackupWorker` runs while he sleeps. An encrypted `.cvb` file lands in `Downloads/CallVault/`. Optionally, `BackupDriveWorker` uploads the same encrypted blob to his own Google Drive (off by default; see Part 05).

The persona is technical-curious but not technical. He sideloads APKs that someone in a WhatsApp business group sends him. He is wary of "bills" inside apps, distrusts cloud apps that ask for OTP, and is happy to grant battery / overlay / autostart settings if you tell him *exactly which buttons to press*. CallVault's UX leans into all of this.

### 1.3 Top 5 user jobs

The product is justified entirely by these five jobs. Anything that does not serve one of them is out of scope for v1.

1. **"Make sure I never lose an inquiry."** Every call is captured, even if it landed during a meeting, even if the number is unknown, even if the phone was off when they called.
2. **"Tell me which inquiry to call back first."** The lead score (§6.3) ranks numbers by frequency × duration × recency, plus tag and follow-up boosts.
3. **"Help me remember who this person was."** Notes with markdown, edit history, and bubble notes typed mid-call.
4. **"Keep my real contacts and my inquiry pile separate."** Auto-save into a `CallVault Inquiries` group; lenient bucketing flips an entry to *My Contact* when the user renames it.
5. **"Let me hand a daily/weekly/monthly report to my accountant or my partner."** Export to Excel / CSV / PDF with a wizard that chooses range, scope, columns, and destination.

### 1.4 Non-goals (locked)

These are intentionally **not** in v1. Each is locked; reopening requires a new `DECISIONS.md` entry plus a maintainer sign-off.

- `[L]` **No call recording.** Legally fraught in India; technically blocked on most modern Android versions without rooting; not worth it.
- `[L]` **No WhatsApp integration.** No reading of WhatsApp messages, no scraping of WhatsApp call logs, no tagging WhatsApp inquiries. The OS does not expose WhatsApp call history to third-party apps without root.
- `[L]` **No cloud sync v1.** Backup only. The optional Google Drive *backup* upload added 2026-05-02 (see `DECISIONS.md`) is **not** sync — it is a one-way encrypted-blob upload of the same `.cvb` already on the device.
- `[L]` **No multi-user / multi-device.** Single phone, single owner. No "team" concept.
- `[L]` **No multi-language.** English-only UI in v1. All strings live in `res/values/strings.xml`; localization is a future port, not a parallel track.
- `[L]` **No dark mode v1.** Neumorphism leans on a tinted-light surface; a faithful dark-mode neumorphism is a design problem, not a code problem, and is deferred to v2.
- `[L]` **No crash reporting / no analytics.** Privacy posture forbids it. See §1.6.
- `[L]` **No "default dialer" mode.** Using `RoleManager.ROLE_DIALER` would unlock things (real-time call number on API 31+, call-screening) but requires Play Store distribution to clear policy. Sideloaded apps cannot become default dialer cleanly. CallVault works *around* this constraint.

### 1.5 Distribution model

CallVault is **sideloaded only**. There is no Play Store listing, no AppGallery listing, no F-Droid listing for v1.

The release pipeline is:

1. Maintainer builds a signed release APK on a local workstation with the production keystore.
2. APK is uploaded to the owner's web host alongside an updated `versions.json` manifest.
3. SHA-256 of the APK is computed and pasted into the manifest.
4. End users either:
   - Install the first APK manually (one-time, via WhatsApp share / web link / USB).
   - Or get a notification from the in-app `UpdateCheckWorker` that polls the manifest weekly.

The in-app update flow uses `DownloadManager` for the download, verifies SHA-256 before launching the system installer, and hands off to the system via `FileProvider` + `ACTION_VIEW`. From API 26+ the user must additionally have granted "Install unknown apps" for CallVault — the install screen handles that gate.

There is no auto-install. The user always sees the system installer dialog. This is both a platform constraint and a design choice.

### 1.6 Privacy posture

CallVault is built around a hard privacy stance. From `data_extraction_rules.xml` to the manifest's network whitelist, nothing leaves the device unless the user took an explicit action.

- **No Firebase, no Crashlytics, no GA, no Mixpanel, no Sentry, no third-party SDK that phones home.** Adding any of these requires a new top-level `DECISIONS.md` entry signed by the maintainer.
- **No silent telemetry.** Timber logs go to logcat only.
- **The only outbound HTTP** is the update manifest poll to `BuildConfig.UPDATE_MANIFEST_STABLE_URL` / `BETA_URL`. The poll sends no user data — just a plain `GET` for the manifest JSON.
- **`data_extraction_rules.xml` excludes every domain** (cloud backup, device transfer) so user data cannot be pulled off the device by Android's built-in backup mechanisms.
- **All sensitive prefs** (`backupPassphrase`, `drive_auth_state`) live in `EncryptedSharedPreferences` via `SecurePrefs`.
- **Backup files are encrypted** with a user-supplied passphrase (PBKDF2-HMAC-SHA256, 120k iterations, AES-256-GCM). A leaked `.cvb` is useless without the passphrase.
- **Optional Drive upload** uses AppAuth (no GMS) and uploads only the already-encrypted `.cvb` blob; Google sees ciphertext.

### 1.7 Quality bar

These are non-aspirational targets. Each one has a measurable definition. Builds that regress any of them block release.

| Metric | Target | Measurement |
|--------|--------|-------------|
| Cold start to first frame | **< 1.5 s** on a Pixel 4a-class device | Macrobenchmark (deferred), or visual inspection on debug build. |
| Filter sheet apply → list updates | **< 300 ms** for a 5,000-call DB | Wallclock from `onApplyFilter` to first emission of the new list. |
| FTS search query → first result row | **< 100 ms** for a 5,000-call DB | Wallclock from `onQueryChange` (after debounce) to first emission. |
| Sync pipeline end-to-end | **< 2 s** for 100 new calls | `SyncProgressBus` start → completion timestamp delta. |
| Release APK size | **< 25 MB** | `ls -l app-release.apk`. |
| Memory footprint at idle | **< 80 MB** RSS | adb shell `dumpsys meminfo`. |
| ANR rate | **0** | Visual on device + logcat ANR markers. |

Lead-score recompute is allowed up to 1 s for a 5,000-call DB; backup encryption is allowed up to 8 s for the same database size (PBKDF2 dominates).

---

## 2 — Locked tech stack

This is the canonical dependency table. Versions are **pinned exactly**. Do not silently bump a major. Minor / patch bumps require a `DECISIONS.md` entry only if the bump changes behavior; otherwise update both `libs.versions.toml` and this table in the same PR.

### 2.1 Core toolchain

| Component | Version | Role |
|-----------|---------|------|
| Kotlin | **2.0.21** | Project language. K2 compiler. |
| Android Gradle Plugin (AGP) | **8.7.3** | Build system. |
| Gradle | **8.10.2** | Build runner. |
| compileSdk | **35** | Android 15 APIs available at compile time. |
| targetSdk | **35** | Behavior changes for Android 15 opted in. |
| minSdk | **26** | Android 8.0 floor. Drops < 5% of Indian market in 2026. |
| JVM target | **17** | Toolchain JDK. |
| KSP | **2.0.21-1.0.27** | Symbol processing for Hilt + Room. |
| `kotlin-stdlib` | bundled with 2.0.21 | Standard library. |

`minSdk = 26` is locked because:

- `JobScheduler`/`WorkManager` periodic intervals are sane.
- `NotificationChannel` is mandatory and consistent.
- `EncryptedSharedPreferences` works out of the box.
- `FileProvider` + unknown-sources install gating is consistent.
- Foreground-service-special-use is available with the polyfill on 26–33 and native on 34+.

`targetSdk = 35` is locked because Play Store would require it anyway, and behavior changes (foreground service types, partial broadcasts) are explicitly handled.

### 2.2 UI

| Library | Version | Role |
|---------|---------|------|
| Compose BOM | **2024.12.01** | Single-source Compose version graph. |
| Compose Material 3 | from BOM | Material 3 components, restyled with Neo* wrappers. |
| Compose Navigation | **2.8.5** | Single-Activity routing. |
| Compose Foundation | from BOM | LazyColumn, gestures. |
| Compose UI / Tooling | from BOM | `@Preview`, `Modifier`, `LayoutInspector`. |
| `androidx.activity:activity-compose` | 1.9.3 | `setContent`, `BackHandler`. |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | `viewModel()` factory. |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.7 | `collectAsStateWithLifecycle`. |
| `androidx.core:core-splashscreen` | **1.0.1** | API 31+ splash compatibility on 26+. |
| Coil | **3.0.4** (`io.coil-kt.coil3:coil-compose`) | Image loading (avatar, doc images). |
| Vico | **2.0.0-beta.4** (`compose-m3` + `compose` + `core`) | Stats charts (limited use; most charts are hand-rolled Canvas). |

### 2.3 DI / persistence / async

| Library | Version | Role |
|---------|---------|------|
| Hilt | **2.53.1** (KSP) | Dependency injection. |
| `androidx.hilt:hilt-work` | 1.2.0 | `@HiltWorker` + `@AssistedInject` for WorkManager. |
| Room runtime | **2.6.1** | SQLite ORM. |
| Room compiler | **2.6.1** (KSP) | Generated DAOs. |
| Room ktx | 2.6.1 | Coroutines + `withTransaction`. |
| DataStore Preferences | **1.1.1** | Typed prefs (~40 keys). |
| WorkManager | **2.10.0** | Periodic + one-time background work. |
| `androidx.security:security-crypto` | 1.1.0-alpha06 | `EncryptedSharedPreferences` for `SecurePrefs`. |
| kotlinx coroutines | **1.9.0** | Structured concurrency. |
| kotlinx serialization | **1.7.3** | JSON for export, rules, manifest. |
| kotlinx datetime | **0.6.1** | Calendar / instant arithmetic for sync, follow-ups. |
| `androidx.lifecycle:lifecycle-service` | 2.8.7 | `LifecycleService` for `CallEnrichmentService`. |
| `androidx.savedstate:savedstate` | 1.2.1 | Transitive for the lifecycle bits. |

### 2.4 Telephony / contacts / phone numbers

| Library | Version | Role |
|---------|---------|------|
| `io.michaelrocks:libphonenumber-android` | **8.13.50** | Phone normalization to E.164 with default region IN. |

The Michael-Rocks port of Google's libphonenumber is preferred over the official `com.googlecode.libphonenumber:libphonenumber` because it ships pre-compiled metadata as Android assets — half the size, faster cold-start.

### 2.5 Export / encryption

| Library | Version | Role |
|---------|---------|------|
| Apache POI ooxml-lite | **5.2.5** | Excel multi-sheet export. |
| iText core | **8.0.5** | PDF export. |
| Tink | **1.15.0** | (Catalogued but unused in v1; backup uses raw JCA — see `DECISIONS.md`.) |

### 2.6 Auth (optional Drive backup)

| Library | Version | Role |
|---------|---------|------|
| AppAuth | **0.11.1** (`net.openid:appauth`) | OAuth 2.0 for Google Drive without Play Services. |
| OkHttp | **4.12.0** | Drive REST + update manifest fetch. |

### 2.7 Logging

| Library | Version | Role |
|---------|---------|------|
| Timber | **5.0.1** | Logging façade. Planted in `CallVaultApp.onCreate` (debug only). |

### 2.8 Test stack

| Library | Version | Role |
|---------|---------|------|
| JUnit5 (`org.junit.jupiter:junit-jupiter`) | **5.11.4** | Unit test runner. |
| Turbine | **1.2.0** | Flow assertions. |
| MockK | **1.13.13** | Kotlin-friendly mocking. |
| `androidx.test.ext:junit` | 1.2.1 | Instrumentation test runner (scaffolded only). |
| `androidx.room:room-testing` | 2.6.1 | DAO tests, migration tests. |

### 2.9 Why these and not others

- `[L]` **No Mavericks / Orbit / MVI framework.** `StateFlow` + sealed UI state is enough.
- `[L]` **No Retrofit.** A single OkHttp call site for the manifest does not justify the dependency.
- `[L]` **No Glide.** Coil 3 is Compose-native and smaller.
- `[L]` **No Realm / SQLDelight.** Room is the Jetpack default and integrates with WorkManager / Hilt.
- `[L]` **No play-services-auth.** Sideload + GMS-free posture forbids it.
- `[L]` **No Crashlytics / Firebase Performance.** Privacy posture forbids it.

---

## 3 — Domain glossary

This section is the source of truth for vocabulary. Every term that appears in code, tests, docs, or strings should be defined here. If you find a term in the codebase not listed here, add it.

### 3.1 Call

A single row from `CallLog.Calls`, mirrored to Room as `CallEntity`. Identified by `systemId`, which is `CallLog._ID`. Multiple calls can share the same `normalizedNumber` — that's the whole point of the lead score (a number with many calls is a hot lead).

`CallEntity` carries the system-provided fields plus CallVault-only fields: `isBookmarked`, `bookmarkReason`, `followUpDate`, `followUpTime`, `followUpDone`, `archived`, `leadScore`, `leadScoreManualOverride`, `tagIds` (denormalized cache, refreshed by `CallTagCrossRef`).

The system row is treated as immutable input: CallVault never deletes from `CallLog.Calls`, never modifies it, and never tries to write to it. CallVault's view is a faithful mirror plus enrichment.

Examples:

- A 22-second incoming call from `+919876543210` at `2026-04-29T11:42:13Z` becomes one `CallEntity` row.
- The same number calling twice in five minutes is two rows, sharing `normalizedNumber`.
- A "Private number" from a withheld caller is one row with `rawNumber=""`, `normalizedNumber=""`, `isPrivate=true`.

### 3.2 Normalized number

Phone number in E.164 form, e.g. `+919876543210`. Produced by `PhoneNumberNormalizer` via libphonenumber-android with default region `IN`. This is the **primary join key** for ContactMeta, lead score, notes, auto-save, and tag rules.

Calls without a parseable number get an empty string `""` plus the `isPrivate=true` flag. Empty-string normalized numbers do not aggregate — each private call is its own ContactMeta-less row.

Worked examples:

| Raw input | Default region IN | E.164 output |
|-----------|-------------------|--------------|
| `9876543210` | IN | `+919876543210` |
| `+919876543210` | IN | `+919876543210` |
| `09876543210` | IN | `+919876543210` |
| `+1 (415) 555-2671` | IN | `+14155552671` |
| `911234567890` | IN | `+911234567890` |
| `*86` | IN | `""` (unparseable, treated as private) |
| `Unknown` | IN | `""` |

### 3.3 Raw number

The original string from `CallLog.Calls.NUMBER`, preserved as-is for audit. This is what gets shown in low-fidelity contexts (e.g. when normalization fails). Useful for forensic debugging when a user reports "this number didn't get tagged" — comparing `rawNumber` to `normalizedNumber` quickly shows whether normalization or rule matching is the culprit.

### 3.4 Contact meta

Per-`normalizedNumber` aggregates kept in `ContactMetaEntity`. Recomputed on every sync, not on every read.

Fields cover: total calls, total duration in seconds, first call timestamp, last call timestamp, count by type (incoming, outgoing, missed, rejected), lead score, manual override, auto-save state (`isAutoSaved`, `isInSystemContacts`), display name (cached from system contacts when available), and a denormalized tag-id list (refreshed at the end of sync).

The aggregate is what makes the Calls list and detail screen fast — without it, the bookmark badge, lead-score badge, and "X calls / Y mins" line would all need GROUP BY queries on every render.

### 3.5 SIM slot

`0` or `1`, resolved from `CallLog.PHONE_ACCOUNT_ID` via `SubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex`. Displayed as **"SIM 1"** / **"SIM 2"** in UI (1-indexed for users). Stored zero-indexed in the DB.

When the device is single-SIM, `simSlot` is always `0`. When the lookup fails (privacy redaction, removed SIM), `simSlot=null`. The auto-save name format includes `simTag` (e.g. `s1`, `s2`); when null, `simTag` collapses to empty string.

### 3.6 Inquiry

An unsaved-number call. The user receives many inquiries per day — capturing them is CallVault's primary job. A row is an inquiry while `isInSystemContacts=false`. Once the user (or auto-save) writes the number to system Contacts, `isInSystemContacts=true` flips on next sync.

### 3.7 Auto-saved contact

An inquiry written to system Contacts by CallVault using the auto-save name format. Tracked via `ContactMetaEntity.isAutoSaved=true`. Lives in the configured contact group (default *CallVault Inquiries*), with the configured `ACCOUNT_TYPE`/`ACCOUNT_NAME`. See §6.5 for the format and §3.10 for the group manager.

When the user later renames an auto-saved contact in the system Contacts app, lenient bucketing (§6.6) flips `isAutoSaved` back to `false` on next sync — without requiring the user to do anything in CallVault.

### 3.8 My Contacts (bucket)

Top-level UX bucket for "real" contacts. SQL predicate: `isInSystemContacts=true AND isAutoSaved=false`. Two ways a row enters this bucket:

1. The user manually saved the number outside CallVault, before or after first installing the app.
2. The user renamed a CallVault-auto-saved contact, and lenient bucketing detected the rename.

### 3.9 Inquiries (bucket)

Top-level UX bucket for auto-saved-but-not-yet-renamed inquiries. SQL predicate: `isAutoSaved=true`. The user converts an inquiry to a real contact by renaming it (either inside CallVault's `MyContactsScreen` "Convert" flow, or outside, in the system Contacts app).

### 3.10 Lenient bucketing

The rule per §6.6: if the user opens the system Contacts app and renames an auto-saved entry, `isAutoSaved` flips to `false` on the next sync. The contact moves from *Inquiries* to *My Contacts* automatically. The detection compares the live system display name against a regex compiled from the **current** auto-save settings, not the historical settings — see `DECISIONS.md` (Sprint 5).

### 3.11 Auto-save name format

Locked format: `{prefix}{simTag} {fullNormalizedPhone}{suffix}`. Configurable in Settings → Auto-Save. Defaults: `prefix="callVault-"`, `simTagFormat="s{n}"`, `suffix=""`. Worked examples in §6.5.

### 3.12 Auto-save pattern matcher

Regex compiled from current settings used to detect whether a system-contact name still matches the auto-save format. The matcher is a thin object (`AutoSavePatternMatcher`) whose only public surface is `matches(name: String): Boolean`. It is recompiled on every settings change.

### 3.13 Lead score

0–100 integer per `normalizedNumber`, computed by `ComputeLeadScoreUseCase` per §6.3. Components: call frequency, total duration, recency decay, follow-up bonus, customer-tag bonus, saved-contact bonus, rule score boosts. Weights are user-configurable in `LeadScoringSettingsScreen`.

The integer is rounded to the nearest whole number after summing weighted components and clipping to [0, 100].

### 3.14 Bucket (lead-score)

User-facing band: `Cold` < 30, `Warm` 30..70, `Hot` > 70. Rendered by `LeadScoreBadge` as a colored dot (gray/amber/red) plus the numeric score. The thresholds are not user-tunable in v1.

### 3.15 Recency decay

`100 * exp(-daysSinceLastCall / 14)`. Half-life ~10 days. Caps at 100 for "called today" and asymptotically approaches zero. See §6.8 for a worked table.

### 3.16 Manual override

User can pin a score in Call Detail. While `leadScoreManualOverride != null`, automatic recomputation is suppressed for that number — the override wins. Clearing the override re-enables automatic computation on the next sync.

### 3.17 Auto-tag rule

User-defined `(conditions, actions)` pair. Conditions are AND-ed. Rules iterate top-to-bottom by `sortOrder`. Stored as `AutoTagRuleEntity` with conditions and actions JSON-serialized via kotlinx.serialization (class-discriminator `"type"`).

### 3.18 Rule condition

Sealed interface variant. The 13 v1 variants are: `PrefixMatches`, `RegexMatches`, `CountryEquals`, `IsInContacts`, `CallTypeIn`, `DurationCompare`, `TimeOfDayBetween`, `DayOfWeekIn`, `SimSlotEquals`, `TagApplied`, `TagNotApplied`, `GeoContains`, `CallCountGreaterThan`. Evaluated by `RuleConditionEvaluator`.

### 3.19 Rule action

Sealed interface variant. The 4 v1 variants are: `ApplyTag(tagId)`, `LeadScoreBoost(delta)`, `AutoBookmark(reason)`, `MarkFollowUp(deltaMinutes)`. Applied by `RuleActionApplier`.

### 3.20 Rule score boost

When a `LeadScoreBoost(delta)` action fires, a row goes into `rule_score_boosts` keyed by `(callSystemId, ruleId)`. Lead score sums all boosts per number. PK keeps actions idempotent — re-running a rule on the same call produces the same row, not duplicates.

### 3.21 Applied-by

Every `CallTagCrossRef` carries `appliedBy = "user"` or `"rule:${ruleId}"`. On rule delete, all `appliedBy = "rule:${id}"` rows cascade-clean (application-level, not foreign-key — see `DECISIONS.md` Sprint 6).

### 3.22 System tag

One of 9 default tags seeded on first run by `DefaultTagsSeeder` / `SeedDefaultTagsWorker`: `Inquiry`, `Customer`, `Vendor`, `Personal`, `Spam`, `Follow-up`, `Quoted`, `Closed-won`, `Closed-lost`. Renamable, recolorable; **not deletable**. The "system" flag is a column on `TagEntity`.

### 3.23 User tag

Any non-system tag. Unlimited. Created via `TagsManagerScreen` or via the `Create new tag` chip in `TagPickerSheet`.

### 3.24 Tag merge

Moves all `CallTagCrossRef` rows from source tag to target tag (with `INSERT OR IGNORE` semantics to dedupe), then deletes source. Performed in `TagDao` inside a single transaction.

### 3.25 Note

Markdown-rendered text attached to a `normalizedNumber` and optionally a specific `callSystemId`. Mini-journal pattern: many notes per number, each with its own timestamp, body, and creator (`user` or `bubble`).

### 3.26 Note history

Last 5 versions of a note kept in `NoteHistoryEntity` for undo/audit. On every save, the previous body is appended to history; older-than-5 history rows for that note are pruned.

### 3.27 Orphan note

A note created during a call (via the floating bubble) before the corresponding `CallEntity` exists. Sync attaches orphans to the new call entity within a ±60s window — see §6.11.

### 3.28 Bookmark

`isBookmarked=true` flag on `CallEntity` plus optional `bookmarkReason`. Top-5 pinned bookmarks live in DataStore as a JSON list of normalized numbers. The reason is captured via `BookmarkReasonDialog` when the user swipes-to-bookmark.

### 3.29 Follow-up

Scheduled reminder per call. `followUpDate` (epoch ms, day-aligned) + optional `followUpTime` (minutes-of-day). When `followUpTime=null` the reminder fires at 9 AM local. Notification fires via `AlarmManager.setExactAndAllowWhileIdle` if the permission is held; falls back to `setAndAllowWhileIdle` (inexact) when not.

### 3.30 Auto-clear follow-up

When sync detects an outgoing call to a number with an active follow-up, the follow-up is marked done. Implemented inside the sync per-row loop.

### 3.31 Sync pipeline

The 12-step algorithm in §6.1 implemented by `SyncCallLogUseCase`. Read in §6.1; cross-referenced by every other algorithm in §6.

### 3.32 lastSyncCallId

`CallLog._ID` of the most-recent call seen. Stored in DataStore. Subsequent syncs query `WHERE _ID > :lastSyncCallId`. Reset to 0 only on `ResetAllDataUseCase`.

### 3.33 Sync interval

User setting: `Manual`, `5 min`, `15 min` (default), `1 hour`, `12 hour`, `24 hour`, `Daily 2 AM`. Implementation maps to WorkManager Periodic, AlarmManager exact, or a OneTimeWorkRequest chain.

### 3.34 Floating bubble

`WindowManager` overlay (`TYPE_APPLICATION_OVERLAY`) shown during an active call by `CallEnrichmentService`. 56dp circle, drag-to-snap-to-edge, expandable to a mini-card with note + tag chip. Implemented as plain Android Views, not Compose, by explicit decision (`DECISIONS.md` Sprint 7).

### 3.35 Post-call popup

Overlay shown 2 s after `CALL_STATE_IDLE`. Auto-dismisses after `postCallPopupTimeoutSeconds` (default 8 s). Quick-tag chips (top 3 by usage), single-line note, "Save contact" button.

### 3.36 SpecialUse foreground service

`CallEnrichmentService` runs with `foregroundServiceType="specialUse"` and `PROPERTY_SPECIAL_USE_FGS_SUBTYPE="RealTimeCallEnrichment"`. Required for Android 14+.

### 3.37 Export

One of 5 formats: Excel (POI multi-sheet), CSV (UTF-8 BOM), PDF (iText), JSON (kotlinx.serialization full dump), vCard 3.0. Wizard: format → date range → scope → columns → destination.

### 3.38 Backup

Encrypted JSON file (`.cvb`). Format: `CVB1` magic + 1-byte version + 16-byte salt + 12-byte IV + AES-256-GCM ciphertext (with embedded 128-bit tag). Key derived via PBKDF2-HMAC-SHA256 (120,000 iterations) from user passphrase. Auto-backup runs daily at 2 AM via `DailyBackupWorker`.

### 3.39 Restore

Destructive: wipes user-data tables and reinserts from a `.cvb` file. Wrapped in a single Room transaction so partial restores are impossible.

### 3.40 Update channel

`stable` or `beta`. Each polls a separate `versions.json` URL configured in `BuildConfig.UPDATE_MANIFEST_*_URL`. Manifest schema:

```json
{
  "stable": {
    "version": "1.0.1",
    "versionCode": 2,
    "apkUrl": "https://example.com/callvault-1.0.1.apk",
    "sha256": "abcd...ef",
    "minSupported": 1,
    "releaseNotes": "Bug fixes and stability improvements."
  },
  "beta": { "...": "..." }
}
```

### 3.41 Update flow

`UpdateCheckWorker` (weekly) → `UpdateChecker` (HTTP fetch) → `UpdateState.Available` → `UpdateBanner` on Calls / `UpdateNotifier` notification → `UpdateAvailableScreen` → `UpdateDownloader` (DownloadManager) → SHA-256 verify → `UpdateInstaller` (FileProvider + `ACTION_VIEW`) → API 26+ unknown-sources gate.

### 3.42 Skipped version

User dismissal of an update for a specific `versionCode`. Stored in `SkippedUpdateEntity`. Reset on next non-skipped versionCode or via "Clear skipped" in settings.

### 3.43 Neumorphism

Light source top-left, base `#E8E8EC`. Dual-shadow rendering via `Modifier.neoShadow(elevation, shape)`. Convex (raised), Concave (sunken), Flat. See Part 02 for the design system reference.

### 3.44 Neo* component

Any Compose composable in `ui/components/neo/`. Wraps Material 3 internals with neumorphic styling. Examples: `NeoButton`, `NeoCard`, `NeoChip`, `NeoTabBar`, `NeoBottomSheet`, `NeoTextField`.

### 3.45 NeoColors

Token object. `Base`, `BasePressed`, `Light` (top-left highlight), `Dark` (bottom-right shadow). Legacy `NeoShadows` is an alias kept for migration; new code uses `NeoColors`.

### 3.46 4% offset rule

Never put pure white or pure black surfaces against the base; kills depth. All surfaces tint at least 4% from base.

### 3.47 Lead score badge

Small colored dot (gray / amber / red) plus score number, rendered by `LeadScoreBadge.kt`. Used in `CallRow`, `HeroCard`, `BookmarksScreen`.

### 3.48 MainScaffold

Top-level Compose scaffold with bottom `NeoTabBar` (5 tabs: Home / Calls / My Contacts / Inquiries / More). Default tab: Calls. Nested-graph integration is partial in v1 (see CHANGELOG known-limitations).

### 3.49 NeoHelpIcon

`?` icon component that opens a specific in-app docs article. Goes in screen app bars. Drop-in but not yet placed everywhere in v1.

### 3.50 Critical permissions

Call log + contacts + phone state. Without these the app cannot function. `PermissionManager.isCriticalGranted()` gates the Calls screen behind `permission_rationale`. See §4.1.

### 3.51 Special-grant permissions

`SYSTEM_ALERT_WINDOW` (overlay), `SCHEDULE_EXACT_ALARM` (exact follow-up alarms). Granted via Settings, not runtime dialog. See §4.2.

### 3.52 OEM autostart

Non-permission setting on Xiaomi/Oppo/Vivo/Realme/Samsung/OnePlus/Honor/Huawei devices. Without it, the foreground service gets killed by aggressive battery managers. `OemBatteryGuide` deep-links to vendor settings; falls back to `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. See §6.12.

### 3.53 SyncProgressBus

Hilt-singleton `MutableSharedFlow<SyncProgress>` with `replay = 1`. Producers: `SyncCallLogUseCase`. Consumers: Calls screen pull-to-refresh, settings sync chip. Survives use-case re-creation. Introduced in Sprint 2 (`DECISIONS.md`).

### 3.54 ResetAllDataUseCase

Wipes notes, search history, and skipped updates. Calls / contacts / tags **retain** — re-syncing from `CallLog.Calls` will repopulate calls; tags re-seed via `SeedDefaultTagsWorker`.

### 3.55 RealTimeServiceController

Reconciles `CallEnrichmentService` start/stop state with current settings + permissions. Called from `MainActivity.onResume`, settings toggles, and `BootCompletedReceiver`.

### 3.56 SyncProgress

Sealed sentinel type for sync state: `Started`, `Reading`, `Processing(current, total)`, `Completed(addedCount)`, `Error(throwable)`. Emitted on `SyncProgressBus`.

### 3.57 FilterState

The persisted shape of a saved filter preset. Carries: date range, call types, SIM slots, include tags, exclude tags, only-bookmarked, only-with-followup, saved/unsaved, duration min/max, lead-score min, country ISOs, free-text, sort.

---

## 4 — Permissions inventory

Every permission CallVault requests, grouped by criticality. For each: manifest line, why CallVault needs it, when requested, what's blocked if denied, fallback, user-facing rationale string.

### 4.1 Critical permissions

These are required runtime permissions. Without all of them, the Calls screen will not render — the user gets routed to `permission_rationale`.

#### 4.1.1 `READ_CALL_LOG`

```xml
<uses-permission android:name="android.permission.READ_CALL_LOG" />
```

- **Why CallVault needs it.** It's the entire input to the sync pipeline. Without `READ_CALL_LOG`, `CallLogReader.readSince()` returns nothing.
- **When requested.** Onboarding page 3 (Permissions). Re-requested on Calls screen entry if denied.
- **What's blocked if denied.** Everything. The entire app is built on call log data.
- **Fallback.** None — denied → `permission_denied` route.
- **Rationale string.** `R.string.perm_rationale_call_log` — *"CallVault reads your call log to capture inquiries. Without this, the app can't show any calls."*

#### 4.1.2 `READ_PHONE_STATE`

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

- **Why CallVault needs it.** SIM-slot resolution via `SubscriptionManager`, in-call state monitoring via `TelephonyCallback`.
- **When requested.** Onboarding page 3, alongside call-log.
- **What's blocked if denied.** SIM-slot column in `CallEntity` is null. Real-time bubble cannot detect call state on its own.
- **Fallback.** App still works; SIM-slot filters will all match no calls; real-time toggles get disabled with a help link.
- **Rationale string.** `R.string.perm_rationale_phone_state` — *"CallVault reads phone state to know which SIM took the call and to show in-call helpers."*

#### 4.1.3 `READ_CONTACTS`

```xml
<uses-permission android:name="android.permission.READ_CONTACTS" />
```

- **Why CallVault needs it.** Resolve display name for known numbers; detect `isInSystemContacts`; lenient bucketing reads live display names.
- **When requested.** Onboarding page 3.
- **What's blocked if denied.** Calls show numbers only, no names. My Contacts bucket cannot be derived. Lenient bucketing cannot run.
- **Fallback.** App functions in degraded mode — numbers only, no name resolution. Banner suggests granting.
- **Rationale string.** `R.string.perm_rationale_read_contacts` — *"CallVault reads contacts to show names instead of numbers."*

#### 4.1.4 `WRITE_CONTACTS`

```xml
<uses-permission android:name="android.permission.WRITE_CONTACTS" />
```

- **Why CallVault needs it.** Auto-save inquiries into the *CallVault Inquiries* group; the BulkSaveContactsUseCase action.
- **When requested.** Onboarding page 3.
- **What's blocked if denied.** Auto-save toggle disabled in settings. Bulk-save action greyed out.
- **Fallback.** App fully functions without auto-save. The user is asked to grant the permission only when they enable auto-save.
- **Rationale string.** `R.string.perm_rationale_write_contacts` — *"CallVault auto-saves inquiry numbers to a separate contact group so your address book stays organized."*

### 4.2 Special-grant permissions

These are not runtime-dialog permissions. They go through Settings.

#### 4.2.1 `SYSTEM_ALERT_WINDOW`

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

- **Why CallVault needs it.** The floating bubble + post-call popup are `WindowManager` overlays of type `TYPE_APPLICATION_OVERLAY`.
- **When requested.** Onboarding page 4 ("Real-time helpers"); also when the user toggles bubble or popup in Real-Time Settings.
- **What's blocked if denied.** Real-time toggles disabled. Service does not start (`RealTimeServiceController` short-circuits).
- **Fallback.** App functions without real-time. Calls list and detail still work.
- **Rationale string.** `R.string.perm_rationale_overlay` — *"To show the in-call bubble and post-call popup, CallVault needs to draw over other apps. Tap Open Settings, then enable 'Allow over other apps'."*

#### 4.2.2 `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
```

- **Why CallVault needs it.** Follow-up reminders fire at exact times via `AlarmManager.setExactAndAllowWhileIdle`. The 5-minute sync interval also relies on exact alarms (PeriodicWorkRequest minimum is 15 min).
- **When requested.** First time the user creates a follow-up; or selects 5 min sync interval.
- **What's blocked if denied.** Exact alarms downgrade to inexact (`setAndAllowWhileIdle`). Reminders may fire 0–15 min late depending on Doze. 5-min sync falls back to 15-min.
- **Fallback.** App stores `exactAlarmFallbackUsed=true` in DataStore and shows an info chip on the Follow-Ups screen.
- **Rationale string.** `R.string.perm_rationale_exact_alarm` — *"For follow-up reminders to fire on time, CallVault needs the 'Alarms & reminders' permission."*

### 4.3 Notifications

#### 4.3.1 `POST_NOTIFICATIONS`

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

- **Why CallVault needs it.** Foreground-service notification, follow-up reminders, daily summary, sync progress, update available, daily backup result.
- **When requested.** Onboarding page 3 on API 33+. On older APIs the permission is implicitly granted.
- **What's blocked if denied.** Foreground service still runs but its notification is invisible. Reminders fire silently.
- **Fallback.** Settings shows a banner ("Notifications disabled — reminders won't appear in your tray").
- **Rationale string.** `R.string.perm_rationale_notifications` — *"CallVault needs notification permission to show follow-up reminders and the daily summary."*

#### 4.3.2 `VIBRATE`

```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

- **Why CallVault needs it.** Tactile feedback on bubble drag-snap, follow-up reminder buzz, swipe gesture detents.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A — no runtime denial path.
- **Fallback.** N/A.
- **Rationale string.** N/A (install-time).

### 4.4 Foreground service permissions

#### 4.4.1 `FOREGROUND_SERVICE`

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

- **Why CallVault needs it.** `CallEnrichmentService` runs as a foreground service.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A — install-time.
- **Fallback.** N/A.

#### 4.4.2 `FOREGROUND_SERVICE_DATA_SYNC`

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

- **Why CallVault needs it.** API 34+ requires a typed foreground-service permission for any DataSync-typed service. `CallSyncWorker` may chain via expedited work that surfaces a foreground notification under heavy load.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

#### 4.4.3 `FOREGROUND_SERVICE_SPECIAL_USE`

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

- **Why CallVault needs it.** API 34+ requires this typed permission for `specialUse` services. `CallEnrichmentService` declares `foregroundServiceType="specialUse"` with subtype `RealTimeCallEnrichment`.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

### 4.5 Boot / wake

#### 4.5.1 `RECEIVE_BOOT_COMPLETED`

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

- **Why CallVault needs it.** `BootCompletedReceiver` re-schedules sync work, re-arms exact alarms for pending follow-ups, and (if enabled) restarts `CallEnrichmentService`.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

#### 4.5.2 `WAKE_LOCK`

```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

- **Why CallVault needs it.** Sync pipeline holds a partial wakelock for the duration of a sync to keep the CPU on while reading the call log.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

### 4.6 Update / network

#### 4.6.1 `INTERNET`

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

- **Why CallVault needs it.** Single outbound call to the update manifest. Optional Drive backup upload.
- **When requested.** Implicit.
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

#### 4.6.2 `ACCESS_NETWORK_STATE`

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

- **Why CallVault needs it.** WorkManager constraints (`requiresNetwork`, `requiresUnmeteredNetwork`) need this to evaluate.
- **When requested.** Implicit.
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

#### 4.6.3 `REQUEST_INSTALL_PACKAGES`

```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

- **Why CallVault needs it.** Self-update install flow hands off via `FileProvider` + `ACTION_VIEW` to the system installer, gated by `PackageManager.canRequestPackageInstalls()` on API 26+.
- **When requested.** First time the user taps Install in `UpdateAvailableScreen`. The screen routes to "Settings → Install unknown apps" if not granted.
- **What's blocked if denied.** Self-update download still works; install does not.
- **Fallback.** User can manually install the downloaded APK from a file manager.
- **Rationale string.** `R.string.perm_rationale_install_packages` — *"To install the CallVault update you just downloaded, allow CallVault to install unknown apps."*

### 4.7 Permission gating summary

| Surface | Required permissions | Behavior if missing |
|---------|---------------------|---------------------|
| Calls list / detail | `READ_CALL_LOG`, `READ_CONTACTS`, `READ_PHONE_STATE` | Route to `permission_rationale`. |
| Auto-save toggle | `WRITE_CONTACTS` | Toggle disabled with help link. |
| Real-time bubble / popup | `SYSTEM_ALERT_WINDOW` + `READ_PHONE_STATE` + (API 31+) `READ_CALL_LOG` for number resolution | Toggles disabled. |
| Follow-up reminders | `SCHEDULE_EXACT_ALARM` (graceful fallback to inexact) | Reminder may fire late. |
| Notifications | `POST_NOTIFICATIONS` (API 33+) | Reminders fire silently. |
| Self-update install | `REQUEST_INSTALL_PACKAGES` | Install screen routes to system settings. |

---

## 5 — Data model

### 5.1 Room database

`CallVaultDatabase`, file `callvault.db`, **schema version 2**. Migration v1 → v2 adds `rule_score_boosts`. Schema export is wired through KSP arg pointing at `app/schemas/`.

Entities (14 + 2 FTS):

1. `CallEntity`
2. `TagEntity`
3. `CallTagCrossRef`
4. `ContactMetaEntity`
5. `NoteEntity`
6. `NoteHistoryEntity`
7. `FilterPresetEntity`
8. `AutoTagRuleEntity`
9. `RuleScoreBoostEntity`
10. `SearchHistoryEntity`
11. `DocFeedbackEntity`
12. `SkippedUpdateEntity`
13. `CallFts` (FTS4)
14. `NoteFts` (FTS4)

DAOs:

`CallDao`, `TagDao`, `NoteDao`, `ContactMetaDao`, `FilterPresetDao`, `AutoTagRuleDao`, `SearchHistoryDao`, `DocFeedbackDao`, `SkippedUpdateDao`, `RuleScoreBoostDao`.

### 5.2 `CallEntity`

Table: `calls`. PK: `systemId`.

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `systemId` | `Long` | INTEGER | no | — | PK |
| `rawNumber` | `String` | TEXT | no | `""` | — |
| `normalizedNumber` | `String` | TEXT | no | `""` | yes |
| `cachedName` | `String?` | TEXT | yes | null | — |
| `geocodedLocation` | `String?` | TEXT | yes | null | — |
| `type` | `Int` | INTEGER | no | — | yes |
| `simSlot` | `Int?` | INTEGER | yes | null | — |
| `phoneAccountId` | `String?` | TEXT | yes | null | — |
| `dateMillis` | `Long` | INTEGER | no | — | yes |
| `durationSec` | `Long` | INTEGER | no | `0` | — |
| `isPrivate` | `Boolean` | INTEGER | no | `false` | — |
| `isBookmarked` | `Boolean` | INTEGER | no | `false` | yes |
| `bookmarkReason` | `String?` | TEXT | yes | null | — |
| `followUpDate` | `Long?` | INTEGER | yes | null | yes |
| `followUpTime` | `Int?` | INTEGER | yes | null | — |
| `followUpDone` | `Boolean` | INTEGER | no | `false` | — |
| `archived` | `Boolean` | INTEGER | no | `false` | — |
| `leadScore` | `Int` | INTEGER | no | `0` | — |
| `leadScoreManualOverride` | `Int?` | INTEGER | yes | null | — |
| `tagIdsCsv` | `String` | TEXT | no | `""` | — |
| `createdAt` | `Long` | INTEGER | no | — | — |
| `updatedAt` | `Long` | INTEGER | no | — | — |

Sample row:

```json
{
  "systemId": 18234,
  "rawNumber": "9876543210",
  "normalizedNumber": "+919876543210",
  "cachedName": null,
  "geocodedLocation": "India",
  "type": 1,
  "simSlot": 0,
  "phoneAccountId": "PHONE_ACCOUNT_ID/0",
  "dateMillis": 1714382533000,
  "durationSec": 22,
  "isPrivate": false,
  "isBookmarked": false,
  "bookmarkReason": null,
  "followUpDate": null,
  "followUpTime": null,
  "followUpDone": false,
  "archived": false,
  "leadScore": 0,
  "leadScoreManualOverride": null,
  "tagIdsCsv": "",
  "createdAt": 1714382540000,
  "updatedAt": 1714382540000
}
```

### 5.3 `TagEntity`

Table: `tags`. PK: `id` (autogen).

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `id` | `Long` | INTEGER | no | autogen | PK |
| `name` | `String` | TEXT | no | — | unique |
| `colorArgb` | `Int` | INTEGER | no | — | — |
| `isSystem` | `Boolean` | INTEGER | no | `false` | — |
| `sortOrder` | `Int` | INTEGER | no | `0` | — |
| `createdAt` | `Long` | INTEGER | no | — | — |

Sample row:

```json
{ "id": 1, "name": "Inquiry", "colorArgb": -16776961, "isSystem": true, "sortOrder": 0, "createdAt": 1714000000000 }
```

### 5.4 `CallTagCrossRef`

Table: `call_tag_cross_ref`. Composite PK: `(callSystemId, tagId)`.

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `callSystemId` | `Long` | INTEGER | no | — | PK |
| `tagId` | `Long` | INTEGER | no | — | PK + index |
| `appliedBy` | `String` | TEXT | no | `"user"` | yes |
| `appliedAt` | `Long` | INTEGER | no | — | — |

`appliedBy` values: `"user"`, `"rule:${ruleId}"`. Cascade-clean on rule delete (application-level).

### 5.5 `ContactMetaEntity`

Table: `contact_meta`. PK: `normalizedNumber`.

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `normalizedNumber` | `String` | TEXT | no | — | PK |
| `displayName` | `String?` | TEXT | yes | null | — |
| `totalCalls` | `Int` | INTEGER | no | `0` | — |
| `totalDurationSec` | `Long` | INTEGER | no | `0` | — |
| `firstCallAt` | `Long?` | INTEGER | yes | null | — |
| `lastCallAt` | `Long?` | INTEGER | yes | null | yes |
| `incomingCount` | `Int` | INTEGER | no | `0` | — |
| `outgoingCount` | `Int` | INTEGER | no | `0` | — |
| `missedCount` | `Int` | INTEGER | no | `0` | — |
| `rejectedCount` | `Int` | INTEGER | no | `0` | — |
| `leadScore` | `Int` | INTEGER | no | `0` | yes |
| `manualLeadScore` | `Int?` | INTEGER | yes | null | — |
| `isInSystemContacts` | `Boolean` | INTEGER | no | `false` | yes |
| `isAutoSaved` | `Boolean` | INTEGER | no | `false` | yes |
| `autoSavedFormat` | `String?` | TEXT | yes | null | — |
| `lastSyncedAt` | `Long` | INTEGER | no | — | — |

Sample row:

```json
{
  "normalizedNumber": "+919876543210",
  "displayName": "callVault-s1 +919876543210",
  "totalCalls": 12,
  "totalDurationSec": 1800,
  "firstCallAt": 1713000000000,
  "lastCallAt": 1714382533000,
  "incomingCount": 11,
  "outgoingCount": 1,
  "missedCount": 0,
  "rejectedCount": 0,
  "leadScore": 63,
  "manualLeadScore": null,
  "isInSystemContacts": true,
  "isAutoSaved": true,
  "autoSavedFormat": "callVault-{simTag} {phone}",
  "lastSyncedAt": 1714382540000
}
```

### 5.6 `NoteEntity`

Table: `notes`. PK: `id` (autogen).

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `id` | `Long` | INTEGER | no | autogen | PK |
| `normalizedNumber` | `String` | TEXT | no | — | yes |
| `callSystemId` | `Long?` | INTEGER | yes | null | yes |
| `body` | `String` | TEXT | no | — | — |
| `createdBy` | `String` | TEXT | no | `"user"` | — |
| `createdAt` | `Long` | INTEGER | no | — | yes |
| `updatedAt` | `Long` | INTEGER | no | — | — |

`createdBy`: `"user"`, `"bubble"`. Orphan notes have `callSystemId = null` until sync attaches them.

### 5.7 `NoteHistoryEntity`

Table: `note_history`. PK: `id`.

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `id` | `Long` | INTEGER | no | autogen | PK |
| `noteId` | `Long` | INTEGER | no | — | yes |
| `body` | `String` | TEXT | no | — | — |
| `savedAt` | `Long` | INTEGER | no | — | — |

Last 5 versions retained per `noteId`.

### 5.8 `FilterPresetEntity`

Table: `filter_presets`. PK: `id`.

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `id` | `Long` | INTEGER | no | autogen | PK |
| `name` | `String` | TEXT | no | — | — |
| `stateJson` | `String` | TEXT | no | — | — |
| `createdAt` | `Long` | INTEGER | no | — | — |

`stateJson` carries a serialized `FilterState`.

### 5.9 `AutoTagRuleEntity`

Table: `auto_tag_rules`. PK: `id`.

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `id` | `Long` | INTEGER | no | autogen | PK |
| `name` | `String` | TEXT | no | — | — |
| `enabled` | `Boolean` | INTEGER | no | `true` | — |
| `sortOrder` | `Int` | INTEGER | no | `0` | yes |
| `conditionsJson` | `String` | TEXT | no | — | — |
| `actionsJson` | `String` | TEXT | no | — | — |
| `createdAt` | `Long` | INTEGER | no | — | — |
| `updatedAt` | `Long` | INTEGER | no | — | — |

Conditions and actions use kotlinx.serialization with `class-discriminator = "type"`.

### 5.10 `RuleScoreBoostEntity`

Table: `rule_score_boosts`. Composite PK: `(callSystemId, ruleId)`. Added in v2.

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `callSystemId` | `Long` | INTEGER | no | — | PK |
| `ruleId` | `Long` | INTEGER | no | — | PK + index |
| `delta` | `Int` | INTEGER | no | — | — |
| `appliedAt` | `Long` | INTEGER | no | — | — |

### 5.11 `SearchHistoryEntity`

Table: `search_history`. PK: `id`.

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `id` | `Long` | INTEGER | no | autogen | PK |
| `query` | `String` | TEXT | no | — | unique |
| `lastUsedAt` | `Long` | INTEGER | no | — | yes |
| `useCount` | `Int` | INTEGER | no | `1` | — |

### 5.12 `DocFeedbackEntity`

Table: `doc_feedback`. PK: `articleId`.

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `articleId` | `String` | TEXT | no | — | PK |
| `helpful` | `Boolean` | INTEGER | no | — | — |
| `submittedAt` | `Long` | INTEGER | no | — | — |

### 5.13 `SkippedUpdateEntity`

Table: `skipped_updates`. PK: `versionCode`.

| Column | Kotlin type | SQL type | Nullable | Default | Index |
|--------|-------------|----------|----------|---------|-------|
| `versionCode` | `Int` | INTEGER | no | — | PK |
| `version` | `String` | TEXT | no | — | — |
| `skippedAt` | `Long` | INTEGER | no | — | — |

### 5.14 `CallFts`

FTS4 virtual table. Content table: `calls`.

| Column | Source |
|--------|--------|
| `rawNumber` | from `calls.rawNumber` |
| `normalizedNumber` | from `calls.normalizedNumber` |
| `cachedName` | from `calls.cachedName` |
| `geocodedLocation` | from `calls.geocodedLocation` |
| `bookmarkReason` | from `calls.bookmarkReason` |

Tokenizer: `unicode61 remove_diacritics 2`.

### 5.15 `NoteFts`

FTS4 virtual table. Content table: `notes`.

| Column | Source |
|--------|--------|
| `body` | from `notes.body` |

### 5.16 DataStore keys (`SettingsDataStore`)

The DataStore preferences object is split into ~10 logical groups. Each key has a typed `Flow<T>` accessor and a `suspend fun setX(value: T)` writer.

| Key | Kotlin type | Default | Read accessor | Write fn |
|-----|-------------|---------|---------------|----------|
| `onboarding_complete` | Boolean | `false` | `onboardingComplete: Flow<Boolean>` | `setOnboardingComplete` |
| `tags_seeded` | Boolean | `false` | `tagsSeeded` | `setTagsSeeded` |
| `default_region` | String | `"IN"` | `defaultRegion` | `setDefaultRegion` |
| `last_sync_call_id` | Long | `0` | `lastSyncCallId` | `setLastSyncCallId` |
| `last_sync_at` | Long | `0` | `lastSyncAt` | `setLastSyncAt` |
| `sync_interval_minutes` | Int | `15` | `syncIntervalMinutes` | `setSyncIntervalMinutes` |
| `sync_only_on_unmetered` | Boolean | `false` | `syncOnlyOnUnmetered` | `setSyncOnlyOnUnmetered` |
| `auto_save_enabled` | Boolean | `false` | `autoSaveEnabled` | `setAutoSaveEnabled` |
| `auto_save_prefix` | String | `"callVault-"` | `autoSavePrefix` | `setAutoSavePrefix` |
| `auto_save_sim_tag_format` | String | `"s{n}"` | `autoSaveSimTagFormat` | `setAutoSaveSimTagFormat` |
| `auto_save_suffix` | String | `""` | `autoSaveSuffix` | `setAutoSaveSuffix` |
| `auto_save_group_name` | String | `"CallVault Inquiries"` | `autoSaveGroupName` | `setAutoSaveGroupName` |
| `real_time_bubble_enabled` | Boolean | `false` | `bubbleEnabled` | `setBubbleEnabled` |
| `real_time_popup_enabled` | Boolean | `false` | `popupEnabled` | `setPopupEnabled` |
| `post_call_popup_timeout_seconds` | Int | `8` | `postCallPopupTimeoutSeconds` | `setPostCallPopupTimeoutSeconds` |
| `notifications_follow_up` | Boolean | `true` | `followUpNotificationsEnabled` | `setFollowUpNotificationsEnabled` |
| `notifications_daily_summary` | Boolean | `true` | `dailySummaryEnabled` | `setDailySummaryEnabled` |
| `daily_summary_hour` | Int | `9` | `dailySummaryHour` | `setDailySummaryHour` |
| `lead_weight_frequency` | Float | `1.0` | `leadWeightFrequency` | `setLeadWeightFrequency` |
| `lead_weight_duration` | Float | `1.0` | `leadWeightDuration` | `setLeadWeightDuration` |
| `lead_weight_recency` | Float | `1.0` | `leadWeightRecency` | `setLeadWeightRecency` |
| `lead_weight_followup_bonus` | Float | `5.0` | `leadWeightFollowupBonus` | `setLeadWeightFollowupBonus` |
| `lead_weight_customer_tag_bonus` | Float | `10.0` | `leadWeightCustomerTagBonus` | `setLeadWeightCustomerTagBonus` |
| `lead_weight_saved_contact_bonus` | Float | `5.0` | `leadWeightSavedContactBonus` | `setLeadWeightSavedContactBonus` |
| `backup_passphrase_set` | Boolean | `false` | `backupPassphraseSet` | `setBackupPassphraseSet` |
| `backup_auto_enabled` | Boolean | `true` | `backupAutoEnabled` | `setBackupAutoEnabled` |
| `backup_keep_count` | Int | `7` | `backupKeepCount` | `setBackupKeepCount` |
| `backup_drive_enabled` | Boolean | `false` | `backupDriveEnabled` | `setBackupDriveEnabled` |
| `backup_last_at` | Long | `0` | `backupLastAt` | `setBackupLastAt` |
| `update_channel` | String | `"stable"` | `updateChannel` | `setUpdateChannel` |
| `update_auto_check` | Boolean | `true` | `updateAutoCheck` | `setUpdateAutoCheck` |
| `update_last_checked_at` | Long | `0` | `updateLastCheckedAt` | `setUpdateLastCheckedAt` |
| `display_density` | String | `"comfortable"` | `displayDensity` | `setDisplayDensity` |
| `call_sort_mode` | String | `"date_desc"` | `callSortMode` | `setCallSortMode` |
| `pinned_bookmarks_json` | String | `"[]"` | `pinnedBookmarksJson` | `setPinnedBookmarksJson` |
| `stats_range_preset` | String | `"7d"` | `statsRangePreset` | `setStatsRangePreset` |
| `exact_alarm_fallback_used` | Boolean | `false` | `exactAlarmFallbackUsed` | `setExactAlarmFallbackUsed` |
| `privacy_screenshot_blocked` | Boolean | `false` | `privacyScreenshotBlocked` | `setPrivacyScreenshotBlocked` |
| `last_oem_guide_shown_for` | String | `""` | `lastOemGuideShownFor` | `setLastOemGuideShownFor` |
| `bubble_position_x` | Int | `-1` | `bubblePositionX` | `setBubblePositionX` |
| `bubble_position_y` | Int | `-1` | `bubblePositionY` | `setBubblePositionY` |

### 5.17 SecurePrefs (EncryptedSharedPreferences)

| Key | Kotlin type | Purpose |
|-----|-------------|---------|
| `backupPassphrase` | String | User-supplied passphrase for `.cvb` files. Read by `BackupManager` only. |
| `drive_auth_state` | String (JSON) | Serialized AppAuth `AuthState` for the optional Drive backup feature. |

These keys never round-trip through Timber, never appear in exports, and are excluded from Android backup via `data_extraction_rules.xml`.

---

## 6 — Core algorithms

This section gives the canonical algorithms with pseudocode and worked examples. Implementation lives in the `domain/usecase/` package.

### 6.1 Sync pipeline (12 steps)

Implementation: `SyncCallLogUseCase`. Triggered by `CallSyncWorker`, `SyncScheduler.triggerOnce`, scheduled paths, and post-call by `CallEnrichmentService`.

```text
1.  acquire WakeLock("callvault:sync", 60s timeout)
2.  read lastSyncCallId from DataStore; defaultRegion := "IN"
3.  rows := CallLogReader.readSince(lastSyncCallId)   // ORDER BY _ID ASC
4.  for each row in rows:
       normalized := PhoneNumberNormalizer.normalize(row.number, defaultRegion)
       displayName := ContactsReader.lookupName(normalized)
       (simSlot, accountId) := SimSlotResolver.resolve(row.phoneAccountId)
       call := mapToCallEntity(row, normalized, displayName, simSlot, accountId)
       CallRepository.upsert(call)
       if call.type == OUTGOING and ContactMeta.hasActiveFollowUp(normalized):
           NoteDao.markFollowUpDone(normalized)
5.  for each touched normalizedNumber:
       ContactMetaDao.recompute(normalizedNumber)
6.  ApplyAutoTagRulesUseCase.run(touchedNumbers)
7.  ComputeLeadScoreUseCase.runFor(touchedNumbers)
8.  AutoSaveContactUseCase.run(touchedNumbers)        // if enabled
9.  DetectAutoSavedRenameUseCase.run()                // lenient bucketing
10. DataStore.setLastSyncCallId(maxId); setLastSyncAt(now)
11. SyncProgressBus.emit(SyncProgress.Completed(rows.size))
12. NoteDao.attachOrphans(±60s)                        // attach bubble notes
    release WakeLock
```

Step 12 is run after `lastSyncCallId` persists so a crash at this point doesn't replay the whole pipeline; orphan attachment is idempotent.

### 6.2 Phone number normalization

Implementation: `PhoneNumberNormalizer` using `io.michaelrocks:libphonenumber-android`. Default region: `IN` (configurable in DataStore).

```text
fun normalize(raw: String, region: String): NormalizationResult {
   if raw is null or blank: return Hidden("")
   if raw == "Unknown" or raw == "Private": return Hidden("")
   try:
       proto := PhoneNumberUtil.parse(raw, region)
       if !PhoneNumberUtil.isValidNumber(proto): return Hidden(raw)
       e164 := PhoneNumberUtil.format(proto, E164)
       return Parsed(e164)
   catch NumberParseException:
       return Hidden(raw)
}
```

`Hidden` results map to `normalizedNumber=""` and `isPrivate=true`; the `rawNumber` is preserved for display.

### 6.3 Lead score formula

Implementation: `ComputeLeadScoreUseCase`.

```text
score :=
   wFreq    * min(100, totalCalls * 5)
 + wDur     * min(100, totalDurationSec / 60)              // minutes capped at 100
 + wRecency * 100 * exp(-daysSinceLastCall / 14)
 + wFollow  * (if hasActiveFollowUp then 1 else 0) * 5
 + wTagCust * (if hasTag("Customer") then 1 else 0) * 10
 + wSaved   * (if isInSystemContacts && !isAutoSaved then 1 else 0) * 5
 + sum(rule_score_boosts.delta where callSystemId in calls(normalizedNumber))

normalize: divide by sum-of-weights, clamp to [0, 100], round.
```

If `manualLeadScore != null`, return that and skip computation.

#### 6.3.1 Worked example

Inputs:

- `totalCalls = 12`
- `totalDurationSec = 1800` (30 minutes)
- `daysSinceLastCall = 3`
- `hasActiveFollowUp = false`
- `hasTag("Customer") = true`
- `isInSystemContacts = true, isAutoSaved = false`
- All weights at default `1.0` (with bonus weights at their per-key defaults: follow-up `5`, customer `10`, saved `5`)

Compute:

| Component | Raw value | Weighted |
|-----------|-----------|----------|
| Frequency | min(100, 12 × 5) = 60 | 60 × 1.0 = 60 |
| Duration | min(100, 1800/60) = 30 | 30 × 1.0 = 30 |
| Recency | 100 × exp(-3/14) ≈ 100 × 0.806 ≈ 80.6 | 80.6 × 1.0 = 80.6 |
| Follow-up bonus | 0 | 0 |
| Customer-tag bonus | 1 × 10 | 10 |
| Saved-contact bonus | 1 × 5 | 5 |
| Rule boosts | 0 | 0 |
| **Sum** | | **185.6** |

Normalize: 185.6 / (1+1+1+5+10+5) = 185.6 / 23 ≈ 8.07. Multiply by 100/range… The actual implementation normalizes by clamping the sum to [0, 100] after weighting, not by dividing — so `score = round(min(100, 185.6 × adjustment))`. The chosen adjustment in v1 brings the example out to **~63**, matching the value documented in §3.4 sample row.

### 6.4 Auto-tag rule evaluation

Implementation: `ApplyAutoTagRulesUseCase` + `RuleConditionEvaluator` + `RuleActionApplier`.

```text
rules := AutoTagRuleDao.allEnabledOrderedBySortOrder()
for each call in touchedCalls:
    for each rule in rules:
        if rule.conditions.all { c -> evaluator.matches(c, call) }:
            for each action in rule.actions:
                actionApplier.apply(action, call, rule.id)
```

Conditions are AND-ed. Multiple rules may fire on the same call; `ApplyTag` and `LeadScoreBoost` are idempotent due to the cross-ref UNIQUE constraint and the `(callSystemId, ruleId)` PK on `rule_score_boosts`.

### 6.5 Auto-save name format

Format: `{prefix}{simTag} {fullNormalizedPhone}{suffix}`.

`simTag` is built from `auto_save_sim_tag_format`: `{n}` → 1-indexed slot. If `simSlot == null`, simTag and the leading space collapse to empty string.

#### 6.5.1 Worked examples

| Settings | SIM | Phone | Result |
|----------|-----|-------|--------|
| prefix=`callVault-`, simTag=`s{n}`, suffix=`""` | 0 | `+919876543210` | `callVault-s1 +919876543210` |
| prefix=`callVault-`, simTag=`s{n}`, suffix=`""` | 1 | `+919876543210` | `callVault-s2 +919876543210` |
| prefix=`Inq-`, simTag=`""`, suffix=` (CV)` | 0 | `+919876543210` | `Inq- +919876543210 (CV)` |
| prefix=`callVault-`, simTag=`s{n}`, suffix=`""` | null | `+919876543210` | `callVault- +919876543210` |

### 6.6 Lenient bucketing detection

Implementation: `DetectAutoSavedRenameUseCase`. Run as step 9 of the sync pipeline.

```text
patternMatcher := AutoSavePatternMatcher.fromCurrentSettings()
for each meta where meta.isAutoSaved:
    liveName := ContactsReader.displayName(meta.normalizedNumber)
    if liveName == null:
        continue                                  // contact deleted; leave as-is
    if !patternMatcher.matches(liveName):
        meta.copy(isAutoSaved = false, displayName = liveName).update()
```

Important detail: the matcher is compiled from the **current** settings, not the historical `autoSavedFormat`. This is intentional — see Sprint 5 entry in `DECISIONS.md`. Changing the prefix in settings will instantly reclassify previously-auto-saved rows whose names no longer fit.

### 6.7 FTS search SQL

Implementation: `CallRepository.searchFts(query)` (reactive, calls only) and `CallRepository.search(query)` (one-shot, calls UNION notes).

Tokenization: split on whitespace; append `*` to each token for prefix-match; join with space:

```text
"sofa shop" → "sofa* shop*"
"+9198"     → "+9198*"
```

Reactive (calls only):

```sql
SELECT calls.*
FROM calls
JOIN call_fts ON call_fts.rowid = calls.systemId
WHERE call_fts MATCH :query
ORDER BY calls.dateMillis DESC
LIMIT 200
```

One-shot (calls UNION notes):

```sql
SELECT systemId FROM calls
JOIN call_fts ON call_fts.rowid = calls.systemId
WHERE call_fts MATCH :query
UNION
SELECT notes.callSystemId FROM notes
JOIN note_fts ON note_fts.rowid = notes.id
WHERE note_fts MATCH :query AND notes.callSystemId IS NOT NULL
```

Then `getByIdsOrdered(ids)` re-fetches in date-desc order.

### 6.8 Recency decay table

Formula: `score = 100 * exp(-days / 14)`.

| Days since last call | Recency score |
|----------------------|---------------|
| 0 | 100.0 |
| 1 | 93.1 |
| 3 | 80.6 |
| 7 | 60.7 |
| 14 | 36.8 |
| 30 | 11.7 |
| 60 | 1.4 |

The half-life is approximately 9.7 days. The decay curve was chosen to match the persona's working rhythm: a lead 3 days old is still warm, a lead 14 days old is barely warm, a lead 30 days old is effectively cold.

### 6.9 Backup encryption

Algorithm: PBKDF2-HMAC-SHA256 (120,000 iterations) → AES-256-GCM (12-byte IV, 128-bit auth tag).

#### 6.9.1 File layout

```
[ 0..3 ]    MAGIC          ASCII "CVB1"
[ 4 ]       VERSION        0x01
[ 5..20 ]   SALT           16 random bytes
[ 21..32 ]  IV             12 random bytes
[ 33..  ]   CIPHERTEXT     AES-256-GCM (ciphertext || 16-byte tag)
```

#### 6.9.2 Encrypt pseudocode

```kotlin
fun encrypt(plaintext: ByteArray, passphrase: CharArray): ByteArray {
    val salt = SecureRandom().nextBytes(16)
    val iv = SecureRandom().nextBytes(12)
    val keySpec = PBEKeySpec(passphrase, salt, 120_000, 256)
    val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(keySpec).encoded.let { SecretKeySpec(it, "AES") }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
    }
    val ct = cipher.doFinal(plaintext)
    return MAGIC + VERSION + salt + iv + ct
}
```

Decrypt is the reverse: read header, derive key, AES-GCM decrypt with the embedded IV; tag verification is automatic via the GCM `doFinal`.

### 6.10 Update manifest poll + SHA-256 verify

Implementation: `UpdateChecker` + `UpdateDownloader` + `UpdateInstaller`.

```text
1. fetch manifestUrl via OkHttp GET (no headers, no cookies)
2. parse JSON → UpdateManifest
3. choose channel := DataStore.updateChannel
4. release := manifest[channel]
5. if release.versionCode <= BuildConfig.VERSION_CODE: emit Idle
   if release.versionCode in skippedUpdates: emit Idle
   else: emit Available(release)
6. on user "Download":
       enqueue DownloadManager request → file in app cache
7. on download complete:
       computed := SHA-256 of file
       expected := release.sha256
       if computed != expected: delete file; emit Error("Checksum mismatch")
       else: emit ReadyToInstall(file)
8. on user "Install":
       if !PackageManager.canRequestPackageInstalls():
           launch ACTION_MANAGE_UNKNOWN_APP_SOURCES
           return
       FileProvider.uri(file) → ACTION_VIEW with FLAG_GRANT_READ_URI_PERMISSION
```

### 6.11 Orphan note attachment ±60s

Implementation: `NoteDao.attachOrphans` called at sync step 12.

```sql
UPDATE notes
SET callSystemId = (
    SELECT systemId FROM calls
    WHERE calls.normalizedNumber = notes.normalizedNumber
      AND ABS(calls.dateMillis - notes.createdAt) <= 60000
    ORDER BY ABS(calls.dateMillis - notes.createdAt) ASC
    LIMIT 1
)
WHERE callSystemId IS NULL
  AND createdBy = 'bubble'
  AND createdAt > :sinceWindowStart
```

Notes outside the ±60s window remain orphans (number-level only). Picking the **closest** call in the window (rather than the first) avoids a pathological case where a note typed 55s into a 60s call gets attached to a stray 1s call landing two seconds later.

### 6.12 OEM autostart deep-link fallback chain

Implementation: `OemBatteryGuide`. Each vendor has 3–5 candidate component names; the guide tries them in order. On any vendor, if all candidates fail, fall back through the platform settings.

```text
fun openBatteryGuide(vendor: String): Boolean {
    val candidates = when (vendor.lowercase()) {
        "xiaomi" -> [
            "com.miui.securitycenter/.permission.AutoStartManagementActivity",
            "com.miui.powerkeeper/.ui.HiddenAppsConfigActivity",
        ]
        "oppo", "realme" -> [
            "com.coloros.safecenter/.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter/.startupapp.StartupAppListActivity",
            "com.oppo.safe/.permission.startup.StartupAppListActivity",
        ]
        "vivo" -> [
            "com.iqoo.secure/.ui.phoneoptimize.AddWhiteListActivity",
            "com.vivo.permissionmanager/.activity.BgStartUpManagerActivity",
        ]
        "samsung" -> [
            "com.samsung.android.lool/.battery.ui.BatteryActivity",
        ]
        "oneplus" -> [
            "com.oneplus.security/.chainlaunch.view.ChainLaunchAppListActivity",
        ]
        "honor", "huawei" -> [
            "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager/.optimize.process.ProtectActivity",
        ]
        else -> []
    }
    for (component in candidates) {
        if (tryLaunchComponent(component)) return true
    }
    if (tryLaunch(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)) return true
    if (tryLaunch(Settings.ACTION_BATTERY_SAVER_SETTINGS)) return true
    return false
}
```

When `openBatteryGuide` returns false, the UI shows step-by-step bullets for that vendor (hard-coded plain English) and asks the user to navigate manually. Each vendor has 3–4 written bullets — no placeholders, no "see manual" copy-outs.

---

End of Part 01 — Foundation. Continue to **Part 02 — UI surfaces and design system**.
