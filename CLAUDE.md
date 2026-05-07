# CLAUDE.md — callNest project context

You are working inside the **callNest** Android project. This file is loaded into every Claude Code session opened in this repo. Read it before doing anything else.

## What this app is

callNest is a sideloaded (non-Play-Store) Android app for Indian small-business owners who receive 20–100 daily inquiry calls. It captures every call from `CallLog.Calls`, auto-saves unsaved inquiry numbers into a dedicated contact group, lets the user tag/note/bookmark/follow-up each call, computes a 0–100 lead score, and exports to Excel/CSV/PDF. Real-time features include a floating in-call bubble and a post-call popup. Self-update is via a hosted `versions.json` manifest, not Play Store.

Full spec: `/home/primathon/Downloads/callNest_mega_prompt.md` (1533 lines, locked).

AI handoff docs for future sessions live in `.claude/docs/README.md`. Start there when you need a compact project map, architecture/flow guide, file index, backlog, or agent working guide.

## Locked tech stack — do not deviate

```
Kotlin 2.0.21 · AGP 8.7.3 · Gradle 8.10.2
compileSdk/targetSdk 35 · minSdk 26 · JVM 17
Compose BOM 2024.12.01 · Material 3
Hilt 2.53.1 (KSP) · Room 2.6.1 (KSP)
DataStore 1.1.1 · WorkManager 2.10.0
kotlinx coroutines 1.9.0 · serialization 1.7.3 · datetime 0.6.1
libphonenumber-android 8.13.50 (io.michaelrocks artifact)
Apache POI ooxml-lite 5.2.5 · iText core 8.0.5 · Tink 1.15.0
Coil 3.0.4 · Vico 2.0.0-beta.4 · Timber 5.0.1
JUnit5 5.11.4 · Turbine 1.2.0 · MockK 1.13.13
```

If a library version doesn't resolve, fall back to next-most-recent stable and document in `DECISIONS.md`. Never silently bump majors.

## Layering — strict

```
ui/      Compose + ViewModels + Neo* components (Android-bound)
domain/  Pure Kotlin: models (sealed for state/conditions/actions), use cases, repository INTERFACES
data/    Repo impls, Room (entities/DAOs/FTS), DataStore, system wrappers, workers, services, exporters, backup, update
util/    Shared helpers (formatters, pattern matchers, asset loaders)
di/      Hilt modules
```

Cross-layer rules:

- `ui/` may import `domain/` and `util/`. Never `data/`.
- `domain/` may import nothing else (pure Kotlin). No Android imports in `domain/model/` or `domain/usecase/`.
- `data/` implements `domain/repository/` interfaces and bridges to Android.

## Existing conventions — match them

- **Single Android module**: `app/`. Don't add modules.
- **Package**: `com.callNest.app`.
- **DI**: every class is `@Inject constructor(...)` or `@Singleton`. ViewModels are `@HiltViewModel`. Workers are `@HiltWorker` with `@AssistedInject`.
- **State**: `StateFlow` for UI state, `SharedFlow` for one-shot events (snackbars, navigation). Collect via `collectAsStateWithLifecycle()`.
- **Strings**: every UI string goes through `stringResource(R.string.X)`. Add new strings to `app/src/main/res/values/strings.xml`. Final-quality English — no Lorem ipsum, no "Coming soon".
- **Errors**: user-friendly and actionable. "Couldn't read your call log. Tap to grant permission." not "SecurityException at …".
- **KDoc**: every public class, every public composable, every public DAO method.
- **Previews**: every Compose composable that ships gets at least one `@Preview`. Pass mock state directly — don't inject Hilt into a previewed composable.
- **Logging**: Timber only. `Timber.d`/`Timber.w`/`Timber.e(t, "...")`. Never `println`, never `Log.d`.
- **No mock data outside `@Preview`.** Production code paths render real DB-backed state.
- **No `TODO(`** in user-reachable paths. Deferred work uses `NotImplementedError("Implemented in Sprint N")` ONLY in classes that no UI route reaches.
- **No comments that explain WHAT** — well-named identifiers do that. Comments only for non-obvious WHY (workaround, hidden constraint, surprising invariant).

## Hard constraints for Claude

1. **Active development phase (2026-05-06 → until further notice): you ARE expected to build, install, and reload the app on the connected device after every code change so the user can see the latest behavior.** Run `./gradlew` (assembleDebug / installDebug), `adb install`, `adb shell pm clear`, `adb shell am start`, and similar commands without asking — the user has pre-authorized this. First-build network downloads are accepted. This will be revoked when the project moves to a testing phase; until then, default to "build & reinstall after edits".
2. **Spec is locked.** Do not relitigate decisions in §3.x. If something seems wrong, flag it — don't quietly redesign.
3. **Spec sections to consult by offset/limit** (do NOT read the whole 1533-line file):
   - §3.x feature specs → ~lines 100–600
   - §4 data model → ~lines 686–900
   - §5 domain layer → ~lines 900–950
   - §6 UI tree → ~lines 950–1070
   - §7 data layer tree → ~lines 1070–1145
   - §8 algorithms → ~lines 1148–1252
   - §9 manifest → ~lines 1255–1334
   - §10 build → ~lines 1337–1359
   - §11 sprint plan → ~lines 1362–1492
   - §13 directives → ~lines 1521–1532

## Current project status

13 sprints (0–12) shipped. 245 Kotlin files in `app/src/main/`, 15 in-app docs articles, 3 unit tests, 0 instrumentation tests landed.

What to read for state:

- `RELEASE-PLAN.md` — phased plan to ship v1.0.0, with explicit you/Claude split. Read first.
- `CHANGELOG.md` — what each version added.
- `DECISIONS.md` — every fallback / deferral / trade-off.
- `docs/architecture.md` — layering, sync pipeline diagram, lead-score formula.
- `TODO.md` — outstanding punch-list (P0 → P3).
- `DEVELOPING.md` — how to build, install, debug.

## How to work in this repo

When the user gives an open-ended task ("continue", "next item", "fix the build"):

1. Open `TODO.md`. Pick the highest-priority unchecked item.
2. Confirm the scope in one sentence.
3. Use the appropriate subagent (see `.claude/agents/`):
   - Feature work → `callNest-android-engineer`
   - Compose UI → `callNest-ui-builder`
   - Tests → `callNest-test-writer`
   - Build errors → `callNest-build-fixer`
   - Docs/changelog → `callNest-doc-writer`
4. After finishing, update `TODO.md` (check the box, add follow-ups), append to `CHANGELOG.md` if user-visible, append to `DECISIONS.md` if a fallback was taken.

When the user invokes a slash command (e.g. `/build`, `/smoke`, `/next`), follow the corresponding file in `.claude/commands/`.

## Don'ts

- Don't add new Gradle modules.
- Don't introduce new state-management libraries (no Mavericks, no MVI frameworks). StateFlow is the answer.
- ~~Don't add Firebase, Crashlytics, GA, or any analytics SDK.~~ **REVERSED 2026-05-05** — see `DECISIONS.md` "Cloud pivot". Supabase Auth, PostHog analytics, and FCM push are now in scope.
- ~~Don't add Google Play Services.~~ **REVERSED 2026-05-05** — FCM requires GMS. App still distributed sideloaded; users without GMS will lose push but core app still works.
- Don't add mock/fake data to production code. Fix the real flow instead.
- Don't write multi-paragraph comment blocks or multi-line docstrings explaining what code does. One short KDoc line max.
- Don't create planning/decision documents speculatively. Update `DECISIONS.md` only when a deferral or trade-off is genuinely taken.

## Useful commands you can run freely

```bash
# Search
grep -rn "pattern" app/src/main/java/com/callNest/app/

# Find files
find app/src/main -name "*.kt" -path "*/screen/calls/*"

# Read with offset/limit (use Read tool, don't cat large files)

# Check what was last touched
ls -lt app/src/main/java/com/callNest/app/ui/screen/

# Project file count
find app/src/main/java/com/callNest/app -name "*.kt" | wc -l
```

## Where to put new things

| New thing                  | Lives in                                                                           |
| -------------------------- | ---------------------------------------------------------------------------------- |
| Compose screen             | `ui/screen/{feature}/{Feature}Screen.kt` + `{Feature}ViewModel.kt`                 |
| Reusable Compose component | `ui/components/neo/Neo{Name}.kt`                                                   |
| Use case                   | `domain/usecase/{Verb}{Noun}UseCase.kt`                                            |
| Domain model               | `domain/model/{Name}.kt`                                                           |
| Repository interface       | `domain/repository/{Name}Repository.kt`                                            |
| Repository impl            | `data/repository/{Name}RepositoryImpl.kt`                                          |
| Room entity                | `data/local/entity/{Name}Entity.kt`                                                |
| DAO                        | `data/local/dao/{Name}Dao.kt`                                                      |
| WorkManager job            | `data/work/{Name}Worker.kt`                                                        |
| Foreground/alarm           | `data/service/...`                                                                 |
| Settings key               | Add to `data/prefs/SettingsDataStore.kt`                                           |
| String                     | Add to `res/values/strings.xml`, reference via `stringResource(R.string.X)`        |
| In-app doc                 | New `app/src/main/assets/docs/NN-slug.md` + bump article list in `AssetDocsLoader` |

## When you're stuck

- Read `DECISIONS.md` first — there's a good chance the trade-off you're reconsidering was already taken intentionally.
- Read `docs/architecture.md` for layering rules.
- Re-read the relevant §3.x section of the spec via Read with offset/limit. Don't infer behavior; the spec is locked.
- Ask the user before redesigning a piece of the data model.
