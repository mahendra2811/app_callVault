# Play Store Submission Plan — callNest

> **Status:** drafted 2026-05-17, not yet implemented. Implement in a future
> session. Reviewed and revised with user decisions on branch split,
> applicationId rename, Anthropic removal, and privacy hosting in
> callNest-web repo.

## Context

callNest v1.0.1 is distributed as a sideloaded APK from
`https://callnest.pooniya.com/download`. Goal: additionally publish on
**Google Play Store** without surrendering the full-featured sideload build
that power-users rely on.

User-locked decisions baked into this plan:

- **Strategy**: dual distribution via a **branch split**, not Gradle flavors.
- **`main` branch** continues as the sideload build (full features).
- **`playstore-main` branch** is the Play Store build — call-log history,
  floating bubble, post-call popup, Anthropic AI, self-update permission,
  and BootReceiver are all stripped.
- **`applicationId`** changes to `com.callnest.app` on **both branches**
  (existing sideload users will need to back up → uninstall → install →
  restore).
- **Anthropic BYOK** is removed everywhere — both branches — since AI
  features are not being pursued right now.
- **Privacy policy** is hosted in the `callNest-web` repo and cross-linked
  from both repos' `CLAUDE.md` files.

---

## TL;DR — First-submission outlook after these changes

| Policy area | Risk | Why |
|---|---|---|
| Target SDK / signing / AAB | LOW | targetSdk 35, signed, R8 minified, AAB switch is trivial. |
| Data Safety form | LOW | Surface shrinks once Anthropic + call-log are removed. |
| Privacy policy URL | LOW | Will live at `https://callnest.pooniya.com/privacy`. |
| `REQUEST_INSTALL_PACKAGES` | NONE | Removed from manifest. |
| `READ_CALL_LOG` | NONE | Removed from `playstore-main`. |
| Floating overlay | NONE | Bubble commented out on `playstore-main`. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | NONE | Service removed with the bubble. |

Probability of passing first submission with this plan applied:
**~85% pass / ~15% one-round-trip on a paperwork question.**

---

## 1. The branch split

### 1.1 Why a branch and not a Gradle flavor

| | Branches (chosen) | Gradle flavors (rejected) |
|---|---|---|
| Maintenance | Two long-lived branches; cherry-pick fixes | One source tree; per-flavor stubs |
| Build surface | Pure — each branch sees only its own code | Mixed — devs must remember which flavor they're in |
| Risk of leaking restricted code | Lower | Higher (a missed `if (BuildConfig.FLAVOR …)` ships everywhere) |
| CI complexity | Two pipelines (already the norm) | Two product flavors in one pipeline |

Branches win on "Play reviewer cannot find any reference to `READ_CALL_LOG`
anywhere in the uploaded bundle".

### 1.2 The cut

```
main                                    playstore-main
└── full sideload build, signed APK     └── lean Play Store build, signed AAB
    applicationId com.callnest.app          applicationId com.callnest.app
    callnest-latest.apk on web              uploaded to Play Console
```

Create `playstore-main` from `main` at the current `HEAD`. Apply the changes
in §3 only on `playstore-main`. Keep both branches receiving bugfixes (use
`git cherry-pick` from `main` → `playstore-main` for cross-cutting fixes).

Document this in `DECISIONS.md` and `RELEASE-CHECKLIST.md`.

---

## 2. Changes that apply to BOTH branches

These are pure cleanup; they make the sideload build cleaner *and* unblock
Play submission.

### 2.1 Remove `REQUEST_INSTALL_PACKAGES`

The self-update flow was deleted on 2026-05-14. The permission is dead code at
[AndroidManifest.xml:13](app/src/main/AndroidManifest.xml#L13).

Action: delete the `<uses-permission>` line. Also delete:

- `UPDATE_MANIFEST_STABLE_URL` + `UPDATE_MANIFEST_BETA_URL` BuildConfig
  fields at [app/build.gradle.kts:52-61](app/build.gradle.kts#L52-L61).
- The in-app doc `app/src/main/assets/docs/14-self-update.md`.
- Any string in `strings.xml` that mentions the auto-update flow.

Users continue to update via the website link (already documented at
`MoreScreen.kt:95`).

### 2.2 Switch `applicationId` to `com.callnest.app`

Current: `applicationId = "com.callvault.app"` at
[app/build.gradle.kts:39](app/build.gradle.kts#L39).
New: `applicationId = "com.callnest.app"` on both branches.

**Migration impact for existing sideload users:** Android treats this as a
*different app*. The old `com.callvault.app` install does **not** auto-upgrade.
Users must:

1. Open old `com.callvault.app` → Settings → Backup → save a backup file.
2. Uninstall old app.
3. Install new `com.callnest.app` build.
4. Restore from backup file.

Note this in `CHANGELOG.md` and in a one-time WhatsApp broadcast (template
lives in `callNest-web/TESTER-DISTRIBUTION.md`; add a new "applicationId
migration" template alongside).

Files to touch besides Gradle:

- `app/src/main/AndroidManifest.xml` — implicit; manifest placeholders use
  `${applicationId}`, so authority strings auto-update. Just verify FileProvider
  authority + AppAuth redirect URI work after the rename.
- Any hardcoded `com.callvault.app` strings — search the tree and replace.

### 2.3 Remove all Anthropic BYOK code

Audit-confirmed touchpoints:

| File | Action |
|---|---|
| `app/src/main/java/com/callNest/app/data/ai/AnthropicClient.kt` | Delete file |
| Any `ai/` package siblings (use-cases, prompt builders) | Delete |
| Settings screen — AI/BYOK section | Remove UI + ViewModel state |
| `app/build.gradle.kts` — any `ANTHROPIC_*` BuildConfig field or dependency | Remove |
| `app/src/main/assets/docs/15-privacy.md` — "AI features (BYOK)" line | Edit out |
| `strings.xml` — any `ai_*` or `anthropic_*` strings | Delete |
| `proguard-rules.pro` — any Anthropic-related keep rules | Remove |
| `MEMORY.md` / `.claude/docs/` — references to AI feature | Update |

This shrinks the Data Safety form (one less third-party destination) and
removes any prompt-injection risk surface the Play reviewer would ask about.

### 2.4 Fix `BootCompletedReceiver`

**What it does** (read
[BootCompletedReceiver.kt](app/src/main/java/com/callNest/app/data/work/BootCompletedReceiver.kt)):
When the user reboots their phone, Android delivers a `BOOT_COMPLETED`
broadcast. The receiver wakes up, checks user settings, and:

1. Re-schedules the periodic **call-log sync** (WorkManager job).
2. Re-starts the **real-time call enrichment service** (the foreground
   service that powers the in-call bubble + post-call popup).

**Do we still need it? Branch-by-branch:**

- **`main` branch (sideload)**: YES. Without it, after every reboot the user
  loses sync + bubble until they manually re-open the app.
- **`playstore-main` branch**: **NO.** Once we remove call-log sync and
  comment out the bubble (§3), the receiver has nothing left to do. **Delete
  the receiver entirely** along with `RECEIVE_BOOT_COMPLETED` permission.
  That removes one more "restricted permission" the Play reviewer can ask
  about.

The `exported="true"` security concern from the original audit disappears on
`playstore-main` because the receiver is gone. On `main`, change it to
`exported="false"` — system broadcasts still reach non-exported receivers
on API 26+, so nothing breaks, but the receiver stops being exposed to other
apps on the device.

### 2.5 Switch release output to AAB (Play) while keeping APK (sideload)

Both formats use the same signing config. The Gradle release pipeline already
produces an APK; add an AAB target on `playstore-main`:

```kotlin
// playstore-main branch, app/build.gradle.kts
android {
    bundle {
        language { enableSplit = true }
        density  { enableSplit = true }
        abi      { enableSplit = true }
    }
}
```

`./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
That goes to Play Console. The sideload `main` branch keeps producing APKs.

### 2.6 Strip the `tools:replace` workaround for AppAuth

[AndroidManifest.xml:114](app/src/main/AndroidManifest.xml#L114) uses
`tools:node="replace"` to override the AppAuth library's
`RedirectUriReceiverActivity`. Re-verify this still works in a release build
with R8. Google's automated pre-launch scan runs the actual binary and will
catch a broken OAuth callback.

If Google Sign-In is dropped on `playstore-main` (Google OAuth is currently
scaffolded-but-commented per the codebase audit), remove the AppAuth dep +
manifest activity entirely on that branch.

---

## 3. Changes that apply ONLY to `playstore-main`

### 3.1 Drop `READ_CALL_LOG` and historical sync

Files affected on `playstore-main` only:

- `app/src/main/AndroidManifest.xml` — remove `<uses-permission READ_CALL_LOG>`.
- `app/src/main/java/com/callNest/app/data/system/CallLogReader.kt` — **comment
  out** the body of the read methods; return empty list. (Comment, don't
  delete — easier to merge bugfixes from `main` later.)
- `app/src/main/java/com/callNest/app/domain/usecase/SyncCallLogUseCase.kt` —
  short-circuit to no-op.
- `app/src/main/java/com/callNest/app/data/work/SyncCallLogWorker.kt` (if
  named similarly) — short-circuit.
- `app/src/main/res/values/strings.xml` — soften onboarding copy that
  promises "imports past 90 days of calls". On Play, the Calls screen starts
  empty and fills as new calls happen.
- Onboarding flow — skip the "Grant Call Log permission" step.

What the user sees: an empty Calls screen on first launch, populating from
the first inbound/outbound call the user makes after install. Live calls
still work via `READ_PHONE_STATE` (allowed; no permissions declaration form
needed).

### 3.2 Comment out the floating in-call bubble

Files affected on `playstore-main` only:

- `app/src/main/AndroidManifest.xml`:
  - Remove `<uses-permission SYSTEM_ALERT_WINDOW>`.
  - Remove `<uses-permission FOREGROUND_SERVICE_SPECIAL_USE>`.
  - Remove the `<property PROPERTY_SPECIAL_USE_FGS_SUBTYPE>` block at lines
    48–50.
  - Remove the `<service CallEnrichmentService>` declaration at lines 81–84.
- `app/src/main/java/com/callNest/app/data/service/CallEnrichmentService.kt`
  — comment out the class body.
- `app/src/main/java/com/callNest/app/data/service/overlay/FloatingBubbleView.kt`
  — comment out the class body.
- `app/src/main/java/com/callNest/app/data/service/overlay/PostCallPopupView.kt`
  — comment out. (The post-call popup is also an overlay — drop it on
  `playstore-main` too.)
- `app/src/main/java/com/callNest/app/util/RealTimeServiceController.kt` —
  short-circuit to no-op.
- Onboarding flow — skip the "Grant Display over other apps" step.

What the user sees: no floating bubble during calls, no post-call popup.
Notes/tags go via the regular Calls list afterwards. This is a real feature
loss for the Play build — power users keep using the sideload build for it.

### 3.3 Delete `BootCompletedReceiver` (per §2.4)

- Remove the `<receiver>` block at
  [AndroidManifest.xml:102–109](app/src/main/AndroidManifest.xml#L102-L109).
- Remove `<uses-permission RECEIVE_BOOT_COMPLETED>` at
  [AndroidManifest.xml:17](app/src/main/AndroidManifest.xml#L17).
- Delete `BootCompletedReceiver.kt`.

### 3.4 `SCHEDULE_EXACT_ALARM` review

Used for follow-up reminders. Allowed on Play under the "reminders" use case
but reviewers may ask. Keep it on `playstore-main` and document in the
Permissions Declaration form: "User explicitly schedules a follow-up at a
specific time; inexact alarms would slip the reminder by minutes and break
the reliability promise of the feature." Low risk.

---

## 4. Listing & Play Console setup

### 4.1 Developer account

- Google Play Console account, **$25 USD one-time**.
- Government ID + tax info — verification 1–7 days.
- New accounts created after Nov 2023 must run **closed testing with ≥12
  testers for ≥14 days** before promoting to production. Plan for this.

### 4.2 Privacy Policy — hosted in `callNest-web` repo

1. In `callNest-web` repo (`/home/primathon/Documents/p_projet/a_web/callNest-web`):
   - Create a new content file. Likely `src/pages/privacy.astro` or
     `src/content/legal/privacy.md` depending on Astro layout.
   - Content comes from `app/src/main/assets/docs/15-privacy.md`, edited to
     reflect the Play Store distribution + Anthropic removal.
   - Publishes at `https://callnest.pooniya.com/privacy`.
2. Update `callNest-web/CLAUDE.md` to include a "Privacy policy lives at
   `src/pages/privacy.astro` — keep in sync with callNest Android repo's
   `app/src/main/assets/docs/15-privacy.md`" reference.
3. Update **this** repo's `CLAUDE.md` to add a "Privacy policy is authoritative
   in callNest-web repo at `src/pages/privacy.astro`; mirrored in
   `app/src/main/assets/docs/15-privacy.md`" reference, and a "Cross-repo:
   see also callnest-web `CLAUDE.md`" line.
4. Add a canonical link in the in-app Privacy article pointing users to the
   public URL.

### 4.3 Data Safety form (after Anthropic removal)

| Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|
| Email (Supabase Auth) | Yes | No | Account management | Required |
| User IDs (Supabase userId, FCM token) | Yes | No | Account, app function | Required |
| App interactions (PostHog) | Yes | No | Analytics | Opt-in, OFF by default |
| Crash + diagnostics (Sentry) | Yes | No | Crash diagnostics | Opt-in, OFF by default |
| Call log / contacts | No | No | On-device only | n/a |
| ~~AI prompts (Anthropic BYOK)~~ | ~~Yes~~ | ~~Yes~~ | **REMOVED** | — |

Security claims: TLS in transit; encryption at rest in Supabase Postgres;
account deletion supported.

### 4.4 `google-services.json`

Currently missing in [app/](app/). FCM silently no-ops without it. Place
before producing the release AAB. Same file works on both branches.

### 4.5 Listing assets

- App icon 512×512 PNG (export from existing adaptive icon).
- Feature graphic 1024×500 PNG (need to design).
- 2–8 phone screenshots: Calls list, Insights, Tags, Backup/Export, Lead
  score, Onboarding. **No floating bubble screenshots** on the Play listing
  — they'd promise a feature the Play build doesn't have.
- Short description ≤80 chars.
- Full description ≤4000 chars.
- Category: `Business`.
- Content rating: target "Everyone".
- Target audience: 18+.

---

## 5. Files that will change — concrete list

### Both branches (apply on `main` first, branch off after)

| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml:13` | Delete `REQUEST_INSTALL_PACKAGES` |
| `app/src/main/AndroidManifest.xml:104` | `exported="true"` → `exported="false"` |
| `app/build.gradle.kts:39` | `applicationId` → `com.callnest.app` |
| `app/build.gradle.kts:52-61` | Delete `UPDATE_MANIFEST_*` BuildConfig fields |
| `app/src/main/assets/docs/14-self-update.md` | Delete |
| `app/src/main/assets/docs/15-privacy.md` | Edit: remove AI mention, point to web URL |
| `app/src/main/java/com/callNest/app/data/ai/` (whole package) | Delete |
| Settings screen — AI/BYOK section | Remove |
| `strings.xml` — `ai_*` keys | Remove |
| `proguard-rules.pro` — any Anthropic keep rules | Remove |
| `CHANGELOG.md` | Add migration note for applicationId rename |
| `RELEASE-CHECKLIST.md` | Document branch-split workflow + applicationId migration |
| `DECISIONS.md` | Append: branch split, applicationId rename, Anthropic removal |
| `CLAUDE.md` | Add cross-repo reference to callnest-web privacy policy |

### `playstore-main` branch only (after branch split)

| File | Change |
|---|---|
| `AndroidManifest.xml` lines 5,10,16,17,48-50,81-84,98-109 | Remove call-log, overlay, FGS-specialUse, boot-receiver |
| `CallLogReader.kt` | Comment out body |
| `SyncCallLogUseCase.kt` | Short-circuit to no-op |
| `BootCompletedReceiver.kt` | Delete file |
| `CallEnrichmentService.kt` | Comment out |
| `FloatingBubbleView.kt` | Comment out |
| `PostCallPopupView.kt` | Comment out |
| `RealTimeServiceController.kt` | Short-circuit |
| Onboarding flow | Skip overlay + call-log permission screens |
| Calls screen empty state | New copy: "Your calls will appear here as they happen" |
| `app/build.gradle.kts` | Add `bundle { … }` for AAB splits |

### `callNest-web` repo

| File | Change |
|---|---|
| `src/pages/privacy.astro` (or equivalent) | New — publishes at /privacy |
| `CLAUDE.md` | Add reference to Android-repo CLAUDE.md |

---

## 6. Order of operations

1. **Pre-flight (on `main`):**
   - Back up current keystore.
   - Sign in to Play Console, register `com.callnest.app`. Reserve the name.

2. **Cleanup on `main`** (§2):
   - Remove `REQUEST_INSTALL_PACKAGES`, fix BootReceiver export, delete
     self-update doc, remove Anthropic package, rename applicationId.
   - Build, install, smoke-test on device.
   - WhatsApp broadcast: warn existing users about backup-before-update.
   - Commit. Tag `pre-playstore-split`.

3. **Create branch `playstore-main` from `main`:**
   ```
   git checkout main
   git pull
   git checkout -b playstore-main
   git push -u origin playstore-main
   ```

4. **Apply Play-only changes on `playstore-main`** (§3):
   - Strip permissions + manifest blocks.
   - Comment out CallLogReader, BubbleView, PostCallPopup, services.
   - Skip onboarding screens.
   - Update strings + empty-state copy.
   - Build a release AAB.

5. **Host the privacy policy** (§4.2):
   - Author the Astro page in `callNest-web`.
   - Push, verify Cloudflare Pages deploys it at
     `https://callnest.pooniya.com/privacy`.
   - Cross-link both `CLAUDE.md` files.

6. **Place `google-services.json`**, regenerate the AAB.

7. **Play Console — internal testing track first:**
   - Upload the AAB.
   - Wait for pre-launch report (5 min, automated on Firebase Test Lab).
   - Fix anything it flags.

8. **Closed testing track ≥14 days, ≥12 testers** (mandatory for new
   accounts post-Nov 2023). Use the existing WhatsApp broadcast to recruit.

9. **Listing copy + assets**, Data Safety form, Permissions Declarations,
   Content Rating, Target Audience.

10. **Submit for production review.** First review: 3–7 days. Expect at
    most one round-trip on the Data Safety form or one of the permission
    justifications (`SCHEDULE_EXACT_ALARM`, `READ_CONTACTS`).

---

## 7. applicationId rename — migration consequences

Both `main` and `playstore-main` move to `applicationId = "com.callnest.app"`.

For existing sideload users on the next `main` build:

- Android treats this as a **brand new app**. The old `com.callvault.app`
  install does **not** auto-update.
- Migration path: open old app → Backup → save backup file → uninstall →
  install new `com.callnest.app` build → Restore from backup.
- One-time WhatsApp broadcast must warn users before publishing the next
  sideload release. Template lives in
  `callNest-web/TESTER-DISTRIBUTION.md`; add a new "applicationId migration"
  template alongside.
- The first `com.callnest.app` sideload release **must** prominently surface
  the Backup-then-Restore flow in onboarding so users don't lose data.
- `CHANGELOG.md` headline for that release: "Package renamed to
  `com.callnest.app`. Existing users: please back up before updating."

---

## 8. Verification — how we know it's ready

Before clicking *Send for review* in Play Console (on `playstore-main` build):

1. **Manifest dump** must show ONLY these `uses-permission`:
   ```
   READ_CONTACTS
   WRITE_CONTACTS
   READ_PHONE_STATE
   POST_NOTIFICATIONS
   SCHEDULE_EXACT_ALARM
   USE_EXACT_ALARM
   FOREGROUND_SERVICE
   FOREGROUND_SERVICE_DATA_SYNC
   WAKE_LOCK
   VIBRATE
   INTERNET
   ACCESS_NETWORK_STATE
   ```
   No `READ_CALL_LOG`, no `SYSTEM_ALERT_WINDOW`, no
   `FOREGROUND_SERVICE_SPECIAL_USE`, no `REQUEST_INSTALL_PACKAGES`, no
   `RECEIVE_BOOT_COMPLETED`. Verify with:
   ```bash
   bundletool dump manifest --bundle=app-release.aab | grep uses-permission
   ```

2. **Bundle install + smoke test** via bundletool on a real device:
   - Sign up with email.
   - Receive a call → it appears in the Calls list (no bubble, no popup —
     expected).
   - Tag the call, write a note.
   - Export CSV.
   - Run a backup.

3. **Network sniff** with mitmproxy for 30 min — only these domains should
   appear: `*.supabase.co`, `*.posthog.com` (if user opted in),
   `*.sentry.io` (if user opted in), `fcm.googleapis.com`. No
   `*.anthropic.com`. No `pooniya.com` runtime calls.

4. **Pre-launch report**: green.

5. **Closed testing**: 14 days, 12 testers, no critical-severity issues
   from `Play Console → Quality → Android vitals`.

6. **Data Safety form** matches the network sniff exactly.

---

## 9. Realistic timeline

| Week | Activity |
|---|---|
| W1 | §2 cleanup on `main`, branch split, §3 changes on `playstore-main`. Anthropic removal. Privacy policy in `callNest-web`. |
| W2 | Internal testing upload, pre-launch report fixes, Data Safety form, listing assets. |
| W3–W4 | Closed testing with 12+ testers (mandatory). |
| W5 | Production submission. First review 3–7 days. |
| W6 | Live on Play Store (best case) — small chance of one paperwork round-trip. |

**6 weeks** end-to-end, of which 2 weeks are mandatory closed-testing wait
time you cannot compress.

---

## 10. Execution order when picked up later

1. Cleanup on `main` (§2: remove Anthropic, REQUEST_INSTALL_PACKAGES,
   self-update doc, fix BootReceiver export, applicationId rename).
2. Build & install the cleaned `main` build on the connected device, verify
   nothing user-visible broke.
3. Commit `main`. Tag `pre-playstore-split`. WhatsApp broadcast to users.
4. Create `playstore-main` branch.
5. Apply §3 changes on `playstore-main`.
6. Set up Play Console + privacy policy + Data Safety + listing assets.
7. Internal track → closed testing → production submission.
