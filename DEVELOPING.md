# callNest — Developer Guide

How to bring the project up locally, install on a device, debug, and pick the work back up later.

This is an Android app — there is **no "dev server"**. The "dev loop" is: build APK → install on device/emulator → see live changes via Compose previews + Android Studio's Apply Changes / Live Edit.

---

## 1. One-time setup

### 1.1 Install the toolchain

| Tool           | Version                               | How                                         |
| -------------- | ------------------------------------- | ------------------------------------------- |
| JDK            | 17                                    | `sudo apt install openjdk-17-jdk`           |
| Android Studio | Ladybug (2024.2.x) or newer           | https://developer.android.com/studio        |
| Android SDK    | API 35 (compileSdk) + API 26 (minSdk) | Installed by Android Studio on first launch |
| Gradle         | 8.10.2                                | Provided via wrapper (see §1.3)             |
| Kotlin         | 2.0.21                                | Bundled with the Compose plugin in Gradle   |

Verify Java:

```bash
java -version    # must report 17.x
```

### 1.2 Open the project

```bash
# In Android Studio: File → Open → select the project folder
"/home/primathon/Documents/p_projet/a_APP/4. callNest"
```

The folder name has a space + dot. **Always quote the path** in shell commands.

Android Studio will spend ~2–5 minutes on the first sync (downloading Gradle, Compose BOM, Hilt, Room, POI, iText, Tink, etc.). Check the **Build** tool window for progress.

### 1.3 Generate the Gradle wrapper jar

The wrapper jar is intentionally not committed. From the project root:

```bash
cd "/home/primathon/Documents/p_projet/a_APP/4. callNest"
gradle wrapper --gradle-version 8.10.2
```

If you don't have `gradle` on `PATH`, install it once via `sudo apt install gradle` (any 8.x is fine; it's only used to bootstrap the wrapper).

After this, you can run `./gradlew` for everything.

### 1.4 First build

```bash
./gradlew assembleDebug
```

First build takes 3–8 minutes (downloads ~500 MB of deps). Subsequent builds are ~30–90 seconds.

The APK lands at:

```
app/build/outputs/apk/debug/app-debug.apk
```

If the build fails, see §5.

---

## 2. Running the app

### 2.1 On a physical Android phone (recommended)

callNest is a call-log app — emulators have empty call logs and can't fully exercise sync, real-time bubbles, or auto-save.

1. Phone settings → **About phone** → tap **Build number** 7 times → Developer options unlocked.
2. Developer options → enable **USB debugging**.
3. Plug into USB.
4. Accept the RSA fingerprint prompt on the phone.
5. Verify:
   ```bash
   adb devices
   ```
   You should see your device listed as `device` (not `unauthorized`).
6. In Android Studio, pick the device in the toolbar → click **Run** (▶️). It will install + launch the debug APK.

Or from the command line:

```bash
./gradlew installDebug
adb shell am start -n com.callNest.app/.MainActivity
```

### 2.2 On an emulator (limited)

Tools → Device Manager → **Create device** → pick Pixel 7 / API 35 → Finish → ▶️ next to the AVD.

You won't see real calls. You'll see the onboarding flow + empty Calls list. Use this for layout work only.

---

## 3. The dev loop

### 3.1 Compose Previews — fastest feedback

Every Neo\* component and most screens ship with `@Preview` annotations. In Android Studio:

- Open any file with `@Preview`, e.g. `ui/components/neo/NeoButton.kt`.
- Click the **split view** icon (top-right).
- Edit code → preview re-renders in <1 second.

Useful previews to keep open while building:

- `ui/screen/calls/CallRow.kt` — the row component used 80% of the screen time
- `ui/screen/calls/CallsScreen.kt` — full list with mock data
- `ui/screen/calldetail/CallDetailScreen.kt`
- `ui/screen/onboarding/OnboardingScreen.kt`

### 3.2 Live Edit / Apply Changes — second-fastest

After installing once via Run (▶️):

- **Apply Code Changes** (⌘⌥⇧R / Ctrl+Alt+Shift+R): pushes Kotlin changes without restarting the activity. Works for most logic edits.
- **Apply Changes and Restart Activity** (⌘⌥R / Ctrl+Alt+R): pushes resource + manifest-meta changes; restarts the screen.
- **Live Edit** (toggle in toolbar): pushes pure-Compose changes as you type. Disable when editing non-Compose code (it gets noisy).

Full reinstall when:

- Manifest changes (new permissions, new components)
- Hilt graph changes (`@HiltAndroidApp`, `@Module`, `@Provides`)
- Room schema changes (entities, DAOs)
- DataStore key additions

### 3.3 Logs

```bash
# All callNest logs:
adb logcat -s "callNest:*" "AndroidRuntime:E"

# Or in Android Studio: View → Tool Windows → Logcat
# Filter:
package:com.callNest.app
```

The app uses **Timber** — `Timber.d("...")`, `Timber.e(throwable, "...")`. In debug builds Timber.plant(DebugTree) — every log shows up in logcat with the source class name.

### 3.4 Inspecting state

- **Layout Inspector** (Tools → Layout Inspector): tree view of the running Compose hierarchy + state values.
- **App Inspection** (Tools → App Inspection):
  - **Database Inspector** — live read of Room tables. Browse `calls`, `contact_meta`, `notes`, run ad-hoc SQL.
  - **Background Task Inspector** — see WorkManager schedules (CallSyncWorker, DailyBackupWorker, UpdateCheckWorker).
- **Profiler** (View → Tool Windows → Profiler): CPU, memory, network. Useful for the spec's perf bar (cold start < 1.5s, filter on 10k < 300ms).

### 3.5 Debugging breakpoints

1. Click the gutter next to any line to set a breakpoint.
2. Click the **Debug** (🐞) icon instead of Run.
3. App launches with debugger attached. Hits will pause execution; inspect locals, evaluate expressions.

To debug background workers, set a breakpoint inside `CallSyncWorker.doWork()` and trigger from the app:

```bash
adb shell cmd jobscheduler run -f com.callNest.app 0
```

Or pull-to-refresh on the Calls screen.

---

## 4. Verifying what works

Walk this list end-to-end after any nontrivial change:

| #   | Flow         | Steps                                                  | Expected                                                                                         |
| --- | ------------ | ------------------------------------------------------ | ------------------------------------------------------------------------------------------------ |
| 1   | Onboarding   | Fresh install → step through 5 pages                   | Lands on Calls; `onboarding_complete=true` in Database Inspector → `Preferences`                 |
| 2   | Sync         | Calls screen → pull-to-refresh                         | New calls appear; `lastSyncCallId` advances in DataStore                                         |
| 3   | Auto-save    | Call yourself from a number not in contacts → wait 30s | New entry appears in Inquiries tab named `callNest-s1 +91…`                                      |
| 4   | Filters      | Calls → filter icon → pick "Missed" + tag → Apply      | List narrows; chip row appears above                                                             |
| 5   | Search       | Calls → 🔍 → type a number fragment                    | FTS results show; recent searches persist                                                        |
| 6   | Tags         | CallDetail → Add tag → pick → save                     | Tag chip shows in row + detail; persists across restart                                          |
| 7   | Follow-up    | CallDetail → Set follow-up → date+time                 | Notification fires at scheduled time with Call back / Snooze actions                             |
| 8   | Real-time    | Make a call to/from the device                         | Floating bubble appears mid-call (if overlay perm granted); post-call popup appears 2s after end |
| 9   | Stats        | More → Stats                                           | 4 charts render with live data                                                                   |
| 10  | Export       | More → Export → CSV → Apply                            | File appears in `Downloads/`; share intent works                                                 |
| 11  | Backup       | Settings → Backup → Manual backup now                  | `.cvb` file in Downloads; restore round-trips                                                    |
| 12  | Update check | Settings → App updates → Check now                     | Hits `BuildConfig.UPDATE_MANIFEST_*_URL`; no-op until you host a real manifest                   |

If a step fails, Logcat + Database Inspector are your first stops.

---

## 5. Common build/runtime issues

### 5.1 `Could not resolve …`

Network problem during dep download. Re-run `./gradlew assembleDebug --refresh-dependencies`.

### 5.2 KSP / Hilt errors

Look for `error: [Hilt]` in build output. Most common: a `@HiltViewModel` whose constructor injects a non-bound type. Fix by adding `@Inject constructor` to the missing class or a `@Binds` in `RepositoryModule`.

### 5.3 Room schema export

If Room complains about missing schema, ensure `app/build.gradle.kts` has:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

And commit the JSON files in `app/schemas/` for migration history.

### 5.4 Manifest references missing class

If you see `ClassNotFoundException` for e.g. `CallEnrichmentService`: confirm the class exists at the FQCN declared in `AndroidManifest.xml`. Sprint 0 stubs were replaced in later sprints; if a stub got deleted, restore.

### 5.5 Compose preview crashes

A preview that injects via Hilt won't render — Hilt isn't available at preview time. Either:

- Pass dummy state into the previewed composable, or
- Wrap the preview in `@PreviewParameter` providers, or
- Move VM-dependent composables out of the previewed surface.

### 5.6 APK won't install: "App not installed"

Usually a signature mismatch. Uninstall first:

```bash
adb uninstall com.callNest.app
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 5.7 `INSTALL_FAILED_USER_RESTRICTED` (Xiaomi/Vivo)

Disable "Verify apps over USB" + enable "Install via USB" in MIUI/Funtouch developer options.

### 5.8 Real-time overlay never shows

1. Settings → Apps → callNest → permissions → enable **Display over other apps**.
2. Confirm `floatingBubbleEnabled = true` in DataStore (Database Inspector).
3. Check `CallEnrichmentService` is foregrounded: `adb shell dumpsys activity services com.callNest.app | grep CallEnrichment`.

---

## 6. Picking work back up later

Status of the project is in:

- **`CHANGELOG.md`** — what shipped in v1.0.0.
- **`DECISIONS.md`** — every fallback, deferral, and trade-off taken.
- **`README.md`** — high-level overview.
- **`docs/architecture.md`** — layering, sync pipeline diagram, lead-score formula.

The pending punch-list is in your last conversation message; copy it into a `TODO.md` so you don't lose it.

### 6.1 Recommended next session order

1. `./gradlew lint assembleDebug` — fix until green. Expect 1–3 iterations.
2. Generate launcher icons + signing keystore.
3. Wire `MainScaffold` into `callNestNavHost` so the 5-tab bottom nav is reachable.
4. Stand up a static `versions-stable.json` somewhere (GitHub Pages works) and set the BuildConfig URL.
5. Sweep empty/loading/error states across screens.
6. Add the missing 6 stats charts.
7. Round out unit + DAO tests.

### 6.2 Resuming with Claude

When you come back, in a fresh chat say:

> "Resume callNest. Project at `/home/primathon/Documents/p_projet/a_APP/4. callNest/`. Pick the next item from `TODO.md`."

The agent will read `CHANGELOG.md`, `DECISIONS.md`, and `TODO.md` for context, then resume one task at a time.

### 6.3 Where things live

| You're looking for…             | Open this                                              |
| ------------------------------- | ------------------------------------------------------ |
| A screen                        | `app/src/main/java/com/callNest/app/ui/screen/{name}/` |
| A reusable component            | `ui/components/neo/`                                   |
| A use case                      | `domain/usecase/`                                      |
| A DAO query                     | `data/local/dao/`                                      |
| A Room entity                   | `data/local/entity/`                                   |
| A WorkManager job               | `data/work/`                                           |
| A foreground/alarm service      | `data/service/`                                        |
| Settings keys                   | `data/prefs/SettingsDataStore.kt`                      |
| Manifest, permissions, channels | `app/src/main/AndroidManifest.xml`, `callNestApp.kt`   |
| Strings                         | `app/src/main/res/values/strings.xml`                  |
| In-app docs articles            | `app/src/main/assets/docs/01..15-*.md`                 |

### 6.4 A useful one-liner

Before commits:

```bash
./gradlew lint assembleDebug --no-daemon 2>&1 | tee /tmp/cv-build.log
```

Skim `/tmp/cv-build.log` for `e:` (errors) and `w:` (warnings).

---

## 7. Quick reference

```bash
# Build debug APK
./gradlew assembleDebug

# Build + install + launch
./gradlew installDebug && adb shell am start -n com.callNest.app/.MainActivity

# Lint only
./gradlew lint

# Unit tests
./gradlew test

# Instrumentation tests (needs connected device/emulator)
./gradlew connectedDebugAndroidTest

# Clear app data
adb shell pm clear com.callNest.app

# Tail app logs
adb logcat -s "callNest:*" "AndroidRuntime:E"

# Full uninstall
adb uninstall com.callNest.app

# Pull a backup file off the device
adb pull /sdcard/Download/callNest-backup-*.cvb ~/

# Inspect Room DB on device
# Tools → App Inspection → Database Inspector (in Android Studio)
```

That's the full loop. Build → install → poke → logcat → repeat.
