# 04 — "How do I do X?" — common tasks

Recipes for the things you'd do daily as a React dev, translated.

## Change a string on screen

Strings live in `app/src/main/res/values/strings.xml`. **Don't hardcode strings in Composables.**

1. Open `strings.xml`. Find the existing string (search for the current text). Edit it.
2. If you're adding a new string, add a new line:
   ```xml
   <string name="cv_calls_refresh">Refresh now</string>
   ```
3. Reference from a Composable:
   ```kotlin
   stringResource(R.string.cv_calls_refresh)
   ```
4. Save. If the app is running, **Apply Changes and Restart Activity** (Ctrl+Alt+Shift+R) to push.

React analogue: changing a string in `i18n/en.json` and hot-reloading.

## Change how a screen looks

Find the screen in `app/src/main/java/com/callvault/app/ui/screen/{feature}/{Feature}Screen.kt`.

Compose syntax cheat sheet (vs JSX):

```kotlin
// JSX:  <div className="p-4 bg-base"><Text>Hello</Text></div>
// Compose:
Box(modifier = Modifier.padding(16.dp).background(NeoColors.Base)) {
    Text("Hello")
}
```

```kotlin
// JSX:  <Button onClick={onSave}>Save</Button>
// Compose:
NeoButton(text = stringResource(R.string.cv_save), onClick = onSave)
```

```kotlin
// JSX:  {items.map(it => <Row key={it.id} item={it} />)}
// Compose:
LazyColumn {
    items(items, key = { it.id }) { item -> Row(item) }
}
```

```kotlin
// JSX:  const [open, setOpen] = useState(false)
// Compose:
var open by remember { mutableStateOf(false) }
```

```kotlin
// JSX:  useEffect(() => { fetch(...) }, [id])
// Compose:
LaunchedEffect(id) { /* runs when id changes */ }
```

## Add a new screen

Use the `/feature` slash command:

```
/feature ContactHistory
```

It scaffolds the entity → DAO → repo → use case → ViewModel → screen → navigation entry across all layers, matching existing conventions.

If you'd rather do it manually, the table at the bottom of `CLAUDE.md` lists every "where things go".

## Add a button that calls a function

In `{Feature}Screen.kt`:

```kotlin
NeoIconButton(
    icon = Icons.Default.Refresh,
    contentDescription = stringResource(R.string.cv_calls_refresh),
    onClick = onRefresh,
)
```

In `{Feature}ViewModel.kt`:

```kotlin
fun refresh() = viewModelScope.launch {
    syncScheduler.triggerOnce()
}
```

Wire from the Screen wrapper:

```kotlin
@Composable
fun CallsScreen(viewModel: CallsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CallsContent(
        state = state,
        onRefresh = viewModel::refresh,
    )
}
```

The pattern: **screen wrapper does Hilt injection + state collection; inner Content composable is pure**. The Content composable is what `@Preview` uses.

## Add a row to the database

1. Add (or edit) an entity in `app/src/main/java/com/callvault/app/data/local/entity/`:
   ```kotlin
   @Entity(tableName = "things")
   data class ThingEntity(
       @PrimaryKey(autoGenerate = true) val id: Long = 0,
       val name: String,
       val createdAt: Long = System.currentTimeMillis(),
   )
   ```
2. Add a DAO in `data/local/dao/ThingDao.kt`:
   ```kotlin
   @Dao
   interface ThingDao {
       @Insert suspend fun insert(thing: ThingEntity): Long
       @Query("SELECT * FROM things ORDER BY createdAt DESC") fun observeAll(): Flow<List<ThingEntity>>
   }
   ```
3. Register in `data/local/CallVaultDatabase.kt`: add the entity to `@Database(entities = [...])`, bump version (e.g. 2 → 3), add an abstract `fun thingDao(): ThingDao`.
4. Add a migration in `data/local/migration/Migrations.kt`:
   ```kotlin
   val MIGRATION_2_3 = object : Migration(2, 3) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("CREATE TABLE things (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
       }
   }
   ```
   And add `MIGRATION_2_3` to the `addMigrations(...)` call.
5. Sync Gradle. Build. KSP regenerates Room code.

React/Next analogue: writing a new Prisma model + running a migration.

## Read settings (DataStore)

Settings live in `data/prefs/SettingsDataStore.kt`. To read or write:

```kotlin
class MyViewModel @Inject constructor(
    private val settings: SettingsDataStore,
) : ViewModel() {

    val state = settings.autoSaveEnabled  // Flow<Boolean>
        .map { ... }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ...)

    fun toggle() = viewModelScope.launch {
        val current = settings.autoSaveEnabled.first()
        settings.setAutoSaveEnabled(!current)
    }
}
```

DataStore is the equivalent of `localStorage` — but typed.

## Make an API-like call (use case → repo → DAO)

There is no HTTP API. "Backend" calls are use cases that hit the local DB:

```kotlin
class GetRecentCallsUseCase @Inject constructor(
    private val callRepo: CallRepository,
) {
    operator fun invoke(limit: Int): Flow<List<Call>> =
        callRepo.observeRecent(limit)
}
```

Inject into a ViewModel:

```kotlin
class HomeViewModel @Inject constructor(
    private val getRecentCalls: GetRecentCallsUseCase,
) : ViewModel() {

    val state = getRecentCalls(limit = 5)
        .map { HomeUiState(calls = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())
}
```

Compare to a React+SWR call:

```typescript
// React analogue, conceptually:
const { data } = useSWR(`/api/recent?limit=5`, fetcher);
```

The Flow<T> emits new values when the underlying DB rows change — like SWR's auto-revalidation, but driven by Room triggers.

## Run something on a schedule

Background scheduling = **WorkManager**. Examples already in `data/work/`:
- `CallSyncWorker` (periodic)
- `DailyBackupWorker` (daily at 2 AM)
- `UpdateCheckWorker` (weekly)

To run something every N minutes:

```kotlin
@HiltWorker
class MyWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val something: SomethingUseCase,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = try {
        something.invoke()
        Result.success()
    } catch (t: Throwable) {
        Timber.e(t, "MyWorker failed")
        Result.retry()
    }

    companion object {
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<MyWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("my_worker", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
```

Then `MyWorker.schedule(this)` from `CallVaultApp.onCreate`.

React/Next analogue: a Vercel Cron job, but running on-device.

## Add a new dependency

1. Open `gradle/libs.versions.toml`.
2. Under `[versions]` add: `mylib = "1.2.3"`.
3. Under `[libraries]` add: `mylib = { module = "com.example:mylib", version.ref = "mylib" }`.
4. In `app/build.gradle.kts` `dependencies { ... }` add: `implementation(libs.mylib)`.
5. Sync Gradle.

This is the equivalent of `npm install mylib@1.2.3`. The catalog file is your `package.json`'s dependencies block, but typed.

**Don't add deps casually.** The locked stack in CLAUDE.md is intentional.

## Check that something works

Run the smoke test list from `DEVELOPING.md` §4. It walks through onboarding → sync → auto-save → filters → search → tags → follow-ups → real-time → stats → export → backup → update check.

For automated tests:

```bash
./gradlew test                           # JVM unit tests
./gradlew connectedDebugAndroidTest      # device-side DAO + integration tests (needs phone connected)
```

## Commit your changes

This project doesn't have a git history yet; nothing's committed. When you're ready:

```bash
cd "/home/primathon/Documents/p_projet/a_APP/4. callVault"
git init
git add .
git commit -m "feat: initial v1.0.0 import"
```

(Be careful — the `app/build/` folder is large. The `.gitignore` should already exclude it.)

## You're ready.

The flow:
1. Edit code.
2. See changes via Compose Preview (UI) or Live Edit (running app).
3. Logcat + Database Inspector when something breaks.
4. Full reinstall when Live Edit can't keep up.

Loop forever.
