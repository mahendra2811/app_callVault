# AI Working Guide

This file teaches future AI agents how to work and answer well in CallVault.

## First Response Pattern

When asked to change code:

1. Read `CLAUDE.md`.
2. Read the relevant `.claude/docs/*` file.
3. Search code with `rg`.
4. Identify the owning layer and file.
5. Make the smallest correct edit.
6. Add or update tests when risk warrants it.
7. Do not run Gradle unless the user explicitly approves build/test commands.
8. Summarize changed files and verification honestly.

## How To Answer The User

The user is building docs for future AI. Prefer clear, direct, practical explanations. Use concrete file paths. Avoid vague "you could" answers when the codebase already shows the right path.

Good answer shape:

- what you changed
- where it lives
- why it matters
- what was not verified, if build/tests were not run
- next highest-value follow-up

Avoid:

- inventing missing files
- claiming tests passed when they were not run
- suggesting Play Store, Firebase, analytics, or cloud sync
- proposing a major redesign without reading existing code and `DECISIONS.md`

## Editing Rules

- Keep one module: `app`.
- Use Kotlin, Compose, Hilt, Room, DataStore, WorkManager.
- Follow existing package `com.callvault.app`.
- Prefer `@Inject constructor`.
- ViewModels use `@HiltViewModel`.
- Workers use `@HiltWorker` and assisted injection.
- UI state should be `StateFlow`; one-shot events should be `SharedFlow`.
- Use Timber, not `println` or Android `Log`.
- UI strings go in `strings.xml`.
- Do not add mock data to production code.
- Do not add `TODO(` in reachable code.
- Keep domain pure where possible.

## Where To Put New Code

- New screen: `ui/screen/{feature}/{Feature}Screen.kt` and `{Feature}ViewModel.kt`
- Reusable Compose component: `ui/components/neo/Neo{Name}.kt`
- Use case: `domain/usecase/{Verb}{Noun}UseCase.kt`
- Domain model: `domain/model/{Name}.kt`
- Repository interface: `domain/repository/{Name}Repository.kt`
- Repository implementation: `data/repository/{Name}RepositoryImpl.kt`
- Room entity: `data/local/entity/{Name}Entity.kt`
- DAO: `data/local/dao/{Name}Dao.kt`
- Migration: `data/local/migration/Migrations.kt`
- Worker: `data/work/{Name}Worker.kt`
- System API wrapper: `data/system/{Name}.kt`
- Foreground/alarm/overlay: `data/service/...`
- Setting: `SettingsDataStore.kt`
- In-app doc: `app/src/main/assets/docs/NN-slug.md` and article list in `AssetDocsLoader`

## Common Task Recipes

### Add A Setting

1. Add key, `Flow`, and setter in `SettingsDataStore.kt`.
2. If UI needs it, expose in the relevant ViewModel state.
3. Persist with debounce for sliders/text fields.
4. Add string resources.
5. If setting affects background work, update scheduler/controller.

### Add A Room Table

1. Add entity in `data/local/entity`.
2. Add DAO in `data/local/dao`.
3. Add entity and abstract DAO to `CallVaultDatabase`.
4. Bump database version.
5. Add migration in `Migrations.kt`.
6. Export schema after approved build.
7. Add repository/use case if UI/domain needs it.

### Add A Screen

1. Create screen and ViewModel under `ui/screen/{feature}`.
2. Add route to `Destinations.kt`.
3. Wire route in `CallVaultNavHost.kt`.
4. Add strings to `strings.xml`.
5. Use Neo components and `CallVaultTheme`.
6. Collect state with lifecycle-aware collection.
7. Add preview where useful.

### Add An Export Column

1. Update `ExportColumns`/config.
2. Update wizard UI in `ColumnsStep`.
3. Update each affected exporter.
4. Update JSON backup only if schema-level data changes.
5. Test CSV escaping and Excel/PDF formatting.

### Add A Rule Condition Or Action

1. Add sealed type in `RuleCondition` or `RuleAction`.
2. Update serialization discriminator handling if needed.
3. Update editor UI rows.
4. Update evaluator/applier.
5. Add tests.
6. Consider rule preview performance.

## Verification Guidance

Do not run build commands unless the user approves. When approved, preferred order:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

For Android behavior, physical device verification matters more than emulator for:

- call log import
- SIM slot resolution
- contacts write
- exact alarms
- overlays
- phone-state monitoring
- OEM battery behavior

Manual smoke flows are listed in `DEVELOPING.md`.

## Known Pitfalls

- `gradle/wrapper/gradle-wrapper.jar` may be absent.
- First Gradle build can download hundreds of MB.
- `FilterPresets` route is currently placeholder.
- Some sprint comments are stale; inspect actual code before relying on comments.
- Overlay UI is Android views, not Compose.
- Backup restore is destructive.
- Auto-save writes to system contacts; be careful in tests.
- Call log and contacts APIs need runtime permissions and real device data.
- WorkManager periodic minimum is 15 minutes; 5-minute sync uses exact AlarmManager.
- DataStore contains stringified JSON for some settings; preserve compatibility.

## Best Next Action When User Says "Continue"

Because no `TODO.md` currently exists, use this order:

1. Ask for approval to run `./gradlew lint assembleDebug` if the user wants build confidence.
2. Otherwise pick from `04-future-plan-and-backlog.md`, starting with:
   - real filter presets route
   - tests for rule evaluation/lead scoring/filter builder
   - stats chart completion
   - NeoHelpIcon/docs wiring
3. Update docs or changelog only for user-visible changes or real decisions.

