# Release Checklist — callNest

End-to-end steps to ship a new APK build to testers. Tuned for the actual
two-repo layout: this Android repo (`app_callVault`) and the marketing site
repo (`web_callNest_marketing`). Follow top to bottom; nothing is optional
unless explicitly marked.

> **Default cadence:** patch version (1.0.0 → 1.0.1) for bug-fix + UX-polish
> releases. Minor bump (1.0.x → 1.1.0) only when a new top-level feature lands.

## 0. Prerequisites (one-time)

- `keystore.properties` exists at this repo root and points to a valid `.jks`.
- `keystore.properties`, `*.jks`, and `*.keystore` are in `.gitignore` (they are).
- `~/Releases/` exists and is writable (the script creates it).
- The web repo is checked out at `/home/primathon/Documents/p_projet/a_web/callNest-web`.
- `gh` CLI is authenticated (`gh auth status`). If not: `gh auth login -h github.com`.
- A connected phone (`adb devices`) for the post-release smoke test.

## 1. Pre-flight check (this repo)

```bash
# 1a. Make sure working tree is clean except for intentional changes for this release.
git status

# 1b. Build debug to catch compile breaks before the long release build.
./gradlew :app:assembleDebug

# 1c. Smoke-test the debug APK on a phone.
./scripts/run-debug.sh
```

Walk the critical paths: cold start → onboarding (first launch only) →
Calls list → pull-to-refresh → Search overlay → Insights tab → Inquiries
Save-now flow → Settings → Export Excel.

## 2. Build the signed release APK

```bash
./scripts/release.sh --bump-patch     # 1.0.0 → 1.0.1, versionCode++
# OR
./scripts/release.sh --bump-minor     # 1.0.x → 1.1.0
# OR
./scripts/release.sh --version 1.2.3  # explicit
```

The script will:

- Read & bump `versionName` / `versionCode` in `app/build.gradle.kts`.
- Run `./gradlew clean assembleRelease`.
- Copy the signed APK to `~/Releases/CallNest-<version>.apk`.
- Write `~/Releases/versions-stable.json` with sha256, size, and release notes
  (auto-pulled from `CHANGELOG.md`'s `## [Unreleased]` block).

**Verify**:

```bash
ls -lh ~/Releases/CallNest-<version>.apk
sha256sum ~/Releases/CallNest-<version>.apk
```

Note the sha256 hash — you'll paste it into `releases.ts` in the web repo.

## 3. Update CHANGELOG.md (this repo)

- Move whatever was under `## [Unreleased] — …` into a versioned section:
  `## [1.0.1] — Tester drop (YYYY-MM-DD)`.
- Add a fresh empty `## [Unreleased]` heading on top for the next cycle.
- Commit shape (do not push yet):
  ```bash
  git add CHANGELOG.md app/build.gradle.kts
  git commit -m "release: callNest v<version> (sha256 <first8>…)"
  ```

## 4. Sync to web repo

```bash
cd /home/primathon/Documents/p_projet/a_web/callNest-web
npm run sync-release
```

This script will:

- Copy `~/Releases/CallNest-<version>.apk` → `public/apk/callnest-latest.apk`.
- Write `public/versions-stable.json` with the correct hosted URL.
- Patch `src/content/releases.ts` for the matching version (or warn if it
  doesn't exist — see next step).

**If the script warned "no entry for v<version> in releases.ts"**, you need
to prepend a new `Release` entry. Use the last release as a template:

```ts
{
  version: "1.0.1",
  date: "2026-05-14",
  size: "21.3 MB",
  sha256: "<paste full sha256 here>",
  added:   [ /* one bullet per user-visible new thing */ ],
  changed: [ /* one bullet per user-visible behavior change */ ],
  fixed:   [ /* one bullet per bug fixed */ ],
  downloadUrl: "/apk/callnest-latest.apk",
},
```

Run `npm run sync-release` again after adding the entry — it'll fill in
size/sha256 automatically.

## 5. Verify the web build

```bash
cd /home/primathon/Documents/p_projet/a_web/callNest-web
npm install              # only if package-lock changed
npm run build            # must finish with "prerendered as static content"
```

Open `out/index.html` or `npm run dev` to eyeball the Download page version
chip, hash, and changelog entry.

## 6. Commit + push the web repo

```bash
cd /home/primathon/Documents/p_projet/a_web/callNest-web
git add public/apk/callnest-latest.apk public/versions-stable.json \
        src/content/releases.ts
git status
git commit -m "release: callNest v<version> (sha256 <first8>…)"
git push origin main
```

Cloudflare Pages (or whichever host is hooked up) will auto-deploy
~2 minutes after the push.

## 7. Commit + push the Android repo

```bash
cd /home/primathon/Documents/p_projet/a_APP/4.\ callVault
git push origin main
```

## 8. GitHub Release (Android repo)

```bash
cd /home/primathon/Documents/p_projet/a_APP/4.\ callVault

# Tag the commit.
git tag v<version> -m "callNest v<version>"
git push origin v<version>

# Upload the signed APK + sha as release assets.
sha256sum ~/Releases/CallNest-<version>.apk > /tmp/CallNest-<version>.sha256
gh release create v<version> \
  ~/Releases/CallNest-<version>.apk \
  /tmp/CallNest-<version>.sha256 \
  --title "Call Nest <version>" \
  --notes-file CHANGELOG.md  # or paste custom notes
```

The `.github/workflows/release.yml` workflow will also fire on tag push and
attach a fresh build, but the manual upload above is faster and uses the
already-signed APK from your machine. Pick one — don't do both with the
same tag.

## 9. Verify the public download

- Visit https://callnest.pooniya.com/download — version chip should match.
- Tap "Download APK" → check the file size matches `~/Releases/`.
- `sha256sum <downloaded.apk>` should match what's in `releases.ts`.

## 10. Tester smoke test

Install the freshly-downloaded APK on a clean phone (or one that already had
v<previous>):

```bash
adb install -r ~/Releases/CallNest-<version>.apk
adb shell am start -n com.callvault.app/com.callNest.app.MainActivity
```

Walk the critical paths from §1c again. Watch logcat for the first ~30
seconds for any unexpected exceptions:

```bash
adb logcat -v brief | grep -iE "callvault|callNest|FATAL|AndroidRuntime"
```

If anything's broken, revert the web-repo commit (testers won't have
downloaded yet — fast rollback) and start over.

## 11. Announce to testers

WhatsApp / Telegram / email the download link with one-line release notes:

> Call Nest v<version> is up at https://callnest.pooniya.com/download
>
> Highlights: <one or two bullets>
>
> If anything's broken, screen-record + send to <support email>.

Done.

---

## Troubleshooting

### `Excel export crashes with NoClassDefFoundError: SheetUtil`

POI's streaming workbook references `java.awt` which doesn't exist on
Android. The exporter is on plain `XSSFWorkbook` since v1.0.1 — don't
switch to `SXSSFWorkbook` without thoroughly testing on a real device first.

### `INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures do not match`

You're trying to install a build signed with a different keystore over an
existing install. Either uninstall the old version first
(`adb uninstall com.callvault.app`) or install the matching debug variant.

### `sync-release.mjs reports "no entry for v<version>"`

The script only patches existing entries. Add a new entry to
`src/content/releases.ts` first, then re-run the script.

### `MediaStore commit failed for content://...`

Older Android (pre-Q) fallback to `getExternalStoragePublicDirectory`
needs `WRITE_EXTERNAL_STORAGE`. Not an issue on minSdk 26 with scoped
storage; if it happens, check that no other app is holding the file open.
