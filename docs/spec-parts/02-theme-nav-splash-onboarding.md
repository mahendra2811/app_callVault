# callNest APP-SPEC — Part 02

## Theme + Navigation + Splash + Onboarding

> Audience: a UX engineer rebuilding the callNest UI from scratch.
> Self-contained — every token, route, screen, and animation needed to
> stand up the app's chrome (theme), wiring (navigation), entry point
> (splash), and first-run flow (onboarding) is captured here.
>
> Cross-references: see Part 01 for §§ 1–6 (project overview, sync
> pipeline, data model, etc.). This part covers §§ 7–15.

---

## 7 — Theme: colors, typography, spacing, shapes, motion

callNest uses a single, locked **neumorphic** design language. There
is no dark mode. There is no dynamic color (Material You is explicitly
disabled). Every screen is painted on a soft, near-white canvas with
dual-shadow elevation and tightly bounded accent colors. The look is
not a visual style choice — it is a contract. Every component matches
this contract, or it does not ship.

### 7.1 Neumorphism principles

The callNest neumorphism is governed by five rules. Internalize
them; every visual decision falls out of them.

**Rule 1 — One light source.**
There is exactly one virtual light in the world, and it sits at the
top-left at 45° above the surface. Every shadow you draw must be
consistent with that light. If you ever find yourself drawing a
shadow on the top-left of a control, you are wrong: the top-left is
the _highlight_ side.

**Rule 2 — Base canvas is `#E8E8EC`.**
The base canvas color is the floor of the design system. All
elevation, both convex and concave, is computed relative to it. The
canvas is _never_ pure white and _never_ pure gray; it is a blue-tinted
near-white that gives the dual shadows enough contrast to read.

**Rule 3 — Dual-shadow elevation.**
Every elevated surface paints **two** shadows:

- A **light** shadow on the top-left (color `#FFFFFF`, the highlight).
- A **dark** shadow on the bottom-right (color `#A3B1C6`, the lowlight).

The two shadows together create the illusion of a soft 3-D extrusion
from the canvas. A single shadow is not allowed — you will see it
immediately as "wrong" because the brain reads it as drop-shadow,
not extrusion.

**Rule 4 — 4% offset rule.**
Every tinted surface (Raised, Inset, BasePressed, the per-tab
backgrounds) is at least 4% lighter or darker than the base canvas.
Anything less and the eye cannot separate the surface from the
canvas, and the dual-shadow disappears. Anything more and the
neumorphic illusion breaks — the surface starts looking like a
flat colored card.

**Rule 5 — Convex by default, concave for input.**
Cards, buttons, FABs, and chips are **convex** (raised). Text
fields, search bars, sliders, progress tracks, and toggles' grooves
are **concave** (inset). A convex control says "I can be tapped".
A concave control says "data goes in here".

**Light source diagram (ASCII, 45° top-left).**

```
       light
       source
         \
          \
   *       \
            v
        +------+
        |      |   <- convex: highlight on TL, shadow on BR
        |      |
        +------+
           ^
           |
        shadow
        falls
        bottom-right

        +------+
        |::::::|   <- concave: shadow on TL, highlight on BR
        |::::::|
        +------+
```

**Why dual-shadow, not Material elevation?**
Material `Elevation` draws a single, downward, blurred shadow that
implies a flat card hovering above a surface. callNest's surfaces
are not hovering; they are pressed _into_ or _out of_ the canvas.
That illusion only works with dual shadows.

---

### 7.2 NeoColors palette

Every color the app may render is in this table. New colors are
added by editing `ui/theme/Color.kt`. The token name is the source
of truth — never use a raw hex literal in a composable.

#### 7.2.1 Base + surface tokens

| Token         | Hex       | Role                                                            | Where it is used                                     | WCAG vs Base                                   |
| ------------- | --------- | --------------------------------------------------------------- | ---------------------------------------------------- | ---------------------------------------------- |
| `Base`        | `#E8E8EC` | Canvas background                                               | `Surface`, root scaffolds, splash post-system-splash | n/a (this _is_ the base)                       |
| `BasePressed` | `#E0E0E5` | Pressed-state for convex buttons / tappable cards               | `NeoButton.pressed`, `NeoCard.onClick` press tint    | 1.04:1 vs Base — barely darker, intentional    |
| `Light`       | `#FFFFFF` | Top-left highlight shadow color                                 | dual-shadow `Modifier.neoShadow(...)` light pass     | 1.21:1 — used as shadow only, never as text bg |
| `Dark`        | `#A3B1C6` | Bottom-right lowlight shadow color                              | dual-shadow dark pass; outlines via `BorderSoft`     | 2.18:1                                         |
| `Raised`      | `#EDEDF2` | Subtle elevated tint for cards that need to read clearly raised | `NeoCard` body, dialog surfaces                      | 1.04:1                                         |
| `Inset`       | `#DFDFE5` | Subtle inset tint for fields and tracks                         | `NeoTextField`, `NeoSlider` track, `NeoSearchBar`    | 1.05:1                                         |

#### 7.2.2 Text tokens

| Token          | Hex       | Role                                         | Where it is used                                 | WCAG vs Base          |
| -------------- | --------- | -------------------------------------------- | ------------------------------------------------ | --------------------- |
| `OnBase`       | `#2A3441` | Primary text                                 | headlines, body, list primary lines              | 12.6:1 — AAA          |
| `OnBaseMuted`  | `#5C6A7A` | Secondary text / icons                       | subtitles, helper labels, secondary list lines   | 5.4:1 — AA            |
| `OnBaseSubtle` | `#8492A3` | Tertiary text — captions, timestamps, helper | timestamps, "N items" counters, empty-state body | 3.1:1 — AA Large only |

`OnBaseSubtle` is **never** used for body text. It is restricted to
captions and to timestamps adjacent to a primary line. Use
`OnBaseMuted` if a label is the primary affordance.

#### 7.2.3 Accent palette

Accents are used for **affordance and category**, never for
decoration. A blue chip means "this is a call". A violet chip means
"this is an inquiry". Mixing accents purely for visual variety is
forbidden.

| Token          | Hex       | Role                              | Where it is used                                    | WCAG vs Base       |
| -------------- | --------- | --------------------------------- | --------------------------------------------------- | ------------------ |
| `AccentBlue`   | `#4F7CFF` | Primary brand accent              | primary buttons, Calls category, splash ring sweep  | 4.7:1 — AA         |
| `AccentTeal`   | `#1FB5A8` | Backup / restore / cloud category | Backup tab tint, splash gradient start              | 3.1:1 — AA Large   |
| `AccentAmber`  | `#E0A82E` | Stats / charts / warning-leaning  | Stats tab, lead-score "warm" band                   | 2.6:1 — Large only |
| `AccentRose`   | `#E5536B` | Tags / destructive-leaning        | tag chips, delete confirmations, follow-up "missed" | 3.5:1 — AA Large   |
| `AccentViolet` | `#8266E5` | Inquiries category                | Inquiries tab, auto-saved inquiry chip              | 3.9:1 — AA Large   |
| `AccentGreen`  | `#34A853` | Success / Home category           | Home tab, splash gradient end, "All caught up"      | 3.4:1 — AA Large   |

Accents below WCAG AA on body text are still safe because the spec
**never uses raw accents for body text**. Accents render on chips,
icon backgrounds, and short labels (≥ 14sp, ≥ medium weight) — the
WCAG Large threshold (3.0:1) is what applies.

#### 7.2.4 Borders + toggles

| Token          | Source                        | Hex equivalent         | Role                                         |
| -------------- | ----------------------------- | ---------------------- | -------------------------------------------- |
| `BorderSoft`   | `Dark.copy(alpha=0.18)`       | rgba(163,177,198,0.18) | Card / dialog / toggle outlines              |
| `BorderAccent` | `AccentBlue.copy(alpha=0.20)` | rgba(79,124,255,0.20)  | Primary card outlines (e.g., today's totals) |
| `ToggleOn`     | —                             | `#34C759`              | iOS-green track for switched-on toggles      |
| `ToggleOff`    | —                             | `#C7C7CC`              | Cool gray for switched-off toggle tracks     |

#### 7.2.5 Category icon tints

The five top-level categories have a permanent tint. These are
**aliases** of the accent palette, not new colors — but the alias
matters because it pins meaning to color.

| Token               | Aliases        | Used by                                                  |
| ------------------- | -------------- | -------------------------------------------------------- |
| `IconCallsTint`     | `AccentBlue`   | Calls tab icon, CallDetail header, "call" in mixed lists |
| `IconInquiriesTint` | `AccentViolet` | Inquiries tab icon, auto-saved inquiry chip              |
| `IconStatsTint`     | `AccentAmber`  | Stats tab icon, chart axes labels                        |
| `IconBackupTint`    | `AccentTeal`   | Backup tab icon, restore progress                        |
| `IconTagsTint`      | `AccentRose`   | Tag chip icons, AutoTagRules screen                      |
| `IconHomeTint`      | `AccentGreen`  | Home tab icon, "today" totals                            |

#### 7.2.6 Phase I additions

| Token                      | Hex       | Role                                            |
| -------------------------- | --------- | ----------------------------------------------- |
| `TabBgHome`                | `#EAF5EE` | Home tab full-screen background tint            |
| `TabBgCalls`               | `#E7EEFB` | Calls tab full-screen background tint           |
| `TabBgInquiries`           | `#EEEAF8` | Inquiries tab full-screen background tint       |
| `TabBgMore`                | `#FAF3E5` | More tab full-screen background tint            |
| `TabBgStats`               | `#FAF3E5` | Stats screen background tint                    |
| `HeaderGradHomeStart`      | `#C4E5CF` | Home top header gradient start                  |
| `HeaderGradHomeEnd`        | `#EAF5EE` | Home top header gradient end                    |
| `HeaderGradCallsStart`     | `#BCD2F4` | Calls top header gradient start                 |
| `HeaderGradCallsEnd`       | `#E7EEFB` | Calls top header gradient end                   |
| `HeaderGradInquiriesStart` | `#D2C7F1` | Inquiries top header gradient start             |
| `HeaderGradInquiriesEnd`   | `#EEEAF8` | Inquiries top header gradient end               |
| `HeaderGradMoreStart`      | `#F0DCA4` | More top header gradient start                  |
| `HeaderGradMoreEnd`        | `#FAF3E5` | More top header gradient end                    |
| `HeaderGradStatsStart`     | `#F0DCA4` | Stats top header gradient start                 |
| `HeaderGradStatsEnd`       | `#FAF3E5` | Stats top header gradient end                   |
| `SplashGradStart`          | `#0E5C4F` | Splash + Onboarding p1 vertical gradient top    |
| `SplashGradEnd`            | `#34A853` | Splash + Onboarding p1 vertical gradient bottom |

---

### 7.3 Per-tab background tints + header gradients (Phase I.4)

Each top-level tab paints its own full-screen background tint, with
a top header that fades from a stronger version of that tint into
the tab background. The result is that the app feels "themed by
tab" without any chrome change.

| Tab          | Background tint            | Header gradient (top → bottom)                        |
| ------------ | -------------------------- | ----------------------------------------------------- |
| Home         | `TabBgHome` `#EAF5EE`      | `HeaderGradHomeStart` → `HeaderGradHomeEnd`           |
| Calls        | `TabBgCalls` `#E7EEFB`     | `HeaderGradCallsStart` → `HeaderGradCallsEnd`         |
| Inquiries    | `TabBgInquiries` `#EEEAF8` | `HeaderGradInquiriesStart` → `HeaderGradInquiriesEnd` |
| More         | `TabBgMore` `#FAF3E5`      | `HeaderGradMoreStart` → `HeaderGradMoreEnd`           |
| Stats (deep) | `TabBgStats` `#FAF3E5`     | `HeaderGradStatsStart` → `HeaderGradStatsEnd`         |

The header gradient is exactly **160dp** tall, fades top-to-bottom
into the tab background, and is rendered with
`Brush.verticalGradient` at `Offset(0, 0)` to `Offset(0, 160dp.toPx)`.

The neumorphic dual-shadow on cards still uses `Light` and `Dark` —
it does not compensate for the tinted background. This is
intentional: the tint is an _under-paint_, not a new canvas.

---

### 7.4 Splash gradient (vertical, teal → green)

```
#0E5C4F  ── 0%
   |
   |  vertical
   |  gradient
   |
#34A853  ── 100%
```

The splash gradient is the only place the app uses heavy color.
It matches the callNest logo (a teal-to-green disc), and it
appears in two locations only:

1. The Compose splash screen.
2. Onboarding page 1 (Welcome).

Both renders use `Brush.verticalGradient(0f to SplashGradStart, 1f to SplashGradEnd)` with `tileMode = TileMode.Clamp`.

The system splash (Android 12+ `windowSplashScreenBackground`) is
set to a single solid color `#0E5C4F` — the gradient _start_. This
ensures no perceptible color jump when the system splash hands off
to the Compose splash.

---

### 7.5 Typography scale

callNest uses **Inter** as the primary type family with
**system-default sans** as the fallback. Inter is bundled as an
asset variable font; if it fails to load (rare, but possible on
older devices with disk corruption), the system font handles it
without layout shift because Inter and Roboto have nearly
identical metrics.

The scale is Material 3's 15-style scale, restricted and tuned
for the neumorphic canvas. **All sizes in `sp`, all line heights
in `sp`, all letter spacing in `sp`** unless noted.

| Style            | Size | Weight         | Line height | Letter spacing | Sample                               | Where used                              |
| ---------------- | ---- | -------------- | ----------- | -------------- | ------------------------------------ | --------------------------------------- |
| `displayLarge`   | 57   | Light (300)    | 64          | -0.25          | "callNest"                           | not used in product (reserved)          |
| `displayMedium`  | 45   | Regular (400)  | 52          | 0              | "Welcome"                            | reserved                                |
| `displaySmall`   | 36   | Regular (400)  | 44          | 0              | "187" big-number                     | Stats hero numbers                      |
| `headlineLarge`  | 32   | SemiBold (600) | 40          | 0              | "Today's calls"                      | dialog hero, error pages                |
| `headlineMedium` | 28   | SemiBold (600) | 36          | 0              | "All caught up."                     | onboarding p5 done state                |
| `headlineSmall`  | 24   | SemiBold (600) | 32          | 0              | "Built for busy founders."           | onboarding p2 title, page hero          |
| `titleLarge`     | 22   | SemiBold (600) | 28          | 0              | "Inquiries"                          | page header titles, AppBar fallback     |
| `titleMedium`    | 16   | Medium (500)   | 24          | 0.15           | "Auto-save settings"                 | section headers, dialog titles          |
| `titleSmall`     | 14   | Medium (500)   | 20          | 0.1            | "Pro tip"                            | card headers, list-section labels       |
| `bodyLarge`      | 16   | Regular (400)  | 24          | 0.5            | "Never lose an inquiry call again."  | onboarding subtext, primary body        |
| `bodyMedium`     | 14   | Regular (400)  | 20          | 0.25           | "Captures every call from your log." | list body, card body, primary list line |
| `bodySmall`      | 12   | Regular (400)  | 16          | 0.4            | "Yesterday · 4:13 PM"                | timestamps, captions, helper            |
| `labelLarge`     | 14   | Medium (500)   | 20          | 0.1            | "Continue"                           | NeoButton labels, FAB labels            |
| `labelMedium`    | 12   | Medium (500)   | 16          | 0.5            | "12 NEW"                             | chip text, badge counts                 |
| `labelSmall`     | 11   | Medium (500)   | 16          | 0.5            | "BETA"                               | tiny tags, beta flags                   |

**Default text color**: `OnBase`. Composables that need a softer
look pass `OnBaseMuted` explicitly via `LocalContentColor`. The
tertiary `OnBaseSubtle` is reserved for `bodySmall` and
`labelSmall` only.

**No serifs. No display fonts. No emoji-as-text.** The single
exception to "no emoji" is the tab labels and the
`NeoPageHeader` left-side glyph — see §7.10.

---

### 7.6 Spacing constants

Spacing is a 4dp grid with named tokens. Composables never use
raw `dp` literals for padding — they always reference these.

| Token             | Value | Use                                                            |
| ----------------- | ----- | -------------------------------------------------------------- |
| `Xs`              | 4dp   | Inside-chip padding, icon-to-label gap in dense lists          |
| `Sm`              | 8dp   | Default between-element gap inside a card                      |
| `Md`              | 12dp  | Card content padding (vertical), button content padding        |
| `Lg`              | 16dp  | Card content padding (horizontal), default page horizontal pad |
| `Xl`              | 20dp  | Section-internal gap (between header and body)                 |
| `Xxl`             | 24dp  | Between-card gap, dialog content padding                       |
| `Xxxl`            | 32dp  | Between-section gap on long pages                              |
| `PageHorizontal`  | 16dp  | Outer horizontal pad for every page                            |
| `PageTopHeader`   | 24dp  | Pad below `NeoPageHeader` before first content                 |
| `SectionGap`      | 24dp  | Between two semantic sections within one page                  |
| `DialogContent`   | 20dp  | Inner pad of `NeoDialog` body                                  |
| `DialogMaxWidth`  | 360dp | Hard cap for dialog width on tablets                           |
| `BottomNavHeight` | 80dp  | Height of the neumorphic bottom nav                            |
| `TopBarHeight`    | 56dp  | Standard `TopAppBar` height (when present)                     |

`PageHorizontal` is the only horizontal pad applied at the page
root. Cards are full-width within that pad and add their own
internal pad.

`PageTopHeader` accounts for the gradient header above; it does
not include status-bar inset (status-bar is handled by the system
window-insets API).

---

### 7.7 Shape tokens

callNest uses six corner radii. Every shape is a
`RoundedCornerShape` except FAB (`CircleShape`) and the chips
(`RoundedCornerShape(50)` — fully pilled).

| Radius | Used by                                                        |
| ------ | -------------------------------------------------------------- |
| 4dp    | progress-bar tracks, micro-chips on dense lists                |
| 8dp    | text fields, search bar, small icon buttons, list item ripples |
| 12dp   | buttons (`NeoButton`), tag chips, secondary cards              |
| 16dp   | primary cards (`NeoCard`), bottom-sheet content blocks         |
| 20dp   | dialogs (`NeoDialog`)                                          |
| 24dp   | bottom-sheet **top corners only** (bottom corners 0)           |

| Shape            | Spec                                                                         | Where                   |
| ---------------- | ---------------------------------------------------------------------------- | ----------------------- |
| `NeoCardShape`   | `RoundedCornerShape(16dp)`                                                   | All cards               |
| `NeoButtonShape` | `RoundedCornerShape(12dp)`                                                   | All buttons             |
| `NeoFabShape`    | `CircleShape`                                                                | FABs                    |
| `NeoSheetShape`  | `RoundedCornerShape(topStart=24dp, topEnd=24dp, bottomStart=0, bottomEnd=0)` | Modal bottom sheets     |
| `NeoDialogShape` | `RoundedCornerShape(20dp)`                                                   | Dialogs                 |
| `NeoChipShape`   | `RoundedCornerShape(50)`                                                     | Tag/category chips      |
| `NeoFieldShape`  | `RoundedCornerShape(8dp)`                                                    | Text fields, search bar |

The 4dp track is special-cased inside `NeoProgressBar` — it does
not have its own named shape because no other component shares it.

---

### 7.8 NeoElevation tokens

`NeoElevation` is the contract for dual-shadow elevation. It maps
a logical level (Small / Medium / Large) to concrete shadow
parameters. There are seven levels:

- `ConvexSmall`, `ConvexMedium`, `ConvexLarge`
- `ConcaveSmall`, `ConcaveMedium`, `ConcaveLarge`
- `Flat`

Each level holds **two** shadow specs — light and dark — with
offset, blur, and alpha.

| Level           | Light offset | Light blur | Light alpha | Dark offset | Dark blur | Dark alpha |
| --------------- | ------------ | ---------- | ----------- | ----------- | --------- | ---------- |
| `ConvexSmall`   | (-2,-2)      | 4dp        | 1.0         | (2,2)       | 4dp       | 0.20       |
| `ConvexMedium`  | (-4,-4)      | 10dp       | 1.0         | (4,4)       | 10dp      | 0.25       |
| `ConvexLarge`   | (-8,-8)      | 20dp       | 1.0         | (8,8)       | 20dp      | 0.30       |
| `ConcaveSmall`  | (2,2)        | 4dp        | 0.20        | (-2,-2)     | 4dp       | 1.0        |
| `ConcaveMedium` | (4,4)        | 10dp       | 0.25        | (-4,-4)     | 10dp      | 1.0        |
| `ConcaveLarge`  | (8,8)        | 20dp       | 0.30        | (-8,-8)     | 20dp      | 1.0        |
| `Flat`          | (0,0)        | 0dp        | 0           | (0,0)       | 0dp       | 0          |

Notice that **concave is convex with the offsets flipped**. A
concave control is, optically, a hole pressed _into_ the canvas:
the highlight is on the bottom-right (where light bounces back up
out of the well) and the shadow is on the top-left (where the
upper rim casts down into the well).

**ASCII — convex vs concave.**

```
Convex (raised)              Concave (inset)
+--------------------+       +--------------------+
| · highlight TL     |       |   shadow TL ·      |
|  *                 |       |    *               |
|     +----------+   |       |    +----------+    |
|     | surface  |   |       |    |  inset   |    |
|     +----------+   |       |    +----------+    |
|              *     |       |               *    |
|     shadow BR ·    |       |  highlight BR ·    |
+--------------------+       +--------------------+
```

**Pairing rules.**

- A convex card is `ConvexMedium` by default. A primary card
  (e.g., today's totals on Home) is `ConvexLarge`. A small chip
  is `ConvexSmall`.
- A field/track/groove is `ConcaveSmall`. A search bar is
  `ConcaveMedium`. A pressed-into-canvas hero (e.g., the
  splash logo disc) is `ConcaveLarge`.
- `Flat` is the absence of elevation. It is the right answer for
  a divider, a non-tappable label group, or a list-item that
  belongs to a parent card (the parent provides the elevation).

---

### 7.9 Motion tokens

callNest has a small motion vocabulary. Every animation in the
app must be expressible in these primitives.

| Token                 | Primitive            | Spec                                                          | Where used                                 |
| --------------------- | -------------------- | ------------------------------------------------------------- | ------------------------------------------ |
| `PressSpring`         | `spring`             | `stiffness = 700f`, `dampingRatio = 0.8f`                     | NeoButton press scale, NeoCard tap scale   |
| `ShowHide`            | `tween`              | `durationMillis = 200`, `easing = FastOutSlowInEasing`        | snackbar, sheet, dialog enter/exit         |
| `RingSweep`           | `tween`              | `durationMillis = 1200`, `easing = FastOutSlowInEasing`       | splash ring 0° → 360°                      |
| `IndeterminateStripe` | `infiniteTransition` | `durationMillis = 1200`, `RepeatMode.Restart`, `LinearEasing` | indeterminate progress, first-sync spinner |
| `PageEnter`           | `tween`              | `durationMillis = 300`, `easing = FastOutSlowInEasing`        | NavHost composable enter                   |
| `PageExit`            | `tween`              | `durationMillis = 300`, `easing = FastOutLinearInEasing`      | NavHost composable exit                    |
| `CrossFade`           | `tween`              | `durationMillis = 220`                                        | empty-state ↔ list state crossfade         |
| `Typewriter`          | scheduled            | `50ms` per character                                          | splash wordmark                            |

**Per-screen transition table.**

| Transition                         | Duration | Easing                        | Notes                                                          |
| ---------------------------------- | -------- | ----------------------------- | -------------------------------------------------------------- |
| Splash → Main                      | 300ms    | `FastOutSlowInEasing`         | crossfade only (no slide); avoids "the app is moving" feeling  |
| Splash → Onboarding                | 300ms    | `FastOutSlowInEasing`         | crossfade                                                      |
| Splash → PermissionRationale       | 300ms    | `FastOutSlowInEasing`         | crossfade                                                      |
| Onboarding p*n → p*(n+1)           | 300ms    | slide + fade                  | slide direction follows reading order; fade handles overlap    |
| Onboarding p5 → Main               | 400ms    | `FastOutSlowInEasing`         | longer crossfade — the only transition that earns it           |
| Main tab ↔ Main tab                | 0ms      | none                          | tab switches are instant; bottom-nav indicator does the motion |
| Main tab → deep (e.g., CallDetail) | 300ms    | slide-from-right + fade       | standard push                                                  |
| deep → Main                        | 300ms    | slide-to-right + fade         | standard pop                                                   |
| any → dialog                       | 200ms    | scale 0.92 → 1.0 + fade       | `ShowHide`                                                     |
| any → bottom sheet                 | 250ms    | translate Y from below + fade | system motion                                                  |
| in-tab back                        | 300ms    | slide + fade                  | matches push                                                   |
| Home → UpdateAvailable (deep link) | 0ms      | none                          | deep link is the entry; no animation                           |

The motion system is intentionally narrow. A new screen must use
one of these — no bespoke transitions.

---

### 7.10 Iconography

**Material Symbols (rounded)** for system icons. **Custom vector
drawables** (`res/drawable/ic_*.xml`) for branded glyphs and the
six category icons. **Bitmaps** (`res/drawable/cv_logo.png`) only
for the wordmark/logo on splash + onboarding p1.

**Tinting rule.** A category icon is _always_ tinted with its
category color (the `Icon*Tint` aliases in §7.2.5). A system icon
in body text uses `OnBaseMuted`. A system icon inside a primary
button uses the button's `contentColor` (which is `Light` on a
filled blue, `OnBase` on a neutral).

**AutoMirrored variants.** Use `Icons.AutoMirrored.Rounded.*` for
any icon that has directional meaning — back arrow, forward arrow,
list-arrow, "send", "exit". Never use the non-mirrored versions
even though the app does not currently ship an RTL locale; we
want RTL to "just work" the day a translator delivers Arabic.

**Emoji policy.** Emoji are allowed in **two** places, and nowhere
else:

1. **`NeoPageHeader`** — the left glyph in the header band of
   each page. The emoji conveys category at a glance.
2. **Home Quick Actions** — the round shortcut tiles on the Home
   tab use emoji as their primary glyph, with a labeled caption
   underneath.

Emoji are **never** used in:

- Body text. Tag names. Notes. Phone-number labels.
- Buttons. (Use a vector icon if a glyph is needed.)
- Lists. Category chips. (Use the tinted `Icon*Tint` vector.)
- Snackbars, dialogs, error messages.

**Why this policy.** Emoji rendering varies across Android
manufacturer fonts. A heart on Samsung looks nothing like a heart
on Pixel. We only allow emoji where the _category_ is the message,
not the _glyph itself_.

---

### 7.11 Shadow modifier internals

Dual-shadow rendering is implemented as `Modifier.neoShadow(level: NeoElevation, shape: Shape)`. It is the only path to a callNest shadow — `Modifier.shadow(...)` from Compose is forbidden because its single-shadow output cannot be made convex+concave.

**Implementation outline.**

1. The modifier wraps a `drawBehind { ... }` block.
2. For each of the two shadow specs (light, dark), it builds a
   `Paint` with a `BlurMaskFilter(blurRadius, NORMAL)`.
3. It draws the `shape`'s outline twice — once translated by the
   light offset with the light color, once by the dark offset
   with the dark color.
4. The base canvas is painted _between_ the two shadow passes by
   the parent `Surface`/`Background`, so the shadow blurs blend
   into the canvas, not into each other.

**Why `BlurMaskFilter` and not `RenderEffect.Blur`.**
`RenderEffect.Blur` requires API 31+ and burns a hardware layer
per shadow pass. `BlurMaskFilter` works on API 26+ (our minSdk)
and is GPU-friendly because it operates on the alpha mask of the
shape outline, not on a bitmap snapshot.

**Performance notes.**

- The modifier is `remember`-keyed by `level` and `shape` — the
  `Paint` and `BlurMaskFilter` instances are cached.
- The blur radius is converted from `dp` to `px` once at recompose,
  not every frame.
- Cards in lists must **not** apply `neoShadow` per-item if the
  list scrolls fast — instead, the parent surface paints the
  shadow once and the items use `Flat`. This optimization is
  enforced by code review, not by the modifier itself.
- On API 26-27, `BlurMaskFilter` cannot be hardware-accelerated.
  We measured this at install time on a Pixel 1 and saw <1ms
  per card — acceptable.

---

## 8 — Navigation graph

callNest has a **two-level NavHost**: a root NavHost owned by
`MainActivity` whose graph is the set of top-level destinations
(Splash, Onboarding, Main, deep screens), and a nested NavHost
owned by `MainScaffold` whose graph is the four tabs.

### 8.1 Top-level NavHost

- **Owner**: `MainActivity` via `setContent { callNestApp() }`.
- **NavController**: `rememberNavController()` provided as
  `LocalRootNav` `CompositionLocal`.
- **Start destination**: `Destinations.Splash.route`.
- **Modifier**: fills the entire window; status-bar inset is
  applied by each screen, not by the NavHost.

**Route registry — every entry from `Destinations.kt`:**

| Destination           | Route                            | Args               | Source                                 |
| --------------------- | -------------------------------- | ------------------ | -------------------------------------- |
| `Main`                | `main`                           | —                  | tabbed surface                         |
| `Splash`              | `splash`                         | —                  | cold start only                        |
| `Onboarding`          | `onboarding`                     | —                  | first launch                           |
| `Calls`               | `calls`                          | —                  | (legacy direct route, in tabs now)     |
| `PermissionRationale` | `permission_rationale`           | —                  | gate when critical perm missing        |
| `PermissionDenied`    | `permission_denied`              | —                  | gate when perm permanently denied      |
| `CallDetail`          | `call_detail/{normalizedNumber}` | URI-encoded String | tap on call list                       |
| `Search`              | `search`                         | —                  | full-screen search overlay             |
| `FilterPresets`       | `filter_presets`                 | —                  | filter manager                         |
| `MyContacts`          | `my_contacts`                    | —                  | system contacts (non-inquiry)          |
| `Inquiries`           | `inquiries`                      | —                  | auto-saved inquiry bucket (also a tab) |
| `AutoSaveSettings`    | `settings/auto_save`             | —                  | nested under Settings                  |
| `AutoTagRules`        | `auto_tag_rules`                 | —                  | rules manager                          |
| `RuleEditor`          | `auto_tag_rules/edit/{ruleId}`   | Long, `-1` = new   | edit rule                              |
| `LeadScoringSettings` | `settings/lead_scoring`          | —                  | nested                                 |
| `RealTimeSettings`    | `settings/real_time`             | —                  | nested                                 |
| `Export`              | `export`                         | —                  | wizard                                 |
| `Backup`              | `backup`                         | —                  | landing                                |
| `UpdateAvailable`     | `update`                         | —                  | manifest match                         |
| `UpdateSettings`      | `settings/updates`               | —                  | nested                                 |
| `Settings`            | `settings`                       | —                  | master                                 |
| `DocsList`            | `docs`                           | —                  | FAQ list                               |
| `DocsArticle`         | `docs/{articleId}`               | URI-encoded String | one article                            |
| `Home`                | `home`                           | —                  | tab route (also routable directly)     |
| `More`                | `more`                           | —                  | tab route                              |

`Calls`, `Home`, `Inquiries`, `More` exist at the top level **only
as legacy aliases**. The canonical mount-point for those four is
inside `Main`'s nested NavHost.

### 8.2 Nested tab NavHost (inside `MainScaffold`)

- **Owner**: `MainScaffold` composable.
- **NavController**: `rememberNavController()` provided as
  `LocalMainTabNav`.
- **Start destination**: `MainTabRoute.Home.route` = `home`.
- **Composables**: one per `MainTabRoute` — Home, Calls,
  Inquiries, More.

```
MainTabRoute.Home       = "home"
MainTabRoute.Calls      = "calls"
MainTabRoute.Inquiries  = "inquiries"
MainTabRoute.More       = "more"
```

The bottom nav drives this NavHost and only this NavHost. Deep
screens (e.g., CallDetail) are pushed onto the **root** NavHost,
not the tab NavHost — they cover the bottom nav.

### 8.3 Splash gating

Cold start is the only path that lands on `Splash`. The splash
runs for ~1500ms (see §9), then calls `onFinished(decision)` where
`decision` is computed from two flags:

```kotlin
when {
    !onboardingComplete -> nav.navigate(Destinations.Onboarding.route) {
        popUpTo(Destinations.Splash.route) { inclusive = true }
    }
    !isCriticalGranted  -> nav.navigate(Destinations.PermissionRationale.route) {
        popUpTo(Destinations.Splash.route) { inclusive = true }
    }
    else                -> nav.navigate(Destinations.Main.route) {
        popUpTo(Destinations.Splash.route) { inclusive = true }
    }
}
```

`onboardingComplete` is read from `SettingsDataStore`.
`isCriticalGranted` is the conjunction of `READ_CALL_LOG`,
`READ_CONTACTS`, and `WRITE_CONTACTS` checked via
`ContextCompat.checkSelfPermission`.

### 8.4 Permission gate routing

After onboarding, the user can revoke permissions in system
Settings. On the next cold start, `Splash` will route them to
`PermissionRationale`. From `PermissionRationale`:

- Tapping "Grant" launches `RequestMultiplePermissions`.
- If the user grants → `nav.navigate(Main) { popUpTo(PermissionRationale) { inclusive = true } }`.
- If the user denies once → stays on `PermissionRationale` with
  the "Why this matters" copy expanded.
- If the user denies with "Don't ask again" (Android returns
  `shouldShowRequestPermissionRationale == false` after deny) →
  routes to `PermissionDenied`.

`PermissionDenied` shows a "Open Settings" deep link via
`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.

### 8.5 Tab-switch behavior

The bottom-nav `NavigationBarItem` uses the canonical "preserve
state" pattern:

```kotlin
tabNav.navigate(route) {
    popUpTo(tabNav.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

Effects:

- Switching from Calls (deep at scroll position 4000px) to
  Inquiries and back restores Calls at scroll position 4000px.
- Tapping the same tab again does **not** scroll-to-top — the
  spec explicitly forbids this because power users use the tab
  to "come back to where I was".
- Opening a deep screen (e.g., CallDetail) is a root NavHost
  push and does **not** affect tab-state-saving.

### 8.6 Back-stack rules

callNest does _not_ follow Android's default back-stack semantics
for tabs. Instead:

**Rule A — Hardware back from any deep screen → Home tab.**
A `popToHome()` helper composable reads both
`LocalRootNav` and `LocalMainTabNav`:

```kotlin
fun popToHome() {
    rootNav.popBackStack(Destinations.Main.route, inclusive = false)
    mainTabNav.navigate(MainTabRoute.Home.route) {
        popUpTo(mainTabNav.graph.findStartDestination().id)
        launchSingleTop = true
    }
}
```

This is wired to `BackHandler` on every deep screen.

**Rule B — Hardware back when on a tab root.**
First press: shows snackbar `"Press back again to exit"` for 2000ms.
Second press within 2000ms: `finish()`.
Tracked via a tiny state machine inside `MainScaffold`.

**Rule C — Hardware back inside tab nested-stack.**
Standard pop. The four tab NavHosts can have their own internal
stacks (e.g., AutoTagRules → RuleEditor) — those pop normally.

### 8.7 Deep links

The app responds to one deep link signal: an `Intent` extra
`route=update_available` on `MainActivity`. On first composition
of `callNestApp`, an effect inspects the intent:

```kotlin
LaunchedEffect(intent) {
    if (intent?.getStringExtra("route") == "update_available") {
        rootNav.navigate(Destinations.UpdateAvailable.route)
    }
}
```

There are no other deep links. No `intent-filter` outside the
launcher. The update-check flow itself runs in-app and does not
require a deep link, but the notification it fires uses one to
get the user back to the right screen after dismissing the
notification.

### 8.8 Route argument formats

| Destination   | Arg                | Type   | Encoding          | Example                       |
| ------------- | ------------------ | ------ | ----------------- | ----------------------------- |
| `CallDetail`  | `normalizedNumber` | String | `Uri.encode(...)` | `call_detail/%2B919876543210` |
| `RuleEditor`  | `ruleId`           | Long   | raw, `-1` for new | `auto_tag_rules/edit/-1`      |
| `DocsArticle` | `articleId`        | String | `Uri.encode(...)` | `docs/auto-save-howto`        |

Decoding:

- String args use `navArgument(name) { type = NavType.StringType }` and `Uri.decode(backStackEntry.arguments?.getString(name)!!)`.
- `ruleId` uses `navArgument(name) { type = NavType.LongType }` and `getLong(name) ?: -1L`.

### 8.9 Deep destination list

Approximate origin-tab map (the tab the user was on when they
opened a deep screen — used purely for analytics-of-the-mind, not
behaviorally; behavior is governed by §8.6 Rule A):

| Destination           | Typical origin tab       |
| --------------------- | ------------------------ |
| `CallDetail`          | Calls / Home / Inquiries |
| `Search`              | Calls / Inquiries        |
| `FilterPresets`       | Calls                    |
| `MyContacts`          | More                     |
| `Inquiries` (deep)    | Home                     |
| `AutoSaveSettings`    | More → Settings          |
| `AutoTagRules`        | More → Settings          |
| `RuleEditor`          | AutoTagRules             |
| `LeadScoringSettings` | More → Settings          |
| `RealTimeSettings`    | More → Settings          |
| `Export`              | More                     |
| `Backup`              | More                     |
| `UpdateAvailable`     | More / notification      |
| `UpdateSettings`      | More → Settings          |
| `Settings`            | More                     |
| `DocsList`            | More                     |
| `DocsArticle`         | DocsList                 |
| `PermissionRationale` | Splash gate              |
| `PermissionDenied`    | PermissionRationale      |

### 8.10 ASCII navigation graph

```
                     [ MainActivity ]
                            |
                            v
                   ( root NavController )
                            |
        +-------------------+-------------------+
        |                   |                   |
        v                   v                   v
    [ Splash ]      [ Onboarding ]      [ PermissionRationale ]
        |                   |                   |
        +---------+---------+-------------------+
                  |
                  v
              [ Main ]
                  |
                  +-- (nested NavController)
                  |        |
                  |   +----+----+----+----+
                  |   |    |    |    |    |
                  |   v    v    v    v    v
                  | Home Calls Inq.  More
                  |
                  +-- deep pushes (root) -->
                       CallDetail
                       Search
                       FilterPresets
                       MyContacts
                       Inquiries (deep)
                       AutoSaveSettings
                       AutoTagRules ─> RuleEditor
                       LeadScoringSettings
                       RealTimeSettings
                       Export
                       Backup
                       UpdateAvailable
                       UpdateSettings
                       Settings
                       DocsList ─> DocsArticle
                       PermissionDenied
```

---

## 9 — Splash screen

### 9.1 Purpose

Mask cold-start latency, brand the app, and gate routing. The
splash is the only place the user ever sees the wordmark animate
in. After this screen, the app is _itself_; the splash never
re-appears within a session.

### 9.2 Entry points

- **Cold start only.** When `MainActivity.onCreate` runs and the
  process is fresh, root NavHost starts at `Splash`.
- Process-death restarts: also cold start, also splash.
- Warm starts (Activity restored from saved state): no splash;
  user re-enters at the last destination.

### 9.3 Exit points

- `Onboarding` — when `onboardingComplete = false`.
- `PermissionRationale` — when `onboardingComplete = true` but
  `isCriticalGranted = false`.
- `Main` — happy path.

All three are popUpTo-inclusive of `Splash`: there is no way to
press back to splash.

### 9.4 Required inputs (data)

None. The splash is pure render. Routing data
(`onboardingComplete`, `isCriticalGranted`) is read once inside
`onFinished` — not as state — to avoid recompose.

### 9.5 Required inputs (user)

None. The splash auto-advances after **1500ms total**.

### 9.6 Mandatory display elements

1. **Full-bleed gradient background** — vertical
   `SplashGradStart` → `SplashGradEnd`, edge to edge, behind the
   status bar.
2. **Centered concave disc** — a 160dp circle painted with
   `Base` and `ConcaveLarge` elevation, holding the
   `cv_logo` bitmap centered (96dp logo inside the 160dp well).
3. **Wordmark "callNest"** — typewriter-animated, white,
   `headlineMedium` weight, positioned 24dp below the disc.

### 9.7 Optional display elements

None. The splash is intentionally minimal.

### 9.8 Empty state

N/A — splash is the empty state by definition.

### 9.9 Loading state

The splash _is_ the loading state. There is no spinner; the
implicit wait is 1500ms regardless of how fast the app is ready.
We choose readiness perception over actual readiness here.

### 9.10 Error state

N/A. The splash cannot error: it has no I/O. If the app crashes
before reaching it, the system splash carries until process death
and the user sees a launcher icon, not us.

### 9.11 Edge cases

- **System splash flash**: Android 12+ shows a system splash
  before our `MainActivity` is composed. We set
  `windowSplashScreenBackground = #0E5C4F` and the icon to the
  app icon, so the handoff to our gradient is invisible.
- **Low-end device degradation**: on devices reporting
  `ActivityManager.isLowRamDevice()`, the ring-sweep stage is
  skipped (the ring is drawn instantly). The wordmark still
  types in.
- **RTL handling**: the wordmark is the brand name "callNest"
  and is rendered LTR regardless of locale — it is not
  translatable.
- **Screen rotation**: the splash is `screenOrientation="portrait"`
  via the manifest activity attribute. Rotation is impossible
  during splash.
- **Accessibility services overlap (TalkBack, Switch Access)**:
  the splash is announced as "callNest loading"; accessibility
  services do not interrupt the auto-advance.

### 9.12 Copy table

| String resource         | English  | Notes                                                                |
| ----------------------- | -------- | -------------------------------------------------------------------- |
| `cv_splash_brand`       | callNest | LTR, never translated                                                |
| `cv_splash_logo_letter` | C        | legacy — used by older logo letter render path; kept for back-compat |

### 9.13 ASCII wireframe (full-screen)

```
+---------------------------------------------+
|                                             |
|                                             |
|             #0E5C4F (top of grad)           |
|                                             |
|                                             |
|                  ,---------,                |
|                 / concave   \               |
|                |   well      |              |
|                |   +-------+ |              |
|                |   | logo  | |              |
|                |   +-------+ |              |
|                 \           /               |
|                  '---------'                |
|                                             |
|                  callNest                  |
|              (white, typewriter)            |
|                                             |
|                                             |
|             #34A853 (bottom of grad)        |
|                                             |
+---------------------------------------------+
```

### 9.14 Accessibility

- `Modifier.semantics { contentDescription = "callNest loading" }` on the root.
- Disc + wordmark are merged into a single semantics node so
  TalkBack reads the brand once, not twice.
- Contrast: white wordmark on `#0E5C4F` → 12.4:1 (AAA). White
  on `#34A853` → 3.9:1 (AA Large). The wordmark sits in the
  upper third where the background is darker, so AAA holds in
  practice.
- No interactive elements. No focusable elements. TalkBack
  cannot get "stuck" on splash.
- Reduced-motion: if `Settings.Global.ANIMATOR_DURATION_SCALE`
  is 0, all three stages collapse to instant render and
  `onFinished` still fires after 1500ms (the _route_ is what's
  important, not the animation).

### 9.15 Performance

- First frame must paint within **200ms** of `Activity.onCreate`.
  This is enforced by:
  - No I/O in `onCreate` other than DataStore _reads-by-Flow_
    that compose into state.
  - The system splash background color (`#0E5C4F`) is identical
    to the Compose splash gradient start, so the "frame zero"
    looks correct even before Compose lays out.
- Total animation: 1500ms.
- No allocation per frame: the disc shadow is a remembered
  `BlurMaskFilter`, the gradient is a remembered `Brush`.
- Target: 0 jank frames on Pixel 4a (mid-tier 2020 device).

### 9.16 Animation timeline (millisecond-by-millisecond)

| Range       | Stage                     | What happens                                                                                                                                                                                                                                     |
| ----------- | ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 0–200ms     | system splash carries     | Android shows `windowSplashScreenBackground = #0E5C4F` with launcher icon. Compose has not yet laid out.                                                                                                                                         |
| 200–500ms   | Compose splash takes over | Gradient paints. Disc enters with `spring(stiffness=600, damping=0.7)` from scale `0.85` to `1.0`. Logo bitmap fades from 0 to 1 over 300ms.                                                                                                     |
| 500–800ms   | ring sweep                | A `Canvas.drawArc` ring at radius = disc radius + 6dp, color `AccentBlue`, stroke 4dp, sweeps from 0° to 360° using `RingSweep` (1200ms tween — the 800ms→1700ms portion gets clipped to the 1500ms total; we crossfade the ring out at 1400ms). |
| 800–1200ms  | wordmark types in         | "callNest" appears one character at a time at `Typewriter` cadence (50ms × 9 chars = 450ms).                                                                                                                                                     |
| 1200–1500ms | settle                    | Everything holds. At 1500ms, `onFinished(decision)` fires.                                                                                                                                                                                       |

Implementation note: the ring's full sweep is 1200ms but the
splash budget is 1500ms; the ring is rendered with a deliberate
overlap — at 1400ms it begins a 100ms fade-out so it does not
"snap" off when the splash crossfades into the next destination.

---

## 10 — Onboarding orchestrator

### 10.1 Purpose

A 5-page, swipe-disabled, advance-only first-run flow. Goal: 60
seconds end-to-end on a happy path; max 2 minutes including
permission grant time and the OEM detour.

### 10.2 Pager structure

- `HorizontalPager(state = pagerState, userScrollEnabled = false, count = 5)`.
- `pagerState = rememberPagerState { 5 }`.
- Page advance is **only** via the `Continue` button on each page.
- Pages 2–5 show a `Back` text button on the top-left.
- A 5-dot progress indicator floats in the top-center; the
  current dot is `AccentBlue` filled, others are
  `BorderSoft` outlined.

### 10.3 Orchestrator file path

`app/src/main/java/com/callNest/app/ui/screen/onboarding/OnboardingScreen.kt`.
Each page is its own file in the same package:

- `OnboardingWelcomePage.kt`
- `OnboardingFeaturesPage.kt`
- `OnboardingPermissionsPage.kt`
- `OnboardingBatteryPage.kt`
- `OnboardingFirstSyncPage.kt`

The orchestrator owns `OnboardingViewModel` (Hilt) which holds:

- `onboardingComplete: StateFlow<Boolean>` (read-only mirror of DataStore)
- `setComplete(): Unit` (writes `true` to DataStore)
- `isCriticalGranted: StateFlow<Boolean>`

### 10.4 Skip rules

None. All five pages are mandatory. The "Skip-for-now" on page 5
is a soft skip — it still completes onboarding and routes to Main.
There is no "Skip onboarding" button anywhere.

### 10.5 Page-transition animation

`AnimatedContent` between page composables (we do not use
HorizontalPager's swipe animation because swipe is disabled, and
the manual advance gives us crisper control):

```kotlin
slideInHorizontally(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
    fadeIn(tween(220)) togetherWith
    slideOutHorizontally(targetOffsetX = { -it / 4 }) +
    fadeOut(tween(180))
```

Duration: 300ms in, 220ms out. Slide direction: forward = enter
from right, exit to left; back = reverse.

### 10.6 Edge cases

- **App killed mid-onboarding**: we persist `onboardingPage`
  into DataStore on every advance. On relaunch, the orchestrator
  reads it and starts at that page (after splash).
- **Permission denied permanent on page 3**: page advances
  regardless; the system permission gate (§8.4) catches it later.
- **Low-end device skipping animations**: if
  `isLowRamDevice() == true`, `AnimatedContent` is replaced with
  a no-animation crossfade.
- **OEM page 4 deep-link fails**: fallback chain — see §14.
- **First-sync errors on page 5**: the user can Retry or
  Skip-for-now (see §15). Skip still completes onboarding.

---

## 11 — Onboarding page 1: Welcome

### 11.1 Purpose

Establish the brand promise in one screen. Show the gradient.
Earn the next tap.

### 11.2 Entry points

From orchestrator only (the first page).

### 11.3 Exit points

`Continue` → page 2.

### 11.4 Required inputs (data)

None — pure render.

### 11.5 Required inputs (user)

`Continue` tap.

### 11.6 Mandatory display elements

1. Full-bleed `SplashGradStart` → `SplashGradEnd` gradient.
2. 160dp concave disc (white-on-base) with `cv_logo` bitmap
   inside, centered horizontally, top offset 22% of screen
   height.
3. White headline "Never lose an inquiry call again."
   (`headlineMedium`, max 2 lines, center-aligned).
4. White-85% subtext one line "callNest catches every call so
   you never miss a lead." (`bodyLarge`, single line, ellipsize).
5. `NeoButton` primary variant labeled "Continue", anchored 32dp
   from the bottom safe-area inset.

### 11.7 Optional display elements

None.

### 11.8 Empty state

N/A — first page is its own state.

### 11.9 Loading state

N/A.

### 11.10 Error state

N/A.

### 11.11 Edge cases

- Very small phones (< 360dp wide): headline allowed to wrap to
  3 lines; subtext drops out entirely (`overflow = Visibility.Gone`
  via if-check).
- Tablet: max content width 480dp, centered.
- Landscape: orchestrator forces portrait via Activity flag, so
  this case does not occur.

### 11.12 Copy table

| ID                        | English                                               |
| ------------------------- | ----------------------------------------------------- |
| `cv_onb_welcome_title`    | Never lose an inquiry call again.                     |
| `cv_onb_welcome_subtitle` | callNest catches every call so you never miss a lead. |
| `cv_onb_welcome_continue` | Continue                                              |

### 11.13 ASCII wireframe

```
+----------------------------------+
| · · · · ·          (progress)    |
|                                  |
|                                  |
|              ,---,               |
|             | logo |              |
|              '---'               |
|                                  |
|   Never lose an inquiry          |
|        call again.               |
|                                  |
|  callNest catches every call    |
|  so you never miss a lead.       |
|                                  |
|                                  |
|         [   Continue   ]         |
+----------------------------------+
```

### 11.14 Accessibility

- Headline is `Modifier.semantics { heading() }`.
- Subtext: `liveRegion = LiveRegionMode.Polite`.
- Continue button: minimum 48dp tap target, content-description
  matches label.
- Color contrast: white on darkest gradient stop is 12.4:1; on
  brightest stop is 3.9:1 — headline placed in upper portion.

### 11.15 Performance

- Same `Brush` instance reused from splash via
  `LocalSplashGradient` provider — no re-allocation on first frame.

---

## 12 — Onboarding page 2: Features

### 12.1 Purpose

Three-card "what this does" pitch. Concrete, not aspirational.

### 12.2 Entry points

From page 1.

### 12.3 Exit points

`Continue` → page 3. `Back` → page 1.

### 12.4 Required inputs (data)

Static features list (in code, not DB).

### 12.5 Required inputs (user)

`Continue` or `Back`.

### 12.6 Mandatory display elements

1. White background (no gradient — gradient is reserved for
   splash + p1).
2. Title `headlineSmall` "Built for busy founders." (top, after
   `PageTopHeader`).
3. Three horizontal `NeoCard`s, each with:
   - 40dp tinted icon (rounded square, `IconCallsTint` /
     `IconInquiriesTint` / `IconStatsTint`).
   - Card title (`titleMedium`).
   - Card body (`bodyMedium`, 2 lines).
4. `Continue` button anchored bottom.
5. `Back` text button top-left.

### 12.7 Optional display elements

None.

### 12.8 Empty state

N/A.

### 12.9 Loading state

N/A.

### 12.10 Error state

N/A.

### 12.11 Edge cases

- The 3 cards stack vertically with 16dp gap; no horizontal
  carousel.
- On 4-inch phones, body of each card may truncate to 1 line —
  acceptable.

### 12.12 Copy table

| ID                                | English                                                                           |
| --------------------------------- | --------------------------------------------------------------------------------- |
| `cv_onb_features_title`           | Built for busy founders.                                                          |
| `cv_onb_features_calls_title`     | Captures every call                                                               |
| `cv_onb_features_calls_body`      | We log every inbound and outbound call from your phone log automatically.         |
| `cv_onb_features_inquiries_title` | Auto-saves inquiries                                                              |
| `cv_onb_features_inquiries_body`  | Unsaved numbers go straight into a dedicated contact group, tagged for follow-up. |
| `cv_onb_features_stats_title`     | Lead scoring + exports                                                            |
| `cv_onb_features_stats_body`      | Score every lead 0–100 and export to Excel, CSV, or PDF in one tap.               |
| `cv_onb_features_continue`        | Continue                                                                          |

### 12.13 Card data table

| Card | Icon                  | Tint                | Title id                          | Body id                          |
| ---- | --------------------- | ------------------- | --------------------------------- | -------------------------------- |
| 1    | `ic_call_log_capture` | `IconCallsTint`     | `cv_onb_features_calls_title`     | `cv_onb_features_calls_body`     |
| 2    | `ic_inquiry_inbox`    | `IconInquiriesTint` | `cv_onb_features_inquiries_title` | `cv_onb_features_inquiries_body` |
| 3    | `ic_stats_sparkline`  | `IconStatsTint`     | `cv_onb_features_stats_title`     | `cv_onb_features_stats_body`     |

### 12.14 ASCII wireframe

```
+----------------------------------+
| < Back        · · · · ·          |
|                                  |
|   Built for busy founders.       |
|                                  |
|  +----------------------------+  |
|  | [icon] Captures every call |  |
|  | We log every inbound and   |  |
|  | outbound call from your... |  |
|  +----------------------------+  |
|                                  |
|  +----------------------------+  |
|  | [icon] Auto-saves inquiries|  |
|  | Unsaved numbers go straight|  |
|  | into a dedicated contact...|  |
|  +----------------------------+  |
|                                  |
|  +----------------------------+  |
|  | [icon] Lead scoring + exp. |  |
|  | Score every lead 0–100 and |  |
|  | export to Excel, CSV, ...  |  |
|  +----------------------------+  |
|                                  |
|         [   Continue   ]         |
+----------------------------------+
```

### 12.15 Accessibility + performance

- Each card has `Modifier.semantics(mergeDescendants = true)` so
  TalkBack reads "icon, title, body" as one node.
- Cards reuse a shared `NeoElevation.ConvexMedium` paint cache.

---

## 13 — Onboarding page 3: Permissions

### 13.1 Purpose

Earn the four critical permissions in one batch. Frame each as a
_reason_, not a _capability_.

### 13.2 Entry points

From page 2.

### 13.3 Exit points

`Grant` → page 4 (regardless of result).
`Back` → page 2.

### 13.4 Required inputs (data)

Permission-state map from `PermissionsGate`.

### 13.5 Required inputs (user)

Tap `Grant`. The system dialog handles the rest.

### 13.6 Mandatory display elements

1. White background.
2. Title `headlineSmall` "Two taps and you're set.".
3. Subtitle `bodyMedium`: "Everything stays on your device.
   callNest never uploads your data."
4. List of 4 permission rows, each:
   - 32dp tinted icon
   - Permission display name (`titleSmall`)
   - Reason (`bodySmall`)
5. `Grant` `NeoButton` primary, full width, anchored bottom.
6. `Back` text button top-left.

### 13.7 Permission rows

| Icon                | Display name     | Reason                                                          |
| ------------------- | ---------------- | --------------------------------------------------------------- |
| `ic_call_log`       | Call log access  | So we can list every inbound and outbound call.                 |
| `ic_contacts`       | Contacts (read)  | So we know which calls are from saved leads.                    |
| `ic_contacts_write` | Contacts (write) | So we can auto-save unsaved inquiries to a group.               |
| `ic_phone_state`    | Phone state      | So the floating bubble + post-call popup can fire in real time. |

### 13.8 Permission state machine

```
[ idle ]
   |
   |  user taps Grant
   v
[ requesting ] -- system dialog --
   |
   +--> all granted        -> emit advance
   +--> partially granted  -> emit advance (gate will re-check on Main entry)
   +--> denied (first)     -> stay; "Why this matters" expands
   +--> denied (permanent) -> emit advance; gate will route to PermissionDenied later
```

The `RequestMultiplePermissions` launcher returns a
`Map<String, Boolean>`. We compute:

```kotlin
val anyPermanentlyDenied = permissions.any { (perm, granted) ->
    !granted && !shouldShowRequestPermissionRationale(perm)
}
```

`anyPermanentlyDenied` does **not** block advance — onboarding
must complete. The gate at `Splash` / on-Main-entry handles it.

### 13.9 Optional / empty / loading / error

All N/A — this is a request flow, not a data flow.

### 13.10 Edge cases

- User dismisses system dialog by tapping outside: counts as
  "denied (first)". State returns to idle, button re-enables.
- User on Android 12+ with one-time permission: treated as
  granted for this session; gate re-checks on next cold start.
- User on Android 13+ with `POST_NOTIFICATIONS`: not requested
  here (we ask later, when first creating a notification).

### 13.11 Copy table

| ID                                  | English                                                            |
| ----------------------------------- | ------------------------------------------------------------------ |
| `cv_onb_perm_title`                 | Two taps and you're set.                                           |
| `cv_onb_perm_subtitle`              | Everything stays on your device. callNest never uploads your data. |
| `cv_onb_perm_calllog_name`          | Call log access                                                    |
| `cv_onb_perm_calllog_reason`        | So we can list every inbound and outbound call.                    |
| `cv_onb_perm_contacts_read_name`    | Contacts (read)                                                    |
| `cv_onb_perm_contacts_read_reason`  | So we know which calls are from saved leads.                       |
| `cv_onb_perm_contacts_write_name`   | Contacts (write)                                                   |
| `cv_onb_perm_contacts_write_reason` | So we can auto-save unsaved inquiries to a group.                  |
| `cv_onb_perm_phone_state_name`      | Phone state                                                        |
| `cv_onb_perm_phone_state_reason`    | So the floating bubble and post-call popup can fire in real time.  |
| `cv_onb_perm_grant`                 | Grant permissions                                                  |
| `cv_onb_perm_why`                   | Why this matters                                                   |

### 13.12 ASCII wireframe

```
+----------------------------------+
| < Back        · · · · ·          |
|                                  |
|  Two taps and you're set.        |
|                                  |
|  Everything stays on your        |
|  device. callNest never         |
|  uploads your data.              |
|                                  |
|  [icon] Call log access          |
|         So we can list every...  |
|                                  |
|  [icon] Contacts (read)          |
|         So we know which calls...|
|                                  |
|  [icon] Contacts (write)         |
|         So we can auto-save...   |
|                                  |
|  [icon] Phone state              |
|         So the floating bubble...|
|                                  |
|     [   Grant permissions   ]    |
+----------------------------------+
```

### 13.13 Accessibility + performance

- Each row is one semantics node.
- Grant button announces its action via `contentDescription`.
- "Why this matters" section uses `liveRegion = Polite` so it
  is announced when expanded.

---

## 14 — Onboarding page 4: OEM battery

### 14.1 Purpose

Earn the manufacturer-specific autostart / battery-saver
exemption that keeps the foreground service alive. Without this,
the post-call popup and floating bubble silently die.

### 14.2 Entry points

From page 3.

### 14.3 Exit points

`Continue` → page 5. `Back` → page 3.

### 14.4 Required inputs (data)

`Build.MANUFACTURER` to select the vendor copy + intent.

### 14.5 Required inputs (user)

`Continue` (always available — this page is informational +
optional deep-link launch).

### 14.6 Mandatory display elements

1. Vendor-specific title, e.g. "Keep callNest running on your
   Xiaomi.".
2. Subtitle: "Your phone may stop callNest when you're not
   looking. Two settings keep it on.".
3. Numbered instruction list (3–5 bullets, vendor-specific).
4. `Open settings` `NeoButton` primary that fires the vendor
   intent.
5. `I've done this` text button (advances).
6. `Continue` is the same as "I've done this" — they share an
   action.

### 14.7 OEM detection table

| `Build.MANUFACTURER` (lowercased contains) | Vendor key | Display name          |
| ------------------------------------------ | ---------- | --------------------- |
| `xiaomi` / `redmi` / `poco`                | `xiaomi`   | Xiaomi / Redmi / POCO |
| `oppo`                                     | `oppo`     | Oppo                  |
| `vivo`                                     | `vivo`     | Vivo                  |
| `realme`                                   | `realme`   | Realme                |
| `samsung`                                  | `samsung`  | Samsung               |
| `oneplus`                                  | `oneplus`  | OnePlus               |
| `honor`                                    | `honor`    | Honor                 |
| `huawei`                                   | `huawei`   | Huawei                |
| anything else                              | `other`    | Other                 |

### 14.8 Vendor intent components

Each vendor has a primary intent. If it fails (`ActivityNotFoundException`), fall through to the next.

| Vendor    | Primary `ComponentName`                                                 |
| --------- | ----------------------------------------------------------------------- |
| `xiaomi`  | `com.miui.securitycenter` / `.permission.AutoStartManagementActivity`   |
| `oppo`    | `com.coloros.safecenter` / `.permission.startup.StartupAppListActivity` |
| `vivo`    | `com.vivo.permissionmanager` / `.activity.BgStartUpManagerActivity`     |
| `realme`  | `com.coloros.safecenter` / `.permission.startup.StartupAppListActivity` |
| `samsung` | `com.samsung.android.lool` / `.battery.ui.BatteryActivity`              |
| `oneplus` | `com.oneplus.security` / `.chainlaunch.view.ChainLaunchAppListActivity` |
| `honor`   | `com.huawei.systemmanager` / `.optimize.process.ProtectActivity`        |
| `huawei`  | `com.huawei.systemmanager` / `.optimize.process.ProtectActivity`        |
| `other`   | (none — go straight to fallback)                                        |

### 14.9 Fallback chain

```
1. Try vendor intent (above).
2. If ActivityNotFoundException ->
   Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
   with data = "package:com.callNest.app".
3. If still ActivityNotFoundException ->
   Settings.ACTION_BATTERY_SAVER_SETTINGS.
4. If still nothing -> show snackbar "Couldn't find the right
   settings on your device. Long-press the callNest icon and
   tap 'App info'.".
```

### 14.10 Vendor instruction copy

#### Xiaomi (3–4 bullets)

1. Tap **Autostart** and toggle callNest **on**.
2. Go back; open **Battery saver**.
3. Find callNest → **No restrictions**.
4. (MIUI 14+) Tap **Other permissions** → enable **Display popup
   while running in background**.

#### Oppo

1. Tap **Allow Auto-Launch** and turn on callNest.
2. Go back; open **Power Manager** → **Battery Optimization**.
3. Find callNest → **Don't optimize**.
4. (ColorOS 13+) Tap **Floating windows** → enable for callNest.

#### Vivo

1. Tap **High background power consumption** → enable callNest.
2. Open **Battery** → **Background power consumption** →
   callNest → **Allow**.
3. (FunTouch 13+) Open **Permissions** → enable **Display over
   other apps**.

#### Realme

1. Tap **Auto Launch** → enable callNest.
2. Open **Battery** → **App battery management** → callNest →
   **Allow**.
3. (Realme UI 4+) Enable **Floating windows** for callNest.

#### Samsung

1. Tap **Battery** → **Background usage limits**.
2. Add callNest to **Never sleeping apps**.
3. Open **App info** → **Battery** → **Unrestricted**.

#### OnePlus

1. Tap **Battery** → **Battery optimization** → callNest →
   **Don't optimize**.
2. Open **Advanced Optimization** → enable for callNest.

#### Honor

1. Tap **Protected apps** and enable callNest.
2. Open **Battery** → **Launch** → enable all three toggles for
   callNest.

#### Huawei

1. Tap **Protected apps** and enable callNest.
2. Open **App launch** → callNest → enable **Auto-launch**,
   **Secondary launch**, **Run in background**.

#### Other

1. Open **App info** for callNest.
2. Open **Battery** → set to **Unrestricted**.
3. (If available) Open **Advanced** → enable **Allow background
   activity**.

### 14.11 Copy table (per page)

| ID                        | English                                                                        |
| ------------------------- | ------------------------------------------------------------------------------ |
| `cv_onb_battery_title`    | Keep callNest running on your %s.                                              |
| `cv_onb_battery_subtitle` | Your phone may stop callNest when you're not looking. Two settings keep it on. |
| `cv_onb_battery_open`     | Open settings                                                                  |
| `cv_onb_battery_done`     | I've done this                                                                 |

`%s` is filled with the display name from §14.7.

### 14.12 Edge cases

- User on a brand-new vendor not in our table → `other` copy.
- User on Android Go: vendor intent often missing — fallback
  chain applies.
- User taps `Open settings`, returns immediately: we cannot
  detect what they did. We trust them.

### 14.13 ASCII wireframe

```
+----------------------------------+
| < Back        · · · · ·          |
|                                  |
|  Keep callNest running          |
|  on your Xiaomi.                 |
|                                  |
|  Your phone may stop callNest   |
|  when you're not looking. Two    |
|  settings keep it on.            |
|                                  |
|  1. Tap Autostart and toggle...  |
|  2. Go back; open Battery...     |
|  3. Find callNest → No restr... |
|  4. (MIUI 14+) Tap Other perm... |
|                                  |
|     [   Open settings   ]        |
|     [   I've done this  ]        |
+----------------------------------+
```

### 14.14 Accessibility + performance

- Numbered list is a semantics list (`Modifier.semantics { collectionInfo = CollectionInfo(rowCount = N, columnCount = 1) }`).
- Each step's text is a single semantics node.

---

## 15 — Onboarding page 5: First sync

### 15.1 Purpose

Run the first sync of `CallLog.Calls` so that when the user
lands on Main, the Calls tab is populated. Show progress; never
block.

### 15.2 Entry points

From page 4.

### 15.3 Exit points

`Continue` (only visible after `done` or `error`) → Main.
`Skip-for-now` (visible after `error`) → Main with possibly empty
Calls list.

### 15.4 Required inputs (data)

`SyncProgressBus` `SharedFlow<SyncProgress>` — see Part 01 §6.1
for the sync pipeline. Emissions:

```kotlin
sealed interface SyncProgress {
    data object Indeterminate : SyncProgress
    data class Determinate(val current: Int, val total: Int) : SyncProgress
    data object Done : SyncProgress
    data class Error(val cause: Throwable) : SyncProgress
}
```

### 15.5 Required inputs (user)

- `Continue` after `Done` (auto-advances after 1500ms in `Done`).
- `Retry` after `Error`.
- `Skip-for-now` after `Error`.

### 15.6 Mandatory display elements

1. Title `headlineSmall` "First import.".
2. Progress visualization:
   - **Indeterminate**: 64dp `NeoSpinner` (concave-rim,
     `IndeterminateStripe` motion) + body "Reading your call log…".
   - **Determinate**: linear `NeoProgressBar` + body "Found N of
     M calls so far…".
   - **Done**: green checkmark inside concave well + body "All
     caught up. Welcome to callNest.".
   - **Error**: red exclamation inside concave well + body
     "We couldn't finish the first import." + secondary body
     `error.localizedMessage` truncated to 2 lines.
3. Buttons (state-dependent):
   - Indeterminate / Determinate: no buttons.
   - Done: `Continue` primary (auto-advances after 1500ms even
     if untapped).
   - Error: `Retry` primary + `Skip for now` text button.

### 15.7 Optional display elements

A small "We'll keep syncing in the background." caption appears
under the progress bar in `Determinate` state if `total > 500`.

### 15.8 Empty state

A user with zero calls in their log: emits `Done` immediately
with `current = 0`. Body becomes "Your call log is empty for
now. callNest will start capturing as calls come in.".

### 15.9 Loading state

The whole page is a loading state for as long as the bus is in
`Indeterminate` or `Determinate`.

### 15.10 Error state

- Network is irrelevant (sync is local).
- Possible causes: `SecurityException` (perm revoked between
  pages 3 and 5), `SQLiteFullException`, `IllegalStateException`
  from a malformed call-log row.
- The error body is user-friendly: "Couldn't read your call log.
  Tap to grant permission." for `SecurityException`; "Your phone
  is low on storage. Free some space and tap Retry." for
  `SQLiteFullException`; otherwise the localized message.
- `Retry` re-runs the same use case. `Skip for now` advances
  with `setComplete()` regardless.

### 15.11 Edge cases

- Sync completes before the page is even shown (very fast
  device): we still hold the `Done` state for 1500ms so the user
  sees the success.
- Sync emits >1 `Indeterminate` then `Determinate` — UI
  crossfades smoothly via `CrossFade` motion token.
- `total` becomes known after 200+ rows already counted
  (cursor.moveToFirst then cursor.count): UI jumps from
  Indeterminate to Determinate at `current = 200`. Acceptable.

### 15.12 Copy table

| ID                            | English                                                                         |
| ----------------------------- | ------------------------------------------------------------------------------- |
| `cv_onb_sync_title`           | First import.                                                                   |
| `cv_onb_sync_indet_body`      | Reading your call log…                                                          |
| `cv_onb_sync_det_body_fmt`    | Found %1$d of %2$d calls so far…                                                |
| `cv_onb_sync_bg_caption`      | We'll keep syncing in the background.                                           |
| `cv_onb_sync_done_body`       | All caught up. Welcome to callNest.                                             |
| `cv_onb_sync_done_empty_body` | Your call log is empty for now. callNest will start capturing as calls come in. |
| `cv_onb_sync_error_title`     | We couldn't finish the first import.                                            |
| `cv_onb_sync_error_perm`      | Couldn't read your call log. Tap to grant permission.                           |
| `cv_onb_sync_error_storage`   | Your phone is low on storage. Free some space and tap Retry.                    |
| `cv_onb_sync_retry`           | Retry                                                                           |
| `cv_onb_sync_skip`            | Skip for now                                                                    |
| `cv_onb_sync_continue`        | Continue                                                                        |

### 15.13 ASCII wireframe (Determinate)

```
+----------------------------------+
|               · · · · ·          |
|                                  |
|         First import.            |
|                                  |
|                                  |
|        +-----------------+       |
|        | concave spinner |       |
|        +-----------------+       |
|                                  |
|     Found 124 of 480 calls...    |
|                                  |
|  [============-----------------]  |
|                                  |
|  We'll keep syncing in the       |
|  background.                     |
|                                  |
|                                  |
+----------------------------------+
```

### 15.14 ASCII wireframe (Done)

```
+----------------------------------+
|               · · · · ·          |
|                                  |
|         First import.            |
|                                  |
|        +-----------------+       |
|        |       ✓         |       |
|        | (green check)   |       |
|        +-----------------+       |
|                                  |
|  All caught up. Welcome          |
|  to callNest.                   |
|                                  |
|         [   Continue   ]         |
+----------------------------------+
```

### 15.15 ASCII wireframe (Error)

```
+----------------------------------+
|               · · · · ·          |
|                                  |
|         First import.            |
|                                  |
|        +-----------------+       |
|        |       !         |       |
|        |   (rose alert)  |       |
|        +-----------------+       |
|                                  |
|  We couldn't finish the          |
|  first import.                   |
|  Couldn't read your call log.    |
|  Tap to grant permission.        |
|                                  |
|         [    Retry    ]          |
|         [ Skip for now ]         |
+----------------------------------+
```

### 15.16 Accessibility + performance

- The progress region is `liveRegion = Polite` so TalkBack
  announces transitions.
- The Done checkmark animates in over 200ms; `Animatable` is
  cancelled if the page leaves before completion.
- On `Done` auto-advance, `setComplete()` fires before
  navigation so `onboardingComplete = true` is durably stored
  by the time Main composes.

### 15.17 Wiring summary

```
OnboardingFirstSyncPage
   collects SyncProgressBus.flow as state
   on enter:
      vm.startFirstSync()  // launches use case if not already running
   render by state:
      Indeterminate -> spinner
      Determinate   -> progress bar
      Done          -> checkmark + auto-advance(1500ms)
      Error         -> alert + Retry / Skip
   on advance:
      vm.setComplete()
      rootNav.navigate(Main) { popUpTo(Onboarding) { inclusive = true } }
```

The use case (`StartFirstSyncUseCase`) is idempotent: re-entering
the page after a process kill resumes from the last persisted
`SyncCheckpoint` — see Part 01 §6.1.

---

_End of Part 02 — Theme + Navigation + Splash + Onboarding._
_Next: Part 03 covers Main scaffold, bottom navigation, and the
four tab home pages (§§ 16–22)._
