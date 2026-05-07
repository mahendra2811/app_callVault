# Initial-View Redesign Proposal

Author: mahi-pm · Date: 2026-05-06 · Status: research + design proposal (no code)
Scope: Splash → Onboarding (5 pages) → Permission rationale / denied
Out of scope: Login, MainScaffold, post-first-sync flows.

> **Note on inspirations.** three.js and framer-motion are web-only and cannot run on Android. They are translated below to Compose-native equivalents (`animate*AsState`, `AnimatedVisibility`, `AnimatedContent`, `Modifier.graphicsLayer`, `LookaheadLayout`) plus Lottie (already in deps — see §0). True 3D (Filament/Sceneform) is **rejected** — too heavy for a sideloaded inquiry app.

---

## 0. Pre-flight corrections to the original brief

| Brief said                          | Reality (verified in code)                                                                                                     | Impact                                                                         |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------ |
| "Lottie not in deps — flag the add" | `lottie-compose` is already wired (`app/build.gradle.kts:215`) and `SplashScreen.kt` already loads `assets/lottie/splash.json` | No new-dep cost. Lottie is the default-path now.                               |
| "App supports dark mode" (implied)  | Light-only (`Theme.kt:12` — `lightColorScheme` only, no `isSystemInDarkTheme()` branch)                                        | One palette to design for.                                                     |
| "PermissionRationale lists 3 perms" | App requests 4 (call log, contacts, phone state, +POST_NOTIFICATIONS on API 33+)                                               | Redesign must accommodate 4.                                                   |
| "Use Neo color tokens"              | `NeoColors` is **legacy**; `SageColors` is the Phase-II target (`Color.kt:81`). Theme.kt already maps M3 to SageColors.        | Redesign uses **SageColors**, not NeoColors.                                   |
| "Onboarding has swipe paging"       | Swipe is **disabled** (`OnboardingScreen.kt:95`, `userScrollEnabled = false`); pager advances only via Continue                | Animations must trigger on `pagerState.currentPage` changes, not on user-drag. |

---

## 1. Current state — file-by-file audit

| Surface              | File                                                | Lines | What it does today                                                                                                                                            | Current motion                                                                                                    |
| -------------------- | --------------------------------------------------- | ----: | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| Splash               | `ui/screen/splash/SplashScreen.kt`                  |   105 | Plays `assets/lottie/splash.json` once over `SplashGradStart→SplashGradEnd` (sage→orange) gradient. 3000ms fallback timer. PNG `cv_logo` if Lottie fails.     | Lottie 1× iteration; no other motion.                                                                             |
| Onboarding host      | `ui/screen/onboarding/OnboardingScreen.kt`          |   194 | `HorizontalPager` (5 pages, **swipe disabled**), top progress-dot row, Hilt entry-point for PermissionManager.                                                | `pagerState.animateScrollToPage()` on `currentPage` change; dot size 8↔10dp (no animation, hard swap).            |
| Welcome              | `ui/screen/onboarding/pages/WelcomePage.kt`         |   106 | Sage→orange gradient bg, 160dp concave-disc logo, `headlineSmall` headline + bodyMedium subtext, white text, primary "Continue".                              | **Static.** No entry animation.                                                                                   |
| Features             | `ui/screen/onboarding/pages/FeaturesPage.kt`        |   138 | Title + 3 stacked `NeoCard` rows (Capture / Auto-save / Insights) each with concave 44dp icon disc + title + body.                                            | **Static.**                                                                                                       |
| Permissions          | `ui/screen/onboarding/pages/PermissionsPage.kt`     |   192 | Title + subtitle + 4 stacked rows (Call log / Contacts / Phone state / Notifications) + "Grant" primary + "Skip" tertiary.                                    | **Static.**                                                                                                       |
| OEM battery          | `ui/screen/onboarding/pages/OemBatteryPage.kt`      |   117 | OEM detected via `OemBatteryGuide.detect()`; numbered step list in a NeoCard; "Open settings" + "Done".                                                       | **Static.** Vertical scroll only.                                                                                 |
| First sync           | `ui/screen/onboarding/pages/FirstSyncPage.kt`       |   151 | Auto-fires `onStart()`; renders `NeoProgressBar` + count text; flips to `Text("Done")` then `onCompleted`; error state has Skip/Retry.                        | `NeoProgressBar` `progress` interpolation only (whatever NeoProgressBar does internally). No success celebration. |
| Permission rationale | `ui/screen/permission/PermissionRationaleScreen.kt` |   106 | `StandardPage` with 🔐 emoji, headline, body, NeoCard with bullet list of missing perms, primary "Grant" CTA. Two overloads (manual `onGrant` vs `launcher`). | **Static.**                                                                                                       |
| Permission denied    | `ui/screen/permission/PermissionDeniedScreen.kt`    |    89 | Plain `Column` (no `StandardPage`!), title + body + "Open settings" CTA. Two overloads.                                                                       | **Static.**                                                                                                       |

**Tone problem**: Welcome uses serif italic implicitly (theme `headlineSmall` is sans, but `displayMedium`/`headlineMedium` are serif italic). Welcome's hero is currently sans — wastes the editorial type system the theme was built for.

**Neo primitives reused**: `NeoButton{Primary,Secondary,Tertiary}`, `NeoCard`, `NeoSurface(elevation=ConcaveSmall|ConcaveMedium)`, `NeoProgressBar`, `NeoEmptyState`, `StandardPage` (rationale only).

---

## 2. Recommended flow — page-by-page

**Overall direction**: editorial-calm, motion-as-meaning. Sage/Orange brand gradient is the spine; serif-italic hero text is the voice; Lottie carries hero animation; `graphicsLayer`-based parallax + `AnimatedContent` carries page transitions. No new color tokens. No 3D. Reduced-motion respected.

### 2.1 Splash — "the curtain"

**Recommendation**: keep Lottie, but _replace_ `splash.json` with a **2-stage choreography**: (1) brand orb forms from sage→orange particles, (2) orb cracks open into the callNest wordmark which then anchors at the top of the next screen via shared-element transition.

Rationale (3 lines): (1) Lottie infra is already in place — zero new deps, zero new build-config. (2) A particle-form-into-mark conveys "we capture and consolidate" — the app's actual job. (3) Shared-element handoff to the Welcome screen makes the splash feel like a _first frame_, not a delay.

**Keyframes (textual)**:

| t (ms) | Frame                                                                                                                    |
| -----: | ------------------------------------------------------------------------------------------------------------------------ |
|      0 | Solid sage canvas. No content.                                                                                           |
|    600 | ~80 sage→orange particles drift toward center; faint glow forms.                                                         |
|   1200 | Particles coalesce into a 120dp orb at vertical center; gentle pulse (scale 1.00→1.04→1.00).                             |
|   1800 | Orb "cracks": top half lifts as a subtle wordmark "callNest" in `displaySmall` italic; orb dissolves; transition begins. |
|   2200 | Splash hands off to Welcome; the wordmark persists as Welcome's top anchor (shared-element).                             |

Total budget **2200ms** with Lottie composition `iterations = 1`. Existing 3000ms fallback timer stays — bumped to 2500ms (slightly above natural finish). On `composition == null`, fall back to current static `cv_logo` PNG centered.

### 2.2 Welcome — first onboarding page

**Recommendation**: keep the sage→orange gradient bg, **replace concave-disc logo with a Lottie phone-call illustration** (incoming-call ring → captured-into-vault), tighten copy, switch headline to serif italic (`displayMedium`).

Layout (top to bottom):

1. Wordmark anchor (40dp tall, from splash shared element) — small, top-left padded 24dp.
2. **Lottie hero** — 220dp square, centered. Loops at 0.6× speed once user lands.
3. Headline (serif italic, white): **"Every inquiry, captured."**
4. Subtext (sans, 85% white): **"Calls land in your call log. callNest catches them, saves them, and turns them into leads — automatically."**
5. Primary "Get started" → advances pager.
6. Tertiary "I have an account" (placeholder for future Login deep-link; safe no-op for now).

Copy is concrete and India-business-friendly. No "Welcome" cliché.

### 2.3 Value-prop pages — "Capture", "Organise", "Decide"

**Decision**: collapse current `FeaturesPage` (single page with 3 stacked cards) into **3 dedicated pages with paginated parallax**. Use the **page-indicator dot row that already exists** plus a **subtle parallax on the hero icon** driven by `pagerState.currentPageOffsetFraction`. **No** Material 3 carousel — wrong tool (carousel is for browseable items, not sequential education).

Rationale: 3 cards stacked on one screen = user reads none of them. One card per screen with motion = user reads each one.

| #   | Page     | Hero                                                              | Headline (serif italic, `headlineMedium`) | Body (sans, `bodyMedium`)                                                                                                    |
| --- | -------- | ----------------------------------------------------------------- | ----------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| 1   | Capture  | Lottie: phone ringing → log entry materializes                    | "Nothing slips through."                  | "Every incoming, outgoing, and missed call is logged the moment it ends — even when callNest isn't open."                    |
| 2   | Organise | Lottie: 3 unsaved numbers fly into a "Inquiries" folder           | "Unknown numbers, sorted."                | "First-time callers are auto-saved into a dedicated Inquiries group. Tag, score, and follow up — without touching Contacts." |
| 3   | Decide   | Lottie: bar chart growing + a 0–100 lead-score badge animating up | "Know who to call back."                  | "Lead scores rank your inquiries 0–100 in real time. Export to Excel or PDF in one tap."                                     |

Parallax detail: each page's hero translates `±24dp` horizontally based on `pagerState.currentPageOffsetFraction` (Compose-native, GPU-cheap via `graphicsLayer`).

### 2.4 Permission rationale (in-flow)

**Recommendation**: keep the existing `PermissionsPage.kt` _position_ in the pager (page 3) but redesign as **one concise screen** with **grouped reasoning**, not 4 equal rows.

Layout:

1. Headline (serif italic): **"Three things we need."** (4th — Notifications — framed as optional below).
2. Sub: **"callNest works on-device. Nothing leaves your phone."**
3. **Soft Lottie illustration** (160dp, top-right): a hand-drawn line illustration of a phone log unlocking (loops slowly, calming).
4. **Three required-permission rows** in a single NeoCard (no individual cards):
   - **Call log** — "to read calls as they end"
   - **Contacts** — "to skip numbers you've already saved"
   - **Phone state** — "to detect calls in real time"
5. **One optional row** (visually de-emphasised, separated by `SageColors.Divider`):
   - **Notifications** _(optional)_ — "for follow-up reminders"
6. Primary CTA "**Grant access**" (single chip launches all 4 — same launcher as today).
7. Tertiary "**Skip for now**" (advances pager; user will see `PermissionRationaleScreen` later if perms still missing).

Microcopy principle: lead with what we _do_, not what we _want_. The line "Nothing leaves your phone" is the trust anchor.

### 2.5 First-sync page

**Recommendation**: keep the live-progress logic (`progress` / `total` / `done` / `error` state); refresh the visual to:

1. **Animated ring counter** instead of horizontal bar. A 200dp ring drawn with Compose `Canvas`; arc sweep animates from 0° → `360° × fraction` via `animate*AsState(tween(400ms))`. Center of ring shows `progress / total` in `displaySmall` italic + label "calls imported" below.
2. **Reassurance banner** above the ring — concave NeoSurface, 100% width, 56dp tall: **"Keep using your phone — we'll finish in the background."** Solves the "user watchdogs the screen" problem.
3. **Error state** — replace generic `NeoEmptyState` with same ring (now in `SageColors.StatusError` tint) + identical Skip/Retry buttons.
4. **Done state** — celebrate with a 600ms one-shot Lottie burst (confetti or soft check-mark) then auto-advance via existing `onCompleted`.

Why ring not bar: ring stays interesting at slow throughputs; bar reads as "loader" and triggers anxiety. Ring is also legible at a glance from across a desk.

### 2.6 Permission-denied screen (post-onboarding guard)

Today: bare `Column` with title + body + "Open settings" CTA. **Looks broken** — no `StandardPage`, no header, no illustration.

**Recommendation**:

1. Wrap in `StandardPage(emoji = "🔒", title = "Settings needed")` for visual consistency with the rest of the app.
2. Add a small **Lottie loop** (illustrated lock with pulsing keyhole, 140dp). Calm, not alarming.
3. Tighten body to two short paragraphs:
   - "**You said no, and we respect that.** But callNest can't read your calls without permission."
   - "Open Settings → Permissions → Allow Call log, Contacts, and Phone state. We'll pick up where you left off."
4. Keep primary "**Open settings**" CTA. Add tertiary "**I changed my mind, ask again**" — wires to `PermissionManager.shouldShowRationale()` if true; otherwise it's a no-op disabled state (Android won't re-prompt after permanent deny — be honest about that).

Empty-state warmth wins over alarm. The user already feels guilty for clicking "Don't ask again" — don't pile on.

---

## 3. Motion-design plan

Conventions: all durations divisible by 50ms; all easings respect `Settings.Global.ANIMATOR_DURATION_SCALE` (Compose's `animate*AsState` already honours it).

| Surface           | Element               | Source                                                                                          |    Duration | Easing                                | Trigger                         | Perf note                                                                             |
| ----------------- | --------------------- | ----------------------------------------------------------------------------------------------- | ----------: | ------------------------------------- | ------------------------------- | ------------------------------------------------------------------------------------- |
| Splash            | Particle-form-to-mark | Lottie composition                                                                              |      2200ms | (baked)                               | Cold launch                     | Lottie hardware-accelerates; fine                                                     |
| Splash → Welcome  | Wordmark hand-off     | `LookaheadLayout` shared element                                                                |       350ms | `tween(EaseInOutCubic)`               | Splash `onFinished`             | Use `Modifier.animateBounds`; avoid `AnimatedContent` for this                        |
| Onboarding        | Page enter            | `AnimatedVisibility(slideInHorizontally + fadeIn)`                                              |       280ms | `spring(stiffness=400f, damping=30f)` | `pagerState.currentPage` change | Wrap page content; `graphicsLayer` for translate + alpha — never `animateContentSize` |
| Onboarding        | Progress-dot active   | `animateDpAsState` for size, `animateColorAsState` for tint                                     |       200ms | `tween(EaseOutCubic)`                 | currentPage change              | Cheap; one composable per dot                                                         |
| Welcome           | Lottie hero           | Lottie                                                                                          | loop @ 0.6× | (baked)                               | composition                     | Pause when `LifecycleEventObserver` reports `ON_PAUSE` to save battery                |
| Value-prop pages  | Hero parallax         | `Modifier.graphicsLayer { translationX = pagerState.currentPageOffsetFraction * 24.dp.toPx() }` |  continuous | n/a                                   | scroll fraction                 | `graphicsLayer` block — does NOT trigger recomposition. Critical.                     |
| Value-prop pages  | Headline reveal       | `AnimatedVisibility(fadeIn + slideInVertically(initialOffsetY = 20.dp))`                        |       320ms | `tween(EaseOutCubic)`                 | page becomes current            | Stagger: hero 0ms, headline +80ms, body +160ms                                        |
| Permissions       | Permission row enter  | Same as headline reveal, staggered 60ms each                                                    |       320ms | same                                  | page becomes current            | Staggering creates "pour-in" feel; avoid `animateContentSize`                         |
| Permissions       | Lottie illustration   | Lottie                                                                                          |   loop slow | (baked)                               | composition                     | 1 instance, ~80KB JSON                                                                |
| First sync        | Ring sweep            | `animateFloatAsState` of fraction                                                               |       400ms | `tween(EaseInOutCubic)`               | `progress`/`total` change       | Driven by Compose `Canvas`; `drawArc` only — no bitmap allocation                     |
| First sync        | Done burst            | Lottie one-shot                                                                                 |       600ms | (baked)                               | `done = true`                   | Released after play to free memory                                                    |
| Permission denied | Pulsing lock          | Lottie                                                                                          |        loop | (baked)                               | composition                     | Calm, slow loop                                                                       |

**Performance discipline (project-wide)**:

- Prefer `Modifier.graphicsLayer { … }` lambda form for translate/alpha — does **not** invalidate measure/layout, only the draw phase.
- Never animate layout dimensions on text-bearing nodes (`animateContentSize` on a long text recomposes per frame).
- Pause Lottie via `LottieComposition` when activity is backgrounded — `lottie-compose`'s `isPlaying = false` is enough.
- Reduced-motion fallback: read `Settings.Global.getFloat(resolver, "animator_duration_scale", 1f)`. If `< 0.01f`, swap Lottie heroes for static first-frame previews and disable parallax.

---

## 4. Theme alignment

**No new color tokens.** Everything below resolves through existing `SageColors` or M3 mappings in `Theme.kt`.

| Use                           | Token / value                                                                                                                  | Justification                                 |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------- |
| Splash background             | `gradientBrand()` (Sage → SageMuted → Orange, 135°, `Color.kt:116`)                                                            | Brand spine. Already exists.                  |
| Splash particles              | `SageColors.Sage` → `SageColors.Orange`                                                                                        | Two-stop gradient via Lottie color overrides. |
| Welcome bg                    | Same `SplashGradStart → SplashGradEnd` (`Color.kt:73-74`) used today                                                           | Continuity with splash.                       |
| Welcome hero text             | `SageColors.TextInverse` (white-warm)                                                                                          | Already defined; reads on gradient.           |
| Value-prop bg                 | `SageColors.Canvas` (existing `NeoColors.Base` analogue)                                                                       | Calm; matches main app.                       |
| Value-prop headline           | `SageColors.Sage` on Canvas                                                                                                    | High contrast, on-brand.                      |
| Value-prop body               | `SageColors.TextSecondary`                                                                                                     | Existing token.                               |
| Permissions illustration line | `SageColors.SageMuted` (single colour line-art)                                                                                | Consistent with "calm on-device" tone.        |
| First-sync ring               | Track: `SageColors.SurfaceAlt`. Sweep: `SageColors.Orange`. Error: `SageColors.StatusError`. Done: `SageColors.StatusSuccess`. | All exist.                                    |
| Reassurance banner            | `NeoSurface(elevation = ConcaveSmall)` (legacy primitive — keep; widely used)                                                  | No new shape.                                 |
| Permission-denied lock        | `SageColors.SageDeep` line + `SageColors.Gold` keyhole accent                                                                  | Both exist.                                   |

**Typography**:

- Headlines on Welcome / value-prop / permissions / first-sync = `MaterialTheme.typography.displayMedium` or `headlineMedium` (serif italic, `Type.kt:55-82`). Today most onboarding pages use `headlineSmall` (sans) — that wastes the editorial display family. Switch to display/headline medium for all hero text.
- Body / row reasons = `bodyMedium` (sans). Unchanged.
- Counters / progress numbers = `NumberDisplay` (`Type.kt:27`) — italic serif, large. Already defined for this purpose.

**Elevations**: only `NeoElevation.Convex`, `ConvexSmall`, `ConcaveSmall`, `ConcaveMedium` — all in current code. No new elevation.

**Shapes**: `MaterialTheme.shapes` (already used by Theme). No new shapes.

---

## 5. Asset checklist

Files Mahendra (or a contractor on Fiverr / LottieFiles) must produce **before** Phase 2 ships. Phase 1 needs none of this.

### Lottie files (target → `app/src/main/assets/lottie/`)

| Filename                      | Purpose                             | Suggested dims |                Frames | Loop | Max size |
| ----------------------------- | ----------------------------------- | -------------: | --------------------: | ---- | -------: |
| `splash_v2.json`              | Particle-form → wordmark crack      |      1080×1080 |    60fps × 2.2s ≈ 132 | once |   120 KB |
| `welcome_call_capture.json`   | Phone ringing → captured into vault |      1080×1080 | 60fps × 4s loop ≈ 240 | loop |   150 KB |
| `valueprop_capture.json`      | Call materializes into log entry    |      1080×1080 |       60fps × 3s loop | loop |   100 KB |
| `valueprop_organise.json`     | Numbers fly into Inquiries folder   |      1080×1080 |     60fps × 3.5s loop | loop |   110 KB |
| `valueprop_decide.json`       | Bar chart + score badge animating   |      1080×1080 |       60fps × 3s loop | loop |   100 KB |
| `permissions_unlock.json`     | Phone log unlocking, calm line-art  |        720×720 |       60fps × 5s loop | loop |    90 KB |
| `firstsync_done_burst.json`   | Confetti / check burst              |        720×720 |          60fps × 0.6s | once |    60 KB |
| `permission_denied_lock.json` | Slow lock pulse, line-art           |        720×720 |       60fps × 4s loop | loop |    70 KB |

**APK size delta**: ≈ 800 KB. Acceptable for a sideloaded app (no Play Store size cap). Document in `DECISIONS.md` after landing.

### Static drawables

| Filename                 | Use                                                       | Density                                |
| ------------------------ | --------------------------------------------------------- | -------------------------------------- |
| `cv_wordmark.svg`        | Splash → Welcome shared element + Welcome top-left anchor | vector                                 |
| `cv_logo.png` (existing) | Splash fallback when Lottie fails                         | already present @ mipmap-mdpi..xxxhdpi |

No new PNGs needed; vector wordmark is preferable.

### Color additions

**None.** All colors resolve through existing `SageColors`.

### Copy strings (final, paste-ready into `res/values/strings.xml`)

```xml
<!-- Splash -->
<string name="cv_splash_brand">callNest</string>

<!-- Welcome -->
<string name="onboarding_welcome_headline">Every inquiry, captured.</string>
<string name="onboarding_welcome_subtext">Calls land in your call log. callNest catches them, saves them, and turns them into leads — automatically.</string>
<string name="onboarding_welcome_cta">Get started</string>
<string name="onboarding_welcome_login_link">I have an account</string>

<!-- Value-prop 1: Capture -->
<string name="onboarding_capture_headline">Nothing slips through.</string>
<string name="onboarding_capture_body">Every incoming, outgoing, and missed call is logged the moment it ends — even when callNest isn\'t open.</string>

<!-- Value-prop 2: Organise -->
<string name="onboarding_organise_headline">Unknown numbers, sorted.</string>
<string name="onboarding_organise_body">First-time callers are auto-saved into a dedicated Inquiries group. Tag, score, and follow up — without touching Contacts.</string>

<!-- Value-prop 3: Decide -->
<string name="onboarding_decide_headline">Know who to call back.</string>
<string name="onboarding_decide_body">Lead scores rank your inquiries 0–100 in real time. Export to Excel or PDF in one tap.</string>

<!-- Permissions (refreshed) -->
<string name="onboarding_permissions_v2_title">Three things we need.</string>
<string name="onboarding_permissions_v2_subtitle">callNest works on-device. Nothing leaves your phone.</string>
<string name="onboarding_permissions_v2_call_log">Call log — to read calls as they end</string>
<string name="onboarding_permissions_v2_contacts">Contacts — to skip numbers you\'ve already saved</string>
<string name="onboarding_permissions_v2_phone_state">Phone state — to detect calls in real time</string>
<string name="onboarding_permissions_v2_notifications">Notifications (optional) — for follow-up reminders</string>
<string name="onboarding_permissions_v2_grant">Grant access</string>
<string name="onboarding_permissions_v2_skip">Skip for now</string>

<!-- First sync -->
<string name="onboarding_first_sync_v2_banner">Keep using your phone — we\'ll finish in the background.</string>
<string name="onboarding_first_sync_v2_label">calls imported</string>

<!-- Permission denied -->
<string name="permission_denied_v2_title">Settings needed</string>
<string name="permission_denied_v2_body_1">You said no, and we respect that. But callNest can\'t read your calls without permission.</string>
<string name="permission_denied_v2_body_2">Open Settings → Permissions → Allow Call log, Contacts, and Phone state. We\'ll pick up where you left off.</string>
<string name="permission_denied_v2_cta">Open settings</string>
<string name="permission_denied_v2_retry">Ask me again</string>
```

---

## 6. Implementation phases

### Phase 1 — half-day (S) · zero new assets

Highest impact / lowest cost. Ship today.

1. **Welcome page typography swap** — change headline from `headlineSmall` to `displayMedium` (already defined as serif italic). Update copy to "Every inquiry, captured." + new subtext. **30 min.**
2. **Onboarding page enter animation** — wrap each page in `AnimatedVisibility(fadeIn + slideInHorizontally, spring stiffness=400 damping=30)` driven by `pagerState.currentPage`. Stagger headline/body 80ms / 160ms. **60 min.**
3. **Progress-dot animation** — replace hard size/color swap with `animateDpAsState` + `animateColorAsState`. **15 min.**
4. **First-sync reassurance banner** — add concave-NeoSurface above progress bar with new copy "Keep using your phone — we'll finish in the background." **20 min.**
5. **Permission-denied layout fix** — wrap in `StandardPage(emoji="🔒", title="Settings needed")`. Update copy. Add tertiary "Ask me again" button. **45 min.**
6. **Permissions page polish** — group required vs optional with a `SageColors.Divider`, single NeoCard wrapping the 4 rows instead of 4 separate cards. **45 min.**

> ≈ 3.5 hours. No new assets, no new strings file beyond what's already paste-ready in §5.

### Phase 2 — 1–2 days (M) · needs new Lottie files

Order Lottie files (LottieFiles or contractor) Friday → ship Mon/Tue.

1. Replace `splash.json` with `splash_v2.json` (particle-form choreography). Bump fallback timer to 2500ms. **30 min once asset lands.**
2. Add `welcome_call_capture.json` Lottie hero to Welcome (replaces concave-disc logo). **30 min.**
3. Decompose `FeaturesPage` into 3 dedicated value-prop pages (`CapturePage`, `OrganisePage`, `DecidePage`). Update `OnboardingViewModel.pageCount` and `OnboardingScreen.kt` `when` block from 5 → 7 pages. Update progress dots. **2–3 hours.**
4. Wire parallax on value-prop heroes via `pagerState.currentPageOffsetFraction` → `graphicsLayer { translationX = … }`. **45 min.**
5. Add `permissions_unlock.json` to permissions page (top-right). **30 min.**
6. Replace first-sync `NeoProgressBar` with custom `Canvas` ring. New file: `ui/components/neo/NeoProgressRing.kt` (consider promoting if reused). Add `firstsync_done_burst.json` for done state. **3–4 hours.**
7. Add `permission_denied_lock.json` to denied screen. **30 min.**

> ≈ 1.5 days. Pager grows from 5 → 7 pages. `OnboardingViewModel` changes `pageCount` only (state is page-index-agnostic except for `firstSyncProgress` which lives on the last page).

### Phase 3 — optional polish (L)

Defer until v1.1+ unless someone has time.

1. **Splash → Welcome shared-element wordmark** via `LookaheadLayout` + `Modifier.animateBounds`. Tricky because splash and welcome live in separate Nav destinations — needs a shared `SharedTransitionLayout` at the NavHost level. **half day.**
2. **First-sync done celebration** — particle confetti drawn in Compose `Canvas` (no Lottie) so colors come from `SageColors` dynamically. **half day.**
3. **Onboarding skip-ahead** — long-press progress dot to jump pages (power-user nicety). **2 hours.**
4. **Lottie color theming** — write a small helper to override Lottie key-color layers from `SageColors` so palette changes propagate without re-exporting Lottie files. **3 hours.**

---

## 7. Risks & opens

| #   | Risk / open                                                                                                                              | Severity              | Mitigation                                                                                                                                   |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------- | --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | Lottie APK delta ≈ 800 KB                                                                                                                | Low                   | Sideloaded app, no Play Store cap. Document in `DECISIONS.md`.                                                                               |
| 2   | Lottie pause-on-background not free — must hook `LifecycleEventObserver`                                                                 | Low                   | Standard pattern; lottie-compose handles via `isPlaying`. Add to a small wrapper composable `LifecycleAwareLottie`.                          |
| 3   | Reduced-motion users — Compose `animate*` honours `ANIMATOR_DURATION_SCALE`, but Lottie does NOT                                         | Medium                | Read scale in `MainActivity`; pass down via `CompositionLocal`. If `< 0.01f`, render Lottie's first frame as static via `progress = 0f`.     |
| 4   | Pager growing from 5 → 7 pages may crowd progress-dot row on small phones                                                                | Low                   | Use `Modifier.weight(1f)` distribution; cap dot size at 8dp inactive when `pageCount > 6`.                                                   |
| 5   | Shared-element splash → welcome only works if both live under one `NavHost` with `SharedTransitionLayout` (requires Compose 1.7+ stable) | Medium (Phase 3 only) | Defer to Phase 3; verify Compose BOM 2024.12.01 includes stable shared-elements. Fallback: simple `AnimatedContent` cross-fade.              |
| 6   | Onboarding can be re-entered (e.g., user kills before first-sync done) — animations should not feel slow on re-entry                     | Medium                | Detect re-entry via `OnboardingViewModel` state; skip Lottie hero auto-play for value-prop pages on re-entry, jump to first incomplete step. |
| 7   | Notifications permission optionality must reflect OS reality (API 33+ only asks; pre-33 auto-granted)                                    | Low                   | Existing `permissionsToRequest()` already handles this. UI just hides the optional row when `Build.VERSION.SDK_INT < 33`.                    |
| 8   | Lottie files from a third-party may use colors that don't match SageColors exactly                                                       | Medium                | Phase 3 task #4 — Lottie color override helper. Until then, brief the contractor with hex codes from `SageColors` directly.                  |
| 9   | Welcome's "I have an account" tertiary button has no destination yet                                                                     | Low                   | Wire as no-op + `Timber.d` for now; route to `LoginScreen` when login flow is connected (currently shipped but unwired per UI_GUIDE.md).     |
| 10  | Accessibility — Lottie animations need `contentDescription` and an opt-out for screen-reader users                                       | Medium                | Wrap each Lottie in `Modifier.semantics { contentDescription = "…"; invisibleToUser = if reduce-motion }`. Verify with TalkBack.             |

---

## 8. Diagram — flow at a glance

```
   ┌──────────┐  2.2s   ┌──────────┐  Continue   ┌──────────┐  Continue   ┌──────────┐
   │  Splash  │ ──────► │ Welcome  │ ──────────► │ Capture  │ ──────────► │ Organise │
   │ (Lottie) │  hand-  │ (Lottie  │             │ (Lottie  │             │ (Lottie  │
   └──────────┘   off   └──────────┘             └──────────┘             └──────────┘
                                                                                 │
                              ┌────────────────────────────────────┐  Continue   │
                              │ Decide → Permissions → OEM-battery │ ◄───────────┘
                              └────────────────────────────────────┘
                                              │ Continue
                                              ▼
                                    ┌────────────────────┐  done   ┌──────────────────┐
                                    │  First-sync (ring) │ ──────► │ Main app (Home)  │
                                    └────────────────────┘         └──────────────────┘
                                              │ error
                                              ▼
                                       Skip / Retry

   Permission permanently denied (post-onboarding) ──► PermissionDeniedScreen
                                                       (Settings deep-link + retry)
```

---

## 9. What this proposal does NOT include

- Login / sign-up redesign — out of scope; flagged in UI_GUIDE.md as separately broken.
- MainScaffold or tab redesigns — separate effort.
- Code. This is research + design only. After Mahendra approves direction, a coder agent (or direct execution) implements Phase 1 first.
- A11y deep-dive beyond the reduced-motion + `contentDescription` notes in §3 and §7.
- I18n — strings in §5 are English only; existing `strings.xml` localisation strategy applies unchanged.

---

End of proposal.
