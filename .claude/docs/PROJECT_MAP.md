# callNest — Project Map

One-page mental model of `app/src/main/`. Last refreshed 2026-05-06. **245 Kotlin files**, single Gradle module `app`, package `com.callNest.app`.

## Layering (strict — enforced by import discipline)

```
ui/      → may import domain/, util/   ❌ never data/
domain/  → pure Kotlin, no Android      ❌ no android.* anywhere in domain/model/ or domain/usecase/
data/    → implements domain/repository/ interfaces, bridges Android
util/    → leaf-level helpers used by both ui/ and data/
di/      → Hilt modules only
```

## Tree (annotated)

```
app/src/main/java/com/callNest/app/
├── data/                                         repo impls + Android plumbing
│   ├── analytics/         PostHog wrapper (cloud pivot 2026-05-05)
│   ├── auth/              Supabase auth bridge (gates the app, see ui/screen/auth/)
│   ├── backup/            encrypted Tink ZIP backup (.cvb)
│   ├── event/             cross-process event bus
│   ├── export/            Excel/CSV/PDF/JSON/vCard exporters; only Excel is reachable today
│   ├── local/
│   │   ├── converter/     Room TypeConverters (lists, sets, enums, Instant)
│   │   ├── dao/           9 DAOs — calls, contacts, notes, tags, rules, follow-ups, search-history, lead-score, skipped-update
│   │   ├── entity/        Room entities (one per DAO + a few embedded)
│   │   ├── fts/            CallFts4 virtual-table + sync trigger
│   │   ├── mapper/        Entity ⇆ domain model mappers
│   │   ├── migration/     Room MIGRATIONS array
│   │   └── seed/          Default tags + lead-score weights
│   ├── prefs/             SettingsDataStore (Proto-DataStore) + SecurePrefs (EncryptedSharedPreferences for backup passphrase)
│   ├── push/              FCM token wiring (cloud pivot)
│   ├── repository/        13 repository impls (one per domain/repository/ interface, plus a few internal)
│   ├── service/
│   │   ├── alarm/         Exact-alarm scheduling for sub-15-min sync (battery cost — see DECISIONS)
│   │   └── overlay/       Floating bubble + post-call popup window
│   ├── system/            ContactSaver, CallLogReader, SimReader (Android system wrappers)
│   ├── update/            versions.json fetch + APK download + PackageInstaller
│   └── work/              WorkManager workers (Sync, DailySummary, AutoBackup, UpdateCheck)
│
├── domain/                                       pure Kotlin
│   ├── model/             13 sealed/data classes — Call, Tag, Note, AutoTagRule, RuleCondition, RuleAction, FilterState, LeadScore, ExportConfig, ContactMeta, AuthSession, StatsModels, SyncResult
│   ├── repository/        8 interfaces — Auth, Call, Contact, Note, Tag, AutoTagRule, Settings, Update
│   └── usecase/           sync, lead-score, reset-all, sync-progress-bus, etc.
│
├── ui/
│   ├── components/neo/    25 design-system primitives (see UI_GUIDE.md)
│   ├── navigation/        callNestNavHost, Destinations (sealed), MainScaffold (4-tab), LocalMainTabNav
│   ├── screen/
│   │   ├── auth/          Login + 6 OTHER auth screens (Welcome, Signup, Forgot, Reset, VerifyEmail, Profile) — only LoginScreen is wired (see AUDIT)
│   │   ├── autotagrules/  RulesList, RuleEditor + components/{ConditionRow, ActionRow, LivePreviewBox}
│   │   ├── backup/        Backup list + create flow
│   │   ├── bookmarks/     Bookmarks list + reason dialog
│   │   ├── calldetail/    Detail screen + sections/{HeroCard, ActionBar, TagsSection, FollowUpSection, NotesJournal, NoteEditorDialog, FollowUpDateTimeDialog, HistoryTimeline, StatsCard}
│   │   ├── calls/         Calls tab + UnsavedPinnedSection, BulkActionBar, CallsFilterSheet, CallRow, components/UpdateBanner
│   │   ├── docs/          In-app FAQ list + article reader (assets/docs/*.md)
│   │   ├── export/        ExportScreen (5-step wizard), QuickExportSheet + steps/{Format, DateRange, Scope, Columns, Destination}
│   │   ├── followups/     Follow-ups list (no calendar yet — see FEATURE_BACKLOG)
│   │   ├── home/          Home tab (Today / Recent unsaved / Quick actions)
│   │   ├── inquiries/     Auto-saved inquiries list + bulk-save dialog
│   │   ├── more/          More tab (grouped rows: Data / Automation / App / Account)
│   │   ├── mycontacts/    Saved contacts (excluding inquiry bucket)
│   │   ├── onboarding/    5-page tour (Welcome, Features, Permissions, OemBattery, FirstSync)
│   │   ├── permission/    Rationale + permanent-deny screens
│   │   ├── search/        Full-screen FTS search overlay
│   │   ├── settings/      Master + 4 sub-pages (AutoSave, LeadScoring, RealTime, UpdateSettings)
│   │   ├── shared/        StandardPage, NeoScaffold (used by every screen)
│   │   ├── splash/        Routes to Login or post-login destination based on AuthState
│   │   ├── stats/         Stats dashboard + charts/{DailyVolumeChart, HourlyHeatmap, TypeDonut, TopNumbersList}
│   │   ├── tags/          TagsManagerScreen, TagEditorDialog, TagPickerSheet (TagPickerSheet is NOT wired into autotagrules — see AUDIT)
│   │   └── update/        UpdateAvailable full-screen
│   ├── theme/             Color, Type, Shape, Spacing, Motion, NeoElevation, NeoShadows, Theme
│   └── util/              UI-only helpers (DateFormatter, etc.)
│
├── util/                  shared helpers (PhoneNumberNormalizer, PermissionManager, AssetDocsLoader)
└── di/                    AppModule, DatabaseModule, RepositoryModule, WorkerModule, ServiceModule
```

## Where to put new things

| New thing                    | Path                                                                  |
| ---------------------------- | --------------------------------------------------------------------- |
| Compose screen               | `ui/screen/{feature}/{Feature}Screen.kt` + `{Feature}ViewModel.kt`    |
| Reusable component           | `ui/components/neo/Neo{Name}.kt`                                      |
| Use case                     | `domain/usecase/{Verb}{Noun}UseCase.kt`                               |
| Domain model                 | `domain/model/{Name}.kt`                                              |
| Repository interface         | `domain/repository/{Name}Repository.kt`                               |
| Repository impl              | `data/repository/{Name}RepositoryImpl.kt`                             |
| Room entity                  | `data/local/entity/{Name}Entity.kt`                                   |
| DAO                          | `data/local/dao/{Name}Dao.kt`                                         |
| WorkManager job              | `data/work/{Name}Worker.kt`                                           |
| Foreground / overlay service | `data/service/...`                                                    |
| Settings key                 | Add to `data/prefs/SettingsDataStore.kt` (and a setter); read via VM  |
| String                       | `res/values/strings.xml` → `stringResource(R.string.X)`               |
| In-app help article          | `assets/docs/NN-slug.md` (and bump article list in `AssetDocsLoader`) |
| Hilt binding                 | the appropriate module under `di/`                                    |

## Cross-cutting conventions

- **DI**: every class is `@Inject constructor(...)` or `@Singleton`. ViewModels `@HiltViewModel`. Workers `@HiltWorker` + `@AssistedInject`.
- **State**: `StateFlow` for UI, `SharedFlow(replay=0)` for one-shot events.
- **Collection**: `collectAsStateWithLifecycle()` — never `collectAsState` for VM state.
- **Logging**: Timber only. Never `println`, never `Log.d`.
- **Strings**: every visible string from `stringResource(R.string.X)`. Final-quality English.
- **Errors**: user-friendly + actionable. Never raw exceptions.
- **No mock data outside `@Preview`**. No `TODO(` in user-reachable paths.

## Sprint-era debt

- 0 instrumentation tests, 3 unit tests. ViewModel + DAO test coverage is the largest debt (P2 in `TODO.md`).
- 6 stats charts still missing (P1 in `TODO.md`).
- Self-update manifest hosting is still pointed at placeholder URLs (P0).
