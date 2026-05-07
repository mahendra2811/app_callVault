---
description: Install the latest debug APK on a connected device and launch the app.
---

Install + launch callNest on a connected device.

Steps:

1. Check a device is connected:

   ```bash
   adb devices
   ```

   If no device, surface to user: "Connect a phone with USB debugging enabled, or start an emulator (Android Studio → Device Manager). API 26+ required."

2. Check the APK exists:

   ```bash
   ls -lh "/home/primathon/Documents/p_projet/a_APP/4. callNest/app/build/outputs/apk/debug/app-debug.apk"
   ```

   If missing, suggest `/build` first.

3. Confirm with the user before installing (writes to their device):

   > "Install app-debug.apk to {device}?"

4. On confirmation, install:

   ```bash
   adb install -r "/home/primathon/Documents/p_projet/a_APP/4. callNest/app/build/outputs/apk/debug/app-debug.apk"
   ```

   `-r` reinstalls, preserving data.

5. If install fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, ask the user before uninstalling:

   > "Existing install has incompatible signature. Uninstall first (will wipe app data)?"
   > Then `adb uninstall com.callNest.app` and retry.

6. Launch:

   ```bash
   adb shell am start -n com.callNest.app/.MainActivity
   ```

7. Suggest `/logs` to tail logcat.
