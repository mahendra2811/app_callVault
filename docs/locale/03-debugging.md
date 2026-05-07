# 03 — Debugging

Console, DevTools, DB inspector — the Android equivalents.

## The console — Logcat

Where browser console logs go in the web, **Logcat** is where Android logs go.

### View it

Android Studio → **View → Tool Windows → Logcat** (or Cmd+6 / Alt+6).

In the filter box, paste:

```
package:com.callNest.app
```

That filters to only your app's logs.

### Print a log

The project uses **Timber** (already imported everywhere):

```kotlin
import timber.log.Timber

Timber.d("simple message")                // debug — like console.log
Timber.i("informational")                  // info
Timber.w("warning")                        // warn
Timber.e("error message")                  // error — like console.error
Timber.e(throwable, "with exception")      // error with stack trace
```

Don't use `println` or `Log.d`. Always Timber.

### Useful filters

| Filter                                           | What you see            |
| ------------------------------------------------ | ----------------------- |
| `package:com.callNest.app`                       | All callNest logs       |
| `package:com.callNest.app level:ERROR`           | Only errors and crashes |
| `package:com.callNest.app tag:CallSyncWorker`    | Just the sync worker    |
| `package:com.callNest.app & "TelephonyCallback"` | String search           |

### CLI alternative

```bash
adb logcat -c                                     # clear buffer
adb logcat -s "callNest:*" "AndroidRuntime:E"   # tail forever
```

## React DevTools equivalent — Layout Inspector

**Tools → Layout Inspector**.

1. With the app running on a phone/emulator, click "Start a new live inspection."
2. Pick `com.callNest.app`.
3. You see a tree of every Composable currently on screen, with its state.

Roughly equivalent to React DevTools' "Components" tab. You can:

- Click any node to see its parameters and modifier chain.
- Toggle "Show recomposition counts" — like React DevTools' highlight-on-render mode.
- See the rendered bounds overlaid on the live screen.

## Database Inspector

callNest stores everything in a local SQLite database called `callNest.db`. To browse it live:

**Tools → App Inspection → Database Inspector → pick `com.callNest.app` → `callNest.db`.**

You can:

- Browse every table (`calls`, `tags`, `notes`, `contact_meta`, etc.).
- Run ad-hoc SQL.
- Watch rows update live as the app does things.

Useful queries (paste into the SQL pane):

```sql
-- Last 10 calls
SELECT date, normalizedNumber, type, duration FROM calls ORDER BY date DESC LIMIT 10;

-- Contact aggregates
SELECT displayName, totalCalls, computedLeadScore, isAutoSaved FROM contact_meta ORDER BY lastCallDate DESC LIMIT 20;

-- Find a number's whole history
SELECT * FROM calls WHERE normalizedNumber = '+919876543210' ORDER BY date;

-- Search notes (FTS)
SELECT n.content FROM notes n JOIN note_fts f ON n.id = f.docid WHERE note_fts MATCH 'price*';
```

This is the equivalent of opening DBeaver or pgAdmin against your local Postgres.

## The Network tab equivalent

callNest is **offline-first**. The only outbound network call is to a `versions.json` URL for self-update. So the network inspector mostly just sits there.

If you do need it:

**Tools → App Inspection → Network Inspector**. Same place as Database Inspector.

## Background jobs — Background Task Inspector

Same panel, third tab: **Background Task Inspector**.

Shows every WorkManager job:

- `CallSyncWorker` — the call-log sync (every 15 min by default).
- `DailyBackupWorker` — 2 AM daily.
- `UpdateCheckWorker` — weekly.
- `LeadScoreRecomputeWorker` — fires when weights change.

You can see when each was last run, when it's next scheduled, and what its result was. Closest equivalent: looking at a cron dashboard or Vercel cron logs.

## Setting breakpoints

Same as VS Code:

1. Click the gutter next to any line.
2. Click **Debug** (🐞 icon, next to ▶️).
3. App launches with the debugger attached. Breakpoints pause execution; inspect locals, evaluate expressions, step in/out.

For a `suspend fun` (Kotlin's async equivalent), you'll see the whole coroutine stack.

## Profiler (when things feel slow)

**View → Tool Windows → Profiler**. Equivalent to Chrome's Performance tab.

- **CPU**: see method-level timing. Useful for the spec's "<300ms filter on 10k rows" target.
- **Memory**: heap snapshots, allocations.
- **Energy**: battery usage breakdown.

## When the app crashes

Logcat shows a stack trace starting with `FATAL EXCEPTION`. The line numbers in the trace are real — click them to jump.

Common patterns to look for:

| Stack trace top says…                      | Usually means                                                                    |
| ------------------------------------------ | -------------------------------------------------------------------------------- |
| `SecurityException` in CallLog or Contacts | A permission isn't granted. Check Settings → Apps → callNest → Permissions.      |
| `IllegalStateException: Cannot find ...`   | Hilt graph misconfigured. Look for missing `@Inject` or `@Provides`.             |
| `NullPointerException` deep in Compose     | A `mutableStateOf(null)` consumer didn't handle null.                            |
| Worker `Result.failure()`                  | Check the worker's body in Logcat — the `Timber.e(...)` will tell you.           |
| `ANR` (App Not Responding)                 | You blocked the main thread. Wrap heavy work in `Dispatchers.IO` via coroutines. |

## Common things to check when stuff doesn't work

- **App permissions**: Settings → Apps → callNest → Permissions. Did the user deny call log or contacts?
- **Overlay permission** (for the floating bubble): Settings → Apps → callNest → "Display over other apps".
- **Battery whitelist** (for the foreground service to stay alive): Settings → Battery → "Don't optimize" for callNest. Many OEMs need extra steps — see `app/src/main/assets/docs/12-oem-battery.md`.
- **Database state**: open Database Inspector and verify rows exist where you expect.
- **DataStore state**: in Database Inspector, browse `Preferences` (it's a sub-tab). All settings live there.

## Next: open `04-common-tasks.md` for "I want to do X" recipes.
