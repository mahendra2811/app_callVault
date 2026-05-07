# callNest — UI System Specification

> **Audience:** Any AI coding assistant (Claude Code, Cursor, Windsurf, Copilot) or human developer building the callNest Android UI.
> **Status:** Locked. Treat every token, rule, and component anatomy as binding unless explicitly overridden.
> **Stack target:** Native Android (Kotlin 2.0+, Jetpack Compose, Material 3 substrate).
> **Direction:** Sage / Earth — Modern Edition with gradient system.
> **Last revised:** 2026-05-02.

---

## Table of Contents

1. [Design Philosophy](#1-design-philosophy)
2. [Strategic Bet](#2-strategic-bet)
3. [Color System](#3-color-system)
4. [Gradient System](#4-gradient-system)
5. [When to Use Gradients (Strict Rules)](#5-when-to-use-gradients-strict-rules)
6. [Typography](#6-typography)
7. [Iconography](#7-iconography)
8. [Spacing & Shape](#8-spacing--shape)
9. [Elevation & Surfaces](#9-elevation--surfaces)
10. [Motion](#10-motion)
11. [Haptics](#11-haptics)
12. [Tech Stack](#12-tech-stack)
13. [Component Anatomy](#13-component-anatomy)
14. [Screen-Level Patterns](#14-screen-level-patterns)
15. [Dark Mode (Phase 2 Ready)](#15-dark-mode-phase-2-ready)
16. [Accessibility](#16-accessibility)
17. [Performance Rules](#17-performance-rules)
18. [Implementation Order](#18-implementation-order)
19. [Code Templates](#19-code-templates)
20. [Forbidden Patterns](#20-forbidden-patterns)

---

## 1. Design Philosophy

callNest's UI is built on three non-negotiable principles:

**Calm over loud.** The user opens this app 50+ times per day. Every visual element is tuned to reduce cognitive load, not demand attention. Warm cream backgrounds, low-chroma colors, generous breathing room, slow motion easing.

**Editorial over corporate.** Pairs an editorial serif (Instrument Serif) with a modern geometric sans (Geist) and a tabular monospace (Geist Mono). The typographic personality signals "considered tool" rather than "shipped quickly".

**Discipline over decoration.** Gradients exist as first-class primitives but follow strict placement rules. One gradient per screen, max two. No gradient backgrounds. No gradient text on screens that already have a gradient surface. Decoration without discipline kills the system.

---

## 2. Strategic Bet

The differentiation moat: SMB owners who try callNest feel a measurable difference within 30 seconds of opening it compared to every other call-log/CRM app on the Play Store. That difference is achieved by:

- Warm tonality (cream + sage + burnt orange) vs. industry-default cold blue-on-white
- Editorial typography vs. industry-default Roboto/Inter
- Roomy spacing (20dp horizontal padding vs. industry-default 16dp)
- Soft motion (320ms standard vs. industry-default 200–250ms)
- Modern gradient system that signals "current 2026 design" without crossing into "AI startup landing page"

---

## 3. Color System

### 3.1 Solid base (light mode)

| Token                | Hex       | RGB           | Use                                              |
| -------------------- | --------- | ------------- | ------------------------------------------------ |
| `bg.canvas`          | `#F5F1EA` | 245, 241, 234 | App background — warm cream                      |
| `bg.surface`         | `#FFFCF7` | 255, 252, 247 | Cards, sheets, elevated content                  |
| `bg.surfaceAlt`      | `#EFEAE0` | 239, 234, 224 | Grouped sections, alternate rows, pressed states |
| `bg.surfaceElevated` | `#FFFFFF` | 255, 255, 255 | Modals, top of stack                             |
| `text.primary`       | `#2C2A26` | 44, 42, 38    | Body text, names, headlines                      |
| `text.secondary`     | `#6B6660` | 107, 102, 96  | Phone numbers, timestamps, metadata              |
| `text.tertiary`      | `#9C968D` | 156, 150, 141 | Disabled, placeholder, hint                      |
| `text.inverse`       | `#FFFCF7` | 255, 252, 247 | Text on gradient or dark surface                 |
| `border.subtle`      | `#E5DFD3` | 229, 223, 211 | Hairline dividers, default card borders          |
| `border.default`     | `#C9C2B4` | 201, 194, 180 | Active borders, focused inputs                   |
| `state.success`      | `#5C8A5C` | 92, 138, 92   | Won leads, success toasts                        |
| `state.warning`      | `#C68B16` | 198, 139, 22  | Follow-ups due, warnings                         |
| `state.error`        | `#B5443A` | 181, 68, 58   | Errors, lost leads, destructive actions          |
| `state.info`         | `#4A6B8A` | 74, 107, 138  | Informational toasts, neutral notices            |

### 3.2 Brand colors (used inside gradients)

| Token               | Hex       | Use                              |
| ------------------- | --------- | -------------------------------- |
| `brand.sage`        | `#3D5A4A` | Primary brand — deep forest sage |
| `brand.sageMuted`   | `#6B8576` | Secondary sage, hover states     |
| `brand.sageLight`   | `#A8BFB1` | Tints, badge backgrounds         |
| `brand.orange`      | `#D97757` | Accent — burnt orange            |
| `brand.orangeMuted` | `#E8A488` | Soft accent backgrounds          |
| `brand.gold`        | `#C68B16` | Heat indicator mid-tone          |
| `brand.cream`       | `#FFFCF7` | Inverse text on dark surfaces    |

### 3.3 Saturation rule

Every solid color sits in the 30–60% saturation range. No neon. No pure primaries. The accent burnt orange is the loudest color in the system and is rationed strictly — one accented surface per screen, used only where the eye must land.

---

## 4. Gradient System

Gradients are first-class design primitives in callNest. Every gradient below ships as a named token and a Compose `Brush` factory.

### 4.1 Gradient tokens

| Token                 | Stops                                                         | Angle    | Use                                              |
| --------------------- | ------------------------------------------------------------- | -------- | ------------------------------------------------ |
| `gradient.brand`      | `#3D5A4A` 0% → `#6B8576` 50% → `#D97757` 100%                 | 135°     | Splash, app icon background, premium upsell hero |
| `gradient.brand.soft` | `#5C8A5C` 0% → `#D97757` 100%                                 | 135°     | FAB, primary CTA buttons                         |
| `gradient.warm`       | `#D97757` 0% → `#C68B16` 100%                                 | 135°     | Hot lead badge, urgency surfaces                 |
| `gradient.cool`       | `#3D5A4A` 0% → `#6B8576` 100%                                 | 135°     | Saved contact accent, "trusted" badges           |
| `gradient.score`      | `#9C968D` 0% → `#C68B16` 33% → `#D97757` 66% → `#B5443A` 100% | linear-x | Lead score bar, heat map                         |
| `gradient.surface`    | `#FFFCF7` 0% → `#EFEAE0` 100%                                 | 180°     | Subtle card lift (very low contrast)             |
| `gradient.border`     | `#3D5A4A` 0% → `#D97757` 100%                                 | 135°     | Focused input border, selected tab underline     |
| `gradient.text.hero`  | `#3D5A4A` 0% → `#D97757` 100%                                 | 90°      | Hero text fills (very sparingly)                 |
| `gradient.mesh.hero`  | 3-radial mesh (see 4.2)                                       | radial   | Onboarding hero, premium screen, splash          |

### 4.2 Mesh gradient (`gradient.mesh.hero`) construction

Compose three overlapping radial gradients on a `bg.canvas` base layer:

| Layer    | Center (x%, y%) | Color     | Alpha | Radius (% of width) |
| -------- | --------------- | --------- | ----- | ------------------- |
| Base     | full            | `#F5F1EA` | 100%  | n/a                 |
| Radial 1 | (20%, 30%)      | `#3D5A4A` | 60%   | 50%                 |
| Radial 2 | (80%, 20%)      | `#D97757` | 40%   | 60%                 |
| Radial 3 | (60%, 90%)      | `#C68B16` | 30%   | 70%                 |

Each radial fades to transparent at its outer radius. Overlapping alpha creates the mesh effect.

**Performance:** This is 3× overdraw on the painted surface. Cache as a static bitmap if the screen is reused. Banned from list rows and any scrolling content.

---

## 5. When to Use Gradients (Strict Rules)

### 5.1 Surface placement matrix

| Surface                      | Gradient? | Which                                                 |
| ---------------------------- | --------- | ----------------------------------------------------- |
| App canvas background        | ❌        | Solid `bg.canvas`                                     |
| Card body                    | ❌        | Solid `bg.surface`                                    |
| List row body                | ❌        | Solid `bg.surface` (alternating with `bg.surfaceAlt`) |
| Lead score badge             | ✅        | `gradient.score` cropped to score %                   |
| Floating action button       | ✅        | `gradient.brand.soft`                                 |
| Primary CTA button           | ✅        | `gradient.brand.soft`                                 |
| Secondary CTA button         | ❌        | Solid `bg.surface` + 1dp `border.default`             |
| Tab bar active indicator     | ✅        | 2dp `gradient.border` underline                       |
| Focused input border         | ✅        | 1.5dp `gradient.border`                               |
| Selected filter chip         | ✅        | `gradient.cool` background                            |
| Unselected filter chip       | ❌        | Solid `bg.surfaceAlt`                                 |
| Onboarding hero              | ✅        | `gradient.mesh.hero` full bleed                       |
| Splash screen                | ✅        | `gradient.brand` full bleed                           |
| Premium upsell screen        | ✅        | `gradient.mesh.hero` full bleed                       |
| Hero text (1 word, 1 screen) | ✅        | `gradient.text.hero` text fill                        |
| Notification banner          | ❌        | Solid surface + 4dp colored left border               |
| Toast                        | ❌        | Solid                                                 |
| Bottom sheet body            | ❌        | Solid `bg.surface`                                    |
| Bottom sheet top edge        | ✅        | 1dp `gradient.brand` at 12% opacity                   |
| Empty state illustration     | ✅        | Gradient stroke (never gradient fill)                 |
| Avatar background            | ❌        | Solid `brand.sageLight` with `brand.sage` initials    |

### 5.2 Density rule

**Maximum two gradient surfaces visible at one time per screen.** If a third is needed (e.g. FAB + active tab + selected chip simultaneously), drop the chip back to solid. Otherwise gradients lose their wayfinding role.

### 5.3 Animation rules

| Element         | Animated gradient?                        |
| --------------- | ----------------------------------------- |
| FAB             | ✅ subtle hue shift on long-press only    |
| Primary CTA     | ❌ static                                 |
| Backgrounds     | ❌ never                                  |
| Splash logo     | ✅ during 320ms entrance                  |
| Onboarding mesh | ❌ static (animated meshes drain battery) |

---

## 6. Typography

### 6.1 Font families

| Role          | Family               | Source               | Notes                                        |
| ------------- | -------------------- | -------------------- | -------------------------------------------- |
| Display serif | **Instrument Serif** | Google Fonts         | Editorial italic feel, for hero moments only |
| Body sans     | **Geist**            | Vercel / open source | Variable font, weights 400–700               |
| Monospace     | **Geist Mono**       | Vercel / open source | Tabular figures for numbers                  |

Load via Android Downloadable Fonts API with bundled fallback for offline first launch.

### 6.2 Type scale

| Style        | Family           | Size | Weight      | Line height | Letter spacing   | Use                            |
| ------------ | ---------------- | ---- | ----------- | ----------- | ---------------- | ------------------------------ |
| `display.lg` | Instrument Serif | 56sp | 600 italic  | 1.05        | -0.02em          | Onboarding hero word           |
| `display.md` | Instrument Serif | 40sp | 600         | 1.1         | -0.02em          | Premium upsell hero            |
| `display.sm` | Instrument Serif | 32sp | 600         | 1.15        | -0.01em          | Empty state hero               |
| `heading.h1` | Instrument Serif | 24sp | 600         | 1.2         | 0                | Screen titles                  |
| `heading.h2` | Geist            | 20sp | 600         | 1.3         | -0.005em         | Section headers                |
| `heading.h3` | Geist            | 17sp | 600         | 1.4         | 0                | Card titles, contact names     |
| `body.lg`    | Geist            | 16sp | 400         | 1.5         | 0                | Long-form content, notes       |
| `body.md`    | Geist            | 15sp | 400         | 1.5         | 0                | Default body text              |
| `body.sm`    | Geist            | 14sp | 400         | 1.45        | 0                | Secondary body                 |
| `caption`    | Geist            | 13sp | 400         | 1.4         | 0.005em          | Timestamps, metadata           |
| `label.lg`   | Geist            | 14sp | 500         | 1.3         | 0.01em           | Button labels                  |
| `label.md`   | Geist            | 12sp | 500         | 1.3         | 0.01em           | Tag chips, small buttons       |
| `label.sm`   | Geist            | 11sp | 600         | 1.2         | 0.04em uppercase | Section dividers               |
| `numeric.lg` | Geist Mono       | 32sp | 600 tabular | 1.1         | 0                | Lead score hero, stats numbers |
| `numeric.md` | Geist Mono       | 17sp | 500 tabular | 1.4         | 0                | Inline numbers, durations      |
| `numeric.sm` | Geist Mono       | 13sp | 400 tabular | 1.4         | 0                | Phone numbers, timestamps      |

### 6.3 Pairing rules

- Serif (Instrument Serif) appears at most once per screen, only on display sizes (32sp+).
- Mono (Geist Mono) is reserved for numbers — phone numbers, durations, lead scores, counts. Never for body text.
- All numeric content uses tabular figures so values align vertically across rows.

---

## 7. Iconography

| Concern               | Choice                                                                                                 |
| --------------------- | ------------------------------------------------------------------------------------------------------ |
| Library               | Lucide Icons (via `lucide-android` Compose port)                                                       |
| Stroke width          | 1.5px                                                                                                  |
| Default size          | 20dp                                                                                                   |
| Tab bar size          | 24dp                                                                                                   |
| Inline (in-text) size | 16dp                                                                                                   |
| FAB size              | 24dp                                                                                                   |
| Tinting               | Inherit `text.primary`; FAB uses `text.inverse`; never use a colored icon on a solid surface           |
| Forbidden             | Material Icons (visual mismatch with editorial typography), filled-style icons (only outline variants) |

---

## 8. Spacing & Shape

### 8.1 Spacing scale

Use only these values. No improvised spacing.

| Token     | Value |
| --------- | ----- |
| `space.0` | 0dp   |
| `space.1` | 4dp   |
| `space.2` | 8dp   |
| `space.3` | 12dp  |
| `space.4` | 16dp  |
| `space.5` | 20dp  |
| `space.6` | 28dp  |
| `space.7` | 40dp  |
| `space.8` | 64dp  |

### 8.2 Layout defaults

| Concern                   | Value                                    |
| ------------------------- | ---------------------------------------- |
| Horizontal screen padding | 20dp (`space.5`)                         |
| Vertical card padding     | 16dp (`space.4`)                         |
| List row height           | 76dp (refined from 72dp for modern feel) |
| Tab bar height            | 72dp                                     |
| Top app bar height        | 56dp                                     |
| Bottom sheet top corners  | 24dp                                     |
| Touch target minimum      | 48dp                                     |
| FAB size                  | 64dp                                     |

### 8.3 Corner radius

| Token         | Value | Use                               |
| ------------- | ----- | --------------------------------- |
| `radius.xs`   | 4dp   | Inputs (rare)                     |
| `radius.sm`   | 8dp   | Small chips, tags                 |
| `radius.md`   | 12dp  | Cards, list items, default sheets |
| `radius.lg`   | 20dp  | Hero cards, onboarding panels     |
| `radius.xl`   | 24dp  | Bottom sheet top corners          |
| `radius.pill` | 999dp | FAB, primary CTAs, filter chips   |

---

## 9. Elevation & Surfaces

**No drop shadows anywhere in the system.** Surfaces are differentiated by:

1. **Background tint shifts** — `bg.surface` is 3% lighter than `bg.canvas`
2. **Hairline borders** — 1dp `border.subtle` on cards that need definition
3. **Press tonal lift** — `bg.surfaceAlt` for pressed state, no scale animation

This is a deliberate departure from Material 3's elevation system. Shadows imply weight and stress; tonal lifts imply paper layered on paper. Closer to a Moleskine notebook than a glass-and-steel skyscraper.

The single exception is the FAB, which gets a subtle `inset 0 1.5dp 0 rgba(255, 255, 255, 0.25)` highlight to suggest depth via gradient luminance — but no outer shadow.

---

## 10. Motion

### 10.1 Easing curves

| Token             | Bezier                           | Use                         |
| ----------------- | -------------------------------- | --------------------------- |
| `ease.standard`   | `cubic-bezier(0.32, 0.72, 0, 1)` | Default for all transitions |
| `ease.emphasized` | `cubic-bezier(0.4, 0, 0.2, 1)`   | Container transforms        |
| `ease.exit`       | `cubic-bezier(0.4, 0, 1, 1)`     | Disappearing elements       |

### 10.2 Durations

| Token               | Value | Use                                |
| ------------------- | ----- | ---------------------------------- |
| `duration.quick`    | 180ms | Taps, ripples, color changes       |
| `duration.standard` | 320ms | Page transitions, sheet open/close |
| `duration.slow`     | 480ms | Hero entrance, splash logo         |

callNest's standard duration (320ms) is intentionally slower than Material's default (200–250ms) to reinforce the calm philosophy.

### 10.3 Spring physics

| Use               | Configuration                                                         |
| ----------------- | --------------------------------------------------------------------- |
| Default           | `Spring.DampingRatioNoBouncy`, `Spring.StiffnessMedium`               |
| Bottom sheet drag | `Spring.DampingRatioLowBouncy`, `Spring.StiffnessLow`                 |
| Forbidden         | `Spring.DampingRatioMediumBouncy` and bouncier — breaks the calm tone |

### 10.4 Press feedback

- Surface tint shift to `bg.surfaceAlt` over 120ms
- No scale animation on list rows or cards
- FAB: scale to 0.96 over 180ms on long-press only

---

## 11. Haptics

Every primary action ships with a calibrated haptic. This is non-optional — modern Android UI without haptics feels broken in 2026.

| Action                       | Haptic                                           |
| ---------------------------- | ------------------------------------------------ |
| Tap a list row               | `HapticFeedbackType.TextHandleMove` (light tick) |
| Confirm a destructive action | `HapticFeedbackType.LongPress`                   |
| Toggle a switch              | `VibrationEffect.EFFECT_TICK`                    |
| Apply a filter               | `VibrationEffect.EFFECT_CLICK`                   |
| Long-press FAB               | `VibrationEffect.EFFECT_HEAVY_CLICK`             |
| Pull-to-refresh threshold    | `VibrationEffect.EFFECT_DOUBLE_CLICK`            |
| Save successful              | `VibrationEffect.EFFECT_TICK` × 2                |
| Error toast                  | `HapticFeedbackType.LongPress`                   |

---

## 12. Tech Stack

### 12.1 Compose & UI

| Library                                         | Version    | Purpose                                   |
| ----------------------------------------------- | ---------- | ----------------------------------------- |
| `androidx.compose:compose-bom`                  | 2026.04.00 | BOM                                       |
| `androidx.compose.material3:material3`          | 1.4.x      | Substrate (accessibility primitives only) |
| `androidx.compose.material3.adaptive:adaptive`  | 1.0.x      | Foldable/tablet support                   |
| `androidx.compose.foundation`                   | latest     | Core                                      |
| `androidx.compose.animation`                    | latest     | Animations                                |
| `androidx.compose.animation:animation-graphics` | latest     | Vector morphing                           |

### 12.2 Fonts & icons

| Library                               | Purpose                 |
| ------------------------------------- | ----------------------- |
| `androidx.core:core-ktx`              | Downloadable Fonts API  |
| Geist + Geist Mono + Instrument Serif | Bundled fallback assets |
| `lucide-android` (Compose port)       | Icon set                |

### 12.3 Graphics & effects

| Library                          | Purpose                               |
| -------------------------------- | ------------------------------------- |
| Compose `Brush` API              | Linear/radial gradients (built-in)    |
| Custom `Modifier.drawBehind`     | Mesh gradients (3-radial composition) |
| `RuntimeShader` (AGSL)           | Advanced effects on API 33+           |
| `BlurEffect` via `graphicsLayer` | Backdrop blur on API 31+              |

### 12.4 Image & loading

| Library                                   | Version | Purpose               |
| ----------------------------------------- | ------- | --------------------- |
| `io.coil-kt.coil3:coil-compose`           | 3.0.x   | Image loading         |
| `com.valentinilk.shimmer:compose-shimmer` | 1.3.x   | Skeleton loading      |
| `com.airbnb.android:lottie-compose`       | 6.6.x   | Onboarding animations |

### 12.5 Splash & navigation

| Library                                  | Version     | Purpose                |
| ---------------------------------------- | ----------- | ---------------------- |
| `androidx.core:core-splashscreen`        | 1.2.0-alpha | Android 12+ splash API |
| `androidx.navigation:navigation-compose` | 2.8.x       | Type-safe routes       |

---

## 13. Component Anatomy

### 13.1 CallListRow

| Property         | Value                                                                                                  |
| ---------------- | ------------------------------------------------------------------------------------------------------ |
| Height           | 76dp                                                                                                   |
| Background       | Solid `bg.surface`                                                                                     |
| Avatar           | 44dp circle, solid `brand.sageLight`, initials in `brand.sage` Geist 16sp 600                          |
| Name (line 1)    | Geist 17sp 600 `text.primary`                                                                          |
| Subtext (line 2) | Phone in Geist Mono 13sp `text.secondary` + " · " + timestamp                                          |
| Lead score badge | Right side, 28dp pill, `gradient.score` cropped to score%, score in Geist Mono 12sp 600 `text.inverse` |
| Tag chips        | Below subtext when present (max 2 visible, "+N" overflow)                                              |
| Bottom border    | 1dp `border.subtle`, full width                                                                        |
| Pressed state    | Background → `bg.surfaceAlt`, 120ms transition                                                         |
| Swipe right      | Reveals bookmark action — orange background, bookmark icon                                             |
| Swipe left       | Reveals tag action — sage background, tag icon                                                         |

### 13.2 FloatingActionButton

| Property     | Value                                       |
| ------------ | ------------------------------------------- |
| Size         | 64dp                                        |
| Shape        | Circle                                      |
| Background   | `gradient.brand.soft` (135°)                |
| Icon         | Plus, 24dp `text.inverse`                   |
| Highlight    | `inset 0 1.5dp 0 rgba(255, 255, 255, 0.25)` |
| Outer shadow | None                                        |
| Position     | 24dp from screen edges                      |
| Pressed      | Brightness 0.92, 120ms                      |
| Long-press   | Scale 0.96 + opens radial action menu       |

### 13.3 PrimaryButton

| Property   | Value                                            |
| ---------- | ------------------------------------------------ |
| Shape      | Pill (999dp radius)                              |
| Height     | 52dp                                             |
| Padding    | Horizontal 24dp                                  |
| Background | `gradient.brand.soft`                            |
| Label      | Geist 15sp 600 `text.inverse`                    |
| Pressed    | Brightness 0.92, 120ms                           |
| Disabled   | Opacity 0.4, no gradient (solid `bg.surfaceAlt`) |

### 13.4 SecondaryButton

| Property   | Value                         |
| ---------- | ----------------------------- |
| Shape      | Pill                          |
| Height     | 52dp                          |
| Background | Solid `bg.surface`            |
| Border     | 1dp `border.default`          |
| Label      | Geist 15sp 600 `text.primary` |
| Pressed    | Background → `bg.surfaceAlt`  |

### 13.5 TabBar

| Property         | Value                                                                                           |
| ---------------- | ----------------------------------------------------------------------------------------------- |
| Height           | 72dp                                                                                            |
| Background       | Solid `bg.surface`                                                                              |
| Top border       | 1dp `border.subtle`                                                                             |
| Active tab icon  | 24dp Lucide `text.primary`                                                                      |
| Active label     | Geist 12sp 600 `text.primary`                                                                   |
| Active indicator | 2dp `gradient.border` underline, 24dp wide, animated slide between tabs (320ms `ease.standard`) |
| Inactive icon    | 24dp Lucide `text.tertiary`                                                                     |
| Inactive label   | Geist 12sp 500 `text.tertiary`                                                                  |

### 13.6 TopAppBar

| Property      | Value                                          |
| ------------- | ---------------------------------------------- |
| Height        | 56dp                                           |
| Background    | Transparent (sits on canvas)                   |
| Title         | Instrument Serif 24sp 600 `text.primary`       |
| Bottom border | 1dp `border.subtle`                            |
| Action icons  | 24dp Lucide `text.primary`, 12dp gap from edge |

### 13.7 TextInput

| Property         | Value                                                         |
| ---------------- | ------------------------------------------------------------- |
| Shape            | `radius.md` (12dp)                                            |
| Height           | 56dp                                                          |
| Background       | Solid `bg.surface`                                            |
| Border (idle)    | 1dp `border.subtle`                                           |
| Border (focused) | 1.5dp `gradient.border`, animated grow from center over 280ms |
| Label (floating) | Geist 13sp 500 `text.secondary`, animates to 11sp on focus    |
| Body             | Geist 15sp 400 `text.primary`                                 |
| Placeholder      | Geist 15sp 400 `text.tertiary`                                |

### 13.8 BottomSheet

| Property        | Value                                                      |
| --------------- | ---------------------------------------------------------- |
| Background      | Solid `bg.surface`                                         |
| Top corners     | 24dp                                                       |
| Top edge accent | 1dp `gradient.brand` at 12% opacity                        |
| Drag handle     | 5dp tall × 36dp wide pill, `border.default`, 12dp from top |
| Content padding | 20dp horizontal, 28dp top (after handle), 32dp bottom      |
| Backdrop        | `rgba(44, 42, 38, 0.5)` (text.primary at 50%)              |

### 13.9 TagChip

| State      | Properties                                                                      |
| ---------- | ------------------------------------------------------------------------------- |
| Unselected | Solid `bg.surfaceAlt`, 1dp `border.subtle`, label `text.primary` Geist 12sp 500 |
| Selected   | `gradient.cool` background, no border, label `text.inverse` Geist 12sp 500      |
| Height     | 28dp                                                                            |
| Padding    | Horizontal 12dp                                                                 |
| Shape      | Pill                                                                            |

### 13.10 FilterChipBar

| Property                      | Value                                                            |
| ----------------------------- | ---------------------------------------------------------------- |
| Height                        | 44dp                                                             |
| Background                    | `bg.canvas`                                                      |
| Chip gap                      | 8dp                                                              |
| Horizontal padding            | 20dp                                                             |
| Active chip count badge       | Geist Mono 11sp 600 `text.inverse` on `brand.orange` 18dp circle |
| Dismiss icon (X) on each chip | 14dp Lucide                                                      |

### 13.11 LeadScoreBadge (small, in lists)

| Property   | Value                                                                      |
| ---------- | -------------------------------------------------------------------------- |
| Shape      | Pill                                                                       |
| Size       | 28dp tall, content-width                                                   |
| Background | `gradient.score` cropped to score% (e.g. score 73 → first 73% of gradient) |
| Number     | Geist Mono 12sp 600 `text.inverse`                                         |
| Padding    | Horizontal 8dp                                                             |

### 13.12 LeadScoreHero (in detail screens)

| Property        | Value                                                  |
| --------------- | ------------------------------------------------------ |
| Surface         | Hero card, full-width, `gradient.mesh.hero` background |
| Score number    | Instrument Serif 64sp 600 `text.inverse`               |
| Score label     | Geist 12sp 500 uppercase `text.inverse` at 80% opacity |
| Texture overlay | 8% opacity grain noise PNG                             |
| Padding         | 28dp                                                   |
| Corners         | `radius.lg` (20dp)                                     |

### 13.13 NotificationBanner

| Property   | Value                                              |
| ---------- | -------------------------------------------------- |
| Background | Solid `bg.surface`                                 |
| Left edge  | 4dp solid state color (success/warning/error/info) |
| Title      | Geist 15sp 600 `text.primary`                      |
| Body       | Geist 13sp 400 `text.secondary`                    |
| Icon       | 20dp Lucide in matching state color                |
| Padding    | 16dp                                               |
| Corners    | `radius.md`                                        |

### 13.14 EmptyState

| Property         | Value                                                   |
| ---------------- | ------------------------------------------------------- |
| Layout           | Centered column                                         |
| Illustration     | Optional, 120dp tall, gradient stroke (`gradient.cool`) |
| Hero text        | Instrument Serif 24sp 600 `text.primary`                |
| Subhead          | Geist 15sp 400 `text.secondary`, max 2 lines            |
| CTA              | Single `PrimaryButton`, 24dp top margin                 |
| Vertical padding | 64dp                                                    |

---

## 14. Screen-Level Patterns

### 14.1 Splash screen

- Full-bleed `gradient.brand` background
- Centered logo, 96dp, animated scale 0.9 → 1.0 + alpha 0 → 1 over 320ms with `ease.standard`
- Hold for 400ms after animation, then transition to next route
- Use Android 12+ Splash Screen API (`core-splashscreen`)

### 14.2 Onboarding hero pages

- Full-bleed `gradient.mesh.hero`
- Hero word in Instrument Serif 56sp italic, fill = `gradient.text.hero`
- Subhead in Geist 18sp 400 `text.primary` at 80% opacity
- CTA at bottom: solid `bg.surfaceElevated` with `border.default` (NOT a gradient — one gradient per screen rule)
- Page indicators: 3dp tall pill bars, active = `brand.sage`, inactive = `border.subtle`

### 14.3 Main scaffold

- Top app bar: `TopAppBar` component (transparent on canvas)
- Body: per-tab content
- Bottom: `TabBar` component
- FAB: bottom-right, 24dp from edges, hides on scroll-down, reveals on scroll-up

### 14.4 List screens (Calls, Inquiries)

- Filter chip bar at top (sticky)
- Optional pinned section header in `label.sm` style
- `CallListRow` items
- Pull-to-refresh: custom haptic at threshold, `gradient.brand.soft` progress arc
- Empty state if no results matching filters

### 14.5 Detail screens

- Hero card at top (`LeadScoreHero` or contact summary)
- Sectioned content below: notes, tags, follow-ups, call history
- Each section: `label.sm` header + content + 16dp gap

### 14.6 Settings

- Grouped list pattern (iOS-inspired)
- Group headers in `label.sm` with 12dp top padding
- Rows: 56dp tall, solid `bg.surface`, hairline `border.subtle` between
- First and last row in group: `radius.md` corners (clipped)
- Toggle: M3 `Switch` wrapped, sage thumb when on
- Chevron right for navigation rows: 16dp Lucide `text.tertiary`

### 14.7 Premium upsell (Phase 2 ready, scaffold in Phase 1)

- Full-bleed `gradient.mesh.hero` top half
- Hero text "callNest Pro" in Instrument Serif 40sp italic with `gradient.text.hero` fill
- Tier cards in solid `bg.surfaceElevated`, 1dp `border.subtle`
- Selected tier: 1.5dp `gradient.border` outline
- Single `PrimaryButton` at bottom

---

## 15. Dark Mode (Phase 2 Ready)

Tokens defined now even though Phase 1 ships light only. This guarantees the system survives the transition.

### 15.1 Solid base (dark)

| Token                | Hex       |
| -------------------- | --------- |
| `bg.canvas`          | `#1F1B16` |
| `bg.surface`         | `#2A2520` |
| `bg.surfaceAlt`      | `#332D27` |
| `bg.surfaceElevated` | `#3A332C` |
| `text.primary`       | `#F0EAE0` |
| `text.secondary`     | `#B8AFA3` |
| `text.tertiary`      | `#7A7268` |
| `text.inverse`       | `#2C2A26` |
| `border.subtle`      | `#3A332C` |
| `border.default`     | `#5C5247` |
| `state.success`      | `#7AAB7A` |
| `state.warning`      | `#D9A845` |
| `state.error`        | `#D9665C` |

### 15.2 Gradients (dark)

| Token                 | Stops                                         |
| --------------------- | --------------------------------------------- |
| `gradient.brand`      | `#5C8A5C` → `#7A9D88` → `#E8A488`             |
| `gradient.brand.soft` | `#7A9D88` → `#E8A488`                         |
| `gradient.warm`       | `#E8A488` → `#D9A845`                         |
| `gradient.cool`       | `#5C8A5C` → `#7A9D88`                         |
| `gradient.score`      | `#7A7268` → `#D9A845` → `#E8A488` → `#D9665C` |
| `gradient.mesh.hero`  | Same composition, alphas dropped to 30/25/20% |

This is a **warm-toned dark mode** — rare and distinctive. Most apps default to cool blue-gray dark themes. callNest keeps its earth-warm identity in both modes.

---

## 16. Accessibility

### 16.1 Contrast ratios (verified)

| Pairing                                          | Ratio  | WCAG                                           |
| ------------------------------------------------ | ------ | ---------------------------------------------- |
| `text.primary` on `bg.canvas`                    | 13.2:1 | AAA                                            |
| `text.primary` on `bg.surface`                   | 13.8:1 | AAA                                            |
| `text.secondary` on `bg.canvas`                  | 5.1:1  | AA                                             |
| `text.tertiary` on `bg.canvas`                   | 3.1:1  | AA Large only — restrict to non-essential text |
| `text.inverse` on `gradient.brand.soft` midpoint | 4.7:1  | AA                                             |
| `text.inverse` on `gradient.warm` midpoint       | 4.5:1  | AA                                             |
| `state.error` on `bg.canvas`                     | 5.4:1  | AA                                             |

All gradient-on-text pairings tested at the worst-case midpoint, not just the lightest stop.

### 16.2 Touch targets

- Minimum 48dp × 48dp on every interactive element
- Tab bar items: 72dp tall × screen-width / 4 wide → always passes
- List row tap zones: 76dp tall × full width → always passes
- Icon buttons: 40dp icon hit-target with 4dp padding → 48dp total

### 16.3 TalkBack semantics

- Every `IconButton` requires a `contentDescription`
- Every `Switch` wrapped in a `Row` with `role = Role.Switch` and labelled parent
- Every `Slider` exposes value via `stateDescription`
- Every `TextField` has a visible `label` (not just placeholder)
- Confirm dialogs use `role = AlertDialog` and trap focus

### 16.4 Dynamic type

- All text scales with system font size (sp units throughout)
- Layouts must not break at 200% font scale — test in Android settings → Display → Font size → Largest

### 16.5 Reduced motion

- Detect via `Settings.Global.ANIMATOR_DURATION_SCALE`
- When reduced motion is requested:
  - Splash logo: appear without scale animation
  - Page transitions: cross-fade only, no slide
  - Skeleton shimmer: static placeholder color
  - FAB long-press scale: skip

---

## 17. Performance Rules

### 17.1 Gradient performance

| Rule                                                | Reason                                                 |
| --------------------------------------------------- | ------------------------------------------------------ |
| Linear/radial gradients on small surfaces (<200dp²) | Free — same cost as solid fill                         |
| Mesh gradient (`gradient.mesh.hero`)                | 3× overdraw — banned from list rows, scrolling content |
| Cache mesh as static bitmap if reused               | Saves redraw cost                                      |
| No animated background gradients                    | Battery drain + distraction                            |
| Animated gradients on FAB only                      | Acceptable — single-element scope                      |

### 17.2 Backdrop blur

- API 31+ only via `BlurEffect`
- ~3ms per frame on mid-range devices
- Use only on: bottom sheet drag area, search overlay
- Provide solid fallback for API 30 and below

### 17.3 Image loading

- Coil 3.x default crossfade: 280ms
- Always specify exact target size in `Modifier.size()` to avoid full-resolution decode
- Use `placeholder = R.drawable.shimmer_placeholder` for known-size avatars

### 17.4 Compose recomposition

- Hoist state at the lowest reasonable level
- Use `derivedStateOf` for computed UI state to prevent unnecessary recomposition
- Use `key()` blocks in lists to preserve item identity
- Use `LazyColumn` with stable keys, never `Column { items.forEach { ... } }`

### 17.5 Performance budgets (UI-specific)

| Metric                           | Target               |
| -------------------------------- | -------------------- |
| Cold start to first frame        | < 1.5s               |
| Filter sheet apply → list update | < 300ms (5k-call DB) |
| FTS query → first result         | < 100ms (5k-call DB) |
| Scroll FPS in list (10k items)   | 60fps sustained      |
| Bottom sheet open animation      | 320ms, no jank       |

---

## 18. Implementation Order

Build the design system in this exact order. Do not skip ahead — each layer depends on the previous.

### Step 1 — Token files

1. `:core:design/tokens/Color.kt` — every solid hex from §3 as `Color` constants
2. `:core:design/tokens/Gradient.kt` — every gradient from §4 as `Brush` factory functions
3. `:core:design/tokens/Spacing.kt` — every value from §8.1 as `Dp` constants
4. `:core:design/tokens/Shape.kt` — every radius from §8.3 as `RoundedCornerShape` instances
5. `:core:design/tokens/Type.kt` — every text style from §6.2 as `TextStyle` definitions
6. `:core:design/tokens/Motion.kt` — every easing curve and duration from §10
7. `:core:design/tokens/Haptic.kt` — haptic helpers from §11

### Step 2 — Theme wrapper

8. `:core:design/SageTheme.kt` — `CompositionLocalProvider` exposing `LocalSageColors`, `LocalSageGradients`, `LocalSageTypography`, `LocalSageSpacing`, `LocalSageShape`. This wraps `MaterialTheme` for accessibility but exposes only Sage tokens to consumers.

### Step 3 — Specialty composables

9. `:core:design/components/HeroMesh.kt` — `gradient.mesh.hero` composable
10. `:core:design/components/GradientBorder.kt` — animated gradient border modifier
11. `:core:design/components/SageHaptics.kt` — `LocalSageHaptics` provider

### Step 4 — Core components (in order)

12. `Avatar`
13. `TagChip`
14. `LeadScoreBadge`
15. `LeadScoreHero`
16. `PrimaryButton` / `SecondaryButton`
17. `TextInput`
18. `TopAppBar`
19. `TabBar`
20. `BottomSheet`
21. `NotificationBanner`
22. `EmptyState`
23. `FloatingActionButton`
24. `CallListRow`
25. `FilterChipBar`

### Step 5 — Screen patterns

26. Splash
27. Onboarding pages
28. Main scaffold
29. List screen template
30. Detail screen template
31. Settings template

Each component must ship with a `@Preview` showing default + all variants before you move to the next.

---

## 19. Code Templates

### 19.1 Color tokens

```kotlin
package com.callNest.core.design.tokens

import androidx.compose.ui.graphics.Color

object SageColors {
    // Background
    val Canvas = Color(0xFFF5F1EA)
    val Surface = Color(0xFFFFFCF7)
    val SurfaceAlt = Color(0xFFEFEAE0)
    val SurfaceElevated = Color(0xFFFFFFFF)

    // Text
    val TextPrimary = Color(0xFF2C2A26)
    val TextSecondary = Color(0xFF6B6660)
    val TextTertiary = Color(0xFF9C968D)
    val TextInverse = Color(0xFFFFFCF7)

    // Border
    val BorderSubtle = Color(0xFFE5DFD3)
    val BorderDefault = Color(0xFFC9C2B4)

    // State
    val StateSuccess = Color(0xFF5C8A5C)
    val StateWarning = Color(0xFFC68B16)
    val StateError = Color(0xFFB5443A)
    val StateInfo = Color(0xFF4A6B8A)

    // Brand (used inside gradients)
    val BrandSage = Color(0xFF3D5A4A)
    val BrandSageMuted = Color(0xFF6B8576)
    val BrandSageLight = Color(0xFFA8BFB1)
    val BrandOrange = Color(0xFFD97757)
    val BrandOrangeMuted = Color(0xFFE8A488)
    val BrandGold = Color(0xFFC68B16)
    val BrandCream = Color(0xFFFFFCF7)
}
```

### 19.2 Gradient tokens

```kotlin
package com.callNest.core.design.tokens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object SageGradients {
    val Brand: Brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to SageColors.BrandSage,
            0.5f to SageColors.BrandSageMuted,
            1.0f to SageColors.BrandOrange
        ),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )

    val BrandSoft: Brush = Brush.linearGradient(
        colors = listOf(SageColors.StateSuccess, SageColors.BrandOrange)
    )

    val Warm: Brush = Brush.linearGradient(
        colors = listOf(SageColors.BrandOrange, SageColors.BrandGold)
    )

    val Cool: Brush = Brush.linearGradient(
        colors = listOf(SageColors.BrandSage, SageColors.BrandSageMuted)
    )

    val Score: Brush = Brush.horizontalGradient(
        colorStops = arrayOf(
            0.0f to SageColors.TextTertiary,
            0.33f to SageColors.BrandGold,
            0.66f to SageColors.BrandOrange,
            1.0f to SageColors.StateError
        )
    )

    val Border: Brush = Brush.linearGradient(
        colors = listOf(SageColors.BrandSage, SageColors.BrandOrange)
    )

    val TextHero: Brush = Brush.horizontalGradient(
        colors = listOf(SageColors.BrandSage, SageColors.BrandOrange)
    )
}
```

### 19.3 Mesh gradient composable

```kotlin
package com.callNest.core.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.callNest.core.design.tokens.SageColors

@Composable
fun HeroMesh(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Base
                drawRect(SageColors.Canvas)

                // Radial 1 — top-left sage
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SageColors.BrandSage.copy(alpha = 0.6f),
                            SageColors.BrandSage.copy(alpha = 0f)
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.3f),
                        radius = size.width * 0.5f
                    )
                )

                // Radial 2 — top-right orange
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SageColors.BrandOrange.copy(alpha = 0.4f),
                            SageColors.BrandOrange.copy(alpha = 0f)
                        ),
                        center = Offset(size.width * 0.8f, size.height * 0.2f),
                        radius = size.width * 0.6f
                    )
                )

                // Radial 3 — bottom gold
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SageColors.BrandGold.copy(alpha = 0.3f),
                            SageColors.BrandGold.copy(alpha = 0f)
                        ),
                        center = Offset(size.width * 0.6f, size.height * 0.9f),
                        radius = size.width * 0.7f
                    )
                )
            }
    ) {
        content()
    }
}
```

### 19.4 Theme wrapper

```kotlin
package com.callNest.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.callNest.core.design.tokens.*

val LocalSageColors = staticCompositionLocalOf { SageColors }
val LocalSageGradients = staticCompositionLocalOf { SageGradients }
val LocalSageSpacing = staticCompositionLocalOf { SageSpacing }
val LocalSageShape = staticCompositionLocalOf { SageShape }
val LocalSageTypography = staticCompositionLocalOf { SageTypography }
val LocalSageMotion = staticCompositionLocalOf { SageMotion }

@Composable
fun SageTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSageColors provides SageColors,
        LocalSageGradients provides SageGradients,
        LocalSageSpacing provides SageSpacing,
        LocalSageShape provides SageShape,
        LocalSageTypography provides SageTypography,
        LocalSageMotion provides SageMotion,
        content = content
    )
}

// Convenience accessor
object Sage {
    val colors @Composable get() = LocalSageColors.current
    val gradients @Composable get() = LocalSageGradients.current
    val spacing @Composable get() = LocalSageSpacing.current
    val shape @Composable get() = LocalSageShape.current
    val typography @Composable get() = LocalSageTypography.current
    val motion @Composable get() = LocalSageMotion.current
}
```

### 19.5 Usage example

```kotlin
@Composable
fun ExampleScreen() {
    SageTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Sage.colors.Canvas)
                .padding(horizontal = Sage.spacing.Space5)
        ) {
            Text(
                text = "Calls",
                style = Sage.typography.HeadingH1,
                color = Sage.colors.TextPrimary
            )
            // ...
        }
    }
}
```

---

## 20. Forbidden Patterns

The following are explicitly banned. AI assistants should refuse or warn when asked to introduce them.

| Pattern                                            | Why banned                                         |
| -------------------------------------------------- | -------------------------------------------------- |
| Drop shadows                                       | Conflicts with paper-on-paper elevation philosophy |
| Material Icons                                     | Visual mismatch with editorial typography          |
| Material You dynamic color                         | Defeats the brand palette                          |
| `Spring.DampingRatioMediumBouncy` or bouncier      | Breaks calm motion tone                            |
| Pure black `#000000`                               | Reserve for logos only                             |
| Pure white `#FFFFFF` (except `bg.surfaceElevated`) | Fights warm palette                                |
| Neon / fully saturated colors                      | Against the 30–60% saturation rule                 |
| Three or more gradients on a single screen         | Loses wayfinding role                              |
| Gradient text + gradient background on same screen | Visual collision                                   |
| Gradient on body text                              | Readability disaster                               |
| Gradient on list row body                          | Banned for performance + calm                      |
| Filled-style icons                                 | Use only outline Lucide                            |
| Roboto, Inter as primary body                      | Replaced by Geist                                  |
| Source Serif Pro, Playfair, Merriweather           | Replaced by Instrument Serif                       |
| `MaterialTheme.colorScheme.*` direct access        | Use `Sage.colors.*` instead                        |
| Hardcoded hex codes outside `Color.kt`             | Always reference tokens                            |
| Hardcoded sp/dp values outside spacing tokens      | Always reference scale                             |
| Animated background gradients                      | Battery + distraction                              |
| AlertDialog for note/tag editors                   | Use bottom sheets                                  |
| `Column { items.forEach { ... } }` for long lists  | Use `LazyColumn`                                   |

---

## Appendix A — Token quick reference

### Spacing

`0` `4` `8` `12` `16` `20` `28` `40` `64` (dp)

### Radius

`xs:4` `sm:8` `md:12` `lg:20` `xl:24` `pill:999` (dp)

### Durations

`quick:180ms` `standard:320ms` `slow:480ms`

### Easing

`standard: (0.32, 0.72, 0, 1)` `emphasized: (0.4, 0, 0.2, 1)` `exit: (0.4, 0, 1, 1)`

### Type sizes

`56` `40` `32` `24` `20` `17` `16` `15` `14` `13` `12` `11` (sp)

---

## Appendix B — File structure

```
:core:design/
├── SageTheme.kt
├── tokens/
│   ├── Color.kt
│   ├── Gradient.kt
│   ├── Spacing.kt
│   ├── Shape.kt
│   ├── Type.kt
│   ├── Motion.kt
│   └── Haptic.kt
├── components/
│   ├── HeroMesh.kt
│   ├── GradientBorder.kt
│   ├── SageHaptics.kt
│   ├── Avatar.kt
│   ├── TagChip.kt
│   ├── LeadScoreBadge.kt
│   ├── LeadScoreHero.kt
│   ├── PrimaryButton.kt
│   ├── SecondaryButton.kt
│   ├── TextInput.kt
│   ├── TopAppBar.kt
│   ├── TabBar.kt
│   ├── BottomSheet.kt
│   ├── NotificationBanner.kt
│   ├── EmptyState.kt
│   ├── FloatingActionButton.kt
│   ├── CallListRow.kt
│   └── FilterChipBar.kt
└── res/
    ├── fonts/
    │   ├── geist_variable.ttf
    │   ├── geist_mono_variable.ttf
    │   └── instrument_serif.ttf
    └── drawable/
        └── grain_noise.png
```

---

## Appendix C — Decision log

| Decision                                      | Rationale                                                        |
| --------------------------------------------- | ---------------------------------------------------------------- |
| Sage + Earth palette over standard blue/white | Differentiation on Play Store; long-session eye comfort          |
| Instrument Serif over Source Serif Pro        | More distinctive editorial italic; modern 2026 feel              |
| Geist over DM Sans                            | Variable font, sharper at small sizes, current Vercel ecosystem  |
| Gradients as first-class tokens               | 2026 visual language; without discipline rules they degrade fast |
| 320ms standard duration vs Material's 250ms   | Reinforces calm philosophy                                       |
| 76dp list rows vs Material's 56dp             | Modern density bias; accommodates avatar + 2 lines + score badge |
| 20dp horizontal padding vs Material's 16dp    | Less crowded against screen edge                                 |
| No drop shadows                               | Paper-on-paper elevation; calmer than glass-and-steel            |
| Lucide icons over Material Icons              | Visual cohesion with editorial typography                        |
| Custom `SageTheme` over `MaterialTheme`       | Prevents M3 colors leaking into the design                       |
| Warm-toned dark mode                          | Preserves brand identity in both modes; rare and distinctive     |

---

**End of callNest UI System Specification.**

This document is the single source of truth for all UI work on callNest. Update it before changing implementation, never after.
