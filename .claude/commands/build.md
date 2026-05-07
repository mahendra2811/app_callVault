---
description: Run `./gradlew lint assembleDebug` to verify the build, then triage any errors via the callNest-build-fixer agent.
---

The user wants to build the project. This is the only context where running `./gradlew` is allowed.

Steps:

1. Confirm with the user before running — first build downloads ~500 MB of dependencies.

   > "Building will download dependencies and may take 3–8 minutes the first time. Proceed?"

2. If they confirm, run:

   ```bash
   cd "/home/primathon/Documents/p_projet/a_APP/4. callNest"
   ./gradlew --offline lint assembleDebug 2>&1 | tee /tmp/cv-build.log
   ```

   Use `--offline` if there's any chance the deps are already cached.
   If `--offline` fails with "no cached version", retry without `--offline`.

3. Look at the tail of `/tmp/cv-build.log`:

   ```bash
   tail -80 /tmp/cv-build.log
   ```

4. **If build succeeded**:
   - Report APK path: `app/build/outputs/apk/debug/app-debug.apk`
   - Report APK size: `du -h app/build/outputs/apk/debug/app-debug.apk`
   - Report lint warnings count: `grep -c "warning:" /tmp/cv-build.log`
   - Suggest `/install` to push to a device.

5. **If build failed**:
   - Extract the first error block from the log.
   - Spawn the `callNest-build-fixer` agent with the error block.
   - After it makes fixes, re-run this command.

6. Cap at 3 fix iterations. After 3, stop and surface remaining errors to the user with a summary.

Never `gradlew clean` reflexively — usually unnecessary and slow.
