./gradlew assembleDebug 
adb install -r app/build/outputs/apk/debug/app-debug.apk  
 adb shell am start -n com.callvault.app.debug/com.callvault.app.MainActivity

# CallVault — Release Plan

Path from current state to "APK live on a website", with a tight split between **what you do** and **what Claude does**. Goal: minimum touch-time from you (~1 hour total, spread across 6 short sessions).

This file is the contract. Everything else (`TODO.md`, `CHANGELOG.md`, `DECISIONS.md`, `.claude/`) supports it.

## Total budget

| Phase                | Your touch-time | Wall-clock  |
| -------------------- | --------------- | ----------- |
| 1. Green build       | ~10 min         | ~30 min     |
| 2. Phone smoke test  | ~15 min         | ~1 hr       |
| 3. P0 closure        | ~12 min         | ~1 hr       |
| 4. P1 features       | ~10 min         | ~2 hr       |
| 5. Quality bar       | ~5 min          | ~1 hr       |
| 6. Release artifacts | ~12 min         | ~1 hr       |
| **Totals**           | **~64 min**     | **~6.5 hr** |

You're on the laptop for ~1 hour total, in six brief check-ins. The rest is Claude grinding through agents while you do other work.

---

## Phase 1 — Green build

**Why you must do this**: only your laptop has Android Studio + the phone.

### You

1. Run `./gradlew assembleDebug` from the project root.
2. Paste the output (last ~80 lines is enough) into the Claude chat.
3. Repeat (1) every time Claude says "try again".

### Claude

- Diagnoses every error via the `callvault-build-fixer` agent.
- Patches `gradle.properties`, manifest, KSP, Hilt, Room, packaging excludes — whatever surfaces.
- Updates `DECISIONS.md` for any non-obvious fix.

### Done when

- `app/build/outputs/apk/debug/app-debug.apk` exists.
- ~3–6 fix iterations expected.

---

## Phase 2 — Phone install + smoke test

**Why you must do this**: only you can plug in the phone and watch the screen.

### You

1. Connect a phone with USB debugging enabled. Run `adb devices` to confirm.
2. `./gradlew installDebug && adb shell am start -n com.callvault.app/.MainActivity`.
3. Walk the 12-step smoke list in `DEVELOPING.md` §4 (onboarding → sync → calls → tags → backup → etc.).
4. Note anything that crashes, looks broken, or behaves wrong. Paste the broken-flow list + relevant logcat stack traces.

### Claude

- Triages each broken flow. Fixes via the appropriate agent.
- Re-emits each fix; you re-install (`./gradlew installDebug`, ~30 sec at this point).

### Done when

- Smoke list passes end-to-end.
- Expect ~5–10 runtime fixes; the sprint agents were scoped to file generation, not runtime correctness.

---

## Phase 3 — P0 closure

The five P0 items in `TODO.md`.

| #   | Item                    | You do                                                                                                                                                                      | Claude does                                                                                                                                                      |
| --- | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | First green build       | (Phase 1)                                                                                                                                                                   | (Phase 1)                                                                                                                                                        |
| 2   | Release keystore        | Run `keytool -genkeypair ...` once. Claude will hand you the exact command + secrets to paste into `keystore.properties`. **5 min, real interruption.**                     | Wires `signingConfigs.release` to read it. Verifies `assembleRelease` produces a signed APK.                                                                     |
| 3   | Update-manifest hosting | Decide where: **GitHub Pages** (free, easiest), Vercel, S3, your own host. Tell Claude the URL. **2 min decision; then 0 if GitHub Pages — Claude can write the workflow.** | Generates `versions-stable.json` + `versions-beta.json` skeletons. If GitHub Pages: writes `.github/workflows/publish-manifest.yml`. Updates `BuildConfig` URLs. |
| 4   | Launcher icons          | Provide a 1024×1024 PNG of your final icon. **5 min if you have one; otherwise Claude generates a placeholder you can ship as v1.**                                         | Runs it through Image Asset Studio's mipmap layout (or generates all densities + adaptive variants via script).                                                  |
| 5   | MainScaffold 5-tab nav  | Nothing.                                                                                                                                                                    | `callvault-android-engineer` wires `MainScaffold` into `CallVaultNavHost` as nested graph. Default tab Calls. ~30 min of agent time.                             |

### Total your interruption: ~12 min.

Most of the 30-min wall clock is Claude working.

---

## Phase 4 — P1 functional gaps

Items Claude does autonomously, no interruption:

- 6 remaining stats charts (SimUtilizationBar, TagDistribution, SavedUnsavedTrend, ConversionFunnel, GeoBars, DayOfWeekBars).
- PDF chart-image embedding via Compose `captureToBitmap`.
- `NeoHelpIcon` placement across ~10 screens.
- Material 3 → Neo\* swap on Update screens.
- `ResetAllDataUseCase` extension to all 13 tables.
- `DailySummaryWorker` real-counts notification.
- Onboarding firstSync error path verification.

### You

After all 7 land, do **one batch test**: `./gradlew installDebug` + ~10 min of poking through the new flows.

### Claude

- Spawns the right subagent per item.
- Reports each landing in chat.
- Updates `TODO.md` (checks the box) and `CHANGELOG.md` after each.

### Done when

- All 7 P1 items checked in `TODO.md`.
- One smoke pass on the phone clears.

### Total your interruption: ~10 min, at the end.

---

## Phase 5 — Quality bar

| Item                                | You do                                                                                            | Claude does                                                         |
| ----------------------------------- | ------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| ViewModel + DAO tests               | Run `./gradlew test connectedDebugAndroidTest` once. Paste failures if any.                       | Writes all tests via `callvault-test-writer`. Iterates on failures. |
| `@Preview` audit                    | Nothing.                                                                                          | Sweeps + fills gaps.                                                |
| Lint warnings sweep                 | Run `./gradlew lint`. Paste report path.                                                          | Triages each via `callvault-build-fixer`.                           |
| Performance check                   | On your phone, time: cold-start, filter on seeded sample data, FTS query, sync run. Send numbers. | Profiles via the Android Studio Profiler if numbers miss spec.      |
| Empty / loading / error state sweep | Nothing.                                                                                          | `callvault-ui-builder` audits every screen.                         |
| Accessibility                       | Run TalkBack on the phone for ~5 min, hit each main screen, send observations.                    | Fixes anything you flag.                                            |

### Total your interruption: ~5 min.

---

## Phase 6 — Release artifacts

| Item                                         | You do                                                                                                                                     | Claude does                                                                     |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------- |
| Privacy policy text                          | Decide what's true (data residency, no telemetry, etc.). 2 sentences max. **2 min.**                                                       | Writes `app/src/main/assets/docs/15-privacy.md` final copy.                     |
| Real screenshots                             | Take 6–8 screenshots on phone after smoke pass: `adb exec-out screencap -p > docs/screenshots/<name>.png`. **5 min.**                      | Embeds them in `README.md`.                                                     |
| ProGuard / R8                                | Nothing.                                                                                                                                   | Verifies `assembleRelease` produces a minified APK; fixes any keep-rule misses. |
| Initial git commit + first signed APK upload | `git init && git add . && git commit -m "feat: v1.0.0"`. Push to GitHub. Upload signed APK to your host or as a GitHub Release. **5 min.** | Writes release notes from `CHANGELOG.md`.                                       |
| CI workflow                                  | Nothing.                                                                                                                                   | Writes `.github/workflows/android.yml` for `lint + assembleDebug` on push.      |

### Total your interruption: ~12 min.

---

## What Claude will _never_ do without your explicit approval

- Run `./gradlew` (you run, Claude reads the output). Exception: Claude may ask "I'd like to verify with `./gradlew --offline lint` — okay?" and wait for your reply.
- Generate the keystore (must be your secret).
- Decide your manifest hosting URL.
- Touch git (commit / push / release / tag).
- Send anything to a third-party service.
- Modify the locked spec at `/home/primathon/Downloads/callvault_mega_prompt.md`.
- Modify `gradle/libs.versions.toml` (locked tech stack).

These are the boundaries. If Claude proposes one, push back.

---

## How to drive each phase

Each phase begins with you typing one short message:

| Phase | Trigger phrase                                                            |
| ----- | ------------------------------------------------------------------------- |
| 1     | Paste the failing build output, then say `go`.                            |
| 2     | After install, paste smoke-test issues, then say `triage`.                |
| 3     | `start P0` — Claude walks the keystore + manifest + icons + tabs flow.    |
| 4     | `start P1` — Claude grinds the 7 items autonomously, reports per landing. |
| 5     | `start quality` — Claude opens the test/lint/preview/a11y batch.          |
| 6     | `start release` — Claude prepares the release artifacts.                  |

End of each phase, Claude says: "Phase N done. Phase N+1 needs from you: X. Go?"

---

## Right-now action

1. **Run** `./gradlew assembleDebug` from the project root.
2. **Paste** the output (last ~80 lines).
3. **Reply** `go`.

That kicks off Phase 1.
