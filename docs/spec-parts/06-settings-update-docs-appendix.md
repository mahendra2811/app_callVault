# CallVault APP-SPEC — Part 06

## Settings Master + Sub-Settings + Update Screens + Docs + Permission Screens + Neo* Component Reference + Copy Guide + Appendices

> Audience: a UX engineer rebuilding CallVault from scratch.
> This part is self-contained. Cross-references to Part 01 (sync algorithm), Part 02 (data layer), Part 03 (real-time services), Part 04 (lead scoring), and Part 05 (backup/export) are noted explicitly.
> All copy strings are normative. All wireframes are reference. All component signatures are normative.

---

## Table of contents (sections 33–46)

| # | Title | Approx lines |
|---|---|---|
| 33 | Settings master screen | 600 |
| 34 | AutoSaveSettings screen | 350 |
| 35 | RealTimeSettings screen | 300 |
| 36 | LeadScoringSettings screen | 300 |
| 37 | UpdateSettings screen | 300 |
| 38 | UpdateAvailable screen | 350 |
| 39 | DocsList screen | 250 |
| 40 | DocsArticle screen | 200 |
| 41 | PermissionRationale screen | 200 |
| 42 | PermissionDenied screen | 150 |
| 43 | Neo* component reference | 600 |
| 44 | Copy / voice guide | 250 |
| 45 | Empty / loading / error state catalog | 250 |
| 46 | Future-proofing notes | 150 |

---

# 33 — Settings master screen

## 33.1 Purpose

The Settings master screen is the single hub for every persistent preference in CallVault. It exposes:

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

| Option | Stored value | Worker policy |
|---|---|---|
| Manual | `MANUAL` | No worker; user uses pull-to-refresh |
| 5 min | `MIN_5` | PeriodicWorkRequest 15min (Android floor) |
| 15 min | `MIN_15` | PeriodicWorkRequest 15min |
| 1 hour | `HOUR_1` | PeriodicWorkRequest 1h |
| 12 hours | `HOUR_12` | PeriodicWorkRequest 12h |
| 24 hours | `DAY_1` | PeriodicWorkRequest 24h |
| Daily 2 AM | `DAILY_2AM` | PeriodicWorkRequest 24h, initial delay aligned |

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

| Key | Default | Effect |
|---|---|---|
| `notif.followup_reminders` | true | Enables FollowUpReminderWorker (15min check) |
| `notif.daily_summary` | false | Schedule/cancel DailySummaryWorker @ 9AM |
| `notif.update_alerts` | true | Allow UpdateCheckWorker to post a notification when an update is found |

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

| Key | Default | Effect |
|---|---|---|
| `backup.auto.enabled` | true | Schedule/cancel `DailyBackupWorker` |
| `backup.retention.days` | 7 | Slider 1–30. Trims older files on each run. |

Sub-link "Open backup screen" → `Backup` (Part 05 §27).

### 33.11.1 Daily backup worker

- PeriodicWorkRequest, 24h, initial delay aligned to 03:00.
- Writes a zip into `app-private/backups/cv-YYYYMMDD-HHmm.zip`.
- After write, deletes any zip older than `retention.days`.

## 33.12 Group: Display

| Key | Default | Effect |
|---|---|---|
| `display.default_tab` | `CALLS` (locked) | Reserved; v1 only Calls |
| `display.pin_unsaved` | true | Calls list pins unsaved-number rows above saved |
| `display.group_by_date` | true | Calls list shows section headers Today/Yesterday/Earlier |

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

| Label | Destination |
|---|---|
| Getting started | DocsArticle/01-getting-started |
| FAQ | DocsList |
| Permission troubleshooting | DocsArticle/02-permissions |
| OEM battery setup | DocsArticle/12-oem-battery |
| Contact support | mailto:support@callvault.app |
| Privacy policy | DocsArticle/15-privacy |
| Terms of use | DocsArticle/15-privacy#terms |
| What's new | DocsArticle/14-self-update |

## 33.16 Group: About

Read-only rows:

- Version: `BuildConfig.VERSION_NAME` (build `BuildConfig.VERSION_CODE`)
- Last sync: formatted via `DateFormatter.relative(lastSyncTimestamp)`
- DB size: `context.getDatabasePath("callvault.db").length()` formatted to MB
- Calls tracked: `CallLogDao.count()`
- Auto-saved: `ContactMetaDao.countAutoSaved()`

## 33.17 Edge cases (master)

- DataStore corruption: read returns defaults; warn via Crashlytics breadcrumb.
- "Reset all data" wipes only 4 of 13 tables (notes, note_history, search_history, skipped_versions). Calls/contacts are derived from the OS log on next sync. This is intentional but documented as a known limitation in Part 06 §46.
- Toggle flicker: each row reflects local state immediately; DataStore write is async.

## 33.18 Copy table (50 strings)

| Key | EN |
|---|---|
| settings.title | Settings |
| settings.group.sync | Sync |
| settings.sync.auto | Automatic sync |
| settings.sync.interval | Interval |
| settings.sync.interval.manual | Manual |
| settings.sync.interval.5m | Every 5 minutes |
| settings.sync.interval.15m | Every 15 minutes |
| settings.sync.interval.1h | Every hour |
| settings.sync.interval.12h | Every 12 hours |
| settings.sync.interval.24h | Every 24 hours |
| settings.sync.interval.daily2am | Daily at 2 AM |
| settings.sync.wifi_only | Wi-Fi only |
| settings.sync.charging_only | Charging only |
| settings.sync.on_open | Sync on app open |
| settings.sync.on_reboot | Sync on reboot |
| settings.group.autosave | Auto-save inquiries |
| settings.autosave.open | Open auto-save settings |
| settings.group.realtime | Real-time features |
| settings.realtime.open | Open real-time settings |
| settings.group.notifications | Notifications |
| settings.notif.followup | Follow-up reminders |
| settings.notif.daily_summary | Daily summary at 9 AM |
| settings.notif.updates | Update alerts |
| settings.group.scoring | Lead scoring |
| settings.scoring.open | Tune lead scoring |
| settings.group.rules | Auto-tag rules |
| settings.rules.count | %d active rules |
| settings.group.backup | Backup & restore |
| settings.backup.auto | Auto-backup |
| settings.backup.retention | Keep last %d days |
| settings.backup.open | Open backup screen |
| settings.group.display | Display |
| settings.display.default_tab | Default tab |
| settings.display.default_tab.locked | Calls (locked in v1) |
| settings.display.pin_unsaved | Pin unsaved on Calls |
| settings.display.group_by_date | Group by date |
| settings.group.privacy | Privacy |
| settings.privacy.block_hidden | Block hidden numbers |
| settings.privacy.hide_blocked | Hide blocked from list |
| settings.privacy.clear_search | Clear search history |
| settings.privacy.clear_notes | Clear all notes |
| settings.privacy.reset | Reset all data |
| settings.privacy.reset.confirm1.title | Reset all data? |
| settings.privacy.reset.confirm1.body | This wipes notes, search history, and skipped update versions. Re-sync will rebuild calls from your OS log. |
| settings.privacy.reset.confirm2.title | Type DELETE to confirm |
| settings.privacy.reset.confirm2.hint | DELETE |
| settings.privacy.reset.confirm.action | Reset |
| settings.group.updates | App updates |
| settings.updates.open | Update settings |
| settings.group.help | Help & docs |
| settings.help.start | Getting started |
| settings.help.faq | Frequently asked questions |
| settings.help.permissions | Permission troubleshooting |
| settings.help.oem | OEM battery setup |
| settings.help.support | Contact support |
| settings.help.privacy | Privacy policy |
| settings.help.terms | Terms of use |
| settings.group.about | About |
| settings.about.version | Version %s (build %d) |
| settings.about.last_sync | Last sync %s |
| settings.about.db_size | Database size %s |
| settings.about.calls_tracked | Calls tracked %d |
| settings.about.auto_saved | Auto-saved %d |

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

Tunes how CallVault names new contacts when it auto-saves an unsaved number. The combined name template is:

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
│ How CallVault names new inquiries  💡    │
├──────────────────────────────────────────┤
│  Auto-save unsaved numbers     [ ●   ]   │
│                                          │
│  Prefix                                  │
│  ┌──────────────────────────┐            │
│  │ callVault                │            │
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
│  │ callVault-s1 +91 98765 43210     │   │
│  └──────────────────────────────────┘   │
│                                          │
│  Group name                              │
│  ┌──────────────────────────┐  [Apply]   │
│  │ CallVault Inquiries      │            │
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

- `StandardPage(title="Auto-save", description="How CallVault names new inquiries", emoji="💡")`
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

| Key | Default |
|---|---|
| `autosave.enabled` | true |
| `autosave.prefix` | `callVault` |
| `autosave.include_sim` | true |
| `autosave.suffix` | (empty) |
| `autosave.group` | `CallVault Inquiries` |
| `autosave.label` | `MOBILE` |
| `autosave.label.custom` | (empty) |
| `autosave.region` | `IN` |

## 34.8 Edge cases

- Prefix contains `/` `\` `:` `*` `?` `"` `<` `>` `|`: stripped via `ContactNameSanitizer.sanitize` before write.
- Region invalid (e.g. `XZ`): validate against `libphonenumber.PhoneNumberUtil.getInstance().supportedRegions()`. On invalid, show inline error and keep last valid value.
- Custom label blank when "Other" selected: fallback to `Other`.
- Long prefix (>30 chars): inline warning "Long prefixes may be truncated by your phone".
- Apply group while another sync is mutating contacts: queue via single-thread executor.
- Preview during typing must not cause jank: keep AutoSaveNameBuilder free of allocations on hot path.

## 34.9 Copy table (20 strings)

| Key | EN |
|---|---|
| autosave.title | Auto-save |
| autosave.subtitle | How CallVault names new inquiries |
| autosave.master | Auto-save unsaved numbers |
| autosave.prefix.label | Prefix |
| autosave.prefix.hint | callVault |
| autosave.sim_tag | Include SIM tag (-s1/-s2) |
| autosave.suffix.label | Suffix |
| autosave.suffix.hint | (none) |
| autosave.preview.title | Preview |
| autosave.group.label | Group name |
| autosave.group.apply | Apply |
| autosave.label.title | Default phone label |
| autosave.label.mobile | Mobile |
| autosave.label.work | Work |
| autosave.label.home | Home |
| autosave.label.other | Other |
| autosave.label.custom.hint | Custom label |
| autosave.region.label | Default region for normalization |
| autosave.region.invalid | Unknown region. Use a 2-letter code like IN or US. |
| autosave.warn.long_prefix | Long prefixes may be truncated by your phone. |

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

| Key | Default |
|---|---|
| `realtime.bubble.enabled` | true |
| `realtime.popup.enabled` | true |
| `realtime.popup.timeout_s` | 8 |
| `realtime.popup.only_unsaved` | true |

## 35.6 Edge cases

- Overlay permission revoked while service is running: `WindowManager.addView` throws `BadTokenException`; service catches, stops itself, posts a snackbar via SettingsViewModel "Display permission was revoked. Re-grant to use the floating bubble."
- OEM battery saver kills service: a banner appears on Calls screen ("Background services were restricted by your phone. Open OEM battery setup.")
- Toggle during active call: changes apply on next call; current call keeps its bubble.

## 35.7 Copy table (15 strings)

| Key | EN |
|---|---|
| realtime.title | Real-time |
| realtime.subtitle | Floating bubble and post-call popup |
| realtime.bubble | Floating bubble during calls |
| realtime.popup | Post-call popup |
| realtime.popup.timeout | Popup timeout |
| realtime.popup.timeout.fmt | %ds |
| realtime.popup.only_unsaved | Show only for unsaved numbers |
| realtime.permission.title | Display over other apps |
| realtime.permission.granted | Granted |
| realtime.permission.notgranted | Not granted |
| realtime.permission.open | Open Settings |
| realtime.warn.revoked | Display permission was revoked. Re-grant to use the floating bubble. |
| realtime.warn.killed | Background services were restricted. Open OEM battery setup. |
| realtime.banner.oem | Open OEM battery setup |
| realtime.banner.dismiss | Dismiss |

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

| Slider | Range | Default |
|---|---|---|
| Call frequency | 0..50 | 25 |
| Total duration | 0..50 | 20 |
| Recency | 0..50 | 25 |
| Has follow-up | 0..25 | 10 |
| Customer tag | 0..30 | 20 |
| Saved contact | 0..25 | 15 |

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

| Key | EN |
|---|---|
| scoring.title | Lead scoring |
| scoring.subtitle | Tune what makes a hot lead |
| scoring.master | Use lead scoring |
| scoring.weight.frequency | Weight: Call frequency |
| scoring.weight.duration | Weight: Total duration |
| scoring.weight.recency | Weight: Recency |
| scoring.bonus.followup | Bonus: Has follow-up |
| scoring.bonus.customer_tag | Bonus: Customer tag |
| scoring.bonus.saved_contact | Bonus: Saved contact |
| scoring.buckets.title | Buckets (locked) |
| scoring.buckets.cold | Cold below 30 |
| scoring.buckets.warm | Warm 30 to 70 |
| scoring.buckets.hot | Hot above 70 |
| scoring.reset | Reset to defaults |
| scoring.snackbar.recomputing | Recomputing scores… |

## 36.8 Cross-references

- Formula: Part 04 §20.
- ContactMeta schema: Part 02 §9.

---

# 37 — UpdateSettings screen

## 37.1 Purpose

Manages the self-update channel and auto-check policy. CallVault is sideloaded; `UpdateCheckWorker` polls a JSON manifest hosted by the developer.

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

| Key | Default |
|---|---|
| `update.channel` | `STABLE` |
| `update.auto_check` | true |

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

| Key | EN |
|---|---|
| update.title | App updates |
| update.subtitle | Stable or beta channel, auto-check |
| update.current | Current version %s (build %d) |
| update.channel | Channel |
| update.channel.stable | Stable |
| update.channel.beta | Beta |
| update.auto_check | Check for updates automatically |
| update.check_now | Check now |
| update.last_checked | Last checked %s |
| update.clear_skipped | Clear skipped versions |
| update.snack.up_to_date | You're on the latest version. |
| update.snack.available | Update available: v%s |
| update.snack.offline | No internet. Try again later. |
| update.snack.bad_manifest | Update server returned bad data. |
| update.snack.beta_latest | You're on the latest beta. |
| update.snack.cleared | Skipped versions cleared. |

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

| Key | EN |
|---|---|
| upd.title | Update |
| upd.available.title | Update to v%s |
| upd.btn.update | Update now |
| upd.btn.skip | Skip this version |
| upd.btn.later | Later |
| upd.downloading | Downloading… |
| upd.btn.cancel | Cancel |
| upd.ready | Ready to install v%s |
| upd.btn.install | Install now |
| upd.installing | Opening installer… |
| upd.error.title | Couldn't update |
| upd.error.reason | Reason: %s |
| upd.btn.retry | Retry |
| upd.error.install_perm | Allow install from unknown sources to continue. |
| upd.btn.open_settings | Open Settings |

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
│ 16 guides about CallVault 📚             │
├──────────────────────────────────────────┤
│ 👋 Getting started                       │
│    Updated 2026-03-12                    │
│    Welcome to CallVault. Here's how…   ➤ │
├──────────────────────────────────────────┤
│ 🔐 Permissions                           │
│    Updated 2026-02-04                    │
│    CallVault needs phone, contacts…    ➤ │
├──────────────────────────────────────────┤
│ … (15 more)                              │
└──────────────────────────────────────────┘
```

## 39.3 Components

- `StandardPage("Help", "16 guides about CallVault", "📚")`
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

| Key | EN |
|---|---|
| docs.title | Help |
| docs.subtitle.fmt | %d guides about CallVault |
| docs.row.updated | Updated %s |
| docs.empty.title | No help articles |
| docs.empty.body | Help articles weren't bundled in this build. |

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
│ Why CallVault needs each one 📖          │
├──────────────────────────────────────────┤
│ Updated 2026-02-04                       │
│                                          │
│ # Permissions                            │
│ CallVault needs the following…           │
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

| Key | EN |
|---|---|
| docs.article.updated | Updated %s |
| docs.feedback.q | Was this helpful? |
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
│ Why CallVault needs access 🔐            │
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

- `StandardPage("Permissions", "Why CallVault needs access", "🔐")`
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

| Key | EN |
|---|---|
| perm.title | Permissions |
| perm.subtitle | Why CallVault needs access |
| perm.phone_state | Phone state |
| perm.phone_state.why | To detect when you're on a call. |
| perm.read_log | Read call log |
| perm.read_log.why | To read your call history. |
| perm.contacts | Contacts |
| perm.contacts.why | To match numbers to names. |
| perm.notif | Post notifications |
| perm.notif.why | To remind you of follow-ups. |
| perm.grant | Grant permissions |

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
│  CallVault can't run without phone,      │
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

| Key | EN |
|---|---|
| perm.denied.title | Permissions are blocked |
| perm.denied.body | CallVault can't run without phone, call log, and contacts access. Open System Settings to enable them. |
| perm.denied.open | Open Settings |

---

# 43 — Neo* component reference

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

| Param | Notes |
|---|---|
| elevation | `Flat`, `Raised`, `Sunken`. Maps to a paired light/dark shadow stack. |
| shape | Defaults to a 16dp rounded card shape. |
| pressed | When true, swaps to inverted (sunken) shadow stack. |

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

Pitfalls: nesting `Modifier.clickable` *and* passing `onClick` doubles the ripple — pick one.

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

Rules: every leaf screen in CallVault uses StandardPage as its outermost composable (except Main, which uses MainScaffold).

## 43.26 MainScaffold

- File: `ui/components/MainScaffold.kt`
- Hosts top bar, bottom nav, snackbar host, FAB slot, top-line-loader slot.

## 43.27 Component matrix

| Component | a11y | Theme tokens used |
|---|---|---|
| NeoSurface | n/a | bg, shadowLight, shadowDark |
| NeoCard | role="button" if onClick | bg, shadow* |
| NeoButton | role="button" | accent, onAccent, disabledAlpha |
| NeoIconButton | contentDescription required | accent |
| NeoChip | role="checkbox" | accent, surface |
| NeoToggle | role="switch" | accent, track |
| NeoSlider | role="slider" | accent |
| NeoSearchBar | role="search" | bg, hint |
| NeoFAB | role="button" | accent |
| NeoTabBar | role="tablist" | accent |
| NeoTopBar | landmark="banner" | bg |
| NeoBottomSheet | trapFocus | bg, scrim |
| NeoTextField | role="textbox" | bg, border, error |
| NeoProgressBar | role="progressbar" | accent |
| NeoLoader | role="progressbar" | accent |
| NeoTopLineLoader | role="progressbar" | accent |
| NeoBadge | role="status" | accent |
| NeoAvatar | contentDescription | derived |
| NeoDivider | n/a | divider |
| NeoEmptyState | n/a | text |
| NeoHelpIcon | role="button" | hint |
| NeoDialog | role="alertdialog" | bg, scrim |

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

| Screen | Tone |
|---|---|
| Onboarding | Warm + reassuring |
| Errors | Actionable; never blame the user |
| Empty states | Light + actionable |
| Settings | Descriptive + neutral |
| Docs | Clear + hands-on |
| Dialogs | Direct; no exclamations |

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

| Bad | Good |
|---|---|
| Oops! Something went wrong! | Couldn't load. Try again. |
| Awesome! Saved! | Saved. |
| Sync failed because the network could not be reached. | Couldn't sync. No internet. |
| The note could not be deleted. | Couldn't delete the note. |
| You have no calls. | No calls yet. |
| Click here to grant permissions. | Grant permissions |
| Are you sure you want to do this? | Reset all data? |
| Yes | Reset |
| No | Cancel |
| Please enter a valid number. | Use digits only. |
| Loading… please wait. | Loading… |
| Your changes were saved successfully. | Saved. |
| Your changes were not saved. | Couldn't save. Try again. |
| Tap to add your first contact! | Add a contact |
| You have not set up backups. | No backups yet. |
| Backups are saved! | Backup created. |
| Failed to export. | Couldn't export. Try again. |
| There are no results matching your search. | No matches. |
| Permissions are required for the app. | CallVault needs phone access. |
| Couldn't load due to error 502. | Couldn't load. Try again. |
| Are you really sure you want to delete this? | Delete this note? |
| Hooray! | (omit) |
| Update was a success. | Updated to v%s. |
| Update was a failure. | Couldn't update. %s. |
| Click here to retry. | Retry |
| Settings saved successfully. | Saved. |
| Network connection lost. | No internet. |
| Hidden numbers found. | %d hidden numbers. |
| Sync is in progress, please don't close the app. | Syncing… |
| Backup file is corrupted. | Backup file is unreadable. |
| Your phone might restrict background tasks. | Background tasks may be restricted by your phone. |
| Tap below to continue. | Continue |
| You have unsaved changes. | Unsaved changes. |
| Field is required. | Required. |
| Phone number must be valid. | Use a valid phone number. |
| Please grant the permission to continue. | Grant permissions to continue. |
| Notes was successfully cleared. | Notes cleared. |
| Failure during contact import. | Couldn't import contacts. |
| File too big! | File is too large. |
| Calculation error occurred. | Couldn't compute the score. |
| You're now on Beta channel! | Switched to Beta. |
| Update server unreachable. | Couldn't reach update server. |
| All set! | Done. |
| Operation failed. | Couldn't complete. Try again. |
| It worked! | Done. |
| Please give us a moment. | One moment… |
| Apologies for the inconvenience. | (omit) |
| You're all good to go. | Ready. |
| Save was performed. | Saved. |
| Could not load the document. | Couldn't open the document. |
| Failed: timeout. | Timed out. Try again. |

## 44.5 Punctuation rules

- Sentences end with `.` except titles.
- Em dashes `—` only in long descriptions, not in buttons.
- Avoid `!` everywhere except onboarding's first welcome line.
- Use `…` for in-progress states, not `...`.

## 44.6 Numbers + dates

- Use `%d` placeholders for plurals; ICU select where the language requires.
- Dates: `2026-04-30` for read-only "Last sync"; relative ("2 min ago") for inline.

## 44.7 Plural rules

| One | Other |
|---|---|
| 1 call | %d calls |
| 1 day | %d days |
| 1 minute | %d minutes |

## 44.8 Cross-references

- All copy keys are reused across Parts 01–06; this guide is the source of truth for any new strings.

---

# 45 — Empty / loading / error state catalog

## 45.1 Pattern overview

| State | Component | When |
|---|---|---|
| Empty | NeoEmptyState | Query returned no rows |
| Loading | NeoLoader / NeoTopLineLoader / skeleton | Async fetch in flight |
| Error | NeoEmptyState (sad icon) / Snackbar / inline | Async fetch failed |

## 45.2 Empty state catalog

| Screen | Title | Body | CTA |
|---|---|---|---|
| Calls | No calls yet | New calls will appear here. | (none) |
| Calls (filtered) | No matches | Try a different filter. | Clear filters |
| Search | No matches | Try a shorter query. | (none) |
| MyContacts | No contacts | Saved contacts appear here. | Sync now |
| Inquiries | No inquiries yet | Auto-saved numbers appear here. | (none) |
| Notes | No notes | Add a note to remember context. | Add note |
| FollowUps | No follow-ups | Schedule one from a call. | (none) |
| Tags | No tags | Tags help you filter calls. | New tag |
| Rules | No rules | Auto-tag rules go here. | New rule |
| Backups | No backups | Auto-backup creates one daily. | Back up now |
| Stats | No data yet | Stats appear after a few calls. | (none) |
| Updates | No updates | You're on the latest version. | Check now |
| Docs | No help articles | Help articles weren't bundled. | (none) |

## 45.3 Loading state catalog

| Screen | Style | Why |
|---|---|---|
| Calls | NeoTopLineLoader during sync, list visible | Don't block; user can browse cached |
| Inquiries | NeoTopLineLoader | Same |
| MyContacts | NeoTopLineLoader | Same |
| Stats | Full-page skeleton | Initial load only |
| Backups | NeoLoader inline on row | One-off action |
| Update download | NeoProgressBar (determinate) | Has known progress |
| Update install | NeoLoader (indeterminate) | Unknown duration |
| Search | NeoLoader inline next to bar | Debounced |
| Onboarding | NeoLoader full-page | First sync |

## 45.4 Error state catalog

| Screen | Style | Copy |
|---|---|---|
| Calls (sync error) | Snackbar | Couldn't sync. %s. [Retry] |
| MyContacts | Snackbar | Couldn't sync. [Retry] |
| Backup creation | Snackbar | Couldn't create backup. [Retry] |
| Backup restore | Full-page | Couldn't restore. The file is unreadable. |
| Update download | Full-page | Couldn't download. %s. [Retry] |
| Update install | Full-page | Couldn't install. %s. [Retry] |
| Docs article | Inline fallback | Couldn't render this article. |
| Settings write | Snackbar | Couldn't save. Try again. |

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

| Item | Notes |
|---|---|
| 6 missing stats charts | Hourly distribution, top callers, week-over-week, missed-call ratio, average duration, hot-lead conversion. |
| PDF chart embedding | Export of stats screen with charts as a single PDF. |
| NoteEditorDialog → NeoDialog | Migrate from raw AlertDialog. |
| TagEditorDialog → NeoDialog | Same. |
| Padding/empty-state audit | Walk every leaf screen, ensure StandardPage padding consistent. |
| Real release keystore | Replace debug keystore for sideload distribution. |
| Manifest hosting | Host `versions.json` at a stable URL with signed checksums. |
| Launcher icon densities | Add xxxhdpi + adaptive icon foreground/background. |
| @Preview audit | Every Composable in `ui/components/neo/*` must have a @Preview with both themes. |
| ViewModel test coverage | Target ≥ 70% for VM code. |
| DAO instrumentation tests | Cover migrations + complex queries. |
| R8 shrink | Enable; verify no proguard issues with Room/DataStore. |
| Lint sweep | Address all warnings, then enforce in CI. |

## 46.2 v2.0 (major, deferred)

| Item | Notes |
|---|---|
| Dark mode | Two new shadow stacks; theme toggle in Display group. |
| Multi-language | Hindi + ES + AR; ensure all strings in `strings.xml`. |
| Voice-to-text on notes | Tap mic in NoteEditor; speech-to-text via `RecognizerIntent`. |
| WhatsApp integration | LOCKED DEFERRED. |
| Call recording | LOCKED DEFERRED (regulatory complexity). |
| Tablet layout | Two-pane on width ≥ 600dp. |
| Wear OS companion | Quick "log a follow-up" surface. |
| Web mirror (read-only) | Encrypted backup syncs to a static viewer. |

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

| Term | Definition |
|---|---|
| Inquiry | Auto-saved unsaved number, named with prefix template. |
| Bucket | Cold/Warm/Hot bin in lead scoring. |
| Manifest | The `versions.json` file the self-update worker fetches. |
| Skipped version | A versionCode the user dismissed; not re-prompted. |
| OEM battery setup | OEM-specific path to whitelist CallVault from background restrictions. |
| Sideload | Install via APK, not Play Store. |
| StandardPage | Outer composable for every leaf screen. |
| Neo* | Neumorphic component family. |

# Appendix B — Settings keys cheat sheet

| Key | Type | Default |
|---|---|---|
| sync.auto.enabled | Boolean | true |
| sync.interval | String | MIN_15 |
| sync.constraint.wifi | Boolean | false |
| sync.constraint.charging | Boolean | false |
| sync.on_open | Boolean | true |
| sync.on_reboot | Boolean | false |
| autosave.enabled | Boolean | true |
| autosave.prefix | String | callVault |
| autosave.include_sim | Boolean | true |
| autosave.suffix | String | "" |
| autosave.group | String | CallVault Inquiries |
| autosave.label | String | MOBILE |
| autosave.label.custom | String | "" |
| autosave.region | String | IN |
| realtime.bubble.enabled | Boolean | true |
| realtime.popup.enabled | Boolean | true |
| realtime.popup.timeout_s | Int | 8 |
| realtime.popup.only_unsaved | Boolean | true |
| notif.followup_reminders | Boolean | true |
| notif.daily_summary | Boolean | false |
| notif.update_alerts | Boolean | true |
| scoring.enabled | Boolean | true |
| scoring.weight.frequency | Int | 25 |
| scoring.weight.duration | Int | 20 |
| scoring.weight.recency | Int | 25 |
| scoring.bonus.followup | Int | 10 |
| scoring.bonus.customer_tag | Int | 20 |
| scoring.bonus.saved_contact | Int | 15 |
| backup.auto.enabled | Boolean | true |
| backup.retention.days | Int | 7 |
| display.default_tab | String | CALLS |
| display.pin_unsaved | Boolean | true |
| display.group_by_date | Boolean | true |
| privacy.block_hidden | Boolean | false |
| privacy.hide_blocked | Boolean | true |
| update.channel | String | STABLE |
| update.auto_check | Boolean | true |

# Appendix C — Worker matrix

| Worker | Periodicity | Trigger | Constraints |
|---|---|---|---|
| PeriodicSyncWorker | per `sync.interval` | sync master ON | wifi/charging optional |
| SyncOnceWorker | one-shot | app open / reboot / pull-to-refresh | network |
| FollowUpReminderWorker | 15 min | follow-ups exist | none |
| DailySummaryWorker | 24 h | notif.daily_summary | none |
| LeadScoreRecomputeWorker | one-shot | weights changed | none |
| DailyBackupWorker | 24 h | backup.auto.enabled | storage available |
| UpdateCheckWorker | 7 d | update.auto_check | network |
| DownloadAndInstallUpdateUseCase | one-shot | user tap | network |

# Appendix D — Route map

| Route | Screen |
|---|---|
| Main | MainScaffold (Calls + Inquiries + MyContacts) |
| Settings | Settings master (§33) |
| AutoSaveSettings | §34 |
| RealTimeSettings | §35 |
| LeadScoringSettings | §36 |
| AutoTagRules | Part 04 §22 |
| Backup | Part 05 §27 |
| UpdateSettings | §37 |
| UpdateAvailable | §38 |
| DocsList | §39 |
| DocsArticle/{slug} | §40 |
| PermissionRationale | §41 |
| PermissionDenied | §42 |

# Appendix E — Theme tokens

| Token | Light | Dark (v2.0) |
|---|---|---|
| bg | #E6E9EF | #1F2429 |
| surface | #EEF1F6 | #232930 |
| accent | #4F6BFF | #5C7BFF |
| onAccent | #FFFFFF | #FFFFFF |
| text | #1B2230 | #E6E9EF |
| textMuted | #6B7585 | #98A2B3 |
| divider | #D0D6E0 | #2C333B |
| shadowLight | #FFFFFF | #2A3138 |
| shadowDark | #B8C0CC | #14181C |
| error | #D14343 | #E76A6A |
| warn | #C68B16 | #E8B14A |
| success | #2E8C57 | #4FB37C |

# Appendix F — File path index for Part 06

| Item | Path |
|---|---|
| SettingsViewModel | `ui/settings/SettingsViewModel.kt` |
| AutoSaveSettingsViewModel | `ui/settings/AutoSaveSettingsViewModel.kt` |
| RealTimeSettingsViewModel | `ui/settings/RealTimeSettingsViewModel.kt` |
| LeadScoringSettingsViewModel | `ui/settings/LeadScoringSettingsViewModel.kt` |
| UpdateSettingsViewModel | `ui/settings/UpdateSettingsViewModel.kt` |
| UpdateAvailableViewModel | `ui/update/UpdateAvailableViewModel.kt` |
| DocsViewModel | `ui/docs/DocsViewModel.kt` |
| AssetDocsLoader | `data/docs/AssetDocsLoader.kt` |
| MarkdownRenderer | `ui/components/MarkdownRenderer.kt` |
| ResetAllDataUseCase | `domain/reset/ResetAllDataUseCase.kt` |
| LeadScoreRecomputeWorker | `work/LeadScoreRecomputeWorker.kt` |
| DailySummaryWorker | `work/DailySummaryWorker.kt` |
| DailyBackupWorker | `work/DailyBackupWorker.kt` |
| UpdateCheckWorker | `work/UpdateCheckWorker.kt` |
| DownloadAndInstallUpdateUseCase | `domain/update/DownloadAndInstallUpdateUseCase.kt` |
| UpdateInstaller | `domain/update/UpdateInstaller.kt` |
| PermissionManager | `permission/PermissionManager.kt` |
| RealTimeServiceController | `realtime/RealTimeServiceController.kt` |
| ContactGroupManager | `data/contacts/ContactGroupManager.kt` |
| AutoSaveNameBuilder | `domain/autosave/AutoSaveNameBuilder.kt` |

# Appendix G — Ordered checklist for a from-scratch rebuild

1. Build NeoTheme + tokens (Appendix E).
2. Build Neo* components in §43 order; @Preview each.
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

| Surface | Budget |
|---|---|
| Settings master first frame | < 80 ms |
| AutoSaveSettings preview recompute per keystroke | < 4 ms |
| LeadScoreRecomputeWorker @ 10k contacts | < 8 s, yields every 500 |
| DocsList parse 16 articles | < 60 ms |
| UpdateCheckWorker | < 2 s typical |

# Appendix K — Telemetry events (privacy-safe; locally aggregated only)

| Event | When | Payload |
|---|---|---|
| settings_toggle | any toggle flip | key, value |
| autosave_preview | preview recompute | (no payload, count only) |
| update_check | worker run | result (up_to_date/available/error) |
| update_install | install attempt | result |
| docs_open | article opened | slug |
| docs_feedback | feedback tap | slug, helpful |
| reset_data | reset complete | (no payload) |

> Note: telemetry stays on-device unless the user opts in to share diagnostics in a future v1.1 setting.

# Appendix L — Migration notes

- DataStore key changes between versions: write a `DataStoreMigration` that reads old keys and translates.
- Room migrations: see Part 02 §8.
- Contact group rename: ContactGroupManager preserves member rows when renaming.

# Appendix M — Error code reference

| Code | Where | Copy |
|---|---|---|
| SYNC_E_NET | sync | No internet. |
| SYNC_E_PERM | sync | Permissions are blocked. |
| SYNC_E_DB | sync | Database is busy. Try again. |
| UPD_E_NET | update | Couldn't reach update server. |
| UPD_E_MANIFEST | update | Update server returned bad data. |
| UPD_E_SHA | update | Checksum mismatch. |
| UPD_E_INSTALL | update | Couldn't install. |
| BACKUP_E_IO | backup | Couldn't write backup file. |
| BACKUP_E_RESTORE | backup | Backup file is unreadable. |

# Appendix N — Final remarks

This Part 06 closes the v1.0 spec. Sections 33–46 are normative. Components and copy here are reused across Parts 01–05; treat this part as the source of truth when those parts disagree.

— end of Part 06 —
