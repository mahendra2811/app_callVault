# Play Store readiness — CallVault

**Honest TL;DR up front:** CallVault was originally designed as a **sideloaded** app precisely because Google Play's policies make a "call-log inspector" extremely hard to ship. Reading `CallLog.Calls` is a *Restricted Permission* (`READ_CALL_LOG`). Your app does **not** qualify as a Default Dialer / Caller-ID handler — those are the only two pre-approved use cases. **Realistic outcome: a first submission will almost certainly be rejected** unless you (a) restructure to qualify under one of the exceptions, or (b) replace the call-log capture flow with one that doesn't read the system call log.

This document covers (1) the technical readiness work you can do today, (2) the policy minefield you'll hit, and (3) the most viable paths to actually get listed.

---

## 1. Things you must do before any submission

### 1.1 App identity

| Item | Status | Action |
|---|---|---|
| Application ID | `com.callvault.app` | Reserve on Play Console; cannot change after first publish. |
| Version code / name | `versionCode 1`, `versionName "1.0.0"` | Bump per release. Auto-increment in CI. |
| Min/target SDK | min 26, target 35 | OK — Play requires target SDK 34+ as of Aug 2025. |
| Signing | Release keystore in `keystore.properties` (not in repo) | Enroll in **Play App Signing**; upload key separate from signing key. |
| ProGuard / R8 | `isMinifyEnabled = true` for release | Verify `proguard-rules.pro` covers Hilt / Room / kotlinx-serialization. |
| Splits | None today | Optional: enable `android.bundle` splits for smaller download. |
| Bundle | `bundle { language { enableSplit = true } …}` | Generate `.aab`, not `.apk`, for Play. |

### 1.2 Privacy policy — **mandatory, hosted**

Play *will not* publish without a public-internet privacy policy URL. CallVault now collects user data (Supabase auth, FCM tokens, optionally PostHog and Anthropic), so the policy must disclose:

- Personal identifiers: email, password (hashed by Supabase, but disclosed)
- Phone-number metadata: every call's number, contact name, time, duration — stored on-device, optionally synced
- Anonymous analytics (only when user opts in): screen views, event names, no PII
- AI digest payload (only when user enables): aggregate counts + first names + tag names — sent to Anthropic per BYOK
- FCM token: stored in Supabase `device_tokens` to deliver push
- Optional Drive backup: file content (encrypted) sent to user's Google Drive

Hosting options: GitHub Pages, Netlify, Vercel, your own domain. Template: https://app-privacy-policy-generator.firebaseapp.com (then heavily edit).

### 1.3 Data safety form (Play Console → App content → Data safety)

You must declare for each data type: collected? shared? collection purpose? user-deletable? encrypted in transit?

| Data type | Collected | Shared | Purpose | Optional? | Encrypted in transit? | User can delete? |
|---|---|---|---|---|---|---|
| Email | Yes | No | Account management | No | Yes (HTTPS) | Yes (account deletion screen) |
| Phone numbers (call log) | Yes (on-device) | No | App functionality | No | N/A on-device | Yes (Reset all data) |
| Contacts | Yes (on-device) | No | App functionality | No | N/A | Yes |
| Call duration / type | Yes (on-device) | No | App functionality | No | N/A | Yes |
| Diagnostics / analytics | Yes | Yes (PostHog) | Analytics | **Yes — opt-in** | Yes | Yes (turn off + reset) |
| App interactions for AI | Yes | Yes (Anthropic) | App functionality | **Yes — opt-in BYOK** | Yes | N/A (request not retained) |
| Device / FCM token | Yes | No | Push notifications | No | Yes | Yes (account deletion) |

### 1.4 Listing assets

- **App icon**: 512×512 PNG, no transparency, no rounded corners (Play applies its own mask)
- **Feature graphic**: 1024×500 PNG/JPG
- **Phone screenshots**: 2-8 per language, 1080×1920 minimum
- **Tablet screenshots**: optional but improves visibility
- **Short description**: 80 chars max
- **Full description**: 4000 chars max
- **App category**: *Business* (most realistic) or *Productivity*
- **Content rating**: complete the IARC questionnaire. CallVault should be PEGI 3 / Everyone.
- **Target audience**: 18+ (sales tool); set "Designed for families" = NO.

### 1.5 Pre-launch report fixes

When you upload an internal testing build, Play runs an automated crawl (Robo + Espresso). Common failures:

- Crashes when notifications permission is denied (Android 13+)
- ANRs from foreground service init
- Lint errors on unused permissions
- Insecure WebView usage (none — N/A)

Fix anything red before promoting to closed/open testing.

### 1.6 Signing + Build

```bash
./gradlew bundleRelease            # Produces app/build/outputs/bundle/release/app-release.aab
```

Upload this `.aab` to Play Console. Enable Play App Signing the first time.

---

## 2. The policy minefield (this is where you will get rejected)

### 2.1 🚨 `READ_CALL_LOG` — Restricted Permission

**This is the single biggest blocker.** Google's policy ([Use of SMS or Call Log permission groups](https://support.google.com/googleplay/android-developer/answer/10208820)) restricts apps that read the call log to two categories:

1. **Default phone handler** (the app the user picks as their dialer in System Settings)
2. **Default caller ID & spam app** (the app the user picks for caller-ID lookups)

CallVault is *neither* — it's a sales-CRM that reads the call log of whatever dialer the user already has. **This use case is not on the approved list.** Apps in this position routinely get rejected with:

> *"Your app violates the Use of SMS or Call Log permission groups policy. Apps that fall outside of the approved use cases must remove all use of these permissions."*

**Mitigation paths (pick one):**

| Path | Realistic? | Effort |
|---|---|---|
| **A. Submit a Permissions Declaration** explaining "core feature: call-log capture for sales productivity"; argue an undocumented exception | Low (~10% accept) — most rejected within hours. Worth trying once. | 1 day |
| **B. Become the user's default dialer.** Implement `InCallService`, replace the system dialer. Big rewrite. | Yes, this is what Truecaller, Drupe, etc. did. | 2-4 weeks of new code + re-design |
| **C. Become a default caller-ID app.** Implement `CallScreeningService` + add the system caller-ID role flow. | Yes; smaller rewrite than B but still major. | 2-3 weeks |
| **D. Ditch system call-log capture entirely.** Detect calls only via `PhoneStateListener` while the app/service is running, and have the user manually log past calls or sync from a paired CRM. | Big product change — kills the auto-capture USP. | 1 week of code, rest is product/UX redesign |
| **E. Stay sideloaded.** Don't ship to Play; distribute the APK via your own site / WhatsApp / SMS link. CallVault was designed for this. | Yes — already supported via `versions-stable.json` self-update. | Zero. |

**My honest recommendation:** Path E (sideloaded) is your *current* shipping channel. Path B/C is the only Play-friendly one if you must be on Play, and it's a **major** refactor. Path D loses the headline feature.

### 2.2 🚨 `SYSTEM_ALERT_WINDOW` — Special permission

Used for the floating bubble. Play requires:
- Justification in the **Permissions Declaration** form
- A clear in-app explanation before requesting `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
- The overlay must serve a legitimate purpose (yours does — "in-call enrichment bubble")

**Status:** likely approved if you justify it, but adds review time. **Not** an outright rejection.

### 2.3 🚨 Foreground service `specialUse` subtype

Your manifest declares:

```xml
<service ... android:foregroundServiceType="specialUse" />
<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="RealTimeCallEnrichment" />
```

Apps using `specialUse` MUST justify it in the Play Console submission. If Google decides the use case fits an existing FGS type (e.g. `phoneCall`), they will reject and ask you to switch. Argue: "live call-context overlay, doesn't fit `phoneCall` because we're not the dialer."

### 2.4 `READ_CONTACTS` / `WRITE_CONTACTS`

Less restricted than `CALL_LOG`. Justification: "auto-save inquiry numbers, CSV import." Should pass with a clear runtime-permission rationale.

### 2.5 `REQUEST_INSTALL_PACKAGES`

Used for in-app self-update. **Play Store doesn't allow this** — apps cannot update themselves outside of Play. Two paths:

- **Strip self-update from the Play build** (build-flavor `play` vs `sideload`). Make `UpdateCheckWorker` a no-op in the play flavor. **You must do this** or Play rejects.
- Replace with **Play In-App Updates API** (`com.google.android.play:app-update-ktx`) — the Play-sanctioned equivalent.

### 2.6 `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`

- `BOOT_COMPLETED`: justify (background work after reboot — daily summary, follow-ups)
- `SCHEDULE_EXACT_ALARM`: Android 12+ runtime-grant; Play requires you have a strong reason. Yours (follow-up reminders at user-chosen time) qualifies.
- `USE_EXACT_ALARM` (API 33+): the strict version, Play allows only for "alarm clock or timer" or "calendar/scheduling" use cases. Follow-up reminders qualify under "calendar" but you must justify.

### 2.7 `POST_NOTIFICATIONS`

Standard, Android 13 runtime. No issues.

### 2.8 OAuth — Google sign-in via Supabase

You scaffolded but disabled this. **Before activating** for Play:
- Verify the OAuth consent screen in Google Cloud Console
- Set the redirect URL on the Supabase side
- App branding review may take a week

### 2.9 Anthropic / PostHog / FCM

These are server-to-server. Play doesn't audit them directly, but the **Data safety form** must declare what you send. Be honest that:
- PostHog is opt-in
- Anthropic only fires if user provides their own API key
- FCM token is per-user

### 2.10 Account deletion

Play requires user-initiated account deletion accessible **without leaving the app** for any app with sign-in. ✅ You have this — `ProfileScreen` → Delete account. Good.

### 2.11 Apps targeting Indian users — DPDP Act compliance

India's Digital Personal Data Protection Act (DPDP, 2023) requires:
- Notice + consent for personal data processing
- Right to erasure (you have it)
- Privacy policy in English + the regional language of the user (your Hindi sweep helps)
- A grievance officer contact

Add these to the privacy policy.

### 2.12 Play's "high-risk" review for finance/contact apps

Apps that access call log / contacts / SMS get extra scrutiny. Expect:
- 7-14 day initial review (vs 1-3 days for vanilla apps)
- Possible rejection, multiple iterations
- Random re-review after publishing

---

## 3. Action plan, in order

### Phase 1 — Decide on the call-log path (1 day, no code)

Pick A / B / C / D / E from §2.1. **This is the gate.** Without picking, everything below is wasted effort.

If **E (sideloaded)**: stop here, this doc isn't for you. Continue distributing via your hosted manifest.

### Phase 2 — Strip Play-incompatible features (3-5 days, code)

If proceeding with Play:

- [ ] Add a `play` product flavor in `app/build.gradle.kts`
- [ ] Remove `REQUEST_INSTALL_PACKAGES` from the play flavor's manifest
- [ ] No-op `UpdateCheckWorker.schedule` in play flavor (or replace with Play In-App Updates)
- [ ] Hide the "Self-update" Settings section in play flavor
- [ ] Verify `applicationIdSuffix` per flavor doesn't clash

### Phase 3 — Privacy & legal (1-2 days, no code)

- [ ] Write privacy policy (use the data-safety table above as the skeleton)
- [ ] Host it (GitHub Pages or your domain)
- [ ] Translate to Hindi (legally not required for Play, but DPDP-friendly)
- [ ] Designate grievance officer (name + email) — DPDP requirement
- [ ] Update in-app links to point at the hosted policy

### Phase 4 — Listing assets (1 day, design)

- [ ] App icon at 512×512
- [ ] Feature graphic 1024×500
- [ ] 5-8 phone screenshots showcasing: pipeline, call detail, weekly digest, templates, Hindi UI
- [ ] Short description (80 chars)
- [ ] Full description (4000 chars) emphasizing the SMB use case
- [ ] Promo video (optional, 30 sec) — meaningfully boosts conversion

### Phase 5 — Console setup (2-3 hours)

- [ ] Create app in Play Console
- [ ] Enroll in Play App Signing
- [ ] Fill Data Safety form per §1.3
- [ ] Fill Permissions Declaration (call log, overlay, special-use FGS, exact-alarm)
- [ ] Set content rating, target audience, ads = No, in-app purchases = No (initially)
- [ ] Add testers for **internal testing track**

### Phase 6 — Test track first (1-2 weeks)

- [ ] Upload signed `.aab` to **internal testing**
- [ ] Run pre-launch report; fix anything red
- [ ] Share opt-in link with 5-10 trusted users
- [ ] Iterate

### Phase 7 — Closed → open → production

- [ ] Promote to closed track (50-100 users)
- [ ] Promote to open testing (anyone)
- [ ] Submit for production review
- [ ] **Expect 7-14 days for first review**, possibly with one rejection round

---

## 4. Specific things in the codebase right now that will trigger Play rejection

| File / config | Issue | Fix |
|---|---|---|
| `AndroidManifest.xml:14` `REQUEST_INSTALL_PACKAGES` | Play forbids self-update | Strip from play flavor (Phase 2) |
| `AndroidManifest.xml:7` `READ_CALL_LOG` | Restricted permission, will trigger Permissions Declaration review | Phase 1 decision (path B/C/D) |
| `AndroidManifest.xml:11` `SYSTEM_ALERT_WINDOW` | Sensitive overlay permission | Justify in Permissions Declaration |
| `AndroidManifest.xml:12-13` `SCHEDULE_EXACT_ALARM` + `USE_EXACT_ALARM` | Sensitive (Android 13+) | Justify as calendar/scheduling |
| `app/src/main/java/com/callvault/app/data/work/UpdateCheckWorker.kt` | Self-updating from `versions.json` | Disable in play flavor |
| `BuildConfig.UPDATE_MANIFEST_STABLE_URL` | Self-update URL | Stop reading in play flavor |
| Privacy policy URL | Not hosted | Host before submitting |
| `docs/cloud-integration.md` | Contains test/dev keys references in code blocks | Verify final aab doesn't include real keys |
| Auth consent screen | Google sign-in scaffolded but no OAuth verification | Verify if activating |

---

## 5. The honest "will it pass?" matrix

| Aspect | Risk |
|---|---|
| Crash-free, technically sound | ✅ Low — build is green, foundations solid |
| Privacy policy + Data safety form | ✅ Low — straightforward to write |
| `READ_CALL_LOG` permission declaration | 🚨 **Very high — likely outright reject unless you become a default dialer / caller-ID app** |
| `SYSTEM_ALERT_WINDOW` justification | 🟡 Medium — needs solid rationale, often approved |
| `specialUse` FGS justification | 🟡 Medium — Google may push back, fixable in 1-2 rounds |
| Self-update mechanism | 🟡 Medium — must strip; easy fix |
| Account deletion | ✅ Low — already implemented |
| Hindi i18n / DPDP fit for Indian users | ✅ Low — already partially shipped |

**Bottom line:** Tech is ready. **Policy fit is the wall.** If you want this on Play, plan for either (a) a 2-4 week refactor to qualify under default-dialer / caller-ID rules, or (b) a real possibility of permanent rejection followed by going back to sideloading.

---

## 6. Alternatives to consider

### 6.1 F-Droid / Aurora Store
Privacy-respecting Android stores that **don't** restrict call-log reading. Listed CallVault would reach a niche but real audience without permission-policy headaches. Effort: low.

### 6.2 Samsung Galaxy Store
Permissive on call-log apps. Not as large as Play but real distribution. Effort: medium.

### 6.3 Direct distribution + WhatsApp link
What you're doing today. Self-update via your hosted manifest is already implemented. Indian SMB audience already shares APKs over WhatsApp; this works.

### 6.4 Play but as a "limited" version
Strip auto-call-log capture entirely; make CallVault a manual call-logging + lead-management tool. Still useful, no policy issues. Distinct from your current product — call it **CallVault Lite** and keep the full version sideload-only.

---

## 7. Summary — do this if you want to ship to Play

1. **Decide today**: Path B (default dialer) / C (caller-ID) / D (no auto-capture) / E (skip Play).
2. If A: try a Permissions Declaration as-is. Cheap experiment. Probably rejected.
3. If B/C: 2-4 weeks of focused work. New `InCallService` / `CallScreeningService`, role-handover UX, redesigned onboarding.
4. If D: 1 week of code, but kills the headline feature. Consider rebranding the Play version.
5. **In parallel** (regardless of path): host the privacy policy, fill the Data Safety form, generate listing assets, add the `play` flavor that strips self-update.
6. Submit to **internal testing first**, never production-first.

The app is technically Play-ready. The policy fit is the gate, and it's a real wall.
