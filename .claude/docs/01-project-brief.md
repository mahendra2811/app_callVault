# Project Brief

## Product

CallVault is an offline-first Android app that turns a phone's call history into a lightweight inquiry CRM. The target user is an Indian small-business owner receiving roughly 20-100 inquiry calls per day.

Core jobs:

- Import calls from `CallLog.Calls`.
- Normalize phone numbers, resolve contacts, SIM slot, carrier, and call metadata.
- Separate true contacts from unsaved inquiries.
- Auto-save unsaved inquiry numbers into a "CallVault Inquiries" contact group.
- Let users tag, note, bookmark, follow up, search, filter, and export calls.
- Compute lead scores from frequency, duration, recency, saved/contact state, follow-ups, tags, and rule boosts.
- Show real-time in-call bubble and post-call popup when permissions allow.
- Back up locally with encrypted `.cvb` files.
- Self-update through hosted APK manifests, not Play Store.

## Current Status

Version in `app/build.gradle.kts`: `1.0.0`, `versionCode = 1`.

`CHANGELOG.md` says 13 sprints are shipped, with full v1 feature coverage and known limitations. The app has 245 Kotlin files under `app/src/main`, 15 in-app docs articles, 3 unit tests, and no landed instrumentation tests.

Known caveat: `CallVaultNavHost` still contains a `FilterPresets` placeholder route and comments in a few files mention older sprint placeholders even though later code exists. Trust code behavior over stale comments.

## Locked Tech Stack

- Kotlin `2.0.21`
- Android Gradle Plugin `8.7.3`
- Gradle `8.10.2`
- compileSdk / targetSdk `35`
- minSdk `26`
- JVM target `17`
- Jetpack Compose BOM `2024.12.01`
- Material 3
- Compose Navigation `2.8.5`
- Hilt `2.53.1` with KSP
- Room `2.6.1` with KSP
- DataStore Preferences `1.1.1`
- WorkManager `2.10.0`
- Kotlin coroutines `1.9.0`
- kotlinx.serialization `1.7.3`
- kotlinx.datetime `0.6.1`
- `io.michaelrocks:libphonenumber-android:8.13.50`
- Apache POI `5.2.5`
- iText core `8.0.5`
- Coil `3.0.4`
- Vico `2.0.0-beta.4`
- Tink `1.15.0`
- Timber `5.0.1`
- Tests: JUnit5 `5.11.4`, Turbine `1.2.0`, MockK `1.13.13`

Do not silently upgrade major versions. If a dependency must change, document the reason in `DECISIONS.md`.

## Privacy And Distribution

CallVault is sideloaded. It declares `REQUEST_INSTALL_PACKAGES` for self-update install flows and uses a FileProvider for APK handoff.

Do not add:

- Firebase
- Crashlytics
- Google Analytics
- Play Services
- server sync
- cloud backup
- remote user logging
- hidden telemetry

Allowed outbound network behavior: update manifest checks to URLs configured by `BuildConfig.UPDATE_MANIFEST_STABLE_URL` and `BuildConfig.UPDATE_MANIFEST_BETA_URL`.

## Build Notes

The repository does not ship `gradle/wrapper/gradle-wrapper.jar`. Generate once with:

```bash
gradle wrapper --gradle-version 8.10.2 --distribution-type bin
```

Then:

```bash
./gradlew assembleDebug
```

Agents should not run Gradle or network-bound dependency downloads unless the user explicitly asks for a build/test run. This is a project rule in `CLAUDE.md` because first builds download large dependencies.

## Android Permissions

Main manifest permissions:

- Call data: `READ_CALL_LOG`, `READ_PHONE_STATE`
- Contacts: `READ_CONTACTS`, `WRITE_CONTACTS`
- Notifications: `POST_NOTIFICATIONS`, `VIBRATE`
- Overlay and exact alarms: `SYSTEM_ALERT_WINDOW`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`
- Services and reboot: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`
- Updates/network: `REQUEST_INSTALL_PACKAGES`, `INTERNET`, `ACCESS_NETWORK_STATE`

Critical runtime permissions are handled through `PermissionManager` and onboarding/rationale screens.

