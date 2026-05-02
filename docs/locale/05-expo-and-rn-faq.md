# 05 — Expo / React Native FAQ

If you've shipped React Native + Expo apps, this is the file that answers "wait, why doesn't my usual workflow work here?"

## Can I use Expo Go to run this app?

**No.** Expo Go only runs apps written in React Native + Expo SDK. CallVault is native Kotlin + Jetpack Compose. They are different runtimes:

| | Expo / React Native | CallVault (this project) |
|--|--|--|
| Source language | JavaScript / TypeScript | Kotlin |
| UI framework | React Native — renders native views via a JS bridge | Jetpack Compose — compiles directly to Android UI |
| Entry point | A JS bundle (`index.js`) | An APK with compiled JVM bytecode |
| Bundler | Metro | Gradle + Compose compiler plugin |
| Hot reload | Metro pushes a new JS bundle over your LAN | Android Studio Live Edit pushes JVM bytecode over ADB |
| What's on the phone | Expo Go app (or a custom dev client) downloads your bundle | A signed APK installed via ADB |

Expo Go has no Kotlin runtime in it. It can't load a `.apk` and would have nothing to do with one. The wire formats are completely different.

## What does this project use instead of Expo / Metro / RN?

| Expo concept | CallVault concept |
|--|--|
| `expo start` (Metro dev server) | **Compose Previews** in Android Studio + **Live Edit** for the running app |
| Expo Go | **Android Studio + a phone with USB debugging** |
| `expo build` | `./gradlew assembleDebug` (debug) or `./gradlew assembleRelease` (signed) |
| EAS Build | Local Gradle build only (no managed cloud build for native projects in this repo) |
| `app.json` / `app.config.js` | `app/build.gradle.kts` + `AndroidManifest.xml` |
| `expo-secure-store` | `EncryptedSharedPreferences` (already in use as `SecurePrefs`) |
| `expo-sqlite` | **Room** (Google's typed SQLite ORM) |
| `AsyncStorage` | **DataStore Preferences** |
| `expo-router` | **Compose Navigation** (`androidx.navigation.compose`) |
| `expo-notifications` | `NotificationManagerCompat` + `AlarmManager.setExact...` (used by `FollowUpAlarmReceiver`) |
| `expo-task-manager` / background fetch | **WorkManager** (`CallSyncWorker`, `DailyBackupWorker`, `UpdateCheckWorker`) |
| `react-native-permissions` | `PermissionManager.kt` + Compose `rememberLauncherForActivityResult` |
| Reanimated / Moti | Compose's built-in animation APIs (`animate*AsState`, `Animatable`, `updateTransition`) |
| RN bridge | None — Compose talks directly to the platform via Kotlin/Java interop |

## What's the closest workflow to "Expo Go on my phone, edit, see changes"?

Three things stacked together get you 90% of that feel.

### 1. Compose Previews — for component work

Like Storybook with hot reload. **No phone needed.**

- Open any `.kt` file with `@Preview` (every Neo* component has one).
- Click split-view in Android Studio.
- Edit → save → re-renders in <1 second.

**This is better than Expo for pure-UI work** because there's no device round-trip.

### 2. Live Edit + wireless ADB — for app-level work

This is the workflow that actually feels like Expo Go.

```bash
# One-time wireless pairing:
# On phone: Settings → Developer options → Wireless debugging → Pair device with pairing code
adb pair <phone-ip>:<pair-port>      # enter the code shown on phone
adb connect <phone-ip>:<connect-port>

# First install:
./gradlew installDebug
adb shell am start -n com.callvault.app/.MainActivity
```

Now in Android Studio:
- Toggle **Live Edit** in the toolbar (small flame icon).
- Edit a Composable → save → 1–3 seconds → phone updates.
- The phone stays on your desk; no cable.

This is the closest thing to scanning a QR code and getting hot reload over Wi-Fi.

### 3. Adb-over-Wi-Fi for the rebuild loop — for non-Compose changes

Live Edit can't push:
- ViewModel / repository changes (Kotlin code outside `@Composable`)
- Manifest / permission changes
- Hilt graph / Room schema changes
- New `BuildConfig` fields
- New string resources

For those, **Apply Changes** (Ctrl+Alt+Shift+R) or a full reinstall:

```bash
./gradlew installDebug
```

Over wireless ADB, this is the fastest you'll get to a "save → see it on phone" loop without RN.

## Why is the dev loop slower than Expo?

Mostly because Compose compiles to JVM bytecode and Gradle has to do real work each time. RN's JS bundle is interpreted at runtime, so Metro can swap it cheaply. Compose has stronger type guarantees and zero JS bridge overhead at runtime — the trade-off is build time.

For 90% of UI work, **Compose Previews** (sub-second feedback) close most of that gap.

## Can I rewrite this in React Native to get my familiar workflow?

You could, but you'd lose:

- **Direct ContentResolver access to `CallLog.Calls` and `ContactsContract.RawContacts`** — RN has no native module for these out of the box. You'd write a Kotlin/Swift native module anyway, and you'd be back in this stack for those parts.
- **`TYPE_APPLICATION_OVERLAY` foreground service for the floating bubble.** RN has Modal, but a system-wide overlay during a phone call requires a Kotlin foreground service with `foregroundServiceType="specialUse"`. Not something Expo's managed workflow supports — you'd be in `expo-dev-client` "bare" territory.
- **`TelephonyCallback`** for live phone-state events (offhook → idle → ringing). Same as above — has to be a native module.
- **Tink + Apache POI + iText.** All Java libraries. Not in the JS ecosystem at this maturity.
- **`AlarmManager.setExactAndAllowWhileIdle`** for follow-up alarms.
- **`WorkManager` periodic sync.**

The spec says: every one of those is non-negotiable. Add up the native modules you'd need to write, and you're effectively writing the Kotlin app anyway, just with a JS UI layer on top — slower at runtime, more failure modes, more to maintain.

If you really wanted a JS layer, the project to look at is **Kotlin Multiplatform Mobile (KMM) + Compose Multiplatform** — but that's not Expo and would be a full rewrite. Out of scope for this repo.

## What about `expo-dev-client` or the bare workflow?

Same core problem. Even "bare" Expo means a React Native runtime in the APK. CallVault has no RN runtime. You'd be embedding RN inside this Android app just to get JS-side fast refresh — a lot of complexity for a workflow problem that Compose Previews + Live Edit + wireless ADB already solves.

## Habits to unlearn

| RN/Expo habit | What to do here instead |
|--|--|
| `console.log("x", obj)` | `Timber.d("x %s", obj)` (or `Timber.d("x ${obj}")`) |
| `console.error(err)` | `Timber.e(err, "context")` |
| Reach for `useState` | `var x by remember { mutableStateOf(...) }` |
| Reach for `useEffect` | `LaunchedEffect(key) { ... }` |
| Use a `<View style={...}>` | `Box(modifier = Modifier...)` |
| Use a `<Text>` | `Text("...")` (built-in Compose) |
| Add an npm package | Edit `gradle/libs.versions.toml` + `app/build.gradle.kts` + Gradle sync |
| Reach for `react-navigation` | Compose Navigation — already wired in `ui/navigation/` |
| Use `react-query` / SWR | A `Flow<T>` from a repository, collected via `collectAsStateWithLifecycle()` |
| Throw together a quick Modal | `ModalBottomSheet` + `NeoBottomSheet` wrapper |
| `if (Platform.OS === 'android')` | Always Android. No platform branches needed. |
| Look for the "Reload" shake gesture | Android Studio's "Apply Changes" button or a full ▶️ |

## What about Flipper / Reactotron equivalents?

Built into Android Studio. See `docs/locale/03-debugging.md`:
- **Layout Inspector** ≈ React DevTools.
- **Database Inspector** ≈ Flipper Database plugin (for Room).
- **Network Inspector** ≈ Flipper Network plugin (rarely needed in this offline-first app).
- **Background Task Inspector** ≈ Flipper Sonarscan — but actually better; shows live WorkManager state.
- **Logcat** ≈ Reactotron logs.

## Summary

- ❌ Expo Go can't run this app — wrong runtime entirely.
- ❌ Don't try to add React Native to the project — you'd lose every native feature the spec demands.
- ✅ **Compose Previews** = your Storybook-with-hot-reload, but built in.
- ✅ **Live Edit + wireless ADB** = your Expo-Go-on-phone equivalent.
- ✅ Both work after one Android Studio install. Nothing extra needed.

Once you internalize that the phone-with-USB-debugging *is* the dev environment (not a deploy target), the workflow stops feeling alien.
