# 02 — Running the app + the dev loop

You finished `01-setup.md` and have a phone connected. Now the daily loop.

## TL;DR — the three modes

```
Mode                       What you change                    Speed       Tool
─────────────────────────────────────────────────────────────────────────────
Compose Preview         a single component's look            <1 sec      Android Studio split-pane
Live Edit               composables on a running app         1–3 sec     Android Studio toolbar toggle
Full reinstall          everything else                      30–90 sec   ./gradlew installDebug
```

**You'll be in Compose Preview mode 80% of the time when working on UI.** It's the closest thing to "browser tab + save".

## Mode 1 — Compose Previews (your main UI dev loop)

Like Storybook, but built-in.

1. Open any Kotlin file in Android Studio that has `@Preview`. For example:
   ```
   app/src/main/java/com/callNest/app/ui/components/neo/NeoButton.kt
   ```
2. Click the **split view** icon at the top-right of the editor (looks like two stacked rectangles).
3. The right pane renders the previews. **Save the file (Ctrl+S)** to re-render. Less than 1 second per cycle.

Tips:

- A single file can have multiple `@Preview` functions — empty state, populated state, error state. Define more if useful.
- If a preview won't render, it usually means the composable injects via Hilt. Don't preview the screen wrapper — preview the inner stateless content.
- **Interactive Preview**: click the ▶️ icon on a preview tile. It runs the composable as if on a phone — clicks work.

### React analogue

This is exactly like Storybook stories. The mental model is: **preview pure components with mock state**, then wire the data once the look is right.

## Mode 2 — Live Edit (incremental on a running app)

Closest equivalent to React Fast Refresh.

1. First, do one full install: click ▶️ in Android Studio, or run:
   ```bash
   ./gradlew installDebug && adb shell am start -n com.callNest.app/.MainActivity
   ```
2. The app is now running on your phone. Don't close Android Studio.
3. In the toolbar, find the **Live Edit toggle** (looks like a little flame) and turn it on.
4. Edit any `@Composable` function. Save. **Changes push to the phone in 1–3 seconds.**

What Live Edit handles:

- ✅ Composable bodies, `Modifier` chains, text, colors, sizes
- ✅ Adding/removing UI inside a Composable

What needs Apply Changes (Ctrl+Alt+Shift+R):

- New top-level functions
- Non-Composable Kotlin code (ViewModels, repositories)
- Resource changes (`strings.xml`)

What needs a full reinstall:

- Manifest edits (new permissions, new components)
- Hilt module changes
- Room schema changes (entities/DAOs)
- New `BuildConfig` fields
- Anything else that fails Apply Changes

If Live Edit ever stops working, a full reinstall fixes it.

## Mode 3 — Full reinstall

```bash
./gradlew installDebug
adb shell am start -n com.callNest.app/.MainActivity
```

Or just click ▶️ in Android Studio (one button does both).

30–90 seconds. Use this whenever Apply Changes warns "needs full reinstall."

## How to view the app

You see the app on your **phone screen** (or emulator). There is no browser window.

If you want to capture what's on screen for a screenshot or bug report:

```bash
# screenshot to your laptop
adb exec-out screencap -p > /tmp/cv-screenshot.png

# screen recording (max 3 min)
adb shell screenrecord /sdcard/cv-rec.mp4
# Ctrl+C to stop, then:
adb pull /sdcard/cv-rec.mp4 ~/
```

## Recommended Android Studio layout

- **Left pane**: Project tree (Cmd+1 / Ctrl+1).
- **Center**: code editor.
- **Right pane**: Compose Preview (split view).
- **Bottom**: Logcat (Cmd+6 / Alt+6) — see `03-debugging.md`.
- **Bottom-right tab**: Database Inspector (when app is running) — see `03-debugging.md`.

## A typical 10-minute task as a React dev would think of it

> "I want to add a 'Refresh' button to the Calls screen."

Mapping to what you'd actually do:

| Step           | React/Next would be…                         | In callNest you do…                                                                                                                 |
| -------------- | -------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| Find the file  | `app/(tabs)/calls/page.tsx`                  | `app/src/main/java/com/callNest/app/ui/screen/calls/CallsScreen.kt`                                                                 |
| Add a button   | `<Button onClick={refresh}>Refresh</Button>` | `NeoIconButton(icon = Icons.Default.Refresh, contentDescription = ..., onClick = onRefresh)`                                        |
| Hook up state  | `const refresh = useCallback(...)`           | Add `onRefresh: () -> Unit` param, call from `CallsViewModel.refresh()`                                                             |
| Add the string | `"Refresh"` inline                           | Add `<string name="cv_calls_refresh">Refresh</string>` to `res/values/strings.xml`, use `stringResource(R.string.cv_calls_refresh)` |
| See it         | Save → browser reloads                       | Save → Live Edit pushes to phone in ~2s                                                                                             |
| Test           | Click button in browser                      | Click button on phone                                                                                                               |

## Common gotchas

### "I clicked Run but nothing changed."

The phone might be locked or the app crashed. Check Logcat for errors. Tap the app icon on the phone home screen to launch.

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"

Existing install has a different signature.

```bash
adb uninstall com.callNest.app
./gradlew installDebug
```

### "Phone says App not installed"

Sometimes Xiaomi/Vivo's MIUI/Funtouch blocks USB installs. Enable **Install via USB** in MIUI/Funtouch developer options.

### "Preview won't render — error about Hilt"

A previewed composable is using `hiltViewModel()`. Don't preview the screen wrapper — preview the inner stateless `Content` composable that takes state as parameters.

### "Build is stuck at `Resolving dependencies`"

Network issue. Try `./gradlew assembleDebug --refresh-dependencies` or check that you can reach `dl.google.com` and `repo.maven.apache.org`.

## Next: open `03-debugging.md` for console / DevTools / DB inspector equivalents.
