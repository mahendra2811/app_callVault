# 01 — One-time setup

This is the equivalent of installing Node + cloning the repo + `npm install`. You do it once.

## What you need installed

| Tool | Version | What it does | React/Next analogue |
|------|---------|--------------|---------------------|
| **JDK 17** | 17.x | Java runtime — Kotlin compiles to JVM bytecode | Node |
| **Android Studio** | Ladybug (2024.2.x) or newer | The IDE. Bundles the Android SDK, emulator, debugger, profiler | VS Code + a bunch of extensions, all in one |
| **Android SDK** | API 35 + API 26 platform | Compile target + min support | Browser baseline targets |
| **Gradle** | 8.10.2 | Build tool — downloads deps, compiles, packages APK | Like `npm` + `webpack` rolled into one |
| **A phone** (recommended) | Android 8+ | Real device for testing | A user's browser |

You'll mostly interact with **Android Studio**. The rest is invoked under the hood.

## Step 1 — Install JDK 17

```bash
sudo apt install openjdk-17-jdk
java -version    # should print 17.x
```

(You're on Linux already — confirmed by the project paths.)

## Step 2 — Install Android Studio

Download from https://developer.android.com/studio. Unzip, run `bin/studio.sh`. Pin to your dock.

On first launch:
1. It asks "Standard" or "Custom" install. Pick **Standard**.
2. It downloads the Android SDK (~3 GB). Let it finish.
3. It opens the welcome screen. Quit. We'll come back.

## Step 3 — Open the project

Android Studio → **Open** → navigate to:

```
/home/primathon/Documents/p_projet/a_APP/4. callVault
```

Click the folder, then **OK**. (The folder name has a space and a dot — Android Studio handles it fine.)

The IDE will spend **2–5 minutes** doing a Gradle sync — downloading every library declared in `gradle/libs.versions.toml`. You'll see a progress bar at the bottom. **Wait for it to finish.**

This is the equivalent of `npm install` after `git clone`. ~500 MB downloaded.

## Step 4 — Bootstrap the Gradle wrapper

The Gradle wrapper jar isn't checked in (intentional). Generate it once:

```bash
cd "/home/primathon/Documents/p_projet/a_APP/4. callVault"
gradle wrapper --gradle-version 8.10.2
```

If `gradle` isn't on your PATH:

```bash
sudo apt install gradle    # any 8.x is fine, just used to bootstrap
```

After this you have a `./gradlew` script in the project root. From now on, every command uses `./gradlew` — never the system `gradle`.

## Step 5 — First build (sanity check)

```bash
./gradlew assembleDebug
```

This will take **3–8 minutes** the first time. Subsequent builds are 30–90 seconds.

> ### Common first-build failures
>
> **`AndroidX dependencies, but the android.useAndroidX property is not enabled`**
> A `gradle.properties` file at the project root is missing or doesn't set the AndroidX flag. Confirm it exists and has at minimum:
> ```properties
> android.useAndroidX=true
> android.enableJetifier=false
> ```
> Then retry the build.
>
> **Gradle deprecation warning about a project name with `" "`, `/`, etc.**
> Comes from running the *system* `gradle wrapper` command — harmless. Once `./gradlew` is used, you're on Gradle 8.10.2 and the warning goes away.
>
> **Other compile / KSP / Hilt errors**
> Paste the full output to Claude — the `callvault-build-fixer` agent knows the common patterns.

When it finishes:

```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

You should see a ~20 MB APK. **You just built the app from source.** This is what you'll install on a phone.

If the build fails: that's expected on a first try. Run the failing output through the `callvault-build-fixer` Claude agent — it knows the common KSP/Hilt/Room failure modes for this project.

## Step 6 — Get a phone or emulator

### Option A: Real phone (recommended)

CallVault's whole job is reading the call log. An emulator has an empty call log, so most of the app will look broken.

1. On the phone: **Settings → About phone → tap Build number 7 times** → Developer options unlocks.
2. **Settings → System → Developer options** → enable **USB debugging**.
3. Plug the phone into your laptop with USB. **Accept the RSA fingerprint prompt** on the phone.
4. Verify:
   ```bash
   adb devices
   ```
   You should see your phone listed as `device` (not `unauthorized`). If it says `unauthorized`, accept the prompt on your phone. If `adb` isn't found:
   ```bash
   export PATH=$PATH:$HOME/Android/Sdk/platform-tools
   ```
   (Add to `~/.bashrc` to persist.)

### Option B: Emulator (good for UI work only)

Android Studio → **Tools → Device Manager → Create Device → Pixel 7 → API 35 (UpsideDownCake)**. Click ▶️ to launch.

You'll have a virtual phone running on your laptop. Install + launch from Android Studio's Run button.

## Step 7 — Verify the install path

```bash
./gradlew installDebug
adb shell am start -n com.callvault.app/.MainActivity
```

The app should launch on your phone. You'll see the **Welcome** onboarding screen.

If you see "App not installed", run `adb uninstall com.callvault.app` and retry.

## You're set up.

Next: open `02-run-the-app.md` to learn the daily dev loop.
