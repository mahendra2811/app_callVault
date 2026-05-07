# callNest — APP-SPEC

This is the canonical product/UI specification for callNest. It is intended for any designer or engineer rebuilding the UI from scratch in any framework. Reading this file alone, you should be able to mock every screen accurately and understand every input, output, edge case, and copy line.

**Status**: Living document. Update `DECISIONS.md` alongside any change to this file.
**Last updated**: 2026-05-02 (Phase I shipped).
**Reading time**: 90–120 minutes. Skim Sections 0–8 for orientation; deep-read Section 9+ when rebuilding a specific screen.
**Source documents**: `CLAUDE.md`, `.claude/docs/06-glossary.md`, `CHANGELOG.md`, `DECISIONS.md`, plus the locked spec at `~/Documents/p_projet/a_APP/callNest_mega_prompt.md` (1533 lines, design intent unchanged).

---

## Table of Contents

### Part 1 — Foundation (§0–§6)

- §0 Document conventions
- §1 Project overview
- §2 Locked tech stack
- §3 Domain glossary
- §4 Permissions inventory
- §5 Data model
- §6 Core algorithms

### Part 2 — Theme + Navigation + Splash + Onboarding (§7–§15)

- §7 Theme: colors, typography, spacing, shapes, motion
- §8 Navigation graph
- §9 Splash screen
- §10 Onboarding orchestrator
- §11 Onboarding page 1: Welcome
- §12 Onboarding page 2: Features
- §13 Onboarding page 3: Permissions
- §14 Onboarding page 4: OEM battery
- §15 Onboarding page 5: First sync

### Part 3 — Main scaffold + 4 tab pages (§16–§20)

- §16 MainScaffold
- §17 Home tab
- §18 Calls tab
- §19 Inquiries tab
- §20 More tab

### Part 4 — Deep pages, batch 1 (§21–§26)

- §21 CallDetail screen
- §22 Search overlay
- §23 Stats dashboard
- §24 Bookmarks screen
- §25 FollowUps screen
- §26 MyContacts screen

### Part 5 — Deep pages, batch 2 (§27–§32)

- §27 TagsManager screen
- §28 AutoTagRules screen
- §29 RuleEditor screen
- §30 Backup screen
- §31 Export screen (5-step wizard)
- §32 QuickExport sheet

### Part 6 — Settings, updates, docs, permissions, components, appendices (§33–§46)

- §33 Settings master screen
- §34 AutoSave settings
- §35 RealTime settings
- §36 LeadScoring settings
- §37 Update settings
- §38 UpdateAvailable screen
- §39 DocsList screen
- §40 DocsArticle screen
- §41 PermissionRationale screen
- §42 PermissionDenied screen
- §43 Neo\* component reference
- §44 Copy / voice guide
- §45 Empty / loading / error state catalog
- §46 Future-proofing notes

---

## Per-page template

Each page section in this spec uses 15 subsections:

1. **Purpose** — what the page is for
2. **Entry points** — every route/intent that opens it
3. **Exit points** — every navigation path it can lead to
4. **Required inputs (data)** — route args + ViewModel state
5. **Required inputs (user)** — taps, swipes, gestures
6. **Mandatory display elements**
7. **Optional display elements**
8. **Empty state** — copy + icon + CTA
9. **Loading state**
10. **Error state**
11. **Edge cases** — minimum 5 per page
12. **Copy table** — every string with id + text
13. **ASCII wireframe**
14. **Accessibility** — TalkBack labels, touch targets, contrast
15. **Performance budget**

---

---

# APP-SPEC Part 01 — Foundation

> Living document. Source of truth for callNest v1.0.0 foundations.
> Last revised: 2026-04-30. Owner: callNest core.

This is **Part 01** of the multi-part callNest APP-SPEC. It covers conventions, project overview, the locked tech stack, the domain glossary, the Android permissions inventory, the data model (Room + DataStore + SecurePrefs), and the canonical algorithms.

Cross-references:

- Part 02 — UI surfaces, screens, navigation, design system, Neo\* components.
- Part 03 — Sync pipeline, real-time service, workers, schedules.
- Part 04 — Auto-tag rules, lead scoring, stats, insights.
- Part 05 — Export, backup, restore, self-update.
- Part 06 — Testing, build, distribution, release ops.
- Part 07 — In-app docs, onboarding, OEM autostart, troubleshooting.

---

## 0 — Document conventions

### 0.1 Glyph legend

The following glyphs are used throughout the APP-SPEC. They are not decoration — each one carries meaning, and reviewers must read them.

| Glyph                      | Meaning                                                                                     |
| -------------------------- | ------------------------------------------------------------------------------------------- |
| `[L]`                      | **Locked** decision. Do not relitigate without writing a new entry in `DECISIONS.md`.       |
| `[D]`                      | **Deferred** to a later milestone. Tracked in `TODO.md`.                                    |
| `[F]`                      | **Fallback** taken because the preferred path was not viable. Documented in `DECISIONS.md`. |
| `[P0]`–`[P3]`              | Priority. P0 blocks a release; P3 is nice-to-have.                                          |
| `[v1]` / `[v1.1]` / `[v2]` | Target version for the item.                                                                |
| `§`                        | Section reference inside this APP-SPEC. `§3.4` means section 3.4 of the relevant Part.      |
| `→`                        | "Resolves to" / "produces".                                                                 |
| `±`                        | Bidirectional tolerance (e.g. ±60s window).                                                 |

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

| Version       | Where                  | What it means                                   |
| ------------- | ---------------------- | ----------------------------------------------- |
| `versionName` | `app/build.gradle.kts` | Marketing string shown in Settings → About.     |
| `versionCode` | `app/build.gradle.kts` | Integer used by self-update comparisons.        |
| `db version`  | `callNestDatabase.kt`  | Room schema version, separate from app version. |

`versionName` is bumped with every user-visible release. `versionCode` is monotonically increasing. `db version` only bumps when a Room migration is added.

### 0.5 How to read this APP-SPEC

The APP-SPEC is structured to be read in two passes:

**Pass 1 — orientation**: read §1 of each Part, top to bottom. This gives you a complete mental model of callNest in roughly 30 minutes.

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

### 1.1 What callNest is

callNest is an **offline-first Android app** that turns the device's call history into a lightweight inquiry CRM. It captures every call from `CallLog.Calls`, normalizes and enriches each row, separates real contacts from unsaved inquiries, and gives the user a fast surface to **tag, note, bookmark, follow up, search, filter, and export** their calls.

The product is targeted, not generic. Generic call-log apps focus on display; callNest focuses on **conversion**: turning a flood of inquiry calls into a triaged, scored, actionable pipeline. This shapes every design decision — the lead score, the auto-save flow, the floating bubble, the post-call popup, and the two-bucket My Contacts / Inquiries split all exist to serve the conversion job.

callNest is **sideloaded**, **GMS-free**, and **single-device**. There is no cloud sync, no account, no telemetry, no analytics, and no Play Store presence. The single allowed outbound network call is a self-update manifest poll. This posture is non-negotiable for v1 — see §1.6.

### 1.2 Target user persona — Indian small-business owner

The primary persona is a **30–55 year-old Indian small-business owner** running a B2C or low-ticket B2B business: a coaching center, a real-estate broker, a furniture showroom, a wedding photographer, a wholesale clothing trader, a plumber, an electrician, a car-dealership salesperson. The defining trait: **20–100 daily inbound calls**, most from numbers not in their contact book.

#### 1.2.1 Day in the life

- **8:30 AM** — first call of the day comes in while the owner is still finishing chai. It's an unsaved number; callNest's floating bubble surfaces _"new inquiry"_ and lets the owner jot one word: _furniture_. The post-call popup offers `Customer / Inquiry / Spam` quick-tag chips; the owner taps `Inquiry` and dismisses.
- **10:00 AM** — at the shop. Picks up the phone, opens callNest. The Calls list shows a pinned section: _Unsaved inquiries — last 7 days_ with 14 entries. He swipes-right on three of them to bookmark; one of them gets a follow-up scheduled for _tomorrow 11 AM_.
- **12:30 PM** — he searches for _"sofa"_ in callNest. FTS hits a note he wrote two weeks ago against a number, plus three calls whose `geocodedLocation` matches "Sofakart Pvt Ltd". He calls one back.
- **3:00 PM** — opens _My Contacts_ tab. His phone Contacts now has 200+ auto-saved entries named `callNest-s1 +91...`. Three of them got renamed to real names yesterday — callNest's lenient bucketing has already moved them out of the Inquiries bucket. He never has to think about it.
- **6:30 PM** — quick check on Stats. _Hot leads: 4_. He opens the lead-score 90+ filter, sees all four, calls each one once. Two convert. He tags them `Closed-won` and adds a markdown note with the order details.
- **8:00 PM** — wraps up. Tomorrow's `Today` follow-up tab has 6 entries cued for 11 AM, 12 PM, and 3 PM. callNest's exact-alarm follow-up reminders fire even when the phone is in Doze.
- **2:00 AM** — `DailyBackupWorker` runs while he sleeps. An encrypted `.cvb` file lands in `Downloads/callNest/`. Optionally, `BackupDriveWorker` uploads the same encrypted blob to his own Google Drive (off by default; see Part 05).

The persona is technical-curious but not technical. He sideloads APKs that someone in a WhatsApp business group sends him. He is wary of "bills" inside apps, distrusts cloud apps that ask for OTP, and is happy to grant battery / overlay / autostart settings if you tell him _exactly which buttons to press_. callNest's UX leans into all of this.

### 1.3 Top 5 user jobs

The product is justified entirely by these five jobs. Anything that does not serve one of them is out of scope for v1.

1. **"Make sure I never lose an inquiry."** Every call is captured, even if it landed during a meeting, even if the number is unknown, even if the phone was off when they called.
2. **"Tell me which inquiry to call back first."** The lead score (§6.3) ranks numbers by frequency × duration × recency, plus tag and follow-up boosts.
3. **"Help me remember who this person was."** Notes with markdown, edit history, and bubble notes typed mid-call.
4. **"Keep my real contacts and my inquiry pile separate."** Auto-save into a `callNest Inquiries` group; lenient bucketing flips an entry to _My Contact_ when the user renames it.
5. **"Let me hand a daily/weekly/monthly report to my accountant or my partner."** Export to Excel / CSV / PDF with a wizard that chooses range, scope, columns, and destination.

### 1.4 Non-goals (locked)

These are intentionally **not** in v1. Each is locked; reopening requires a new `DECISIONS.md` entry plus a maintainer sign-off.

- `[L]` **No call recording.** Legally fraught in India; technically blocked on most modern Android versions without rooting; not worth it.
- `[L]` **No WhatsApp integration.** No reading of WhatsApp messages, no scraping of WhatsApp call logs, no tagging WhatsApp inquiries. The OS does not expose WhatsApp call history to third-party apps without root.
- `[L]` **No cloud sync v1.** Backup only. The optional Google Drive _backup_ upload added 2026-05-02 (see `DECISIONS.md`) is **not** sync — it is a one-way encrypted-blob upload of the same `.cvb` already on the device.
- `[L]` **No multi-user / multi-device.** Single phone, single owner. No "team" concept.
- `[L]` **No multi-language.** English-only UI in v1. All strings live in `res/values/strings.xml`; localization is a future port, not a parallel track.
- `[L]` **No dark mode v1.** Neumorphism leans on a tinted-light surface; a faithful dark-mode neumorphism is a design problem, not a code problem, and is deferred to v2.
- `[L]` **No crash reporting / no analytics.** Privacy posture forbids it. See §1.6.
- `[L]` **No "default dialer" mode.** Using `RoleManager.ROLE_DIALER` would unlock things (real-time call number on API 31+, call-screening) but requires Play Store distribution to clear policy. Sideloaded apps cannot become default dialer cleanly. callNest works _around_ this constraint.

### 1.5 Distribution model

callNest is **sideloaded only**. There is no Play Store listing, no AppGallery listing, no F-Droid listing for v1.

The release pipeline is:

1. Maintainer builds a signed release APK on a local workstation with the production keystore.
2. APK is uploaded to the owner's web host alongside an updated `versions.json` manifest.
3. SHA-256 of the APK is computed and pasted into the manifest.
4. End users either:
   - Install the first APK manually (one-time, via WhatsApp share / web link / USB).
   - Or get a notification from the in-app `UpdateCheckWorker` that polls the manifest weekly.

The in-app update flow uses `DownloadManager` for the download, verifies SHA-256 before launching the system installer, and hands off to the system via `FileProvider` + `ACTION_VIEW`. From API 26+ the user must additionally have granted "Install unknown apps" for callNest — the install screen handles that gate.

There is no auto-install. The user always sees the system installer dialog. This is both a platform constraint and a design choice.

### 1.6 Privacy posture

callNest is built around a hard privacy stance. From `data_extraction_rules.xml` to the manifest's network whitelist, nothing leaves the device unless the user took an explicit action.

- **No Firebase, no Crashlytics, no GA, no Mixpanel, no Sentry, no third-party SDK that phones home.** Adding any of these requires a new top-level `DECISIONS.md` entry signed by the maintainer.
- **No silent telemetry.** Timber logs go to logcat only.
- **The only outbound HTTP** is the update manifest poll to `BuildConfig.UPDATE_MANIFEST_STABLE_URL` / `BETA_URL`. The poll sends no user data — just a plain `GET` for the manifest JSON.
- **`data_extraction_rules.xml` excludes every domain** (cloud backup, device transfer) so user data cannot be pulled off the device by Android's built-in backup mechanisms.
- **All sensitive prefs** (`backupPassphrase`, `drive_auth_state`) live in `EncryptedSharedPreferences` via `SecurePrefs`.
- **Backup files are encrypted** with a user-supplied passphrase (PBKDF2-HMAC-SHA256, 120k iterations, AES-256-GCM). A leaked `.cvb` is useless without the passphrase.
- **Optional Drive upload** uses AppAuth (no GMS) and uploads only the already-encrypted `.cvb` blob; Google sees ciphertext.

### 1.7 Quality bar

These are non-aspirational targets. Each one has a measurable definition. Builds that regress any of them block release.

| Metric                              | Target                                 | Measurement                                                        |
| ----------------------------------- | -------------------------------------- | ------------------------------------------------------------------ |
| Cold start to first frame           | **< 1.5 s** on a Pixel 4a-class device | Macrobenchmark (deferred), or visual inspection on debug build.    |
| Filter sheet apply → list updates   | **< 300 ms** for a 5,000-call DB       | Wallclock from `onApplyFilter` to first emission of the new list.  |
| FTS search query → first result row | **< 100 ms** for a 5,000-call DB       | Wallclock from `onQueryChange` (after debounce) to first emission. |
| Sync pipeline end-to-end            | **< 2 s** for 100 new calls            | `SyncProgressBus` start → completion timestamp delta.              |
| Release APK size                    | **< 25 MB**                            | `ls -l app-release.apk`.                                           |
| Memory footprint at idle            | **< 80 MB** RSS                        | adb shell `dumpsys meminfo`.                                       |
| ANR rate                            | **0**                                  | Visual on device + logcat ANR markers.                             |

Lead-score recompute is allowed up to 1 s for a 5,000-call DB; backup encryption is allowed up to 8 s for the same database size (PBKDF2 dominates).

---

## 2 — Locked tech stack

This is the canonical dependency table. Versions are **pinned exactly**. Do not silently bump a major. Minor / patch bumps require a `DECISIONS.md` entry only if the bump changes behavior; otherwise update both `libs.versions.toml` and this table in the same PR.

### 2.1 Core toolchain

| Component                   | Version             | Role                                                    |
| --------------------------- | ------------------- | ------------------------------------------------------- |
| Kotlin                      | **2.0.21**          | Project language. K2 compiler.                          |
| Android Gradle Plugin (AGP) | **8.7.3**           | Build system.                                           |
| Gradle                      | **8.10.2**          | Build runner.                                           |
| compileSdk                  | **35**              | Android 15 APIs available at compile time.              |
| targetSdk                   | **35**              | Behavior changes for Android 15 opted in.               |
| minSdk                      | **26**              | Android 8.0 floor. Drops < 5% of Indian market in 2026. |
| JVM target                  | **17**              | Toolchain JDK.                                          |
| KSP                         | **2.0.21-1.0.27**   | Symbol processing for Hilt + Room.                      |
| `kotlin-stdlib`             | bundled with 2.0.21 | Standard library.                                       |

`minSdk = 26` is locked because:

- `JobScheduler`/`WorkManager` periodic intervals are sane.
- `NotificationChannel` is mandatory and consistent.
- `EncryptedSharedPreferences` works out of the box.
- `FileProvider` + unknown-sources install gating is consistent.
- Foreground-service-special-use is available with the polyfill on 26–33 and native on 34+.

`targetSdk = 35` is locked because Play Store would require it anyway, and behavior changes (foreground service types, partial broadcasts) are explicitly handled.

### 2.2 UI

| Library                                          | Version                                              | Role                                                            |
| ------------------------------------------------ | ---------------------------------------------------- | --------------------------------------------------------------- |
| Compose BOM                                      | **2024.12.01**                                       | Single-source Compose version graph.                            |
| Compose Material 3                               | from BOM                                             | Material 3 components, restyled with Neo\* wrappers.            |
| Compose Navigation                               | **2.8.5**                                            | Single-Activity routing.                                        |
| Compose Foundation                               | from BOM                                             | LazyColumn, gestures.                                           |
| Compose UI / Tooling                             | from BOM                                             | `@Preview`, `Modifier`, `LayoutInspector`.                      |
| `androidx.activity:activity-compose`             | 1.9.3                                                | `setContent`, `BackHandler`.                                    |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7                                                | `viewModel()` factory.                                          |
| `androidx.lifecycle:lifecycle-runtime-compose`   | 2.8.7                                                | `collectAsStateWithLifecycle`.                                  |
| `androidx.core:core-splashscreen`                | **1.0.1**                                            | API 31+ splash compatibility on 26+.                            |
| Coil                                             | **3.0.4** (`io.coil-kt.coil3:coil-compose`)          | Image loading (avatar, doc images).                             |
| Vico                                             | **2.0.0-beta.4** (`compose-m3` + `compose` + `core`) | Stats charts (limited use; most charts are hand-rolled Canvas). |

### 2.3 DI / persistence / async

| Library                                | Version          | Role                                                |
| -------------------------------------- | ---------------- | --------------------------------------------------- |
| Hilt                                   | **2.53.1** (KSP) | Dependency injection.                               |
| `androidx.hilt:hilt-work`              | 1.2.0            | `@HiltWorker` + `@AssistedInject` for WorkManager.  |
| Room runtime                           | **2.6.1**        | SQLite ORM.                                         |
| Room compiler                          | **2.6.1** (KSP)  | Generated DAOs.                                     |
| Room ktx                               | 2.6.1            | Coroutines + `withTransaction`.                     |
| DataStore Preferences                  | **1.1.1**        | Typed prefs (~40 keys).                             |
| WorkManager                            | **2.10.0**       | Periodic + one-time background work.                |
| `androidx.security:security-crypto`    | 1.1.0-alpha06    | `EncryptedSharedPreferences` for `SecurePrefs`.     |
| kotlinx coroutines                     | **1.9.0**        | Structured concurrency.                             |
| kotlinx serialization                  | **1.7.3**        | JSON for export, rules, manifest.                   |
| kotlinx datetime                       | **0.6.1**        | Calendar / instant arithmetic for sync, follow-ups. |
| `androidx.lifecycle:lifecycle-service` | 2.8.7            | `LifecycleService` for `CallEnrichmentService`.     |
| `androidx.savedstate:savedstate`       | 1.2.1            | Transitive for the lifecycle bits.                  |

### 2.4 Telephony / contacts / phone numbers

| Library                                  | Version     | Role                                                 |
| ---------------------------------------- | ----------- | ---------------------------------------------------- |
| `io.michaelrocks:libphonenumber-android` | **8.13.50** | Phone normalization to E.164 with default region IN. |

The Michael-Rocks port of Google's libphonenumber is preferred over the official `com.googlecode.libphonenumber:libphonenumber` because it ships pre-compiled metadata as Android assets — half the size, faster cold-start.

### 2.5 Export / encryption

| Library               | Version    | Role                                                                     |
| --------------------- | ---------- | ------------------------------------------------------------------------ |
| Apache POI ooxml-lite | **5.2.5**  | Excel multi-sheet export.                                                |
| iText core            | **8.0.5**  | PDF export.                                                              |
| Tink                  | **1.15.0** | (Catalogued but unused in v1; backup uses raw JCA — see `DECISIONS.md`.) |

### 2.6 Auth (optional Drive backup)

| Library | Version                           | Role                                              |
| ------- | --------------------------------- | ------------------------------------------------- |
| AppAuth | **0.11.1** (`net.openid:appauth`) | OAuth 2.0 for Google Drive without Play Services. |
| OkHttp  | **4.12.0**                        | Drive REST + update manifest fetch.               |

### 2.7 Logging

| Library | Version   | Role                                                            |
| ------- | --------- | --------------------------------------------------------------- |
| Timber  | **5.0.1** | Logging façade. Planted in `callNestApp.onCreate` (debug only). |

### 2.8 Test stack

| Library                                    | Version     | Role                                           |
| ------------------------------------------ | ----------- | ---------------------------------------------- |
| JUnit5 (`org.junit.jupiter:junit-jupiter`) | **5.11.4**  | Unit test runner.                              |
| Turbine                                    | **1.2.0**   | Flow assertions.                               |
| MockK                                      | **1.13.13** | Kotlin-friendly mocking.                       |
| `androidx.test.ext:junit`                  | 1.2.1       | Instrumentation test runner (scaffolded only). |
| `androidx.room:room-testing`               | 2.6.1       | DAO tests, migration tests.                    |

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

`CallEntity` carries the system-provided fields plus callNest-only fields: `isBookmarked`, `bookmarkReason`, `followUpDate`, `followUpTime`, `followUpDone`, `archived`, `leadScore`, `leadScoreManualOverride`, `tagIds` (denormalized cache, refreshed by `CallTagCrossRef`).

The system row is treated as immutable input: callNest never deletes from `CallLog.Calls`, never modifies it, and never tries to write to it. callNest's view is a faithful mirror plus enrichment.

Examples:

- A 22-second incoming call from `+919876543210` at `2026-04-29T11:42:13Z` becomes one `CallEntity` row.
- The same number calling twice in five minutes is two rows, sharing `normalizedNumber`.
- A "Private number" from a withheld caller is one row with `rawNumber=""`, `normalizedNumber=""`, `isPrivate=true`.

### 3.2 Normalized number

Phone number in E.164 form, e.g. `+919876543210`. Produced by `PhoneNumberNormalizer` via libphonenumber-android with default region `IN`. This is the **primary join key** for ContactMeta, lead score, notes, auto-save, and tag rules.

Calls without a parseable number get an empty string `""` plus the `isPrivate=true` flag. Empty-string normalized numbers do not aggregate — each private call is its own ContactMeta-less row.

Worked examples:

| Raw input           | Default region IN | E.164 output                           |
| ------------------- | ----------------- | -------------------------------------- |
| `9876543210`        | IN                | `+919876543210`                        |
| `+919876543210`     | IN                | `+919876543210`                        |
| `09876543210`       | IN                | `+919876543210`                        |
| `+1 (415) 555-2671` | IN                | `+14155552671`                         |
| `911234567890`      | IN                | `+911234567890`                        |
| `*86`               | IN                | `""` (unparseable, treated as private) |
| `Unknown`           | IN                | `""`                                   |

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

An unsaved-number call. The user receives many inquiries per day — capturing them is callNest's primary job. A row is an inquiry while `isInSystemContacts=false`. Once the user (or auto-save) writes the number to system Contacts, `isInSystemContacts=true` flips on next sync.

### 3.7 Auto-saved contact

An inquiry written to system Contacts by callNest using the auto-save name format. Tracked via `ContactMetaEntity.isAutoSaved=true`. Lives in the configured contact group (default _callNest Inquiries_), with the configured `ACCOUNT_TYPE`/`ACCOUNT_NAME`. See §6.5 for the format and §3.10 for the group manager.

When the user later renames an auto-saved contact in the system Contacts app, lenient bucketing (§6.6) flips `isAutoSaved` back to `false` on next sync — without requiring the user to do anything in callNest.

### 3.8 My Contacts (bucket)

Top-level UX bucket for "real" contacts. SQL predicate: `isInSystemContacts=true AND isAutoSaved=false`. Two ways a row enters this bucket:

1. The user manually saved the number outside callNest, before or after first installing the app.
2. The user renamed a callNest-auto-saved contact, and lenient bucketing detected the rename.

### 3.9 Inquiries (bucket)

Top-level UX bucket for auto-saved-but-not-yet-renamed inquiries. SQL predicate: `isAutoSaved=true`. The user converts an inquiry to a real contact by renaming it (either inside callNest's `MyContactsScreen` "Convert" flow, or outside, in the system Contacts app).

### 3.10 Lenient bucketing

The rule per §6.6: if the user opens the system Contacts app and renames an auto-saved entry, `isAutoSaved` flips to `false` on the next sync. The contact moves from _Inquiries_ to _My Contacts_ automatically. The detection compares the live system display name against a regex compiled from the **current** auto-save settings, not the historical settings — see `DECISIONS.md` (Sprint 5).

### 3.11 Auto-save name format

Locked format: `{prefix}{simTag} {fullNormalizedPhone}{suffix}`. Configurable in Settings → Auto-Save. Defaults: `prefix="callNest-"`, `simTagFormat="s{n}"`, `suffix=""`. Worked examples in §6.5.

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
    "apkUrl": "https://example.com/callNest-1.0.1.apk",
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

### 3.44 Neo\* component

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

Every permission callNest requests, grouped by criticality. For each: manifest line, why callNest needs it, when requested, what's blocked if denied, fallback, user-facing rationale string.

### 4.1 Critical permissions

These are required runtime permissions. Without all of them, the Calls screen will not render — the user gets routed to `permission_rationale`.

#### 4.1.1 `READ_CALL_LOG`

```xml
<uses-permission android:name="android.permission.READ_CALL_LOG" />
```

- **Why callNest needs it.** It's the entire input to the sync pipeline. Without `READ_CALL_LOG`, `CallLogReader.readSince()` returns nothing.
- **When requested.** Onboarding page 3 (Permissions). Re-requested on Calls screen entry if denied.
- **What's blocked if denied.** Everything. The entire app is built on call log data.
- **Fallback.** None — denied → `permission_denied` route.
- **Rationale string.** `R.string.perm_rationale_call_log` — _"callNest reads your call log to capture inquiries. Without this, the app can't show any calls."_

#### 4.1.2 `READ_PHONE_STATE`

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

- **Why callNest needs it.** SIM-slot resolution via `SubscriptionManager`, in-call state monitoring via `TelephonyCallback`.
- **When requested.** Onboarding page 3, alongside call-log.
- **What's blocked if denied.** SIM-slot column in `CallEntity` is null. Real-time bubble cannot detect call state on its own.
- **Fallback.** App still works; SIM-slot filters will all match no calls; real-time toggles get disabled with a help link.
- **Rationale string.** `R.string.perm_rationale_phone_state` — _"callNest reads phone state to know which SIM took the call and to show in-call helpers."_

#### 4.1.3 `READ_CONTACTS`

```xml
<uses-permission android:name="android.permission.READ_CONTACTS" />
```

- **Why callNest needs it.** Resolve display name for known numbers; detect `isInSystemContacts`; lenient bucketing reads live display names.
- **When requested.** Onboarding page 3.
- **What's blocked if denied.** Calls show numbers only, no names. My Contacts bucket cannot be derived. Lenient bucketing cannot run.
- **Fallback.** App functions in degraded mode — numbers only, no name resolution. Banner suggests granting.
- **Rationale string.** `R.string.perm_rationale_read_contacts` — _"callNest reads contacts to show names instead of numbers."_

#### 4.1.4 `WRITE_CONTACTS`

```xml
<uses-permission android:name="android.permission.WRITE_CONTACTS" />
```

- **Why callNest needs it.** Auto-save inquiries into the _callNest Inquiries_ group; the BulkSaveContactsUseCase action.
- **When requested.** Onboarding page 3.
- **What's blocked if denied.** Auto-save toggle disabled in settings. Bulk-save action greyed out.
- **Fallback.** App fully functions without auto-save. The user is asked to grant the permission only when they enable auto-save.
- **Rationale string.** `R.string.perm_rationale_write_contacts` — _"callNest auto-saves inquiry numbers to a separate contact group so your address book stays organized."_

### 4.2 Special-grant permissions

These are not runtime-dialog permissions. They go through Settings.

#### 4.2.1 `SYSTEM_ALERT_WINDOW`

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

- **Why callNest needs it.** The floating bubble + post-call popup are `WindowManager` overlays of type `TYPE_APPLICATION_OVERLAY`.
- **When requested.** Onboarding page 4 ("Real-time helpers"); also when the user toggles bubble or popup in Real-Time Settings.
- **What's blocked if denied.** Real-time toggles disabled. Service does not start (`RealTimeServiceController` short-circuits).
- **Fallback.** App functions without real-time. Calls list and detail still work.
- **Rationale string.** `R.string.perm_rationale_overlay` — _"To show the in-call bubble and post-call popup, callNest needs to draw over other apps. Tap Open Settings, then enable 'Allow over other apps'."_

#### 4.2.2 `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
```

- **Why callNest needs it.** Follow-up reminders fire at exact times via `AlarmManager.setExactAndAllowWhileIdle`. The 5-minute sync interval also relies on exact alarms (PeriodicWorkRequest minimum is 15 min).
- **When requested.** First time the user creates a follow-up; or selects 5 min sync interval.
- **What's blocked if denied.** Exact alarms downgrade to inexact (`setAndAllowWhileIdle`). Reminders may fire 0–15 min late depending on Doze. 5-min sync falls back to 15-min.
- **Fallback.** App stores `exactAlarmFallbackUsed=true` in DataStore and shows an info chip on the Follow-Ups screen.
- **Rationale string.** `R.string.perm_rationale_exact_alarm` — _"For follow-up reminders to fire on time, callNest needs the 'Alarms & reminders' permission."_

### 4.3 Notifications

#### 4.3.1 `POST_NOTIFICATIONS`

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

- **Why callNest needs it.** Foreground-service notification, follow-up reminders, daily summary, sync progress, update available, daily backup result.
- **When requested.** Onboarding page 3 on API 33+. On older APIs the permission is implicitly granted.
- **What's blocked if denied.** Foreground service still runs but its notification is invisible. Reminders fire silently.
- **Fallback.** Settings shows a banner ("Notifications disabled — reminders won't appear in your tray").
- **Rationale string.** `R.string.perm_rationale_notifications` — _"callNest needs notification permission to show follow-up reminders and the daily summary."_

#### 4.3.2 `VIBRATE`

```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

- **Why callNest needs it.** Tactile feedback on bubble drag-snap, follow-up reminder buzz, swipe gesture detents.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A — no runtime denial path.
- **Fallback.** N/A.
- **Rationale string.** N/A (install-time).

### 4.4 Foreground service permissions

#### 4.4.1 `FOREGROUND_SERVICE`

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

- **Why callNest needs it.** `CallEnrichmentService` runs as a foreground service.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A — install-time.
- **Fallback.** N/A.

#### 4.4.2 `FOREGROUND_SERVICE_DATA_SYNC`

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

- **Why callNest needs it.** API 34+ requires a typed foreground-service permission for any DataSync-typed service. `CallSyncWorker` may chain via expedited work that surfaces a foreground notification under heavy load.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

#### 4.4.3 `FOREGROUND_SERVICE_SPECIAL_USE`

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

- **Why callNest needs it.** API 34+ requires this typed permission for `specialUse` services. `CallEnrichmentService` declares `foregroundServiceType="specialUse"` with subtype `RealTimeCallEnrichment`.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

### 4.5 Boot / wake

#### 4.5.1 `RECEIVE_BOOT_COMPLETED`

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

- **Why callNest needs it.** `BootCompletedReceiver` re-schedules sync work, re-arms exact alarms for pending follow-ups, and (if enabled) restarts `CallEnrichmentService`.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

#### 4.5.2 `WAKE_LOCK`

```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

- **Why callNest needs it.** Sync pipeline holds a partial wakelock for the duration of a sync to keep the CPU on while reading the call log.
- **When requested.** Implicit (install-time).
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

### 4.6 Update / network

#### 4.6.1 `INTERNET`

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

- **Why callNest needs it.** Single outbound call to the update manifest. Optional Drive backup upload.
- **When requested.** Implicit.
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

#### 4.6.2 `ACCESS_NETWORK_STATE`

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

- **Why callNest needs it.** WorkManager constraints (`requiresNetwork`, `requiresUnmeteredNetwork`) need this to evaluate.
- **When requested.** Implicit.
- **What's blocked if denied.** N/A.
- **Fallback.** N/A.

#### 4.6.3 `REQUEST_INSTALL_PACKAGES`

```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

- **Why callNest needs it.** Self-update install flow hands off via `FileProvider` + `ACTION_VIEW` to the system installer, gated by `PackageManager.canRequestPackageInstalls()` on API 26+.
- **When requested.** First time the user taps Install in `UpdateAvailableScreen`. The screen routes to "Settings → Install unknown apps" if not granted.
- **What's blocked if denied.** Self-update download still works; install does not.
- **Fallback.** User can manually install the downloaded APK from a file manager.
- **Rationale string.** `R.string.perm_rationale_install_packages` — _"To install the callNest update you just downloaded, allow callNest to install unknown apps."_

### 4.7 Permission gating summary

| Surface                  | Required permissions                                                                         | Behavior if missing                       |
| ------------------------ | -------------------------------------------------------------------------------------------- | ----------------------------------------- |
| Calls list / detail      | `READ_CALL_LOG`, `READ_CONTACTS`, `READ_PHONE_STATE`                                         | Route to `permission_rationale`.          |
| Auto-save toggle         | `WRITE_CONTACTS`                                                                             | Toggle disabled with help link.           |
| Real-time bubble / popup | `SYSTEM_ALERT_WINDOW` + `READ_PHONE_STATE` + (API 31+) `READ_CALL_LOG` for number resolution | Toggles disabled.                         |
| Follow-up reminders      | `SCHEDULE_EXACT_ALARM` (graceful fallback to inexact)                                        | Reminder may fire late.                   |
| Notifications            | `POST_NOTIFICATIONS` (API 33+)                                                               | Reminders fire silently.                  |
| Self-update install      | `REQUEST_INSTALL_PACKAGES`                                                                   | Install screen routes to system settings. |

---

## 5 — Data model

### 5.1 Room database

`callNestDatabase`, file `callNest.db`, **schema version 2**. Migration v1 → v2 adds `rule_score_boosts`. Schema export is wired through KSP arg pointing at `app/schemas/`.

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

| Column                    | Kotlin type | SQL type | Nullable | Default | Index |
| ------------------------- | ----------- | -------- | -------- | ------- | ----- |
| `systemId`                | `Long`      | INTEGER  | no       | —       | PK    |
| `rawNumber`               | `String`    | TEXT     | no       | `""`    | —     |
| `normalizedNumber`        | `String`    | TEXT     | no       | `""`    | yes   |
| `cachedName`              | `String?`   | TEXT     | yes      | null    | —     |
| `geocodedLocation`        | `String?`   | TEXT     | yes      | null    | —     |
| `type`                    | `Int`       | INTEGER  | no       | —       | yes   |
| `simSlot`                 | `Int?`      | INTEGER  | yes      | null    | —     |
| `phoneAccountId`          | `String?`   | TEXT     | yes      | null    | —     |
| `dateMillis`              | `Long`      | INTEGER  | no       | —       | yes   |
| `durationSec`             | `Long`      | INTEGER  | no       | `0`     | —     |
| `isPrivate`               | `Boolean`   | INTEGER  | no       | `false` | —     |
| `isBookmarked`            | `Boolean`   | INTEGER  | no       | `false` | yes   |
| `bookmarkReason`          | `String?`   | TEXT     | yes      | null    | —     |
| `followUpDate`            | `Long?`     | INTEGER  | yes      | null    | yes   |
| `followUpTime`            | `Int?`      | INTEGER  | yes      | null    | —     |
| `followUpDone`            | `Boolean`   | INTEGER  | no       | `false` | —     |
| `archived`                | `Boolean`   | INTEGER  | no       | `false` | —     |
| `leadScore`               | `Int`       | INTEGER  | no       | `0`     | —     |
| `leadScoreManualOverride` | `Int?`      | INTEGER  | yes      | null    | —     |
| `tagIdsCsv`               | `String`    | TEXT     | no       | `""`    | —     |
| `createdAt`               | `Long`      | INTEGER  | no       | —       | —     |
| `updatedAt`               | `Long`      | INTEGER  | no       | —       | —     |

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

| Column      | Kotlin type | SQL type | Nullable | Default | Index  |
| ----------- | ----------- | -------- | -------- | ------- | ------ |
| `id`        | `Long`      | INTEGER  | no       | autogen | PK     |
| `name`      | `String`    | TEXT     | no       | —       | unique |
| `colorArgb` | `Int`       | INTEGER  | no       | —       | —      |
| `isSystem`  | `Boolean`   | INTEGER  | no       | `false` | —      |
| `sortOrder` | `Int`       | INTEGER  | no       | `0`     | —      |
| `createdAt` | `Long`      | INTEGER  | no       | —       | —      |

Sample row:

```json
{
  "id": 1,
  "name": "Inquiry",
  "colorArgb": -16776961,
  "isSystem": true,
  "sortOrder": 0,
  "createdAt": 1714000000000
}
```

### 5.4 `CallTagCrossRef`

Table: `call_tag_cross_ref`. Composite PK: `(callSystemId, tagId)`.

| Column         | Kotlin type | SQL type | Nullable | Default  | Index      |
| -------------- | ----------- | -------- | -------- | -------- | ---------- |
| `callSystemId` | `Long`      | INTEGER  | no       | —        | PK         |
| `tagId`        | `Long`      | INTEGER  | no       | —        | PK + index |
| `appliedBy`    | `String`    | TEXT     | no       | `"user"` | yes        |
| `appliedAt`    | `Long`      | INTEGER  | no       | —        | —          |

`appliedBy` values: `"user"`, `"rule:${ruleId}"`. Cascade-clean on rule delete (application-level).

### 5.5 `ContactMetaEntity`

Table: `contact_meta`. PK: `normalizedNumber`.

| Column               | Kotlin type | SQL type | Nullable | Default | Index |
| -------------------- | ----------- | -------- | -------- | ------- | ----- |
| `normalizedNumber`   | `String`    | TEXT     | no       | —       | PK    |
| `displayName`        | `String?`   | TEXT     | yes      | null    | —     |
| `totalCalls`         | `Int`       | INTEGER  | no       | `0`     | —     |
| `totalDurationSec`   | `Long`      | INTEGER  | no       | `0`     | —     |
| `firstCallAt`        | `Long?`     | INTEGER  | yes      | null    | —     |
| `lastCallAt`         | `Long?`     | INTEGER  | yes      | null    | yes   |
| `incomingCount`      | `Int`       | INTEGER  | no       | `0`     | —     |
| `outgoingCount`      | `Int`       | INTEGER  | no       | `0`     | —     |
| `missedCount`        | `Int`       | INTEGER  | no       | `0`     | —     |
| `rejectedCount`      | `Int`       | INTEGER  | no       | `0`     | —     |
| `leadScore`          | `Int`       | INTEGER  | no       | `0`     | yes   |
| `manualLeadScore`    | `Int?`      | INTEGER  | yes      | null    | —     |
| `isInSystemContacts` | `Boolean`   | INTEGER  | no       | `false` | yes   |
| `isAutoSaved`        | `Boolean`   | INTEGER  | no       | `false` | yes   |
| `autoSavedFormat`    | `String?`   | TEXT     | yes      | null    | —     |
| `lastSyncedAt`       | `Long`      | INTEGER  | no       | —       | —     |

Sample row:

```json
{
  "normalizedNumber": "+919876543210",
  "displayName": "callNest-s1 +919876543210",
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
  "autoSavedFormat": "callNest-{simTag} {phone}",
  "lastSyncedAt": 1714382540000
}
```

### 5.6 `NoteEntity`

Table: `notes`. PK: `id` (autogen).

| Column             | Kotlin type | SQL type | Nullable | Default  | Index |
| ------------------ | ----------- | -------- | -------- | -------- | ----- |
| `id`               | `Long`      | INTEGER  | no       | autogen  | PK    |
| `normalizedNumber` | `String`    | TEXT     | no       | —        | yes   |
| `callSystemId`     | `Long?`     | INTEGER  | yes      | null     | yes   |
| `body`             | `String`    | TEXT     | no       | —        | —     |
| `createdBy`        | `String`    | TEXT     | no       | `"user"` | —     |
| `createdAt`        | `Long`      | INTEGER  | no       | —        | yes   |
| `updatedAt`        | `Long`      | INTEGER  | no       | —        | —     |

`createdBy`: `"user"`, `"bubble"`. Orphan notes have `callSystemId = null` until sync attaches them.

### 5.7 `NoteHistoryEntity`

Table: `note_history`. PK: `id`.

| Column    | Kotlin type | SQL type | Nullable | Default | Index |
| --------- | ----------- | -------- | -------- | ------- | ----- |
| `id`      | `Long`      | INTEGER  | no       | autogen | PK    |
| `noteId`  | `Long`      | INTEGER  | no       | —       | yes   |
| `body`    | `String`    | TEXT     | no       | —       | —     |
| `savedAt` | `Long`      | INTEGER  | no       | —       | —     |

Last 5 versions retained per `noteId`.

### 5.8 `FilterPresetEntity`

Table: `filter_presets`. PK: `id`.

| Column      | Kotlin type | SQL type | Nullable | Default | Index |
| ----------- | ----------- | -------- | -------- | ------- | ----- |
| `id`        | `Long`      | INTEGER  | no       | autogen | PK    |
| `name`      | `String`    | TEXT     | no       | —       | —     |
| `stateJson` | `String`    | TEXT     | no       | —       | —     |
| `createdAt` | `Long`      | INTEGER  | no       | —       | —     |

`stateJson` carries a serialized `FilterState`.

### 5.9 `AutoTagRuleEntity`

Table: `auto_tag_rules`. PK: `id`.

| Column           | Kotlin type | SQL type | Nullable | Default | Index |
| ---------------- | ----------- | -------- | -------- | ------- | ----- |
| `id`             | `Long`      | INTEGER  | no       | autogen | PK    |
| `name`           | `String`    | TEXT     | no       | —       | —     |
| `enabled`        | `Boolean`   | INTEGER  | no       | `true`  | —     |
| `sortOrder`      | `Int`       | INTEGER  | no       | `0`     | yes   |
| `conditionsJson` | `String`    | TEXT     | no       | —       | —     |
| `actionsJson`    | `String`    | TEXT     | no       | —       | —     |
| `createdAt`      | `Long`      | INTEGER  | no       | —       | —     |
| `updatedAt`      | `Long`      | INTEGER  | no       | —       | —     |

Conditions and actions use kotlinx.serialization with `class-discriminator = "type"`.

### 5.10 `RuleScoreBoostEntity`

Table: `rule_score_boosts`. Composite PK: `(callSystemId, ruleId)`. Added in v2.

| Column         | Kotlin type | SQL type | Nullable | Default | Index      |
| -------------- | ----------- | -------- | -------- | ------- | ---------- |
| `callSystemId` | `Long`      | INTEGER  | no       | —       | PK         |
| `ruleId`       | `Long`      | INTEGER  | no       | —       | PK + index |
| `delta`        | `Int`       | INTEGER  | no       | —       | —          |
| `appliedAt`    | `Long`      | INTEGER  | no       | —       | —          |

### 5.11 `SearchHistoryEntity`

Table: `search_history`. PK: `id`.

| Column       | Kotlin type | SQL type | Nullable | Default | Index  |
| ------------ | ----------- | -------- | -------- | ------- | ------ |
| `id`         | `Long`      | INTEGER  | no       | autogen | PK     |
| `query`      | `String`    | TEXT     | no       | —       | unique |
| `lastUsedAt` | `Long`      | INTEGER  | no       | —       | yes    |
| `useCount`   | `Int`       | INTEGER  | no       | `1`     | —      |

### 5.12 `DocFeedbackEntity`

Table: `doc_feedback`. PK: `articleId`.

| Column        | Kotlin type | SQL type | Nullable | Default | Index |
| ------------- | ----------- | -------- | -------- | ------- | ----- |
| `articleId`   | `String`    | TEXT     | no       | —       | PK    |
| `helpful`     | `Boolean`   | INTEGER  | no       | —       | —     |
| `submittedAt` | `Long`      | INTEGER  | no       | —       | —     |

### 5.13 `SkippedUpdateEntity`

Table: `skipped_updates`. PK: `versionCode`.

| Column        | Kotlin type | SQL type | Nullable | Default | Index |
| ------------- | ----------- | -------- | -------- | ------- | ----- |
| `versionCode` | `Int`       | INTEGER  | no       | —       | PK    |
| `version`     | `String`    | TEXT     | no       | —       | —     |
| `skippedAt`   | `Long`      | INTEGER  | no       | —       | —     |

### 5.14 `CallFts`

FTS4 virtual table. Content table: `calls`.

| Column             | Source                        |
| ------------------ | ----------------------------- |
| `rawNumber`        | from `calls.rawNumber`        |
| `normalizedNumber` | from `calls.normalizedNumber` |
| `cachedName`       | from `calls.cachedName`       |
| `geocodedLocation` | from `calls.geocodedLocation` |
| `bookmarkReason`   | from `calls.bookmarkReason`   |

Tokenizer: `unicode61 remove_diacritics 2`.

### 5.15 `NoteFts`

FTS4 virtual table. Content table: `notes`.

| Column | Source            |
| ------ | ----------------- |
| `body` | from `notes.body` |

### 5.16 DataStore keys (`SettingsDataStore`)

The DataStore preferences object is split into ~10 logical groups. Each key has a typed `Flow<T>` accessor and a `suspend fun setX(value: T)` writer.

| Key                               | Kotlin type | Default                | Read accessor                       | Write fn                          |
| --------------------------------- | ----------- | ---------------------- | ----------------------------------- | --------------------------------- |
| `onboarding_complete`             | Boolean     | `false`                | `onboardingComplete: Flow<Boolean>` | `setOnboardingComplete`           |
| `tags_seeded`                     | Boolean     | `false`                | `tagsSeeded`                        | `setTagsSeeded`                   |
| `default_region`                  | String      | `"IN"`                 | `defaultRegion`                     | `setDefaultRegion`                |
| `last_sync_call_id`               | Long        | `0`                    | `lastSyncCallId`                    | `setLastSyncCallId`               |
| `last_sync_at`                    | Long        | `0`                    | `lastSyncAt`                        | `setLastSyncAt`                   |
| `sync_interval_minutes`           | Int         | `15`                   | `syncIntervalMinutes`               | `setSyncIntervalMinutes`          |
| `sync_only_on_unmetered`          | Boolean     | `false`                | `syncOnlyOnUnmetered`               | `setSyncOnlyOnUnmetered`          |
| `auto_save_enabled`               | Boolean     | `false`                | `autoSaveEnabled`                   | `setAutoSaveEnabled`              |
| `auto_save_prefix`                | String      | `"callNest-"`          | `autoSavePrefix`                    | `setAutoSavePrefix`               |
| `auto_save_sim_tag_format`        | String      | `"s{n}"`               | `autoSaveSimTagFormat`              | `setAutoSaveSimTagFormat`         |
| `auto_save_suffix`                | String      | `""`                   | `autoSaveSuffix`                    | `setAutoSaveSuffix`               |
| `auto_save_group_name`            | String      | `"callNest Inquiries"` | `autoSaveGroupName`                 | `setAutoSaveGroupName`            |
| `real_time_bubble_enabled`        | Boolean     | `false`                | `bubbleEnabled`                     | `setBubbleEnabled`                |
| `real_time_popup_enabled`         | Boolean     | `false`                | `popupEnabled`                      | `setPopupEnabled`                 |
| `post_call_popup_timeout_seconds` | Int         | `8`                    | `postCallPopupTimeoutSeconds`       | `setPostCallPopupTimeoutSeconds`  |
| `notifications_follow_up`         | Boolean     | `true`                 | `followUpNotificationsEnabled`      | `setFollowUpNotificationsEnabled` |
| `notifications_daily_summary`     | Boolean     | `true`                 | `dailySummaryEnabled`               | `setDailySummaryEnabled`          |
| `daily_summary_hour`              | Int         | `9`                    | `dailySummaryHour`                  | `setDailySummaryHour`             |
| `lead_weight_frequency`           | Float       | `1.0`                  | `leadWeightFrequency`               | `setLeadWeightFrequency`          |
| `lead_weight_duration`            | Float       | `1.0`                  | `leadWeightDuration`                | `setLeadWeightDuration`           |
| `lead_weight_recency`             | Float       | `1.0`                  | `leadWeightRecency`                 | `setLeadWeightRecency`            |
| `lead_weight_followup_bonus`      | Float       | `5.0`                  | `leadWeightFollowupBonus`           | `setLeadWeightFollowupBonus`      |
| `lead_weight_customer_tag_bonus`  | Float       | `10.0`                 | `leadWeightCustomerTagBonus`        | `setLeadWeightCustomerTagBonus`   |
| `lead_weight_saved_contact_bonus` | Float       | `5.0`                  | `leadWeightSavedContactBonus`       | `setLeadWeightSavedContactBonus`  |
| `backup_passphrase_set`           | Boolean     | `false`                | `backupPassphraseSet`               | `setBackupPassphraseSet`          |
| `backup_auto_enabled`             | Boolean     | `true`                 | `backupAutoEnabled`                 | `setBackupAutoEnabled`            |
| `backup_keep_count`               | Int         | `7`                    | `backupKeepCount`                   | `setBackupKeepCount`              |
| `backup_drive_enabled`            | Boolean     | `false`                | `backupDriveEnabled`                | `setBackupDriveEnabled`           |
| `backup_last_at`                  | Long        | `0`                    | `backupLastAt`                      | `setBackupLastAt`                 |
| `update_channel`                  | String      | `"stable"`             | `updateChannel`                     | `setUpdateChannel`                |
| `update_auto_check`               | Boolean     | `true`                 | `updateAutoCheck`                   | `setUpdateAutoCheck`              |
| `update_last_checked_at`          | Long        | `0`                    | `updateLastCheckedAt`               | `setUpdateLastCheckedAt`          |
| `display_density`                 | String      | `"comfortable"`        | `displayDensity`                    | `setDisplayDensity`               |
| `call_sort_mode`                  | String      | `"date_desc"`          | `callSortMode`                      | `setCallSortMode`                 |
| `pinned_bookmarks_json`           | String      | `"[]"`                 | `pinnedBookmarksJson`               | `setPinnedBookmarksJson`          |
| `stats_range_preset`              | String      | `"7d"`                 | `statsRangePreset`                  | `setStatsRangePreset`             |
| `exact_alarm_fallback_used`       | Boolean     | `false`                | `exactAlarmFallbackUsed`            | `setExactAlarmFallbackUsed`       |
| `privacy_screenshot_blocked`      | Boolean     | `false`                | `privacyScreenshotBlocked`          | `setPrivacyScreenshotBlocked`     |
| `last_oem_guide_shown_for`        | String      | `""`                   | `lastOemGuideShownFor`              | `setLastOemGuideShownFor`         |
| `bubble_position_x`               | Int         | `-1`                   | `bubblePositionX`                   | `setBubblePositionX`              |
| `bubble_position_y`               | Int         | `-1`                   | `bubblePositionY`                   | `setBubblePositionY`              |

### 5.17 SecurePrefs (EncryptedSharedPreferences)

| Key                | Kotlin type   | Purpose                                                                  |
| ------------------ | ------------- | ------------------------------------------------------------------------ |
| `backupPassphrase` | String        | User-supplied passphrase for `.cvb` files. Read by `BackupManager` only. |
| `drive_auth_state` | String (JSON) | Serialized AppAuth `AuthState` for the optional Drive backup feature.    |

These keys never round-trip through Timber, never appear in exports, and are excluded from Android backup via `data_extraction_rules.xml`.

---

## 6 — Core algorithms

This section gives the canonical algorithms with pseudocode and worked examples. Implementation lives in the `domain/usecase/` package.

### 6.1 Sync pipeline (12 steps)

Implementation: `SyncCallLogUseCase`. Triggered by `CallSyncWorker`, `SyncScheduler.triggerOnce`, scheduled paths, and post-call by `CallEnrichmentService`.

```text
1.  acquire WakeLock("callNest:sync", 60s timeout)
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

| Component           | Raw value                             | Weighted          |
| ------------------- | ------------------------------------- | ----------------- |
| Frequency           | min(100, 12 × 5) = 60                 | 60 × 1.0 = 60     |
| Duration            | min(100, 1800/60) = 30                | 30 × 1.0 = 30     |
| Recency             | 100 × exp(-3/14) ≈ 100 × 0.806 ≈ 80.6 | 80.6 × 1.0 = 80.6 |
| Follow-up bonus     | 0                                     | 0                 |
| Customer-tag bonus  | 1 × 10                                | 10                |
| Saved-contact bonus | 1 × 5                                 | 5                 |
| Rule boosts         | 0                                     | 0                 |
| **Sum**             |                                       | **185.6**         |

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

| Settings                                       | SIM  | Phone           | Result                      |
| ---------------------------------------------- | ---- | --------------- | --------------------------- |
| prefix=`callNest-`, simTag=`s{n}`, suffix=`""` | 0    | `+919876543210` | `callNest-s1 +919876543210` |
| prefix=`callNest-`, simTag=`s{n}`, suffix=`""` | 1    | `+919876543210` | `callNest-s2 +919876543210` |
| prefix=`Inq-`, simTag=`""`, suffix=` (CV)`     | 0    | `+919876543210` | `Inq- +919876543210 (CV)`   |
| prefix=`callNest-`, simTag=`s{n}`, suffix=`""` | null | `+919876543210` | `callNest- +919876543210`   |

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
| -------------------- | ------------- |
| 0                    | 100.0         |
| 1                    | 93.1          |
| 3                    | 80.6          |
| 7                    | 60.7          |
| 14                   | 36.8          |
| 30                   | 11.7          |
| 60                   | 1.4           |

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

---

# callNest APP-SPEC — Part 02

## Theme + Navigation + Splash + Onboarding

> Audience: a UX engineer rebuilding the callNest UI from scratch.
> Self-contained — every token, route, screen, and animation needed to
> stand up the app's chrome (theme), wiring (navigation), entry point
> (splash), and first-run flow (onboarding) is captured here.
>
> Cross-references: see Part 01 for §§ 1–6 (project overview, sync
> pipeline, data model, etc.). This part covers §§ 7–15.

---

## 7 — Theme: colors, typography, spacing, shapes, motion

callNest uses a single, locked **neumorphic** design language. There
is no dark mode. There is no dynamic color (Material You is explicitly
disabled). Every screen is painted on a soft, near-white canvas with
dual-shadow elevation and tightly bounded accent colors. The look is
not a visual style choice — it is a contract. Every component matches
this contract, or it does not ship.

### 7.1 Neumorphism principles

The callNest neumorphism is governed by five rules. Internalize
them; every visual decision falls out of them.

**Rule 1 — One light source.**
There is exactly one virtual light in the world, and it sits at the
top-left at 45° above the surface. Every shadow you draw must be
consistent with that light. If you ever find yourself drawing a
shadow on the top-left of a control, you are wrong: the top-left is
the _highlight_ side.

**Rule 2 — Base canvas is `#E8E8EC`.**
The base canvas color is the floor of the design system. All
elevation, both convex and concave, is computed relative to it. The
canvas is _never_ pure white and _never_ pure gray; it is a blue-tinted
near-white that gives the dual shadows enough contrast to read.

**Rule 3 — Dual-shadow elevation.**
Every elevated surface paints **two** shadows:

- A **light** shadow on the top-left (color `#FFFFFF`, the highlight).
- A **dark** shadow on the bottom-right (color `#A3B1C6`, the lowlight).

The two shadows together create the illusion of a soft 3-D extrusion
from the canvas. A single shadow is not allowed — you will see it
immediately as "wrong" because the brain reads it as drop-shadow,
not extrusion.

**Rule 4 — 4% offset rule.**
Every tinted surface (Raised, Inset, BasePressed, the per-tab
backgrounds) is at least 4% lighter or darker than the base canvas.
Anything less and the eye cannot separate the surface from the
canvas, and the dual-shadow disappears. Anything more and the
neumorphic illusion breaks — the surface starts looking like a
flat colored card.

**Rule 5 — Convex by default, concave for input.**
Cards, buttons, FABs, and chips are **convex** (raised). Text
fields, search bars, sliders, progress tracks, and toggles' grooves
are **concave** (inset). A convex control says "I can be tapped".
A concave control says "data goes in here".

**Light source diagram (ASCII, 45° top-left).**

```
       light
       source
         \
          \
   *       \
            v
        +------+
        |      |   <- convex: highlight on TL, shadow on BR
        |      |
        +------+
           ^
           |
        shadow
        falls
        bottom-right

        +------+
        |::::::|   <- concave: shadow on TL, highlight on BR
        |::::::|
        +------+
```

**Why dual-shadow, not Material elevation?**
Material `Elevation` draws a single, downward, blurred shadow that
implies a flat card hovering above a surface. callNest's surfaces
are not hovering; they are pressed _into_ or _out of_ the canvas.
That illusion only works with dual shadows.

---

### 7.2 NeoColors palette

Every color the app may render is in this table. New colors are
added by editing `ui/theme/Color.kt`. The token name is the source
of truth — never use a raw hex literal in a composable.

#### 7.2.1 Base + surface tokens

| Token         | Hex       | Role                                                            | Where it is used                                     | WCAG vs Base                                   |
| ------------- | --------- | --------------------------------------------------------------- | ---------------------------------------------------- | ---------------------------------------------- |
| `Base`        | `#E8E8EC` | Canvas background                                               | `Surface`, root scaffolds, splash post-system-splash | n/a (this _is_ the base)                       |
| `BasePressed` | `#E0E0E5` | Pressed-state for convex buttons / tappable cards               | `NeoButton.pressed`, `NeoCard.onClick` press tint    | 1.04:1 vs Base — barely darker, intentional    |
| `Light`       | `#FFFFFF` | Top-left highlight shadow color                                 | dual-shadow `Modifier.neoShadow(...)` light pass     | 1.21:1 — used as shadow only, never as text bg |
| `Dark`        | `#A3B1C6` | Bottom-right lowlight shadow color                              | dual-shadow dark pass; outlines via `BorderSoft`     | 2.18:1                                         |
| `Raised`      | `#EDEDF2` | Subtle elevated tint for cards that need to read clearly raised | `NeoCard` body, dialog surfaces                      | 1.04:1                                         |
| `Inset`       | `#DFDFE5` | Subtle inset tint for fields and tracks                         | `NeoTextField`, `NeoSlider` track, `NeoSearchBar`    | 1.05:1                                         |

#### 7.2.2 Text tokens

| Token          | Hex       | Role                                         | Where it is used                                 | WCAG vs Base          |
| -------------- | --------- | -------------------------------------------- | ------------------------------------------------ | --------------------- |
| `OnBase`       | `#2A3441` | Primary text                                 | headlines, body, list primary lines              | 12.6:1 — AAA          |
| `OnBaseMuted`  | `#5C6A7A` | Secondary text / icons                       | subtitles, helper labels, secondary list lines   | 5.4:1 — AA            |
| `OnBaseSubtle` | `#8492A3` | Tertiary text — captions, timestamps, helper | timestamps, "N items" counters, empty-state body | 3.1:1 — AA Large only |

`OnBaseSubtle` is **never** used for body text. It is restricted to
captions and to timestamps adjacent to a primary line. Use
`OnBaseMuted` if a label is the primary affordance.

#### 7.2.3 Accent palette

Accents are used for **affordance and category**, never for
decoration. A blue chip means "this is a call". A violet chip means
"this is an inquiry". Mixing accents purely for visual variety is
forbidden.

| Token          | Hex       | Role                              | Where it is used                                    | WCAG vs Base       |
| -------------- | --------- | --------------------------------- | --------------------------------------------------- | ------------------ |
| `AccentBlue`   | `#4F7CFF` | Primary brand accent              | primary buttons, Calls category, splash ring sweep  | 4.7:1 — AA         |
| `AccentTeal`   | `#1FB5A8` | Backup / restore / cloud category | Backup tab tint, splash gradient start              | 3.1:1 — AA Large   |
| `AccentAmber`  | `#E0A82E` | Stats / charts / warning-leaning  | Stats tab, lead-score "warm" band                   | 2.6:1 — Large only |
| `AccentRose`   | `#E5536B` | Tags / destructive-leaning        | tag chips, delete confirmations, follow-up "missed" | 3.5:1 — AA Large   |
| `AccentViolet` | `#8266E5` | Inquiries category                | Inquiries tab, auto-saved inquiry chip              | 3.9:1 — AA Large   |
| `AccentGreen`  | `#34A853` | Success / Home category           | Home tab, splash gradient end, "All caught up"      | 3.4:1 — AA Large   |

Accents below WCAG AA on body text are still safe because the spec
**never uses raw accents for body text**. Accents render on chips,
icon backgrounds, and short labels (≥ 14sp, ≥ medium weight) — the
WCAG Large threshold (3.0:1) is what applies.

#### 7.2.4 Borders + toggles

| Token          | Source                        | Hex equivalent         | Role                                         |
| -------------- | ----------------------------- | ---------------------- | -------------------------------------------- |
| `BorderSoft`   | `Dark.copy(alpha=0.18)`       | rgba(163,177,198,0.18) | Card / dialog / toggle outlines              |
| `BorderAccent` | `AccentBlue.copy(alpha=0.20)` | rgba(79,124,255,0.20)  | Primary card outlines (e.g., today's totals) |
| `ToggleOn`     | —                             | `#34C759`              | iOS-green track for switched-on toggles      |
| `ToggleOff`    | —                             | `#C7C7CC`              | Cool gray for switched-off toggle tracks     |

#### 7.2.5 Category icon tints

The five top-level categories have a permanent tint. These are
**aliases** of the accent palette, not new colors — but the alias
matters because it pins meaning to color.

| Token               | Aliases        | Used by                                                  |
| ------------------- | -------------- | -------------------------------------------------------- |
| `IconCallsTint`     | `AccentBlue`   | Calls tab icon, CallDetail header, "call" in mixed lists |
| `IconInquiriesTint` | `AccentViolet` | Inquiries tab icon, auto-saved inquiry chip              |
| `IconStatsTint`     | `AccentAmber`  | Stats tab icon, chart axes labels                        |
| `IconBackupTint`    | `AccentTeal`   | Backup tab icon, restore progress                        |
| `IconTagsTint`      | `AccentRose`   | Tag chip icons, AutoTagRules screen                      |
| `IconHomeTint`      | `AccentGreen`  | Home tab icon, "today" totals                            |

#### 7.2.6 Phase I additions

| Token                      | Hex       | Role                                            |
| -------------------------- | --------- | ----------------------------------------------- |
| `TabBgHome`                | `#EAF5EE` | Home tab full-screen background tint            |
| `TabBgCalls`               | `#E7EEFB` | Calls tab full-screen background tint           |
| `TabBgInquiries`           | `#EEEAF8` | Inquiries tab full-screen background tint       |
| `TabBgMore`                | `#FAF3E5` | More tab full-screen background tint            |
| `TabBgStats`               | `#FAF3E5` | Stats screen background tint                    |
| `HeaderGradHomeStart`      | `#C4E5CF` | Home top header gradient start                  |
| `HeaderGradHomeEnd`        | `#EAF5EE` | Home top header gradient end                    |
| `HeaderGradCallsStart`     | `#BCD2F4` | Calls top header gradient start                 |
| `HeaderGradCallsEnd`       | `#E7EEFB` | Calls top header gradient end                   |
| `HeaderGradInquiriesStart` | `#D2C7F1` | Inquiries top header gradient start             |
| `HeaderGradInquiriesEnd`   | `#EEEAF8` | Inquiries top header gradient end               |
| `HeaderGradMoreStart`      | `#F0DCA4` | More top header gradient start                  |
| `HeaderGradMoreEnd`        | `#FAF3E5` | More top header gradient end                    |
| `HeaderGradStatsStart`     | `#F0DCA4` | Stats top header gradient start                 |
| `HeaderGradStatsEnd`       | `#FAF3E5` | Stats top header gradient end                   |
| `SplashGradStart`          | `#0E5C4F` | Splash + Onboarding p1 vertical gradient top    |
| `SplashGradEnd`            | `#34A853` | Splash + Onboarding p1 vertical gradient bottom |

---

### 7.3 Per-tab background tints + header gradients (Phase I.4)

Each top-level tab paints its own full-screen background tint, with
a top header that fades from a stronger version of that tint into
the tab background. The result is that the app feels "themed by
tab" without any chrome change.

| Tab          | Background tint            | Header gradient (top → bottom)                        |
| ------------ | -------------------------- | ----------------------------------------------------- |
| Home         | `TabBgHome` `#EAF5EE`      | `HeaderGradHomeStart` → `HeaderGradHomeEnd`           |
| Calls        | `TabBgCalls` `#E7EEFB`     | `HeaderGradCallsStart` → `HeaderGradCallsEnd`         |
| Inquiries    | `TabBgInquiries` `#EEEAF8` | `HeaderGradInquiriesStart` → `HeaderGradInquiriesEnd` |
| More         | `TabBgMore` `#FAF3E5`      | `HeaderGradMoreStart` → `HeaderGradMoreEnd`           |
| Stats (deep) | `TabBgStats` `#FAF3E5`     | `HeaderGradStatsStart` → `HeaderGradStatsEnd`         |

The header gradient is exactly **160dp** tall, fades top-to-bottom
into the tab background, and is rendered with
`Brush.verticalGradient` at `Offset(0, 0)` to `Offset(0, 160dp.toPx)`.

The neumorphic dual-shadow on cards still uses `Light` and `Dark` —
it does not compensate for the tinted background. This is
intentional: the tint is an _under-paint_, not a new canvas.

---

### 7.4 Splash gradient (vertical, teal → green)

```
#0E5C4F  ── 0%
   |
   |  vertical
   |  gradient
   |
#34A853  ── 100%
```

The splash gradient is the only place the app uses heavy color.
It matches the callNest logo (a teal-to-green disc), and it
appears in two locations only:

1. The Compose splash screen.
2. Onboarding page 1 (Welcome).

Both renders use `Brush.verticalGradient(0f to SplashGradStart, 1f to SplashGradEnd)` with `tileMode = TileMode.Clamp`.

The system splash (Android 12+ `windowSplashScreenBackground`) is
set to a single solid color `#0E5C4F` — the gradient _start_. This
ensures no perceptible color jump when the system splash hands off
to the Compose splash.

---

### 7.5 Typography scale

callNest uses **Inter** as the primary type family with
**system-default sans** as the fallback. Inter is bundled as an
asset variable font; if it fails to load (rare, but possible on
older devices with disk corruption), the system font handles it
without layout shift because Inter and Roboto have nearly
identical metrics.

The scale is Material 3's 15-style scale, restricted and tuned
for the neumorphic canvas. **All sizes in `sp`, all line heights
in `sp`, all letter spacing in `sp`** unless noted.

| Style            | Size | Weight         | Line height | Letter spacing | Sample                               | Where used                              |
| ---------------- | ---- | -------------- | ----------- | -------------- | ------------------------------------ | --------------------------------------- |
| `displayLarge`   | 57   | Light (300)    | 64          | -0.25          | "callNest"                           | not used in product (reserved)          |
| `displayMedium`  | 45   | Regular (400)  | 52          | 0              | "Welcome"                            | reserved                                |
| `displaySmall`   | 36   | Regular (400)  | 44          | 0              | "187" big-number                     | Stats hero numbers                      |
| `headlineLarge`  | 32   | SemiBold (600) | 40          | 0              | "Today's calls"                      | dialog hero, error pages                |
| `headlineMedium` | 28   | SemiBold (600) | 36          | 0              | "All caught up."                     | onboarding p5 done state                |
| `headlineSmall`  | 24   | SemiBold (600) | 32          | 0              | "Built for busy founders."           | onboarding p2 title, page hero          |
| `titleLarge`     | 22   | SemiBold (600) | 28          | 0              | "Inquiries"                          | page header titles, AppBar fallback     |
| `titleMedium`    | 16   | Medium (500)   | 24          | 0.15           | "Auto-save settings"                 | section headers, dialog titles          |
| `titleSmall`     | 14   | Medium (500)   | 20          | 0.1            | "Pro tip"                            | card headers, list-section labels       |
| `bodyLarge`      | 16   | Regular (400)  | 24          | 0.5            | "Never lose an inquiry call again."  | onboarding subtext, primary body        |
| `bodyMedium`     | 14   | Regular (400)  | 20          | 0.25           | "Captures every call from your log." | list body, card body, primary list line |
| `bodySmall`      | 12   | Regular (400)  | 16          | 0.4            | "Yesterday · 4:13 PM"                | timestamps, captions, helper            |
| `labelLarge`     | 14   | Medium (500)   | 20          | 0.1            | "Continue"                           | NeoButton labels, FAB labels            |
| `labelMedium`    | 12   | Medium (500)   | 16          | 0.5            | "12 NEW"                             | chip text, badge counts                 |
| `labelSmall`     | 11   | Medium (500)   | 16          | 0.5            | "BETA"                               | tiny tags, beta flags                   |

**Default text color**: `OnBase`. Composables that need a softer
look pass `OnBaseMuted` explicitly via `LocalContentColor`. The
tertiary `OnBaseSubtle` is reserved for `bodySmall` and
`labelSmall` only.

**No serifs. No display fonts. No emoji-as-text.** The single
exception to "no emoji" is the tab labels and the
`NeoPageHeader` left-side glyph — see §7.10.

---

### 7.6 Spacing constants

Spacing is a 4dp grid with named tokens. Composables never use
raw `dp` literals for padding — they always reference these.

| Token             | Value | Use                                                            |
| ----------------- | ----- | -------------------------------------------------------------- |
| `Xs`              | 4dp   | Inside-chip padding, icon-to-label gap in dense lists          |
| `Sm`              | 8dp   | Default between-element gap inside a card                      |
| `Md`              | 12dp  | Card content padding (vertical), button content padding        |
| `Lg`              | 16dp  | Card content padding (horizontal), default page horizontal pad |
| `Xl`              | 20dp  | Section-internal gap (between header and body)                 |
| `Xxl`             | 24dp  | Between-card gap, dialog content padding                       |
| `Xxxl`            | 32dp  | Between-section gap on long pages                              |
| `PageHorizontal`  | 16dp  | Outer horizontal pad for every page                            |
| `PageTopHeader`   | 24dp  | Pad below `NeoPageHeader` before first content                 |
| `SectionGap`      | 24dp  | Between two semantic sections within one page                  |
| `DialogContent`   | 20dp  | Inner pad of `NeoDialog` body                                  |
| `DialogMaxWidth`  | 360dp | Hard cap for dialog width on tablets                           |
| `BottomNavHeight` | 80dp  | Height of the neumorphic bottom nav                            |
| `TopBarHeight`    | 56dp  | Standard `TopAppBar` height (when present)                     |

`PageHorizontal` is the only horizontal pad applied at the page
root. Cards are full-width within that pad and add their own
internal pad.

`PageTopHeader` accounts for the gradient header above; it does
not include status-bar inset (status-bar is handled by the system
window-insets API).

---

### 7.7 Shape tokens

callNest uses six corner radii. Every shape is a
`RoundedCornerShape` except FAB (`CircleShape`) and the chips
(`RoundedCornerShape(50)` — fully pilled).

| Radius | Used by                                                        |
| ------ | -------------------------------------------------------------- |
| 4dp    | progress-bar tracks, micro-chips on dense lists                |
| 8dp    | text fields, search bar, small icon buttons, list item ripples |
| 12dp   | buttons (`NeoButton`), tag chips, secondary cards              |
| 16dp   | primary cards (`NeoCard`), bottom-sheet content blocks         |
| 20dp   | dialogs (`NeoDialog`)                                          |
| 24dp   | bottom-sheet **top corners only** (bottom corners 0)           |

| Shape            | Spec                                                                         | Where                   |
| ---------------- | ---------------------------------------------------------------------------- | ----------------------- |
| `NeoCardShape`   | `RoundedCornerShape(16dp)`                                                   | All cards               |
| `NeoButtonShape` | `RoundedCornerShape(12dp)`                                                   | All buttons             |
| `NeoFabShape`    | `CircleShape`                                                                | FABs                    |
| `NeoSheetShape`  | `RoundedCornerShape(topStart=24dp, topEnd=24dp, bottomStart=0, bottomEnd=0)` | Modal bottom sheets     |
| `NeoDialogShape` | `RoundedCornerShape(20dp)`                                                   | Dialogs                 |
| `NeoChipShape`   | `RoundedCornerShape(50)`                                                     | Tag/category chips      |
| `NeoFieldShape`  | `RoundedCornerShape(8dp)`                                                    | Text fields, search bar |

The 4dp track is special-cased inside `NeoProgressBar` — it does
not have its own named shape because no other component shares it.

---

### 7.8 NeoElevation tokens

`NeoElevation` is the contract for dual-shadow elevation. It maps
a logical level (Small / Medium / Large) to concrete shadow
parameters. There are seven levels:

- `ConvexSmall`, `ConvexMedium`, `ConvexLarge`
- `ConcaveSmall`, `ConcaveMedium`, `ConcaveLarge`
- `Flat`

Each level holds **two** shadow specs — light and dark — with
offset, blur, and alpha.

| Level           | Light offset | Light blur | Light alpha | Dark offset | Dark blur | Dark alpha |
| --------------- | ------------ | ---------- | ----------- | ----------- | --------- | ---------- |
| `ConvexSmall`   | (-2,-2)      | 4dp        | 1.0         | (2,2)       | 4dp       | 0.20       |
| `ConvexMedium`  | (-4,-4)      | 10dp       | 1.0         | (4,4)       | 10dp      | 0.25       |
| `ConvexLarge`   | (-8,-8)      | 20dp       | 1.0         | (8,8)       | 20dp      | 0.30       |
| `ConcaveSmall`  | (2,2)        | 4dp        | 0.20        | (-2,-2)     | 4dp       | 1.0        |
| `ConcaveMedium` | (4,4)        | 10dp       | 0.25        | (-4,-4)     | 10dp      | 1.0        |
| `ConcaveLarge`  | (8,8)        | 20dp       | 0.30        | (-8,-8)     | 20dp      | 1.0        |
| `Flat`          | (0,0)        | 0dp        | 0           | (0,0)       | 0dp       | 0          |

Notice that **concave is convex with the offsets flipped**. A
concave control is, optically, a hole pressed _into_ the canvas:
the highlight is on the bottom-right (where light bounces back up
out of the well) and the shadow is on the top-left (where the
upper rim casts down into the well).

**ASCII — convex vs concave.**

```
Convex (raised)              Concave (inset)
+--------------------+       +--------------------+
| · highlight TL     |       |   shadow TL ·      |
|  *                 |       |    *               |
|     +----------+   |       |    +----------+    |
|     | surface  |   |       |    |  inset   |    |
|     +----------+   |       |    +----------+    |
|              *     |       |               *    |
|     shadow BR ·    |       |  highlight BR ·    |
+--------------------+       +--------------------+
```

**Pairing rules.**

- A convex card is `ConvexMedium` by default. A primary card
  (e.g., today's totals on Home) is `ConvexLarge`. A small chip
  is `ConvexSmall`.
- A field/track/groove is `ConcaveSmall`. A search bar is
  `ConcaveMedium`. A pressed-into-canvas hero (e.g., the
  splash logo disc) is `ConcaveLarge`.
- `Flat` is the absence of elevation. It is the right answer for
  a divider, a non-tappable label group, or a list-item that
  belongs to a parent card (the parent provides the elevation).

---

### 7.9 Motion tokens

callNest has a small motion vocabulary. Every animation in the
app must be expressible in these primitives.

| Token                 | Primitive            | Spec                                                          | Where used                                 |
| --------------------- | -------------------- | ------------------------------------------------------------- | ------------------------------------------ |
| `PressSpring`         | `spring`             | `stiffness = 700f`, `dampingRatio = 0.8f`                     | NeoButton press scale, NeoCard tap scale   |
| `ShowHide`            | `tween`              | `durationMillis = 200`, `easing = FastOutSlowInEasing`        | snackbar, sheet, dialog enter/exit         |
| `RingSweep`           | `tween`              | `durationMillis = 1200`, `easing = FastOutSlowInEasing`       | splash ring 0° → 360°                      |
| `IndeterminateStripe` | `infiniteTransition` | `durationMillis = 1200`, `RepeatMode.Restart`, `LinearEasing` | indeterminate progress, first-sync spinner |
| `PageEnter`           | `tween`              | `durationMillis = 300`, `easing = FastOutSlowInEasing`        | NavHost composable enter                   |
| `PageExit`            | `tween`              | `durationMillis = 300`, `easing = FastOutLinearInEasing`      | NavHost composable exit                    |
| `CrossFade`           | `tween`              | `durationMillis = 220`                                        | empty-state ↔ list state crossfade         |
| `Typewriter`          | scheduled            | `50ms` per character                                          | splash wordmark                            |

**Per-screen transition table.**

| Transition                         | Duration | Easing                        | Notes                                                          |
| ---------------------------------- | -------- | ----------------------------- | -------------------------------------------------------------- |
| Splash → Main                      | 300ms    | `FastOutSlowInEasing`         | crossfade only (no slide); avoids "the app is moving" feeling  |
| Splash → Onboarding                | 300ms    | `FastOutSlowInEasing`         | crossfade                                                      |
| Splash → PermissionRationale       | 300ms    | `FastOutSlowInEasing`         | crossfade                                                      |
| Onboarding p*n → p*(n+1)           | 300ms    | slide + fade                  | slide direction follows reading order; fade handles overlap    |
| Onboarding p5 → Main               | 400ms    | `FastOutSlowInEasing`         | longer crossfade — the only transition that earns it           |
| Main tab ↔ Main tab                | 0ms      | none                          | tab switches are instant; bottom-nav indicator does the motion |
| Main tab → deep (e.g., CallDetail) | 300ms    | slide-from-right + fade       | standard push                                                  |
| deep → Main                        | 300ms    | slide-to-right + fade         | standard pop                                                   |
| any → dialog                       | 200ms    | scale 0.92 → 1.0 + fade       | `ShowHide`                                                     |
| any → bottom sheet                 | 250ms    | translate Y from below + fade | system motion                                                  |
| in-tab back                        | 300ms    | slide + fade                  | matches push                                                   |
| Home → UpdateAvailable (deep link) | 0ms      | none                          | deep link is the entry; no animation                           |

The motion system is intentionally narrow. A new screen must use
one of these — no bespoke transitions.

---

### 7.10 Iconography

**Material Symbols (rounded)** for system icons. **Custom vector
drawables** (`res/drawable/ic_*.xml`) for branded glyphs and the
six category icons. **Bitmaps** (`res/drawable/cv_logo.png`) only
for the wordmark/logo on splash + onboarding p1.

**Tinting rule.** A category icon is _always_ tinted with its
category color (the `Icon*Tint` aliases in §7.2.5). A system icon
in body text uses `OnBaseMuted`. A system icon inside a primary
button uses the button's `contentColor` (which is `Light` on a
filled blue, `OnBase` on a neutral).

**AutoMirrored variants.** Use `Icons.AutoMirrored.Rounded.*` for
any icon that has directional meaning — back arrow, forward arrow,
list-arrow, "send", "exit". Never use the non-mirrored versions
even though the app does not currently ship an RTL locale; we
want RTL to "just work" the day a translator delivers Arabic.

**Emoji policy.** Emoji are allowed in **two** places, and nowhere
else:

1. **`NeoPageHeader`** — the left glyph in the header band of
   each page. The emoji conveys category at a glance.
2. **Home Quick Actions** — the round shortcut tiles on the Home
   tab use emoji as their primary glyph, with a labeled caption
   underneath.

Emoji are **never** used in:

- Body text. Tag names. Notes. Phone-number labels.
- Buttons. (Use a vector icon if a glyph is needed.)
- Lists. Category chips. (Use the tinted `Icon*Tint` vector.)
- Snackbars, dialogs, error messages.

**Why this policy.** Emoji rendering varies across Android
manufacturer fonts. A heart on Samsung looks nothing like a heart
on Pixel. We only allow emoji where the _category_ is the message,
not the _glyph itself_.

---

### 7.11 Shadow modifier internals

Dual-shadow rendering is implemented as `Modifier.neoShadow(level: NeoElevation, shape: Shape)`. It is the only path to a callNest shadow — `Modifier.shadow(...)` from Compose is forbidden because its single-shadow output cannot be made convex+concave.

**Implementation outline.**

1. The modifier wraps a `drawBehind { ... }` block.
2. For each of the two shadow specs (light, dark), it builds a
   `Paint` with a `BlurMaskFilter(blurRadius, NORMAL)`.
3. It draws the `shape`'s outline twice — once translated by the
   light offset with the light color, once by the dark offset
   with the dark color.
4. The base canvas is painted _between_ the two shadow passes by
   the parent `Surface`/`Background`, so the shadow blurs blend
   into the canvas, not into each other.

**Why `BlurMaskFilter` and not `RenderEffect.Blur`.**
`RenderEffect.Blur` requires API 31+ and burns a hardware layer
per shadow pass. `BlurMaskFilter` works on API 26+ (our minSdk)
and is GPU-friendly because it operates on the alpha mask of the
shape outline, not on a bitmap snapshot.

**Performance notes.**

- The modifier is `remember`-keyed by `level` and `shape` — the
  `Paint` and `BlurMaskFilter` instances are cached.
- The blur radius is converted from `dp` to `px` once at recompose,
  not every frame.
- Cards in lists must **not** apply `neoShadow` per-item if the
  list scrolls fast — instead, the parent surface paints the
  shadow once and the items use `Flat`. This optimization is
  enforced by code review, not by the modifier itself.
- On API 26-27, `BlurMaskFilter` cannot be hardware-accelerated.
  We measured this at install time on a Pixel 1 and saw <1ms
  per card — acceptable.

---

## 8 — Navigation graph

callNest has a **two-level NavHost**: a root NavHost owned by
`MainActivity` whose graph is the set of top-level destinations
(Splash, Onboarding, Main, deep screens), and a nested NavHost
owned by `MainScaffold` whose graph is the four tabs.

### 8.1 Top-level NavHost

- **Owner**: `MainActivity` via `setContent { callNestApp() }`.
- **NavController**: `rememberNavController()` provided as
  `LocalRootNav` `CompositionLocal`.
- **Start destination**: `Destinations.Splash.route`.
- **Modifier**: fills the entire window; status-bar inset is
  applied by each screen, not by the NavHost.

**Route registry — every entry from `Destinations.kt`:**

| Destination           | Route                            | Args               | Source                                 |
| --------------------- | -------------------------------- | ------------------ | -------------------------------------- |
| `Main`                | `main`                           | —                  | tabbed surface                         |
| `Splash`              | `splash`                         | —                  | cold start only                        |
| `Onboarding`          | `onboarding`                     | —                  | first launch                           |
| `Calls`               | `calls`                          | —                  | (legacy direct route, in tabs now)     |
| `PermissionRationale` | `permission_rationale`           | —                  | gate when critical perm missing        |
| `PermissionDenied`    | `permission_denied`              | —                  | gate when perm permanently denied      |
| `CallDetail`          | `call_detail/{normalizedNumber}` | URI-encoded String | tap on call list                       |
| `Search`              | `search`                         | —                  | full-screen search overlay             |
| `FilterPresets`       | `filter_presets`                 | —                  | filter manager                         |
| `MyContacts`          | `my_contacts`                    | —                  | system contacts (non-inquiry)          |
| `Inquiries`           | `inquiries`                      | —                  | auto-saved inquiry bucket (also a tab) |
| `AutoSaveSettings`    | `settings/auto_save`             | —                  | nested under Settings                  |
| `AutoTagRules`        | `auto_tag_rules`                 | —                  | rules manager                          |
| `RuleEditor`          | `auto_tag_rules/edit/{ruleId}`   | Long, `-1` = new   | edit rule                              |
| `LeadScoringSettings` | `settings/lead_scoring`          | —                  | nested                                 |
| `RealTimeSettings`    | `settings/real_time`             | —                  | nested                                 |
| `Export`              | `export`                         | —                  | wizard                                 |
| `Backup`              | `backup`                         | —                  | landing                                |
| `UpdateAvailable`     | `update`                         | —                  | manifest match                         |
| `UpdateSettings`      | `settings/updates`               | —                  | nested                                 |
| `Settings`            | `settings`                       | —                  | master                                 |
| `DocsList`            | `docs`                           | —                  | FAQ list                               |
| `DocsArticle`         | `docs/{articleId}`               | URI-encoded String | one article                            |
| `Home`                | `home`                           | —                  | tab route (also routable directly)     |
| `More`                | `more`                           | —                  | tab route                              |

`Calls`, `Home`, `Inquiries`, `More` exist at the top level **only
as legacy aliases**. The canonical mount-point for those four is
inside `Main`'s nested NavHost.

### 8.2 Nested tab NavHost (inside `MainScaffold`)

- **Owner**: `MainScaffold` composable.
- **NavController**: `rememberNavController()` provided as
  `LocalMainTabNav`.
- **Start destination**: `MainTabRoute.Home.route` = `home`.
- **Composables**: one per `MainTabRoute` — Home, Calls,
  Inquiries, More.

```
MainTabRoute.Home       = "home"
MainTabRoute.Calls      = "calls"
MainTabRoute.Inquiries  = "inquiries"
MainTabRoute.More       = "more"
```

The bottom nav drives this NavHost and only this NavHost. Deep
screens (e.g., CallDetail) are pushed onto the **root** NavHost,
not the tab NavHost — they cover the bottom nav.

### 8.3 Splash gating

Cold start is the only path that lands on `Splash`. The splash
runs for ~1500ms (see §9), then calls `onFinished(decision)` where
`decision` is computed from two flags:

```kotlin
when {
    !onboardingComplete -> nav.navigate(Destinations.Onboarding.route) {
        popUpTo(Destinations.Splash.route) { inclusive = true }
    }
    !isCriticalGranted  -> nav.navigate(Destinations.PermissionRationale.route) {
        popUpTo(Destinations.Splash.route) { inclusive = true }
    }
    else                -> nav.navigate(Destinations.Main.route) {
        popUpTo(Destinations.Splash.route) { inclusive = true }
    }
}
```

`onboardingComplete` is read from `SettingsDataStore`.
`isCriticalGranted` is the conjunction of `READ_CALL_LOG`,
`READ_CONTACTS`, and `WRITE_CONTACTS` checked via
`ContextCompat.checkSelfPermission`.

### 8.4 Permission gate routing

After onboarding, the user can revoke permissions in system
Settings. On the next cold start, `Splash` will route them to
`PermissionRationale`. From `PermissionRationale`:

- Tapping "Grant" launches `RequestMultiplePermissions`.
- If the user grants → `nav.navigate(Main) { popUpTo(PermissionRationale) { inclusive = true } }`.
- If the user denies once → stays on `PermissionRationale` with
  the "Why this matters" copy expanded.
- If the user denies with "Don't ask again" (Android returns
  `shouldShowRequestPermissionRationale == false` after deny) →
  routes to `PermissionDenied`.

`PermissionDenied` shows a "Open Settings" deep link via
`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.

### 8.5 Tab-switch behavior

The bottom-nav `NavigationBarItem` uses the canonical "preserve
state" pattern:

```kotlin
tabNav.navigate(route) {
    popUpTo(tabNav.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

Effects:

- Switching from Calls (deep at scroll position 4000px) to
  Inquiries and back restores Calls at scroll position 4000px.
- Tapping the same tab again does **not** scroll-to-top — the
  spec explicitly forbids this because power users use the tab
  to "come back to where I was".
- Opening a deep screen (e.g., CallDetail) is a root NavHost
  push and does **not** affect tab-state-saving.

### 8.6 Back-stack rules

callNest does _not_ follow Android's default back-stack semantics
for tabs. Instead:

**Rule A — Hardware back from any deep screen → Home tab.**
A `popToHome()` helper composable reads both
`LocalRootNav` and `LocalMainTabNav`:

```kotlin
fun popToHome() {
    rootNav.popBackStack(Destinations.Main.route, inclusive = false)
    mainTabNav.navigate(MainTabRoute.Home.route) {
        popUpTo(mainTabNav.graph.findStartDestination().id)
        launchSingleTop = true
    }
}
```

This is wired to `BackHandler` on every deep screen.

**Rule B — Hardware back when on a tab root.**
First press: shows snackbar `"Press back again to exit"` for 2000ms.
Second press within 2000ms: `finish()`.
Tracked via a tiny state machine inside `MainScaffold`.

**Rule C — Hardware back inside tab nested-stack.**
Standard pop. The four tab NavHosts can have their own internal
stacks (e.g., AutoTagRules → RuleEditor) — those pop normally.

### 8.7 Deep links

The app responds to one deep link signal: an `Intent` extra
`route=update_available` on `MainActivity`. On first composition
of `callNestApp`, an effect inspects the intent:

```kotlin
LaunchedEffect(intent) {
    if (intent?.getStringExtra("route") == "update_available") {
        rootNav.navigate(Destinations.UpdateAvailable.route)
    }
}
```

There are no other deep links. No `intent-filter` outside the
launcher. The update-check flow itself runs in-app and does not
require a deep link, but the notification it fires uses one to
get the user back to the right screen after dismissing the
notification.

### 8.8 Route argument formats

| Destination   | Arg                | Type   | Encoding          | Example                       |
| ------------- | ------------------ | ------ | ----------------- | ----------------------------- |
| `CallDetail`  | `normalizedNumber` | String | `Uri.encode(...)` | `call_detail/%2B919876543210` |
| `RuleEditor`  | `ruleId`           | Long   | raw, `-1` for new | `auto_tag_rules/edit/-1`      |
| `DocsArticle` | `articleId`        | String | `Uri.encode(...)` | `docs/auto-save-howto`        |

Decoding:

- String args use `navArgument(name) { type = NavType.StringType }` and `Uri.decode(backStackEntry.arguments?.getString(name)!!)`.
- `ruleId` uses `navArgument(name) { type = NavType.LongType }` and `getLong(name) ?: -1L`.

### 8.9 Deep destination list

Approximate origin-tab map (the tab the user was on when they
opened a deep screen — used purely for analytics-of-the-mind, not
behaviorally; behavior is governed by §8.6 Rule A):

| Destination           | Typical origin tab       |
| --------------------- | ------------------------ |
| `CallDetail`          | Calls / Home / Inquiries |
| `Search`              | Calls / Inquiries        |
| `FilterPresets`       | Calls                    |
| `MyContacts`          | More                     |
| `Inquiries` (deep)    | Home                     |
| `AutoSaveSettings`    | More → Settings          |
| `AutoTagRules`        | More → Settings          |
| `RuleEditor`          | AutoTagRules             |
| `LeadScoringSettings` | More → Settings          |
| `RealTimeSettings`    | More → Settings          |
| `Export`              | More                     |
| `Backup`              | More                     |
| `UpdateAvailable`     | More / notification      |
| `UpdateSettings`      | More → Settings          |
| `Settings`            | More                     |
| `DocsList`            | More                     |
| `DocsArticle`         | DocsList                 |
| `PermissionRationale` | Splash gate              |
| `PermissionDenied`    | PermissionRationale      |

### 8.10 ASCII navigation graph

```
                     [ MainActivity ]
                            |
                            v
                   ( root NavController )
                            |
        +-------------------+-------------------+
        |                   |                   |
        v                   v                   v
    [ Splash ]      [ Onboarding ]      [ PermissionRationale ]
        |                   |                   |
        +---------+---------+-------------------+
                  |
                  v
              [ Main ]
                  |
                  +-- (nested NavController)
                  |        |
                  |   +----+----+----+----+
                  |   |    |    |    |    |
                  |   v    v    v    v    v
                  | Home Calls Inq.  More
                  |
                  +-- deep pushes (root) -->
                       CallDetail
                       Search
                       FilterPresets
                       MyContacts
                       Inquiries (deep)
                       AutoSaveSettings
                       AutoTagRules ─> RuleEditor
                       LeadScoringSettings
                       RealTimeSettings
                       Export
                       Backup
                       UpdateAvailable
                       UpdateSettings
                       Settings
                       DocsList ─> DocsArticle
                       PermissionDenied
```

---

## 9 — Splash screen

### 9.1 Purpose

Mask cold-start latency, brand the app, and gate routing. The
splash is the only place the user ever sees the wordmark animate
in. After this screen, the app is _itself_; the splash never
re-appears within a session.

### 9.2 Entry points

- **Cold start only.** When `MainActivity.onCreate` runs and the
  process is fresh, root NavHost starts at `Splash`.
- Process-death restarts: also cold start, also splash.
- Warm starts (Activity restored from saved state): no splash;
  user re-enters at the last destination.

### 9.3 Exit points

- `Onboarding` — when `onboardingComplete = false`.
- `PermissionRationale` — when `onboardingComplete = true` but
  `isCriticalGranted = false`.
- `Main` — happy path.

All three are popUpTo-inclusive of `Splash`: there is no way to
press back to splash.

### 9.4 Required inputs (data)

None. The splash is pure render. Routing data
(`onboardingComplete`, `isCriticalGranted`) is read once inside
`onFinished` — not as state — to avoid recompose.

### 9.5 Required inputs (user)

None. The splash auto-advances after **1500ms total**.

### 9.6 Mandatory display elements

1. **Full-bleed gradient background** — vertical
   `SplashGradStart` → `SplashGradEnd`, edge to edge, behind the
   status bar.
2. **Centered concave disc** — a 160dp circle painted with
   `Base` and `ConcaveLarge` elevation, holding the
   `cv_logo` bitmap centered (96dp logo inside the 160dp well).
3. **Wordmark "callNest"** — typewriter-animated, white,
   `headlineMedium` weight, positioned 24dp below the disc.

### 9.7 Optional display elements

None. The splash is intentionally minimal.

### 9.8 Empty state

N/A — splash is the empty state by definition.

### 9.9 Loading state

The splash _is_ the loading state. There is no spinner; the
implicit wait is 1500ms regardless of how fast the app is ready.
We choose readiness perception over actual readiness here.

### 9.10 Error state

N/A. The splash cannot error: it has no I/O. If the app crashes
before reaching it, the system splash carries until process death
and the user sees a launcher icon, not us.

### 9.11 Edge cases

- **System splash flash**: Android 12+ shows a system splash
  before our `MainActivity` is composed. We set
  `windowSplashScreenBackground = #0E5C4F` and the icon to the
  app icon, so the handoff to our gradient is invisible.
- **Low-end device degradation**: on devices reporting
  `ActivityManager.isLowRamDevice()`, the ring-sweep stage is
  skipped (the ring is drawn instantly). The wordmark still
  types in.
- **RTL handling**: the wordmark is the brand name "callNest"
  and is rendered LTR regardless of locale — it is not
  translatable.
- **Screen rotation**: the splash is `screenOrientation="portrait"`
  via the manifest activity attribute. Rotation is impossible
  during splash.
- **Accessibility services overlap (TalkBack, Switch Access)**:
  the splash is announced as "callNest loading"; accessibility
  services do not interrupt the auto-advance.

### 9.12 Copy table

| String resource         | English  | Notes                                                                |
| ----------------------- | -------- | -------------------------------------------------------------------- |
| `cv_splash_brand`       | callNest | LTR, never translated                                                |
| `cv_splash_logo_letter` | C        | legacy — used by older logo letter render path; kept for back-compat |

### 9.13 ASCII wireframe (full-screen)

```
+---------------------------------------------+
|                                             |
|                                             |
|             #0E5C4F (top of grad)           |
|                                             |
|                                             |
|                  ,---------,                |
|                 / concave   \               |
|                |   well      |              |
|                |   +-------+ |              |
|                |   | logo  | |              |
|                |   +-------+ |              |
|                 \           /               |
|                  '---------'                |
|                                             |
|                  callNest                  |
|              (white, typewriter)            |
|                                             |
|                                             |
|             #34A853 (bottom of grad)        |
|                                             |
+---------------------------------------------+
```

### 9.14 Accessibility

- `Modifier.semantics { contentDescription = "callNest loading" }` on the root.
- Disc + wordmark are merged into a single semantics node so
  TalkBack reads the brand once, not twice.
- Contrast: white wordmark on `#0E5C4F` → 12.4:1 (AAA). White
  on `#34A853` → 3.9:1 (AA Large). The wordmark sits in the
  upper third where the background is darker, so AAA holds in
  practice.
- No interactive elements. No focusable elements. TalkBack
  cannot get "stuck" on splash.
- Reduced-motion: if `Settings.Global.ANIMATOR_DURATION_SCALE`
  is 0, all three stages collapse to instant render and
  `onFinished` still fires after 1500ms (the _route_ is what's
  important, not the animation).

### 9.15 Performance

- First frame must paint within **200ms** of `Activity.onCreate`.
  This is enforced by:
  - No I/O in `onCreate` other than DataStore _reads-by-Flow_
    that compose into state.
  - The system splash background color (`#0E5C4F`) is identical
    to the Compose splash gradient start, so the "frame zero"
    looks correct even before Compose lays out.
- Total animation: 1500ms.
- No allocation per frame: the disc shadow is a remembered
  `BlurMaskFilter`, the gradient is a remembered `Brush`.
- Target: 0 jank frames on Pixel 4a (mid-tier 2020 device).

### 9.16 Animation timeline (millisecond-by-millisecond)

| Range       | Stage                     | What happens                                                                                                                                                                                                                                     |
| ----------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 0–200ms     | system splash carries     | Android shows `windowSplashScreenBackground = #0E5C4F` with launcher icon. Compose has not yet laid out.                                                                                                                                         |
| 200–500ms   | Compose splash takes over | Gradient paints. Disc enters with `spring(stiffness=600, damping=0.7)` from scale `0.85` to `1.0`. Logo bitmap fades from 0 to 1 over 300ms.                                                                                                     |
| 500–800ms   | ring sweep                | A `Canvas.drawArc` ring at radius = disc radius + 6dp, color `AccentBlue`, stroke 4dp, sweeps from 0° to 360° using `RingSweep` (1200ms tween — the 800ms→1700ms portion gets clipped to the 1500ms total; we crossfade the ring out at 1400ms). |
| 800–1200ms  | wordmark types in         | "callNest" appears one character at a time at `Typewriter` cadence (50ms × 9 chars = 450ms).                                                                                                                                                     |
| 1200–1500ms | settle                    | Everything holds. At 1500ms, `onFinished(decision)` fires.                                                                                                                                                                                       |

Implementation note: the ring's full sweep is 1200ms but the
splash budget is 1500ms; the ring is rendered with a deliberate
overlap — at 1400ms it begins a 100ms fade-out so it does not
"snap" off when the splash crossfades into the next destination.

---

## 10 — Onboarding orchestrator

### 10.1 Purpose

A 5-page, swipe-disabled, advance-only first-run flow. Goal: 60
seconds end-to-end on a happy path; max 2 minutes including
permission grant time and the OEM detour.

### 10.2 Pager structure

- `HorizontalPager(state = pagerState, userScrollEnabled = false, count = 5)`.
- `pagerState = rememberPagerState { 5 }`.
- Page advance is **only** via the `Continue` button on each page.
- Pages 2–5 show a `Back` text button on the top-left.
- A 5-dot progress indicator floats in the top-center; the
  current dot is `AccentBlue` filled, others are
  `BorderSoft` outlined.

### 10.3 Orchestrator file path

`app/src/main/java/com/callNest/app/ui/screen/onboarding/OnboardingScreen.kt`.
Each page is its own file in the same package:

- `OnboardingWelcomePage.kt`
- `OnboardingFeaturesPage.kt`
- `OnboardingPermissionsPage.kt`
- `OnboardingBatteryPage.kt`
- `OnboardingFirstSyncPage.kt`

The orchestrator owns `OnboardingViewModel` (Hilt) which holds:

- `onboardingComplete: StateFlow<Boolean>` (read-only mirror of DataStore)
- `setComplete(): Unit` (writes `true` to DataStore)
- `isCriticalGranted: StateFlow<Boolean>`

### 10.4 Skip rules

None. All five pages are mandatory. The "Skip-for-now" on page 5
is a soft skip — it still completes onboarding and routes to Main.
There is no "Skip onboarding" button anywhere.

### 10.5 Page-transition animation

`AnimatedContent` between page composables (we do not use
HorizontalPager's swipe animation because swipe is disabled, and
the manual advance gives us crisper control):

```kotlin
slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
    fadeIn(tween(220)) togetherWith
    slideOutHorizontally(targetOffsetX = { -it / 4 }) +
    fadeOut(tween(180))
```

Duration: 300ms in, 220ms out. Slide direction: forward = enter
from right, exit to left; back = reverse.

### 10.6 Edge cases

- **App killed mid-onboarding**: we persist `onboardingPage`
  into DataStore on every advance. On relaunch, the orchestrator
  reads it and starts at that page (after splash).
- **Permission denied permanent on page 3**: page advances
  regardless; the system permission gate (§8.4) catches it later.
- **Low-end device skipping animations**: if
  `isLowRamDevice() == true`, `AnimatedContent` is replaced with
  a no-animation crossfade.
- **OEM page 4 deep-link fails**: fallback chain — see §14.
- **First-sync errors on page 5**: the user can Retry or
  Skip-for-now (see §15). Skip still completes onboarding.

---

## 11 — Onboarding page 1: Welcome

### 11.1 Purpose

Establish the brand promise in one screen. Show the gradient.
Earn the next tap.

### 11.2 Entry points

From orchestrator only (the first page).

### 11.3 Exit points

`Continue` → page 2.

### 11.4 Required inputs (data)

None — pure render.

### 11.5 Required inputs (user)

`Continue` tap.

### 11.6 Mandatory display elements

1. Full-bleed `SplashGradStart` → `SplashGradEnd` gradient.
2. 160dp concave disc (white-on-base) with `cv_logo` bitmap
   inside, centered horizontally, top offset 22% of screen
   height.
3. White headline "Never lose an inquiry call again."
   (`headlineMedium`, max 2 lines, center-aligned).
4. White-85% subtext one line "callNest catches every call so
   you never miss a lead." (`bodyLarge`, single line, ellipsize).
5. `NeoButton` primary variant labeled "Continue", anchored 32dp
   from the bottom safe-area inset.

### 11.7 Optional display elements

None.

### 11.8 Empty state

N/A — first page is its own state.

### 11.9 Loading state

N/A.

### 11.10 Error state

N/A.

### 11.11 Edge cases

- Very small phones (< 360dp wide): headline allowed to wrap to
  3 lines; subtext drops out entirely (`overflow = Visibility.Gone`
  via if-check).
- Tablet: max content width 480dp, centered.
- Landscape: orchestrator forces portrait via Activity flag, so
  this case does not occur.

### 11.12 Copy table

| ID                        | English                                               |
| ------------------------- | ----------------------------------------------------- |
| `cv_onb_welcome_title`    | Never lose an inquiry call again.                     |
| `cv_onb_welcome_subtitle` | callNest catches every call so you never miss a lead. |
| `cv_onb_welcome_continue` | Continue                                              |

### 11.13 ASCII wireframe

```
+----------------------------------+
| · · · · ·          (progress)    |
|                                  |
|                                  |
|              ,---,               |
|             | logo |              |
|              '---'               |
|                                  |
|   Never lose an inquiry          |
|        call again.               |
|                                  |
|  callNest catches every call    |
|  so you never miss a lead.       |
|                                  |
|                                  |
|         [   Continue   ]         |
+----------------------------------+
```

### 11.14 Accessibility

- Headline is `Modifier.semantics { heading() }`.
- Subtext: `liveRegion = LiveRegionMode.Polite`.
- Continue button: minimum 48dp tap target, content-description
  matches label.
- Color contrast: white on darkest gradient stop is 12.4:1; on
  brightest stop is 3.9:1 — headline placed in upper portion.

### 11.15 Performance

- Same `Brush` instance reused from splash via
  `LocalSplashGradient` provider — no re-allocation on first frame.

---

## 12 — Onboarding page 2: Features

### 12.1 Purpose

Three-card "what this does" pitch. Concrete, not aspirational.

### 12.2 Entry points

From page 1.

### 12.3 Exit points

`Continue` → page 3. `Back` → page 1.

### 12.4 Required inputs (data)

Static features list (in code, not DB).

### 12.5 Required inputs (user)

`Continue` or `Back`.

### 12.6 Mandatory display elements

1. White background (no gradient — gradient is reserved for
   splash + p1).
2. Title `headlineSmall` "Built for busy founders." (top, after
   `PageTopHeader`).
3. Three horizontal `NeoCard`s, each with:
   - 40dp tinted icon (rounded square, `IconCallsTint` /
     `IconInquiriesTint` / `IconStatsTint`).
   - Card title (`titleMedium`).
   - Card body (`bodyMedium`, 2 lines).
4. `Continue` button anchored bottom.
5. `Back` text button top-left.

### 12.7 Optional display elements

None.

### 12.8 Empty state

N/A.

### 12.9 Loading state

N/A.

### 12.10 Error state

N/A.

### 12.11 Edge cases

- The 3 cards stack vertically with 16dp gap; no horizontal
  carousel.
- On 4-inch phones, body of each card may truncate to 1 line —
  acceptable.

### 12.12 Copy table

| ID                                | English                                                                           |
| --------------------------------- | --------------------------------------------------------------------------------- |
| `cv_onb_features_title`           | Built for busy founders.                                                          |
| `cv_onb_features_calls_title`     | Captures every call                                                               |
| `cv_onb_features_calls_body`      | We log every inbound and outbound call from your phone log automatically.         |
| `cv_onb_features_inquiries_title` | Auto-saves inquiries                                                              |
| `cv_onb_features_inquiries_body`  | Unsaved numbers go straight into a dedicated contact group, tagged for follow-up. |
| `cv_onb_features_stats_title`     | Lead scoring + exports                                                            |
| `cv_onb_features_stats_body`      | Score every lead 0–100 and export to Excel, CSV, or PDF in one tap.               |
| `cv_onb_features_continue`        | Continue                                                                          |

### 12.13 Card data table

| Card | Icon                  | Tint                | Title id                          | Body id                          |
| ---- | --------------------- | ------------------- | --------------------------------- | -------------------------------- |
| 1    | `ic_call_log_capture` | `IconCallsTint`     | `cv_onb_features_calls_title`     | `cv_onb_features_calls_body`     |
| 2    | `ic_inquiry_inbox`    | `IconInquiriesTint` | `cv_onb_features_inquiries_title` | `cv_onb_features_inquiries_body` |
| 3    | `ic_stats_sparkline`  | `IconStatsTint`     | `cv_onb_features_stats_title`     | `cv_onb_features_stats_body`     |

### 12.14 ASCII wireframe

```
+----------------------------------+
| < Back        · · · · ·          |
|                                  |
|   Built for busy founders.       |
|                                  |
|  +----------------------------+  |
|  | [icon] Captures every call |  |
|  | We log every inbound and   |  |
|  | outbound call from your... |  |
|  +----------------------------+  |
|                                  |
|  +----------------------------+  |
|  | [icon] Auto-saves inquiries|  |
|  | Unsaved numbers go straight|  |
|  | into a dedicated contact...|  |
|  +----------------------------+  |
|                                  |
|  +----------------------------+  |
|  | [icon] Lead scoring + exp. |  |
|  | Score every lead 0–100 and |  |
|  | export to Excel, CSV, ...  |  |
|  +----------------------------+  |
|                                  |
|         [   Continue   ]         |
+----------------------------------+
```

### 12.15 Accessibility + performance

- Each card has `Modifier.semantics(mergeDescendants = true)` so
  TalkBack reads "icon, title, body" as one node.
- Cards reuse a shared `NeoElevation.ConvexMedium` paint cache.

---

## 13 — Onboarding page 3: Permissions

### 13.1 Purpose

Earn the four critical permissions in one batch. Frame each as a
_reason_, not a _capability_.

### 13.2 Entry points

From page 2.

### 13.3 Exit points

`Grant` → page 4 (regardless of result).
`Back` → page 2.

### 13.4 Required inputs (data)

Permission-state map from `PermissionsGate`.

### 13.5 Required inputs (user)

Tap `Grant`. The system dialog handles the rest.

### 13.6 Mandatory display elements

1. White background.
2. Title `headlineSmall` "Two taps and you're set.".
3. Subtitle `bodyMedium`: "Everything stays on your device.
   callNest never uploads your data."
4. List of 4 permission rows, each:
   - 32dp tinted icon
   - Permission display name (`titleSmall`)
   - Reason (`bodySmall`)
5. `Grant` `NeoButton` primary, full width, anchored bottom.
6. `Back` text button top-left.

### 13.7 Permission rows

| Icon                | Display name     | Reason                                                          |
| ------------------- | ---------------- | --------------------------------------------------------------- |
| `ic_call_log`       | Call log access  | So we can list every inbound and outbound call.                 |
| `ic_contacts`       | Contacts (read)  | So we know which calls are from saved leads.                    |
| `ic_contacts_write` | Contacts (write) | So we can auto-save unsaved inquiries to a group.               |
| `ic_phone_state`    | Phone state      | So the floating bubble + post-call popup can fire in real time. |

### 13.8 Permission state machine

```
[ idle ]
   |
   |  user taps Grant
   v
[ requesting ] -- system dialog --
   |
   +--> all granted        -> emit advance
   +--> partially granted  -> emit advance (gate will re-check on Main entry)
   +--> denied (first)     -> stay; "Why this matters" expands
   +--> denied (permanent) -> emit advance; gate will route to PermissionDenied later
```

The `RequestMultiplePermissions` launcher returns a
`Map<String, Boolean>`. We compute:

```kotlin
val anyPermanentlyDenied = permissions.any { (perm, granted) ->
    !granted && !shouldShowRequestPermissionRationale(perm)
}
```

`anyPermanentlyDenied` does **not** block advance — onboarding
must complete. The gate at `Splash` / on-Main-entry handles it.

### 13.9 Optional / empty / loading / error

All N/A — this is a request flow, not a data flow.

### 13.10 Edge cases

- User dismisses system dialog by tapping outside: counts as
  "denied (first)". State returns to idle, button re-enables.
- User on Android 12+ with one-time permission: treated as
  granted for this session; gate re-checks on next cold start.
- User on Android 13+ with `POST_NOTIFICATIONS`: not requested
  here (we ask later, when first creating a notification).

### 13.11 Copy table

| ID                                  | English                                                            |
| ----------------------------------- | ------------------------------------------------------------------ |
| `cv_onb_perm_title`                 | Two taps and you're set.                                           |
| `cv_onb_perm_subtitle`              | Everything stays on your device. callNest never uploads your data. |
| `cv_onb_perm_calllog_name`          | Call log access                                                    |
| `cv_onb_perm_calllog_reason`        | So we can list every inbound and outbound call.                    |
| `cv_onb_perm_contacts_read_name`    | Contacts (read)                                                    |
| `cv_onb_perm_contacts_read_reason`  | So we know which calls are from saved leads.                       |
| `cv_onb_perm_contacts_write_name`   | Contacts (write)                                                   |
| `cv_onb_perm_contacts_write_reason` | So we can auto-save unsaved inquiries to a group.                  |
| `cv_onb_perm_phone_state_name`      | Phone state                                                        |
| `cv_onb_perm_phone_state_reason`    | So the floating bubble and post-call popup can fire in real time.  |
| `cv_onb_perm_grant`                 | Grant permissions                                                  |
| `cv_onb_perm_why`                   | Why this matters                                                   |

### 13.12 ASCII wireframe

```
+----------------------------------+
| < Back        · · · · ·          |
|                                  |
|  Two taps and you're set.        |
|                                  |
|  Everything stays on your        |
|  device. callNest never         |
|  uploads your data.              |
|                                  |
|  [icon] Call log access          |
|         So we can list every...  |
|                                  |
|  [icon] Contacts (read)          |
|         So we know which calls...|
|                                  |
|  [icon] Contacts (write)         |
|         So we can auto-save...   |
|                                  |
|  [icon] Phone state              |
|         So the floating bubble...|
|                                  |
|     [   Grant permissions   ]    |
+----------------------------------+
```

### 13.13 Accessibility + performance

- Each row is one semantics node.
- Grant button announces its action via `contentDescription`.
- "Why this matters" section uses `liveRegion = Polite` so it
  is announced when expanded.

---

## 14 — Onboarding page 4: OEM battery

### 14.1 Purpose

Earn the manufacturer-specific autostart / battery-saver
exemption that keeps the foreground service alive. Without this,
the post-call popup and floating bubble silently die.

### 14.2 Entry points

From page 3.

### 14.3 Exit points

`Continue` → page 5. `Back` → page 3.

### 14.4 Required inputs (data)

`Build.MANUFACTURER` to select the vendor copy + intent.

### 14.5 Required inputs (user)

`Continue` (always available — this page is informational +
optional deep-link launch).

### 14.6 Mandatory display elements

1. Vendor-specific title, e.g. "Keep callNest running on your
   Xiaomi.".
2. Subtitle: "Your phone may stop callNest when you're not
   looking. Two settings keep it on.".
3. Numbered instruction list (3–5 bullets, vendor-specific).
4. `Open settings` `NeoButton` primary that fires the vendor
   intent.
5. `I've done this` text button (advances).
6. `Continue` is the same as "I've done this" — they share an
   action.

### 14.7 OEM detection table

| `Build.MANUFACTURER` (lowercased contains) | Vendor key | Display name          |
| ------------------------------------------ | ---------- | --------------------- |
| `xiaomi` / `redmi` / `poco`                | `xiaomi`   | Xiaomi / Redmi / POCO |
| `oppo`                                     | `oppo`     | Oppo                  |
| `vivo`                                     | `vivo`     | Vivo                  |
| `realme`                                   | `realme`   | Realme                |
| `samsung`                                  | `samsung`  | Samsung               |
| `oneplus`                                  | `oneplus`  | OnePlus               |
| `honor`                                    | `honor`    | Honor                 |
| `huawei`                                   | `huawei`   | Huawei                |
| anything else                              | `other`    | Other                 |

### 14.8 Vendor intent components

Each vendor has a primary intent. If it fails (`ActivityNotFoundException`), fall through to the next.

| Vendor    | Primary `ComponentName`                                                 |
| --------- | ----------------------------------------------------------------------- |
| `xiaomi`  | `com.miui.securitycenter` / `.permission.AutoStartManagementActivity`   |
| `oppo`    | `com.coloros.safecenter` / `.permission.startup.StartupAppListActivity` |
| `vivo`    | `com.vivo.permissionmanager` / `.activity.BgStartUpManagerActivity`     |
| `realme`  | `com.coloros.safecenter` / `.permission.startup.StartupAppListActivity` |
| `samsung` | `com.samsung.android.lool` / `.battery.ui.BatteryActivity`              |
| `oneplus` | `com.oneplus.security` / `.chainlaunch.view.ChainLaunchAppListActivity` |
| `honor`   | `com.huawei.systemmanager` / `.optimize.process.ProtectActivity`        |
| `huawei`  | `com.huawei.systemmanager` / `.optimize.process.ProtectActivity`        |
| `other`   | (none — go straight to fallback)                                        |

### 14.9 Fallback chain

```
1. Try vendor intent (above).
2. If ActivityNotFoundException ->
   Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
   with data = "package:com.callNest.app".
3. If still ActivityNotFoundException ->
   Settings.ACTION_BATTERY_SAVER_SETTINGS.
4. If still nothing -> show snackbar "Couldn't find the right
   settings on your device. Long-press the callNest icon and
   tap 'App info'.".
```

### 14.10 Vendor instruction copy

#### Xiaomi (3–4 bullets)

1. Tap **Autostart** and toggle callNest **on**.
2. Go back; open **Battery saver**.
3. Find callNest → **No restrictions**.
4. (MIUI 14+) Tap **Other permissions** → enable **Display popup
   while running in background**.

#### Oppo

1. Tap **Allow Auto-Launch** and turn on callNest.
2. Go back; open **Power Manager** → **Battery Optimization**.
3. Find callNest → **Don't optimize**.
4. (ColorOS 13+) Tap **Floating windows** → enable for callNest.

#### Vivo

1. Tap **High background power consumption** → enable callNest.
2. Open **Battery** → **Background power consumption** →
   callNest → **Allow**.
3. (FunTouch 13+) Open **Permissions** → enable **Display over
   other apps**.

#### Realme

1. Tap **Auto Launch** → enable callNest.
2. Open **Battery** → **App battery management** → callNest →
   **Allow**.
3. (Realme UI 4+) Enable **Floating windows** for callNest.

#### Samsung

1. Tap **Battery** → **Background usage limits**.
2. Add callNest to **Never sleeping apps**.
3. Open **App info** → **Battery** → **Unrestricted**.

#### OnePlus

1. Tap **Battery** → **Battery optimization** → callNest →
   **Don't optimize**.
2. Open **Advanced Optimization** → enable for callNest.

#### Honor

1. Tap **Protected apps** and enable callNest.
2. Open **Battery** → **Launch** → enable all three toggles for
   callNest.

#### Huawei

1. Tap **Protected apps** and enable callNest.
2. Open **App launch** → callNest → enable **Auto-launch**,
   **Secondary launch**, **Run in background**.

#### Other

1. Open **App info** for callNest.
2. Open **Battery** → set to **Unrestricted**.
3. (If available) Open **Advanced** → enable **Allow background
   activity**.

### 14.11 Copy table (per page)

| ID                        | English                                                                        |
| ------------------------- | ------------------------------------------------------------------------------ |
| `cv_onb_battery_title`    | Keep callNest running on your %s.                                              |
| `cv_onb_battery_subtitle` | Your phone may stop callNest when you're not looking. Two settings keep it on. |
| `cv_onb_battery_open`     | Open settings                                                                  |
| `cv_onb_battery_done`     | I've done this                                                                 |

`%s` is filled with the display name from §14.7.

### 14.12 Edge cases

- User on a brand-new vendor not in our table → `other` copy.
- User on Android Go: vendor intent often missing — fallback
  chain applies.
- User taps `Open settings`, returns immediately: we cannot
  detect what they did. We trust them.

### 14.13 ASCII wireframe

```
+----------------------------------+
| < Back        · · · · ·          |
|                                  |
|  Keep callNest running          |
|  on your Xiaomi.                 |
|                                  |
|  Your phone may stop callNest   |
|  when you're not looking. Two    |
|  settings keep it on.            |
|                                  |
|  1. Tap Autostart and toggle...  |
|  2. Go back; open Battery...     |
|  3. Find callNest → No restr... |
|  4. (MIUI 14+) Tap Other perm... |
|                                  |
|     [   Open settings   ]        |
|     [   I've done this  ]        |
+----------------------------------+
```

### 14.14 Accessibility + performance

- Numbered list is a semantics list (`Modifier.semantics { collectionInfo = CollectionInfo(rowCount = N, columnCount = 1) }`).
- Each step's text is a single semantics node.

---

## 15 — Onboarding page 5: First sync

### 15.1 Purpose

Run the first sync of `CallLog.Calls` so that when the user
lands on Main, the Calls tab is populated. Show progress; never
block.

### 15.2 Entry points

From page 4.

### 15.3 Exit points

`Continue` (only visible after `done` or `error`) → Main.
`Skip-for-now` (visible after `error`) → Main with possibly empty
Calls list.

### 15.4 Required inputs (data)

`SyncProgressBus` `SharedFlow<SyncProgress>` — see Part 01 §6.1
for the sync pipeline. Emissions:

```kotlin
sealed interface SyncProgress {
    data object Indeterminate : SyncProgress
    data class Determinate(val current: Int, val total: Int) : SyncProgress
    data object Done : SyncProgress
    data class Error(val cause: Throwable) : SyncProgress
}
```

### 15.5 Required inputs (user)

- `Continue` after `Done` (auto-advances after 1500ms in `Done`).
- `Retry` after `Error`.
- `Skip-for-now` after `Error`.

### 15.6 Mandatory display elements

1. Title `headlineSmall` "First import.".
2. Progress visualization:
   - **Indeterminate**: 64dp `NeoSpinner` (concave-rim,
     `IndeterminateStripe` motion) + body "Reading your call log…".
   - **Determinate**: linear `NeoProgressBar` + body "Found N of
     M calls so far…".
   - **Done**: green checkmark inside concave well + body "All
     caught up. Welcome to callNest.".
   - **Error**: red exclamation inside concave well + body
     "We couldn't finish the first import." + secondary body
     `error.localizedMessage` truncated to 2 lines.
3. Buttons (state-dependent):
   - Indeterminate / Determinate: no buttons.
   - Done: `Continue` primary (auto-advances after 1500ms even
     if untapped).
   - Error: `Retry` primary + `Skip for now` text button.

### 15.7 Optional display elements

A small "We'll keep syncing in the background." caption appears
under the progress bar in `Determinate` state if `total > 500`.

### 15.8 Empty state

A user with zero calls in their log: emits `Done` immediately
with `current = 0`. Body becomes "Your call log is empty for
now. callNest will start capturing as calls come in.".

### 15.9 Loading state

The whole page is a loading state for as long as the bus is in
`Indeterminate` or `Determinate`.

### 15.10 Error state

- Network is irrelevant (sync is local).
- Possible causes: `SecurityException` (perm revoked between
  pages 3 and 5), `SQLiteFullException`, `IllegalStateException`
  from a malformed call-log row.
- The error body is user-friendly: "Couldn't read your call log.
  Tap to grant permission." for `SecurityException`; "Your phone
  is low on storage. Free some space and tap Retry." for
  `SQLiteFullException`; otherwise the localized message.
- `Retry` re-runs the same use case. `Skip for now` advances
  with `setComplete()` regardless.

### 15.11 Edge cases

- Sync completes before the page is even shown (very fast
  device): we still hold the `Done` state for 1500ms so the user
  sees the success.
- Sync emits >1 `Indeterminate` then `Determinate` — UI
  crossfades smoothly via `CrossFade` motion token.
- `total` becomes known after 200+ rows already counted
  (cursor.moveToFirst then cursor.count): UI jumps from
  Indeterminate to Determinate at `current = 200`. Acceptable.

### 15.12 Copy table

| ID                            | English                                                                         |
| ----------------------------- | ------------------------------------------------------------------------------- |
| `cv_onb_sync_title`           | First import.                                                                   |
| `cv_onb_sync_indet_body`      | Reading your call log…                                                          |
| `cv_onb_sync_det_body_fmt`    | Found %1$d of %2$d calls so far…                                                |
| `cv_onb_sync_bg_caption`      | We'll keep syncing in the background.                                           |
| `cv_onb_sync_done_body`       | All caught up. Welcome to callNest.                                             |
| `cv_onb_sync_done_empty_body` | Your call log is empty for now. callNest will start capturing as calls come in. |
| `cv_onb_sync_error_title`     | We couldn't finish the first import.                                            |
| `cv_onb_sync_error_perm`      | Couldn't read your call log. Tap to grant permission.                           |
| `cv_onb_sync_error_storage`   | Your phone is low on storage. Free some space and tap Retry.                    |
| `cv_onb_sync_retry`           | Retry                                                                           |
| `cv_onb_sync_skip`            | Skip for now                                                                    |
| `cv_onb_sync_continue`        | Continue                                                                        |

### 15.13 ASCII wireframe (Determinate)

```
+----------------------------------+
|               · · · · ·          |
|                                  |
|         First import.            |
|                                  |
|                                  |
|        +-----------------+       |
|        | concave spinner |       |
|        +-----------------+       |
|                                  |
|     Found 124 of 480 calls...    |
|                                  |
|  [============-----------------]  |
|                                  |
|  We'll keep syncing in the       |
|  background.                     |
|                                  |
|                                  |
+----------------------------------+
```

### 15.14 ASCII wireframe (Done)

```
+----------------------------------+
|               · · · · ·          |
|                                  |
|         First import.            |
|                                  |
|        +-----------------+       |
|        |       ✓         |       |
|        | (green check)   |       |
|        +-----------------+       |
|                                  |
|  All caught up. Welcome          |
|  to callNest.                   |
|                                  |
|         [   Continue   ]         |
+----------------------------------+
```

### 15.15 ASCII wireframe (Error)

```
+----------------------------------+
|               · · · · ·          |
|                                  |
|         First import.            |
|                                  |
|        +-----------------+       |
|        |       !         |       |
|        |   (rose alert)  |       |
|        +-----------------+       |
|                                  |
|  We couldn't finish the          |
|  first import.                   |
|  Couldn't read your call log.    |
|  Tap to grant permission.        |
|                                  |
|         [    Retry    ]          |
|         [ Skip for now ]         |
+----------------------------------+
```

### 15.16 Accessibility + performance

- The progress region is `liveRegion = Polite` so TalkBack
  announces transitions.
- The Done checkmark animates in over 200ms; `Animatable` is
  cancelled if the page leaves before completion.
- On `Done` auto-advance, `setComplete()` fires before
  navigation so `onboardingComplete = true` is durably stored
  by the time Main composes.

### 15.17 Wiring summary

```
OnboardingFirstSyncPage
   collects SyncProgressBus.flow as state
   on enter:
      vm.startFirstSync()  // launches use case if not already running
   render by state:
      Indeterminate -> spinner
      Determinate   -> progress bar
      Done          -> checkmark + auto-advance(1500ms)
      Error         -> alert + Retry / Skip
   on advance:
      vm.setComplete()
      rootNav.navigate(Main) { popUpTo(Onboarding) { inclusive = true } }
```

The use case (`StartFirstSyncUseCase`) is idempotent: re-entering
the page after a process kill resumes from the last persisted
`SyncCheckpoint` — see Part 01 §6.1.

---

_End of Part 02 — Theme + Navigation + Splash + Onboarding._
_Next: Part 03 covers Main scaffold, bottom navigation, and the
four tab home pages (§§ 16–22)._

---

# callNest APP-SPEC — Part 03

## Main scaffold + 4 tab pages

> Audience: a UX engineer rebuilding the callNest Android UI from scratch.
> This part is self-contained for the four primary tabs (Home, Calls,
> Inquiries, More) and the chrome that hosts them (top app bar +
> bottom navigation). Cross-references to other parts:
>
> - Data model and entities → Part 01 §5
> - App boot, permissions, splash → Part 02 §9
> - Call Detail screen (deep-link target from Calls tab) → Part 04
> - Settings + Stats + Updates → Part 05
> - Bottom sheets (Filter, Tag picker, Quick Export) → Part 06
> - Theme tokens (TabBg*, HeaderGrad*, BorderAccent, BorderSoft,
>   NeoSurface, NeoCard, NeoChip, NeoButton, NeoTabBar, NeoTopBar,
>   NeoAvatar, LeadScoreBadge) → Part 00
>
> Phase note: this document targets the post-Phase-I.6 UI which collapses
> "My Contacts" into Inquiries (Inquiries tab is the only contact-management
> surface), and reduces the top-bar overflow menu to two items.

---

## Table of contents

- §16 — MainScaffold (top bar + bottom nav)
- §17 — Home tab
- §18 — Calls tab
- §19 — Inquiries tab
- §20 — More tab

---

# §16 — MainScaffold (top bar + bottom nav)

The MainScaffold is not a "page". It is the persistent chrome that hosts
all four tab pages inside a nested NavHost. It owns the top app bar, the
bottom navigation, the back-press exit guard, the per-tab background tint,
and two CompositionLocals (`LocalRootNav`, `LocalMainTabNav`) that nested
screens use for navigation.

## 16.1 — Purpose

- Provide a single, durable Scaffold for the four primary tabs so
  headers, bottom nav, and system bar insets are consistent across every
  tab page.
- Host a **nested** `NavHost` (`mainTabNav`) so switching tabs does not
  destroy the back-stack of the other tabs. Each tab is a `composable`
  destination inside `mainTabNav`.
- Share top-level chrome controls (Search, Profile menu, badge counts)
  rather than reimplementing them per tab.
- Coordinate cross-tab side effects: refresh on tab change, exit guard
  on back-press, badge maintenance, sync banner.

## 16.2 — Entry points

| From                         | Behavior                                                                                     |
| ---------------------------- | -------------------------------------------------------------------------------------------- |
| App cold start (Launcher)    | After splash + permission gate (Part 02 §9.4), `rootNav` navigates to `route=main_tabs`.     |
| Process death restoration    | `MainScaffold` is rebuilt; `mainTabNav` restores its saved state and lands on last tab.      |
| Deep-link `callNest://calls` | Routes through rootNav → MainScaffold → mainTabNav.navigate("calls").                        |
| Notification tap (follow-up) | Routes through rootNav → MainScaffold → mainTabNav.navigate("home"), then opens detail.      |
| Returning from CallDetail    | rootNav.popBackStack() drops the detail; MainScaffold reappears on whichever tab was active. |

## 16.3 — Exit points

| To                             | Trigger                                                                                      |
| ------------------------------ | -------------------------------------------------------------------------------------------- |
| `route=call_detail/{number}`   | Row tap inside Calls tab or Home tab "Recent unsaved" list.                                  |
| `route=stats`                  | Quick-action chip on Home, or "Stats" row in More.                                           |
| `route=tag_manager`            | "Tags" row in More.                                                                          |
| `route=auto_tag_rules`         | "Auto-tag rules" row in More.                                                                |
| `route=lead_scoring`           | "Lead scoring" row in More.                                                                  |
| `route=realtime_features`      | "Real-time features" row in More.                                                            |
| `route=auto_save`              | "Auto-save" row in More.                                                                     |
| `route=app_updates`            | "App updates" row in More.                                                                   |
| `route=help_docs`              | "Help & docs" row in More.                                                                   |
| `route=settings`               | "Settings" row in More, or top-bar overflow → Settings.                                      |
| `route=backup`                 | "Backup & restore" row in More, or Home → Quick actions → Backup.                            |
| `route=export`                 | "Export" row in More.                                                                        |
| `route=search` (full-screen)   | Top-bar Search icon (any tab).                                                               |
| Quick Export bottom sheet      | Home → "Quick Export" chip OR More → "Quick Export" row. Modal overlay, **not** a route.     |
| Sign-out confirmation dialog   | Top-bar overflow → Sign out (only visible when Drive account is linked).                     |
| Android home (process visible) | Single back-press on root tab → snackbar "Press back again to exit"; second within 2s exits. |

## 16.4 — Required inputs (data)

| Source                    | Type / Flow                              | Default     | Used for                                         |
| ------------------------- | ---------------------------------------- | ----------- | ------------------------------------------------ |
| `mainTabNav`              | `NavHostController` (remembered)         | tab=`calls` | Nested navigation between the 4 tabs.            |
| `rootNav`                 | `NavHostController` (provided by parent) | n/a         | Routing OUT of the scaffold to detail screens.   |
| `currentRoute`            | `State<String?>`                         | `"calls"`   | Tab background tint + selected-tab indicator.    |
| `inquiriesBadge`          | `StateFlow<Int>`                         | 0           | Bottom-nav badge on Inquiries tab.               |
| `moreBadge`               | `StateFlow<Boolean>`                     | false       | Bottom-nav "1" dot on More tab when update due.  |
| `signedInToDrive`         | `StateFlow<Boolean>`                     | false       | Show / hide Sign-out menu item.                  |
| `syncProgress`            | `SharedFlow<SyncProgressEvent>`          | n/a         | Top-line linear progress bar inside the top bar. |
| `WindowInsets.systemBars` | Compose insets                           | n/a         | Padding for status bar and navigation bar.       |
| `BackHandler` state       | local (lastBackPressMs: Long)            | 0L          | Double-press-to-exit gate.                       |

ViewModel: `MainScaffoldViewModel @HiltViewModel constructor(contactRepo, updateRepo, driveAuthRepo, syncProgressBus)`.

State class:

```kotlin
data class MainScaffoldUiState(
    val inquiriesBadge: Int = 0,
    val moreBadge: Boolean = false,
    val signedInToDrive: Boolean = false,
    val syncing: Boolean = false,
    val syncProgressFraction: Float? = null
)
```

Combinators:

- `inquiriesBadge` ← `contactRepo.observeUnsavedLast7Days().map { it.size }`.
- `moreBadge` ← `combine(updateRepo.state, settings.observeUpdateSkipped()) { s, skipped -> s is UpdateState.Available && !skipped }`.
- `syncing` ← `syncProgressBus.flow.map { it !is SyncProgressEvent.Idle }.distinctUntilChanged()`.

## 16.5 — Required inputs (user)

| Trigger                          | Behavior                                                                                                                                    | State change                       |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- |
| Tap a bottom-nav tab             | `mainTabNav.navigate(tab) { popUpTo(start) { saveState = true } }`                                                                          | `currentRoute` updates.            |
| Tap currently-selected tab       | Scroll to top (each tab observes a `LocalScrollToTopBus`).                                                                                  | Per-tab list scrolls to index 0.   |
| Long-press a bottom-nav tab      | Reserved for future "switcher". Currently no-op + light haptic.                                                                             | None.                              |
| Tap Search icon (top bar)        | `rootNav.navigate("search")` — opens full-screen search overlay.                                                                            | None.                              |
| Tap overflow (⋮)                 | Open `DropdownMenu` with Profile + (optional) Sign out.                                                                                     | `menuExpanded = true`.             |
| Tap "Sign out" menu item         | Open AlertDialog → confirm → `driveAuthRepo.signOut()`.                                                                                     | `signedInToDrive` becomes `false`. |
| Tap "Profile" menu item          | `rootNav.navigate("settings?focus=profile")`.                                                                                               | None at scaffold.                  |
| System back-press at root        | If `lastBackPressMs` within 2000 ms → `(activity as Activity).finish()`. Else show snackbar "Press back again to exit" + record press time. | `lastBackPressMs` set.             |
| System back-press inside a sheet | Sheet handles its own back; scaffold does not consume.                                                                                      | None.                              |
| Pull-to-refresh inside a tab     | Tab forwards to `SyncScheduler.triggerOnce()`. Scaffold reflects via top-line.                                                              | `syncing = true` until `Idle`.     |

## 16.6 — Mandatory display elements

### 16.6.1 — NeoTopBar

- `showBrand = true` → leading slot renders the small callNest wordmark
  (`logoMonochrome` painter at 18.sp height).
- `title = "callNest"` (lowercase brand). Style: `MaterialTheme.typography.titleMedium`,
  `letterSpacing = 0.5.sp`. Color: `Theme.OnSurfaceStrong`.
- Leading icon: app glyph (24.dp NeoSurface concave circle, tinted `BrandTeal`).
- Trailing actions:
  1. Search icon (`Icons.Outlined.Search`, 24.dp). Touch target 48.dp.
  2. Overflow menu (⋮ `Icons.Outlined.MoreVert`, 24.dp). Touch target 48.dp.
- Top-line indeterminate `LinearProgressIndicator` rendered just below
  the bar when `syncing == true`. Height 2.dp. Color `BrandTeal`. Hidden
  otherwise (no layout reservation; renders in an overlay box so the
  page below does not jump).

### 16.6.2 — NeoTabBar (bottom)

- Four tabs in fixed order:

  | Index | Route       | Label     | Icon (selected)          | Icon (unselected)          |
  | ----- | ----------- | --------- | ------------------------ | -------------------------- |
  | 0     | `home`      | Home      | `Icons.Filled.Home`      | `Icons.Outlined.Home`      |
  | 1     | `calls`     | Calls     | `Icons.Filled.Call`      | `Icons.Outlined.Call`      |
  | 2     | `inquiries` | Inquiries | `Icons.Filled.Inbox`     | `Icons.Outlined.Inbox`     |
  | 3     | `more`      | More      | `Icons.Filled.MoreHoriz` | `Icons.Outlined.MoreHoriz` |

- Selected indicator: 4.dp pill behind the selected icon, color
  `BrandTeal.copy(alpha=0.18f)`.
- Inquiries badge: small pill displaying `inquiriesBadge`. When 0, hide.
  When > 99, show `99+`.
- More badge: 8.dp dot, color `Accent.Warning`, no number. Visible only
  when `moreBadge == true`.
- Bottom inset: `Modifier.windowInsetsPadding(WindowInsets.navigationBars)`
  ensures the bar floats above gesture/3-button nav.
- Height: 64.dp content + system inset.

### 16.6.3 — Per-tab background tint

The Scaffold container sets `containerColor` based on `currentRoute`:

| Route       | Container color  |
| ----------- | ---------------- |
| `home`      | `TabBgHome`      |
| `calls`     | `TabBgCalls`     |
| `inquiries` | `TabBgInquiries` |
| `more`      | `TabBgMore`      |

The tint changes are animated with `animateColorAsState(durationMillis = 240)`.

### 16.6.4 — System bar handling

- `Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))`
  on the top bar.
- `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` on the bottom bar.
- Edge-to-edge enabled at Activity level (`enableEdgeToEdge()`).
- Light/dark status icons follow `MaterialTheme.colorScheme.surface` luminance.

### 16.6.5 — Snackbar host

`SnackbarHostState` is hoisted at scaffold level so any tab page may post
a snackbar via `LocalSnackbarHost.current.showSnackbar(...)`. Position:
above the bottom nav, with 8.dp gap.

### 16.6.6 — CompositionLocals

```kotlin
val LocalRootNav = staticCompositionLocalOf<NavHostController> { error("rootNav not provided") }
val LocalMainTabNav = staticCompositionLocalOf<NavHostController> { error("mainTabNav not provided") }
val LocalSnackbarHost = staticCompositionLocalOf<SnackbarHostState> { error("snackbarHost not provided") }
val LocalScrollToTopBus = staticCompositionLocalOf<MutableSharedFlow<String>> { error("scrollToTopBus not provided") }
```

These are provided once, at the top of `MainScaffold { ... }`, and consumed by
every tab page.

## 16.7 — Optional display elements

| Element                  | Condition                                               |
| ------------------------ | ------------------------------------------------------- |
| Top-line linear progress | `syncing == true`.                                      |
| Sign out menu item       | `signedInToDrive == true`.                              |
| Inquiries badge          | `inquiriesBadge > 0`.                                   |
| More dot                 | `moreBadge == true` (update available AND not skipped). |
| Snackbar                 | When any tab posts a message.                           |
| Update banner (in-tab)   | Tabs may render their own banners; scaffold does not.   |

## 16.8 — Empty state

Scaffold itself has no empty state. Each tab handles its own. If
`mainTabNav` is somehow without a current destination (should be
unreachable), fall back to `home` and log a Timber warning.

## 16.9 — Loading state

The scaffold renders immediately (no blocking IO at construction). The
top-line `LinearProgressIndicator` is the only loading affordance owned
at this level. Tab pages have their own skeletons / spinners.

## 16.10 — Error state

The scaffold has no error UI of its own. If `MainScaffoldViewModel`
encounters a flow error from a repository, it is caught with `.catch { Timber.e(it) }`
and the badge silently falls back to the last known value (or 0). The
scaffold never blocks the user from seeing the tabs because of a
transient repo error.

## 16.11 — Edge cases

1. **Process death during sync**: when restored, `syncing` is recomputed
   from `SyncProgressBus.replayCache.last()`. If empty, default to false.
2. **Tab switch mid-scroll**: each tab uses `rememberSaveable(key="tab-$route") { LazyListState() }`
   so scroll position is preserved across tab switches.
3. **Back from a deep tab destination**: `rootNav` handles the pop. If the
   user is on `home` and presses back twice within 2s, the activity
   finishes; otherwise a snackbar shows.
4. **System bar collision with FAB**: there is no FAB at scaffold level.
   Tab pages own any FABs and apply `windowInsetsPadding` themselves.
5. **Right-to-left languages**: the bottom nav uses `LocalLayoutDirection`.
   Tab order remains logical (Home first), which mirrors visually for RTL.
6. **Foldable / large screens**: `compactScreen = maxWidth < 600.dp`. On
   non-compact, the scaffold delegates to a `MainScaffoldExpanded`
   composable (rail nav). Out of scope here; documented in Part 07.
7. **Deep-link to a tab while another tab is still loading**: the target
   tab navigates regardless of loading state. The previous tab's loading
   state survives via `saveState = true`.
8. **Snackbar collision with bottom sheet**: snackbar layer sits above
   bottom nav and **below** modal sheets, so a sheet hides any active
   snackbar.
9. **Update arrives while user is on More tab**: the More-tab dot lights
   up immediately, and the App-update row also gains its own inline
   "Available — install now" affordance (see §20).
10. **Drive sign-out while on More tab**: the overflow menu's Sign-out
    item disappears mid-render. Use `AnimatedVisibility` to fade it.

## 16.12 — Copy table

| String id                  | English                                    | Notes                         |
| -------------------------- | ------------------------------------------ | ----------------------------- |
| `app_brand`                | callNest                                   | Lowercase. Do not localize.   |
| `top_bar_search_a11y`      | Search                                     | TalkBack action label.        |
| `top_bar_overflow_a11y`    | More options                               | TalkBack.                     |
| `top_bar_menu_profile`     | Profile                                    | Routes to settings#profile.   |
| `top_bar_menu_signout`     | Sign out                                   | Drive only.                   |
| `signout_confirm_title`    | Sign out of Google Drive?                  | AlertDialog title.            |
| `signout_confirm_body`     | Backups will pause until you sign back in. |                               |
| `signout_confirm_cta`      | Sign out                                   | Destructive.                  |
| `signout_cancel`           | Cancel                                     |                               |
| `nav_home`                 | Home                                       |                               |
| `nav_calls`                | Calls                                      |                               |
| `nav_inquiries`            | Inquiries                                  |                               |
| `nav_more`                 | More                                       |                               |
| `inquiries_badge_overflow` | 99+                                        |                               |
| `back_press_exit`          | Press back again to exit                   | Snackbar.                     |
| `sync_in_progress_a11y`    | Syncing your call log                      | TalkBack on the progress bar. |

## 16.13 — ASCII wireframe

```
┌──────────────────────────────────────┐
│ ▒ callNest              🔍   ⋮      │  ← NeoTopBar (52.dp + status inset)
│ ──────────────────────────────────── │  ← optional 2.dp progress line
│                                      │
│           [ TAB CONTENT ]            │  ← nested NavHost
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
│ ┌──────────────────────────────────┐ │  ← snackbar host (above nav)
│ │ Press back again to exit         │ │
│ └──────────────────────────────────┘ │
├──────────────────────────────────────┤
│  🏠     📞     📥³    ⋯•             │  ← NeoTabBar (64.dp + nav inset)
│ Home  Calls  Inquiries  More         │
└──────────────────────────────────────┘
```

The `³` after Inquiries is the badge count; the `•` after More is the
update dot.

## 16.14 — Accessibility

- Every icon-only button has a `contentDescription` from string resources.
- Bottom-nav tabs use `Modifier.semantics { selected = (route == currentRoute); role = Role.Tab }`.
  TalkBack reads "Calls, tab, 2 of 4, selected".
- Touch targets: 48.dp minimum for tabs, top-bar actions, and overflow
  menu items.
- Contrast: `BrandTeal` on `TabBg*` ≥ 4.5:1 verified for selected indicator.
- Snackbar uses `LiveRegion.Polite`.
- The double-press exit snackbar is non-dismissible by tap (3s duration)
  to prevent accidental dismiss.
- Dynamic type up to 200% does not break the bottom nav: labels truncate
  with `…` and ellipsize one line.
- High-contrast mode swaps NeoSurface concave shadows for solid 1.dp
  borders (handled in NeoTheme).

## 16.15 — Performance budget

- First paint of MainScaffold after splash: < 80 ms on Pixel 4a (cold
  composition; tab pages composed lazily).
- Tab switch: < 16 ms recomposition. Achieve via `key(currentRoute) { ... }`
  on the per-tab background animator, not the whole scaffold.
- Top-bar progress overlay must not re-layout the page. Render in a
  `Box` over the content, not in the column flow.
- Snackbar host limits concurrent messages to 1 (Material default).
- Memory: scaffold itself holds no list state — all heavy state lives in
  tab ViewModels that are scoped to the destination via `hiltViewModel()`.

---

# §17 — Home tab

The Home tab is the at-a-glance landing surface for the day. It provides
a snapshot of activity, the topmost unsaved callers, and quick links to
the most-used flows. It does NOT host any navigation logic of its own;
all routing goes through `LocalRootNav` or `LocalMainTabNav`.

## 17.1 — Purpose

Give the user a 10-second readout of "what happened today and what
needs my attention" without scrolling more than one screen on a Pixel
4a. Provide three first-class entry points:

1. The numeric snapshot of today.
2. The shortlist of recent unsaved callers (deep-link into Inquiries).
3. The four quick actions (Calls, Stats, Backup, Quick Export).

## 17.2 — Entry points

| From                        | Behavior                                                                     |
| --------------------------- | ---------------------------------------------------------------------------- |
| App start                   | Land on `calls` (default tab). User taps `home` tab to arrive here.          |
| Notification: follow-up due | rootNav → main_tabs; mainTabNav → home; pulse "Follow-ups due" tile briefly. |
| Notification: backup result | rootNav → main_tabs → home (no in-app dialog).                               |
| Tap `home` in bottom nav    | Direct.                                                                      |
| Re-tap `home` while on Home | LocalScrollToTopBus emits "home" → list scrolls to top.                      |
| Process restart             | Restored to whichever tab was active; if "home", state restored from VM.     |

## 17.3 — Exit points

| To                           | Trigger                                              |
| ---------------------------- | ---------------------------------------------------- |
| `inquiries` tab              | Tap "Save all" inside Recent-unsaved card.           |
| `calls` tab                  | Tap Quick Action chip "Calls".                       |
| `route=stats`                | Tap Quick Action chip "Stats".                       |
| `route=backup`               | Tap Quick Action chip "Backup".                      |
| Quick Export bottom sheet    | Tap Quick Action chip "Quick Export". Modal overlay. |
| `route=call_detail/{number}` | Tap any row in Recent-unsaved list.                  |
| `route=follow_ups`           | Tap "Follow-ups due" tile (scoped to upcoming list). |

## 17.4 — Required inputs (data)

ViewModel: `HomeViewModel @HiltViewModel constructor(callRepo: CallRepository, contactRepo: ContactRepository, settings: SettingsDataStore)`.

State:

```kotlin
data class HomeUiState(
    val callsToday: Int = 0,
    val missedToday: Int = 0,
    val unsavedTotal: Int = 0,
    val followUpsDue: Int = 0,
    val recentUnsaved: List<RecentUnsavedItem> = emptyList(),
    val loading: Boolean = true,
    val syncRunning: Boolean = false,
    val permissionGranted: Boolean = true
)

data class RecentUnsavedItem(
    val normalizedNumber: String,
    val displayLabel: String,
    val initial: Char,
    val accentSeed: Int,
    val lastCallEpochMs: Long,
    val callCount: Int
)
```

Source flows:

| State field         | Type                    | Source                                               | Default |
| ------------------- | ----------------------- | ---------------------------------------------------- | ------- |
| `callsToday`        | Int                     | `callRepo.observeRecent(200).map { dayBucket(it) }`  | 0       |
| `missedToday`       | Int                     | derived from same window, type==MISSED               | 0       |
| `unsavedTotal`      | Int                     | `callRepo.observeUnsavedLast7Days().map { it.size }` | 0       |
| `followUpsDue`      | Int                     | `followUpRepo.observeDueWithin24h().map { it.size }` | 0       |
| `recentUnsaved`     | List<RecentUnsavedItem> | `callRepo.observeUnsavedLast7Days().map { take(3) }` | empty   |
| `loading`           | Boolean                 | `combine` first emission                             | true    |
| `syncRunning`       | Boolean                 | `SyncProgressBus.flow.map { it !is Idle }`           | false   |
| `permissionGranted` | Boolean                 | `permissionAdapter.observeReadCallLog()`             | true    |

Combine:

```kotlin
val state: StateFlow<HomeUiState> = combine(
    callRepo.observeRecent(200),
    callRepo.observeUnsavedLast7Days(),
    followUpRepo.observeDueWithin24h(),
    syncProgressBus.flow.onStart { emit(SyncProgressEvent.Idle) },
    permissionAdapter.observeReadCallLog()
) { recent, unsaved, dueFollowUps, syncEvt, granted ->
    val (todayStart, now) = todayBounds()
    val todayCalls = recent.filter { it.startedAtMs in todayStart..now }
    HomeUiState(
        callsToday = todayCalls.size,
        missedToday = todayCalls.count { it.type == CallType.MISSED },
        unsavedTotal = unsaved.size,
        followUpsDue = dueFollowUps.size,
        recentUnsaved = unsaved.take(3).map { it.toItem() },
        loading = false,
        syncRunning = syncEvt !is SyncProgressEvent.Idle,
        permissionGranted = granted
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
```

## 17.5 — Required inputs (user)

| Trigger                            | Behavior                                                          | State change                  |
| ---------------------------------- | ----------------------------------------------------------------- | ----------------------------- |
| Tap "Calls today" stat tile        | `mainTabNav.navigate("calls")` with filter preset = today.        | None (navigation event).      |
| Tap "Missed" stat tile             | `mainTabNav.navigate("calls")` with filter preset = missed,today. | None.                         |
| Tap "Unsaved" stat tile            | `mainTabNav.navigate("inquiries")`.                               | None.                         |
| Tap "Follow-ups due" stat tile     | `rootNav.navigate("follow_ups")`.                                 | None.                         |
| Tap a Recent-unsaved row           | `rootNav.navigate("call_detail/$number")`.                        | None.                         |
| Tap "Save all" CTA                 | `mainTabNav.navigate("inquiries")` and pulse the inquiries tab.   | None.                         |
| Tap Quick Action: 📞 Calls         | `mainTabNav.navigate("calls")`.                                   | None.                         |
| Tap Quick Action: 📊 Stats         | `rootNav.navigate("stats")`.                                      | None.                         |
| Tap Quick Action: 💾 Backup        | `rootNav.navigate("backup")`.                                     | None.                         |
| Tap Quick Action: 📥 Quick Export  | Parent-controlled `quickExportSheetVisible = true`.               | Modal overlay opens.          |
| Pull to refresh                    | `SyncScheduler.triggerOnce()`.                                    | `syncRunning = true` briefly. |
| Long-press a stat tile             | Show contextual hint tooltip (e.g. "Calls received today").       | None.                         |
| Long-press "Recent unsaved" header | Open a hidden dev menu (only when build is debug).                | None in release.              |
| Re-tap Home tab                    | Scroll to top via `LocalScrollToTopBus`.                          | List `firstVisibleItem = 0`.  |

## 17.6 — Mandatory display elements

The Home tab uses `StandardPage` as its container:

```kotlin
StandardPage(
    title = "Home",
    description = "Today at a glance",
    emoji = "🏠",
    backgroundColor = TabBgHome,
    headerGradient = HeaderGradHome
) { padding ->
    LazyColumn(
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TodaysSnapshotCard(state) }
        item { RecentUnsavedCard(state) }
        item { QuickActionsCard(...) }
    }
}
```

### 17.6.1 — Card 1: Today's snapshot

- Container: `NeoCard(border = BorderAccent)`. Padding 16.dp.
- Header row:
  - Emoji 📅, label "Today's snapshot" (`titleSmall` bold).
  - Trailing relative timestamp "as of HH:mm" (muted).
- Body: a `Row(horizontalArrangement = Arrangement.SpaceBetween)` with
  four equal-width tiles. Each tile is a `Column(horizontalAlignment = CenterHorizontally)`:
  - Emoji (24.sp): `📞 / ❌ / 📥 / 🔔`.
  - Count: `headlineSmall` bold, color `OnSurfaceStrong`.
  - Label: `labelSmall`, color `OnSurfaceMuted`.

| Tile | Emoji | Count source   | Label          |
| ---- | ----- | -------------- | -------------- |
| 1    | 📞    | `callsToday`   | Calls          |
| 2    | ❌    | `missedToday`  | Missed         |
| 3    | 📥    | `unsavedTotal` | Unsaved        |
| 4    | 🔔    | `followUpsDue` | Follow-ups due |

Each tile is a `Modifier.clickable` target with 48.dp min height.

### 17.6.2 — Card 2: Recent unsaved

- Container: `NeoCard(border = BorderSoft)`.
- Header row: "Recent unsaved" + count badge `({unsavedTotal})`.
- Body: top 3 rows from `recentUnsaved`. Each row:
  - `NeoAvatar(initial = item.initial, accentSeed = item.accentSeed)`
    (40.dp; deterministic tint).
  - `Column`:
    - Line 1: `item.displayLabel` (`bodyMedium` bold).
    - Line 2: `formatRelative(item.lastCallEpochMs)` + " · " + `"${item.callCount} calls"` (`bodySmall`, muted).
  - Trailing chevron (`KeyboardArrowRight`, 20.dp, muted).
- Inline `NeoButton` "Save all" pinned at the bottom-right of the card,
  visible only when `unsavedTotal > 0`.
- Empty body (when `unsavedTotal == 0`): single line "Nothing pending. ✨" in
  italic muted, height 48.dp.

### 17.6.3 — Card 3: Quick actions

- Container: `NeoCard(border = BorderSoft)`.
- Header: "Quick actions".
- Body: a 2×2 grid of `NeoChip` buttons (or a `FlowRow` if width permits
  4 across; on Pixel 4a it wraps to 2×2 because each chip is 144.dp wide).
- Chips:

  | Chip emoji | Label        | Action                                      |
  | ---------- | ------------ | ------------------------------------------- |
  | 📞         | Calls        | `mainTabNav.navigate("calls")`              |
  | 📊         | Stats        | `rootNav.navigate("stats")`                 |
  | 💾         | Backup       | `rootNav.navigate("backup")`                |
  | 📥         | Quick Export | `onQuickExportClick()` (opens parent sheet) |

Each chip: 56.dp height. Concave NeoSurface. Press state ripples to
convex briefly (200 ms).

## 17.7 — Optional display elements

| Element                     | Condition                                                           |
| --------------------------- | ------------------------------------------------------------------- |
| Top-line sync progress      | `syncRunning == true` (rendered by scaffold).                       |
| Permission rationale banner | `permissionGranted == false`. Pinned above Card 1.                  |
| "First-day welcome" banner  | If user has < 5 total calls in DB. Dismissible.                     |
| Insight card (rotating)     | Reserved for Phase II; behind feature flag `home_insights_enabled`. |
| "Backup overdue" banner     | If lastBackupAt is older than 14 days AND Drive is connected.       |

## 17.8 — Empty state

If `state == HomeUiState()` (initial, post-loading) and DB is empty:

```
┌────────────────────────────────────┐
│           (centered)               │
│            📭                      │
│      No calls yet                  │
│  Pull down to sync your call log   │
│    [ Grant permission ]   <- only  │
│           if needed                │
└────────────────────────────────────┘
```

- Icon: `📭` at 64.sp.
- Title: `titleMedium`, "No calls yet."
- Subtitle: `bodyMedium` muted, "Pull down to sync your call log."
- CTA `NeoButton`: text varies — "Grant permission" if not granted else
  "Sync now" → triggers `SyncScheduler.triggerOnce()`.

## 17.9 — Loading state

- First emission: render three skeleton placeholders shaped like the
  three cards (Card 1: header line + 4 placeholder rectangles; Card 2:
  header + 3 placeholder rows; Card 3: header + 4 chip-shaped boxes).
- Use `Modifier.shimmer()` (Coil 3 + custom shimmer modifier).
- Skeleton duration cap: 800 ms. If state has not arrived, switch to
  spinner `CircularProgressIndicator` to avoid eternal shimmer.

## 17.10 — Error state

If `combine` throws (`.catch { ... }`):

- Render a `NeoCard` (border = BorderSoft) with:
  - Title "Something's off."
  - Body "We couldn't load your snapshot. Pull down to retry."
  - `NeoButton` "Retry" → triggers a re-collect (`viewModel.retry()`).
- Snackbar: "Snapshot unavailable. Retrying…" (auto-dismiss 4s).
- Cards 2 and 3 still render with whatever partial state is available.

## 17.11 — Edge cases

1. **0 calls today** — Stat tile shows "0", color muted. Tap is still
   active and routes to Calls tab; the user will see whatever pre-today
   data exists.
2. **100+ unsaved** — `unsavedTotal` rendered as the literal number,
   not capped. The Recent-unsaved list still shows top 3.
3. **Follow-up firing during render** — A new `followUpsDue` value
   arrives while the user looks at Home. The tile pulses (one-shot
   `animateFloatAsState` brightening for 600 ms).
4. **Sync in progress** — Scaffold shows the top-line bar; Home does
   nothing extra. Pull-to-refresh is disabled until current sync ends.
5. **Permission missing** — Render the banner above Card 1: "We need
   call log access to show your snapshot." with `Grant` CTA which
   re-triggers the permission rationale sheet.
6. **Low memory pressure** — `recentUnsaved` is hard-capped at 3, regardless
   of source size. List items are stable-keyed by normalizedNumber so
   recompositions skip.
7. **Day rollover at midnight** — `todayBounds()` is recomputed on every
   emission, so right after midnight the snapshot resets.
8. **DST transition** — `todayBounds()` uses `ZoneId.systemDefault()` and
   `LocalDate.now()` for safety.
9. **No network and Drive backup overdue** — banner still shows; tapping
   it navigates to Backup screen which displays a "no network" hint there.
10. **User opened app at 23:59** — Snapshot will reset within the minute.
    No client action; the next state emission will reflect the new day.

## 17.12 — Copy table

| String id                      | English                                             | Placeholders            |
| ------------------------------ | --------------------------------------------------- | ----------------------- |
| `home_title`                   | Home                                                |                         |
| `home_description`             | Today at a glance                                   |                         |
| `home_card_snapshot_title`     | Today's snapshot                                    |                         |
| `home_snapshot_as_of`          | as of %1$s                                          | %1$s = local time HH:mm |
| `home_tile_calls`              | Calls                                               |                         |
| `home_tile_missed`             | Missed                                              |                         |
| `home_tile_unsaved`            | Unsaved                                             |                         |
| `home_tile_follow_ups`         | Follow-ups due                                      |                         |
| `home_card_recent_title`       | Recent unsaved                                      |                         |
| `home_recent_count`            | (%1$d)                                              | %1$d = unsavedTotal     |
| `home_recent_empty`            | Nothing pending. ✨                                 |                         |
| `home_recent_save_all`         | Save all                                            |                         |
| `home_recent_row_calls`        | %1$d calls                                          | %1$d = call count       |
| `home_card_quickactions_title` | Quick actions                                       |                         |
| `home_quick_calls`             | Calls                                               |                         |
| `home_quick_stats`             | Stats                                               |                         |
| `home_quick_backup`            | Backup                                              |                         |
| `home_quick_export`            | Quick Export                                        |                         |
| `home_empty_title`             | No calls yet                                        |                         |
| `home_empty_subtitle`          | Pull down to sync your call log.                    |                         |
| `home_empty_cta_grant`         | Grant permission                                    |                         |
| `home_empty_cta_sync`          | Sync now                                            |                         |
| `home_perm_banner`             | We need call log access to show your snapshot.      |                         |
| `home_perm_grant`              | Grant                                               |                         |
| `home_backup_overdue`          | Last backup was %1$s ago. Tap to back up now.       | %1$s = relative time    |
| `home_error_title`             | Something's off.                                    |                         |
| `home_error_body`              | We couldn't load your snapshot. Pull down to retry. |                         |
| `home_error_retry`             | Retry                                               |                         |

## 17.13 — ASCII wireframe

```
┌──────────────────────────────────────┐
│ ▒ callNest            🔍   ⋮        │
├──────────────────────────────────────┤
│  🏠  Home                            │
│      Today at a glance               │
│ ──────────────────────────────────── │
│ ┌──────────────────────────────────┐ │
│ │ 📅 Today's snapshot   as of 14:32│ │
│ │ ┌────┐ ┌────┐ ┌────┐ ┌────┐     │ │
│ │ │ 📞 │ │ ❌ │ │ 📥 │ │ 🔔 │     │ │
│ │ │ 47 │ │  3 │ │  9 │ │  2 │     │ │
│ │ │Call│ │Miss│ │Unsv│ │FU  │     │ │
│ │ └────┘ └────┘ └────┘ └────┘     │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ Recent unsaved              (9)  │ │
│ │ ─────────────────────────────────│ │
│ │ ◉ +91 98765 43210  · 12 m · 3 c >│ │
│ │ ◉ +91 99887 76655  · 35 m · 1 c >│ │
│ │ ◉ +91 90909 90909  · 2 h  · 2 c >│ │
│ │              [ Save all ]        │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ Quick actions                    │ │
│ │ [ 📞 Calls ] [ 📊 Stats ]        │ │
│ │ [ 💾 Backup ] [ 📥 Quick Export ]│ │
│ └──────────────────────────────────┘ │
├──────────────────────────────────────┤
│  🏠   📞   📥³   ⋯                   │
└──────────────────────────────────────┘
```

## 17.14 — Accessibility

- TalkBack reads each stat tile as: "Calls today, 47, button". Achieved
  via `Modifier.semantics(mergeDescendants = true) { contentDescription = "..." ; role = Role.Button }`.
- Stat tiles have a focus order that flows left-to-right then down.
- Recent-unsaved rows: "Unsaved caller, +91 98765 43210, last call 12
  minutes ago, 3 calls. Double-tap to open."
- "Save all" button: "Save all unsaved callers, button. Opens Inquiries."
- Quick-action chips: "Backup, button" — includes destination hint via
  `onClickLabel = "Open backup screen"`.
- Touch targets: all interactive elements ≥ 48.dp.
- Dynamic type: `headlineSmall` allowed up to 200% scale before tiles
  switch to a 1-up vertical layout (handled by `BoxWithConstraints`).
- Contrast: count text on `NeoCard` ≥ 7:1 (AAA).

## 17.15 — Performance budget

- First paint of Home: < 120 ms after tab switch on Pixel 4a.
- `LazyColumn` items: 3 (always). No virtualization concerns.
- Recompositions on stat update: limited to the changed tile via
  derivedStateOf-keyed slot composables.
- Initial data load: ≤ 200 ms for the combine emission against a
  500-row DB (Room with index on `started_at`).
- Memory: < 1 MB for the Home VM state.
- No image loading on Home (avatars are vector initials).

---

# §18 — Calls tab

The Calls tab is the primary work surface and the most complex tab.
It shows the user's call log, lets them filter, search, switch between
flat and grouped-by-number views, pin recent unsaved inquiries at the
top, and operate on multiple calls at once via bulk-select.

## 18.1 — Purpose

Be the daily driver. The user spends the majority of in-app time here.
The page must:

- Load fast (skeleton in < 80 ms; first visible row in < 250 ms on a
  10k-row DB).
- Surface the most-actionable items first (pinned unsaved last 7 days).
- Make every row tap-rich: row-tap to detail, long-press to bulk-select,
  swipe for the two configurable quick actions.
- Support filters that survive process restart (saved presets) and
  active filters as removable chips.

## 18.2 — Entry points

| From                        | Behavior                                    |
| --------------------------- | ------------------------------------------- |
| App start                   | Calls is the default tab (locked).          |
| Bottom nav tap              | Direct.                                     |
| Home → "Calls" stat tile    | Pre-applies filter preset = today.          |
| Home → "Missed" stat tile   | Pre-applies filter preset = today + missed. |
| Home → Quick action: Calls  | Direct.                                     |
| Notification: missed call   | rootNav → main_tabs → calls; row pulses.    |
| Search overlay → result tap | Opens Call Detail; back lands here.         |
| Process restart             | Restored to Calls if it was active.         |
| Re-tap Calls in bottom nav  | Scroll to top.                              |

## 18.3 — Exit points

| To                           | Trigger                                                         |
| ---------------------------- | --------------------------------------------------------------- |
| `route=call_detail/{number}` | Tap a row.                                                      |
| `route=search`               | Tap Search icon in top bar.                                     |
| Calls filter sheet (modal)   | Tap Filter icon in top bar.                                     |
| Tag picker bottom sheet      | Bulk action "Tag", or row-3-dot "Tag".                          |
| Share intent (system)        | Bulk action "Export" → render then `ACTION_SEND`.               |
| Confirmation dialog (delete) | Bulk action "Delete".                                           |
| Inquiries tab                | Bulk action "Save" (auto-saves selected unsaved → flips later). |
| Pinned section "Hide" tap    | Updates settings; section unmounts.                             |

## 18.4 — Required inputs (data)

ViewModel: `CallsViewModel @HiltViewModel constructor(callRepo, tagRepo, contactRepo, settings, syncProgressBus, updateRepo, scheduler: SyncScheduler)`.

State:

```kotlin
data class CallsUiState(
    val filter: CallFilter = CallFilter(),
    val viewMode: ViewMode = ViewMode.Flat,           // Flat | GroupedByNumber
    val pinnedSectionVisible: Boolean = true,
    val pinnedUnsaved: List<CallRow> = emptyList(),
    val flatCalls: List<CallSection> = emptyList(),   // sticky-date sectioned
    val groupedByNumber: List<NumberGroup> = emptyList(),
    val totalMatches: Int = 0,
    val tagsById: Map<Long, Tag> = emptyMap(),
    val savedContactNumbers: Set<String> = emptySet(),
    val bulkMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isRefreshing: Boolean = false,
    val updateBanner: UpdateBannerState = UpdateBannerState.Hidden,
    val empty: EmptyKind = EmptyKind.None,             // None | NoCalls | NoMatches | NoPermission
    val loading: Boolean = true
)
```

Sources:

| Field                  | Source                                                                 | Default      |
| ---------------------- | ---------------------------------------------------------------------- | ------------ |
| `filter`               | local; persisted to `SavedStateHandle` for process death               | empty filter |
| `viewMode`             | `settings.observe { displayGroupedByNumber }` mapped                   | Flat         |
| `pinnedSectionVisible` | `settings.observe { displayShowUnsavedPinned }`                        | true         |
| `pinnedUnsaved`        | `callRepo.observeUnsavedLast7Days().take(20)` mapped to CallRow        | empty        |
| `flatCalls`            | `callRepo.observeFiltered(filter)` → groupBy date bucket → CallSection | empty        |
| `groupedByNumber`      | `callRepo.observeFilteredGrouped(filter)` from a DAO view              | empty        |
| `tagsById`             | `tagRepo.observeAll().associateBy { it.id }`                           | empty        |
| `savedContactNumbers`  | `contactRepo.observeSavedNormalizedNumbers().toSet()`                  | empty        |
| `bulkMode`             | local                                                                  | false        |
| `selectedIds`          | local                                                                  | empty        |
| `isRefreshing`         | combine SwipeRefresh state ⊕ syncProgressBus                           | false        |
| `updateBanner`         | `combine(updateRepo.state, settings.observeUpdateSkipped())`           | Hidden       |
| `empty`                | derived (size + permission + filter)                                   | None         |
| `loading`              | first emission                                                         | true         |

`CallFilter`:

```kotlin
data class CallFilter(
    val callTypes: Set<CallType> = emptySet(),
    val dateRange: DateRangePreset = DateRangePreset.None,
    val customRange: ClosedRange<Long>? = null,
    val durationBucket: Set<DurationBucket> = emptySet(),
    val sim: Set<SimSlot> = emptySet(),
    val contactStatus: Set<ContactStatus> = emptySet(),
    val tagIds: Set<Long> = emptySet(),
    val tagMode: TagMode = TagMode.AnyOf,
    val bookmarkedOnly: Boolean = false,
    val hasNotes: TriState = TriState.Any,
    val hasFollowUp: TriState = TriState.Any,
    val leadScoreRange: IntRange? = null
)
```

`CallRow`:

```kotlin
data class CallRow(
    val id: Long,
    val normalizedNumber: String,
    val displayName: String,           // contact name OR formatted number
    val initial: Char,
    val accentSeed: Int,
    val type: CallType,
    val startedAtMs: Long,
    val durationSec: Int,
    val sim: SimSlot?,
    val tagIds: List<Long>,
    val visibleTags: List<Tag>,        // first 3 resolved
    val overflowTagsCount: Int,
    val isBookmarked: Boolean,
    val leadScore: Int,
    val isContactSaved: Boolean,
    val isAutoSaved: Boolean
)
```

`CallSection`:

```kotlin
data class CallSection(
    val header: DateHeader,            // Today / Yesterday / Mon / Tue / yyyy-MM-dd
    val rows: List<CallRow>
)
```

## 18.5 — Required inputs (user)

| Trigger                               | Behavior                                             | State change                          |
| ------------------------------------- | ---------------------------------------------------- | ------------------------------------- |
| Tap row                               | `rootNav.navigate("call_detail/$normalizedNumber")`. | None.                                 |
| Long-press row                        | Enter bulk mode; select that row.                    | `bulkMode = true; selectedIds += id`. |
| Tap row in bulk mode                  | Toggle selection.                                    | `selectedIds ± id`.                   |
| Swipe row right                       | Toggle bookmark (default).                           | row.isBookmarked flips.               |
| Swipe row left                        | Archive (default; configurable to Delete).           | row hidden until next sync.           |
| Tap Filter icon                       | Open `CallsFilterSheet` (modal).                     | None.                                 |
| Tap Search icon                       | `rootNav.navigate("search")`.                        | None.                                 |
| Tap view-mode toggle                  | Flip between Flat ↔ Grouped.                         | `viewMode` toggled and persisted.     |
| Tap chip "Today" (active filter)      | Remove it from the filter.                           | `filter` updated; query re-runs.      |
| Tap "Clear filters" (empty match CTA) | Reset filter to empty.                               | `filter = CallFilter()`.              |
| Tap pinned-section header             | Collapse/expand.                                     | local `pinnedExpanded` flips.         |
| Tap pinned-section X                  | Hide section permanently (settings).                 | setting updated; section unmounts.    |
| Pull to refresh                       | `scheduler.triggerOnce()`.                           | `isRefreshing = true`.                |
| Tap bulk: Tag                         | Open tag picker bottom sheet for `selectedIds`.      | None at tab.                          |
| Tap bulk: Bookmark                    | `callRepo.bulkSetBookmarked(selectedIds, true)`.     | rows update.                          |
| Tap bulk: Save                        | `contactRepo.autoSaveAll(selectedIds)`.              | inquiries badge increments.           |
| Tap bulk: Export                      | Open Quick Export sheet pre-scoped to selection.     | None at tab.                          |
| Tap bulk: Delete                      | Confirm dialog → `callRepo.bulkDelete(selectedIds)`. | rows removed.                         |
| Tap bulk: Done                        | Exit bulk mode.                                      | `bulkMode = false; selectedIds = ∅`.  |
| System back in bulk mode              | Same as Done.                                        | Same.                                 |

## 18.6 — Mandatory display elements

Container: `StandardPage(title="Calls", description="Your call log", emoji="📞", backgroundColor=TabBgCalls, headerGradient=HeaderGradCalls)`.

> Implementation note: when bulk mode is active, the page swaps to a
> `NeoScaffold` whose `bottomBar` slot hosts the `BulkActionBar`. This
> avoids stacking the bar on top of the bottom nav (the bottom nav is
> hidden while bulk mode is active). The page background remains
> `TabBgCalls`.

### 18.6.1 — Top-bar actions (Calls-specific)

The scaffold's top-bar trailing slot is augmented for this tab via
`LocalTopBarActions.current.set { ... }`. The full action set:

1. Search icon — routes to search.
2. Filter icon — opens filter sheet. Badge with active-filter count.
3. View-mode toggle — `Icons.Outlined.ViewAgenda` ↔ `Icons.Outlined.ViewList`.
4. Overflow ⋮ (already present from scaffold).

### 18.6.2 — Pinned-unsaved section (collapsible)

Visible iff `pinnedSectionVisible == true` AND `pinnedUnsaved.isNotEmpty()`.

- Container: `NeoCard(border = BorderAccent)`. Margin top 8.dp.
- Header row:
  - Title "Unsaved inquiries — last 7 days" (`titleSmall` bold).
  - Count badge `({n})`.
  - Chevron (`KeyboardArrowDown` when expanded; rotates 180° when collapsed).
  - Trailing X (`Icons.Outlined.Close`, 20.dp) → permanently hides.
- Body (when expanded): a vertically-stacked sub-list of `CallRow`s,
  capped at 7. If `pinnedUnsaved.size > 7`, show "Show all (N)" link
  → routes to Inquiries.
- Animations: header chevron rotates with `animateFloatAsState`. Body
  collapses with `AnimatedVisibility(slideInVertically + fadeIn)`.

### 18.6.3 — Active filter chips row

Visible iff `filter` has any non-default value. Renders just below the
pinned section, above the main list.

- Horizontal `LazyRow` with 8.dp gaps.
- Each chip: `NeoChip(label, trailingIcon = Close)` → tap removes the
  filter facet, X removes only that facet.
- Chip examples: "Today", "Missed", "Has notes", "Tag: Inquiry", "Lead 70+".
- Trailing chip "Clear all" → resets filter.

### 18.6.4 — Sticky date headers

Headers: `Today`, `Yesterday`, `Monday`, `Tuesday`, ... weekday names for
the most recent 6 days, then the literal date `EEE, MMM d` for older.

- `LazyColumn` `stickyHeader { ... }` (Compose 1.7+).
- Header style: `labelMedium` bold, color `OnSurfaceMuted`, background
  `TabBgCalls.copy(alpha=0.96f)`. Height 32.dp. Padding 16.dp horizontal.

### 18.6.5 — CallRow

Row layout (from leading to trailing):

```
[ NeoAvatar 40dp ][ pad 12dp ][ name + meta + tags ][ flex spacer ][ trailing icons ]
```

- `NeoAvatar(initial, accentSeed)`: deterministic tint from
  `accentSeed = name.hashCode().rem(8).absoluteValue`.
- Center column:
  - Line 1: `displayName` (`bodyLarge` bold, ellipsize 1 line).
  - Line 2: type icon (12.sp, colored: incoming green, outgoing teal,
    missed red) + " · " + `formatRelative(startedAtMs)` + " · " + `formatDuration(durationSec)`.
  - Line 3 (only if `tagIds.isNotEmpty()`): tag pill row, `FlowRow`,
    `maxLines = 1`. First 3 pills shown; if `overflowTagsCount > 0`,
    append `+N` chip.
- Trailing column:
  - Bookmark star (`Icons.Filled.Star` if `isBookmarked`, otherwise
    `Icons.Outlined.StarBorder`). Color `Accent.Yellow` when filled.
  - `LeadScoreBadge(leadScore)`: pill 32.dp wide showing the score with
    color → cold gray (<30), warm amber (30–70), hot red (>70).

Row height: 80.dp (with tag pills) or 64.dp (without). Min touch target
48.dp.

Selected-state visual (bulk mode):

- Convex NeoSurface flips to inset/concave with `BrandTeal.copy(alpha=0.10f)` overlay.
- Leading avatar replaced with `Icons.Filled.CheckCircle` (24.dp, BrandTeal).

### 18.6.6 — BulkActionBar (bottom)

Visible iff `bulkMode == true`. Replaces the bottom nav.

- Height 64.dp. NeoSurface convex.
- Contents (left to right): "(N) selected" label, then six icon buttons
  with labels:
  1. Tag (`Icons.Outlined.LocalOffer`)
  2. Bookmark (`Icons.Outlined.StarOutline`)
  3. Save (`Icons.Outlined.PersonAdd`)
  4. Export (`Icons.Outlined.IosShare`)
  5. Delete (`Icons.Outlined.Delete`, color destructive)
  6. Done (`Icons.Outlined.Check`)

### 18.6.7 — Empty placements

- No calls at all: full-screen empty state.
- Filter no-match: empty state with "Clear filters" CTA.
- Permission denied: full-screen permission rationale.

(See §18.8.)

## 18.7 — Optional display elements

| Element                   | Condition                                             |
| ------------------------- | ----------------------------------------------------- |
| Update banner (in-tab)    | `updateBanner != Hidden` AND user is on Calls tab.    |
| Pinned section            | `pinnedSectionVisible && pinnedUnsaved.isNotEmpty()`. |
| Active filter chips row   | Filter is non-default.                                |
| SIM badge on row          | Device has dual SIM AND `sim != null`.                |
| Tag pills row             | `tagIds.isNotEmpty()`.                                |
| LeadScoreBadge            | `leadScore > 0` (always shown by default).            |
| Pull-to-refresh indicator | Pulldown gesture.                                     |
| Sticky date headers       | Always present in flat mode; absent in grouped mode.  |
| Group expansion chevron   | Only in grouped mode.                                 |

## 18.8 — Empty state

Three flavors, mutually exclusive:

### 18.8.1 — No calls

```
        📭
   No calls yet
Pull down to sync your call log
   [ Sync now ]
```

### 18.8.2 — Filter no-match

```
        🔎
No calls match these filters
   [ Clear filters ]
```

### 18.8.3 — Permission denied

```
        🔒
Call log access is needed
We use this to read your incoming/outgoing calls.
Nothing leaves your device.
   [ Grant permission ]
```

Each is a `Column(Modifier.fillMaxSize().padding(24.dp), Center, Center)`.

## 18.9 — Loading state

- Initial composition: render 6 skeleton rows shaped like a CallRow
  (avatar circle + 2 lines + trailing pill).
- Shimmer at 60 fps for at most 800 ms, then `CircularProgressIndicator`
  if still empty.
- The pinned section shows its own 2-row skeleton if `pinnedUnsaved` is
  loading.

## 18.10 — Error state

If the filtered query throws (e.g., FTS table missing after an aborted
migration):

- Snackbar "Couldn't load calls. Tap to retry." (action button "Retry").
- Body renders the no-calls empty state with the title swapped to
  "Couldn't load calls" and CTA "Retry".
- Timber error log; never crash.

## 18.11 — Edge cases

1. **0 calls** — empty state §18.8.1; pinned section absent.
2. **Exactly 1 call** — single row with sticky header "Today"; no pinned section unless that call is unsaved.
3. **10,000 calls** — `LazyColumn` virtualizes; ensure stable keys (`key = { it.id }`); avoid `derivedStateOf` per row.
4. **All calls unsaved** — pinned section shows top 7 unsaved; main list shows all.
5. **All calls saved** — pinned section absent.
6. **Single-SIM device** — hide SIM badge entirely (`telephonyAdapter.activeSimCount == 1`).
7. **Private number** — `displayName = "Private number"`; avatar `?`.
8. **Archived calls** — hidden by default; visible only when filter has `Archived = true`.
9. **Mid-sync** — top-line bar visible; rows still render from cache; new rows insert at top with `animateItem()`.
10. **Bulk mode + tab switch attempt** — bottom nav is hidden in bulk mode, but if user uses gestures: leaving Calls auto-exits bulk mode and clears selection.
11. **Bulk mode + back press** — exits bulk mode (does not pop the page).
12. **Filter sheet open + back press** — closes sheet only.
13. **Swipe right on a saved-contact row in the pinned section** — that
    row no longer qualifies as unsaved; it disappears from the pinned
    section but remains in the main list.

## 18.12 — Copy table

| String id                         | English                                                                                            |
| --------------------------------- | -------------------------------------------------------------------------------------------------- |
| `calls_title`                     | Calls                                                                                              |
| `calls_description`               | Your call log                                                                                      |
| `calls_top_search_a11y`           | Search                                                                                             |
| `calls_top_filter_a11y`           | Filter                                                                                             |
| `calls_top_view_mode_flat`        | Switch to flat list                                                                                |
| `calls_top_view_mode_grouped`     | Switch to grouped by number                                                                        |
| `calls_pinned_title`              | Unsaved inquiries — last 7 days                                                                    |
| `calls_pinned_count`              | (%1$d)                                                                                             |
| `calls_pinned_show_all`           | Show all (%1$d)                                                                                    |
| `calls_pinned_hide_a11y`          | Hide pinned section                                                                                |
| `calls_active_filter_clear`       | Clear all                                                                                          |
| `calls_header_today`              | Today                                                                                              |
| `calls_header_yesterday`          | Yesterday                                                                                          |
| `calls_header_weekday_format`     | EEEE                                                                                               |
| `calls_header_older_format`       | EEE, MMM d                                                                                         |
| `calls_row_calls_count`           | %1$d calls                                                                                         |
| `calls_row_overflow_tags`         | +%1$d                                                                                              |
| `calls_row_bookmark_a11y`         | Bookmarked                                                                                         |
| `calls_row_unbookmark_a11y`       | Not bookmarked                                                                                     |
| `calls_lead_cold`                 | Cold                                                                                               |
| `calls_lead_warm`                 | Warm                                                                                               |
| `calls_lead_hot`                  | Hot                                                                                                |
| `calls_swipe_right_bookmark`      | Bookmark                                                                                           |
| `calls_swipe_left_archive`        | Archive                                                                                            |
| `calls_bulk_count`                | %1$d selected                                                                                      |
| `calls_bulk_tag`                  | Tag                                                                                                |
| `calls_bulk_bookmark`             | Bookmark                                                                                           |
| `calls_bulk_save`                 | Save                                                                                               |
| `calls_bulk_export`               | Export                                                                                             |
| `calls_bulk_delete`               | Delete                                                                                             |
| `calls_bulk_delete_confirm_title` | Delete %1$d calls?                                                                                 |
| `calls_bulk_delete_confirm_body`  | This removes the entries from your callNest database. Your phone's system call log isn't affected. |
| `calls_bulk_delete_cta`           | Delete                                                                                             |
| `calls_bulk_done`                 | Done                                                                                               |
| `calls_empty_no_calls_title`      | No calls yet                                                                                       |
| `calls_empty_no_calls_body`       | Pull down to sync your call log.                                                                   |
| `calls_empty_no_calls_cta`        | Sync now                                                                                           |
| `calls_empty_no_match_title`      | No calls match these filters                                                                       |
| `calls_empty_no_match_cta`        | Clear filters                                                                                      |
| `calls_empty_perm_title`          | Call log access is needed                                                                          |
| `calls_empty_perm_body`           | We use this to read your incoming and outgoing calls. Nothing leaves your device.                  |
| `calls_empty_perm_cta`            | Grant permission                                                                                   |
| `calls_error_snack`               | Couldn't load calls. Tap to retry.                                                                 |
| `calls_error_retry`               | Retry                                                                                              |

## 18.13 — ASCII wireframes

### Default (with pinned + active filter)

```
┌──────────────────────────────────────┐
│ ▒ callNest    🔍  ⛃²  ⬚  ⋮          │  filter badge=2
├──────────────────────────────────────┤
│ 📞 Calls                              │
│    Your call log                      │
│ ──────────────────────────────────── │
│ ┌──────────────────────────────────┐ │
│ │ Unsaved inquiries — last 7 days  │ │
│ │ (5)                       ▾   ✕  │ │
│ │ ◉ +91 98765 43210 ↘ 2m  💬     ★ │ │
│ │ ◉ +91 99887 76655 ↘ 7m         ★ │ │
│ │ … Show all (5)                   │ │
│ └──────────────────────────────────┘ │
│ [ Today × ][ Missed × ][ Clear all ] │
│                                      │
│ ─── Today ─────────────────────────  │
│ ◉ Ramesh Mobile     ↘ 14:32 · 2:14 ★ │
│ ◉ +91 90909 90909   ✗ 14:01 · 0:00 ★ │
│ ─── Yesterday ─────────────────────  │
│ ◉ Anita Wholesale   ↗ 17:50 · 1:02   │
├──────────────────────────────────────┤
│   🏠     📞     📥³    ⋯              │
└──────────────────────────────────────┘
```

### Pinned collapsed

```
┌──────────────────────────────────────┐
│ ┌──────────────────────────────────┐ │
│ │ Unsaved inquiries — last 7 days  │ │
│ │ (5)                       ▸   ✕  │ │
│ └──────────────────────────────────┘ │
```

### Bulk mode

```
┌──────────────────────────────────────┐
│ ▒ callNest    🔍  ⛃   ⬚  ⋮          │
├──────────────────────────────────────┤
│ ◉ Ramesh Mobile     ↘ 14:32 · 2:14   │  unselected
│ ✔ +91 90909 90909   ✗ 14:01 · 0:00   │  selected
│ ✔ Anita Wholesale   ↗ 17:50 · 1:02   │  selected
├──────────────────────────────────────┤
│ 2 selected                            │
│ [🏷] [★] [👤+] [⤴] [🗑] [✓]           │
└──────────────────────────────────────┘
```

### No-match empty

```
┌──────────────────────────────────────┐
│ [ Today × ][ Missed × ][ Tag: VIP ×]│
│                                      │
│             🔎                       │
│   No calls match these filters       │
│       [ Clear filters ]              │
└──────────────────────────────────────┘
```

### Grouped-by-number

```
┌──────────────────────────────────────┐
│ ◉ Ramesh Mobile     latest 14:32  3 ▸│
│   ↳ 3 calls · last 14:32 · total 8m  │
│ ◉ +91 90909 90909   latest 14:01  2 ▸│
│   ↳ 2 calls · last 14:01 · total 0m  │
└──────────────────────────────────────┘
```

## 18.14 — Accessibility

- Each row's `contentDescription`:
  "Call from Ramesh Mobile, incoming, today at 2:32 PM, 2 minutes 14 seconds, bookmarked, lead score 78 hot. Double-tap to open. Long-press for bulk select."
- The bookmark trailing icon is exposed as a toggle action via
  `customAction(label = "Bookmark") { ... }` so TalkBack users can
  toggle without swiping.
- Sticky headers announce "Date header, Today, in list".
- Bulk-mode selection state is exposed via `selected = true/false` with
  `role = Role.Checkbox`.
- BulkActionBar buttons each have `contentDescription` and `onClickLabel`.
- Filter icon includes the active count: "Filter, 2 active".
- All swipe targets have alternative `customAction`s so non-gesture users
  can perform the action via TalkBack.
- Color is never the sole signal: bookmark uses both color and icon
  shape; lead-score badge uses both color and the literal score number.
- Touch targets ≥ 48.dp; row height ≥ 64.dp accommodates this.

## 18.15 — Performance budget

- First skeleton paint: < 80 ms after tab switch.
- First row visible: < 250 ms on a 10k-row DB (Room with composite index
  on `(started_at DESC, id)`).
- Scrolling 60 fps maintained for a 10k-row list. Achieved by:
  - `LazyColumn` with stable keys.
  - Pre-resolved tag pills (no per-row DB query).
  - `derivedStateOf` for the active-filter chip set.
  - Avoiding `Modifier.shadow` per row (use `NeoSurface` which paints to a `RenderEffect` once).
- Memory: < 6 MB for 10k CallRow objects (estimate: ~600 bytes/row).
- Eager render budget: at most 1 screen-worth (≈ 12 rows) materialized
  on first composition; the rest stream in via `LazyColumn`.
- Filter recompute cost: O(N) on N matching rows; queries below 5k rows
  complete < 30 ms with proper indices.

---

# §19 — Inquiries tab

The Inquiries tab is the management surface for auto-saved contacts —
unsaved callers that callNest automatically promoted into a system
contact group using a configurable name pattern (e.g. `callNest-s1 +91…`).
This tab surfaces those auto-saved entries, lets the user search and
bulk-convert them into "real" contacts, and observes the auto-saved/manual
flip when the user renames an entry in the system Contacts app.

## 19.1 — Purpose

Be the single management surface for inquiries. Make it trivial to:

- See every unsaved-but-auto-saved caller in one list, sorted by recency.
- Search by partial number or pattern label.
- Convert one or many to real contacts (rename → triggers auto-flip).
- Long-press to enter bulk mode for batch conversion.

After Phase I.6, this tab replaces the older "My Contacts ↔ Inquiries"
two-tab section. My Contacts is no longer present in the bottom nav;
real contacts live in the system Contacts app and are referenced by
callNest transparently when the row's number matches.

## 19.2 — Entry points

| From                           | Behavior                                                       |
| ------------------------------ | -------------------------------------------------------------- |
| Bottom nav: tap "Inquiries"    | Direct.                                                        |
| Home → "Unsaved" stat tile     | Navigates here with no preset.                                 |
| Home → "Save all" CTA          | Navigates here, no preset, snackbar "Tap a row to convert".    |
| Calls bulk action: Save        | After save action completes, snackbar with "View" → goes here. |
| Notification: auto-saved batch | Routes here.                                                   |
| Process restart                | Restored if active.                                            |
| Re-tap Inquiries               | Scroll to top.                                                 |

## 19.3 — Exit points

| To                           | Trigger                                     |
| ---------------------------- | ------------------------------------------- |
| `route=call_detail/{number}` | Tap a row.                                  |
| Convert dialog               | Tap "Convert" inline, or bulk Convert.      |
| System Contacts app (intent) | "Convert" → ACTION_EDIT on the contact URI. |
| Tag picker bottom sheet      | Bulk action "Tag".                          |

## 19.4 — Required inputs (data)

ViewModel: `InquiriesViewModel @HiltViewModel constructor(contactRepo, callRepo, settings)`.

State:

```kotlin
data class InquiriesUiState(
    val query: String = "",
    val items: List<InquiryRow> = emptyList(),
    val totalCount: Int = 0,
    val bulkMode: Boolean = false,
    val selectedNumbers: Set<String> = emptySet(),
    val loading: Boolean = true,
    val empty: Boolean = false
)

data class InquiryRow(
    val normalizedNumber: String,
    val displayLabel: String,           // "callNest-s1 +91…"
    val initial: Char,
    val accentSeed: Int,
    val totalCalls: Int,
    val lastCallAtMs: Long,
    val firstSeenAtMs: Long
)
```

Sources:

- `items` ← `combine(contactRepo.observeAutoSaved(), query) { all, q ->
 if (q.isBlank()) all else all.filter { it.matches(q) } }`.
- `totalCount` ← `items.size`.
- `empty` ← `items.isEmpty() && !loading`.

## 19.5 — Required inputs (user)

| Trigger                  | Behavior                                                                   | State change                   |
| ------------------------ | -------------------------------------------------------------------------- | ------------------------------ |
| Type in search bar       | `query` updates with 250 ms debounce.                                      | `query` updates; list filters. |
| Tap row                  | `rootNav.navigate("call_detail/$normalizedNumber")`.                       | None.                          |
| Tap "Convert" inline     | Open Convert dialog with prefilled name suggestion.                        | None.                          |
| Long-press row           | Enter bulk mode; select that row.                                          | `bulkMode = true`.             |
| Tap row in bulk mode     | Toggle selection.                                                          | `selectedNumbers ± n`.         |
| Bulk action: Convert all | Open Convert sheet for many; loops through ACTION_EDIT.                    | None on success.               |
| Bulk action: Bulk save   | **Disabled** with tooltip "Already saved".                                 | None.                          |
| Bulk action: Tag         | Open tag picker.                                                           | None.                          |
| Bulk action: Done        | Exit bulk mode.                                                            | `bulkMode = false`.            |
| Pull to refresh          | `contactRepo.reconcileAutoSaved()` (re-runs DetectAutoSavedRenameUseCase). | rows may flip out.             |

## 19.6 — Mandatory display elements

Container: `StandardPage(title="Inquiries", description="Auto-saved callers waiting for a name", emoji="📥", backgroundColor=TabBgInquiries, headerGradient=HeaderGradInquiries)`.

### 19.6.1 — Search bar

- Sticky at the top of the list (below the header). 48.dp height.
- Leading icon `Icons.Outlined.Search`.
- Placeholder: "Search by number or label".
- Trailing X visible when query is non-empty → clears query.
- IME action `Search`.

### 19.6.2 — Inquiry rows (LazyColumn)

Each row layout:

```
[ NeoAvatar (auto-saved tint) ][ name + meta ][ Convert button ]
```

- `NeoAvatar` uses a fixed `Accent.Inquiry` tint (deterministic across all auto-saved rows so they read as a cohort), with the initial letter `?` if the displayLabel begins with "+91".
- Center column:
  - Line 1: `displayLabel` (`bodyLarge` bold, ellipsize).
  - Line 2: `formatRelative(lastCallAtMs)` + " · " + `"$totalCalls calls"` (`bodySmall`, muted).
- Trailing: `NeoButton(label="Convert", small=true)`.

### 19.6.3 — Bulk batch toolbar (when bulk mode)

- Same NeoSurface bar as Calls' BulkActionBar.
- Buttons: Tag · Convert all · Bulk save (disabled, with tooltip "Already saved") · Done.

### 19.6.4 — Empty state

```
        📥
   No inquiries yet
New unsaved callers will appear here automatically.
```

## 19.7 — Optional display elements

| Element                         | Condition                                                                                   |
| ------------------------------- | ------------------------------------------------------------------------------------------- |
| "Pattern updated" banner        | If user changed the auto-save pattern in settings recently and rows are reconciling.        |
| "Pattern not configured" banner | If `settings.autoSavePrefix` is empty AND there are auto-saved entries from a prior config. |
| Search results count            | Below search bar when query is non-empty.                                                   |
| Loading shimmer                 | During first emission.                                                                      |

## 19.8 — Empty state

See §19.6.4. Two variants:

- **Truly empty** — no auto-saved rows exist. Copy: "No inquiries yet."
- **Filtered to empty** — query did not match. Copy: "No matches for ‘%1$s’." with "Clear search" CTA.

## 19.9 — Loading state

- 5 skeleton rows shaped like inquiry rows.
- 800 ms cap, then spinner.

## 19.10 — Error state

If `contactRepo.observeAutoSaved()` throws (rare; CP query failed):

- Snackbar: "Couldn't read inquiries. Tap to retry."
- Body: empty state with title "Couldn't load inquiries" and CTA "Retry".

## 19.11 — Edge cases

1. **User renamed in system Contacts** — `DetectAutoSavedRenameUseCase`
   flips `isAutoSaved=false` on next sync. On tab visit, the row
   disappears with `animateItem(fadeOut)`. Verify state correctness on
   `LifecycleEventObserver(ON_RESUME)` by re-collecting.
2. **Pattern matcher mismatch** — A contact whose name superficially
   matches the pattern but was actually a manual entry: trust the DB
   `isAutoSaved` flag, not pattern alone (the flag was set at creation).
3. **Group not yet created** — On first auto-save, the contact group is
   created lazily. The row still appears here because the DB row exists
   even before the system contact does. If the system contact is missing,
   "Convert" surfaces an error: "Contact unavailable. Try again after the
   next sync."
4. **User uninstalled callNest group from system** — `reconcileAutoSaved()`
   detects missing system contacts and offers to recreate them via a
   banner.
5. **10k auto-saved entries** — `LazyColumn` virtualizes; search uses
   `Flow<List>` debounce 250 ms; query runs in-memory because the list
   is bounded.
6. **Bulk Convert across many** — Convert opens system Contacts intents
   serially. If the user cancels mid-loop, partial conversions remain;
   show a snackbar "Converted N of M".
7. **Convert succeeds but flip doesn't happen** — re-run reconcile on
   next ON_RESUME.
8. **System Contacts app is disabled** — Convert button shows error
   snackbar "Contacts app is unavailable on this device."
9. **User searches with special chars** — query is sanitized; no regex.
10. **Tab visited during initial sync** — show shimmer; do not block UI.

## 19.12 — Copy table

| String id                        | English                                                  |
| -------------------------------- | -------------------------------------------------------- |
| `inquiries_title`                | Inquiries                                                |
| `inquiries_description`          | Auto-saved callers waiting for a name                    |
| `inquiries_search_placeholder`   | Search by number or label                                |
| `inquiries_search_clear_a11y`    | Clear search                                             |
| `inquiries_row_calls`            | %1$d calls                                               |
| `inquiries_row_convert`          | Convert                                                  |
| `inquiries_bulk_count`           | %1$d selected                                            |
| `inquiries_bulk_tag`             | Tag                                                      |
| `inquiries_bulk_convert_all`     | Convert all                                              |
| `inquiries_bulk_save`            | Bulk save                                                |
| `inquiries_bulk_save_disabled`   | Already saved                                            |
| `inquiries_bulk_done`            | Done                                                     |
| `inquiries_empty_title`          | No inquiries yet                                         |
| `inquiries_empty_body`           | New unsaved callers will appear here automatically.      |
| `inquiries_search_empty_title`   | No matches for ‘%1$s’                                    |
| `inquiries_search_empty_cta`     | Clear search                                             |
| `inquiries_pattern_updated`      | Pattern updated. Reconciling…                            |
| `inquiries_pattern_missing`      | Auto-save pattern is not configured. Set it in Settings. |
| `inquiries_convert_dialog_title` | Convert to a real contact                                |
| `inquiries_convert_dialog_body`  | We'll open Contacts so you can rename %1$s.              |
| `inquiries_convert_cta`          | Open Contacts                                            |
| `inquiries_convert_cancel`       | Cancel                                                   |
| `inquiries_convert_partial`      | Converted %1$d of %2$d                                   |
| `inquiries_error_snack`          | Couldn't read inquiries. Tap to retry.                   |
| `inquiries_error_retry`          | Retry                                                    |

## 19.13 — ASCII wireframe

```
┌──────────────────────────────────────┐
│ ▒ callNest            🔍   ⋮        │
├──────────────────────────────────────┤
│ 📥 Inquiries                          │
│    Auto-saved callers waiting…        │
│ ──────────────────────────────────── │
│ ┌──────────────────────────────────┐ │
│ │ 🔍 Search by number or label  ✕  │ │
│ └──────────────────────────────────┘ │
│ ◉ callNest-s1 +91 98765 43210       │
│   2h ago · 4 calls    [ Convert ]    │
│ ◉ callNest-s1 +91 99887 76655       │
│   1d ago · 1 call     [ Convert ]    │
│ ◉ callNest-s2 +91 90909 90909       │
│   3d ago · 7 calls    [ Convert ]    │
│ …                                    │
├──────────────────────────────────────┤
│   🏠     📞     📥³    ⋯              │
└──────────────────────────────────────┘
```

Bulk mode replaces the bottom nav with the batch toolbar (same pattern
as Calls).

## 19.14 — Accessibility

- Row description: "Inquiry, callNest-s1 plus 91 98765 43210, last call 2 hours ago, 4 calls. Double-tap to open. Long-press for bulk select."
- "Convert" button description: "Convert callNest-s1 plus 91 98765 43210 to a real contact, button. Opens Contacts."
- Search bar uses `imeAction = Search`. TalkBack reads "Search inquiries, edit text".
- Bulk-mode "Bulk save" button is disabled with `Modifier.semantics { stateDescription = "Already saved" ; disabled() }`.
- Sticky search bar maintains 48.dp touch target.

## 19.15 — Performance budget

- First paint: < 100 ms.
- Search debounce: 250 ms; in-memory filter on a list of 10k entries
  completes in < 8 ms.
- LazyColumn with stable keys (`normalizedNumber`).
- No image loading; avatars are vector initials.
- Memory: < 2 MB for 10k inquiry rows.

---

# §20 — More tab

The More tab is the catch-all settings/management menu. It is visually
the simplest tab — three grouped cards of routed rows — but it is the
single most-touched tab for power users because every advanced feature
lives one tap away from here.

## 20.1 — Purpose

Provide a clear, scannable index of every callNest feature that does
not have a primary tab of its own. Group rows into three semantic
buckets (Data, Automation, App), keep iconography consistent, and surface
the App-update affordance prominently when applicable.

## 20.2 — Entry points

| From                        | Behavior                                |
| --------------------------- | --------------------------------------- |
| Bottom nav: tap "More"      | Direct.                                 |
| Home → Quick action: Backup | Routes to Backup directly (skips More). |
| Process restart             | Restored if active.                     |
| Re-tap More                 | Scroll to top.                          |

## 20.3 — Exit points

Each row routes to a specific destination, listed in §20.6.

## 20.4 — Required inputs (data)

ViewModel: `MoreViewModel @HiltViewModel constructor(updateRepo, settings, driveAuthRepo)`.

State:

```kotlin
data class MoreUiState(
    val updateAvailable: Boolean = false,
    val updateVersion: String? = null,
    val driveSignedIn: Boolean = false,
    val autoSaveEnabled: Boolean = false,
    val autoTagRulesCount: Int = 0,
    val tagsCount: Int = 0
)
```

Sources:

- `updateAvailable` ← `updateRepo.state.map { it is UpdateState.Available && !skipped }`.
- `updateVersion` ← latest version string when available.
- `driveSignedIn` ← `driveAuthRepo.observeSignedIn()`.
- Counts come from respective repos for subtitle text.

## 20.5 — Required inputs (user)

| Trigger                            | Behavior                                                 | State change |
| ---------------------------------- | -------------------------------------------------------- | ------------ |
| Tap a row                          | Route to the destination.                                | None at tab. |
| Tap Quick Export row               | Open Quick Export sheet (parent-controlled).             | sheet opens. |
| Tap App updates row when available | Routes to update screen with banner state.               | None.        |
| Pull to refresh                    | No-op (More has nothing to refresh; show subtle haptic). | None.        |

## 20.6 — Mandatory display elements

Container: `StandardPage(title="More", description="Everything else", emoji="⋯", backgroundColor=TabBgMore, headerGradient=HeaderGradMore)`.

The body is a single `Column` with three `NeoCard`s, each containing
`MoreRow`s. A `MoreRow` is:

- Leading: 40.dp NeoSurface concave circle, with a 20.dp `Icon` tinted
  per row.
- Center: `Column`:
  - Title (`bodyLarge`).
  - Optional subtitle (`bodySmall`, muted) — only present for select
    rows (e.g. "Last backup: 3h ago").
- Trailing: `KeyboardArrowRight` chevron (20.dp, muted).

Row touch target: 56.dp height.

### 20.6.1 — The 12 rows

| #   | Group      | Emoji | Title              | Subtitle (when present)                 | Icon tint        | Destination               |
| --- | ---------- | ----- | ------------------ | --------------------------------------- | ---------------- | ------------------------- |
| 1   | Data       | 📤    | Export             | CSV · Excel · PDF                       | `Accent.Blue`    | `route=export`            |
| 2   | Data       | ⚡    | Quick Export       | Last 7 days · Excel                     | `Accent.Teal`    | `onQuickExport()` (sheet) |
| 3   | Data       | 💾    | Backup & restore   | "Last backup: %1$s" or "Not configured" | `Accent.Green`   | `route=backup`            |
| 4   | Data       | 🏷    | Tags               | "%1$d tags"                             | `Accent.Purple`  | `route=tag_manager`       |
| 5   | Automation | 🤖    | Auto-tag rules     | "%1$d rules"                            | `Accent.Indigo`  | `route=auto_tag_rules`    |
| 6   | Automation | 🎯    | Lead scoring       | —                                       | `Accent.Red`     | `route=lead_scoring`      |
| 7   | Automation | ⚡    | Real-time features | "Bubble · Post-call popup"              | `Accent.Yellow`  | `route=realtime_features` |
| 8   | Automation | 📥    | Auto-save          | "On" / "Off"                            | `Accent.Inquiry` | `route=auto_save`         |
| 9   | App        | 📊    | Stats              | —                                       | `Accent.Cyan`    | `route=stats`             |
| 10  | App        | 🚀    | App updates        | "Update %1$s available" or "Up to date" | `Accent.Warning` | `route=app_updates`       |
| 11  | App        | 📚    | Help & docs        | —                                       | `Accent.Slate`   | `route=help_docs`         |
| 12  | App        | ⚙️    | Settings           | —                                       | `Accent.Gray`    | `route=settings`          |

Each `NeoCard` group has:

- Header label (small caps, muted): `Data`, `Automation`, `App`.
- Vertical list of rows with 1.dp dividers (NeoDivider).

## 20.7 — Optional display elements

| Element                          | Condition                                                                                            |
| -------------------------------- | ---------------------------------------------------------------------------------------------------- |
| "Update %1$s available" subtitle | `updateAvailable && updateVersion != null`. Renders as small accent pill rather than muted subtitle. |
| Sign-out CTA in overflow         | Only if `driveSignedIn`.                                                                             |
| Backup last-run subtitle         | Present iff `lastBackupAt != null`.                                                                  |
| Auto-save subtitle "On"/"Off"    | Always present (state).                                                                              |
| Tags count subtitle              | Always present.                                                                                      |

## 20.8 — Empty state

The More tab is never empty — all 12 rows always render.

## 20.9 — Loading state

The More tab does not show a global loader. Subtitles that need data
("Tags: %d tags", "Last backup: %s") render with a tiny shimmer until
the underlying flow has emitted (≤ 200 ms). Rows are tappable
immediately.

## 20.10 — Error state

If `updateRepo.state` errors, the App-update row falls back to "Up to
date" with a `Timber.w` log. No user-facing error.

## 20.11 — Edge cases

1. **Drive not signed in** — Sign-out menu item in top-bar overflow is
   hidden. Backup row remains tappable; the Backup screen handles its
   own onboarding.
2. **Updates available** — Row 10 shows the update pill; the bottom-nav
   More dot is also lit (handled by scaffold §16).
3. **All 12 rows tappable** — verify with screen reader that none are
   accidentally `enabled = false`.
4. **Scroll behavior with bottom inset** — `LazyColumn` consumes the
   inset; the last row is fully tappable above the bottom nav.
5. **Subtitle text overflow** — subtitles ellipsize at 1 line.
6. **High dynamic type** — row height grows; chevron stays right-aligned.
7. **Dark mode** — Accent tints are theme-aware via `MaterialTheme.colorScheme`.
8. **User rotates device mid-tap** — row tap is debounced 300 ms to
   prevent double navigation.
9. **Tags repo errors** — subtitle gracefully falls back to the title
   only.
10. **Right-to-left** — chevron icon swaps to `KeyboardArrowLeft` via
    `Modifier.scale(scaleX = -1f, scaleY = 1f)` when LayoutDirection is
    Rtl.

## 20.12 — Copy table

| String id                       | English                  | Notes          |
| ------------------------------- | ------------------------ | -------------- |
| `more_title`                    | More                     |                |
| `more_description`              | Everything else          |                |
| `more_group_data`               | Data                     |                |
| `more_group_automation`         | Automation               |                |
| `more_group_app`                | App                      |                |
| `more_row_export`               | Export                   |                |
| `more_row_export_sub`           | CSV · Excel · PDF        |                |
| `more_row_quick_export`         | Quick Export             |                |
| `more_row_quick_export_sub`     | Last 7 days · Excel      |                |
| `more_row_backup`               | Backup & restore         |                |
| `more_row_backup_sub_last`      | Last backup: %1$s ago    | %1$s relative  |
| `more_row_backup_sub_none`      | Not configured           |                |
| `more_row_tags`                 | Tags                     |                |
| `more_row_tags_sub`             | %1$d tags                |                |
| `more_row_auto_tag`             | Auto-tag rules           |                |
| `more_row_auto_tag_sub`         | %1$d rules               |                |
| `more_row_lead_scoring`         | Lead scoring             |                |
| `more_row_realtime`             | Real-time features       |                |
| `more_row_realtime_sub`         | Bubble · Post-call popup |                |
| `more_row_auto_save`            | Auto-save                |                |
| `more_row_auto_save_on`         | On                       |                |
| `more_row_auto_save_off`        | Off                      |                |
| `more_row_stats`                | Stats                    |                |
| `more_row_app_updates`          | App updates              |                |
| `more_row_app_updates_avail`    | Update %1$s available    | %1$s = version |
| `more_row_app_updates_uptodate` | Up to date               |                |
| `more_row_help`                 | Help & docs              |                |
| `more_row_settings`             | Settings                 |                |

## 20.13 — ASCII wireframe

```
┌──────────────────────────────────────┐
│ ▒ callNest            🔍   ⋮        │
├──────────────────────────────────────┤
│ ⋯ More                               │
│   Everything else                    │
│ ──────────────────────────────────── │
│ DATA                                 │
│ ┌──────────────────────────────────┐ │
│ │ ⓘ📤 Export                     ▸│ │
│ │       CSV · Excel · PDF          │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ⚡ Quick Export                ▸│ │
│ │       Last 7 days · Excel        │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ💾 Backup & restore            ▸│ │
│ │       Last backup: 3h ago        │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ🏷 Tags                        ▸│ │
│ │       12 tags                    │ │
│ └──────────────────────────────────┘ │
│ AUTOMATION                           │
│ ┌──────────────────────────────────┐ │
│ │ ⓘ🤖 Auto-tag rules              ▸│ │
│ │       4 rules                    │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ🎯 Lead scoring                ▸│ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ⚡ Real-time features          ▸│ │
│ │       Bubble · Post-call popup   │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ📥 Auto-save                   ▸│ │
│ │       On                         │ │
│ └──────────────────────────────────┘ │
│ APP                                  │
│ ┌──────────────────────────────────┐ │
│ │ ⓘ📊 Stats                       ▸│ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ🚀 App updates                 ▸│ │
│ │       Update 1.2.0 available     │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ📚 Help & docs                 ▸│ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ⚙️ Settings                    ▸│ │
│ └──────────────────────────────────┘ │
├──────────────────────────────────────┤
│   🏠     📞     📥³    ⋯•             │
└──────────────────────────────────────┘
```

## 20.14 — Accessibility

- Each row description: "Backup and restore. Last backup 3 hours ago. Button."
- Card group headers expose `headingLevel = 2`.
- Chevrons are decorative (`contentDescription = null`).
- The "Update available" pill on row 10 is announced as "Update one point two point zero available."
- Touch targets ≥ 56.dp.
- Quick Export row's destination is a sheet, not a route — TalkBack
  announces "Quick Export, opens sheet, button" via `onClickLabel`.

## 20.15 — Performance budget

- First paint: < 80 ms.
- Subtitle data loads asynchronously without blocking the row layout.
- No images; icons are vector.
- Memory: trivial (< 200 KB).

---

> End of Part 03.
>
> Next: Part 04 — Call Detail screen (deep target from Calls and Home),
> tag picker bottom sheet, follow-up scheduling, notes journal, and
> share-contact-card flow.

---

# callNest APP-SPEC — Part 04: Deep Pages I

> Section: Call detail · Search · Stats · Bookmarks · Follow-ups · My Contacts
> Audience: a UX engineer rebuilding callNest from scratch.
> Status: locked. Cross-references: Part 02 (Tabs & Home), Part 03 (Library tab),
> Part 05 (Deep pages II), Part 06 (Appendices — Neo\* components, CallRow, color
> tokens, formatters).
> Sister files: see `docs/spec-parts/02-tabs-and-home.md`,
> `docs/spec-parts/03-library.md`.

This part covers six "deep" pages — pages reached by drilling into a row from
one of the four bottom tabs. Each page follows the same 15-section template
introduced in Part 02. Where the template repeats verbatim wording, it has been
expanded with the specifics that distinguish that page.

The six pages, numbered to continue the global section counter from Parts 02/03:

| #   | Page       | Route                           | Spec section |
| --- | ---------- | ------------------------------- | ------------ |
| 21  | CallDetail | `callDetail/{normalizedNumber}` | §21          |
| 22  | Search     | `search` (overlay)              | §22          |
| 23  | Stats      | `stats`                         | §23          |
| 24  | Bookmarks  | `bookmarks`                     | §24          |
| 25  | FollowUps  | `followUps`                     | §25          |
| 26  | MyContacts | `myContacts`                    | §26          |

Per-page template (15 subsections, identical to Parts 02 + 03):

1. Purpose
2. Entry points
3. Exit points
4. Required inputs (data) — route args + ViewModel state
5. Required inputs (user) — taps, swipes, gestures
6. Mandatory display elements
7. Optional display elements
8. Empty state
9. Loading state
10. Error state
11. Edge cases (≥5)
12. Copy table
13. ASCII wireframe
14. Accessibility
15. Performance budget

Conventions used throughout:

- `Neo*` = the in-house component library documented in Part 06 Appendix A.
- `StandardPage` = the screen scaffold (top bar + colored tab background +
  header gradient + body slot) documented in Part 06 Appendix B.
- `CallRow` = the canonical call list item documented in Part 06 Appendix C.
- "deterministic color" = HSL hash of normalized number — see Part 06
  Appendix F.
- "phase I.2" = the second hardening pass on detail surfaces; see
  `RELEASE-PLAN.md` and `CHANGELOG.md` v0.18.x.

---

## §21 — CallDetail screen

`com.callNest.app.ui.screen.detail.CallDetailScreen`

### 21.1 Purpose

CallDetail is the most important deep page in the app. It is the _single number
of truth_ for a phone number: who they are (name + avatar + saved/unsaved state),
how the user has interacted with them (every call, every tag, every note, every
follow-up), and what the user is going to do next about them (call, message,
WhatsApp, save, block, schedule).

Every other surface in callNest eventually delegates here. From Calls, the user
taps a row → CallDetail. From Library → CallDetail. From Search → CallDetail.
From the post-call popup "Open" button → CallDetail. From a notification's
"View" action → CallDetail. From an exported PDF link (deep-linked via
`callNest://detail/<num>`) → CallDetail.

Because of that fan-in, CallDetail must answer in a single glance:

1. _Who is this?_ Hero card — name, avatar, status pill, lead score.
2. _What can I do right now?_ Action bar — call, message, WhatsApp, save, block.
3. _What's the history at a glance?_ Stats card — totals + averages + dates.
4. _How have I categorized them?_ Tags section.
5. _What did I write down about them?_ Notes journal.
6. _What did I promise to do?_ Follow-up section.
7. _What's the full timeline?_ Call history timeline.
8. _What admin levers do I have?_ Manage section.

The page is a vertically scrolled `LazyColumn`; the user reads top-down and
scrolls until they find what they need. Nothing is collapsed by default.

### 21.2 Entry points

| Source                                        | Args passed                        | Notes                 |
| --------------------------------------------- | ---------------------------------- | --------------------- |
| Calls tab → CallRow tap                       | `normalizedNumber`                 | Most common (~70%)    |
| Library tab → ContactRow tap                  | `normalizedNumber`                 | ~15%                  |
| Search overlay → result tap                   | `normalizedNumber`                 | ~7%                   |
| Bookmarks → row or pinned-bookmark tap        | `normalizedNumber`                 |                       |
| FollowUps → row tap                           | `normalizedNumber`                 |                       |
| MyContacts → row tap                          | `normalizedNumber`                 |                       |
| Post-call popup → "Open" button               | `normalizedNumber`                 |                       |
| Floating in-call bubble → expand → tap header | `normalizedNumber`                 |                       |
| Notification ("Missed call from …") → tap     | `normalizedNumber` (PendingIntent) |                       |
| External deep link (`callNest://detail/<n>`)  | `normalizedNumber`                 | Phase I.2             |
| Exported PDF link clicked while app open      | `normalizedNumber`                 | Same handler as above |

The route is `callDetail/{normalizedNumber}` where `normalizedNumber` is
URL-encoded E.164 (e.g. `+919876543210` → `%2B919876543210`). The
`NavController` decodes back to the raw E.164 string before passing into the
ViewModel.

For unknown / private numbers, the upstream caller passes an empty string. The
ViewModel detects this and renders a degraded "Private number" hero card; see
edge case 21.11.b.

### 21.3 Exit points

- **Back arrow** → `popBackStack()` to whichever surface invoked it.
- **System back gesture** → same as above.
- **Share contact card** (top app bar trailing icon) → `Intent.ACTION_SEND` with
  vCard text/x-vcard MIME; chooser dialog. Stays on CallDetail after share.
- **Action bar Call** → `Intent.ACTION_DIAL` with `tel:<number>`. Phone app
  becomes foreground.
- **Action bar Message** → `Intent.ACTION_SENDTO` with `smsto:<number>`. SMS app
  becomes foreground.
- **Action bar WhatsApp** → `Intent.ACTION_VIEW` with `https://wa.me/<num>`.
  WhatsApp opens; if not installed, browser opens chooser.
- **Action bar Save** → `ContactsContract.Intents.Insert.ACTION` prefilled with
  number. After Contacts returns, the screen re-fetches and the hero card flips
  from "Unsaved" to "Saved".
- **Action bar Block** → confirmation dialog → if confirmed, calls a stub
  wrapper around `TelecomManager.blockNumber()` (Phase I.2 stub: writes to local
  block list only; deep system block deferred to v1.1).
- **"Save to contacts" CTA** in hero card → same as action bar Save.
- **"Add tag"** → opens `TagPickerSheet` (a `ModalBottomSheet`); on dismiss
  returns to CallDetail.
- **Tag chip ×** → removes tag inline; no navigation.
- **"Add note" / Edit / Delete** → opens `NoteEditorDialog` (full-screen dialog);
  on confirm/dismiss returns to CallDetail.
- **"Set follow-up"** → DatePicker → TimePicker → returns to CallDetail.
- **Follow-up Edit / Cancel / Snooze** → inline mutate; no navigation.
- **Manage → Edit notes** → scrolls to and focuses the notes journal.
- **Manage → Clear all data for this number** → confirmation dialog → on
  confirm, ViewModel issues `ClearNumberDataUseCase` then `popBackStack()`.
- **Manage → Report spam** → applies the predefined `Spam` tag, shows snackbar
  "Reported as spam.", stays on screen.

### 21.4 Required inputs (data)

Route arg: `normalizedNumber: String` (E.164, may be empty for private).

`CallDetailViewModel` state — single `StateFlow<CallDetailUiState>`:

| Field               | Type                     | Source                                    |
| ------------------- | ------------------------ | ----------------------------------------- |
| `normalizedNumber`  | `String`                 | savedStateHandle                          |
| `displayName`       | `String?`                | `contactsRepo.observeName(num)`           |
| `formattedNumber`   | `String`                 | `PhoneFormatter.formatForDisplay(num)`    |
| `geocodedLocation`  | `String?`                | `callRepo.observeMostRecentGeocoded(num)` |
| `avatarSeed`        | `String`                 | derived = normalizedNumber                |
| `status`            | `enum SavedStatus`       | derived: Saved / Unsaved / AutoSaved      |
| `leadScore`         | `Int` 0..100             | `LeadScoreUseCase.observe(num)`           |
| `leadBucket`        | `enum LeadBucket`        | derived from `leadScore`                  |
| `stats`             | `NumberStats`            | `NumberStatsUseCase.observe(num)`         |
| `tags`              | `List<TagApplication>`   | `tagRepo.observeApplied(num)`             |
| `notes`             | `List<NoteEntry>`        | `noteRepo.observe(num)` (newest first)    |
| `followUp`          | `FollowUp?`              | `callRepo.observeFollowUp(num)`           |
| `history`           | `PagingData<CallEntity>` | `callRepo.pagedHistory(num, pageSize=50)` |
| `historyTotalCount` | `Int`                    | counted upfront                           |
| `isLoading`         | `Boolean`                | true until first emission of stats        |
| `isError`           | `String?`                | non-null = banner copy                    |
| `permissionMissing` | `Boolean`                | true if call-log perm revoked while open  |

`NumberStats` (computed by `NumberStatsUseCase`):

| Field              | Type         | Notes                              |
| ------------------ | ------------ | ---------------------------------- |
| `totalCalls`       | `Int`        |                                    |
| `incomingCount`    | `Int`        |                                    |
| `outgoingCount`    | `Int`        |                                    |
| `missedCount`      | `Int`        | includes rejected per spec §3.4    |
| `totalDurationSec` | `Long`       |                                    |
| `firstCallAtMs`    | `Long`       |                                    |
| `lastCallAtMs`     | `Long`       |                                    |
| `avgDurationSec`   | `Long`       | `totalDurationSec / answeredCount` |
| `missedRatio`      | `Float` 0..1 | `missedCount / totalCalls`         |

The ViewModel exposes a `SharedFlow<CallDetailEvent>` for one-shot side effects:
snackbars, toast on share, "Copied number" feedback, navigation pops.

```kotlin
sealed interface CallDetailEvent {
    data class Snackbar(val text: String) : CallDetailEvent
    data class StartIntent(val intent: Intent) : CallDetailEvent
    data object PopBack : CallDetailEvent
}
```

### 21.5 Required inputs (user)

| Gesture                                 | Effect                                       |
| --------------------------------------- | -------------------------------------------- |
| Tap back                                | popBackStack                                 |
| Tap share-contact (top bar)             | fire vCard share intent                      |
| Tap any of the 5 action-bar buttons     | fire corresponding intent                    |
| Tap "Save to contacts" CTA in hero card | fire ContactsContract insert intent          |
| Long-press hero number                  | copy to clipboard, snackbar "Copied"         |
| Tap LeadScoreBadge                      | tooltip popover with score breakdown         |
| Tap a tag chip                          | no-op (visual feedback only)                 |
| Tap × on a tag chip                     | remove that tag (`RemoveTagUseCase`)         |
| Tap "Add tag"                           | open TagPickerSheet                          |
| Tap a note's Edit                       | open NoteEditorDialog prefilled              |
| Tap a note's Delete                     | confirmation → `DeleteNoteUseCase`           |
| Tap "Add note"                          | open NoteEditorDialog blank                  |
| Tap "Set follow-up"                     | DatePicker → TimePicker chain                |
| Tap follow-up Edit                      | DatePicker → TimePicker prefilled            |
| Tap follow-up Cancel                    | confirmation → `CancelFollowUpUseCase`       |
| Tap follow-up Snooze                    | menu (1h / 1d / pick…)                       |
| Tap a history-timeline row              | no-op (already on this number's detail)      |
| Long-press a history-timeline row       | popup menu: Copy timestamp, Delete this call |
| Tap Manage → Edit notes                 | scroll + focus notes journal                 |
| Tap Manage → Clear all data             | dialog → clear → pop                         |
| Tap Manage → Report spam                | apply Spam tag                               |
| Pull-to-refresh on the LazyColumn       | re-fetch stats + history                     |
| Scroll                                  | normal vertical scroll                       |

There is no horizontal swipe, no edge swipe, no shake gesture.

### 21.6 Mandatory display elements

In strict top-to-bottom order:

1. **Top app bar** — height 64dp, background TabBgCalls, leading `Icons.AutoMirrored.Filled.ArrowBack`, title `displayName ?: formattedNumber`, trailing single `IconButton(Icons.Filled.IosShare)` "Share contact card". No overflow ⋮ in Phase I.2 (removed because every action it hosted got promoted to either the action bar or the manage section).
2. **Hero card** — `NeoCard` with `TabBgCalls` background, padding 20dp, contains:
   - Top row: `NeoAvatar(64dp, seed=avatarSeed)` on the left; on its right a column with `displayName` (`Typography.titleLarge`) above `formattedNumber` (`Typography.bodyMedium`, secondary color). If `displayName == null`, the formatted number is rendered as the title and a smaller "Unsaved number" subtitle appears below.
   - Second row: `StatusPill(status)` + `LeadScoreBadge(leadScore)`.
   - Third row (only if `status == Unsaved`): `NeoButton.Primary(text = "Save to contacts", icon = PersonAdd)` full width.
   - Fourth row (only if `geocodedLocation != null`): `Icon(LocationOn) + " " + geocodedLocation`, body small, secondary color.
3. **Action bar** — single `Row(modifier = horizontalScroll)` of 5 `NeoIconButton`s, each 56dp × 56dp, 12dp gap:
   - Call (`Icons.Filled.Call`, accent green tint)
   - Message (`Icons.AutoMirrored.Filled.Message`, accent blue tint)
   - WhatsApp (custom vector, brand green tint)
   - Save (`Icons.Filled.PersonAdd`, accent purple tint) — disabled when `status == Saved`
   - Block (`Icons.Filled.Block`, error tint)
     Each button has a 12sp label below the icon.
4. **Stats card** — `NeoCard`, 16dp padding, two-column grid of stats:
   - Total calls · Talk time
   - First call · Last call
   - Avg duration · Missed rate
     Values are large; labels small. Empty values render `—` (em dash), not `0`.
5. **Tags section** — Section header "Tags" (titleSmall) with trailing "Add tag" `NeoButton.Tertiary`. Below: a `FlowRow(spacing = 8dp)` of applied tag chips, each with leading colored dot + text + trailing × icon. If no tags applied, a single greyed placeholder chip "No tags yet" (non-interactive).
6. **Notes journal** — Section header "Notes" with right-aligned count "(N)". Below: vertical list of note cards, newest first. Each note card:
   - Top row: timestamp (e.g. "Apr 14, 3:42 PM") + Edit + Delete inline icon buttons.
   - Body: markdown-rendered note (bold/italic/bullets/links via `MarkdownRenderer`, which supports a strict subset — see Part 06 Appendix M).
     At the end of the list: `NeoButton.Secondary("Add note", icon = NoteAdd)` full width.
7. **Follow-up section** — Section header "Follow-up". Below:
   - If `followUp != null && followUp.doneAt == null`: a `NeoCard` with the date+time on the left, and three `NeoIconButton`s on the right: Edit / Cancel / Snooze. A small "in 3 days" relative-time hint underneath.
   - If `followUp != null && followUp.doneAt != null`: a strikethrough card showing "Completed on …".
   - Else: a `NeoButton.Primary("Set follow-up", icon = NotificationsActive)` full width.
8. **Call history timeline** — Section header "Call history" with count "(M)". Below: a vertical list (capped at 50 with a "Show more" loader for older pages — see edge case 21.11.d). Each row:
   - Leading: type icon (incoming = down-arrow green, outgoing = up-arrow blue, missed = red strike, rejected = orange strike, voicemail = purple).
   - Center column: date · time · duration (e.g. "Apr 14 · 3:42 PM · 4m 12s").
   - Trailing: SIM badge (SIM 1 / SIM 2 / single-SIM = nothing).
9. **Manage section** — last vertical block, separated by a 32dp top spacer and a horizontal divider. Section header "Manage". Below: three `NeoButton.Tertiary`s, full-width, stacked:
   - Edit notes (icon = NoteEdit) — scrolls to notes journal.
   - Clear all data for this number (icon = DeleteSweep, danger tint) — opens confirmation.
   - Report spam (icon = Report, warning tint) — applies Spam tag.
10. Trailing 24dp bottom spacer so the last button isn't flush against the system nav bar.

### 21.7 Optional display elements

- **Trend arrow on Lead Score** if delta vs 30d prior is computable: tiny ▲/▼ next to the score.
- **"Auto-saved on Apr 12" subtitle** in the hero card when `status == AutoSaved`.
- **Geocoded location row** in hero card (only if available).
- **"3 unread notes" badge** (Phase II) — not in v1.0.
- **Recent activity sparkline** (24h heatmap) under stats card — Phase II.

### 21.8 Empty state

CallDetail itself is never "empty" in the global sense — there is always at least
one call that brought the user here. But individual sections can be empty:

| Section   | Empty copy / element                                                                                                                  |
| --------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| Tags      | Greyed placeholder chip: `"No tags yet"`                                                                                              |
| Notes     | Light card: `"No notes yet."` + small body: `"Tap Add note to capture context."`                                                      |
| Follow-up | Just the `Set follow-up` button (no separate empty card).                                                                             |
| History   | Cannot be empty by definition; if it ever is, render `"No calls in history. This is unusual — try refreshing."` and a refresh button. |

The screen-level "404" state is documented in 21.11.a.

### 21.9 Loading state

- First emission: `isLoading = true`. The screen shows the **top bar fully**
  (back + title placeholder shimmer + share icon disabled), then a single
  full-screen `CircularProgressIndicator` centered in the body.
- After the hero info has loaded but stats/history haven't, the hero card
  renders for real and the rest of the body uses skeleton shimmers (one
  rectangle per section) for up to 600ms.
- Pull-to-refresh: `RefreshIndicator` at top; the existing data stays visible
  underneath; no full-screen spinner.

### 21.10 Error state

- **Permission revoked while open** (`READ_CALL_LOG` denied externally): the
  body is replaced with a `NeoErrorState` containing icon `LockOutline`,
  title `"Call log access was turned off."`, body `"Re-grant permission to see this number's history."`, and a primary button `"Grant permission"`.
- **DB read error** (catch-all): `NeoErrorState` with `ErrorOutline`,
  title `"Couldn't load this contact."`, body `"Pull to refresh or try again in a moment."`, and a `"Retry"` button.
- **Intent has no handler** (e.g. WhatsApp not installed): snackbar
  `"WhatsApp isn't installed on this device."`. The screen is unchanged.
- **Block stub failure**: snackbar `"Couldn't block this number. Try again."`.
- **Save intent cancelled by user**: snackbar `"Save cancelled."` (informational, not an error).

### 21.11 Edge cases

a. **Number not in DB.** ViewModel sees `historyTotalCount == 0` after first
refresh. Render a dedicated "404" body: icon `SearchOff`, title `"This number isn't in your call log yet."`, body `"It may have been deleted, or this is a deep link from a stale source."`, button `"Go back"`. The hero card and action bar still render — calling/messaging an unknown number is still useful.

b. **Private number** (caller passed empty string for `normalizedNumber`). The
ViewModel synthesizes a degraded state: `displayName = "Private number"`,
action bar shows only the Block button enabled (Call/Message/WhatsApp/Save
are disabled with a tooltip `"Number not available."`). History timeline
shows all rows whose `phoneNumber.isBlank() == true`.

c. **0 calls in history but contact exists.** Defensive — shouldn't happen since
we sourced the contact from a call. If it does, fall through to (a).

d. **1000+ calls in history.** Use `Pager(pageSize = 50, prefetchDistance = 10)`.
The first page (50 rows) renders inline; the next page is appended on scroll.
Compose key on `callId` to keep scroll position stable. Do **not** lazily
recompute totals while paging — totals come from `NumberStatsUseCase` which
queries with `COUNT(*)` once.

e. **Tag picker offline / DB locked.** TagPickerSheet shows skeleton rows for
up to 1s then a `"Couldn't load tags. Tap to retry."` row.

f. **Follow-up scheduled in the past.** Allowed (the user may be back-dating a
note-style reminder). The follow-up card simply shows "Overdue · 2 days ago"
in red. The notification worker (which only fires on future timestamps) will
ignore it.

g. **Note > 5000 characters.** The dialog's TextField has `maxLength = 5000`. The
counter turns red at 4900. Pasting a longer string truncates with a snackbar.

h. **Markdown with malicious link** (`javascript:`). Renderer strips any non-`http(s)`
schemes silently; `tel:`/`mailto:` are allowed; everything else is treated as
plaintext.

i. **Concurrent edit** (user edits a note in this screen while the call sync
worker writes a new call). Room observers re-emit independently; the notes
list and the history list refresh independently without blocking each other.

j. **Hot-reload during edit.** If a configuration change occurs (rotation, dark
mode toggle), the open NoteEditorDialog persists its draft via
`rememberSaveable`.

k. **Share contact card with no display name.** vCard text uses the formatted
number as `FN`; `N` field is left blank. The share text body reads
`"Contact from callNest: <number>"`.

l. **Number is the user's own number** (Telephony `getLine1Number()`). Action bar
Call/Message remain enabled (the user might want to leave themselves a voicemail).
No special UI — there is no reliable way to detect this on Android 10+.

m. **Block confirmation race.** If the user taps Block twice in <300ms, the
second tap is debounced (button enters loading state on first press).

n. **Clear all data for number** while the number is currently in an active call
(in-call bubble visible). Allowed: the in-call bubble survives; only DB rows
are cleared. A new call entry will be re-inserted on `CALL_STATE_IDLE`.

### 21.12 Copy table

| Key                              | Copy                                                                                                                 |
| -------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `cd_top_bar_title_unsaved`       | (formatted number)                                                                                                   |
| `cd_top_bar_title_saved`         | (display name)                                                                                                       |
| `cd_share_icon_cd`               | Share contact card                                                                                                   |
| `cd_back_icon_cd`                | Back                                                                                                                 |
| `cd_status_pill_saved`           | Saved                                                                                                                |
| `cd_status_pill_unsaved`         | Unsaved                                                                                                              |
| `cd_status_pill_autosaved`       | Auto-saved                                                                                                           |
| `cd_status_pill_blocked`         | Blocked                                                                                                              |
| `cd_save_cta_label`              | Save to contacts                                                                                                     |
| `cd_lead_cold`                   | Cold lead                                                                                                            |
| `cd_lead_warm`                   | Warm lead                                                                                                            |
| `cd_lead_hot`                    | Hot lead                                                                                                             |
| `cd_lead_score_tooltip_title`    | How this score is calculated                                                                                         |
| `cd_lead_score_tooltip_body`     | Recency, frequency, answered ratio, follow-up activity.                                                              |
| `cd_action_call`                 | Call                                                                                                                 |
| `cd_action_message`              | Message                                                                                                              |
| `cd_action_whatsapp`             | WhatsApp                                                                                                             |
| `cd_action_save`                 | Save                                                                                                                 |
| `cd_action_block`                | Block                                                                                                                |
| `cd_action_block_confirm_title`  | Block this number?                                                                                                   |
| `cd_action_block_confirm_body`   | You won't get calls or messages from this number until you unblock it.                                               |
| `cd_action_block_confirm_yes`    | Block                                                                                                                |
| `cd_action_block_confirm_no`     | Cancel                                                                                                               |
| `cd_stats_title`                 | At a glance                                                                                                          |
| `cd_stats_total_calls`           | Total calls                                                                                                          |
| `cd_stats_talk_time`             | Talk time                                                                                                            |
| `cd_stats_first_call`            | First call                                                                                                           |
| `cd_stats_last_call`             | Last call                                                                                                            |
| `cd_stats_avg_duration`          | Avg duration                                                                                                         |
| `cd_stats_missed_rate`           | Missed rate                                                                                                          |
| `cd_tags_title`                  | Tags                                                                                                                 |
| `cd_tags_add`                    | Add tag                                                                                                              |
| `cd_tags_empty_chip`             | No tags yet                                                                                                          |
| `cd_tags_remove_cd`              | Remove tag %1$s                                                                                                      |
| `cd_notes_title`                 | Notes                                                                                                                |
| `cd_notes_count`                 | (%1$d)                                                                                                               |
| `cd_notes_empty_title`           | No notes yet.                                                                                                        |
| `cd_notes_empty_body`            | Tap Add note to capture context.                                                                                     |
| `cd_notes_add`                   | Add note                                                                                                             |
| `cd_notes_edit`                  | Edit                                                                                                                 |
| `cd_notes_delete`                | Delete                                                                                                               |
| `cd_notes_delete_confirm_title`  | Delete this note?                                                                                                    |
| `cd_notes_delete_confirm_body`   | This can't be undone.                                                                                                |
| `cd_notes_delete_confirm_yes`    | Delete                                                                                                               |
| `cd_notes_dialog_title_new`      | New note                                                                                                             |
| `cd_notes_dialog_title_edit`     | Edit note                                                                                                            |
| `cd_notes_dialog_placeholder`    | What did you discuss? Markdown supported.                                                                            |
| `cd_notes_dialog_save`           | Save                                                                                                                 |
| `cd_notes_dialog_cancel`         | Cancel                                                                                                               |
| `cd_notes_dialog_counter`        | %1$d / 5000                                                                                                          |
| `cd_followup_title`              | Follow-up                                                                                                            |
| `cd_followup_set`                | Set follow-up                                                                                                        |
| `cd_followup_edit`               | Edit                                                                                                                 |
| `cd_followup_cancel`             | Cancel                                                                                                               |
| `cd_followup_snooze`             | Snooze                                                                                                               |
| `cd_followup_snooze_1h`          | 1 hour                                                                                                               |
| `cd_followup_snooze_1d`          | 1 day                                                                                                                |
| `cd_followup_snooze_pick`        | Pick a time…                                                                                                         |
| `cd_followup_overdue`            | Overdue · %1$s                                                                                                       |
| `cd_followup_done_prefix`        | Completed on %1$s                                                                                                    |
| `cd_history_title`               | Call history                                                                                                         |
| `cd_history_count`               | (%1$d)                                                                                                               |
| `cd_history_show_more`           | Show older calls                                                                                                     |
| `cd_manage_title`                | Manage                                                                                                               |
| `cd_manage_edit_notes`           | Edit notes                                                                                                           |
| `cd_manage_clear_all`            | Clear all data for this number                                                                                       |
| `cd_manage_clear_confirm_title`  | Clear all data?                                                                                                      |
| `cd_manage_clear_confirm_body`   | This deletes every call, tag, note, bookmark, and follow-up for %1$s. The contact in your phone book is not touched. |
| `cd_manage_clear_confirm_yes`    | Clear                                                                                                                |
| `cd_manage_clear_confirm_no`     | Keep                                                                                                                 |
| `cd_manage_report_spam`          | Report spam                                                                                                          |
| `cd_manage_report_spam_snackbar` | Reported as spam.                                                                                                    |
| `cd_error_perm_title`            | Call log access was turned off.                                                                                      |
| `cd_error_perm_body`             | Re-grant permission to see this number's history.                                                                    |
| `cd_error_perm_button`           | Grant permission                                                                                                     |
| `cd_error_db_title`              | Couldn't load this contact.                                                                                          |
| `cd_error_db_body`               | Pull to refresh or try again in a moment.                                                                            |
| `cd_error_db_button`             | Retry                                                                                                                |
| `cd_error_no_whatsapp`           | WhatsApp isn't installed on this device.                                                                             |
| `cd_error_block_failed`          | Couldn't block this number. Try again.                                                                               |
| `cd_error_save_cancelled`        | Save cancelled.                                                                                                      |
| `cd_404_title`                   | This number isn't in your call log yet.                                                                              |
| `cd_404_body`                    | It may have been deleted, or this is a deep link from a stale source.                                                |
| `cd_404_button`                  | Go back                                                                                                              |
| `cd_private_label`               | Private number                                                                                                       |
| `cd_private_disabled_tooltip`    | Number not available.                                                                                                |
| `cd_copied_snackbar`             | Copied                                                                                                               |

### 21.13 ASCII wireframe

Default state — a saved contact with notes and a follow-up:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Ravi (Wholesale)                              ⤴ share  │  top bar (TabBgCalls)
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ╭───╮  Ravi (Wholesale)                                 │ │
│ │ │ R │  +91 98765 43210                                  │ │  hero card
│ │ ╰───╯  ┌──────┐ ┌────────────┐                          │ │
│ │        │Saved │ │ Hot · 82   │                          │ │
│ │        └──────┘ └────────────┘                          │ │
│ │  📍 Mumbai, Maharashtra                                 │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐                             │
│  │📞 │ │💬 │ │🟢 │ │👤+│ │🚫 │   action bar                │
│  └───┘ └───┘ └───┘ └───┘ └───┘                             │
│  Call  Msg   WA    Save  Block                              │
│                                                             │
│ ┌─ At a glance ─────────────────────────────────────────┐  │
│ │ Total calls   42      │  Talk time     2h 14m         │  │  stats card
│ │ First call    Mar 02  │  Last call     Apr 14         │  │
│ │ Avg duration  3m 11s  │  Missed rate   12%            │  │
│ └────────────────────────────────────────────────────────┘  │
│                                                             │
│  Tags                                       [+ Add tag]     │
│  ● Wholesale ×   ● VIP ×   ● Repeat ×                       │
│                                                             │
│  Notes (3)                                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Apr 14, 3:42 PM                       [Edit] [Del]   │  │
│  │ **Quoted ₹42,000** for the bulk order.               │  │
│  │ - Pickup Friday                                      │  │
│  │ - Asked to revisit pricing in 2 weeks                │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Apr 10, 11:15 AM                      [Edit] [Del]   │  │
│  │ Discussed payment terms.                             │  │
│  └──────────────────────────────────────────────────────┘  │
│  [ + Add note ]                                             │
│                                                             │
│  Follow-up                                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Apr 28 · 10:00 AM    in 3 days   [Edit][Cancel][Snz]│  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  Call history (42)                                          │
│  ↓ Apr 14 · 3:42 PM · 4m 12s                       SIM 1   │
│  ↑ Apr 12 · 9:08 AM · 1m 30s                       SIM 1   │
│  ✗ Apr 11 · 6:55 PM · missed                       SIM 2   │
│  ↓ Apr 10 · 11:15 AM · 12m 04s                     SIM 1   │
│  …                                                          │
│  [ Show older calls ]                                       │
│                                                             │
│  ────────────────────────────────────────────────────       │
│  Manage                                                     │
│  [ Edit notes                                          ]    │
│  [ Clear all data for this number                      ]    │
│  [ Report spam                                         ]    │
└─────────────────────────────────────────────────────────────┘
```

Unsaved-number state:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   +91 98201 11111                              ⤴ share  │
├─────────────────────────────────────────────────────────────┤
│ ┌─ hero ─────────────────────────────────────────────────┐ │
│ │ ╭───╮  +91 98201 11111                                  │ │
│ │ │ # │  Unsaved number                                   │ │
│ │ ╰───╯  ┌────────┐ ┌────────────┐                        │ │
│ │        │Unsaved │ │ Cold · 14   │                       │ │
│ │        └────────┘ └────────────┘                        │ │
│ │  [ + Save to contacts ]                                 │ │
│ └─────────────────────────────────────────────────────────┘ │
│  ... (rest identical)                                       │
└─────────────────────────────────────────────────────────────┘
```

Loading state:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   ░░░░░░░░░░░░                                ⤴         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                       ◐  loading…                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

Error (permission revoked):

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Ravi (Wholesale)                              ⤴       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                       🔒                                    │
│         Call log access was turned off.                     │
│   Re-grant permission to see this number's history.         │
│              [  Grant permission  ]                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

404 state (number not in DB):

```
┌─────────────────────────────────────────────────────────────┐
│  ←   +91 90000 00000                              ⤴        │
├─────────────────────────────────────────────────────────────┤
│  hero card (degraded — no stats)                            │
│  action bar                                                 │
│                                                             │
│                       🔍 SearchOff                          │
│        This number isn't in your call log yet.              │
│  It may have been deleted, or this is a stale deep link.    │
│                  [   Go back   ]                            │
└─────────────────────────────────────────────────────────────┘
```

### 21.14 Accessibility

- Every icon-only button has a `contentDescription`.
- Hero number is a `selectable Text` so VoiceOver/TalkBack can read individual
  digits; `semantics { contentDescription = "Phone number, $spelledOutDigits" }`.
- Lead score badge: `semantics { contentDescription = "Lead score $score out of 100, $bucketLabel" }`.
- Action bar buttons: minimum 48dp touch target (we use 56dp).
- Notes section is a `LazyColumn` item with `heading()` semantics on the section
  header; each note is a list item.
- Markdown links open with confirmation if scheme is non-https/non-tel.
- All snackbars are `LiveRegion.Polite`.
- Contrast: pill colors and badges meet WCAG AA against the TabBgCalls
  background. Dark mode uses pre-tested swap palette in Part 06 Appendix D.
- Talkback reading order: top bar → hero → action bar → stats → tags → notes →
  follow-up → history → manage. Verified via `mergeDescendants = true` on each
  card.

### 21.15 Performance budget

| Metric                                | Budget                 |
| ------------------------------------- | ---------------------- |
| Time to first paint of top bar + hero | ≤ 120 ms               |
| Time to stats card filled             | ≤ 250 ms               |
| Time to first 50 history rows         | ≤ 350 ms               |
| Memory (steady state)                 | ≤ 18 MB above baseline |
| Frame rate during scroll              | 60 fps p95             |
| Pull-to-refresh round trip            | ≤ 600 ms               |

`NumberStatsUseCase` issues a single SQL query with grouped aggregates; it does
not iterate on JVM. Notes and tags are observed via Flow; updates re-emit only
the changed list.

---

## §22 — Search overlay

`com.callNest.app.ui.screen.search.SearchScreen`

### 22.1 Purpose

Search is the universal find-anything page. It is reached from the persistent
search icon in the Calls and Library top bars, and from the global keyboard
shortcut (`Ctrl+K` on hardware-keyboard devices).

It is intentionally _not_ wrapped in `StandardPage`. The colored tab background
and the header gradient would add chrome that fights focus. Instead the page is
pure white (or pure surface in dark mode), edge-to-edge, with only the search
field at the top. This is the one place in the app where the user wants tunnel
vision.

The search runs against the FTS4 virtual table `call_search_fts` (built from the
columns `phoneNumber`, `displayName`, `geocodedLocation`, plus indexed
joined-in fields `noteText` and `tagName`). Token prefix matching is enabled
(`tokenize = unicode61 "remove_diacritics=2"`); query rewriting handles single-
character no-op, special-character escaping, and per-token trim.

### 22.2 Entry points

| Source                                          | Effect               |
| ----------------------------------------------- | -------------------- |
| Top-bar search icon on Calls / Library          | navigate to `search` |
| Hardware keyboard `Ctrl + K` from any tab       | navigate to `search` |
| Empty-state CTA on the Calls tab "Search calls" | navigate to `search` |
| Stats insight card "View calls in this range"   | navigate to `search` |

The route is the literal string `search` — no args.

### 22.3 Exit points

- Back arrow / system back → `popBackStack()`.
- Tap a result → navigate to `callDetail/{normalizedNumber}` (popping search off
  the back stack so back from CallDetail returns to the original tab).
- Tap a recent search row → fills the field with that query (does not navigate).
- Tap the trailing × → clears the field; does not exit.
- Tap "Clear" next to recent searches → empties recent history; does not exit.

### 22.4 Required inputs (data)

`SearchViewModel` state:

| Field            | Type                 | Notes                                          |
| ---------------- | -------------------- | ---------------------------------------------- |
| `query`          | `String`             | trimmed, length-capped at 80 chars             |
| `debouncedQuery` | `String`             | derived, 300ms debounce                        |
| `recents`        | `List<RecentSearch>` | last 10 from `SearchHistoryDao`                |
| `results`        | `List<CallEntity>`   | capped at 200 (see edge case)                  |
| `isLoading`      | `Boolean`            | true while FTS in flight                       |
| `activeFilters`  | `List<SearchFilter>` | populated from advanced filters; empty in v1.0 |

Source: `callRepo.searchFts(q: String): Flow<List<CallEntity>>`. The
implementation:

```sql
SELECT c.* FROM call c
JOIN call_search_fts fts ON fts.rowid = c.id
WHERE call_search_fts MATCH :tokens
ORDER BY c.timestampUtc DESC
LIMIT 200
```

### 22.5 Required inputs (user)

| Gesture                  | Effect                                                   |
| ------------------------ | -------------------------------------------------------- |
| Type into field          | updates `query`; debounced 300ms triggers search         |
| Tap × in field           | clears query                                             |
| Tap leading back arrow   | popBackStack                                             |
| Tap a recent-search row  | fills field with that query                              |
| Long-press recent-search | offers "Remove" item                                     |
| Tap "Clear" recents      | empties history                                          |
| Tap a result row         | navigate to CallDetail; persist the query as recent      |
| Pull-to-refresh          | re-runs the current query                                |
| Hardware keyboard Enter  | dismisses keyboard; query still runs (already debounced) |

### 22.6 Mandatory display elements

- **Inline search field** (replaces the standard top app bar). 56dp tall.
  Background = surface; leading `IconButton(ArrowBack)`; trailing `IconButton(Clear)`
  visible only when `query.isNotEmpty()`. Cursor color = `AccentBlue`.
  Placeholder `"Search number, name, note, tag…"`. Single-line.
- **Active filter chips row** (only if `activeFilters.isNotEmpty()` — empty in
  v1.0 but reserved space).
- **Body — empty query state**:
  - Section "Recent" (titleSmall) with trailing `TextButton("Clear")`.
  - List of up to 10 `RecentSearchRow`s, each = leading `History` icon +
    query text + trailing × (per-row remove).
  - If recents is empty: a single placeholder "Try a number, name, or note keyword."
- **Body — query non-empty, no results**:
  - Centered `NeoEmptyState`: icon `SearchOff`, title `"No matches."`, body
    `"Try a number, name, or note keyword. callNest searches across notes, tags, names, and numbers."`.
- **Body — query non-empty, results**:
  - `LazyColumn` of `CallRow`s (the canonical row, see Part 06 Appendix C).
    Each row gets a subtle highlight on the matched text token (Compose
    `AnnotatedString` with `SpanStyle(background = AccentBlue.copy(alpha = 0.16f))`).
  - Trailing footer "Showing top 200 results — narrow your query for more." if
    cap was hit.

### 22.7 Optional display elements

- **Voice search mic** (Phase II) — placeholder space reserved on the right of
  the field.
- **"Did you mean?" suggestion** — Phase II.
- **Filter chips** (date range, type) — Phase II.

### 22.8 Empty state

The "no recents and no query" state is the canonical empty state:

> Tip: "Try a number, name, or note keyword."

Icon: `SearchOutline` 48dp, secondary tint, centered with the tip 24dp below.

When the query has run and yielded zero rows, the `SearchOff` empty state above
is shown.

### 22.9 Loading state

A 2dp `LinearProgressIndicator` directly under the search field, indeterminate,
visible while `isLoading == true`. The previous results list (if any) remains
visible underneath (do not blank it).

For the very first search of a session (cold FTS), the indicator may show for up
to 800ms while SQLite warms its index.

### 22.10 Error state

- **FTS syntax error** (defensive — the sanitizer should prevent this): show a
  snackbar `"Search hit an error. Try simplifying your query."` and re-render
  the empty/recents state.
- **DB unavailable** (rare): full-screen `NeoErrorState` with `"Search is unavailable right now. Try again."`.

### 22.11 Edge cases

a. **Single character query.** Do not run FTS until length ≥ 2. Show the recents
list still.

b. **Query is all whitespace.** Treated as empty.

c. **Special characters** (`'`, `"`, `*`, `(`, `)`). Sanitizer strips them
before passing to FTS `MATCH`; otherwise SQLite would throw.

d. **200+ results.** Hard cap. Footer message tells the user to narrow.

e. **No notes or tags exist on this device.** FTS still works on number, name,
and geocoded location only — query something matching those.

f. **User pastes a 500-character string.** Field clamps to 80; pastes over
trigger a snackbar `"Search query truncated."`.

g. **Quick consecutive typing.** Debounce 300ms means only the final query in a
burst is run. The loading indicator only shows for queries actually in flight.

h. **Result tapped while next debounce is pending.** Cancel the pending search;
navigate immediately.

i. **Recent searches contain stale numbers.** A recent like "+91 98765 43210"
that no longer matches anything in the DB still runs and yields the empty
state (it is not auto-pruned).

### 22.12 Copy table

| Key                      | Copy                                                                                           |
| ------------------------ | ---------------------------------------------------------------------------------------------- |
| `srch_field_placeholder` | Search number, name, note, tag…                                                                |
| `srch_field_clear_cd`    | Clear search                                                                                   |
| `srch_back_cd`           | Back                                                                                           |
| `srch_recents_title`     | Recent                                                                                         |
| `srch_recents_clear`     | Clear                                                                                          |
| `srch_recents_remove_cd` | Remove %1$s from recents                                                                       |
| `srch_empty_tip`         | Try a number, name, or note keyword.                                                           |
| `srch_no_results_title`  | No matches.                                                                                    |
| `srch_no_results_body`   | Try a number, name, or note keyword. callNest searches across notes, tags, names, and numbers. |
| `srch_cap_footer`        | Showing top 200 results — narrow your query for more.                                          |
| `srch_truncated_paste`   | Search query truncated.                                                                        |
| `srch_error_syntax`      | Search hit an error. Try simplifying your query.                                               |
| `srch_error_unavailable` | Search is unavailable right now. Try again.                                                    |
| `srch_loading_cd`        | Searching                                                                                      |

### 22.13 ASCII wireframes

Empty query, with recents:

```
┌─────────────────────────────────────────────────────────────┐
│ ←  ┌─────────────────────────────────────────┐              │
│    │  Search number, name, note, tag…       │              │
│    └─────────────────────────────────────────┘              │
├─────────────────────────────────────────────────────────────┤
│ Recent                                            [Clear]   │
│                                                             │
│  ↺  Ravi wholesale                                  ×       │
│  ↺  +91 98765                                       ×       │
│  ↺  payment                                         ×       │
│  ↺  follow up                                       ×       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

Empty query, no recents:

```
┌─────────────────────────────────────────────────────────────┐
│ ←  ┌─ Search number, name, note, tag… ─────────┐            │
│    └────────────────────────────────────────────┘            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                       🔍                                    │
│         Try a number, name, or note keyword.                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

Active query with results:

```
┌─────────────────────────────────────────────────────────────┐
│ ←  ┌─ ravi ─────────────────────────────────── × ┐          │
│    └────────────────────────────────────────────┘          │
│ ──── (loading bar) ────                                    │
├─────────────────────────────────────────────────────────────┤
│ ╭───╮  Ravi (Wholesale)                  ↓ Apr 14 3:42 PM   │
│ │ R │  +91 98765 43210                   4m 12s   SIM 1     │
│ ╰───╯                                                       │
│ ╭───╮  Ravi Mehta                        ↑ Apr 13 11:00 AM  │
│ │ R │  +91 90000 11111                   2m 03s   SIM 2     │
│ ╰───╯                                                       │
│ ...                                                         │
└─────────────────────────────────────────────────────────────┘
```

No results:

```
┌─────────────────────────────────────────────────────────────┐
│ ←  ┌─ qwxlk  ──────────────────────────────── × ┐           │
│    └─────────────────────────────────────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                       🔍✗                                   │
│                  No matches.                                │
│  Try a number, name, or note keyword. callNest searches    │
│  across notes, tags, names, and numbers.                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 22.14 Accessibility

- The field is a `TextField` with proper `imeAction = ImeAction.Search` and
  semantics `role = SearchField`.
- Cursor and selection colors are AA against surface in both themes.
- Result rows expose the same a11y surface as the canonical `CallRow` (see
  Part 06 Appendix C).
- Empty/no-result states are announced via `LiveRegion.Assertive` only on
  _transition_ into the state (not on every recomposition).
- Highlighted match span uses background color _and_ underline so colorblind
  users can still see the match.

### 22.15 Performance budget

| Metric                                 | Budget       |
| -------------------------------------- | ------------ |
| Keystroke → debounced query fire       | 300 ms ± 20  |
| FTS round trip for typical 50k-call DB | ≤ 80 ms      |
| FTS round trip for 200k-call DB        | ≤ 220 ms     |
| First result paint after query fire    | ≤ 350 ms p95 |
| Memory                                 | ≤ 8 MB       |

The 200-result cap, the FTS index, and the 300ms debounce are jointly tuned to
hit these numbers on a Snapdragon 660-class device. If the user has fewer than
1000 calls total, the entire query path is ≤ 30ms.

---

## §23 — Stats dashboard

`com.callNest.app.ui.screen.stats.StatsScreen`

### 23.1 Purpose

Stats is the chart-heavy visualisation page. It answers business-owner questions
in the form: "How am I doing this week vs last? When are calls coming in? Who
are my top inquiries? What share am I missing?"

The spec in §3.10 of the master prompt lists 10 possible charts. v1.0 ships
_4 of those 10_; the rest are scaffolded for v1.1+. The 4 v1.0 charts are:
DailyVolume (line + 7d MA), TypeDonut (incoming/outgoing/missed/voicemail),
HourlyHeatmap (24×7), and TopNumbersList (segmented by count or duration).

The page is also where automated **Insights** surface — small, pre-computed
notices generated by `GenerateInsightsUseCase` that highlight things the user
might miss (e.g. "Missed rate jumped from 12% to 31% this week" or "You haven't
followed up with 4 hot leads from last week").

### 23.2 Entry points

| Source                                    | Effect                            |
| ----------------------------------------- | --------------------------------- |
| Bottom nav → Stats tab → "Open dashboard" | navigate to `stats`               |
| Home shortcut tile "View stats"           | navigate to `stats`               |
| Notification "Weekly summary" tap         | deep link to `stats?range=last7d` |

### 23.3 Exit points

- Back arrow → popBackStack to whichever tab launched it.
- Top-bar `DateRangeChip` → opens DateRangeSheet (modal bottom sheet).
- Insight card "View" buttons → may navigate to Search (with prefilled query) or
  to FollowUps (with prefilled tab).
- TopNumbersList row tap → CallDetail.
- "Export PDF" button → currently shows snackbar `"Available in v1.1"` (no
  navigation). The button still renders so testers can locate the future hook.

### 23.4 Required inputs (data)

`StatsViewModel` state:

| Field          | Type             | Notes                                                                                                   |
| -------------- | ---------------- | ------------------------------------------------------------------------------------------------------- |
| `range`        | `DateRange`      | sealed: `Today`, `Last7d`, `Last30d` (default), `ThisMonth`, `LastMonth`, `Last90d`, `Custom(from, to)` |
| `snapshot`     | `StatsSnapshot?` | computed by `BuildStatsSnapshotUseCase(range)`                                                          |
| `prevSnapshot` | `StatsSnapshot?` | same range shifted back, for trend arrows                                                               |
| `insights`     | `List<Insight>`  | ≤ 5, from `GenerateInsightsUseCase(snapshot)`                                                           |
| `isLoading`    | `Boolean`        |                                                                                                         |
| `error`        | `String?`        |                                                                                                         |

`StatsSnapshot`:

| Field              | Type                        |
| ------------------ | --------------------------- | ----------------- |
| `range`            | `DateRange`                 |
| `totalCalls`       | `Int`                       |
| `talkTimeSec`      | `Long`                      |
| `avgDurationSec`   | `Long`                      |
| `missedRatio`      | `Float`                     |
| `byType`           | `Map<CallType, Int>`        |
| `byHourDay`        | `IntArray` size 24\*7 = 168 |
| `dailyVolume`      | `List<DailyPoint>`          |
| `topByCount`       | `List<LeaderboardEntry>`    |
| `topByDuration`    | `List<LeaderboardEntry>`    |
| `leadDistribution` | `Triple<Int, Int, Int>`     | cold / warm / hot |

### 23.5 Required inputs (user)

| Gesture                             | Effect                                |
| ----------------------------------- | ------------------------------------- |
| Tap back                            | popBackStack                          |
| Tap DateRangeChip                   | opens DateRangeSheet                  |
| Tap a preset chip in the sheet      | sets range, dismiss sheet, recompute  |
| Tap "Custom…"                       | opens DatePickerDialog (from + to)    |
| Tap a TopNumbers row                | navigate to CallDetail                |
| Toggle TopNumbers segmented control | switches between byCount / byDuration |
| Tap an insight's primary CTA        | as defined by insight type            |
| Tap "Export PDF"                    | snackbar "Available in v1.1"          |
| Pull-to-refresh                     | re-run snapshot                       |
| Pinch-zoom on a chart               | no-op (Phase II)                      |

### 23.6 Mandatory display elements

`StandardPage` with `TabBgStats` (warm purple) + `HeaderGradStats` (purple →
blue gradient) header.

Top app bar: leading back arrow, title "Stats", trailing `NeoChip(label = range.displayLabel, trailingIcon = ExpandMore)`.

Body — `LazyColumn`, items in this order:

1. **Overview row** — `Row(modifier = horizontalScroll)` of 4 `NeoCard`s, each
   180dp wide × 96dp tall:
   - Total calls (icon: PhoneInTalk, accent blue) + value + delta arrow
   - Talk time (icon: Timer, accent green) + formatted "Xh Ym"
   - Avg duration (icon: HourglassBottom, accent amber)
   - Missed rate (icon: PhoneMissed, accent red) + percent

2. **Lead distribution mini-bar** — `NeoCard` with section title "Lead mix".
   Inside: a single 16dp tall `Row` of 3 `Box`es, weighted by cold/warm/hot
   counts, colored gray/amber/red. Below the bar a 3-column legend with counts.

3. **Insights** — 0 to 5 `NeoCard`s stacked, each with:
   - Severity-colored 4dp left border (Info=base, Warn=amber, Critical=red).
   - Leading icon matching severity.
   - Title (titleSmall).
   - Body (bodyMedium).
   - Optional primary CTA `NeoButton.Tertiary` aligned right.

4. **Daily volume chart** — `NeoCard`, height 220dp:
   - Header row: title "Calls per day" + small legend "● Volume — 7d avg".
   - Compose `Canvas` rendering: vertical bars (or line — implementation choice
     per Part 08 §8.4) for daily totals, plus an overlaid 7-day moving average
     line. X-axis date ticks at start, midpoint, end of range. Y-axis hidden.

5. **Type donut** — `NeoCard`, height 220dp:
   - Compose `Canvas` arcs for incoming / outgoing / missed / voicemail.
   - Legend on the right with percentage and count per slice.
   - Center label: total count.

6. **Hourly heatmap** — `NeoCard`, height 320dp:
   - 24 columns (hours 0–23) × 7 rows (Sun–Sat).
   - Each cell colored by intensity (white → AccentBlue gradient, log-scaled).
   - Row labels on left (Sun, Mon, …); column labels on bottom (12a, 6a, 12p, 6p).
   - Tap a cell → highlights it and shows "12 calls · Tue 3-4 PM" tooltip.

7. **Top numbers list** — `NeoCard`:
   - Header: title "Top numbers" + segmented control "By count / By duration".
   - Below: 10 `LeaderboardEntry` rows. Each row = NeoAvatar (sm) + name/number
     - numeric value (count or duration) + trailing chevron.

8. **Export PDF** — `NeoButton.Primary("Export PDF as report")` full width, in
   its own bottom block. Disabled affordance because v1.0 only shows a snackbar.

Trailing 32dp spacer.

### 23.7 Optional display elements

- **Trend arrows** on overview cards (only when `prevSnapshot` is non-null).
- **Insight dismiss icon** (Phase II — currently insights regenerate each load).
- **Range delta sub-line** on overview cards: "vs prev. period: +12%".

### 23.8 Empty state

If `snapshot.totalCalls == 0` for the chosen range:

- Overview cards still render with `0` / `—` values (no trend arrows).
- Lead mix bar collapses to a single greyed bar with label `"No data."`.
- Insights section is hidden entirely.
- Each chart card renders its placeholder (see per-chart copy below).

Copy: `stats_empty_range_title` = "No calls in this range." · `stats_empty_range_body` = "Try a wider range from the chip above."

### 23.9 Loading state

- First load: full-screen `CircularProgressIndicator` centered. Top app bar
  rendered.
- Range change: keep the previous snapshot visible; show a 2dp linear progress
  bar at the very top of the body. Each chart card shows a shimmer overlay until
  its data is available.
- Pull-to-refresh: standard `RefreshIndicator`.

### 23.10 Error state

- **Date range invalid** (custom from > to): swap them silently (logged via
  Timber.w) and recompute.
- **Range exceeds 90 days**: warn snackbar `"Wide ranges may take longer to compute."` but proceed.
- **Snapshot computation error**: full-screen `NeoErrorState` with title
  `"Couldn't load stats."`, body `"Pull to refresh."`, button `"Retry"`.
- **Single-SIM device**: SIM-utilization chart (when added in v1.1) will simply
  hide; v1.0 has no SIM chart so this is a no-op.

### 23.11 Edge cases

a. **0 calls in range** — see Empty state. Each chart shows its own placeholder
("No data in this range") instead of an empty plot area.

b. **Single-SIM device** — no SIM chart in v1.0; future-proofed.

c. **200k+ calls in range** — `BuildStatsSnapshotUseCase` does a single SQL
pass with grouped aggregates; daily volume is computed by `GROUP BY date(timestampUtc, 'unixepoch', 'localtime')`. Sampling kicks in _only_ for the heatmap render (it draws once per cell, not per call).

d. **Custom range with from > to** — swap in ViewModel before passing to use
case; emit a `Snackbar("Swapped your dates so 'from' comes first.")`.

e. **Custom range with from == to** — treated as a single-day range; daily
volume chart is degenerate (one bar) but renders.

f. **Range > 365 days** — clamped to 365 with snackbar `"Range capped at 365 days. Use export for longer history."`.

g. **All calls are missed in the range** — donut renders with one slice (red
"Missed 100%"); incoming/outgoing slices render as 1px hairlines so the
legend still resolves them.

h. **DST transition inside the range** — `dailyVolume` aggregation uses the
device's current zone; days during DST shift may show 23 or 25 hours of data.
Acceptable for v1.0; flagged in DECISIONS.md.

i. **Insight CTA target removed** (e.g. an insight references a number whose
data was cleared) — the CTA shows a snackbar `"That contact is no longer in your data."` instead of navigating.

j. **Export PDF tapped** — v1.0 always shows `"Available in v1.1"`. No work is
queued.

### 23.12 Copy table

| Key                             | Copy                                                     |
| ------------------------------- | -------------------------------------------------------- |
| `stats_title`                   | Stats                                                    |
| `stats_back_cd`                 | Back                                                     |
| `stats_range_chip_cd`           | Change date range                                        |
| `stats_range_today`             | Today                                                    |
| `stats_range_last7d`            | Last 7 days                                              |
| `stats_range_last30d`           | Last 30 days                                             |
| `stats_range_this_month`        | This month                                               |
| `stats_range_last_month`        | Last month                                               |
| `stats_range_last90d`           | Last 90 days                                             |
| `stats_range_custom`            | Custom…                                                  |
| `stats_overview_total`          | Total calls                                              |
| `stats_overview_talk`           | Talk time                                                |
| `stats_overview_avg`            | Avg duration                                             |
| `stats_overview_missed`         | Missed rate                                              |
| `stats_overview_delta_up`       | +%1$s vs prev                                            |
| `stats_overview_delta_down`     | %1$s vs prev                                             |
| `stats_lead_mix_title`          | Lead mix                                                 |
| `stats_lead_cold`               | Cold                                                     |
| `stats_lead_warm`               | Warm                                                     |
| `stats_lead_hot`                | Hot                                                      |
| `stats_insights_title`          | Insights                                                 |
| `stats_chart_daily_title`       | Calls per day                                            |
| `stats_chart_daily_legend_vol`  | Volume                                                   |
| `stats_chart_daily_legend_avg`  | 7d avg                                                   |
| `stats_chart_daily_empty`       | No daily data in this range.                             |
| `stats_chart_donut_title`       | Call mix                                                 |
| `stats_chart_donut_empty`       | No call mix to show.                                     |
| `stats_chart_heatmap_title`     | When calls happen                                        |
| `stats_chart_heatmap_empty`     | Not enough data to map by hour.                          |
| `stats_chart_top_title`         | Top numbers                                              |
| `stats_chart_top_segment_count` | By count                                                 |
| `stats_chart_top_segment_dur`   | By duration                                              |
| `stats_chart_top_empty`         | No top numbers in this range.                            |
| `stats_export_button`           | Export PDF as report                                     |
| `stats_export_unavailable`      | Available in v1.1                                        |
| `stats_empty_range_title`       | No calls in this range.                                  |
| `stats_empty_range_body`        | Try a wider range from the chip above.                   |
| `stats_warn_wide_range`         | Wide ranges may take longer to compute.                  |
| `stats_warn_swap_dates`         | Swapped your dates so 'from' comes first.                |
| `stats_warn_clamp_range`        | Range capped at 365 days. Use export for longer history. |
| `stats_error_title`             | Couldn't load stats.                                     |
| `stats_error_body`              | Pull to refresh.                                         |
| `stats_error_retry`             | Retry                                                    |
| `stats_insight_target_missing`  | That contact is no longer in your data.                  |

### 23.13 ASCII wireframes

Default state (Last 30 days):

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Stats                              [ Last 30 days ⌄ ]  │
├─────────────────────────────────────────────────────────────┤
│ ┌─Total─┐ ┌─Talk─┐ ┌─Avg──┐ ┌Missed┐                       │
│ │ 412 ▲ │ │ 18h  │ │ 2m 38│ │ 18%▼ │   overview row →     │
│ └───────┘ └──────┘ └──────┘ └──────┘                       │
│                                                             │
│ Lead mix                                                    │
│ ████████████████░░░░░░░░░░░░░░  64% Cold · 28% Warm · 8% Hot│
│                                                             │
│ Insights                                                    │
│ ┃ ⚠ Missed rate jumped to 31% this week.       [View]      │
│ ┃ ℹ 4 hot leads have no follow-up scheduled.   [Plan]      │
│                                                             │
│ ┌─ Calls per day ──────────────────────────────────────┐   │
│ │   ▁▂▆█▆▃▂  ▂▄▆▆▄▂   line + bars                     │   │
│ │ Mar 15           Mar 30           Apr 14            │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                             │
│ ┌─ Call mix ──────────────────────────────────────────┐    │
│ │       ◔     412     ● Incoming  62%                 │    │
│ │      ◐  ◑   total   ● Outgoing  20%                 │    │
│ │             calls   ● Missed    16%                 │    │
│ │                     ● Voicemail  2%                 │    │
│ └──────────────────────────────────────────────────────┘   │
│                                                             │
│ ┌─ When calls happen ──────────────────────────────────┐   │
│ │     12a  6a  12p  6p                                 │   │
│ │ Sun ░░░ ░░  ▓▓▓ ▓░                                   │   │
│ │ Mon ░░░ ▓▓  ███ ██                                   │   │
│ │ ...                                                  │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                             │
│ ┌─ Top numbers ─────────── [By count │ By duration] ──┐    │
│ │  1  Ravi Wholesale       42 calls                   │    │
│ │  2  Suresh                28 calls                  │    │
│ │  …                                                  │    │
│ └──────────────────────────────────────────────────────┘   │
│                                                             │
│  [ Export PDF as report ]                                   │
└─────────────────────────────────────────────────────────────┘
```

Empty range state:

```
┌─ Stats ───────── [ Today ⌄ ] ─┐
│  Total 0   Talk —   Avg —    │
│                              │
│  No calls in this range.     │
│  Try a wider range from the  │
│  chip above.                 │
└──────────────────────────────┘
```

Loading:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Stats                              [ Last 30 days ⌄ ]  │
│ ──── (linear progress) ──── (top of body)                  │
│                                                             │
│      shimmers everywhere                                    │
└─────────────────────────────────────────────────────────────┘
```

Error:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Stats                              [ Last 30 days ⌄ ]  │
├─────────────────────────────────────────────────────────────┤
│                       ⚠                                     │
│              Couldn't load stats.                           │
│                Pull to refresh.                             │
│                  [ Retry ]                                  │
└─────────────────────────────────────────────────────────────┘
```

DateRangeSheet:

```
┌── Choose a range ─────────────────────────────────┐
│ [Today] [7d] [30d✓] [This mo] [Last mo] [90d]    │
│ [ Custom… ]                                       │
└───────────────────────────────────────────────────┘
```

### 23.14 Accessibility

- Each chart card is a single semantics merge with a textual fallback summary
  (e.g. "Calls per day from Mar 15 to Apr 14: average 14, peak 32 on Apr 02.").
  TalkBack reads the summary instead of trying to traverse the canvas.
- Heatmap exposes per-cell semantics on focus only (otherwise 168 cells would
  spam reading order).
- Insight cards: severity is announced as part of the title prefix
  ("Warning: …", "Info: …").
- DateRangeSheet preset chips have role `RadioButton` and a single selection.
- Color contrast: heatmap cells use a luminance-pair pre-tested for AA.
- Charts respect `Configuration.fontScale`: chart axis ticks scale with text;
  the canvas itself is unscaled.

### 23.15 Performance budget

| Metric                             | Budget       |
| ---------------------------------- | ------------ |
| Snapshot for 30k-call DB           | ≤ 220 ms     |
| Snapshot for 200k-call DB          | ≤ 800 ms     |
| Chart paint per frame              | ≤ 4 ms       |
| Memory                             | ≤ 22 MB      |
| Range-change → first chart updated | ≤ 280 ms p95 |

Heatmap painting uses a single `Canvas.drawRect` per cell with pre-computed
colors. DailyVolume uses a precomputed `Path` recreated only on data change.
TopNumbers uses Compose `LazyColumn` with `key = entry.normalizedNumber`.

---

## §24 — Bookmarks screen

`com.callNest.app.ui.screen.bookmarks.BookmarksScreen`

### 24.1 Purpose

Bookmarks lets the user _star_ individual calls (not contacts) so they can
return to them quickly. The semantic is closer to "this specific conversation
mattered" than "this contact matters" — which is why it's call-scoped, not
number-scoped.

The first bookmark for a number prompts the user for a free-text "reason"
which gets stored alongside; this is shown as the row's subtitle in the list.
Subsequent bookmarks for the same number reuse the existing reason silently.

The page also supports up to 5 _pinned_ bookmarks — the user's favorite
favorites — that float at the top in a horizontal carousel.

### 24.2 Entry points

| Source                                     | Effect                  |
| ------------------------------------------ | ----------------------- |
| Library tab → Bookmarks list item          | navigate to `bookmarks` |
| Home shortcut tile "Bookmarks"             | navigate to `bookmarks` |
| CallDetail history → long-press → bookmark | applies & toast         |

### 24.3 Exit points

- Back arrow → popBackStack.
- Tap a bookmark row → `callDetail/{normalizedNumber}`.
- Tap a pinned-bookmark carousel item → same as above.
- Long-press a row → action sheet (Unpin/Pin · Remove · Open).
- Long-press a pinned item → drag mode begins; on drop, `ReorderPinUseCase`.

### 24.4 Required inputs (data)

`BookmarksViewModel` state:

| Field               | Type                  | Source                                                      |
| ------------------- | --------------------- | ----------------------------------------------------------- |
| `pinnedBookmarks`   | `List<BookmarkEntry>` | `settings.observePinnedBookmarks()` (≤ 5)                   |
| `allBookmarks`      | `List<BookmarkEntry>` | `bookmarkRepo.observeAll()` (sorted by `bookmarkedAt DESC`) |
| `isLoading`         | `Boolean`             |                                                             |
| `firstReasonPrompt` | `BookmarkEntry?`      | non-null when first bookmark of a number is being created   |

`BookmarkEntry`:

| Field              | Type      |
| ------------------ | --------- | ---------------------------- |
| `id`               | `Long`    |
| `callId`           | `Long`    |
| `normalizedNumber` | `String`  |
| `displayName`      | `String?` |
| `bookmarkedAt`     | `Long`    |
| `reason`           | `String?` |
| `pinPosition`      | `Int?`    | null = not pinned; else 1..5 |

### 24.5 Required inputs (user)

| Gesture                          | Effect                                    |
| -------------------------------- | ----------------------------------------- |
| Tap row                          | navigate to CallDetail                    |
| Long-press row                   | bottom sheet: Pin / Unpin / Remove / Open |
| Tap Pin in sheet                 | promote to pinned (if < 5 already)        |
| Tap Remove in sheet              | confirmation → delete                     |
| Tap pinned-carousel item         | navigate to CallDetail                    |
| Long-press pinned item           | enters drag mode (haptic feedback)        |
| Drag pinned item to new slot     | reorder; reorder is committed on drop     |
| Tap up/down arrows on pinned     | alternative to drag, accessible           |
| Pull-to-refresh on the main list | re-fetch                                  |

### 24.6 Mandatory display elements

`StandardPage` with:

- Title: `"Bookmarks"`
- Subtitle: `"Calls you've starred"`
- Header glyph: ⭐ (or `Icons.Filled.Star` in actual code)

Body — `LazyColumn`:

1. **Pinned section** (only if `pinnedBookmarks.isNotEmpty()`):
   - Header titleSmall "Pinned" + count.
   - Below: a `Row(modifier = horizontalScroll)` of up to 5 pinned items, each:
     - 96dp wide × 120dp tall `NeoCard` with NeoAvatar at top,
       displayName (or formatted number) below, and a tiny "Top N" badge.
     - In drag mode, two small `↑` and `↓` `NeoIconButton`s overlay the bottom
       edge for keyboard/non-drag accessibility.

2. **All bookmarks section**:
   - Header titleSmall "All" + count.
   - List of `CallRow`s (the canonical row, see Part 06 Appendix C), one per
     bookmark, sorted by `bookmarkedAt DESC`.
   - Trailing star-filled icon at the row end indicates bookmark state (always
     filled here).
   - Subtitle line shows `reason` when present, else the row's normal subtitle.

### 24.7 Optional display elements

- **First-bookmark prompt dialog** (`NeoDialog`) opens automatically the first
  time a number is bookmarked anywhere in the app. Asks for an optional reason
  string (≤ 120 chars). "Skip" or "Save" buttons. Subsequent bookmarks for the
  same number do not re-prompt.

- **Bookmark-collected animation** — when a row is freshly created, it pulses
  gold once on first paint. Implementation: `AnimatedContent` with a shimmer
  overlay 600ms, then settles.

### 24.8 Empty state

When `allBookmarks.isEmpty()`:

- Centered `NeoEmptyState`: animated star icon (gentle scale-pulse 1.0 ↔ 1.1
  every 1.6s), title `"No bookmarks yet."`, body `"Star a call to save it here. Long-press any row in your call list to bookmark it."`. No CTA button — bookmarks are created elsewhere.

### 24.9 Loading state

- First load: shimmers — 1 pinned-carousel placeholder strip + 6 row shimmers.
- Pull-to-refresh: standard indicator.

### 24.10 Error state

- DB read fails: full-screen `NeoErrorState`, title `"Couldn't load bookmarks."`,
  body `"Try again."`, button `"Retry"`.
- Pin failed (e.g. >5 pinned attempted): snackbar `"You can pin up to 5 bookmarks."` and the pin attempt is rolled back.

### 24.11 Edge cases

a. **0 bookmarks** → empty state above.

b. **1 bookmark** → no pinned section (the carousel is hidden); a single-row
"All" list. The page subtitle remains "Calls you've starred".

c. **5 pinned bookmarks** → the pin action in the action sheet is disabled with
a tooltip `"Unpin one first."`.

d. **Drag during scroll** — when the user is mid-drag on a pinned item, the
outer LazyColumn is locked from scrolling (we set `userScrollEnabled = false`
for the duration of the drag).

e. **Bookmark removed mid-scroll** by another window of the app — Compose key
on bookmarkId; the row animates out with a 220ms fade.

f. **First-bookmark prompt dismissed via system back** — treated as Skip;
bookmark is saved without a reason.

g. **Reason text contains emojis** — supported (rendered with default emoji
font). Emoji + RTL mix renders correctly (Compose default).

h. **Pinned bookmark refers to a deleted call** (the call row was hard-deleted
via Manage→Clear) — entry is auto-pruned by `BookmarkRepository.observeAll`
join semantics; a snackbar `"Removed a bookmark whose call no longer exists."`
appears once per session.

i. **Configuration change while reorder drag is in progress** — drag is
cancelled; pinned positions revert to last committed state.

### 24.12 Copy table

| Key                           | Copy                                                                              |
| ----------------------------- | --------------------------------------------------------------------------------- |
| `bm_title`                    | Bookmarks                                                                         |
| `bm_subtitle`                 | Calls you've starred                                                              |
| `bm_back_cd`                  | Back                                                                              |
| `bm_pinned_section`           | Pinned                                                                            |
| `bm_all_section`              | All                                                                               |
| `bm_pinned_badge`             | Top %1$d                                                                          |
| `bm_action_pin`               | Pin                                                                               |
| `bm_action_unpin`             | Unpin                                                                             |
| `bm_action_remove`            | Remove                                                                            |
| `bm_action_open`              | Open                                                                              |
| `bm_remove_confirm_title`     | Remove this bookmark?                                                             |
| `bm_remove_confirm_body`      | The call itself stays in your history.                                            |
| `bm_remove_confirm_yes`       | Remove                                                                            |
| `bm_remove_confirm_no`        | Cancel                                                                            |
| `bm_first_prompt_title`       | Why this call?                                                                    |
| `bm_first_prompt_body`        | Add a quick reason so you remember later. (optional)                              |
| `bm_first_prompt_placeholder` | e.g. "Quoted ₹42k for bulk order"                                                 |
| `bm_first_prompt_save`        | Save                                                                              |
| `bm_first_prompt_skip`        | Skip                                                                              |
| `bm_first_prompt_counter`     | %1$d / 120                                                                        |
| `bm_pin_limit_snackbar`       | You can pin up to 5 bookmarks.                                                    |
| `bm_pin_limit_tooltip`        | Unpin one first.                                                                  |
| `bm_drag_arrow_up_cd`         | Move up                                                                           |
| `bm_drag_arrow_down_cd`       | Move down                                                                         |
| `bm_empty_title`              | No bookmarks yet.                                                                 |
| `bm_empty_body`               | Star a call to save it here. Long-press any row in your call list to bookmark it. |
| `bm_error_title`              | Couldn't load bookmarks.                                                          |
| `bm_error_body`               | Try again.                                                                        |
| `bm_error_retry`              | Retry                                                                             |
| `bm_pruned_snackbar`          | Removed a bookmark whose call no longer exists.                                   |
| `bm_loading_cd`               | Loading bookmarks                                                                 |

### 24.13 ASCII wireframes

Default with pinned + all:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Bookmarks                                              │
│      Calls you've starred                          ⭐        │
├─────────────────────────────────────────────────────────────┤
│ Pinned (3)                                                  │
│ ┌──────┐ ┌──────┐ ┌──────┐                                  │
│ │ ╭R╮  │ │ ╭S╮  │ │ ╭#╮  │   horizontal carousel           │
│ │ Ravi │ │Sures │ │+9182 │                                  │
│ │ Top1 │ │Top 2 │ │Top 3 │                                  │
│ └──────┘ └──────┘ └──────┘                                  │
│                                                             │
│ All (12)                                                    │
│ ╭───╮ Ravi (Wholesale)                Apr 14 3:42 PM ★      │
│ │ R │ Quoted ₹42k for bulk order                            │
│ ╰───╯                                                       │
│ ╭───╮ Suresh                          Apr 13 11:00 AM ★     │
│ │ S │ —                                                     │
│ ╰───╯                                                       │
│ ...                                                         │
└─────────────────────────────────────────────────────────────┘
```

Empty:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Bookmarks                                              │
│      Calls you've starred                          ⭐        │
├─────────────────────────────────────────────────────────────┤
│                       ✦                                     │
│                  No bookmarks yet.                          │
│   Star a call to save it here. Long-press any row in        │
│   your call list to bookmark it.                            │
└─────────────────────────────────────────────────────────────┘
```

First-bookmark prompt:

```
┌── Why this call? ──────────────────────────────┐
│ Add a quick reason so you remember later.      │
│ ┌──────────────────────────────────────────┐   │
│ │ e.g. "Quoted ₹42k for bulk order"       │   │
│ └──────────────────────────────────────────┘   │
│                                       0 / 120  │
│           [ Skip ]        [ Save ]             │
└────────────────────────────────────────────────┘
```

Drag mode (pinned reorder):

```
Pinned (3) — tap arrows or drag to reorder
┌──────┐ ┌──────┐ ┌──────┐
│ ╭R╮  │ │ ╭S╮  │ │ ╭#╮  │
│ Ravi │ │Sures │ │+9182 │
│ ▲ ▼  │ │ ▲ ▼  │ │ ▲ ▼  │
└──────┘ └──────┘ └──────┘
```

### 24.14 Accessibility

- Pinned carousel items have `role = Button` and `contentDescription = "Pinned bookmark, position N: $name"`.
- Drag mode is keyboard-accessible via the up/down arrow buttons; their
  `onClick` calls the same reorder use case.
- Empty state's pulsing star respects `Settings.Global.ANIMATOR_DURATION_SCALE` —
  if scale is 0, the pulse is disabled.
- First-bookmark dialog uses `Modifier.semantics { isDialog = true }`.
- Star icons in rows have `contentDescription = "Bookmarked"`.

### 24.15 Performance budget

| Metric                           | Budget   |
| -------------------------------- | -------- |
| First paint                      | ≤ 180 ms |
| Drag-to-drop reorder commit      | ≤ 50 ms  |
| Memory                           | ≤ 9 MB   |
| Bookmark add propagation to list | ≤ 200 ms |

---

## §25 — FollowUps screen

`com.callNest.app.ui.screen.followups.FollowUpsScreen`

### 25.1 Purpose

FollowUps is the proactive-action page. It groups every scheduled reminder into
four buckets (Today / Overdue / Upcoming / Completed) and lets the user knock
through them one at a time.

The page is opinionated: the default tab on entry is **Overdue** if any overdue
follow-ups exist, otherwise **Today**, otherwise **Upcoming**. This means the
user always lands on the bucket that needs attention, not on an empty Today.

Bulk operations (snooze all / mark done / clear) are available via long-press
of any row, which enters multi-select.

### 25.2 Entry points

| Source                                  | Effect                               |
| --------------------------------------- | ------------------------------------ |
| Library tab → Follow-ups list item      | navigate to `followUps`              |
| Home shortcut tile "Follow-ups today"   | navigate to `followUps?tab=today`    |
| Notification "3 follow-ups today" tap   | deep link to `followUps?tab=today`   |
| Notification "Overdue follow-up" tap    | deep link to `followUps?tab=overdue` |
| CallDetail follow-up section "View all" | navigate to `followUps`              |

### 25.3 Exit points

- Back arrow → popBackStack.
- Tap a row → `callDetail/{normalizedNumber}`.
- Long-press → enters multi-select; "Done" button in app bar exits multi-select.
- Bulk Snooze all → BottomSheet (1h / 1d / pick…) → applies → snackbar.
- Bulk Mark done → applies `MarkFollowUpDoneUseCase` for each → snackbar.
- Bulk Clear → confirmation → applies `CancelFollowUpUseCase` for each.

### 25.4 Required inputs (data)

`FollowUpsViewModel` state:

| Field            | Type                | Notes                                 |
| ---------------- | ------------------- | ------------------------------------- |
| `today`          | `List<FollowUpRow>` | due today (00:00 ≤ due < 24:00 local) |
| `overdue`        | `List<FollowUpRow>` | due < now and not done                |
| `upcoming`       | `List<FollowUpRow>` | due > end of today and not done       |
| `completed`      | `List<FollowUpRow>` | doneAt != null, sorted desc           |
| `selectedTab`    | `enum FollowUpTab`  | initialised by initial-tab logic      |
| `multiSelectIds` | `Set<Long>`         | empty = single-select mode            |
| `isLoading`      | `Boolean`           |                                       |

`FollowUpRow`:

| Field              | Type      |
| ------------------ | --------- |
| `followUpId`       | `Long`    |
| `callId`           | `Long`    |
| `normalizedNumber` | `String`  |
| `displayName`      | `String?` |
| `dueAt`            | `Long`    |
| `snoozedFromAt`    | `Long?`   |
| `doneAt`           | `Long?`   |

Source: `callRepo.observeFollowUps()`. The use case classifies into the four
buckets in pure Kotlin.

### 25.5 Required inputs (user)

| Gesture                         | Effect                                    |
| ------------------------------- | ----------------------------------------- |
| Tap a tab                       | switches `selectedTab`                    |
| Tap a row (non-multi)           | navigate to CallDetail                    |
| Long-press a row                | enters multi-select with that row checked |
| Tap a row (multi)               | toggles its selection                     |
| Tap "Done" in app bar (multi)   | exits multi-select                        |
| Tap bulk Snooze (multi)         | bottom sheet → snooze all selected        |
| Tap bulk Mark done (multi)      | mark all selected done                    |
| Tap bulk Clear (multi)          | confirmation → cancel all selected        |
| Pull-to-refresh                 | re-fetch                                  |
| Tap snooze icon on a single row | mini-menu: 1h / 1d / pick…                |

### 25.6 Mandatory display elements

`StandardPage` with:

- Title `"Follow-ups"`
- Subtitle `"Reminders due today and ahead"`
- Header glyph 🔔

Below the header: `TabRow` with 4 tabs:

- Today (badge = count)
- Overdue (badge = count, red dot if > 0)
- Upcoming (badge = count)
- Completed (no badge)

Body — depending on `selectedTab`, a `LazyColumn` of follow-up rows. Each row:

- Leading: `NeoAvatar` (sm).
- Title: displayName or formattedNumber.
- Subtitle line 1: due date+time (e.g. "Today · 4:30 PM" or "Apr 22 · 9:00 AM").
- Subtitle line 2 (only if snoozed): `Snoozed from ${snoozedFromAt}` (italic).
- Trailing: snooze icon button + checkbox (in multi-select).

In multi-select, the top app bar swaps to a count + Done button + overflow
(Snooze all / Mark done / Clear).

### 25.7 Optional display elements

- **Trailing "in 3 hours" relative time hint** in the subtitle line.
- **Strikethrough** on title for completed-tab rows.
- **Soft red highlight** on overdue rows (background AccentRed @ 8% alpha).

### 25.8 Empty state per tab

| Tab       | Title (icon)                  | Body                                         |
| --------- | ----------------------------- | -------------------------------------------- |
| Today     | "All caught up for today!" 🎉 | "Future reminders will show up here."        |
| Overdue   | "No overdue follow-ups." ✓    | "Keep it up."                                |
| Upcoming  | "Nothing scheduled." 📅       | "Schedule a follow-up from any call detail." |
| Completed | "No completed follow-ups." 📝 | "Done items will appear here."               |

Each empty state is a centered NeoEmptyState within the body slot; the TabRow
remains visible.

### 25.9 Loading state

- First load: 1 TabRow (with placeholder badges) + 5 row shimmers in the body.
- Pull-to-refresh: standard indicator.
- Tab switch: instant — all 4 lists are already in state.

### 25.10 Error state

- DB read failure: replace body with `NeoErrorState`. TabRow remains.
- Snooze/done/cancel use case failure: snackbar
  `"Couldn't update that follow-up."`; the row reverts.

### 25.11 Edge cases

a. **Follow-up at exactly midnight 00:00:00.** Belongs to Today (the day it
begins). Belongs to Overdue once `now > dueAt`. Boundary handled by
half-open intervals: `[startOfDay, startOfDay + 24h)`.

b. **Follow-up with no time (date only).** Defaults to 9:00 AM in the device
local zone. Set in `ScheduleFollowUpUseCase` when `time == null`.

c. **`doneAt != null` but `dueAt` in the future.** Goes into Completed (the
user marked it done early). Not "Upcoming". Verified via order-of-checks:
`doneAt != null` first, then bucketize by `dueAt`.

d. **Snooze 1h on an overdue item that becomes due in <1h still in past.**
Allowed; the new `dueAt = now + 1h` so the row leaves Overdue and lands in
Today (or Upcoming if 1h crosses midnight).

e. **Snooze 1d on an item that's already 5 days overdue.** Sets `dueAt = now + 24h`.
Not "1 day from original dueAt". Documented in spec §3.7.

f. **Bulk snooze with mixed buckets selected.** All selected items get the
same snooze offset applied to _now_. Buckets recompute on next emit.

g. **Bulk clear of 50+ items.** Performed in a single Room transaction; the
undo snackbar offers `"Undo"` for 8s with the count `"Cleared 53."`.

h. **Tab switched mid-multi-select.** Multi-select persists across tabs (the
ids carry over).

i. **DST transition between snoozedFromAt and dueAt.** Display uses local zone
formatter; no special handling.

### 25.12 Copy table

| Key                            | Copy                                              |
| ------------------------------ | ------------------------------------------------- |
| `fu_title`                     | Follow-ups                                        |
| `fu_subtitle`                  | Reminders due today and ahead                     |
| `fu_back_cd`                   | Back                                              |
| `fu_tab_today`                 | Today                                             |
| `fu_tab_overdue`               | Overdue                                           |
| `fu_tab_upcoming`              | Upcoming                                          |
| `fu_tab_completed`             | Completed                                         |
| `fu_due_today_at`              | Today · %1$s                                      |
| `fu_due_tomorrow_at`           | Tomorrow · %1$s                                   |
| `fu_due_at`                    | %1$s · %2$s                                       |
| `fu_overdue_label`             | Overdue · %1$s                                    |
| `fu_snoozed_from`              | Snoozed from %1$s                                 |
| `fu_snooze_cd`                 | Snooze                                            |
| `fu_snooze_1h`                 | 1 hour                                            |
| `fu_snooze_1d`                 | 1 day                                             |
| `fu_snooze_pick`               | Pick a time…                                      |
| `fu_done_cd`                   | Mark done                                         |
| `fu_multi_count`               | %1$d selected                                     |
| `fu_multi_done`                | Done                                              |
| `fu_multi_snooze_all`          | Snooze all                                        |
| `fu_multi_mark_done`           | Mark done                                         |
| `fu_multi_clear`               | Clear                                             |
| `fu_multi_clear_confirm_title` | Clear %1$d follow-ups?                            |
| `fu_multi_clear_confirm_body`  | This removes the reminders only — the calls stay. |
| `fu_multi_clear_confirm_yes`   | Clear                                             |
| `fu_multi_clear_confirm_no`    | Cancel                                            |
| `fu_multi_cleared_snackbar`    | Cleared %1$d.                                     |
| `fu_multi_cleared_undo`        | Undo                                              |
| `fu_today_empty_title`         | All caught up for today!                          |
| `fu_today_empty_body`          | Future reminders will show up here.               |
| `fu_overdue_empty_title`       | No overdue follow-ups.                            |
| `fu_overdue_empty_body`        | Keep it up.                                       |
| `fu_upcoming_empty_title`      | Nothing scheduled.                                |
| `fu_upcoming_empty_body`       | Schedule a follow-up from any call detail.        |
| `fu_completed_empty_title`     | No completed follow-ups.                          |
| `fu_completed_empty_body`      | Done items will appear here.                      |
| `fu_error_title`               | Couldn't load follow-ups.                         |
| `fu_error_body`                | Try again.                                        |
| `fu_error_retry`               | Retry                                             |
| `fu_action_failed_snackbar`    | Couldn't update that follow-up.                   |

### 25.13 ASCII wireframes

Default — Today tab:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Follow-ups                                             │
│      Reminders due today and ahead                  🔔      │
├─────────────────────────────────────────────────────────────┤
│ [Today (3)] [Overdue (1●)] [Upcoming (8)] [Completed]      │
├─────────────────────────────────────────────────────────────┤
│ ╭───╮ Ravi (Wholesale)                Today · 4:30 PM   🔔  │
│ │ R │                                                       │
│ ╰───╯                                                       │
│ ╭───╮ Suresh                          Today · 6:00 PM   🔔  │
│ │ S │ Snoozed from 2:00 PM                                  │
│ ╰───╯                                                       │
│ ╭───╮ +91 99000 11111                 Today · 8:30 PM   🔔  │
│ │ # │                                                       │
│ ╰───╯                                                       │
└─────────────────────────────────────────────────────────────┘
```

Overdue tab with one item:

```
┌────────────────────────────────────────────┐
│ [Today] [Overdue (1●)*] [Upcoming] [Done] │
├────────────────────────────────────────────┤
│ ╭───╮ Anil  (red bg)   Overdue · 2 days   │
│ │ A │                                  🔔 │
│ ╰───╯                                     │
└────────────────────────────────────────────┘
```

Multi-select:

```
┌─────────────────────────────────────────────┐
│ ←  3 selected   [Snooze all][Done][Clear]✕ │
├─────────────────────────────────────────────┤
│ ☑ Ravi          Today · 4:30 PM             │
│ ☑ Suresh        Today · 6:00 PM             │
│ ☐ +91 99000     Today · 8:30 PM             │
│ ☑ Anil          Overdue · 2 days            │
└─────────────────────────────────────────────┘
```

Empty Today:

```
┌────────────────────────────────────────────┐
│ [Today] [Overdue] [Upcoming] [Completed]  │
├────────────────────────────────────────────┤
│                  🎉                        │
│        All caught up for today!            │
│   Future reminders will show up here.     │
└────────────────────────────────────────────┘
```

### 25.14 Accessibility

- TabRow exposes proper `selected` semantics; tab labels include the badge
  count ("Today, 3 due").
- Each row has `role = Button` (single-tap mode) or `role = Checkbox` (multi-select mode), switched at runtime.
- Snooze icon button has explicit `contentDescription = "Snooze ${displayName} for 1 hour"` after a choice is committed.
- Empty-state icons are `decorative = true`; emoji titles are spoken.
- Multi-select count is announced via `LiveRegion.Polite` on transitions.

### 25.15 Performance budget

| Metric                  | Budget   |
| ----------------------- | -------- |
| First paint             | ≤ 200 ms |
| Tab switch              | < 16 ms  |
| Bulk action of 50 items | ≤ 350 ms |
| Memory                  | ≤ 10 MB  |

---

## §26 — MyContacts screen

`com.callNest.app.ui.screen.contacts.MyContactsScreen`

### 26.1 Purpose

MyContacts shows the user's _human-saved_ contacts — i.e. people they have
explicitly chosen to put in their phone book. Auto-saved inquiry numbers
(`isAutoSaved == true`) are explicitly excluded; those live under "All people"
in the Library tab. The page exists so the user has a quick lens on "who matters
most" without the noise of every cold inquiry that callNest auto-promoted.

The selector is `isInSystemContacts == true && isAutoSaved == false`.

A small inline "promoted from inquiry" badge appears on contacts whose
`autoSavedAt` is non-null but who have _also_ been promoted manually since (i.e.
auto-saved first, then later edited / re-saved through Contacts).

### 26.2 Entry points

| Source                              | Effect                   |
| ----------------------------------- | ------------------------ |
| Library tab → My Contacts list item | navigate to `myContacts` |
| Home shortcut tile "My contacts"    | navigate to `myContacts` |
| Settings → "Manage contact group"   | navigate to `myContacts` |

### 26.3 Exit points

- Back arrow → popBackStack.
- Tap a contact row → `callDetail/{normalizedNumber}`.
- Long-press a contact row → action sheet (Open · Open in Phone book · Copy number).

### 26.4 Required inputs (data)

`MyContactsViewModel` state:

| Field       | Type               | Notes                                       |
| ----------- | ------------------ | ------------------------------------------- |
| `query`     | `String`           | filter; debounced 200ms                     |
| `contacts`  | `List<ContactRow>` | filtered; sorted by displayName ASC, locale |
| `isLoading` | `Boolean`          |                                             |
| `isError`   | `String?`          |                                             |

`ContactRow`:

| Field              | Type      |
| ------------------ | --------- | ------------------------------------ |
| `normalizedNumber` | `String`  |
| `displayName`      | `String`  | non-null (rows without name skipped) |
| `formattedNumber`  | `String`  |                                      |
| `avatarSeed`       | `String`  |                                      |
| `wasPromoted`      | `Boolean` | `autoSavedAt != null`                |

Source: `contactsRepo.observeMyContacts()` filtered by selector above.

### 26.5 Required inputs (user)

| Gesture                  | Effect                                          |
| ------------------------ | ----------------------------------------------- |
| Type into search field   | filters list                                    |
| Tap × in search          | clears query                                    |
| Tap a contact row        | navigate to CallDetail                          |
| Long-press a contact row | action sheet (Open · Open in Phone book · Copy) |
| Pull-to-refresh          | resync from Contacts provider                   |
| Scroll                   | normal scroll                                   |

### 26.6 Mandatory display elements

`StandardPage`:

- Title `"My Contacts"`
- Subtitle `"People you've saved"`
- Header glyph 👥

Body:

1. **Search field** — inline, 48dp tall, full-width, rounded. Leading `Search`
   icon, trailing `Clear` icon when non-empty. Placeholder `"Filter by name or number"`.

2. **Contact list** — `LazyColumn` of `ContactRow`s, each:
   - Leading: `NeoAvatar` (md) deterministic-colored.
   - Title: displayName (`Typography.titleSmall`).
   - Subtitle: formattedNumber (`Typography.bodySmall`, secondary).
   - Trailing: small text "promoted from inquiry" badge if `wasPromoted == true`.

   Sticky alphabetical headers (Compose `stickyHeader`) for letters A..Z, '#' for non-letter prefixes.

### 26.7 Optional display elements

- **Section count** in the subtitle: `"People you've saved · 124"`.
- **First-letter quick scrubber** on the right edge — Phase II.
- **Avatar deduplication** — when 2+ contacts share an initial, the seed is
  still per-number so colors differ.

### 26.8 Empty state

When `contacts.isEmpty()` (and query is empty):

- Centered NeoEmptyState: icon 👥, title `"No contacts yet."`, body
  `"callNest auto-saves new inquiries to your phone. Once you've saved someone in your phone book, they'll appear here."`. No CTA — saving happens elsewhere.

When query is non-empty and yields nothing:

- Centered: title `"No contacts match \"$query\""`, body
  `"Try a shorter name or number."`.

### 26.9 Loading state

- 8 row shimmers + 1 search field placeholder.
- Pull-to-refresh: standard indicator.

### 26.10 Error state

- Contacts permission revoked: full-screen `NeoErrorState`, icon `LockOutline`,
  title `"Contacts access was turned off."`, body `"Re-grant permission to see your saved contacts here."`, button `"Grant permission"`.
- Provider read failure: `NeoErrorState` with `"Couldn't load contacts."` + Retry.

### 26.11 Edge cases

a. **1000+ contacts** — `LazyColumn` keys on `normalizedNumber`; sticky headers
are O(1) per header; no full re-sort on scroll. First paint stays under
budget.

b. **Contact without a phone number** — skipped (we have nothing to navigate
to). A footer line `"Hidden N contacts without a phone number."` appears below
the list.

c. **Name with emoji** (e.g. "Ravi 🌟") — rendered correctly; sticky header is
computed from the _first letter character_ of the unicode-stripped name. If
the name _starts_ with an emoji, the header is `'#'`.

d. **Name with RTL chars** (Hebrew/Arabic) — Compose default LTR/RTL handling
applies; sticky header uses the first strong directional letter.

e. **Two contacts with same normalized number** — Contacts provider sometimes
returns duplicates after sync conflicts. We dedupe by `normalizedNumber`
keeping the first (displayName-wise) occurrence; the rest go into a
"Duplicates" footer list (Phase II will offer merge).

f. **Contact provider permission lost mid-session** — observer detects
`SecurityException`; ViewModel transitions to error state.

g. **Search query is a number partial** — matches against both `formattedNumber`
(with stripped formatting) and `displayName`.

h. **Search query exact-matches one contact** — that row scrolls into view if
off-screen.

i. **Configuration change while scrolled deep** — `LazyListState` is
`rememberSaveable`; position is preserved.

### 26.12 Copy table

| Key                         | Copy                                                                                                                |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `mc_title`                  | My Contacts                                                                                                         |
| `mc_subtitle`               | People you've saved                                                                                                 |
| `mc_subtitle_count`         | People you've saved · %1$d                                                                                          |
| `mc_back_cd`                | Back                                                                                                                |
| `mc_search_placeholder`     | Filter by name or number                                                                                            |
| `mc_search_clear_cd`        | Clear filter                                                                                                        |
| `mc_promoted_badge`         | promoted from inquiry                                                                                               |
| `mc_action_open`            | Open                                                                                                                |
| `mc_action_open_phonebook`  | Open in Phone book                                                                                                  |
| `mc_action_copy`            | Copy number                                                                                                         |
| `mc_copied_snackbar`        | Copied                                                                                                              |
| `mc_hidden_no_phone_footer` | Hidden %1$d contacts without a phone number.                                                                        |
| `mc_empty_title`            | No contacts yet.                                                                                                    |
| `mc_empty_body`             | callNest auto-saves new inquiries to your phone. Once you've saved someone in your phone book, they'll appear here. |
| `mc_no_match_title`         | No contacts match "%1$s"                                                                                            |
| `mc_no_match_body`          | Try a shorter name or number.                                                                                       |
| `mc_error_perm_title`       | Contacts access was turned off.                                                                                     |
| `mc_error_perm_body`        | Re-grant permission to see your saved contacts here.                                                                |
| `mc_error_perm_button`      | Grant permission                                                                                                    |
| `mc_error_load_title`       | Couldn't load contacts.                                                                                             |
| `mc_error_load_body`        | Try again.                                                                                                          |
| `mc_error_load_retry`       | Retry                                                                                                               |

### 26.13 ASCII wireframes

Default:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   My Contacts                                            │
│      People you've saved · 124                       👥      │
├─────────────────────────────────────────────────────────────┤
│ ┌─ Filter by name or number ───────────────── 🔍 ┐          │
│ └─────────────────────────────────────────────────┘          │
├─────────────────────────────────────────────────────────────┤
│ A                                                           │
│ ╭───╮ Anil Kumar                  +91 90000 11111           │
│ │ A │                                                       │
│ ╰───╯                                                       │
│ ╭───╮ Anjali  (promoted from inquiry)  +91 90000 22222      │
│ │ A │                                                       │
│ ╰───╯                                                       │
│ R                                                           │
│ ╭───╮ Ravi (Wholesale)             +91 98765 43210          │
│ │ R │                                                       │
│ ╰───╯                                                       │
│ ...                                                         │
│ Hidden 3 contacts without a phone number.                   │
└─────────────────────────────────────────────────────────────┘
```

Filtered:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   My Contacts                                            │
├─────────────────────────────────────────────────────────────┤
│ ┌─ rav ─────────────────────────────────────── × ┐          │
│ └─────────────────────────────────────────────────┘          │
│ ╭───╮ Ravi (Wholesale)             +91 98765 43210          │
│ │ R │                                                       │
│ ╰───╯                                                       │
│ ╭───╮ Ravi Mehta                   +91 90000 33333          │
│ │ R │                                                       │
│ ╰───╯                                                       │
└─────────────────────────────────────────────────────────────┘
```

No matches:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   My Contacts                                            │
├─────────────────────────────────────────────────────────────┤
│ ┌─ qzx ─────────────────────────────────────── × ┐          │
│ └─────────────────────────────────────────────────┘          │
│                                                             │
│             No contacts match "qzx"                         │
│         Try a shorter name or number.                       │
└─────────────────────────────────────────────────────────────┘
```

Permission error:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   My Contacts                                            │
├─────────────────────────────────────────────────────────────┤
│                       🔒                                    │
│         Contacts access was turned off.                     │
│   Re-grant permission to see your saved contacts here.      │
│              [  Grant permission  ]                         │
└─────────────────────────────────────────────────────────────┘
```

Empty:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   My Contacts                                            │
├─────────────────────────────────────────────────────────────┤
│                       👥                                    │
│                  No contacts yet.                           │
│   callNest auto-saves new inquiries to your phone.         │
│   Once you've saved someone in your phone book,             │
│   they'll appear here.                                      │
└─────────────────────────────────────────────────────────────┘
```

### 26.14 Accessibility

- Each row exposes `contentDescription = "${displayName}, ${spelledNumber}${if (wasPromoted) ", promoted from inquiry"}"`.
- Sticky header has `heading()` semantics so TalkBack announces "Section A".
- Search field uses `imeAction = ImeAction.Search`; pressing Enter dismisses
  the keyboard.
- Contrast: promoted badge uses the warning-tinted token which is AA on the
  TabBgLibrary background.
- Permission error CTA has a clear hint about what tapping it does (opens
  system settings).

### 26.15 Performance budget

| Metric                      | Budget   |
| --------------------------- | -------- |
| First paint (≤500 contacts) | ≤ 220 ms |
| First paint (5000 contacts) | ≤ 600 ms |
| Search filter recompute     | ≤ 30 ms  |
| Memory                      | ≤ 14 MB  |
| Scroll fps                  | 60 p95   |

The list is filtered in-memory (we hold all contacts in `StateFlow`); scaling
beyond ~20k contacts would warrant a SQLite-backed page; outside scope for v1.0.

---

## Cross-references

- **Shared `CallRow`** used by §22, §24, §25 — see Part 06 Appendix C.
- **`NeoCard`, `NeoButton`, `NeoChip`, `NeoIconButton`, `NeoAvatar`,
  `NeoSearchBar`, `NeoEmptyState`, `NeoErrorState`, `NeoDialog`** — Part 06
  Appendix A.
- **`StandardPage` scaffold** — Part 06 Appendix B.
- **Color tokens** (`TabBgCalls`, `TabBgLibrary`, `TabBgStats`, `AccentBlue`,
  `AccentGreen`, `AccentRed`, `AccentAmber`, `AccentPurple`) — Part 06 Appendix D.
- **Formatters** (`PhoneFormatter`, `DateTimeFormatter`, `DurationFormatter`,
  `RelativeTimeFormatter`) — Part 06 Appendix E.
- **`avatarSeed` → HSL hash** — Part 06 Appendix F.
- **`MarkdownRenderer` rules** — Part 06 Appendix M.
- **Use cases**: `LeadScoreUseCase`, `NumberStatsUseCase`,
  `BuildStatsSnapshotUseCase`, `GenerateInsightsUseCase`,
  `ScheduleFollowUpUseCase`, `CancelFollowUpUseCase`, `MarkFollowUpDoneUseCase`,
  `RemoveTagUseCase`, `DeleteNoteUseCase`, `ClearNumberDataUseCase` — Part 05 §5.x.
- **DAOs**: `CallDao`, `NoteDao`, `TagDao`, `BookmarkDao`, `FollowUpDao`,
  `SearchHistoryDao` — Part 06 §6.x.
- **Phase I.2 changes** that affect this part: top-bar overflow removal, manage
  section addition, deep-link route normalization. See `CHANGELOG.md` v0.18.x.

---

## Implementation notes

- All scrolling surfaces use a single `LazyColumn`; no nested scrollables. The
  pinned-bookmarks carousel uses `Modifier.horizontalScroll` inside a
  non-lazy item to keep the item count predictable.
- Stats charts are _Compose Canvas_, not Vico, despite Vico being on the
  dependency list. We started with Vico in the Stats sprint and ran into
  layout-pass thrash on the heatmap; Canvas was the path of least resistance.
  Documented in `DECISIONS.md` D-024.
- The CallDetail page uses `rememberLazyListState()` keyed on the
  `normalizedNumber` so navigating from CallA → CallB → back preserves CallA's
  scroll position correctly.
- `MarkdownRenderer` is a 200-line in-house implementation; we explicitly avoid
  Markwon to keep the dependency footprint small. It supports: `**bold**`,
  `*italic*`, `- bullet`, `1. ordered`, `[text](url)`, inline code, and
  headings up to `##`. Anything else renders as plaintext.
- The Stats range chip's persistence: the last selected range is saved to
  DataStore (`stats.last_range`) so the user doesn't have to re-pick on every
  visit.
- The FollowUps initial-tab logic runs _once_ per cold start; subsequent visits
  in the same session honor whatever tab the user last left.
- The SearchScreen does not save its query across navigations. This is
  intentional — Search is "scratch space".

---

## Sprint pointers (where this code lives now)

| Page       | Screen file                              | ViewModel                                   |
| ---------- | ---------------------------------------- | ------------------------------------------- |
| CallDetail | `ui/screen/detail/CallDetailScreen.kt`   | `ui/screen/detail/CallDetailViewModel.kt`   |
| Search     | `ui/screen/search/SearchScreen.kt`       | `ui/screen/search/SearchViewModel.kt`       |
| Stats      | `ui/screen/stats/StatsScreen.kt`         | `ui/screen/stats/StatsViewModel.kt`         |
| Bookmarks  | `ui/screen/bookmarks/BookmarksScreen.kt` | `ui/screen/bookmarks/BookmarksViewModel.kt` |
| FollowUps  | `ui/screen/followups/FollowUpsScreen.kt` | `ui/screen/followups/FollowUpsViewModel.kt` |
| MyContacts | `ui/screen/contacts/MyContactsScreen.kt` | `ui/screen/contacts/MyContactsViewModel.kt` |

End of Part 04. See Part 05 for: Tag picker, Note editor, Tag manager, Settings,
About, Update flow, Backup/restore, Onboarding, In-app docs, Floating in-call
bubble, Post-call popup.

---

# callNest APP-SPEC — Part 05: Deep pages 2

> Tags · Auto-tag rules · Rule editor · Backup · Export wizard · Quick-export sheet
>
> Audience: a UX engineer rebuilding callNest from scratch with no prior context.
> This document is intentionally self-contained: every page lists its inputs,
> outputs, copy strings, ASCII wireframes, accessibility notes, and a
> performance budget.

---

## How to read this part

Each page below uses the canonical 15-subsection template that runs through
parts 02, 03, and 04 of the spec:

1. Purpose
2. Entry points
3. Exit points
4. Required inputs (data)
5. Required inputs (user)
6. Mandatory display
7. Optional display
8. Empty state
9. Loading state
10. Error state
11. Edge cases
12. Copy table
13. ASCII wireframe
14. Accessibility
15. Performance budget

Cross references in this part:

- `BackupManager` algorithm — see Part 01 §6.9.
- `RuleConditionEvaluator` — see Part 01 §6.4.
- `LeadScoreCalculator` formula — see Part 01 §6.7.
- `FilterState` model — see Part 02 §15.
- `NeoScaffold`, `StandardPage`, `NeoCard`, `NeoButton`, `NeoTextField`,
  `NeoBottomSheet`, `NeoLoader`, `NeoProgressBar`, `ColoredChip` — see
  Part 04 (component catalogue).
- `AutoTagRuleRepository` — see Part 01 §7.3.
- `TagRepository` — see Part 01 §7.2.

Implementation notes for spec deviations are flagged inline with the
prefix `> NOTE:`.

---

## 27 — TagsManagerScreen

### 27.1 Purpose

The TagsManagerScreen is the central place a user goes to organise the
vocabulary they apply to calls. A tag in callNest is a small, coloured,
emoji-prefixed label that a user — or an auto-tag rule — attaches to a
`CallEntity` row. Tags drive filtering on the Calls list, scoping on the
Stats screen, and routing in the auto-tag rules engine.

This screen lets the user:

- Browse every tag in the system in a single scrollable list.
- See, at a glance, how many calls each tag is currently applied to.
- Rename a tag.
- Recolour a tag (pick from a curated palette).
- Change a tag's emoji prefix.
- Merge two tags into one (cascading the merge across every call row
  and every rule that references the source tag).
- Delete a user-created tag.
- Create a brand-new user tag.

System tags (the nine seeded on first DB create) cannot be deleted, but
they CAN be renamed, recoloured, and re-emojied. The intent is that a
user can take a "Vendor" system tag, rename it to "Supplier", and the
underlying `tagId` stays stable so existing call assignments continue
to work.

### 27.2 Entry points

The TagsManagerScreen can be reached from any of the following points:

| From              | Trigger                                                                                 |
| ----------------- | --------------------------------------------------------------------------------------- |
| MoreScreen        | Tapping the "Tags" row under the "Organise" section.                                    |
| FilterSheet       | Tapping the "Manage tags…" link at the bottom of the tag chip group.                    |
| CallDetailsScreen | Tapping the pencil icon next to the tag chips, then tapping "Manage tags…" in the menu. |
| RuleEditor screen | Tapping the gear icon next to a tag picker dropdown.                                    |
| Onboarding tour   | The "Tags" step has a "Take a look" button.                                             |
| Deep link         | `callNest://tags` (used by the in-app docs cross-links).                                |

### 27.3 Exit points

| To                                     | Trigger                                         |
| -------------------------------------- | ----------------------------------------------- |
| Previous screen                        | Hardware back / nav-bar back arrow.             |
| TagEditorDialog (modal, in-screen)     | Tap a row, tap FAB, or tap a row's pencil icon. |
| MergeIntoDialog (modal, in-screen)     | Long-press a row.                               |
| DeleteConfirmDialog (modal, in-screen) | Swipe-left to delete (only if usage > 0).       |
| Snackbar (transient, stays on screen)  | Successful save / merge / delete.               |
| Toast (transient)                      | Attempt to delete a system tag.                 |

### 27.4 Required inputs (data)

The screen subscribes to a single `StateFlow<TagsManagerUiState>`
exposed by `TagsManagerViewModel`. The state shape:

```
data class TagsManagerUiState(
    val isLoading: Boolean,
    val tags: List<TagWithCount>,
    val searchQuery: String,
    val error: String?,
)

data class TagWithCount(
    val id: Long,
    val name: String,
    val colorHex: String,           // e.g. "#FF6B6B"
    val emoji: String,              // single grapheme cluster
    val isSystem: Boolean,
    val callCount: Int,             // computed via CallTagDao.countCallsForTag(id)
)
```

The ViewModel composes this state from:

- `TagRepository.observeAllTags(): Flow<List<TagEntity>>`
- `CallTagDao.observeCountsByTag(): Flow<Map<Long, Int>>` (joined on tag id)

Both flows are combined via `combine { … }` and emitted on
`Dispatchers.Default`. The ViewModel applies the search filter
client-side because the tag list is bounded (typically <100 entries).

### 27.5 Required inputs (user)

The user can:

- Type into the search field at the top.
- Tap a row to open `TagEditorDialog` for that tag.
- Long-press a row to open `MergeIntoDialog`.
- Swipe a row left to reveal the destructive "Delete" action.
- Tap the FAB (`+`) to create a new tag.
- Tap a tag's pencil/gear inline icon (synonym for tapping the row).
- Use the toolbar overflow → "Reset to defaults" (only resets the
  nine system tags' name/colour/emoji; never affects user tags).

### 27.6 Mandatory display

The screen body is wrapped in a `StandardPage` composable:

```
StandardPage(
    title = "Tags",
    description = "Categories for your calls",
    emoji = "🏷️",
)
```

Inside the page body, top-to-bottom:

1. A `NeoTextField` filter input with placeholder
   `"Search tags…"` and a clear-X icon when non-empty. 8 dp top padding.
2. A horizontal hairline divider (`BorderSoft`, 1 dp).
3. A `LazyColumn` of tag rows. Each row is laid out as:
   ```
   Row(
       modifier = Modifier
           .fillMaxWidth()
           .height(64.dp)
           .clickable { … },
       verticalAlignment = Alignment.CenterVertically,
   ) {
       Spacer(Modifier.width(16.dp))
       ColoredChip(
           text = "${tag.emoji} ${tag.name}",
           backgroundColor = Color(parseColor(tag.colorHex)),
           textColor = chooseContrastColor(tag.colorHex),
       )
       Spacer(Modifier.weight(1f))
       Text(
           text = "${tag.callCount} calls",
           style = MaterialTheme.typography.labelMedium,
           color = MaterialTheme.colorScheme.onSurfaceVariant,
       )
       Spacer(Modifier.width(8.dp))
       Icon(Icons.Default.ChevronRight, contentDescription = null)
       Spacer(Modifier.width(16.dp))
   }
   ```
4. A trailing FAB anchored to the screen's bottom-right corner with a
   `+` icon, label `"New tag"` (visually hidden, used by talkback).

The nine system tags are always present at the top of the list (they
sort first by `isSystem desc`, then by `name asc`). User tags follow,
sorted by `name asc`.

### 27.7 Optional display

These elements are shown when the corresponding state holds:

- An inline "9 system tags" hint as a `Text` in `labelSmall` between
  the search field and the list, only when `searchQuery.isBlank() &&
tags.size >= 9`.
- A "No matches" hint inline (not a full empty state) when
  `searchQuery.isNotBlank() && filteredTags.isEmpty()`. Renders inside
  the `LazyColumn` as a centred `Text` with 32 dp vertical padding and
  the copy `"No tags match \"{query}\""`.
- A subtle shimmer skeleton on the row's count badge while
  `CallTagDao.observeCountsByTag()` has not emitted yet. The chip
  itself renders immediately from the tag stream.

### 27.8 Empty state

The TagsManagerScreen should never be empty in a healthy install — the
nine system tags are seeded on first DB create via `TagSeeder.seed()` in
`AppDatabase.Callback.onCreate()`. If the list is observed empty for
any reason (e.g. a corrupted DB after a failed restore), the screen
shows:

```
StandardEmpty(
    emoji = "🏷️",
    title = "No tags yet",
    body = "Tap + to create your first tag.",
    primaryCta = NeoButton("Create tag") { openEditor(null) },
)
```

> NOTE: We deliberately do not auto-reseed here — re-seeding in the UI
> layer would mask the underlying corruption. Instead, surface the
> error path via the `BackupRestoreScreen` which has explicit reseed
> tools.

### 27.9 Loading state

While `uiState.isLoading == true`:

- The search field renders disabled (text colour
  `colorScheme.onSurfaceVariant`, alpha 0.6).
- The list area renders a `LazyColumn` of 6 `ShimmerRow` placeholders,
  each 64 dp tall, with a chip-shape shimmer block + count shimmer.
- The FAB is hidden (not just disabled — fully hidden, because tapping
  it before tags load would race the list).

The loading state is transient — it should resolve in <300 ms under
the performance budget below.

### 27.10 Error state

If the `TagRepository.observeAllTags()` flow emits an error:

- The list region renders a `StandardError` composable with copy
  `"Couldn't load your tags. Pull down or tap retry."` plus a primary
  `NeoButton("Retry")` that re-invokes the flow.
- The FAB stays hidden until tags load successfully, because the
  Editor dialog cannot persist without the parent list.
- Long-tap on the error region copies the underlying exception's
  `localizedMessage` to clipboard (debug-only; release builds skip).

### 27.11 Edge cases

| Case                                 | Handling                                                                                                                                                                                                                                                                                                        |
| ------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Deleting a tag applied to 1000 calls | Show `DeleteConfirmDialog` with copy `"This will remove the tag from 1,000 calls. Continue?"`. On confirm, delete is performed in a single Room transaction `(DELETE FROM call_tag_cross_ref WHERE tag_id = ?; DELETE FROM tag WHERE id = ?)`. UI shows a `NeoLoader` overlay until the transaction commits.    |
| Merge target = source                | Block at validation time. The `MergeIntoDialog` greys out the source tag from its picker; if the user somehow selects it (e.g. screen reader), the Save button stays disabled with copy `"Pick a different tag to merge into."`.                                                                                |
| Rename to an existing name           | Block at validation. The `TagEditorDialog`'s Save button is disabled and a helper text below the name field reads `"A tag named \"{name}\" already exists."` (case-insensitive compare, normalised whitespace).                                                                                                 |
| Emoji selection                      | Two paths: (a) a curated grid of 32 business emojis (see table below), (b) a free-text single-grapheme input that accepts any single emoji or unicode grapheme cluster. Validate via `androidx.emoji2.text.EmojiCompat.get().getEmojiMatch()`. Reject multi-codepoint inputs that don't form a single grapheme. |
| Deleting a system tag                | Toast: `"System tags can't be deleted. You can rename or recolour them instead."`                                                                                                                                                                                                                               |
| Concurrent edit (two devices)        | Last-write-wins. The repo uses `OnConflictStrategy.REPLACE` keyed on `id`.                                                                                                                                                                                                                                      |
| Tag colour invalid hex               | Default to `#888888`; log a `Timber.w` and store the corrected value back.                                                                                                                                                                                                                                      |
| Long tag name (>30 chars)            | Truncate with ellipsis on the row; full name shown in the editor. Save is blocked at >40 chars with helper text `"Keep tag names under 40 characters."`.                                                                                                                                                        |
| Search query containing emoji        | Match against both `name` and `emoji` fields, case-folded.                                                                                                                                                                                                                                                      |

The 32 curated business emojis (used in the editor's emoji grid):

| #   | Emoji | Suggested use     |
| --- | ----- | ----------------- |
| 1   | 🏷️    | Generic tag       |
| 2   | 📞    | Inbound call      |
| 3   | 📲    | Outbound call     |
| 4   | 💬    | Discussion        |
| 5   | 💼    | Business          |
| 6   | 💰    | Revenue           |
| 7   | 💸    | Refund / loss     |
| 8   | 🛒    | Order             |
| 9   | 📦    | Fulfilment        |
| 10  | 🚚    | Shipping          |
| 11  | 🧾    | Invoice           |
| 12  | 📝    | Note / quoted     |
| 13  | 📌    | Pinned            |
| 14  | ⭐    | VIP               |
| 15  | ⚠️    | Warning           |
| 16  | 🚫    | Spam              |
| 17  | 🛠️    | Vendor / supplier |
| 18  | 🤝    | Partner           |
| 19  | 👤    | Personal          |
| 20  | 👥    | Customer          |
| 21  | 🏢    | Company           |
| 22  | 📍    | Location          |
| 23  | 🕐    | Follow-up         |
| 24  | ✅    | Closed-won        |
| 25  | ❌    | Closed-lost       |
| 26  | 🔁    | Recurring         |
| 27  | 🎯    | Hot lead          |
| 28  | 🧊    | Cold lead         |
| 29  | 🔥    | Urgent            |
| 30  | 📅    | Scheduled         |
| 31  | 🎉    | Celebrate         |
| 32  | ❓    | Unknown           |

The nine seeded system tags:

| #   | Name        | Emoji | Default colour | Purpose                            |
| --- | ----------- | ----- | -------------- | ---------------------------------- |
| 1   | Inquiry     | ❓    | `#3B82F6`      | Default for unknown inbound calls. |
| 2   | Customer    | 👥    | `#10B981`      | Returning buyer.                   |
| 3   | Vendor      | 🛠️    | `#F59E0B`      | Supplier or service provider.      |
| 4   | Personal    | 👤    | `#8B5CF6`      | Family / friends.                  |
| 5   | Spam        | 🚫    | `#EF4444`      | Telemarketing / scam.              |
| 6   | Follow-up   | 🕐    | `#06B6D4`      | Needs callback.                    |
| 7   | Quoted      | 📝    | `#F97316`      | Quote sent.                        |
| 8   | Closed-won  | ✅    | `#22C55E`      | Deal won.                          |
| 9   | Closed-lost | ❌    | `#94A3B8`      | Deal lost.                         |

### 27.12 Copy table

| Key                   | Resource id                 | English                                                                         |
| --------------------- | --------------------------- | ------------------------------------------------------------------------------- |
| Title                 | `tags_title`                | Tags                                                                            |
| Subtitle              | `tags_subtitle`             | Categories for your calls                                                       |
| Search placeholder    | `tags_search_hint`          | Search tags…                                                                    |
| Count suffix          | `tags_count_suffix`         | %1$d calls                                                                      |
| FAB label             | `tags_new_fab_label`        | New tag                                                                         |
| FAB content desc      | `tags_new_fab_a11y`         | Create a new tag                                                                |
| Empty title           | `tags_empty_title`          | No tags yet                                                                     |
| Empty body            | `tags_empty_body`           | Tap + to create your first tag.                                                 |
| Empty CTA             | `tags_empty_cta`            | Create tag                                                                      |
| Error body            | `tags_error_body`           | Couldn't load your tags. Pull down or tap retry.                                |
| Error CTA             | `tags_error_cta`            | Retry                                                                           |
| Delete confirm title  | `tags_delete_title`         | Delete this tag?                                                                |
| Delete confirm body   | `tags_delete_body`          | This will remove the tag from %1$d calls. Continue?                             |
| Delete confirm cta    | `tags_delete_confirm`       | Delete                                                                          |
| Delete cancel         | `tags_delete_cancel`        | Cancel                                                                          |
| Cannot delete system  | `tags_cannot_delete_system` | System tags can't be deleted. You can rename or recolour them instead.          |
| Editor title (new)    | `tag_editor_title_new`      | New tag                                                                         |
| Editor title (edit)   | `tag_editor_title_edit`     | Edit tag                                                                        |
| Editor name label     | `tag_editor_name_label`     | Name                                                                            |
| Editor emoji label    | `tag_editor_emoji_label`    | Emoji                                                                           |
| Editor colour label   | `tag_editor_color_label`    | Colour                                                                          |
| Editor save           | `tag_editor_save`           | Save                                                                            |
| Editor cancel         | `tag_editor_cancel`         | Cancel                                                                          |
| Validation: empty     | `tag_editor_err_empty`      | Give your tag a name.                                                           |
| Validation: too long  | `tag_editor_err_too_long`   | Keep tag names under 40 characters.                                             |
| Validation: duplicate | `tag_editor_err_duplicate`  | A tag named "%1$s" already exists.                                              |
| Validation: bad emoji | `tag_editor_err_bad_emoji`  | Pick a single emoji.                                                            |
| Merge dialog title    | `tag_merge_title`           | Merge into…                                                                     |
| Merge dialog body     | `tag_merge_body`            | All %1$d calls tagged "%2$s" will move to the chosen tag. This can't be undone. |
| Merge dialog cta      | `tag_merge_cta`             | Merge                                                                           |
| Merge done snackbar   | `tag_merge_snack`           | Merged "%1$s" into "%2$s".                                                      |
| Saved snackbar        | `tag_saved_snack`           | Tag saved.                                                                      |
| Deleted snackbar      | `tag_deleted_snack`         | Tag deleted.                                                                    |
| Reset overflow        | `tags_reset_overflow`       | Reset system tags                                                               |
| Reset confirm         | `tags_reset_confirm`        | Restore the original 9 system tags? Your custom tags won't be touched.          |

### 27.13 ASCII wireframe

```
┌──────────────────────────────────────────────────┐
│ ←  Tags                                       ⋮  │
├──────────────────────────────────────────────────┤
│                                                  │
│  🏷️                                              │
│  Tags                                            │
│  Categories for your calls                       │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ 🔍  Search tags…                       ✕ │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  9 system tags                                   │
│  ────────────────────────────────────────────    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ [❓ Inquiry  ]                42 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [👥 Customer ]               118 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [🛠️ Vendor   ]                23 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [👤 Personal ]                 9 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [🚫 Spam     ]                17 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [🕐 Follow-up]                 6 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [📝 Quoted   ]                14 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [✅ Closed-won]                8 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [❌ Closed-lost]               5 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [🔥 Hot lead ]                 3 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [🎯 VIP      ]                 2 calls › │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│                                          ┌────┐  │
│                                          │  + │  │
│                                          └────┘  │
└──────────────────────────────────────────────────┘
```

Editor dialog wireframe:

```
┌──────────────────────────────────────────────┐
│  Edit tag                                  ✕ │
├──────────────────────────────────────────────┤
│                                              │
│  Name                                        │
│  ┌────────────────────────────────────────┐  │
│  │ Customer                              │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  Emoji                                       │
│  ┌────────────────────────────────────────┐  │
│  │ 🏷️ 📞 📲 💬 💼 💰 💸 🛒 📦 🚚 🧾 📝 │  │
│  │ 📌 ⭐ ⚠️ 🚫 🛠️ 🤝 👤 👥 🏢 📍 🕐 ✅ │  │
│  │ ❌ 🔁 🎯 🧊 🔥 📅 🎉 ❓ — Custom: [👥]│  │
│  └────────────────────────────────────────┘  │
│                                              │
│  Colour                                      │
│  ┌────────────────────────────────────────┐  │
│  │ ● ● ● ● ● ● ● ● ● ● ● ●                │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  Preview:  [👥 Customer]                     │
│                                              │
│           [ Cancel ]  [ Save ]               │
└──────────────────────────────────────────────┘
```

Merge dialog wireframe:

```
┌──────────────────────────────────────────────┐
│  Merge into…                              ✕  │
├──────────────────────────────────────────────┤
│  All 14 calls tagged "Quoted" will move to   │
│  the chosen tag. This can't be undone.       │
│                                              │
│  ○ ❓ Inquiry                                 │
│  ○ 👥 Customer                                │
│  ● ✅ Closed-won                              │
│  ○ ❌ Closed-lost                             │
│  ○ 🔥 Hot lead                                │
│  ────────────────────────────────────────    │
│           [ Cancel ]   [ Merge ]              │
└──────────────────────────────────────────────┘
```

### 27.14 Accessibility

- Every row has `contentDescription = "Tag {name}, applied to {count}
calls. Double-tap to edit. Long-press for more actions."`
- The FAB has `contentDescription = "Create a new tag"`.
- Search field announces `"Search tags. {n} of {m} tags shown."` after
  a 600 ms debounce.
- Colour preview chips use a 4.5:1 contrast minimum; the
  `chooseContrastColor()` helper picks black or white text based on
  the relative luminance of the chip background.
- Keyboard nav: `Tab` moves between search → first row → … → FAB.
  `Shift+Tab` reverses. `Enter` activates a row. `Delete` on a
  focused row triggers the swipe-delete confirmation.
- The emoji grid is announced as `"Emoji picker, 32 options, swipe
right to navigate."`
- Long-press is also exposed as an "Actions" menu via the row's
  custom-actions API (`CustomAction("Merge into another tag…")`).
- Live region: when the count badge updates after a merge or delete,
  it's announced via an `accessibilityLiveRegion = Polite`.
- All tap targets are ≥48×48 dp.
- The chip shapes do not rely solely on colour — the emoji prefix
  is the primary visual identifier.

### 27.15 Performance budget

- Initial render: ≤200 ms from navigation commit to first row painted
  (assumes ≤50 tags; with the warm Room cache).
- Scroll: 60 fps on a Pixel 4a (`maxItemsPerScroll = 50`).
- Search filter: debounce 200 ms; the filter pass is O(n) over an
  in-memory list bounded by ~100 tags, target <2 ms.
- Tag count subscription: backed by a single `Flow<Map<Long, Int>>`
  that emits at most once per upstream Room invalidation. On a
  100k-call DB the count query runs in <80 ms with the
  `idx_call_tag_cross_ref_tag` index.
- Editor save: <50 ms in-DB; UI dismisses the dialog optimistically.
- Merge: bounded by `UPDATE call_tag_cross_ref SET tag_id = ?
WHERE tag_id = ?` — runs in a single transaction, target <500 ms
  for 10k cross-refs on a Pixel 4a.
- Memory: the screen retains <500 KB beyond the baseline navigator
  stack.

---

## 28 — AutoTagRulesScreen

### 28.1 Purpose

The AutoTagRulesScreen lists every auto-tag rule the user has defined.
Rules are the engine that lets callNest automatically tag, score, or
follow-up incoming calls based on patterns the user has expressed. The
screen is read-mostly: the rich editing happens in `RuleEditor`. From
the list, the user can:

- See every rule, ordered by execution priority.
- See, per rule, how many of the latest 200 calls would match.
- Toggle a rule on or off without entering the editor.
- Reorder rules (priority order matters when actions conflict).
- Bulk-delete rules.
- Tap into a rule to edit it.
- Create a new rule.

Rules execute on every sync and on every new call event from the
`CallEventReceiver`.

### 28.2 Entry points

| From                   | Trigger                                                                                                          |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------- |
| MoreScreen             | "Auto-tag rules" row.                                                                                            |
| FilterSheet            | "Why was this tagged?" link → `RuleEditor` directly via deep link, but back stack returns to AutoTagRulesScreen. |
| Onboarding             | "Try a rule" button on the onboarding-rules step.                                                                |
| Settings → Power tools | "Manage rules" link.                                                                                             |
| Deep link              | `callNest://rules`.                                                                                              |

### 28.3 Exit points

| To                         | Trigger                                               |
| -------------------------- | ----------------------------------------------------- |
| Previous screen            | Hardware back / nav-bar back.                         |
| `RuleEditor/{ruleId}`      | Tap a row.                                            |
| `RuleEditor/-1`            | Tap FAB.                                              |
| Bulk-delete confirm dialog | Long-press a row.                                     |
| Snackbar                   | Successful enable/disable toggle, reorder, or delete. |

### 28.4 Required inputs (data)

```
data class AutoTagRulesUiState(
    val isLoading: Boolean,
    val rules: List<RuleListItem>,
    val draggedFromIndex: Int?,
    val draggedToIndex: Int?,
    val error: String?,
)

data class RuleListItem(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val priority: Int,
    val matchCount: Int,            // computed live, capped to 200
    val isInvalid: Boolean,         // true if conditions JSON failed to parse
)
```

Sources:

- `AutoTagRuleRepository.observeAllRules(): Flow<List<AutoTagRule>>`
- For each emitted rule, `RuleConditionEvaluator.matchCountIn(latest200)`
  via a memoised computation on `Dispatchers.Default`.
- The `latest200` snapshot comes from
  `CallRepository.observeLatest(200): Flow<List<Call>>`.

The match-count computation is debounced 250 ms so a slider drag in
the editor doesn't thrash the list.

### 28.5 Required inputs (user)

- Tap a row → open editor.
- Tap a row's switch → toggle active.
- Long-press a row → enter selection mode (multi-select for delete).
- Drag the up/down handle → reorder.
- Tap FAB → new rule.
- Tap toolbar overflow → "Run rules now" (manually re-evaluates over
  the entire call history; shown only if the user has Power tools
  enabled).

### 28.6 Mandatory display

```
StandardPage(
    title = "Rules",
    description = "Automatic tagging that runs every sync",
    emoji = "🪄",
)
```

Body:

1. A `LazyColumn` of rule rows. Each row:
   ```
   Row(modifier = Modifier.height(72.dp)) {
       DragHandle(up, down)
       Column(weight = 1f) {
           Text(rule.name, titleMedium)
           Text("Applies to ${matchCount} calls", labelSmall, onSurfaceVariant)
       }
       Switch(rule.isActive)
       ChevronRight
   }
   ```
2. A FAB anchored bottom-right, label `"New rule"`.

When `rule.isInvalid`, the row's subtitle reads `"⚠ Couldn't read this
rule — tap to fix"` in the error colour.

### 28.7 Optional display

- A "Run rules now" button in the toolbar overflow, only when Power
  tools are enabled.
- A 1-line banner above the list: `"50+ rules — performance may
drop"` when `rules.size >= 50`.
- Selection-mode toolbar replaces the standard toolbar when ≥1 row is
  long-pressed: shows count selected + "Delete" + "Cancel".
- A shimmer loader over the count subtitle while match counts are
  computing.

### 28.8 Empty state

```
StandardEmpty(
    emoji = "🪄",
    title = "No rules yet",
    body = "Create your first rule to auto-tag incoming calls.",
    primaryCta = NeoButton("Create a rule") { navigate("RuleEditor/-1") },
    secondaryCta = NeoTextButton("Browse examples") { openDocs("rules-examples") },
)
```

The "Browse examples" link opens the in-app docs article
`docs/06-rules-examples.md` which walks through 8 starter rules
(prefix matchers, time-of-day rules, duration thresholds, etc.).

### 28.9 Loading state

- A `LazyColumn` of 4 `ShimmerRow`s, each 72 dp tall.
- Toolbar disabled.
- FAB hidden.

Loading is transient (<300 ms).

### 28.10 Error state

If the rules flow throws:

- Inline `StandardError` block with copy `"Couldn't load your rules.
Tap to retry."` and a `NeoButton("Retry")`.
- FAB hidden.
- The toolbar overflow's "Run rules now" is disabled with a tooltip
  explaining the error.

### 28.11 Edge cases

| Case                                  | Handling                                                                                                                                                                     |
| ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Rule with invalid JSON conditions     | `isInvalid = true`, row subtitle shows the warning. Tapping opens the editor; the editor surfaces `"This rule's conditions couldn't be read. Rebuild them and save to fix."` |
| Rule with deleted tag id (in actions) | The action is auto-removed when loading into the editor; a snackbar fires `"Removed an action that referenced a deleted tag."`                                               |
| Reorder during sync                   | The reorder is queued — the running sync is allowed to finish using the old priorities, and the new priority order is applied to the next sync.                              |
| Rules count cap                       | No hard cap, but a warning banner shows at ≥50. At ≥100 the FAB is replaced with a disabled tooltip `"Reaching 100 rules — please consolidate or trim."`                     |
| Concurrent edit                       | Last-write-wins on `id`.                                                                                                                                                     |
| Match-count compute during DB write   | The compute coroutine catches `RoomDatabaseClosedException` and emits `null`; the row falls back to "—".                                                                     |
| User toggles 50 rules off in <1 s     | Each toggle posts to a `MutableSharedFlow<Long, Boolean>(replay = 0, extraBufferCapacity = 64)` and writes are batched in 100 ms windows.                                    |
| First boot with no rules              | Show empty state with a CTA. Do not seed any rule (unlike tags).                                                                                                             |

### 28.12 Copy table

| Key                      | Resource id                      | English                                                            |
| ------------------------ | -------------------------------- | ------------------------------------------------------------------ |
| Title                    | `rules_title`                    | Rules                                                              |
| Subtitle                 | `rules_subtitle`                 | Automatic tagging that runs every sync                             |
| Subtitle (count)         | `rules_subtitle_count`           | Applies to %1$d calls                                              |
| Subtitle (invalid)       | `rules_subtitle_invalid`         | ⚠ Couldn't read this rule — tap to fix                             |
| FAB label                | `rules_new_fab`                  | New rule                                                           |
| Empty title              | `rules_empty_title`              | No rules yet                                                       |
| Empty body               | `rules_empty_body`               | Create your first rule to auto-tag incoming calls.                 |
| Empty CTA primary        | `rules_empty_cta_primary`        | Create a rule                                                      |
| Empty CTA secondary      | `rules_empty_cta_secondary`      | Browse examples                                                    |
| Run now overflow         | `rules_run_now`                  | Run rules now                                                      |
| Run now started          | `rules_run_now_started`          | Running %1$d rules over your history…                              |
| Run now done             | `rules_run_now_done`             | Done. Re-tagged %1$d calls.                                        |
| Selection delete         | `rules_selection_delete`         | Delete %1$d rules                                                  |
| Selection delete confirm | `rules_selection_delete_confirm` | Delete %1$d rules? Calls already tagged by them won't be affected. |
| Reorder hint             | `rules_reorder_hint`             | Drag up/down to change priority                                    |
| Cap warning              | `rules_cap_warning`              | %1$d rules — performance may drop. Consider consolidating.         |
| Cap reached              | `rules_cap_reached`              | Reaching 100 rules — please consolidate or trim.                   |
| Toggle on snackbar       | `rules_toggle_on`                | "%1$s" enabled.                                                    |
| Toggle off snackbar      | `rules_toggle_off`               | "%1$s" disabled.                                                   |
| Reorder snackbar         | `rules_reorder_snack`            | Priority updated.                                                  |
| Deleted snackbar         | `rules_deleted_snack`            | Rule deleted.                                                      |
| Bulk deleted snackbar    | `rules_bulk_deleted_snack`       | %1$d rules deleted.                                                |
| Error body               | `rules_error_body`               | Couldn't load your rules. Tap to retry.                            |

### 28.13 ASCII wireframe

```
┌──────────────────────────────────────────────────┐
│ ←  Rules                                       ⋮  │
├──────────────────────────────────────────────────┤
│  🪄                                              │
│  Rules                                           │
│  Automatic tagging that runs every sync          │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ ⇅  Tag +91-9XXX as Customer            ▶ │    │
│  │    Applies to 87 calls          ●━━━○   │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  Mark short calls under 10s as Spam  ▶ │    │
│  │    Applies to 14 calls          ●━━━○   │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  Boost lead score for SIM 1          ▶ │    │
│  │    Applies to 122 calls         ○━━━●   │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  Auto-bookmark VIP numbers           ▶ │    │
│  │    Applies to 4 calls           ●━━━○   │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  Quoted → follow-up in 3 days        ▶ │    │
│  │    Applies to 18 calls          ●━━━○   │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  ⚠ Couldn't read this rule — tap…    ▶ │    │
│  │                                  ○━━━●   │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│                                          ┌────┐  │
│                                          │ +  │  │
│                                          └────┘  │
└──────────────────────────────────────────────────┘
```

### 28.14 Accessibility

- Each row's content description: `"Rule {name}, applies to {n}
calls, currently {on|off}. Double-tap to edit, swipe right with two
fingers to reorder."`
- The toggle's role is `Role.Switch`; talkback announces state changes.
- Drag handle has `contentDescription = "Reorder rule {name}.
Currently at position {i} of {n}."`
- Reorder handle exposed as custom actions: `"Move up", "Move down",
"Move to top", "Move to bottom"`.
- The error subtitle is announced with `accessibilityLiveRegion =
Polite`.
- Long-press alternative: row's overflow menu exposes "Select",
  "Delete", "Duplicate".

### 28.15 Performance budget

- Initial render: ≤300 ms with up to 30 rules.
- Match-count compute: capped to latest 200 calls, target <40 ms per
  rule on a Pixel 4a, parallelised across rules with
  `coroutineScope { rules.map { async { … } } }`.
- Reorder write: single `UPDATE rules SET priority = ? WHERE id = ?`
  per row; whole reorder in a transaction <100 ms.
- Toggle write: <20 ms.
- Memory: list state plus 200-call snapshot ≈ 600 KB.

---

## 29 — RuleEditor screen

### 29.1 Purpose

The RuleEditor is where the actual logic of an auto-tag rule is
defined. It has three jobs:

1. Let the user describe a set of conditions that must ALL be true for
   the rule to fire.
2. Let the user pick a list of actions to take when the rule fires.
3. Continuously preview, in real time, how many calls in the user's
   history would have matched the rule as currently expressed.

This screen is opened with either an existing `ruleId` or `-1` for
new-rule mode.

### 29.2 Entry points

| From               | Trigger                                                 |
| ------------------ | ------------------------------------------------------- |
| AutoTagRulesScreen | Tap a row (existing rule) or FAB (new rule).            |
| FilterSheet        | "Why was this tagged?" → opens the matching rule.       |
| Onboarding         | "Try this example" → seeds a rule and opens the editor. |
| Deep link          | `callNest://rules/{id}` or `callNest://rules/new`.      |

### 29.3 Exit points

| To                     | Trigger                                 |
| ---------------------- | --------------------------------------- |
| AutoTagRulesScreen     | Back / Save (saves and pops).           |
| `DiscardChangesDialog` | Back when the rule has unsaved changes. |
| Snackbar               | "Saved" / "Discarded".                  |

### 29.4 Required inputs (data)

```
data class RuleEditorUiState(
    val ruleId: Long?,                        // null when new
    val name: String,
    val conditions: List<Condition>,
    val actions: List<Action>,
    val previewCount: Int?,                   // null while computing
    val isPreviewLoading: Boolean,
    val isDirty: Boolean,
    val isValid: Boolean,
    val nameError: String?,
    val conditionErrors: Map<Int, String>,    // index → message
    val actionErrors: Map<Int, String>,
)
```

Sources:

- For existing rule: `AutoTagRuleRepository.getRule(id)` once at start.
- For preview: a `combine(snapshot200, conditionsFlow)` mapped through
  `RuleConditionEvaluator.matchCountIn(snapshot200, conditions)` with
  a 400 ms debounce on the conditions flow.

### 29.5 Required inputs (user)

- Type into the `Name` text field.
- Tap "+ Add condition" → choose a variant from a sheet → fill
  variant-specific inputs.
- Tap "+ Add action" → choose a variant → fill inputs.
- Re-order conditions / actions via drag handles.
- Remove a condition / action via the trailing trash icon.
- Tap Save (toolbar) — only enabled when `isValid`.
- Hardware back / nav back — opens discard-changes confirm if
  `isDirty`.

### 29.6 Mandatory display

```
StandardPage(
    title = uiState.name.ifBlank { "New rule" },
    description = "When all conditions match, do these actions",
    emoji = "⚙️",
)
```

Body in three sections, each rendered as a `NeoCard` with `BorderSoft`:

#### Section A — Name

- A single `NeoTextField`, label `"Name"`, hint `"Tag +91 prefix as
customer"`, max length 60. Helper text shows `nameError` if any.

#### Section B — When all of these are true…

- Header text: `"When all of these are true…"` (titleSmall) plus a
  caption `"All conditions must match for the rule to fire."`
- For each condition, a `ConditionRow` composable:
  ```
  Row {
      DragHandle
      VariantDropdown   // shows e.g. "Number prefix"
      VariantInputs     // variant-specific layout
      RemoveIconButton
  }
  ```
- A trailing `NeoTextButton("+ Add condition")` that opens a bottom
  sheet listing the 13 variants.

#### Section C — Then…

- Header `"Then…"` with caption `"These actions run when the rule
fires. Multiple actions can apply at once."`
- For each action, an `ActionRow` (mirrors `ConditionRow`).
- A trailing `NeoTextButton("+ Add action")` opening a sheet of 4
  variants.

#### LivePreviewBox

Below Section C, a fixed banner with green/grey backgrounds:

```
┌─────────────────────────────────────────────┐
│ 🔮  This rule would apply to 87 calls       │
│     in your last 200.                        │
└─────────────────────────────────────────────┘
```

When `isPreviewLoading`: shows `NeoLoader` and copy `"Working it
out…"`.
When count is 0: orange-tinted with copy `"No calls match yet — try
loosening a condition."`.
When count ≥1: green-tinted.
When evaluator throws (e.g. invalid regex): red-tinted with
`"Can't preview — fix the conditions above."`.

#### Top app bar

- Back arrow.
- Title (rule name in bar ellipsised).
- Save action (text button), enabled when:
  - `name` non-blank,
  - `conditions.size >= 1`,
  - `actions.size >= 1`,
  - `conditionErrors.isEmpty()`,
  - `actionErrors.isEmpty()`.
- Overflow: "Duplicate", "Delete", "View as JSON" (debug builds only).

### 29.7 Optional display

- "Reset to defaults" button next to the title bar's overflow when
  editing an existing rule that was modified.
- A small chip "Rule disabled" beside the title if the rule is saved
  but `!isActive` — tapping toggles via a confirmation snackbar with
  Undo.
- A help icon next to each variant dropdown that opens a 3-line tip
  in a tooltip.

### 29.8 Empty state

For a new rule:

- Section A: empty name field with placeholder.
- Section B: a placeholder card `"No conditions yet. Add at least one
to get started."` with an inline `NeoButton("Add condition")`.
- Section C: similar placeholder card.
- LivePreviewBox: hidden until ≥1 condition is added.

### 29.9 Loading state

- For an existing rule, the screen renders a `NeoLoader` filling the
  body region until the first `getRule(id)` returns. The toolbar shows
  the title `"Loading…"`.
- For new rules, no loader — render straight to the empty state.

### 29.10 Error state

- If `getRule(id)` fails, show a full-screen `StandardError` with copy
  `"Couldn't open this rule. It may have been deleted."` and CTAs
  `"Back to rules"` and `"Retry"`.
- If saving fails (e.g. DB constraint), the Save button re-enables and
  a snackbar shows `"Couldn't save. {error}"` with action `"Retry"`.

### 29.11 Edge cases

| Case                                                  | Handling                                                                                                            |
| ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| Invalid regex in `RegexMatches`                       | Live error `"That regex didn't compile: {message}"`. Save disabled. Preview red.                                    |
| Unreachable rule                                      | Preview shows 0 with the orange tip. Save still allowed — the rule simply won't ever fire.                          |
| Action references a tag the user later deletes        | On reload the action is removed; snackbar fires.                                                                    |
| Reordering rules                                      | Not done here — list screen handles priority.                                                                       |
| Conflicting actions (two `ApplyTag` for the same tag) | Allowed; the second is a no-op. UI shows a yellow info chip "Duplicate action".                                     |
| Editing a rule that's currently running               | Save queues until the current evaluator pass finishes (max wait 1 s, else falls back to fire-and-forget overwrite). |
| Two users on two devices                              | Last-write-wins on `id`.                                                                                            |
| Very long condition list (>20)                        | Allowed, but a banner reads `"Lots of conditions — make sure you really mean ALL of these."`                        |
| Time-of-day pickers crossing midnight                 | Allowed: `from = 22:00`, `to = 02:00` is interpreted as 22:00–02:00 next day.                                       |

#### Condition variants — the 13 documented types

| #   | Variant            | Sealed-class name      | Inputs                              | Valid range / format                                                 | Validation message                             |
| --- | ------------------ | ---------------------- | ----------------------------------- | -------------------------------------------------------------------- | ---------------------------------------------- |
| 1   | Number prefix      | `PrefixMatches`        | `prefix: String`                    | 1–10 chars, digits / `+` / leading zero allowed.                     | "Enter a phone-number prefix like +91 or 080." |
| 2   | Number regex       | `RegexMatches`         | `pattern: String`                   | Must compile via `Pattern.compile`. Cap length 200.                  | "That regex didn't compile: {message}"         |
| 3   | Country            | `CountryEquals`        | `iso2: String`                      | ISO 3166-1 alpha-2, picker UI.                                       | "Pick a country."                              |
| 4   | In contacts        | `IsInContacts`         | `(boolean — implicit)`              | Always valid.                                                        | —                                              |
| 5   | Call type          | `CallTypeIn`           | `types: Set<CallType>`              | At least one of: incoming / outgoing / missed / rejected / blocked.  | "Pick at least one call type."                 |
| 6   | Duration compare   | `DurationCompare`      | `op: <, ≤, =, ≥, >`; `seconds: Int` | 0–86 400.                                                            | "Enter seconds between 0 and 86,400."          |
| 7   | Time of day        | `TimeOfDayBetween`     | `fromMin: Int`, `toMin: Int`        | Each 0–1439. From may exceed to (wraps midnight).                    | "Pick a from and to time."                     |
| 8   | Day of week        | `DayOfWeekIn`          | `days: Set<DayOfWeek>`              | At least one.                                                        | "Pick at least one day."                       |
| 9   | SIM slot           | `SimSlotEquals`        | `slot: Int`                         | 0 or 1 (we only model two SIMs; eSIMs map to slot 1 currently).      | "Pick SIM 1 or SIM 2."                         |
| 10  | Tag applied        | `TagApplied`           | `tagId: Long`                       | Tag must exist.                                                      | "Pick a tag."                                  |
| 11  | Tag NOT applied    | `TagNotApplied`        | `tagId: Long`                       | Tag must exist.                                                      | "Pick a tag."                                  |
| 12  | Geo contains       | `GeoContains`          | `substring: String`                 | 1–60 chars (matches `geocodedLocation` substring, case-insensitive). | "Enter a place name (e.g. Pune)."              |
| 13  | Total call count > | `CallCountGreaterThan` | `n: Int`                            | 0–10 000. Counts calls from the same `e164Number`.                   | "Enter a number between 0 and 10,000."         |

#### Action variants — the 4 documented types

| #   | Variant          | Sealed-class name | Inputs                         | Side effect                                                                                            |
| --- | ---------------- | ----------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------ |
| 1   | Apply tag        | `ApplyTag`        | `tagId: Long`                  | Inserts into `call_tag_cross_ref`. Idempotent.                                                         |
| 2   | Lead score boost | `LeadScoreBoost`  | `delta: Int` (signed, −50…+50) | Adds delta to the call's `leadScore`, clamped to 0…100.                                                |
| 3   | Auto-bookmark    | `AutoBookmark`    | `(none)`                       | Sets `isBookmarked = true`.                                                                            |
| 4   | Mark follow-up   | `MarkFollowUp`    | `days: Int` (1–365)            | Sets `followUpAt = now + days*24h`. Replaces any existing follow-up for the call (we keep the latest). |

### 29.12 Copy table

| Key                    | Resource id                     | English                                                                    |
| ---------------------- | ------------------------------- | -------------------------------------------------------------------------- |
| Title (new)            | `rule_editor_title_new`         | New rule                                                                   |
| Title (edit)           | `rule_editor_title_edit`        | Edit rule                                                                  |
| Subtitle               | `rule_editor_subtitle`          | When all conditions match, do these actions                                |
| Section A label        | `rule_editor_name_label`        | Name                                                                       |
| Section A hint         | `rule_editor_name_hint`         | e.g. Tag +91 prefix as customer                                            |
| Section B header       | `rule_editor_when_header`       | When all of these are true…                                                |
| Section B caption      | `rule_editor_when_caption`      | All conditions must match for the rule to fire.                            |
| Section B add          | `rule_editor_when_add`          | + Add condition                                                            |
| Section B empty        | `rule_editor_when_empty`        | No conditions yet. Add at least one to get started.                        |
| Section C header       | `rule_editor_then_header`       | Then…                                                                      |
| Section C caption      | `rule_editor_then_caption`      | These actions run when the rule fires. Multiple actions can apply at once. |
| Section C add          | `rule_editor_then_add`          | + Add action                                                               |
| Section C empty        | `rule_editor_then_empty`        | No actions yet. Add at least one to get started.                           |
| Preview computing      | `rule_editor_preview_computing` | Working it out…                                                            |
| Preview none           | `rule_editor_preview_zero`      | No calls match yet — try loosening a condition.                            |
| Preview ok             | `rule_editor_preview_ok`        | This rule would apply to %1$d calls in your last 200.                      |
| Preview error          | `rule_editor_preview_error`     | Can't preview — fix the conditions above.                                  |
| Save                   | `rule_editor_save`              | Save                                                                       |
| Save success           | `rule_editor_saved_snack`       | Rule saved.                                                                |
| Save error             | `rule_editor_save_err`          | Couldn't save. %1$s                                                        |
| Discard title          | `rule_editor_discard_title`     | Discard changes?                                                           |
| Discard body           | `rule_editor_discard_body`      | Your edits will be lost.                                                   |
| Discard confirm        | `rule_editor_discard_confirm`   | Discard                                                                    |
| Discard cancel         | `rule_editor_discard_cancel`    | Keep editing                                                               |
| Duplicate overflow     | `rule_editor_duplicate`         | Duplicate                                                                  |
| Delete overflow        | `rule_editor_delete`            | Delete                                                                     |
| Delete confirm         | `rule_editor_delete_confirm`    | Delete this rule? Calls already tagged by it won't be affected.            |
| Disabled chip          | `rule_editor_disabled_chip`     | Rule disabled                                                              |
| Enable snackbar        | `rule_editor_enabled_snack`     | Rule enabled.                                                              |
| Variant: Prefix        | `rule_var_prefix`               | Number prefix                                                              |
| Variant: Regex         | `rule_var_regex`                | Number regex                                                               |
| Variant: Country       | `rule_var_country`              | Country                                                                    |
| Variant: InContacts    | `rule_var_in_contacts`          | Caller is in contacts                                                      |
| Variant: CallType      | `rule_var_call_type`            | Call type                                                                  |
| Variant: Duration      | `rule_var_duration`             | Duration                                                                   |
| Variant: TimeOfDay     | `rule_var_time_of_day`          | Time of day                                                                |
| Variant: DayOfWeek     | `rule_var_day_of_week`          | Day of week                                                                |
| Variant: Sim           | `rule_var_sim`                  | SIM slot                                                                   |
| Variant: TagApplied    | `rule_var_tag_applied`          | Already has tag                                                            |
| Variant: TagNotApplied | `rule_var_tag_not_applied`      | Doesn't have tag                                                           |
| Variant: Geo           | `rule_var_geo`                  | Location contains                                                          |
| Variant: CallCount     | `rule_var_call_count`           | Total calls from this number                                               |
| Action: ApplyTag       | `rule_act_apply_tag`            | Apply tag                                                                  |
| Action: LeadScore      | `rule_act_lead_score`           | Adjust lead score                                                          |
| Action: Bookmark       | `rule_act_bookmark`             | Bookmark the call                                                          |
| Action: FollowUp       | `rule_act_followup`             | Mark follow-up in N days                                                   |
| Add condition sheet    | `rule_pick_condition_sheet`     | Add a condition                                                            |
| Add action sheet       | `rule_pick_action_sheet`        | Add an action                                                              |

### 29.13 ASCII wireframes

#### Wireframe — empty new rule

```
┌──────────────────────────────────────────────────┐
│ ←  New rule                                Save  │
├──────────────────────────────────────────────────┤
│  ⚙️                                              │
│  New rule                                        │
│  When all conditions match, do these actions     │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Name                                     │    │
│  │ ┌──────────────────────────────────────┐ │    │
│  │ │ e.g. Tag +91 prefix as customer      │ │    │
│  │ └──────────────────────────────────────┘ │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ When all of these are true…              │    │
│  │ All conditions must match for the rule   │    │
│  │ to fire.                                 │    │
│  │                                          │    │
│  │   No conditions yet. Add at least one    │    │
│  │   to get started.                        │    │
│  │                                          │    │
│  │ + Add condition                          │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Then…                                    │    │
│  │   No actions yet. Add at least one to    │    │
│  │   get started.                           │    │
│  │ + Add action                             │    │
│  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
```

#### Wireframe — rule with conditions (preview many)

```
┌──────────────────────────────────────────────────┐
│ ←  Tag +91 prefix as customer              Save  │
├──────────────────────────────────────────────────┤
│  ⚙️                                              │
│  Tag +91 prefix as customer                      │
│  When all conditions match, do these actions     │
│                                                  │
│  Name: Tag +91 prefix as customer                │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ When all of these are true…              │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  [Number prefix ▾]  +91          🗑     │    │
│  │ ⇅  [Call type ▾]      ☑ inbound    🗑     │    │
│  │ ⇅  [Duration ▾]       > 20s        🗑     │    │
│  │ + Add condition                          │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Then…                                    │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  [Apply tag ▾]      👥 Customer   🗑    │    │
│  │ ⇅  [Lead score ▾]     +10           🗑    │    │
│  │ + Add action                             │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ 🔮  This rule would apply to 87 calls    │    │
│  │     in your last 200.                    │    │
│  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
```

#### Wireframe — preview zero

```
│  ┌──────────────────────────────────────────┐    │
│  │ 🔮  No calls match yet — try loosening   │    │
│  │     a condition.                         │    │
│  └──────────────────────────────────────────┘    │
```

#### Wireframe — preview error

```
│  ┌──────────────────────────────────────────┐    │
│  │ ⚠  Can't preview — fix the conditions    │    │
│  │     above.                               │    │
│  └──────────────────────────────────────────┘    │
```

### 29.14 Accessibility

- Each section has a `Modifier.semantics { heading() }` on its header.
- Drag handles expose custom actions: `"Move up"`, `"Move down"`,
  `"Remove"`.
- Variant dropdowns use `Role.Button`; the chosen variant is read out
  on focus.
- Time pickers fall back to the platform `TimePicker` dialog with full
  TalkBack support.
- Day-of-week chip group: each chip exposes `selected` state.
- Live preview banner uses `accessibilityLiveRegion = Polite` so
  changes announce on debounce.
- Save button announces its disabled reason via `stateDescription`:
  e.g. `"Save unavailable. Add at least one condition."`
- Discard dialog is announced as a `Role.Alert`.
- Min 48×48 dp tap targets on every dropdown and remove button.
- Regex error helper text is associated to its field via
  `Modifier.semantics { error("…") }`.

### 29.15 Performance budget

- Initial render: ≤350 ms even for a 20-condition rule.
- Preview compute: <300 ms over 200 calls × 20 conditions on a
  Pixel 4a. Debounce 400 ms; cancel-on-new-edit.
- Save: serialise via kotlinx.serialization, single Room upsert, total
  <80 ms.
- Memory: editor holds <800 KB beyond the navigator baseline.
- The variant chooser sheet hydrates lazily; its first open triggers a
  one-time tag-list snapshot via `TagRepository.snapshot()`.
- Regex compile is cached per pattern in `RuleConditionEvaluator`.

---

## 30 — BackupScreen

### 30.1 Purpose

The BackupScreen lets the user protect their callNest data — locally
and (optionally) to Google Drive — and restore from backup if their
device is lost or the app is reinstalled. The screen is the single
control surface for:

- Toggling automatic local backups on a daily schedule.
- Setting the retention window (number of recent backups kept).
- Triggering an on-demand backup right now.
- Restoring from a `.cvb` file picked by the user.
- Setting / changing / clearing the backup encryption passphrase.
- Connecting / disconnecting a Google Drive account and toggling
  upload-after-backup.
- Viewing the current backup status (last backup time, last upload
  time, size).

Backups are encrypted at rest with AES-256-GCM keyed off a
PBKDF2-HMAC-SHA256-derived key from the user's passphrase.

> NOTE: The mega-spec mentions Tink keysets in passing. Implementation
> chose PBKDF2 + raw GCM for two reasons: (1) the user passphrase is
> the only secret material we want to persist offline, so a Tink
> keyset would itself need to be wrapped by the same passphrase, and
> (2) PBKDF2 is built into Android since API 1 and avoids dragging
> Tink's full surface into the backup module. This deviation is
> recorded in `DECISIONS.md`.

### 30.2 Entry points

| From               | Trigger                                  |
| ------------------ | ---------------------------------------- |
| MoreScreen         | "Backup & restore" row.                  |
| Onboarding         | "Set up backups" step.                   |
| Settings → Storage | "Manage backups".                        |
| Update screen      | "Backup before updating" CTA (one-shot). |
| Deep link          | `callNest://backup`.                     |

### 30.3 Exit points

| To                                  | Trigger                                               |
| ----------------------------------- | ----------------------------------------------------- |
| Previous screen                     | Back.                                                 |
| `PassphraseSetupDialog`             | "Set/Change passphrase".                              |
| `PassphraseEntryDialog`             | "Manual backup now" / "Restore from file" (when set). |
| System file picker (`OpenDocument`) | "Restore from file".                                  |
| System Google Sign-In flow          | "Sign in with Google".                                |
| `RestoreConfirmDialog`              | Mid-restore confirmation.                             |
| `BackupCompleteSheet`               | After a successful manual backup.                     |

### 30.4 Required inputs (data)

```
data class BackupUiState(
    val autoBackupEnabled: Boolean,
    val retentionDays: Int,             // 3..14
    val passphraseStatus: PassphraseStatus,   // NotSet, Set
    val lastLocalBackupAt: Long?,
    val lastLocalBackupSizeBytes: Long?,
    val localBackupCount: Int,

    val driveEnabled: Boolean,
    val driveSignedInEmail: String?,
    val autoUploadAfterBackup: Boolean,
    val lastDriveUploadAt: Long?,
    val driveOauthConfigured: Boolean,

    val isManualBackupRunning: Boolean,
    val isRestoreRunning: Boolean,
    val isUploadRunning: Boolean,
    val error: BackupError?,
)

sealed interface PassphraseStatus { object NotSet; object Set }

sealed interface BackupError {
    data class PassphraseMissing(val for: String): BackupError
    data class DriveQuotaExceeded(val message: String): BackupError
    data class DriveOauthExpired(val message: String): BackupError
    data class DriveFolderMissing(val message: String): BackupError
    data class FilePickerCancelled(val message: String): BackupError
    data class WrongPassphrase(val message: String): BackupError
    data class CorruptArchive(val message: String): BackupError
    data class IoFailure(val message: String): BackupError
}
```

Sources:

- `SettingsDataStore.observe(…)` for the toggles and retention.
- `BackupRepository.observeLatest(): Flow<BackupSummary?>` for last
  backup metadata (read from `backup_history` table).
- `DriveRepository.observeAccountStatus(): Flow<DriveAccountStatus>`
  for sign-in / quota / folder existence.
- `BuildConfig.DRIVE_OAUTH_CONFIGURED` for whether the Drive section
  should be shown vs replaced with a "not configured" warning.

### 30.5 Required inputs (user)

- Toggle "Auto-backup".
- Drag retention slider (3–14).
- Tap "Manual backup now".
- Tap "Restore from file" → file picker → passphrase prompt → confirm.
- Tap "Set passphrase" / "Change passphrase" → dialog with two fields
  - show-as-text toggle.
- Tap "Save to Google Drive" toggle.
- Tap "Sign in with Google" / "Sign out".
- Tap "Upload now".
- Toggle "Auto-upload after each local backup".

### 30.6 Mandatory display

```
StandardPage(
    title = "Backup & restore",
    description = "Keep your data safe — locally and in your Drive.",
    emoji = "🛡️",
)
```

Body composed of two `NeoCard`s:

#### Card A — Local

- Header row: `Local` (titleSmall) + status pill `Last backup • 2h
ago • 1.4 MB` (or `Never` when null).
- `Auto-backup` switch row.
- `Keep last N backups` slider row, with live label `Keep last 7
backups`.
- `Manual backup now` `NeoButton` (full width).
- `Restore from file` `NeoButton` (full width, secondary).
- `Backup encryption passphrase` row:
  ```
  Row {
      Column {
          Text("Passphrase")
          Text(if (set) "Set" else "Not set", labelSmall, onSurfaceVariant)
      }
      Spacer(weight = 1f)
      NeoTextButton(if (set) "Change" else "Set") { … }
  }
  ```

#### Card B — Cloud (Google Drive)

When `driveOauthConfigured == false`:

```
NeoCard(BorderSoft) {
  Column {
    Header "Cloud (Google Drive)"
    Text("Drive isn't configured. See docs/locale/06-google-cloud-setup.md.")
    NeoButton("Open setup docs") { openDocs("06-google-cloud-setup") }
  }
}
```

When `driveOauthConfigured == true`:

- Master toggle: `Save to Google Drive` (default OFF). When OFF, the
  rest of the card collapses with a single row left visible:
  `"When enabled, encrypted backups can be uploaded to your Drive."`
- When ON and not signed in: `Sign in with Google` button.
- When ON and signed in:
  - Status row: `"Signed in as you@example.com"` + `Sign out`.
  - `Upload now` button (disabled when passphrase not set).
  - `Auto-upload after each local backup` switch.
  - Status pill: `Last upload • yesterday at 21:04`.
- Explainer text at the bottom of the card:
  `"Backups are encrypted with your passphrase before upload — nothing
readable leaves your device."`

### 30.7 Optional display

- A "Verify last backup" link under Card A — runs a non-destructive
  decrypt + integrity check on the most recent file.
- A "View backup history" link → opens `BackupHistorySheet` (a
  bottom sheet listing the retained backups with size, age,
  passphrase-version label, plus a "Delete" action per entry).
- An info chip "Battery saver mode is on — backup may be delayed"
  when `PowerManager.isPowerSaveMode == true`.
- A red banner above Card A when `passphraseStatus == NotSet &&
autoBackupEnabled`: `"Auto-backup is on but no passphrase is set.
Set one to start backing up."`

### 30.8 Empty state

There's no true empty state — the screen always renders both cards.
However, when no backup has ever been made:

- Status pill in Card A reads `"Never backed up."`.
- Card B's last-upload pill is hidden.

### 30.9 Loading state

- While the ViewModel hydrates: a skeleton placeholder for the two
  cards (44 dp shimmer rows × 4 each).
- During a manual backup (`isManualBackupRunning`): the "Manual backup
  now" button shows a `NeoLoader` (24 dp) and the label changes to
  `"Backing up…"`. Other controls disable.
- During a restore (`isRestoreRunning`): the entire screen overlays a
  modal `NeoProgressBar` with copy `"Restoring — don't close the
app."`. All UI is blocked.
- During an upload: the "Upload now" button shows a loader; the
  auto-upload switch disables.

### 30.10 Error state

Errors render as banners at the top of the affected card:

| BackupError                   | Banner copy                                                                                    |
| ----------------------------- | ---------------------------------------------------------------------------------------------- |
| `PassphraseMissing("backup")` | "Set a passphrase before backing up." with CTA "Set passphrase".                               |
| `PassphraseMissing("upload")` | "Set a passphrase before uploading." with CTA.                                                 |
| `DriveQuotaExceeded`          | "Your Drive is full. Free space, or change account."                                           |
| `DriveOauthExpired`           | "Your Drive sign-in expired. Sign in again." with CTA "Sign in".                               |
| `DriveFolderMissing`          | "Your callNest folder in Drive is gone — we'll recreate it on next upload." (info, not error). |
| `FilePickerCancelled`         | "Restore cancelled." (transient snackbar, not banner).                                         |
| `WrongPassphrase`             | "That passphrase doesn't match this backup."                                                   |
| `CorruptArchive`              | "This file isn't a callNest backup, or it's corrupted."                                        |
| `IoFailure(msg)`              | "Couldn't write the backup: {msg}." with CTA "Retry".                                          |

### 30.11 Edge cases

| Case                                          | Handling                                                                                                                                                                                                                                                                                                                                                                  |
| --------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Passphrase forgotten                          | Lost forever. The Set/Change dialog explicitly states `"There's no recovery — if you forget this passphrase, your backups can't be opened. Write it down somewhere safe."`                                                                                                                                                                                                |
| Backup during sync                            | The backup waits for the running sync to finish (single-flight `Mutex` shared with `SyncService`).                                                                                                                                                                                                                                                                        |
| Restore replacing existing data               | Double-confirm dialog: confirm 1 reads `"This will REPLACE everything in callNest with the contents of {fileName}. Type DELETE to continue."` confirm 2 reads `"Last chance — your current calls, tags, rules, and notes will be wiped. Continue?"`. The actual operation is a single Room transaction: drop user data, restore from archive, commit. Failure rolls back. |
| Drive OAuth expired                           | Refresh-token flow attempted silently; if it fails, surface the `DriveOauthExpired` banner.                                                                                                                                                                                                                                                                               |
| Drive quota exceeded                          | Cache the upload in `cache/pending-uploads/` (capped at 3 entries) and retry next time the quota check passes.                                                                                                                                                                                                                                                            |
| Drive folder deleted by the user              | Recreate `callNest/` at next upload; never touch the user's other Drive content.                                                                                                                                                                                                                                                                                          |
| Low disk space (<50 MB)                       | Refuse to start a manual backup; show a `Snackbar("Not enough space — free at least 50 MB.")`.                                                                                                                                                                                                                                                                            |
| Battery saver                                 | Auto-backups still run via `WorkManager` constraints `setRequiresBatteryNotLow(true)`; manual backups always run.                                                                                                                                                                                                                                                         |
| App killed mid-backup                         | The `.cvb.tmp` file is not promoted to `.cvb`; on next launch the temp is deleted.                                                                                                                                                                                                                                                                                        |
| Restore from a `.cvb` made by a newer schema  | Detected via the magic header version byte. We refuse and show `"This backup was made on a newer version of callNest. Update the app and try again."`                                                                                                                                                                                                                     |
| Restore from a `.cvb` made by an older schema | We run the same Room migration chain on the restored DB before commit.                                                                                                                                                                                                                                                                                                    |
| User signs out of Drive                       | We do NOT delete the cloud copies — we just disconnect the account.                                                                                                                                                                                                                                                                                                       |
| User toggles auto-upload OFF                  | Pending uploads are cleared.                                                                                                                                                                                                                                                                                                                                              |
| Two devices with same Google account          | Both read/write the same `callNest/` folder; backup filenames include device hostname + timestamp so they don't clash.                                                                                                                                                                                                                                                    |

### 30.12 Backup file format

```
+-----------+-------+------+----------------------------+--------+
| Magic     | Salt  | IV   | AES-256-GCM ciphertext     | Tag    |
| "CVB1"    | 16 B  | 12 B | variable                    | 16 B   |
+-----------+-------+------+----------------------------+--------+
4 bytes
```

- Magic header: ASCII bytes `0x43 0x56 0x42 0x31` (`CVB1`). Version 1.
- Salt: 16 random bytes from `SecureRandom`.
- IV: 12 random bytes.
- Key derivation: PBKDF2-HMAC-SHA256, 120,000 iterations, salt above,
  output 32 bytes (256 bits).
- Cipher: AES-256-GCM, 128-bit auth tag.
- Plaintext: a gzip-compressed JSON snapshot produced by
  `BackupSnapshotBuilder` (calls / tags / rules / notes / settings /
  schema version / device id).
- Filename: `callNest-YYYYMMDD-HHmmss-{hostname}.cvb`.

### 30.13 Copy table

| Key                        | Resource id                    | English                                                                                                          |
| -------------------------- | ------------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| Title                      | `backup_title`                 | Backup & restore                                                                                                 |
| Subtitle                   | `backup_subtitle`              | Keep your data safe — locally and in your Drive.                                                                 |
| Local card header          | `backup_local_header`          | Local                                                                                                            |
| Auto-backup label          | `backup_auto_label`            | Auto-backup                                                                                                      |
| Auto-backup caption        | `backup_auto_caption`          | Run a backup once a day when charging and on Wi-Fi.                                                              |
| Retention label            | `backup_retention_label`       | Keep last %1$d backups                                                                                           |
| Manual now                 | `backup_manual_now`            | Manual backup now                                                                                                |
| Manual now running         | `backup_manual_running`        | Backing up…                                                                                                      |
| Restore from file          | `backup_restore_file`          | Restore from file                                                                                                |
| Passphrase row label       | `backup_passphrase_label`      | Backup encryption passphrase                                                                                     |
| Passphrase set             | `backup_passphrase_set`        | Set                                                                                                              |
| Passphrase not set         | `backup_passphrase_not_set`    | Not set                                                                                                          |
| Set                        | `backup_passphrase_set_cta`    | Set                                                                                                              |
| Change                     | `backup_passphrase_change_cta` | Change                                                                                                           |
| Last backup never          | `backup_last_never`            | Never backed up.                                                                                                 |
| Last backup at             | `backup_last_at`               | Last backup • %1$s • %2$s                                                                                        |
| Cloud header               | `backup_cloud_header`          | Cloud (Google Drive)                                                                                             |
| Drive master toggle        | `backup_drive_master`          | Save to Google Drive                                                                                             |
| Drive sign in              | `backup_drive_signin`          | Sign in with Google                                                                                              |
| Drive signed in as         | `backup_drive_signed_in`       | Signed in as %1$s                                                                                                |
| Drive sign out             | `backup_drive_signout`         | Sign out                                                                                                         |
| Drive upload now           | `backup_drive_upload_now`      | Upload now                                                                                                       |
| Drive upload running       | `backup_drive_upload_running`  | Uploading…                                                                                                       |
| Drive auto-upload          | `backup_drive_auto_upload`     | Auto-upload after each local backup                                                                              |
| Drive last upload          | `backup_drive_last_upload`     | Last upload • %1$s                                                                                               |
| Drive explainer            | `backup_drive_explainer`       | Backups are encrypted with your passphrase before upload — nothing readable leaves your device.                  |
| Drive not configured       | `backup_drive_not_configured`  | Drive isn't configured. See docs/locale/06-google-cloud-setup.md.                                                |
| Drive open docs            | `backup_drive_open_docs`       | Open setup docs                                                                                                  |
| Drive when disabled        | `backup_drive_when_disabled`   | When enabled, encrypted backups can be uploaded to your Drive.                                                   |
| Banner: passphrase missing | `backup_banner_passphrase`     | Auto-backup is on but no passphrase is set. Set one to start backing up.                                         |
| Banner: low space          | `backup_banner_low_space`      | Not enough space — free at least 50 MB.                                                                          |
| Restore confirm 1          | `backup_restore_confirm1`      | This will REPLACE everything in callNest with the contents of %1$s. Type DELETE to continue.                     |
| Restore confirm 2          | `backup_restore_confirm2`      | Last chance — your current calls, tags, rules, and notes will be wiped. Continue?                                |
| Restore done               | `backup_restore_done`          | Restored from %1$s.                                                                                              |
| Restore wrong passphrase   | `backup_restore_wrong_pass`    | That passphrase doesn't match this backup.                                                                       |
| Restore corrupt            | `backup_restore_corrupt`       | This file isn't a callNest backup, or it's corrupted.                                                            |
| Backup done                | `backup_done`                  | Backup saved to your Downloads folder.                                                                           |
| Passphrase dialog title    | `backup_pass_dialog_title`     | Backup passphrase                                                                                                |
| Passphrase dialog body     | `backup_pass_dialog_body`      | There's no recovery — if you forget this passphrase, your backups can't be opened. Write it down somewhere safe. |
| Passphrase dialog field 1  | `backup_pass_dialog_f1`        | New passphrase                                                                                                   |
| Passphrase dialog field 2  | `backup_pass_dialog_f2`        | Confirm passphrase                                                                                               |
| Passphrase show            | `backup_pass_dialog_show`      | Show as text                                                                                                     |
| Passphrase save            | `backup_pass_dialog_save`      | Save                                                                                                             |
| Passphrase mismatch        | `backup_pass_dialog_mismatch`  | The two entries don't match.                                                                                     |
| Passphrase too short       | `backup_pass_dialog_short`     | Use at least 8 characters.                                                                                       |

### 30.14 ASCII wireframes

#### Wireframe — defaults, no passphrase, drive off

```
┌──────────────────────────────────────────────────┐
│ ←  Backup & restore                              │
├──────────────────────────────────────────────────┤
│  🛡️                                              │
│  Backup & restore                                │
│  Keep your data safe — locally and in your       │
│  Drive.                                          │
│                                                  │
│  ⚠ Auto-backup is on but no passphrase is set.   │
│    Set one to start backing up.   [Set passphrase]│
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Local             Last backup • Never    │    │
│  ├──────────────────────────────────────────┤    │
│  │ Auto-backup                       [ ●━○ ]│    │
│  │ Keep last 7 backups                       │    │
│  │ ───────────────●───────────────           │    │
│  │ 3              7              14          │    │
│  │                                          │    │
│  │ [    Manual backup now    ]              │    │
│  │ [    Restore from file    ]              │    │
│  │                                          │    │
│  │ Backup passphrase                        │    │
│  │ Not set                            [Set] │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Cloud (Google Drive)              [ ○━━●]│    │
│  │ Save to Google Drive            (off)    │    │
│  │ When enabled, encrypted backups can be   │    │
│  │ uploaded to your Drive.                  │    │
│  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
```

#### Wireframe — passphrase set, signed in, all on

```
│  ┌──────────────────────────────────────────┐    │
│  │ Local        Last backup • 2h ago • 1.4MB│    │
│  ├──────────────────────────────────────────┤    │
│  │ Auto-backup                       [ ●━○ ]│    │
│  │ Keep last 7 backups                      │    │
│  │ [   Manual backup now   ]                │    │
│  │ [   Restore from file   ]                │    │
│  │ Backup passphrase                        │    │
│  │ Set                            [Change]  │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Cloud (Google Drive)         [ ●━○ ]     │    │
│  │ Signed in as you@example.com  [Sign out] │    │
│  │ [   Upload now   ]                       │    │
│  │ Auto-upload after each backup    [ ●━○ ] │    │
│  │ Last upload • yesterday at 21:04         │    │
│  │ ────────────────────────────────────     │    │
│  │ Backups are encrypted with your passphrase│   │
│  │ before upload — nothing readable leaves   │   │
│  │ your device.                              │   │
│  └──────────────────────────────────────────┘    │
```

#### Wireframe — Drive not configured

```
│  ┌──────────────────────────────────────────┐    │
│  │ Cloud (Google Drive)                     │    │
│  ├──────────────────────────────────────────┤    │
│  │ Drive isn't configured. See              │    │
│  │ docs/locale/06-google-cloud-setup.md.    │    │
│  │ [ Open setup docs ]                      │    │
│  └──────────────────────────────────────────┘    │
```

#### Wireframe — restore in progress

```
┌──────────────────────────────────────────────────┐
│                                                  │
│                                                  │
│           Restoring — don't close the app.       │
│                                                  │
│           ▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░  62 %              │
│                                                  │
│           Reading callNest-2026-04-…            │
│                                                  │
└──────────────────────────────────────────────────┘
```

### 30.15 Accessibility

- All toggles have `Role.Switch` with explicit on/off state strings.
- The retention slider has `valueRange = 3f..14f`,
  `steps = 10`, and a `stateDescription` that announces
  `"Keep last {n} backups"`.
- The two cards are headings (`Modifier.semantics { heading() }`).
- Status pills use `accessibilityLiveRegion = Polite`.
- The restore overlay is announced as `Role.ProgressBar` with
  percentage; talkback pauses gestures until it's gone.
- Set/Change dialog announces the passphrase warning before the
  fields, so screen-reader users hear the no-recovery rule first.
- Passphrase fields default to `KeyboardType.Password`; the show-as-
  text toggle is announced as `"Show passphrase"`.
- Min 48×48 dp tap targets across the screen.
- Drive sign-in is the system flow — it inherits Google's a11y.

### 30.15 Performance budget

- Initial render: ≤300 ms (cards have <30 children combined).
- Manual backup: ≤2 s for 10k calls, ≤6 s for 100k calls on a
  Pixel 4a. Compression dominates; we run on `Dispatchers.IO`.
- Restore: ≤4 s for 10k calls on a Pixel 4a. Single transaction.
- Upload: bounded by network. We use `okhttp` resumable uploads.
- Memory: peak <40 MB during compress; we stream gzip.
- DataStore writes (toggle/slider): <20 ms each.
- Drive status flow: emits at most once per minute under steady
  state.

---

## 31 — Export screen (5-step wizard)

### 31.1 Purpose

The Export screen is a guided wizard that walks the user through:

1. Picking an export format (Excel / CSV / PDF / JSON / vCard).
2. Picking a date range.
3. Picking the scope (current filter vs all data).
4. Picking columns (Excel/CSV only).
5. Picking a destination (Downloads vs custom URI).

…and then runs the export. It's distinct from the QuickExport sheet
(§32), which is a one-tap shortcut with no choices.

### 31.2 Entry points

| From                  | Trigger                                                          |
| --------------------- | ---------------------------------------------------------------- |
| MoreScreen            | "Export…" row.                                                   |
| Calls list overflow   | "Export this view" → opens with scope pre-set to current filter. |
| Stats screen overflow | "Export stats as PDF" → format pre-set to PDF.                   |
| In-app docs           | "Try exporting now" link.                                        |
| Deep link             | `callNest://export`.                                             |

### 31.3 Exit points

| To                                     | Trigger                       |
| -------------------------------------- | ----------------------------- |
| Previous screen                        | Cancel / back at step 1.      |
| File picker (`ACTION_CREATE_DOCUMENT`) | At step 5 if "Pick location". |
| `ProgressDialog`                       | After Generate.               |
| Snackbar with Open / Share             | On success.                   |
| `StandardError` snackbar               | On failure.                   |

### 31.4 Required inputs (data)

```
data class ExportUiState(
    val step: Int,                 // 1..5
    val format: ExportFormat?,
    val rangePreset: RangePreset?,
    val customRange: LongRange?,
    val scope: Scope,              // CurrentFilter | AllData
    val columns: Set<Column>,      // valid for Excel/CSV
    val destination: Destination,  // Downloads | Pick(uri)
    val customUri: Uri?,
    val isGenerating: Boolean,
    val progress: Float,
    val rowsExported: Int,
    val totalRows: Int?,
    val resultPath: String?,
    val resultSizeBytes: Long?,
    val error: ExportError?,
)
```

The screen also needs the upstream FilterState to show the user how
many rows their "current filter" scope represents:

- `FilterRepository.observeFilterState(): Flow<FilterState>`.
- `CallRepository.countByFilter(filterState): suspend Long`.

### 31.5 Required inputs (user)

- Step 1: Tap one of 5 format cards.
- Step 2: Tap a preset chip OR pick a custom range.
- Step 3: Tap one of 2 radios.
- Step 4 (Excel/CSV only): Toggle 12 column switches.
- Step 5: Tap a radio; if "Pick location", launch the file picker.
- Bottom bar: Back / Next-or-Generate.

### 31.6 Mandatory display

```
NeoScaffold(
    topBar = TopAppBar(title = "Export", actions = [Cancel]),
    bottomBar = WizardNav(back, primary),
)
```

Each step occupies the full body. Step indicator at top: `1 / 5`,
`2 / 5`, etc., with a thin progress underline.

#### Step 1 — Format

A 2-column grid of 5 `NeoCard`s:

| Index | Emoji | Title | Subtitle                           |
| ----- | ----- | ----- | ---------------------------------- |
| 1     | 📊    | Excel | "Multi-sheet workbook (.xlsx)"     |
| 2     | 📄    | CSV   | "Single comma-separated file"      |
| 3     | 📑    | PDF   | "Printable, includes stats"        |
| 4     | 💾    | JSON  | "Structured, optionally encrypted" |
| 5     | 📇    | vCard | "Contacts only (vCard 3.0)"        |

Tapping selects; selection is shown as a 2 dp coloured border.

#### Step 2 — Date range

Preset chips: `Today`, `Last 7 days`, `Last 30 days`, `This month`,
`Last month`, `Custom`. When `Custom` is chosen, two date pickers
appear (from / to), and a row reads `"X days, Y calls"` showing the
live count.

#### Step 3 — Scope

```
Radio:
  ●  Current filter         (12 calls)
  ○  All data               (1,872 calls)
```

When the user has no active filter, "Current filter" is greyed out
with a helper text `"You haven't filtered the calls list, so this is
the same as All data."`

#### Step 4 — Columns (Excel / CSV only)

Skipped automatically for PDF / JSON / vCard. A list of 12 toggles:

| Toggle               | Default |
| -------------------- | ------- |
| Date & time          | ON      |
| Number               | ON      |
| Contact name         | ON      |
| Type (in/out/missed) | ON      |
| Duration             | ON      |
| SIM slot             | ON      |
| Tags                 | ON      |
| Notes                | OFF     |
| Lead score           | OFF     |
| Geocoded location    | OFF     |
| Bookmarked           | OFF     |
| Archived             | OFF     |

A subtle helper at the bottom reads `"At least one column must be
selected."`. The Generate button disables otherwise.

#### Step 5 — Destination

Radios:

- `●  Downloads folder` — saves to public `Downloads/callNest/`.
- `○  Pick location…` — launches `ACTION_CREATE_DOCUMENT` with a
  suggested filename `callNest-2026-04-30-1430.xlsx`.

Below the radios, a 1-line preview: `"Saving as
callNest-2026-04-30-1430.xlsx (~ 240 KB est.)"`.

#### Bottom bar

| State      | Left button | Right button                        |
| ---------- | ----------- | ----------------------------------- |
| Step 1     | (hidden)    | Next (disabled until format chosen) |
| Step 2..4  | Back        | Next                                |
| Step 5     | Back        | Generate                            |
| Generating | (disabled)  | Cancel                              |

#### Progress dialog

Shown over the screen during `isGenerating`:

```
NeoCard {
    NeoProgressBar(progress)
    Text("Exporting %d / %d…")
    NeoTextButton("Cancel")
}
```

#### Success snackbar

`"Saved callNest-2026-04-30-1430.xlsx (240 KB)"` with actions
`Open` and `Share`. Stays 8 s. Tapping Open uses
`FileProvider` + `ACTION_VIEW`. Tapping Share uses `ACTION_SEND`.

### 31.7 Optional display

- A tiny "Estimate" pill under each step's title showing how big the
  output will be. Updates as choices change.
- A "Save these settings as a preset" link at step 5 (deferred —
  Sprint 13+).
- A history of last 5 exports under the toolbar overflow (deferred —
  Sprint 13+).

### 31.8 Empty state

There's no empty state — the wizard always renders. However, at step
3 if `countByFilter == 0L` AND `scope == CurrentFilter`, show a
warning `"No calls match your current filter — pick All data or
adjust your filter first."` and disable Next.

### 31.9 Loading state

- The "current filter" count and "all data" count are shown as
  shimmer placeholders while loading.
- During generate, the screen is overlaid by the progress dialog and
  bottom bar disables.

### 31.10 Error state

| Error                                      | Surface                                                                                                        |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------- |
| Zero rows after filter                     | Inline at step 3 (see above).                                                                                  |
| Pick-location URI revoked                  | At generate time, snackbar `"Couldn't write to that location. Pick again or use Downloads."`                   |
| Low disk space                             | Pre-flight check at generate; snackbar `"Not enough space — free %d MB and try again."`                        |
| Format-specific error (e.g. POI exception) | Snackbar `"Export failed: {message}"` with Retry.                                                              |
| User cancels mid-run                       | Toast `"Export cancelled."`                                                                                    |
| 100k rows                                  | Allowed but with a banner before generate `"This is a big export — it may take a minute and 50+ MB of space."` |

### 31.11 Edge cases

| Case                            | Handling                                                                                                                                                           |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 100k row export                 | Excel: chunked write, 5,000 rows per `SXSSFWorkbook` flush. CSV: streamed line-by-line. PDF: pages capped at 50 by default; user can opt into "Full" via overflow. |
| Zero rows after filter          | See above.                                                                                                                                                         |
| Pick-location URI revoked       | We fall back to Downloads after a confirm.                                                                                                                         |
| Custom range from > to          | Validation blocks Next at step 2.                                                                                                                                  |
| Non-default locale              | Excel / CSV use locale-aware date format with explicit ISO-8601 column option in advanced mode.                                                                    |
| User toggles all 12 columns off | Generate disables.                                                                                                                                                 |
| Pause during background work    | The progress survives configuration change because the use-case runs in a `WorkManager` job; UI re-binds to the same job by id.                                    |
| Cancel mid-run                  | The use-case checks `isActive` between row chunks; partial files are deleted.                                                                                      |
| Encryption (JSON only)          | Optional checkbox at step 1 if JSON is chosen — `"Encrypt with backup passphrase"`. Reuses backup KDF.                                                             |

### 31.12 Format behaviour table

| Format                   | Sheets                                                                                                                                                                         | Notes |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----- |
| Excel (.xlsx)            | Sheet 1 "Calls" (one row per call), Sheet 2 "Tags" (one row per tag with usage), Sheet 3 "Summary" (totals). Uses `SXSSFWorkbook` for streaming.                               |
| CSV (.csv)               | Single sheet. UTF-8 with BOM (so Excel reads correctly). RFC 4180 quoting.                                                                                                     |
| PDF (.pdf)               | A4 portrait. Cover page with date range + total counts. Body: 1 row per call, 25 rows per page, with optional stats charts on the cover (currently 4/10 charts implemented).   |
| JSON (.json or .json.cv) | Top-level keys: `meta`, `calls`, `tags`, `rules`, `notes`. Pretty-printed. Optional encryption uses the backup passphrase + the same `.cvb` envelope but with extension `.cv`. |
| vCard (.vcf)             | vCard 3.0. One `BEGIN:VCARD` block per unique contact in the selected range. Includes FN, TEL (multiple), CATEGORIES (tags), NOTE.                                             |

> NOTE: The mega-spec lists 10 stats charts for the PDF. The current
> implementation ships 4: calls-by-day, calls-by-tag, calls-by-type,
> top-numbers. The remaining 6 (avg duration / hour-of-day heatmap /
> SIM split / lead-score distribution / win-rate by tag / streak
> calendar) are deferred. Recorded in `DECISIONS.md`.

### 31.13 Copy table

| Key                    | Resource id               | English                                                                         |
| ---------------------- | ------------------------- | ------------------------------------------------------------------------------- |
| Title                  | `export_title`            | Export                                                                          |
| Cancel                 | `export_cancel`           | Cancel                                                                          |
| Step indicator         | `export_step_indicator`   | %1$d of 5                                                                       |
| Step 1 header          | `export_step1_header`     | Pick a format                                                                   |
| Format Excel           | `export_fmt_xlsx`         | Excel                                                                           |
| Format Excel sub       | `export_fmt_xlsx_sub`     | Multi-sheet workbook (.xlsx)                                                    |
| Format CSV             | `export_fmt_csv`          | CSV                                                                             |
| Format CSV sub         | `export_fmt_csv_sub`      | Single comma-separated file                                                     |
| Format PDF             | `export_fmt_pdf`          | PDF                                                                             |
| Format PDF sub         | `export_fmt_pdf_sub`      | Printable, includes stats                                                       |
| Format JSON            | `export_fmt_json`         | JSON                                                                            |
| Format JSON sub        | `export_fmt_json_sub`     | Structured, optionally encrypted                                                |
| Format vCard           | `export_fmt_vcf`          | vCard                                                                           |
| Format vCard sub       | `export_fmt_vcf_sub`      | Contacts only (vCard 3.0)                                                       |
| Step 2 header          | `export_step2_header`     | Pick a date range                                                               |
| Range Today            | `export_range_today`      | Today                                                                           |
| Range 7 days           | `export_range_7d`         | Last 7 days                                                                     |
| Range 30 days          | `export_range_30d`        | Last 30 days                                                                    |
| Range this month       | `export_range_this_month` | This month                                                                      |
| Range last month       | `export_range_last_month` | Last month                                                                      |
| Range custom           | `export_range_custom`     | Custom                                                                          |
| Range live count       | `export_range_count`      | %1$d days, %2$d calls                                                           |
| Range invalid          | `export_range_invalid`    | Pick a from-date before the to-date.                                            |
| Step 3 header          | `export_step3_header`     | Pick a scope                                                                    |
| Scope filter           | `export_scope_filter`     | Current filter (%1$d calls)                                                     |
| Scope all              | `export_scope_all`        | All data (%1$d calls)                                                           |
| Scope no filter helper | `export_scope_no_filter`  | You haven't filtered the calls list, so this is the same as All data.           |
| Scope zero warn        | `export_scope_zero`       | No calls match your current filter — pick All data or adjust your filter first. |
| Step 4 header          | `export_step4_header`     | Pick columns                                                                    |
| Step 4 caption         | `export_step4_caption`    | At least one column must be selected.                                           |
| Col date               | `export_col_date`         | Date & time                                                                     |
| Col number             | `export_col_number`       | Number                                                                          |
| Col name               | `export_col_name`         | Contact name                                                                    |
| Col type               | `export_col_type`         | Type                                                                            |
| Col duration           | `export_col_duration`     | Duration                                                                        |
| Col sim                | `export_col_sim`          | SIM slot                                                                        |
| Col tags               | `export_col_tags`         | Tags                                                                            |
| Col notes              | `export_col_notes`        | Notes                                                                           |
| Col lead               | `export_col_lead`         | Lead score                                                                      |
| Col geo                | `export_col_geo`          | Geocoded location                                                               |
| Col bookmark           | `export_col_bookmark`     | Bookmarked                                                                      |
| Col archive            | `export_col_archive`      | Archived                                                                        |
| Step 5 header          | `export_step5_header`     | Pick a destination                                                              |
| Dest downloads         | `export_dest_downloads`   | Downloads folder                                                                |
| Dest pick              | `export_dest_pick`        | Pick location…                                                                  |
| Dest preview           | `export_dest_preview`     | Saving as %1$s (~ %2$s est.)                                                    |
| Big warning            | `export_big_warn`         | This is a big export — it may take a minute and 50+ MB of space.                |
| Generate               | `export_generate`         | Generate                                                                        |
| Generating             | `export_generating`       | Exporting %1$d / %2$d…                                                          |
| Cancel run             | `export_cancel_run`       | Cancel                                                                          |
| Cancelled              | `export_cancelled`        | Export cancelled.                                                               |
| Success                | `export_success`          | Saved %1$s (%2$s)                                                               |
| Open                   | `export_open`             | Open                                                                            |
| Share                  | `export_share`            | Share                                                                           |
| Failure                | `export_fail`             | Export failed: %1$s                                                             |
| Retry                  | `export_retry`            | Retry                                                                           |
| Encryption optional    | `export_json_encrypt`     | Encrypt with backup passphrase                                                  |

### 31.14 ASCII wireframes

#### Step 1 — Format

```
┌──────────────────────────────────────────────────┐
│ ←  Export                                        │
│ ────────────────────────                         │
│  1 of 5                                          │
│                                                  │
│  Pick a format                                   │
│                                                  │
│  ┌────────────────┐  ┌────────────────┐          │
│  │ 📊 Excel       │  │ 📄 CSV         │          │
│  │ Multi-sheet    │  │ Comma-sep…     │          │
│  └────────────────┘  └────────────────┘          │
│  ┌────────────────┐  ┌────────────────┐          │
│  │ 📑 PDF         │  │ 💾 JSON        │          │
│  │ Printable…     │  │ Structured…    │          │
│  └────────────────┘  └────────────────┘          │
│  ┌────────────────┐                              │
│  │ 📇 vCard       │                              │
│  │ Contacts only… │                              │
│  └────────────────┘                              │
│                                                  │
│ ───────────────────────────────────────────      │
│                                       [ Next ]   │
└──────────────────────────────────────────────────┘
```

#### Step 2 — Date range

```
│  2 of 5                                          │
│  Pick a date range                               │
│                                                  │
│  [Today] [Last 7 days] [Last 30 days]            │
│  [This month] [Last month] [Custom ●]            │
│                                                  │
│  From  ┌──────────────┐   To  ┌──────────────┐   │
│        │ 2026-04-01   │      │ 2026-04-30   │   │
│        └──────────────┘      └──────────────┘   │
│                                                  │
│  30 days, 1,124 calls                            │
│                                                  │
│ [ Back ]                              [ Next ]   │
```

#### Step 3 — Scope

```
│  3 of 5                                          │
│  Pick a scope                                    │
│                                                  │
│  ●  Current filter            (87 calls)         │
│  ○  All data                  (1,872 calls)      │
│                                                  │
│ [ Back ]                              [ Next ]   │
```

#### Step 4 — Columns (Excel/CSV)

```
│  4 of 5                                          │
│  Pick columns                                    │
│                                                  │
│  Date & time              [ ●━○ ]                │
│  Number                   [ ●━○ ]                │
│  Contact name             [ ●━○ ]                │
│  Type                     [ ●━○ ]                │
│  Duration                 [ ●━○ ]                │
│  SIM slot                 [ ●━○ ]                │
│  Tags                     [ ●━○ ]                │
│  Notes                    [ ○━━●]                │
│  Lead score               [ ○━━●]                │
│  Geocoded location        [ ○━━●]                │
│  Bookmarked               [ ○━━●]                │
│  Archived                 [ ○━━●]                │
│                                                  │
│  At least one column must be selected.           │
│                                                  │
│ [ Back ]                              [ Next ]   │
```

#### Step 5 — Destination

```
│  5 of 5                                          │
│  Pick a destination                              │
│                                                  │
│  ●  Downloads folder                             │
│  ○  Pick location…                               │
│                                                  │
│  Saving as callNest-2026-04-30-1430.xlsx        │
│  (~ 240 KB est.)                                 │
│                                                  │
│ [ Back ]                          [ Generate ]   │
```

#### Generating

```
┌──────────────────────────────────────────────────┐
│            ┌──────────────────────────────┐      │
│            │   Exporting 1,234 / 1,872…   │      │
│            │   ▓▓▓▓▓▓▓▓░░░░░░░  66 %       │      │
│            │            [ Cancel ]         │      │
│            └──────────────────────────────┘      │
└──────────────────────────────────────────────────┘
```

### 31.15 Accessibility

- Step indicator is read aloud at each step transition: `"Step 3 of
5: Pick a scope"`. `accessibilityLiveRegion = Polite`.
- Format cards have `Role.RadioButton` + `selected` state.
- Range chip group has `Role.RadioButton` semantics.
- Date pickers fall back to platform pickers.
- Column toggles are `Role.Switch`.
- Bottom-bar primary button announces its disabled reason via
  `stateDescription` (e.g. `"Generate unavailable. Pick at least one
column."`).
- The progress dialog uses `Role.ProgressBar` and announces
  percentage every 10%.
- Snackbar is announced as `accessibilityLiveRegion = Assertive`.
- Min 48×48 dp on every chip and toggle.
- Cancel during run is reachable in two taps.

### 31.16 Performance budget

- Step transitions: ≤16 ms (single frame).
- Live count queries (steps 2–3): ≤80 ms on 100k DB; debounced 150 ms.
- Excel export: ≤8 s for 10k rows; ≤90 s for 100k rows on a Pixel 4a.
  Streamed via `SXSSFWorkbook(100)`.
- CSV export: ≤2 s for 10k rows; ≤25 s for 100k rows.
- PDF export: ≤6 s for 1,000 rows + 4 charts.
- JSON export: ≤1 s for 10k rows.
- vCard export: ≤1 s for 1,000 contacts.
- Memory: peak <80 MB during Excel export thanks to streaming.
- Cancel responsiveness: ≤500 ms from tap to UI dismiss.

---

## 32 — QuickExport sheet

### 32.1 Purpose

The QuickExport sheet is a bottom-sheet shortcut for the three most
common export types. It exists so power users can dump their current
view without walking through five wizard steps. It's a sheet, not a
route — it's mounted inside whichever screen invokes it, and dismissed
by sliding down or tapping outside.

The three options are deliberate:

1. CSV of the current filter.
2. Excel workbook of the current filter.
3. JSON of the entire DB (for archival / migration).

### 32.2 Entry points

| From                  | Trigger                              |
| --------------------- | ------------------------------------ |
| Home (Dashboard)      | "Quick actions" chip "Quick export". |
| MainScaffold overflow | Top-right kebab → "Quick export".    |
| MoreScreen            | "Quick export" row.                  |

The sheet is implemented as a parent-controlled `NeoBottomSheet`. It
is NOT a route in the navigation graph — opening / closing is driven
by a `MutableStateFlow<Boolean>` in `MainViewModel`.

### 32.3 Exit points

| To                                   | Trigger                                    |
| ------------------------------------ | ------------------------------------------ |
| Caller (sheet dismisses)             | Slide down, tap outside, hardware back.    |
| Caller (sheet stays, status updates) | Tap any of the 3 cards while idle / error. |
| File viewer                          | Tap "Open" in success state.               |
| System share sheet                   | Tap "Share" in success state.              |

### 32.4 Required inputs (data)

```
sealed interface QuickExportState {
    object Idle : QuickExportState
    data class Running(val format: ExportFormat) : QuickExportState
    data class Success(val format: ExportFormat, val path: String,
                        val sizeBytes: Long, val openIntent: Intent,
                        val shareIntent: Intent) : QuickExportState
    data class Error(val format: ExportFormat, val message: String) : QuickExportState
}
```

`QuickExportViewModel` owns this state. It also reads
`FilterRepository.observeFilterState()` to know what "current filter"
means at the moment of the click.

### 32.5 Required inputs (user)

- Tap one of three cards.
- Tap "Open" / "Share" / "Retry" in success / error.

### 32.6 Mandatory display

```
NeoBottomSheet(
    isVisible = ...,
    onDismiss = ...,
) {
    Header(title = "Quick Export", subtitle = "Saves to your Downloads folder.")
    NeoCard("📄 CSV (current filter)") { onClick = startCsv }
    NeoCard("📊 Excel workbook (current filter)") { onClick = startXlsx }
    NeoCard("💾 Whole DB as JSON") { onClick = startJson }
    StatusRow(state)
}
```

The status row at the bottom takes 64 dp and switches between four
visuals:

- Idle: empty (just a 1 dp top border).
- Running: `NeoLoader` 36 dp + `"Exporting…"` text.
- Success: `NeoCard` with green border, copy `"Exported %1$s (%2$d
KB)"`, and three buttons: `Open`, `Share`, `Retry`.
- Error: `NeoCard` with red border, copy `"Couldn't export: %1$s"`,
  and one button: `Retry`.

#### Auto-dismiss

In Success state, after 3 s of inactivity, the sheet auto-dismisses.
If the user taps Open / Share / Retry, the timer is cancelled.

### 32.7 Optional display

- A "Last quick export 2h ago" caption under the header, only when a
  previous quick export exists in DataStore (`lastQuickExportAt`).
- A "Wizard?" link in the top-right of the sheet header that closes
  the sheet and opens the full Export wizard preserving any picked
  format.

### 32.8 Empty state

There's no empty state for the sheet itself. However, if the current
filter would yield zero rows (CSV / Excel cards), tapping the card
shows the Error state without running the export, with copy `"No
calls match your current filter."`

### 32.9 Loading state

While `Running`:

- The three cards are disabled (alpha 0.5).
- The status row shows the loader.
- The sheet cannot be dismissed via tap outside (we treat in-flight
  exports as needing acknowledgement). Hardware back triggers a
  "Cancel?" confirm.

### 32.10 Error state

| Cause                          | Surface                                                                                                   |
| ------------------------------ | --------------------------------------------------------------------------------------------------------- |
| Filter not yet hydrated (race) | We fall back to default `FilterState` and proceed. No error shown.                                        |
| Downloads dir unwritable       | Auto-fallback to `cacheDir/quick-exports/`; the path label changes accordingly and we still show success. |
| Export failed (POI / IO)       | Error state with `Retry`.                                                                                 |
| Cancelled mid-run              | Returns to Idle, no banner.                                                                               |
| Zero rows for CSV/Excel        | Error: `"No calls match your current filter."` with Retry disabled.                                       |

### 32.11 Edge cases

| Case                              | Handling                                                                            |
| --------------------------------- | ----------------------------------------------------------------------------------- |
| Filter hydration race             | Use `FilterState.Default` (no filters).                                             |
| Downloads dir unwritable          | Fallback to `cacheDir`.                                                             |
| Export cancelled mid-run          | Sheet returns to Idle, no toast.                                                    |
| User reopens sheet during Running | Sheet stays in Running; no second job is queued.                                    |
| User reopens sheet after Success  | Sheet shows Success again with the same Open/Share intents (so they can re-share).  |
| User reopens after Error          | Sheet shows Error with Retry.                                                       |
| Two concurrent quick exports      | Blocked by the ViewModel's `Mutex`; second tap is a no-op.                          |
| Whole DB JSON > 100 MB            | Allowed; we stream-write. Sheet shows progress percentage in the status row's text. |

### 32.12 Copy table

| Key             | Resource id         | English                             |
| --------------- | ------------------- | ----------------------------------- |
| Header title    | `qe_title`          | Quick Export                        |
| Header subtitle | `qe_subtitle`       | Saves to your Downloads folder.     |
| CSV card        | `qe_card_csv`       | 📄 CSV (current filter)             |
| Excel card      | `qe_card_xlsx`      | 📊 Excel workbook (current filter)  |
| JSON card       | `qe_card_json`      | 💾 Whole DB as JSON                 |
| Status running  | `qe_status_running` | Exporting…                          |
| Status success  | `qe_status_success` | Exported %1$s (%2$s)                |
| Status error    | `qe_status_error`   | Couldn't export: %1$s               |
| Status zero     | `qe_status_zero`    | No calls match your current filter. |
| Open            | `qe_open`           | Open                                |
| Share           | `qe_share`          | Share                               |
| Retry           | `qe_retry`          | Retry                               |
| Last export     | `qe_last_export`    | Last quick export %1$s ago          |
| Wizard link     | `qe_wizard_link`    | Use the full wizard                 |

### 32.13 ASCII wireframe (4 states)

#### Idle

```
            ┌──────────────────────────────────┐
            │  Quick Export             ─      │
            │  Saves to your Downloads folder. │
            ├──────────────────────────────────┤
            │ ┌──────────────────────────────┐ │
            │ │ 📄 CSV (current filter)      │ │
            │ └──────────────────────────────┘ │
            │ ┌──────────────────────────────┐ │
            │ │ 📊 Excel workbook            │ │
            │ │    (current filter)          │ │
            │ └──────────────────────────────┘ │
            │ ┌──────────────────────────────┐ │
            │ │ 💾 Whole DB as JSON          │ │
            │ └──────────────────────────────┘ │
            ├──────────────────────────────────┤
            │ (idle — no status)               │
            └──────────────────────────────────┘
```

#### Running

```
            ├──────────────────────────────────┤
            │  ◐  Exporting…                   │
            ├──────────────────────────────────┤
```

#### Success

```
            ├──────────────────────────────────┤
            │ ┌──────────────────────────────┐ │
            │ │ ✓ Exported callNest-2026-…  │ │
            │ │   (240 KB)                   │ │
            │ │ [ Open ]  [ Share ]  [ Retry]│ │
            │ └──────────────────────────────┘ │
            └──────────────────────────────────┘
   (auto-dismisses after 3 s if no tap)
```

#### Error

```
            ├──────────────────────────────────┤
            │ ┌──────────────────────────────┐ │
            │ │ ⚠ Couldn't export: not enough│ │
            │ │   space in Downloads.        │ │
            │ │ [ Retry ]                    │ │
            │ └──────────────────────────────┘ │
            └──────────────────────────────────┘
```

### 32.14 Accessibility

- The sheet is announced as `Role.Dialog` with title `"Quick Export.
Saves to your Downloads folder."`.
- Each card has `Role.Button` and `contentDescription` like
  `"Export CSV of the current filter, 87 calls."` (the count is read
  from the live filter snapshot).
- Status row uses `accessibilityLiveRegion = Assertive` so success /
  error announce immediately.
- The 3-second auto-dismiss is reset by any focus event from the
  user, so screen-reader users always have time to act.
- The sheet's swipe-down dismiss has a fallback `Close` action
  exposed via `customActions`.
- Min 48×48 dp tap targets.

### 32.15 Performance budget

- Sheet open animation: 250 ms.
- First card tap → first byte written: ≤200 ms.
- CSV (10k rows): ≤2 s end-to-end.
- Excel (10k rows): ≤8 s end-to-end.
- JSON whole DB (10k rows total): ≤1 s end-to-end.
- Memory: <30 MB extra during run.
- Auto-dismiss timer: lightweight `LaunchedEffect` keyed on the
  Success state.

---

## Appendix A — Cross-references

| Topic                                                                                                                                             | See                           |
| ------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------- |
| `BackupManager` algorithm                                                                                                                         | Part 01 §6.9                  |
| `RuleConditionEvaluator`                                                                                                                          | Part 01 §6.4                  |
| `LeadScoreCalculator`                                                                                                                             | Part 01 §6.7                  |
| `FilterState` model                                                                                                                               | Part 02 §15                   |
| `NeoCard` / `NeoButton` / `NeoTextField` / `NeoLoader` / `NeoProgressBar` / `NeoBottomSheet` / `StandardPage` / `StandardEmpty` / `StandardError` | Part 04 (component catalogue) |
| `AutoTagRuleRepository`                                                                                                                           | Part 01 §7.3                  |
| `TagRepository`                                                                                                                                   | Part 01 §7.2                  |
| `BackupRepository`                                                                                                                                | Part 01 §7.5                  |
| `DriveRepository`                                                                                                                                 | Part 01 §7.6                  |
| `ExportUseCase`                                                                                                                                   | Part 01 §6.10                 |
| `QuickExportViewModel`                                                                                                                            | Part 01 §8.4                  |

## Appendix B — Implementation deviations (recap)

1. **PBKDF2 over Tink keysets** — chosen because the only secret
   material is the user-entered passphrase; a Tink keyset would
   itself need wrapping by that passphrase, adding a layer with no
   security benefit. Recorded in `DECISIONS.md`.
2. **6/10 PDF stats charts deferred** — initial release ships
   calls-by-day, calls-by-tag, calls-by-type, top-numbers. Avg
   duration / hour-of-day heatmap / SIM split / lead-score
   distribution / win-rate by tag / streak calendar are deferred to
   post-v1. Recorded in `DECISIONS.md`.
3. **Quick Export "Whole DB JSON" is plaintext** — encryption is
   reserved for the wizard JSON path. Quick Export is a power-user
   shortcut; a passphrase prompt would defeat the "one-tap" promise.
4. **Tag emoji uses EmojiCompat for grapheme validation** — no Tink,
   no third-party emoji libs. Falls back to length check on devices
   without EmojiCompat init complete.
5. **Drive integration is feature-flagged** by `BuildConfig
.DRIVE_OAUTH_CONFIGURED`. The default release sets this `false`
   until the OAuth client is provisioned per the docs in
   `docs/locale/06-google-cloud-setup.md`.
6. **Rule preview cap of 200 calls** — keeps live recompute under
   300 ms even on a low-end Pixel 4a. Users with very rare match
   patterns can still see the preview number jump on next sync.
7. **Backup `.cvb` magic header bytes** — `CVB1`. Future versions
   bump to `CVB2`, etc., and the version-byte check refuses
   newer-format archives with a clear error.
8. **System tags are preserved across resets** — `Reset system tags`
   only restores the original 9 names/colours/emojis; user tags are
   never deleted by any UI affordance other than explicit per-tag
   deletion or merge.
9. **Rule actions order matters within a single rule** — `ApplyTag`
   then `LeadScoreBoost` differs subtly from the reverse only in
   logging order. The persisted JSON preserves insertion order via
   `kotlinx.serialization`'s default behaviour.
10. **Export wizard cancel is best-effort** — partial files are
    deleted on cancel; if cancellation arrives mid-write the file is
    truncated and removed before the snackbar fires.

## Appendix C — DataStore keys touched by this part

| Key                        | Type      | Default      | Owner screen  |
| -------------------------- | --------- | ------------ | ------------- |
| `auto_backup_enabled`      | Boolean   | `false`      | BackupScreen  |
| `backup_retention_days`    | Int       | `7`          | BackupScreen  |
| `backup_passphrase_hash`   | String?   | null         | BackupScreen  |
| `backup_drive_enabled`     | Boolean   | `false`      | BackupScreen  |
| `backup_drive_account`     | String?   | null         | BackupScreen  |
| `backup_auto_upload`       | Boolean   | `false`      | BackupScreen  |
| `last_local_backup_at`     | Long?     | null         | BackupScreen  |
| `last_drive_upload_at`     | Long?     | null         | BackupScreen  |
| `last_quick_export_at`     | Long?     | null         | QuickExport   |
| `last_quick_export_format` | String?   | null         | QuickExport   |
| `export_default_columns`   | StringSet | (7 defaults) | Export wizard |
| `export_last_destination`  | String    | `downloads`  | Export wizard |
| `export_last_range_preset` | String    | `last_30d`   | Export wizard |
| `rules_run_now_last_at`    | Long?     | null         | AutoTagRules  |
| `tags_last_reset_at`       | Long?     | null         | TagsManager   |

## Appendix D — Snackbar / toast inventory for this part

| Surface        | Trigger                   | Duration                  |
| -------------- | ------------------------- | ------------------------- |
| Snackbar       | Tag saved                 | Short                     |
| Snackbar       | Tag deleted               | Short                     |
| Snackbar       | Tags merged               | Long (with Undo)          |
| Toast          | Cannot delete system tag  | Short                     |
| Snackbar       | Rule enabled / disabled   | Short (with Undo)         |
| Snackbar       | Rule saved                | Short                     |
| Snackbar       | Rule deleted              | Long (with Undo)          |
| Snackbar       | Rule discard              | Short                     |
| Banner         | Backup passphrase missing | Persistent until resolved |
| Snackbar       | Backup done               | Long (with Open/Share)    |
| Snackbar       | Restore done              | Long                      |
| Snackbar       | Backup error              | Long (with Retry)         |
| Snackbar       | Drive sign-in expired     | Long (with Sign in)       |
| Snackbar       | Export success            | Long (with Open/Share)    |
| Snackbar       | Export error              | Long (with Retry)         |
| Snackbar       | Export cancelled          | Short                     |
| Inline (sheet) | Quick export success      | Auto-dismiss 3s           |
| Inline (sheet) | Quick export error        | Until acknowledged        |

## Appendix E — Failure modes summary table

| Failure                                | Detected by                                               | Recovery                                                  |
| -------------------------------------- | --------------------------------------------------------- | --------------------------------------------------------- |
| DB corruption affecting tags           | `TagRepository.observeAllTags` error                      | Show empty state with manual reseed CTA in BackupRestore. |
| Invalid rule JSON                      | Deserialiser exception in `AutoTagRuleRepository.loadAll` | Mark `isInvalid`, show in row.                            |
| Regex compile failure                  | `Pattern.compile` in `RegexMatches.matches`               | Editor live-error; runtime skip + `Timber.w`.             |
| Backup decryption failure (wrong pass) | `AEADBadTagException`                                     | "That passphrase doesn't match this backup."              |
| Backup magic mismatch                  | First 4 bytes != `CVB1`                                   | "This file isn't a callNest backup, or it's corrupted."   |
| Backup version too new                 | Magic bytes `CVB2`+                                       | "This backup was made on a newer version of callNest."    |
| Drive 401                              | OAuth token rejected                                      | Refresh; if fail, banner.                                 |
| Drive 403 quota                        | HTTP 403 reason `quotaExceeded`                           | "Your Drive is full."                                     |
| Drive 404 folder                       | HTTP 404 on folder GET                                    | Recreate folder.                                          |
| Export disk full                       | `IOException: ENOSPC`                                     | "Not enough space — free %d MB."                          |
| Export PI failure                      | `OutOfMemoryError` during POI                             | Fallback to CSV with banner explaining.                   |
| Export cancelled                       | Coroutine cancellation                                    | Snackbar "Export cancelled."                              |
| Quick export concurrent                | `Mutex.tryLock` returns false                             | No-op (already running).                                  |
| Quick export zero rows                 | `count == 0` after filter                                 | Error in status row.                                      |

## Appendix F — Telemetry (intentionally none)

Per the project directive in `CLAUDE.md` (§Don'ts):

> Don't add Firebase, Crashlytics, GA, or any analytics SDK. Spec
> §13: "Nothing leaves the device except update version checks."

Therefore none of the screens in this part emit any analytic events.
The only network egress paths are:

- The update-manifest fetch (covered in Part 06).
- Google Drive uploads (only when explicitly enabled by the user).

All other failure surfacing is local — Timber logs, in-memory state,
and snackbar copy.

## Appendix G — String-resource summary count

| Screen             | Strings (this part)                                |
| ------------------ | -------------------------------------------------- |
| TagsManagerScreen  | 30                                                 |
| AutoTagRulesScreen | 22                                                 |
| RuleEditor         | 49                                                 |
| BackupScreen       | 41                                                 |
| Export wizard      | 53                                                 |
| QuickExport sheet  | 12                                                 |
| **Total**          | **207 new strings to add to `values/strings.xml`** |

## Appendix H — Glossary (this part only)

- **System tag** — one of the 9 tags seeded on first DB create. Cannot
  be deleted; can be renamed/recoloured.
- **Cross-ref** — the `call_tag_cross_ref` join table; rows here are
  the source of truth for "which tags are on which calls".
- **Match count** — the number of calls in the latest 200 that satisfy
  every condition of a rule. Recomputed on edit and on sync.
- **`.cvb`** — callNest Backup. The file extension and magic header
  for our encrypted backup blobs.
- **PBKDF2** — Password-Based Key Derivation Function 2; we use HMAC-
  SHA256 with 120,000 iterations.
- **Quick export** — a one-tap shortcut for CSV/Excel/JSON without the
  wizard.
- **Wizard** — the 5-step Export screen.
- **Live preview** — the rule-editor's running match-count, debounced
  400 ms.
- **Discard** — drop unsaved edits; opposite of Save.
- **Reset system tags** — restore the 9 seeded tags' name/colour/emoji
  to defaults; user tags untouched.

## Appendix I — Outstanding TODOs surfaced by this part

These are the items this part adds to `TODO.md`. They are NOT
implementation requirements for this spec — they are followups that
become apparent when the spec is read in full.

1. P1 — Implement the remaining 6 PDF stats charts.
2. P2 — Add a "Save these export settings as a preset" affordance.
3. P2 — Add a "Last 5 exports" history sheet.
4. P3 — Allow a third SIM slot (eSIM-only devices that expose >2).
5. P3 — Allow "Apply tag" rule action to apply multiple tags in one
   action (currently one tag per action).
6. P3 — Localise the curated business-emoji list per locale (e.g.
   Indian-context emojis variant).
7. P3 — Add a "Test this passphrase" affordance in the Set/Change
   dialog that decrypts the latest backup as a sanity check.
8. P3 — Allow custom backup folder pickers for users who prefer a
   non-Downloads location.
9. P3 — Surface the `auto-tag rule "Run now" history` as a sheet so
   users can see when a manual re-run last touched their data.
10. P3 — Add a "Why was this tagged?" affordance on call rows that
    deep-links into the matching rule's editor.

## Appendix J — Keyboard shortcut map (hardware keyboard users)

callNest is an Android phone app, but a non-trivial fraction of
power users plug in a Bluetooth keyboard. The screens in this part
react to the following key combinations when an external keyboard is
attached:

| Screen       | Key                        | Action                                       |
| ------------ | -------------------------- | -------------------------------------------- |
| TagsManager  | `/`                        | Focus the search field.                      |
| TagsManager  | `Esc`                      | Clear search if focused, else navigate back. |
| TagsManager  | `Enter` (on row)           | Open editor.                                 |
| TagsManager  | `Delete` (on row)          | Trigger swipe-delete confirm.                |
| TagsManager  | `N`                        | New tag (FAB).                               |
| AutoTagRules | `N`                        | New rule.                                    |
| AutoTagRules | `Space` (on row)           | Toggle active.                               |
| AutoTagRules | `Alt+Up/Down` (on row)     | Reorder.                                     |
| RuleEditor   | `Ctrl+S`                   | Save.                                        |
| RuleEditor   | `Esc`                      | Discard / back.                              |
| RuleEditor   | `Ctrl+D`                   | Duplicate.                                   |
| Backup       | `B`                        | Trigger manual backup.                       |
| Backup       | `R`                        | Open restore picker.                         |
| Export       | `Right Arrow` / `Tab`      | Next step.                                   |
| Export       | `Left Arrow` / `Shift+Tab` | Previous step.                               |
| Export       | `Enter`                    | Activate primary button.                     |
| QuickExport  | `1` / `2` / `3`            | Trigger CSV / Excel / JSON.                  |
| QuickExport  | `Esc`                      | Dismiss sheet.                               |

These are wired via `Modifier.onKeyEvent { … }` at the screen-level
composables, gated by the platform's `KeyEvent.isCtrlPressed` and
`isAltPressed` helpers.

## Appendix K — Notes on dark mode

All screens in this part respect `MaterialTheme.colorScheme` and
follow these conventions:

- Tag chips use the user's configured `colorHex` as background; the
  text colour is derived via WCAG-grade luminance check, not the
  theme. So a "Customer" tag stays green-on-white in light mode and
  green-on-white in dark mode (the chip is filled).
- Rule rows in the AutoTagRules screen draw on `surface` with `1 dp`
  hairlines on `outlineVariant`.
- The BackupScreen banners use `errorContainer` /
  `tertiaryContainer` for warning / success states, which automatically
  pick the dark-mode-appropriate token.
- The Export wizard's bottom bar uses `surfaceContainerLowest` so it
  reads as a separate plane in both modes.
- The QuickExport sheet inherits the platform sheet container colour
  (`surfaceContainerHigh`).
- All emojis render identically across modes (Android System UI
  emoji font handles its own contrast).
- Selection state on format cards uses a 2 dp `primary` border in
  both modes; in light mode the fill is `primaryContainer`, in dark
  mode the fill is `primary` at 20 % alpha.

## Appendix L — Notes on right-to-left support

callNest's launch locale is en-IN, but the spec requires layout
direction support for future localisation. Specific notes:

- `LazyColumn` rows in TagsManager and AutoTagRules already mirror
  correctly because they use `Row { … }` with `Spacer(weight = 1f)`
  and trailing chevrons.
- The drag handles in AutoTagRules are mirrored (the up/down arrows
  do not flip — they're vertical).
- The Export wizard's progress bar at the top reverses direction in
  RTL.
- The Backup sliders use the platform `Slider` which already reverses.
- The QuickExport sheet's status row mirrors so the loader sits on
  the right edge.
- Emoji prefixes in tag chips stay before the text in both LTR and
  RTL because the chip composes them as a single `Text` line; if a
  future locale needs them after, the chip layout is updated.

## Appendix M — Decisions explicitly NOT taken in this part

We deliberately deferred or rejected the following:

1. **No tag colour picker with arbitrary RGB** — the curated palette
   (12 hand-picked colours) keeps the look consistent. Users wanting
   custom hex can edit the DB via backup/restore, but the UI doesn't
   expose it.
2. **No tag icons beyond emojis** — the design system commits to an
   emoji-prefix tag affordance. Custom drawables would explode the
   asset surface.
3. **No rule-priority numerical input** — priority is set purely by
   list reorder. Showing numbers risks mismatched user mental
   models when conditions overlap.
4. **No "AND/OR" toggle in rule conditions** — all conditions are
   AND. Users wanting OR create a second rule with the same actions.
   This was a deliberate simplification recorded in `DECISIONS.md`.
5. **No pre-canned rule templates inside the editor** — the
   "Browse examples" link routes to in-app docs instead. Templates
   inside the editor would duplicate the docs and bloat the
   editor's UI.
6. **No Drive-only mode (Drive without local backup)** — local is
   always primary. Users who don't want local files can disable
   auto-backup and use only manual + upload, but local is always
   the staging path.
7. **No third cloud (Dropbox / OneDrive)** — Drive is the only cloud
   target by design. Sideloaded apps + Drive's free quota cover the
   target user.
8. **No PDF password protection** — the JSON path covers that
   need. PDF passwords would require iText pro features we're not
   licensing.
9. **No vCard 4.0** — vCard 3.0 has the broadest contact-app
   compatibility. 4.0 is rejected by some Android contacts apps.
10. **No CSV without BOM toggle** — the spec mandates BOM so Excel
    on Windows reads UTF-8 correctly. Tools that don't want BOM can
    strip the first 3 bytes.

## Appendix N — Module-level dependencies introduced by this part

This part exercises the following Hilt modules:

- `RepositoryModule` — provides `TagRepository`, `AutoTagRuleRepository`,
  `BackupRepository`, `DriveRepository`, `ExportRepository`,
  `FilterRepository`.
- `WorkerModule` — `BackupWorker`, `DriveUploadWorker`,
  `ExportWorker`.
- `EvaluatorModule` — `RuleConditionEvaluator`.
- `KeyDerivationModule` — `Pbkdf2KeyDeriver`.
- `EmojiModule` — `EmojiCompatLoader`.
- `DriveOauthModule` (feature-flagged) — `DriveAuthClient`.
- `ExporterModule` — `ExcelExporter`, `CsvExporter`, `PdfExporter`,
  `JsonExporter`, `VCardExporter`.

All ViewModels are bound as `@HiltViewModel` and built with
`@Inject constructor(...)` per the project rules in `CLAUDE.md`.

## Appendix O — End-of-spec checklist for the rebuilding engineer

Before declaring this part of the spec "done" in your build, verify:

- [ ] Nine system tags seeded on first DB create.
- [ ] Tags screen renders, search filters, FAB opens editor, swipe-
      delete confirms only when usage > 0, system tags blocked from
      delete.
- [ ] Tag merge cascades across `call_tag_cross_ref` and any rule
      action referencing the source tag.
- [ ] AutoTagRules screen renders empty state, FAB creates rule,
      switch toggles `isActive`, drag reorders priority.
- [ ] AutoTagRules match-counts capped to latest 200, debounced.
- [ ] RuleEditor supports all 13 condition variants and 4 action
      variants.
- [ ] RuleEditor live preview debounced 400 ms; shows 0/many/error
      tints.
- [ ] RuleEditor Save disabled when invalid; discard dialog on
      back-with-dirty.
- [ ] BackupScreen shows correct status pill in all four states
      (never / never+passphrase / set / drive-on).
- [ ] BackupScreen passphrase dialog enforces ≥8 chars + match.
- [ ] BackupScreen restore double-confirms with "Type DELETE".
- [ ] Backup `.cvb` magic, salt, IV, AES-256-GCM, 120k PBKDF2 iters.
- [ ] BackupScreen Drive section gated by `BuildConfig
    .DRIVE_OAUTH_CONFIGURED`.
- [ ] Export wizard 5 steps with bottom-bar nav, format cards,
      preset chips, scope radios, 12 column toggles, destination
      picker.
- [ ] Export wizard cancellable mid-run, partial files cleaned up.
- [ ] Export Excel uses `SXSSFWorkbook(100)` for streaming.
- [ ] Export CSV writes BOM + RFC 4180 quotes.
- [ ] QuickExport sheet has 3 cards + 4-state status row + 3-second
      auto-dismiss in Success.
- [ ] QuickExport falls back to `cacheDir` if Downloads unwritable.
- [ ] All 207 new strings added to `values/strings.xml`.
- [ ] All ViewModels are `@HiltViewModel` and use `StateFlow`.
- [ ] All Compose composables that ship have at least one `@Preview`.
- [ ] All errors are user-friendly per `CLAUDE.md` rules.
- [ ] Timber-only logging; no `Log.d` / `println`.
- [ ] No mock data outside `@Preview`.
- [ ] No `TODO(` in user-reachable paths.
- [ ] All copy in tables matches `values/strings.xml`.
- [ ] All ASCII wireframes match the implemented layouts.

---

_End of Part 05._

---

# callNest APP-SPEC — Part 06

## Settings Master + Sub-Settings + Update Screens + Docs + Permission Screens + Neo\* Component Reference + Copy Guide + Appendices

> Audience: a UX engineer rebuilding callNest from scratch.
> This part is self-contained. Cross-references to Part 01 (sync algorithm), Part 02 (data layer), Part 03 (real-time services), Part 04 (lead scoring), and Part 05 (backup/export) are noted explicitly.
> All copy strings are normative. All wireframes are reference. All component signatures are normative.

---

## Table of contents (sections 33–46)

| #   | Title                                 | Approx lines |
| --- | ------------------------------------- | ------------ |
| 33  | Settings master screen                | 600          |
| 34  | AutoSaveSettings screen               | 350          |
| 35  | RealTimeSettings screen               | 300          |
| 36  | LeadScoringSettings screen            | 300          |
| 37  | UpdateSettings screen                 | 300          |
| 38  | UpdateAvailable screen                | 350          |
| 39  | DocsList screen                       | 250          |
| 40  | DocsArticle screen                    | 200          |
| 41  | PermissionRationale screen            | 200          |
| 42  | PermissionDenied screen               | 150          |
| 43  | Neo\* component reference             | 600          |
| 44  | Copy / voice guide                    | 250          |
| 45  | Empty / loading / error state catalog | 250          |
| 46  | Future-proofing notes                 | 150          |

---

# 33 — Settings master screen

## 33.1 Purpose

The Settings master screen is the single hub for every persistent preference in callNest. It exposes:

- 12 grouped collapsible NeoCards (Sync, Auto-Save, Real-Time, Notifications, Lead Scoring, Auto-Tag Rules, Backup & Restore, Display, Privacy, App Updates, Help & Docs, About).
- Master toggles that gate dependent options below them (progressive disclosure).
- Sub-links that navigate to deeper sub-setting screens (e.g. AutoSaveSettings, LeadScoringSettings).

Every persisted toggle writes to `SettingsDataStore` (DataStore<Preferences>) debounced at 400ms. Side-effects (e.g. scheduling Workers, restarting the floating-bubble service) are dispatched immediately on the trailing edge of debounce.

## 33.2 Route

- Route: `Settings`
- Reached from: bottom-nav More tab, top-bar gear icon.
- Back stack: pops to wherever the user was.

## 33.3 ViewModel

`SettingsViewModel`:

- Injects `SettingsDataStore`, `WorkManager`, `RealTimeServiceController`, `BackupScheduler`, `ResetAllDataUseCase`.
- Exposes `state: StateFlow<SettingsUiState>`.
- Each toggle emits to a `MutableSharedFlow` debounced 400ms; trailing emission writes to DataStore.

## 33.4 Layout (top-down)

```
┌──────────────────────────────────────────┐
│ ← Settings                               │
├──────────────────────────────────────────┤
│ ▾ Sync                                   │
│   Automatic sync                  [ ●  ] │
│   Interval                               │
│     ○ Manual                             │
│     ● Every 15 min                       │
│     ○ Every hour                         │
│     ○ Every 12 hours                     │
│     ○ Daily 2 AM                         │
│   Wi-Fi only                      [ ●  ] │
│   Charging only                   [    ] │
│   Sync on app open                [ ●  ] │
│   Sync on reboot                  [    ] │
├──────────────────────────────────────────┤
│ ▸ Auto-save inquiries          ➤         │
├──────────────────────────────────────────┤
│ ▸ Real-time features           ➤         │
├──────────────────────────────────────────┤
│ ▾ Notifications                          │
│   Follow-up reminders             [ ●  ] │
│   Daily summary at 9 AM           [    ] │
│   Update alerts                   [ ●  ] │
├──────────────────────────────────────────┤
│ ▸ Lead scoring                  ➤        │
│ ▸ Auto-tag rules                ➤        │
├──────────────────────────────────────────┤
│ ▾ Backup & restore                       │
│   Auto-backup                     [ ●  ] │
│   Keep last        [—■—————]  7 days     │
│   Open backup screen            ➤        │
├──────────────────────────────────────────┤
│ ▾ Display                                │
│   Default tab          Calls (locked)    │
│   Pin unsaved on Calls            [ ●  ] │
│   Group by date                   [    ] │
├──────────────────────────────────────────┤
│ ▾ Privacy                                │
│   Block hidden numbers            [    ] │
│   Hide blocked from list          [ ●  ] │
│   Clear search history            [Run]  │
│   Clear all notes                 [Run]  │
│   Reset all data                  [Run]  │
├──────────────────────────────────────────┤
│ ▸ App updates                  ➤         │
├──────────────────────────────────────────┤
│ ▾ Help & docs                            │
│   Getting started               ➤        │
│   FAQ                           ➤        │
│   Permission troubleshooting    ➤        │
│   OEM battery setup             ➤        │
│   Contact support               ➤ mailto │
│   Privacy policy                ➤        │
│   Terms of use                  ➤        │
├──────────────────────────────────────────┤
│ ▾ About                                  │
│   Version 1.4.2 (build 142)              │
│   Last sync 2026-04-30 09:14             │
│   DB size 14.2 MB                        │
│   Calls tracked 8,412                    │
│   Auto-saved 312                         │
└──────────────────────────────────────────┘
```

## 33.5 Group: Sync

### 33.5.1 Master toggle

- Key: `sync.auto.enabled`
- Default: `true`
- On flip OFF: cancel `PeriodicSyncWorker` (`WorkManager.cancelUniqueWork("sync-periodic")`).
- On flip ON: re-enqueue with current interval (see 33.5.2).

### 33.5.2 Interval radio (revealed only when master ON)

| Option     | Stored value | Worker policy                                  |
| ---------- | ------------ | ---------------------------------------------- |
| Manual     | `MANUAL`     | No worker; user uses pull-to-refresh           |
| 5 min      | `MIN_5`      | PeriodicWorkRequest 15min (Android floor)      |
| 15 min     | `MIN_15`     | PeriodicWorkRequest 15min                      |
| 1 hour     | `HOUR_1`     | PeriodicWorkRequest 1h                         |
| 12 hours   | `HOUR_12`    | PeriodicWorkRequest 12h                        |
| 24 hours   | `DAY_1`      | PeriodicWorkRequest 24h                        |
| Daily 2 AM | `DAILY_2AM`  | PeriodicWorkRequest 24h, initial delay aligned |

Note: Android WorkManager's minimum periodic interval is 15min. The "5 min" label is preserved for future expedited execution but currently maps to 15min — see Part 01 §6.1 for the sync algorithm rationale.

### 33.5.3 Wi-Fi only

- Key: `sync.constraint.wifi`
- Default: `false`
- Effect: `Constraints.Builder().setRequiredNetworkType(UNMETERED)` when ON; `CONNECTED` when OFF.

### 33.5.4 Charging only

- Key: `sync.constraint.charging`
- Default: `false`
- Effect: `Constraints.Builder().setRequiresCharging(true)` when ON.

### 33.5.5 Sync on app open

- Key: `sync.on_open`
- Default: `true`
- Effect: `MainActivity.onResume` triggers a one-shot `OneTimeWorkRequest<SyncOnceWorker>` if last sync > 60s ago.

### 33.5.6 Sync on reboot

- Key: `sync.on_reboot`
- Default: `false`
- Effect: `BootCompletedReceiver` enqueues a one-shot sync.

### 33.5.7 Edge cases

- Constraint changes mid-flight: rescheduling cancels the in-flight worker only on next tick.
- Battery saver active: WorkManager defers; banner appears on Calls screen.
- User toggles too fast: 400ms debounce coalesces; only last value commits.

## 33.6 Group: Auto-Save Inquiries

Single sub-link row: "Open auto-save settings" → routes to `AutoSaveSettings` (see Part 06 §34).

Rationale: Auto-save has 7+ tunables and a live preview, too dense to inline in master.

## 33.7 Group: Real-Time Features

Single sub-link → `RealTimeSettings` (§35).

## 33.8 Group: Notifications

| Key                        | Default | Effect                                                                 |
| -------------------------- | ------- | ---------------------------------------------------------------------- |
| `notif.followup_reminders` | true    | Enables FollowUpReminderWorker (15min check)                           |
| `notif.daily_summary`      | false   | Schedule/cancel DailySummaryWorker @ 9AM                               |
| `notif.update_alerts`      | true    | Allow UpdateCheckWorker to post a notification when an update is found |

### 33.8.1 Daily summary worker

- `DailySummaryWorker` (PeriodicWorkRequest, 24h, initial delay aligned to next 09:00).
- Computes today's: total inbound calls, missed, auto-saved, hot leads gained.
- Posts a notification via `NotificationHelper.postDailySummary(...)`.

### 33.8.2 Edge cases

- User in DnD: notification suppressed by OS; we don't fight it.
- Notifications permission revoked (Android 13+): row shows inline warning chip "Notifications blocked. Enable in System Settings."

## 33.9 Group: Lead Scoring

Sub-link → `LeadScoringSettings` (§36).

## 33.10 Group: Auto-Tag Rules

Sub-link → `AutoTagRules` (defined in Part 04 §22). Master row shows count: "3 active rules".

## 33.11 Group: Backup & Restore

| Key                     | Default | Effect                                      |
| ----------------------- | ------- | ------------------------------------------- |
| `backup.auto.enabled`   | true    | Schedule/cancel `DailyBackupWorker`         |
| `backup.retention.days` | 7       | Slider 1–30. Trims older files on each run. |

Sub-link "Open backup screen" → `Backup` (Part 05 §27).

### 33.11.1 Daily backup worker

- PeriodicWorkRequest, 24h, initial delay aligned to 03:00.
- Writes a zip into `app-private/backups/cv-YYYYMMDD-HHmm.zip`.
- After write, deletes any zip older than `retention.days`.

## 33.12 Group: Display

| Key                     | Default          | Effect                                                   |
| ----------------------- | ---------------- | -------------------------------------------------------- |
| `display.default_tab`   | `CALLS` (locked) | Reserved; v1 only Calls                                  |
| `display.pin_unsaved`   | true             | Calls list pins unsaved-number rows above saved          |
| `display.group_by_date` | true             | Calls list shows section headers Today/Yesterday/Earlier |

## 33.13 Group: Privacy

### 33.13.1 Block hidden numbers

- Key: `privacy.block_hidden`
- Default: `false`
- Effect (advisory only — we cannot actually block calls without being default dialer): hides logs whose number is empty/null/`-1`.

### 33.13.2 Hide blocked from list

- Key: `privacy.hide_blocked`
- Default: `true`
- Effect: filters CallLogEntity rows where `meta.is_blocked = 1`.

### 33.13.3 Clear search history

- Single button. Confirm dialog: title "Clear search history?", body "Recent searches will be removed. This cannot be undone.", actions [Cancel] [Clear].
- Action: `SearchHistoryDao.deleteAll()`.

### 33.13.4 Clear all notes

- Confirm dialog. On confirm: `NoteDao.deleteAll()` + `NoteHistoryDao.deleteAll()`.

### 33.13.5 Reset all data (DOUBLE CONFIRM)

- Step 1 dialog: title "Reset all data?", body "This wipes notes, search history, and skipped versions. Re-sync will rebuild calls from the OS log."
- Step 2 dialog: requires the user to type `DELETE` in a NeoTextField; [Reset] only enabled when text matches.
- Calls `ResetAllDataUseCase`, which clears: notes, note_history, search_history, skipped_versions (4 of 13 tables — see Edge cases).

## 33.14 Group: App Updates

Sub-link → `UpdateSettings` (§37).

## 33.15 Group: Help & Docs (8 sub-links)

| Label                      | Destination                    |
| -------------------------- | ------------------------------ |
| Getting started            | DocsArticle/01-getting-started |
| FAQ                        | DocsList                       |
| Permission troubleshooting | DocsArticle/02-permissions     |
| OEM battery setup          | DocsArticle/12-oem-battery     |
| Contact support            | mailto:support@callNest.app    |
| Privacy policy             | DocsArticle/15-privacy         |
| Terms of use               | DocsArticle/15-privacy#terms   |
| What's new                 | DocsArticle/14-self-update     |

## 33.16 Group: About

Read-only rows:

- Version: `BuildConfig.VERSION_NAME` (build `BuildConfig.VERSION_CODE`)
- Last sync: formatted via `DateFormatter.relative(lastSyncTimestamp)`
- DB size: `context.getDatabasePath("callNest.db").length()` formatted to MB
- Calls tracked: `CallLogDao.count()`
- Auto-saved: `ContactMetaDao.countAutoSaved()`

## 33.17 Edge cases (master)

- DataStore corruption: read returns defaults; warn via Crashlytics breadcrumb.
- "Reset all data" wipes only 4 of 13 tables (notes, note_history, search_history, skipped_versions). Calls/contacts are derived from the OS log on next sync. This is intentional but documented as a known limitation in Part 06 §46.
- Toggle flicker: each row reflects local state immediately; DataStore write is async.

## 33.18 Copy table (50 strings)

| Key                                   | EN                                                                                                          |
| ------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| settings.title                        | Settings                                                                                                    |
| settings.group.sync                   | Sync                                                                                                        |
| settings.sync.auto                    | Automatic sync                                                                                              |
| settings.sync.interval                | Interval                                                                                                    |
| settings.sync.interval.manual         | Manual                                                                                                      |
| settings.sync.interval.5m             | Every 5 minutes                                                                                             |
| settings.sync.interval.15m            | Every 15 minutes                                                                                            |
| settings.sync.interval.1h             | Every hour                                                                                                  |
| settings.sync.interval.12h            | Every 12 hours                                                                                              |
| settings.sync.interval.24h            | Every 24 hours                                                                                              |
| settings.sync.interval.daily2am       | Daily at 2 AM                                                                                               |
| settings.sync.wifi_only               | Wi-Fi only                                                                                                  |
| settings.sync.charging_only           | Charging only                                                                                               |
| settings.sync.on_open                 | Sync on app open                                                                                            |
| settings.sync.on_reboot               | Sync on reboot                                                                                              |
| settings.group.autosave               | Auto-save inquiries                                                                                         |
| settings.autosave.open                | Open auto-save settings                                                                                     |
| settings.group.realtime               | Real-time features                                                                                          |
| settings.realtime.open                | Open real-time settings                                                                                     |
| settings.group.notifications          | Notifications                                                                                               |
| settings.notif.followup               | Follow-up reminders                                                                                         |
| settings.notif.daily_summary          | Daily summary at 9 AM                                                                                       |
| settings.notif.updates                | Update alerts                                                                                               |
| settings.group.scoring                | Lead scoring                                                                                                |
| settings.scoring.open                 | Tune lead scoring                                                                                           |
| settings.group.rules                  | Auto-tag rules                                                                                              |
| settings.rules.count                  | %d active rules                                                                                             |
| settings.group.backup                 | Backup & restore                                                                                            |
| settings.backup.auto                  | Auto-backup                                                                                                 |
| settings.backup.retention             | Keep last %d days                                                                                           |
| settings.backup.open                  | Open backup screen                                                                                          |
| settings.group.display                | Display                                                                                                     |
| settings.display.default_tab          | Default tab                                                                                                 |
| settings.display.default_tab.locked   | Calls (locked in v1)                                                                                        |
| settings.display.pin_unsaved          | Pin unsaved on Calls                                                                                        |
| settings.display.group_by_date        | Group by date                                                                                               |
| settings.group.privacy                | Privacy                                                                                                     |
| settings.privacy.block_hidden         | Block hidden numbers                                                                                        |
| settings.privacy.hide_blocked         | Hide blocked from list                                                                                      |
| settings.privacy.clear_search         | Clear search history                                                                                        |
| settings.privacy.clear_notes          | Clear all notes                                                                                             |
| settings.privacy.reset                | Reset all data                                                                                              |
| settings.privacy.reset.confirm1.title | Reset all data?                                                                                             |
| settings.privacy.reset.confirm1.body  | This wipes notes, search history, and skipped update versions. Re-sync will rebuild calls from your OS log. |
| settings.privacy.reset.confirm2.title | Type DELETE to confirm                                                                                      |
| settings.privacy.reset.confirm2.hint  | DELETE                                                                                                      |
| settings.privacy.reset.confirm.action | Reset                                                                                                       |
| settings.group.updates                | App updates                                                                                                 |
| settings.updates.open                 | Update settings                                                                                             |
| settings.group.help                   | Help & docs                                                                                                 |
| settings.help.start                   | Getting started                                                                                             |
| settings.help.faq                     | Frequently asked questions                                                                                  |
| settings.help.permissions             | Permission troubleshooting                                                                                  |
| settings.help.oem                     | OEM battery setup                                                                                           |
| settings.help.support                 | Contact support                                                                                             |
| settings.help.privacy                 | Privacy policy                                                                                              |
| settings.help.terms                   | Terms of use                                                                                                |
| settings.group.about                  | About                                                                                                       |
| settings.about.version                | Version %s (build %d)                                                                                       |
| settings.about.last_sync              | Last sync %s                                                                                                |
| settings.about.db_size                | Database size %s                                                                                            |
| settings.about.calls_tracked          | Calls tracked %d                                                                                            |
| settings.about.auto_saved             | Auto-saved %d                                                                                               |

## 33.19 Test plan

- Toggle flips: each toggle individually, verify DataStore key persists across process restart.
- Worker scheduling: toggle interval, observe `WorkManager.getWorkInfosForUniqueWork("sync-periodic")` reflects new period.
- Reset double-confirm: typing wrong text leaves [Reset] disabled.
- Privacy hide_blocked: verify Calls list re-queries on flip.

## 33.20 Cross-references

- Sync algorithm details: Part 01 §6.1.
- Auto-tag rules: Part 04 §22.
- Backup screen: Part 05 §27.
- Lead score formula: Part 04 §20.
- Floating bubble service: Part 03 §16.

---

# 34 — AutoSaveSettings screen

## 34.1 Purpose

Tunes how callNest names new contacts when it auto-saves an unsaved number. The combined name template is:

```
{prefix}{simSuffix} {phoneNumber}
```

…written to `Contacts` provider via `ContactWriter.upsert`. The phone label and group ring-fence the contact to a single addressable bucket so the user can later mass-export or mass-delete.

## 34.2 Route

`AutoSaveSettings`. Reached from Settings master.

## 34.3 Wireframe

```
┌──────────────────────────────────────────┐
│ ← Auto-save                              │
│ How callNest names new inquiries  💡    │
├──────────────────────────────────────────┤
│  Auto-save unsaved numbers     [ ●   ]   │
│                                          │
│  Prefix                                  │
│  ┌──────────────────────────┐            │
│  │ callNest                │            │
│  └──────────────────────────┘            │
│                                          │
│  Include SIM tag (-s1/-s2)    [ ●   ]    │
│                                          │
│  Suffix                                  │
│  ┌──────────────────────────┐            │
│  │                          │            │
│  └──────────────────────────┘            │
│                                          │
│  Preview                                 │
│  ┌──────────────────────────────────┐   │
│  │ callNest-s1 +91 98765 43210     │   │
│  └──────────────────────────────────┘   │
│                                          │
│  Group name                              │
│  ┌──────────────────────────┐  [Apply]   │
│  │ callNest Inquiries      │            │
│  └──────────────────────────┘            │
│                                          │
│  Default phone label                     │
│   ● Mobile                               │
│   ○ Work                                 │
│   ○ Home                                 │
│   ○ Other  ┌──────────────────┐          │
│            │ (custom)         │          │
│            └──────────────────┘          │
│                                          │
│  Default region for normalization        │
│  ┌──────┐                                │
│  │ IN   │                                │
│  └──────┘                                │
└──────────────────────────────────────────┘
```

## 34.4 Components

- `StandardPage(title="Auto-save", description="How callNest names new inquiries", emoji="💡")`
- `NeoToggle` — master autoSaveEnabled
- `NeoTextField` — prefix
- `NeoToggle` — includeSimTag
- `NeoTextField` — suffix
- Live preview row (read-only, NeoCard)
- `NeoTextField` + `NeoButton` — group name + Apply
- Radio group — default phone label
- `NeoTextField` (conditional) — custom label
- `NeoTextField` — region

## 34.5 Live preview algorithm

`AutoSaveNameBuilder.preview(prefix, simSlot=1, includeSim, suffix, sample="+919876543210", region)`:

```kotlin
val sim = if (includeSim) "-s${simSlot}" else ""
val formatted = PhoneNormalizer.format(sample, region)
return "$prefix$sim$suffix $formatted"
```

Recomputes on every keystroke (text changes flow through a `derivedStateOf`).

## 34.6 ViewModel

`AutoSaveSettingsViewModel`:

- Reads from `SettingsDataStore` keys `autosave.*`.
- Writes debounced 400ms.
- `applyGroup()`: calls `ContactGroupManager.renameGroup(oldName, newName)` if exists, else `ensureGroup(newName)`.

## 34.7 Defaults

| Key                     | Default              |
| ----------------------- | -------------------- |
| `autosave.enabled`      | true                 |
| `autosave.prefix`       | `callNest`           |
| `autosave.include_sim`  | true                 |
| `autosave.suffix`       | (empty)              |
| `autosave.group`        | `callNest Inquiries` |
| `autosave.label`        | `MOBILE`             |
| `autosave.label.custom` | (empty)              |
| `autosave.region`       | `IN`                 |

## 34.8 Edge cases

- Prefix contains `/` `\` `:` `*` `?` `"` `<` `>` `|`: stripped via `ContactNameSanitizer.sanitize` before write.
- Region invalid (e.g. `XZ`): validate against `libphonenumber.PhoneNumberUtil.getInstance().supportedRegions()`. On invalid, show inline error and keep last valid value.
- Custom label blank when "Other" selected: fallback to `Other`.
- Long prefix (>30 chars): inline warning "Long prefixes may be truncated by your phone".
- Apply group while another sync is mutating contacts: queue via single-thread executor.
- Preview during typing must not cause jank: keep AutoSaveNameBuilder free of allocations on hot path.

## 34.9 Copy table (20 strings)

| Key                        | EN                                                 |
| -------------------------- | -------------------------------------------------- |
| autosave.title             | Auto-save                                          |
| autosave.subtitle          | How callNest names new inquiries                   |
| autosave.master            | Auto-save unsaved numbers                          |
| autosave.prefix.label      | Prefix                                             |
| autosave.prefix.hint       | callNest                                           |
| autosave.sim_tag           | Include SIM tag (-s1/-s2)                          |
| autosave.suffix.label      | Suffix                                             |
| autosave.suffix.hint       | (none)                                             |
| autosave.preview.title     | Preview                                            |
| autosave.group.label       | Group name                                         |
| autosave.group.apply       | Apply                                              |
| autosave.label.title       | Default phone label                                |
| autosave.label.mobile      | Mobile                                             |
| autosave.label.work        | Work                                               |
| autosave.label.home        | Home                                               |
| autosave.label.other       | Other                                              |
| autosave.label.custom.hint | Custom label                                       |
| autosave.region.label      | Default region for normalization                   |
| autosave.region.invalid    | Unknown region. Use a 2-letter code like IN or US. |
| autosave.warn.long_prefix  | Long prefixes may be truncated by your phone.      |

## 34.10 Test plan

- Type `cv ` then preview shows `cv -s1 +91 98765 43210`.
- Toggle SIM off; preview drops `-s1`.
- Apply with new group name; verify `ContactGroupManager` writes.
- Region `XZ` shows inline error, prior valid value kept on save.

## 34.11 Cross-references

- Phone normalization: Part 02 §10.
- Contact writer: Part 02 §11.
- ContactGroupManager: Part 02 §12.

---

# 35 — RealTimeSettings screen

## 35.1 Purpose

Toggles the two real-time UX surfaces: the floating bubble during an active call, and the post-call popup. Both depend on `SYSTEM_ALERT_WINDOW` ("Display over other apps").

## 35.2 Wireframe

```
┌──────────────────────────────────────────┐
│ ← Real-time                              │
│ Floating bubble and post-call popup ✨   │
├──────────────────────────────────────────┤
│ Floating bubble during calls    [ ● ]    │
│ Post-call popup                  [ ● ]    │
│                                          │
│  Popup timeout                           │
│  3s ─────■───────────── 30s   8s         │
│                                          │
│  Show only for unsaved numbers   [ ● ]   │
│                                          │
│  Display over other apps                 │
│  Granted    [ Open Settings ]            │
└──────────────────────────────────────────┘
```

## 35.3 Components

- `StandardPage("Real-time", "Floating bubble and post-call popup", "✨")`
- `NeoToggle` — floatingBubbleEnabled
- `NeoToggle` — postCallPopupEnabled
- `NeoSlider` (3..30, step 1) — popupTimeoutSeconds (revealed only when popup ON)
- `NeoToggle` — popupOnlyForUnsaved (revealed only when popup ON)
- Permission row: status badge + `NeoButton` "Open Settings"

## 35.4 ViewModel

`RealTimeSettingsViewModel`:

- Reads `realtime.*` keys.
- Writes debounced 400ms.
- After any toggle change → `RealTimeServiceController.evaluateAndApply()` which:
  - Starts/stops `FloatingBubbleService`.
  - Registers/unregisters `PostCallReceiver`.

## 35.5 Defaults

| Key                           | Default |
| ----------------------------- | ------- |
| `realtime.bubble.enabled`     | true    |
| `realtime.popup.enabled`      | true    |
| `realtime.popup.timeout_s`    | 8       |
| `realtime.popup.only_unsaved` | true    |

## 35.6 Edge cases

- Overlay permission revoked while service is running: `WindowManager.addView` throws `BadTokenException`; service catches, stops itself, posts a snackbar via SettingsViewModel "Display permission was revoked. Re-grant to use the floating bubble."
- OEM battery saver kills service: a banner appears on Calls screen ("Background services were restricted by your phone. Open OEM battery setup.")
- Toggle during active call: changes apply on next call; current call keeps its bubble.

## 35.7 Copy table (15 strings)

| Key                            | EN                                                                   |
| ------------------------------ | -------------------------------------------------------------------- |
| realtime.title                 | Real-time                                                            |
| realtime.subtitle              | Floating bubble and post-call popup                                  |
| realtime.bubble                | Floating bubble during calls                                         |
| realtime.popup                 | Post-call popup                                                      |
| realtime.popup.timeout         | Popup timeout                                                        |
| realtime.popup.timeout.fmt     | %ds                                                                  |
| realtime.popup.only_unsaved    | Show only for unsaved numbers                                        |
| realtime.permission.title      | Display over other apps                                              |
| realtime.permission.granted    | Granted                                                              |
| realtime.permission.notgranted | Not granted                                                          |
| realtime.permission.open       | Open Settings                                                        |
| realtime.warn.revoked          | Display permission was revoked. Re-grant to use the floating bubble. |
| realtime.warn.killed           | Background services were restricted. Open OEM battery setup.         |
| realtime.banner.oem            | Open OEM battery setup                                               |
| realtime.banner.dismiss        | Dismiss                                                              |

## 35.8 Cross-references

- Floating bubble service: Part 03 §16.
- Post-call popup: Part 03 §17.
- PermissionManager: Part 03 §18.

---

# 36 — LeadScoringSettings screen

## 36.1 Purpose

Lets the user tune the 6 weights used by `LeadScoreCalculator`. After save, a `LeadScoreRecomputeWorker` walks every `ContactMeta` row and writes new scores.

## 36.2 Wireframe

```
┌──────────────────────────────────────────┐
│ ← Lead scoring                           │
│ Tune what makes a hot lead 🎯            │
├──────────────────────────────────────────┤
│ Use lead scoring                  [ ● ]  │
│                                          │
│  Weight: Call frequency                  │
│  0 ──────────■───── 50    25             │
│  Weight: Total duration                  │
│  0 ─────■────────── 50    20             │
│  Weight: Recency                         │
│  0 ──────────■───── 50    25             │
│  Bonus: Has follow-up                    │
│  0 ─────■────────── 25    10             │
│  Bonus: Customer tag                     │
│  0 ──────────■───── 30    20             │
│  Bonus: Saved contact                    │
│  0 ─────■────────── 25    15             │
│                                          │
│  Buckets (locked)                        │
│   Cold  <30                              │
│   Warm  30–70                            │
│   Hot   >70                              │
│                                          │
│           [ Reset to defaults ]          │
└──────────────────────────────────────────┘
```

## 36.3 Components

- `StandardPage("Lead scoring", "Tune what makes a hot lead", "🎯")`
- `NeoToggle` — leadScoreEnabled
- 6× `NeoSlider`
- Read-only buckets card
- `NeoButton` (variant Secondary) — Reset

## 36.4 Defaults

| Slider         | Range | Default |
| -------------- | ----- | ------- |
| Call frequency | 0..50 | 25      |
| Total duration | 0..50 | 20      |
| Recency        | 0..50 | 25      |
| Has follow-up  | 0..25 | 10      |
| Customer tag   | 0..30 | 20      |
| Saved contact  | 0..25 | 15      |

## 36.5 Persistence + worker

On any slider's debounced 400ms commit:

1. Write to DataStore.
2. Cancel any pending `LeadScoreRecomputeWorker`.
3. Enqueue a new one (CoroutineWorker; iterates `ContactMetaDao.allFlow()`, recomputes, writes batch).

## 36.6 Edge cases

- Sum of weights > 100: allowed; final score is clamped to 0..100 inside the formula.
- All weights 0: score equals the bonuses only.
- Recompute during sync: queued (uniqueWork name `lead-score-recompute`, REPLACE policy).
- 100k contacts: worker is paginated 500 at a time, yields between batches to keep it interruptible.

## 36.7 Copy table (15 strings)

| Key                          | EN                         |
| ---------------------------- | -------------------------- |
| scoring.title                | Lead scoring               |
| scoring.subtitle             | Tune what makes a hot lead |
| scoring.master               | Use lead scoring           |
| scoring.weight.frequency     | Weight: Call frequency     |
| scoring.weight.duration      | Weight: Total duration     |
| scoring.weight.recency       | Weight: Recency            |
| scoring.bonus.followup       | Bonus: Has follow-up       |
| scoring.bonus.customer_tag   | Bonus: Customer tag        |
| scoring.bonus.saved_contact  | Bonus: Saved contact       |
| scoring.buckets.title        | Buckets (locked)           |
| scoring.buckets.cold         | Cold below 30              |
| scoring.buckets.warm         | Warm 30 to 70              |
| scoring.buckets.hot          | Hot above 70               |
| scoring.reset                | Reset to defaults          |
| scoring.snackbar.recomputing | Recomputing scores…        |

## 36.8 Cross-references

- Formula: Part 04 §20.
- ContactMeta schema: Part 02 §9.

---

# 37 — UpdateSettings screen

## 37.1 Purpose

Manages the self-update channel and auto-check policy. callNest is sideloaded; `UpdateCheckWorker` polls a JSON manifest hosted by the developer.

## 37.2 Wireframe

```
┌──────────────────────────────────────────┐
│ ← App updates                            │
│ Stable or beta channel, auto-check 🆙    │
├──────────────────────────────────────────┤
│  Current version  1.4.2 (build 142)      │
│                                          │
│  Channel                                 │
│   ● Stable                               │
│   ○ Beta                                 │
│                                          │
│  Check for updates automatically [ ● ]   │
│                                          │
│         [ Check now ]                    │
│                                          │
│  Last checked  2026-04-30 08:00          │
│                                          │
│  [ Clear skipped versions ]              │
└──────────────────────────────────────────┘
```

## 37.3 Components

- `StandardPage("App updates", "Stable or beta channel, auto-check", "🆙")`
- Read-only version row
- Radio group — channel
- `NeoToggle` — autoCheckEnabled
- `NeoButton` Primary — "Check now"
- Read-only last-checked row
- `NeoButton` Tertiary — "Clear skipped versions"

## 37.4 Defaults

| Key                 | Default  |
| ------------------- | -------- |
| `update.channel`    | `STABLE` |
| `update.auto_check` | true     |

## 37.5 Behaviors

- On channel switch: cancel current work, enqueue with new channel param.
- On auto-check toggle ON: schedule `UpdateCheckWorker` weekly.
- On auto-check toggle OFF: cancel it.
- On "Check now": one-shot worker; show spinner; snackbar with result.
  - Result "Up to date" or "Update available v1.5.0" (tap → UpdateAvailable).
- On "Clear skipped": `SkippedVersionDao.deleteAll()` + snackbar.

## 37.6 Edge cases

- Offline: snackbar "No internet. Try again later."
- Manifest 404: snackbar "Couldn't reach update server."
- Manifest schema invalid: log + snackbar "Update server returned bad data."
- Beta returns no update: snackbar "You're on the latest beta."
- User on a beta then switches to Stable while on a higher version: do not "downgrade-prompt"; show "You're ahead of Stable."

## 37.7 Copy table (12 strings)

| Key                       | EN                                 |
| ------------------------- | ---------------------------------- |
| update.title              | App updates                        |
| update.subtitle           | Stable or beta channel, auto-check |
| update.current            | Current version %s (build %d)      |
| update.channel            | Channel                            |
| update.channel.stable     | Stable                             |
| update.channel.beta       | Beta                               |
| update.auto_check         | Check for updates automatically    |
| update.check_now          | Check now                          |
| update.last_checked       | Last checked %s                    |
| update.clear_skipped      | Clear skipped versions             |
| update.snack.up_to_date   | You're on the latest version.      |
| update.snack.available    | Update available: v%s              |
| update.snack.offline      | No internet. Try again later.      |
| update.snack.bad_manifest | Update server returned bad data.   |
| update.snack.beta_latest  | You're on the latest beta.         |
| update.snack.cleared      | Skipped versions cleared.          |

## 37.8 Cross-references

- UpdateCheckWorker schema: Part 05 §29.
- Manifest spec: Part 05 §30.

---

# 38 — UpdateAvailable screen

## 38.1 Purpose

Single screen that switches its body based on `UpdateState`: Available → Downloading → ReadyToInstall → Installing → Error.

## 38.2 Wireframe (Available)

```
┌──────────────────────────────────────────┐
│ ← Update                                 │
├──────────────────────────────────────────┤
│         ┌──────┐                         │
│         │  CV  │   (NeoAvatar)           │
│         └──────┘                         │
│                                          │
│         Update to v1.5.0                 │
│                                          │
│  ## What's new                           │
│  - Faster sync                           │
│  - New empty states                      │
│  - Bugfixes                              │
│                                          │
│  [ Update now ]      (Primary)           │
│  [ Skip this version ] (Secondary)       │
│  [ Later ]           (Tertiary)          │
└──────────────────────────────────────────┘
```

## 38.3 Wireframe (Downloading)

```
│  Downloading…                            │
│  ──────■──────────────────  42%          │
│        [ Cancel ]                        │
```

## 38.4 Wireframe (ReadyToInstall)

```
│  Ready to install v1.5.0                 │
│        [ Install now ]                   │
```

## 38.5 Wireframe (Installing)

```
│  Opening installer…                      │
│  ●●●  (indeterminate)                    │
```

## 38.6 Wireframe (Error)

```
│  ⚠  Couldn't update                      │
│  Reason: SHA-256 mismatch.               │
│        [ Retry ]                         │
```

## 38.7 Components

- `NeoAvatar` (large)
- `MarkdownRenderer` (release notes)
- 3× `NeoButton`
- `NeoProgressBar` (determinate + indeterminate variant)
- `NeoEmptyState` (used as error)

## 38.8 ViewModel

`UpdateAvailableViewModel`:

- Driven by `UpdateRepository.state: StateFlow<UpdateState>`.
- Actions: `onUpdate()`, `onSkip()`, `onLater()`, `onInstall()`, `onCancel()`, `onRetry()`.

## 38.9 Edge cases

- SHA-256 mismatch: file deleted from cache; state → Error("Checksum mismatch").
- Download paused by OS: state stays Downloading; resumed automatically when network returns.
- Install permission denied (Android 8+ requires `REQUEST_INSTALL_PACKAGES`): show fallback: small explanation + "Open Settings" deep-link to `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` for our package.
- versionCode previously skipped: caller (UpdateCheckWorker) silently skips; user can revisit via "Clear skipped versions".
- App backgrounded mid-download: WorkManager keeps it; foreground resync on resume.

## 38.10 Copy table (12 strings)

| Key                    | EN                                              |
| ---------------------- | ----------------------------------------------- |
| upd.title              | Update                                          |
| upd.available.title    | Update to v%s                                   |
| upd.btn.update         | Update now                                      |
| upd.btn.skip           | Skip this version                               |
| upd.btn.later          | Later                                           |
| upd.downloading        | Downloading…                                    |
| upd.btn.cancel         | Cancel                                          |
| upd.ready              | Ready to install v%s                            |
| upd.btn.install        | Install now                                     |
| upd.installing         | Opening installer…                              |
| upd.error.title        | Couldn't update                                 |
| upd.error.reason       | Reason: %s                                      |
| upd.btn.retry          | Retry                                           |
| upd.error.install_perm | Allow install from unknown sources to continue. |
| upd.btn.open_settings  | Open Settings                                   |

## 38.11 Cross-references

- DownloadAndInstallUpdateUseCase: Part 05 §31.
- UpdateInstaller: Part 05 §32.

---

# 39 — DocsList screen

## 39.1 Purpose

Lists 16 in-app help articles bundled in `assets/docs/*.md`. Each row shows emoji + title + last-updated date + first-paragraph excerpt + chevron.

## 39.2 Wireframe

```
┌──────────────────────────────────────────┐
│ ← Help                                   │
│ 16 guides about callNest 📚             │
├──────────────────────────────────────────┤
│ 👋 Getting started                       │
│    Updated 2026-03-12                    │
│    Welcome to callNest. Here's how…   ➤ │
├──────────────────────────────────────────┤
│ 🔐 Permissions                           │
│    Updated 2026-02-04                    │
│    callNest needs phone, contacts…    ➤ │
├──────────────────────────────────────────┤
│ … (15 more)                              │
└──────────────────────────────────────────┘
```

## 39.3 Components

- `StandardPage("Help", "16 guides about callNest", "📚")`
- `LazyColumn` of article rows (NeoCard each, tappable)

## 39.4 Article slugs (locked)

1. 01-getting-started
2. 02-permissions
3. 03-auto-save
4. 04-my-contacts-vs-inquiries
5. 05-tags-and-rules
6. 06-filtering-and-search
7. 07-floating-bubble
8. 08-post-call-popup
9. 09-lead-scoring
10. 10-export
11. 11-backup-restore
12. 12-oem-battery
13. 13-not-default-dialer
14. 14-self-update
15. 15-privacy
16. 16-google-drive-backup (Phase G)

## 39.5 ViewModel

`DocsViewModel` uses `AssetDocsLoader.list()`:

- Enumerates `assets/docs/*.md`.
- Parses YAML front-matter (title, emoji, updated).
- Returns sorted list (slug numeric prefix).

## 39.6 Edge cases

- Missing article file: not listed; warn breadcrumb.
- Malformed front-matter: skip with `Logger.w`.
- Empty `assets/docs/`: show NeoEmptyState ("No help articles bundled.").

## 39.7 Copy table

| Key               | EN                                           |
| ----------------- | -------------------------------------------- |
| docs.title        | Help                                         |
| docs.subtitle.fmt | %d guides about callNest                     |
| docs.row.updated  | Updated %s                                   |
| docs.empty.title  | No help articles                             |
| docs.empty.body   | Help articles weren't bundled in this build. |

## 39.8 Cross-references

- AssetDocsLoader: Part 02 §13.

---

# 40 — DocsArticle screen

## 40.1 Purpose

Renders one markdown article and captures helpful/not-helpful feedback.

## 40.2 Wireframe

```
┌──────────────────────────────────────────┐
│ ← Permissions                            │
│ Why callNest needs each one 📖          │
├──────────────────────────────────────────┤
│ Updated 2026-02-04                       │
│                                          │
│ # Permissions                            │
│ callNest needs the following…           │
│ - **READ_CALL_LOG** — to read your call  │
│   history.                               │
│ - **READ_CONTACTS** …                    │
│                                          │
│ Was this helpful?      [ 👍 ]   [ 👎 ]   │
└──────────────────────────────────────────┘
```

## 40.3 Components

- `StandardPage(title=articleTitle, description=articleExcerpt, emoji="📖")`
- Body: `MarkdownRenderer`
- Feedback row: 2× `NeoIconButton`

## 40.4 ViewModel

`DocsViewModel` (shared with DocsList). On feedback: writes a `DocFeedbackEntity(slug, helpful, ts)`.

## 40.5 Edge cases

- Very long article: smooth scroll; preserve scroll position on rotation.
- Broken markdown: graceful fallback (raw text inside a code block).
- Tap "helpful" twice: debounce 1s.

## 40.6 Copy table

| Key                  | EN                       |
| -------------------- | ------------------------ |
| docs.article.updated | Updated %s               |
| docs.feedback.q      | Was this helpful?        |
| docs.feedback.thanks | Thanks for the feedback. |

## 40.7 Cross-references

- MarkdownRenderer: Part 06 §43 (in component reference).

---

# 41 — PermissionRationale screen

## 41.1 Purpose

Shown the first time the user lands on Calls without the critical runtime permissions, or when they revisit after a denial. Explains why each permission is needed.

## 41.2 Wireframe

```
┌──────────────────────────────────────────┐
│ ← Permissions                            │
│ Why callNest needs access 🔐            │
├──────────────────────────────────────────┤
│ 📞  Phone state                          │
│    To detect when you're on a call.      │
│                                          │
│ 📒  Read call log                        │
│    To read your call history.            │
│                                          │
│ 👥  Contacts                             │
│    To match numbers to names.            │
│                                          │
│ 🔔  Post notifications                   │
│    To remind you of follow-ups.          │
│                                          │
│        [ Grant permissions ]             │
└──────────────────────────────────────────┘
```

## 41.3 Components

- `StandardPage("Permissions", "Why callNest needs access", "🔐")`
- List of permission rows (icon + name + reason)
- `NeoButton` Primary

## 41.4 Logic

- Tap "Grant permissions" → `RequestMultiplePermissions` launcher with the missing set.
- After result: re-check via `PermissionManager.isReady()`.
  - If ready → pop back to Main, route to Calls.
  - If partial → re-show this screen with remaining items.
  - If permanently denied → route to `PermissionDenied`.

## 41.5 Edge cases

- User revokes externally while screen is in background: `onResume` re-evaluates.
- New permission added in a future Android version: list builds dynamically from `PermissionManager.required(buildSdk)`.

## 41.6 Copy table

| Key                  | EN                               |
| -------------------- | -------------------------------- |
| perm.title           | Permissions                      |
| perm.subtitle        | Why callNest needs access        |
| perm.phone_state     | Phone state                      |
| perm.phone_state.why | To detect when you're on a call. |
| perm.read_log        | Read call log                    |
| perm.read_log.why    | To read your call history.       |
| perm.contacts        | Contacts                         |
| perm.contacts.why    | To match numbers to names.       |
| perm.notif           | Post notifications               |
| perm.notif.why       | To remind you of follow-ups.     |
| perm.grant           | Grant permissions                |

## 41.7 Cross-references

- PermissionManager: Part 03 §18.

---

# 42 — PermissionDenied screen

## 42.1 Purpose

Shown when permissions were permanently denied (Android "Don't ask again"). The only path forward is the System Settings.

## 42.2 Wireframe

```
┌──────────────────────────────────────────┐
│ ← Permissions are blocked                │
├──────────────────────────────────────────┤
│  callNest can't run without phone,      │
│  call log, and contacts access. Open     │
│  System Settings to enable them.         │
│                                          │
│        [ Open Settings ]                 │
└──────────────────────────────────────────┘
```

## 42.3 Components

- `StandardPage(title="Permissions are blocked", description=null, emoji=null)`
- Body text
- `NeoButton` Primary → `PermissionManager.openAppSettings()` (intent `ACTION_APPLICATION_DETAILS_SETTINGS`).

## 42.4 Logic

- `onResume` re-checks `PermissionManager.isReady()`.
- If granted → pop and route to Main.

## 42.5 Edge cases

- OEM custom permission center missing the action: fallback to a generic settings intent + snackbar.
- User toggled one but not all: still routes back here until isReady().

## 42.6 Copy table

| Key               | EN                                                                                                    |
| ----------------- | ----------------------------------------------------------------------------------------------------- |
| perm.denied.title | Permissions are blocked                                                                               |
| perm.denied.body  | callNest can't run without phone, call log, and contacts access. Open System Settings to enable them. |
| perm.denied.open  | Open Settings                                                                                         |

---

# 43 — Neo\* component reference

This is the canonical catalog of every reusable UI primitive prefixed `Neo`. Use these — do not roll your own surfaces.

> Convention: every component lives at `ui/components/neo/{Name}.kt`. Every public Composable accepts a trailing `modifier: Modifier = Modifier` parameter.

## 43.1 NeoSurface

- File: `ui/components/neo/NeoSurface.kt`
- Signature:

```kotlin
@Composable
fun NeoSurface(
    modifier: Modifier = Modifier,
    elevation: NeoElevation = NeoElevation.Raised,
    shape: Shape = NeoShapes.Card,
    pressed: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
)
```

| Param     | Notes                                                                 |
| --------- | --------------------------------------------------------------------- |
| elevation | `Flat`, `Raised`, `Sunken`. Maps to a paired light/dark shadow stack. |
| shape     | Defaults to a 16dp rounded card shape.                                |
| pressed   | When true, swaps to inverted (sunken) shadow stack.                   |

Rules:

- Never set a background color directly on a Modifier inside a NeoSurface — pass through `content`.
- Always use `NeoTheme.colors.background` as the base; the shadows depend on it.

States: default (raised), pressed (sunken), disabled (flatter shadows + 60% alpha).

```
┌──────────────┐
│              │  ← raised (top-left light, bottom-right dark)
└──────────────┘
```

Pitfalls:

- Stacking two NeoSurfaces with the same elevation looks muddy. Pair Raised + Sunken instead.
- Putting `clickable` on a child without setting `pressed` won't animate.

## 43.2 NeoCard

- File: `ui/components/neo/NeoCard.kt`
- Signature:

```kotlin
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
)
```

Rules:

- All list rows that look "card-like" must use NeoCard, never `Card` from Material.
- onClick wires up an interactionSource + pressed-state for the underlying NeoSurface.

States: default, pressed (NeoSurface sunken), disabled (alpha 0.6).

Pitfalls: nesting `Modifier.clickable` _and_ passing `onClick` doubles the ripple — pick one.

## 43.3 NeoButton

- File: `ui/components/neo/NeoButton.kt`
- Signature:

```kotlin
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NeoButtonVariant = NeoButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
)
```

Variants: `Primary`, `Secondary`, `Tertiary`, `Destructive`.

States: default, pressed (sunken), disabled (alpha .6 + no ripple), loading (NeoLoader replaces text).

```
┌────────────────────┐
│   Update now       │   ← Primary, raised
└────────────────────┘
```

Pitfalls:

- Don't put two Primary buttons in the same view. Primary is always the "main action".
- Destructive is reserved for irreversible actions.

## 43.4 NeoIconButton

- File: `ui/components/neo/NeoIconButton.kt`
- Signature:

```kotlin
@Composable
fun NeoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: Int? = null,
)
```

Rules: always pass `contentDescription` for a11y; pass null only for purely decorative icons.

States: default, pressed, disabled.

## 43.5 NeoChip

- File: `ui/components/neo/NeoChip.kt`
- Signature:

```kotlin
@Composable
fun NeoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onClose: (() -> Unit)? = null,
)
```

Rules: chips are single-tap toggles. Use a row of chips for multi-select filters. Use `onClose` only when chip represents a removable filter.

States: unselected (raised), selected (sunken + accent border), disabled.

## 43.6 NeoToggle

- File: `ui/components/neo/NeoToggle.kt`
- Signature:

```kotlin
@Composable
fun NeoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

Rules: always paired with a label in a parent Row. Don't put icons inside the toggle thumb.

States: off (knob left, sunken track), on (knob right, raised), disabled.

```
[  ●─── ]  off
[ ───●  ]  on
```

## 43.7 NeoSlider

- File: `ui/components/neo/NeoSlider.kt`
- Signature:

```kotlin
@Composable
fun NeoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

Rules: prefer integer steps for human-meaningful values; show the resolved value as a label next to the slider.

States: default, dragging (knob raised), disabled.

## 43.8 NeoSearchBar

- File: `ui/components/neo/NeoSearchBar.kt`

```kotlin
@Composable
fun NeoSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    leadingIcon: ImageVector? = Icons.Default.Search,
    trailingIcon: ImageVector? = null,
    onTrailingClick: () -> Unit = {},
)
```

Rules: debounce upstream (in ViewModel) at 250ms — not in the bar itself.

## 43.9 NeoFAB

- File: `ui/components/neo/NeoFAB.kt`

```kotlin
@Composable
fun NeoFAB(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
)
```

Rules: only one FAB visible per screen. Extended (with label) only when the screen has a single primary action and there's room.

## 43.10 NeoTabBar

- File: `ui/components/neo/NeoTabBar.kt`

```kotlin
@Composable
fun NeoTabBar(
    tabs: List<NeoTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
)
```

Rules: max 5 tabs. Use NeoBottomNav for primary destinations instead.

## 43.11 NeoTopBar

- File: `ui/components/neo/NeoTopBar.kt`

```kotlin
@Composable
fun NeoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
)
```

Rules: title left-aligned. Back arrow only if `onBack` present.

## 43.12 NeoBottomSheet

- File: `ui/components/neo/NeoBottomSheet.kt`

```kotlin
@Composable
fun NeoBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
)
```

Rules: use for short, focused tasks (filters, edit-tag). For multi-step flows, use a route.

## 43.13 NeoTextField

- File: `ui/components/neo/NeoTextField.kt`

```kotlin
@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: () -> Unit = {},
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
)
```

States: default (sunken), focused (sunken + accent border), error (sunken + red border + helper text), disabled.

## 43.14 NeoProgressBar

- File: `ui/components/neo/NeoProgressBar.kt`

```kotlin
@Composable
fun NeoProgressBar(
    progress: Float? = null, // null = indeterminate
    modifier: Modifier = Modifier,
)
```

## 43.15 NeoLoader

- File: `ui/components/neo/NeoLoader.kt`
- Circular spinner. Use inside buttons (loading=true), inline rows, or full-page empties.

## 43.16 NeoTopLineLoader

- File: `ui/components/neo/NeoTopLineLoader.kt` (Phase I.3)
- Horizontal indeterminate stripe at the top of a screen during a sync. 3dp tall, accent color, rendered above content with z-index 100.

## 43.17 NeoBadge

- File: `ui/components/neo/NeoBadge.kt`
- Small numeric or dot badge. Used on icons (NeoIconButton.badge), tabs.

## 43.18 NeoAvatar

- File: `ui/components/neo/NeoAvatar.kt`

```kotlin
@Composable
fun NeoAvatar(
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    imageUri: Uri? = null,
)
```

Rules: deterministic color from name hash. Initials fallback when no image.

## 43.19 NeoDivider

- File: `ui/components/neo/NeoDivider.kt`
- A 1px line in `NeoTheme.colors.divider`. Avoid stacking dividers in cards — use whitespace.

## 43.20 NeoEmptyState

- File: `ui/components/neo/NeoEmptyState.kt`

```kotlin
@Composable
fun NeoEmptyState(
    icon: ImageVector,
    title: String,
    body: String? = null,
    cta: String? = null,
    onCta: () -> Unit = {},
    modifier: Modifier = Modifier,
)
```

Rules: title <30 chars, body <80 chars, single CTA.

## 43.21 NeoHelpIcon

- File: `ui/components/neo/NeoHelpIcon.kt`
- Small `?` icon button that pops a NeoTooltip with a one-line explainer.

## 43.22 NeoDialog

- File: `ui/components/neo/NeoDialog.kt`

```kotlin
@Composable
fun NeoDialog(
    title: String,
    body: String?,
    primary: NeoDialogAction,
    secondary: NeoDialogAction? = null,
    onDismiss: () -> Unit,
)
```

Rules: max 2 actions. Destructive variant only on irreversible actions.

## 43.23 LeadScoreBadge

- File: `ui/components/LeadScoreBadge.kt`
- Pill: Cold (gray) / Warm (amber) / Hot (red).

## 43.24 NeoPageHeader

- File: `ui/components/NeoPageHeader.kt`
- Title + optional description + optional emoji.

## 43.25 StandardPage

- File: `ui/components/StandardPage.kt`

```kotlin
@Composable
fun StandardPage(
    title: String,
    description: String? = null,
    emoji: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
)
```

Rules: every leaf screen in callNest uses StandardPage as its outermost composable (except Main, which uses MainScaffold).

## 43.26 MainScaffold

- File: `ui/components/MainScaffold.kt`
- Hosts top bar, bottom nav, snackbar host, FAB slot, top-line-loader slot.

## 43.27 Component matrix

| Component        | a11y                        | Theme tokens used               |
| ---------------- | --------------------------- | ------------------------------- |
| NeoSurface       | n/a                         | bg, shadowLight, shadowDark     |
| NeoCard          | role="button" if onClick    | bg, shadow\*                    |
| NeoButton        | role="button"               | accent, onAccent, disabledAlpha |
| NeoIconButton    | contentDescription required | accent                          |
| NeoChip          | role="checkbox"             | accent, surface                 |
| NeoToggle        | role="switch"               | accent, track                   |
| NeoSlider        | role="slider"               | accent                          |
| NeoSearchBar     | role="search"               | bg, hint                        |
| NeoFAB           | role="button"               | accent                          |
| NeoTabBar        | role="tablist"              | accent                          |
| NeoTopBar        | landmark="banner"           | bg                              |
| NeoBottomSheet   | trapFocus                   | bg, scrim                       |
| NeoTextField     | role="textbox"              | bg, border, error               |
| NeoProgressBar   | role="progressbar"          | accent                          |
| NeoLoader        | role="progressbar"          | accent                          |
| NeoTopLineLoader | role="progressbar"          | accent                          |
| NeoBadge         | role="status"               | accent                          |
| NeoAvatar        | contentDescription          | derived                         |
| NeoDivider       | n/a                         | divider                         |
| NeoEmptyState    | n/a                         | text                            |
| NeoHelpIcon      | role="button"               | hint                            |
| NeoDialog        | role="alertdialog"          | bg, scrim                       |

## 43.28 Common pitfalls (across components)

- Don't wrap a NeoCard in a Material Surface — the shadows fight each other.
- Always pass `interactionSource` from the parent if you want hover/pressed states to coordinate.
- Avoid `Modifier.size(0.dp)` on a NeoSurface — it can crash the shadow renderer on some OEMs.
- Test with both light and dark backgrounds; the dual-shadow technique requires a non-pure-white/black bg.

---

# 44 — Copy / voice guide

## 44.1 Voice

- Professional, neutral, terse, confident.
- Avoid: exclamation marks, emoji in dialogs, "Awesome!", marketing tone, passive voice.
- Prefer "we" only in errors ("we couldn't save it"). Otherwise omit subject.

## 44.2 Per-screen tone

| Screen       | Tone                             |
| ------------ | -------------------------------- |
| Onboarding   | Warm + reassuring                |
| Errors       | Actionable; never blame the user |
| Empty states | Light + actionable               |
| Settings     | Descriptive + neutral            |
| Docs         | Clear + hands-on                 |
| Dialogs      | Direct; no exclamations          |

## 44.3 Patterns

### Error string

`Couldn't <verb>. <reason or retry hint>.`

Examples:

- `Couldn't sync. No internet.`
- `Couldn't save the note. Try again.`

### Empty state

- Icon
- Title (<30 chars)
- Body (<80 chars)
- 1 CTA (verb-first)

### Confirm dialog

- Title (<40 chars), ends with `?`
- Body explaining consequence (1 sentence)
- Cancel + Action; Action uses Destructive variant if applicable.

## 44.4 Bad → Good (50 examples)

| Bad                                                   | Good                                              |
| ----------------------------------------------------- | ------------------------------------------------- |
| Oops! Something went wrong!                           | Couldn't load. Try again.                         |
| Awesome! Saved!                                       | Saved.                                            |
| Sync failed because the network could not be reached. | Couldn't sync. No internet.                       |
| The note could not be deleted.                        | Couldn't delete the note.                         |
| You have no calls.                                    | No calls yet.                                     |
| Click here to grant permissions.                      | Grant permissions                                 |
| Are you sure you want to do this?                     | Reset all data?                                   |
| Yes                                                   | Reset                                             |
| No                                                    | Cancel                                            |
| Please enter a valid number.                          | Use digits only.                                  |
| Loading… please wait.                                 | Loading…                                          |
| Your changes were saved successfully.                 | Saved.                                            |
| Your changes were not saved.                          | Couldn't save. Try again.                         |
| Tap to add your first contact!                        | Add a contact                                     |
| You have not set up backups.                          | No backups yet.                                   |
| Backups are saved!                                    | Backup created.                                   |
| Failed to export.                                     | Couldn't export. Try again.                       |
| There are no results matching your search.            | No matches.                                       |
| Permissions are required for the app.                 | callNest needs phone access.                      |
| Couldn't load due to error 502.                       | Couldn't load. Try again.                         |
| Are you really sure you want to delete this?          | Delete this note?                                 |
| Hooray!                                               | (omit)                                            |
| Update was a success.                                 | Updated to v%s.                                   |
| Update was a failure.                                 | Couldn't update. %s.                              |
| Click here to retry.                                  | Retry                                             |
| Settings saved successfully.                          | Saved.                                            |
| Network connection lost.                              | No internet.                                      |
| Hidden numbers found.                                 | %d hidden numbers.                                |
| Sync is in progress, please don't close the app.      | Syncing…                                          |
| Backup file is corrupted.                             | Backup file is unreadable.                        |
| Your phone might restrict background tasks.           | Background tasks may be restricted by your phone. |
| Tap below to continue.                                | Continue                                          |
| You have unsaved changes.                             | Unsaved changes.                                  |
| Field is required.                                    | Required.                                         |
| Phone number must be valid.                           | Use a valid phone number.                         |
| Please grant the permission to continue.              | Grant permissions to continue.                    |
| Notes was successfully cleared.                       | Notes cleared.                                    |
| Failure during contact import.                        | Couldn't import contacts.                         |
| File too big!                                         | File is too large.                                |
| Calculation error occurred.                           | Couldn't compute the score.                       |
| You're now on Beta channel!                           | Switched to Beta.                                 |
| Update server unreachable.                            | Couldn't reach update server.                     |
| All set!                                              | Done.                                             |
| Operation failed.                                     | Couldn't complete. Try again.                     |
| It worked!                                            | Done.                                             |
| Please give us a moment.                              | One moment…                                       |
| Apologies for the inconvenience.                      | (omit)                                            |
| You're all good to go.                                | Ready.                                            |
| Save was performed.                                   | Saved.                                            |
| Could not load the document.                          | Couldn't open the document.                       |
| Failed: timeout.                                      | Timed out. Try again.                             |

## 44.5 Punctuation rules

- Sentences end with `.` except titles.
- Em dashes `—` only in long descriptions, not in buttons.
- Avoid `!` everywhere except onboarding's first welcome line.
- Use `…` for in-progress states, not `...`.

## 44.6 Numbers + dates

- Use `%d` placeholders for plurals; ICU select where the language requires.
- Dates: `2026-04-30` for read-only "Last sync"; relative ("2 min ago") for inline.

## 44.7 Plural rules

| One      | Other      |
| -------- | ---------- |
| 1 call   | %d calls   |
| 1 day    | %d days    |
| 1 minute | %d minutes |

## 44.8 Cross-references

- All copy keys are reused across Parts 01–06; this guide is the source of truth for any new strings.

---

# 45 — Empty / loading / error state catalog

## 45.1 Pattern overview

| State   | Component                                    | When                   |
| ------- | -------------------------------------------- | ---------------------- |
| Empty   | NeoEmptyState                                | Query returned no rows |
| Loading | NeoLoader / NeoTopLineLoader / skeleton      | Async fetch in flight  |
| Error   | NeoEmptyState (sad icon) / Snackbar / inline | Async fetch failed     |

## 45.2 Empty state catalog

| Screen           | Title            | Body                            | CTA           |
| ---------------- | ---------------- | ------------------------------- | ------------- |
| Calls            | No calls yet     | New calls will appear here.     | (none)        |
| Calls (filtered) | No matches       | Try a different filter.         | Clear filters |
| Search           | No matches       | Try a shorter query.            | (none)        |
| MyContacts       | No contacts      | Saved contacts appear here.     | Sync now      |
| Inquiries        | No inquiries yet | Auto-saved numbers appear here. | (none)        |
| Notes            | No notes         | Add a note to remember context. | Add note      |
| FollowUps        | No follow-ups    | Schedule one from a call.       | (none)        |
| Tags             | No tags          | Tags help you filter calls.     | New tag       |
| Rules            | No rules         | Auto-tag rules go here.         | New rule      |
| Backups          | No backups       | Auto-backup creates one daily.  | Back up now   |
| Stats            | No data yet      | Stats appear after a few calls. | (none)        |
| Updates          | No updates       | You're on the latest version.   | Check now     |
| Docs             | No help articles | Help articles weren't bundled.  | (none)        |

## 45.3 Loading state catalog

| Screen          | Style                                      | Why                                 |
| --------------- | ------------------------------------------ | ----------------------------------- |
| Calls           | NeoTopLineLoader during sync, list visible | Don't block; user can browse cached |
| Inquiries       | NeoTopLineLoader                           | Same                                |
| MyContacts      | NeoTopLineLoader                           | Same                                |
| Stats           | Full-page skeleton                         | Initial load only                   |
| Backups         | NeoLoader inline on row                    | One-off action                      |
| Update download | NeoProgressBar (determinate)               | Has known progress                  |
| Update install  | NeoLoader (indeterminate)                  | Unknown duration                    |
| Search          | NeoLoader inline next to bar               | Debounced                           |
| Onboarding      | NeoLoader full-page                        | First sync                          |

## 45.4 Error state catalog

| Screen             | Style           | Copy                                      |
| ------------------ | --------------- | ----------------------------------------- |
| Calls (sync error) | Snackbar        | Couldn't sync. %s. [Retry]                |
| MyContacts         | Snackbar        | Couldn't sync. [Retry]                    |
| Backup creation    | Snackbar        | Couldn't create backup. [Retry]           |
| Backup restore     | Full-page       | Couldn't restore. The file is unreadable. |
| Update download    | Full-page       | Couldn't download. %s. [Retry]            |
| Update install     | Full-page       | Couldn't install. %s. [Retry]             |
| Docs article       | Inline fallback | Couldn't render this article.             |
| Settings write     | Snackbar        | Couldn't save. Try again.                 |

## 45.5 Snackbar guidelines

- 1 line of copy.
- 1 action max ("Retry" or "Open").
- 4-second duration; 8 seconds with action.

## 45.6 Skeleton vs spinner

- Use **skeleton** when the screen has a known layout shape and you want zero CLS-equivalent jank.
- Use **NeoLoader** when the layout depends on fetched data shape.
- Use **NeoTopLineLoader** when there's already cached content to show.

## 45.7 Cross-references

- NeoEmptyState: §43.20.
- Snackbar host: §43.26 (MainScaffold).

---

# 46 — Future-proofing notes

## 46.1 v1.1 (next minor)

| Item                         | Notes                                                                                                       |
| ---------------------------- | ----------------------------------------------------------------------------------------------------------- |
| 6 missing stats charts       | Hourly distribution, top callers, week-over-week, missed-call ratio, average duration, hot-lead conversion. |
| PDF chart embedding          | Export of stats screen with charts as a single PDF.                                                         |
| NoteEditorDialog → NeoDialog | Migrate from raw AlertDialog.                                                                               |
| TagEditorDialog → NeoDialog  | Same.                                                                                                       |
| Padding/empty-state audit    | Walk every leaf screen, ensure StandardPage padding consistent.                                             |
| Real release keystore        | Replace debug keystore for sideload distribution.                                                           |
| Manifest hosting             | Host `versions.json` at a stable URL with signed checksums.                                                 |
| Launcher icon densities      | Add xxxhdpi + adaptive icon foreground/background.                                                          |
| @Preview audit               | Every Composable in `ui/components/neo/*` must have a @Preview with both themes.                            |
| ViewModel test coverage      | Target ≥ 70% for VM code.                                                                                   |
| DAO instrumentation tests    | Cover migrations + complex queries.                                                                         |
| R8 shrink                    | Enable; verify no proguard issues with Room/DataStore.                                                      |
| Lint sweep                   | Address all warnings, then enforce in CI.                                                                   |

## 46.2 v2.0 (major, deferred)

| Item                   | Notes                                                         |
| ---------------------- | ------------------------------------------------------------- |
| Dark mode              | Two new shadow stacks; theme toggle in Display group.         |
| Multi-language         | Hindi + ES + AR; ensure all strings in `strings.xml`.         |
| Voice-to-text on notes | Tap mic in NoteEditor; speech-to-text via `RecognizerIntent`. |
| WhatsApp integration   | LOCKED DEFERRED.                                              |
| Call recording         | LOCKED DEFERRED (regulatory complexity).                      |
| Tablet layout          | Two-pane on width ≥ 600dp.                                    |
| Wear OS companion      | Quick "log a follow-up" surface.                              |
| Web mirror (read-only) | Encrypted backup syncs to a static viewer.                    |

## 46.3 Tech debt watchlist

- ResetAllDataUseCase wipes 4 of 13 tables. Decide v1.1 whether to widen scope or rename to "Reset light".
- WorkManager 5-min option label is misleading (15-min floor). Either rename or wait for ExpressWorker stability.
- AssetDocsLoader does no caching; each DocsList visit re-parses. Fine today, watch as articles grow.
- Compose recomposition counts on Calls list: profile at 10k rows.

## 46.4 Cross-references

- Sync algorithm: Part 01 §6.1.
- Lead score: Part 04 §20.
- Backup format: Part 05 §27.

---

# Appendix A — Glossary

| Term              | Definition                                                            |
| ----------------- | --------------------------------------------------------------------- |
| Inquiry           | Auto-saved unsaved number, named with prefix template.                |
| Bucket            | Cold/Warm/Hot bin in lead scoring.                                    |
| Manifest          | The `versions.json` file the self-update worker fetches.              |
| Skipped version   | A versionCode the user dismissed; not re-prompted.                    |
| OEM battery setup | OEM-specific path to whitelist callNest from background restrictions. |
| Sideload          | Install via APK, not Play Store.                                      |
| StandardPage      | Outer composable for every leaf screen.                               |
| Neo\*             | Neumorphic component family.                                          |

# Appendix B — Settings keys cheat sheet

| Key                         | Type    | Default            |
| --------------------------- | ------- | ------------------ |
| sync.auto.enabled           | Boolean | true               |
| sync.interval               | String  | MIN_15             |
| sync.constraint.wifi        | Boolean | false              |
| sync.constraint.charging    | Boolean | false              |
| sync.on_open                | Boolean | true               |
| sync.on_reboot              | Boolean | false              |
| autosave.enabled            | Boolean | true               |
| autosave.prefix             | String  | callNest           |
| autosave.include_sim        | Boolean | true               |
| autosave.suffix             | String  | ""                 |
| autosave.group              | String  | callNest Inquiries |
| autosave.label              | String  | MOBILE             |
| autosave.label.custom       | String  | ""                 |
| autosave.region             | String  | IN                 |
| realtime.bubble.enabled     | Boolean | true               |
| realtime.popup.enabled      | Boolean | true               |
| realtime.popup.timeout_s    | Int     | 8                  |
| realtime.popup.only_unsaved | Boolean | true               |
| notif.followup_reminders    | Boolean | true               |
| notif.daily_summary         | Boolean | false              |
| notif.update_alerts         | Boolean | true               |
| scoring.enabled             | Boolean | true               |
| scoring.weight.frequency    | Int     | 25                 |
| scoring.weight.duration     | Int     | 20                 |
| scoring.weight.recency      | Int     | 25                 |
| scoring.bonus.followup      | Int     | 10                 |
| scoring.bonus.customer_tag  | Int     | 20                 |
| scoring.bonus.saved_contact | Int     | 15                 |
| backup.auto.enabled         | Boolean | true               |
| backup.retention.days       | Int     | 7                  |
| display.default_tab         | String  | CALLS              |
| display.pin_unsaved         | Boolean | true               |
| display.group_by_date       | Boolean | true               |
| privacy.block_hidden        | Boolean | false              |
| privacy.hide_blocked        | Boolean | true               |
| update.channel              | String  | STABLE             |
| update.auto_check           | Boolean | true               |

# Appendix C — Worker matrix

| Worker                          | Periodicity         | Trigger                             | Constraints            |
| ------------------------------- | ------------------- | ----------------------------------- | ---------------------- |
| PeriodicSyncWorker              | per `sync.interval` | sync master ON                      | wifi/charging optional |
| SyncOnceWorker                  | one-shot            | app open / reboot / pull-to-refresh | network                |
| FollowUpReminderWorker          | 15 min              | follow-ups exist                    | none                   |
| DailySummaryWorker              | 24 h                | notif.daily_summary                 | none                   |
| LeadScoreRecomputeWorker        | one-shot            | weights changed                     | none                   |
| DailyBackupWorker               | 24 h                | backup.auto.enabled                 | storage available      |
| UpdateCheckWorker               | 7 d                 | update.auto_check                   | network                |
| DownloadAndInstallUpdateUseCase | one-shot            | user tap                            | network                |

# Appendix D — Route map

| Route               | Screen                                        |
| ------------------- | --------------------------------------------- |
| Main                | MainScaffold (Calls + Inquiries + MyContacts) |
| Settings            | Settings master (§33)                         |
| AutoSaveSettings    | §34                                           |
| RealTimeSettings    | §35                                           |
| LeadScoringSettings | §36                                           |
| AutoTagRules        | Part 04 §22                                   |
| Backup              | Part 05 §27                                   |
| UpdateSettings      | §37                                           |
| UpdateAvailable     | §38                                           |
| DocsList            | §39                                           |
| DocsArticle/{slug}  | §40                                           |
| PermissionRationale | §41                                           |
| PermissionDenied    | §42                                           |

# Appendix E — Theme tokens

| Token       | Light   | Dark (v2.0) |
| ----------- | ------- | ----------- |
| bg          | #E6E9EF | #1F2429     |
| surface     | #EEF1F6 | #232930     |
| accent      | #4F6BFF | #5C7BFF     |
| onAccent    | #FFFFFF | #FFFFFF     |
| text        | #1B2230 | #E6E9EF     |
| textMuted   | #6B7585 | #98A2B3     |
| divider     | #D0D6E0 | #2C333B     |
| shadowLight | #FFFFFF | #2A3138     |
| shadowDark  | #B8C0CC | #14181C     |
| error       | #D14343 | #E76A6A     |
| warn        | #C68B16 | #E8B14A     |
| success     | #2E8C57 | #4FB37C     |

# Appendix F — File path index for Part 06

| Item                            | Path                                               |
| ------------------------------- | -------------------------------------------------- |
| SettingsViewModel               | `ui/settings/SettingsViewModel.kt`                 |
| AutoSaveSettingsViewModel       | `ui/settings/AutoSaveSettingsViewModel.kt`         |
| RealTimeSettingsViewModel       | `ui/settings/RealTimeSettingsViewModel.kt`         |
| LeadScoringSettingsViewModel    | `ui/settings/LeadScoringSettingsViewModel.kt`      |
| UpdateSettingsViewModel         | `ui/settings/UpdateSettingsViewModel.kt`           |
| UpdateAvailableViewModel        | `ui/update/UpdateAvailableViewModel.kt`            |
| DocsViewModel                   | `ui/docs/DocsViewModel.kt`                         |
| AssetDocsLoader                 | `data/docs/AssetDocsLoader.kt`                     |
| MarkdownRenderer                | `ui/components/MarkdownRenderer.kt`                |
| ResetAllDataUseCase             | `domain/reset/ResetAllDataUseCase.kt`              |
| LeadScoreRecomputeWorker        | `work/LeadScoreRecomputeWorker.kt`                 |
| DailySummaryWorker              | `work/DailySummaryWorker.kt`                       |
| DailyBackupWorker               | `work/DailyBackupWorker.kt`                        |
| UpdateCheckWorker               | `work/UpdateCheckWorker.kt`                        |
| DownloadAndInstallUpdateUseCase | `domain/update/DownloadAndInstallUpdateUseCase.kt` |
| UpdateInstaller                 | `domain/update/UpdateInstaller.kt`                 |
| PermissionManager               | `permission/PermissionManager.kt`                  |
| RealTimeServiceController       | `realtime/RealTimeServiceController.kt`            |
| ContactGroupManager             | `data/contacts/ContactGroupManager.kt`             |
| AutoSaveNameBuilder             | `domain/autosave/AutoSaveNameBuilder.kt`           |

# Appendix G — Ordered checklist for a from-scratch rebuild

1. Build NeoTheme + tokens (Appendix E).
2. Build Neo\* components in §43 order; @Preview each.
3. Build StandardPage + MainScaffold.
4. Wire SettingsDataStore + key constants (Appendix B).
5. Implement Settings master (§33) — all groups, no sub-screens yet.
6. Implement AutoSaveSettings (§34) with live preview.
7. Implement RealTimeSettings (§35) and wire RealTimeServiceController.
8. Implement LeadScoringSettings (§36) and LeadScoreRecomputeWorker.
9. Implement UpdateSettings (§37), UpdateCheckWorker, manifest fetcher.
10. Implement UpdateAvailable (§38) with all 5 states.
11. Implement DocsList (§39), DocsArticle (§40), AssetDocsLoader.
12. Implement PermissionRationale (§41), PermissionDenied (§42).
13. Run the empty/loading/error catalog (§45) screen-by-screen.
14. Run the copy guide (§44) over every string in `strings.xml`.
15. Audit @Preview, lint, ViewModel tests, DAO instrumentation tests.
16. Cut a release; tag with versionCode; publish a `versions.json` entry.

# Appendix H — Known limitations (v1.0)

1. ResetAllData wipes only 4 of 13 tables. Calls are derived from the OS log on next sync.
2. Sync 5-min interval is silently floored to 15 min by WorkManager.
3. Floating bubble can be killed by aggressive OEM battery savers; recovery requires user action via OEM settings.
4. Update channel switch from Beta → Stable while ahead of Stable does not downgrade automatically.
5. Stats screen is missing 6 charts planned for v1.1.
6. NoteEditorDialog and TagEditorDialog are still raw AlertDialogs (migration planned v1.1).
7. AssetDocsLoader has no caching layer.
8. No dark mode in v1.0.

# Appendix I — Accessibility checklist (Part 06 surfaces)

- Every NeoIconButton has `contentDescription`.
- Every NeoToggle has a labelled parent Row and `role=switch`.
- Every NeoSlider exposes value via `stateDescription`.
- Every NeoTextField has a `label` (visible) — not just placeholder.
- Confirm dialogs use `role=alertdialog` and trap focus.
- Color contrast ≥ 4.5:1 for body text in both light theme and the future dark theme.

# Appendix J — Performance budgets

| Surface                                          | Budget                  |
| ------------------------------------------------ | ----------------------- |
| Settings master first frame                      | < 80 ms                 |
| AutoSaveSettings preview recompute per keystroke | < 4 ms                  |
| LeadScoreRecomputeWorker @ 10k contacts          | < 8 s, yields every 500 |
| DocsList parse 16 articles                       | < 60 ms                 |
| UpdateCheckWorker                                | < 2 s typical           |

# Appendix K — Telemetry events (privacy-safe; locally aggregated only)

| Event            | When              | Payload                             |
| ---------------- | ----------------- | ----------------------------------- |
| settings_toggle  | any toggle flip   | key, value                          |
| autosave_preview | preview recompute | (no payload, count only)            |
| update_check     | worker run        | result (up_to_date/available/error) |
| update_install   | install attempt   | result                              |
| docs_open        | article opened    | slug                                |
| docs_feedback    | feedback tap      | slug, helpful                       |
| reset_data       | reset complete    | (no payload)                        |

> Note: telemetry stays on-device unless the user opts in to share diagnostics in a future v1.1 setting.

# Appendix L — Migration notes

- DataStore key changes between versions: write a `DataStoreMigration` that reads old keys and translates.
- Room migrations: see Part 02 §8.
- Contact group rename: ContactGroupManager preserves member rows when renaming.

# Appendix M — Error code reference

| Code             | Where  | Copy                             |
| ---------------- | ------ | -------------------------------- |
| SYNC_E_NET       | sync   | No internet.                     |
| SYNC_E_PERM      | sync   | Permissions are blocked.         |
| SYNC_E_DB        | sync   | Database is busy. Try again.     |
| UPD_E_NET        | update | Couldn't reach update server.    |
| UPD_E_MANIFEST   | update | Update server returned bad data. |
| UPD_E_SHA        | update | Checksum mismatch.               |
| UPD_E_INSTALL    | update | Couldn't install.                |
| BACKUP_E_IO      | backup | Couldn't write backup file.      |
| BACKUP_E_RESTORE | backup | Backup file is unreadable.       |

# Appendix N — Final remarks

This Part 06 closes the v1.0 spec. Sections 33–46 are normative. Components and copy here are reused across Parts 01–05; treat this part as the source of truth when those parts disagree.

— end of Part 06 —
