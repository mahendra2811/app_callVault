---
description: Tail logcat filtered to CallVault + critical Android errors. Runs in the background; you'll see new lines as the app emits them.
---

Tail logs filtered to CallVault.

Run this in the background so the user can keep working:

```bash
adb logcat -c   # clear buffer first
adb logcat \
  -s "CallVault:*" \
  -s "AndroidRuntime:E" \
  -s "System.err:*" \
  -s "WorkManager:*" \
  -s "Hilt:*"
```

Use the Bash tool with `run_in_background: true` so it streams.

Surface key signals when you see them:
- Crash stack traces → flag immediately, suggest `callvault-build-fixer` agent.
- `IllegalStateException: Cannot find ...` → likely a Hilt graph issue.
- WorkManager `Worker result FAILURE` → flag the worker name.
- `SecurityException` in CallLog/Contacts paths → permission issue, suggest user check Settings → Apps → CallVault → Permissions.

Tip for the user: to see only WorkManager scheduling, filter by `WorkManager`. To watch sync, the tag is `CallSyncWorker`.

To stop later: kill the background bash process or `adb logcat -G 16K` (clears buffer).
