# callNest — UI Guide

What an agent needs to build a callNest screen without exploring. Last refreshed 2026-05-06.

## Design system at a glance

callNest uses a **custom neumorphic system** ("Neo*"). Two kinds of surfaces — convex (raised) and concave (pressed-in) — render as the same base color `NeoColors.Base` with light/dark soft-shadow pairs from `NeoShadows.kt`. Material 3 typography is reused; Material widgets (Button, Card, TextField) are **not** for production surfaces — use Neo* equivalents.

> See `AUDIT_2026-05-06.md` § "UI direction" for the recommendation on whether to keep this system or migrate.

## Layout primitives (`ui/screen/shared/`)

| Primitive      | When to use                                                                                                                                                                                                                                                    |
| -------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `StandardPage` | Default for any non-tab screen. Header (emoji + title + description) + back arrow + scrollable body slot. Use `chromeless = true` for the 4 main tabs (Home/Calls/Inquiries/More) to suppress header. Supports `backgroundColor` and `headerGradient` per-tab. |
| `NeoScaffold`  | Use when you need full control: custom topBar, bottomBar, FAB, snackbar host. `ExportScreen` and `CallsScreen` use it. Don't combine with `StandardPage` — pick one.                                                                                           |

## Component inventory (`ui/components/neo/`, 25 files)

| Component                    | Purpose                                                                                                                                                                | Common props                                                            |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| `NeoSurface`                 | Base raised/concave surface. Everything else stacks on this.                                                                                                           | `elevation = NeoElevation.{Convex, ConvexSmall, ConcaveSmall}`, `shape` |
| `NeoCard`                    | Convex content card with padding.                                                                                                                                      | `border`, `onClick`                                                     |
| `NeoButton`                  | Primary CTA.                                                                                                                                                           | `variant = {Primary, Secondary, Tertiary}`, `icon`, `enabled`           |
| `NeoIconButton`              | Tappable icon (44dp default, can shrink to 32dp inline).                                                                                                               | `icon`, `size`, `contentDescription`                                    |
| `NeoFAB`                     | Floating action button.                                                                                                                                                | Same as NeoButton + position via Scaffold.                              |
| `NeoTextField`               | Labeled text input. **For inline rule-editor inputs use the unexposed `InlineTextField` in `autotagrules/components/ConditionRow.kt`** — should be promoted to `neo/`. |
| `NeoToggle`                  | iOS-style switch.                                                                                                                                                      | `checked`, `onChange`                                                   |
| `NeoChip`                    | Pill toggle / chip-button.                                                                                                                                             | `text`, `selected`, `onClick`                                           |
| `NeoSlider`                  | Lead-score weight slider.                                                                                                                                              | `value`, `onChange`, `range`                                            |
| `NeoTopBar`                  | Top app bar with optional nav icon + actions slot.                                                                                                                     | `title`, `navIcon`, `onNavClick`, `actions`                             |
| `NeoBottomSheet`             | Restyled M3 ModalBottomSheet.                                                                                                                                          | (used by Tags picker, filter sheet)                                     |
| `NeoDialog`                  | Custom-shaped dialog with header/body/footer slots.                                                                                                                    | (used by reset-all confirm)                                             |
| `NeoTabBar`                  | Bottom 4-tab nav for `MainScaffold`.                                                                                                                                   | (Home/Calls/Inquiries/More)                                             |
| `NeoPageHeader`              | Emoji + title + description block (used by `StandardPage` internally).                                                                                                 | `emoji`, `title`, `description`                                         |
| `NeoSearchBar`               | Single-line search field with leading magnifier.                                                                                                                       | `value`, `onChange`, `onClear`                                          |
| `NeoBadge`                   | Small count pill (recent unsaved).                                                                                                                                     | `count`                                                                 |
| `NeoChip` (selected variant) | Active filter chip.                                                                                                                                                    |                                                                         |
| `NeoEmptyState`              | Empty/error state w/ icon + title + message + action.                                                                                                                  | `icon`, `title`, `message`, `action`                                    |
| `NeoProgressBar`             | Determinate horizontal bar.                                                                                                                                            | `progress` (0..1)                                                       |
| `NeoTopLineLoader`           | Indeterminate top-of-screen line.                                                                                                                                      |                                                                         |
| `NeoLoader`                  | Centered spinner.                                                                                                                                                      |                                                                         |
| `NeoDivider`                 | Subtle horizontal separator.                                                                                                                                           |                                                                         |
| `NeoAvatar`                  | Initials-or-image circle.                                                                                                                                              | `name`, `imageUri`                                                      |
| `NeoHelpIcon`                | "?" button that opens the right docs article.                                                                                                                          | `articleId`                                                             |
| `LeadScoreBadge`             | Color-coded 0–100 score chip (Hot/Warm/Cool/Cold).                                                                                                                     | `score`                                                                 |
| `ShadowModifier`             | Internal `Modifier.neoShadow(...)` extension — used by Surface variants.                                                                                               | (don't apply directly outside the package)                              |

## Color tokens (`ui/theme/Color.kt`)

| Token                                                                                              | Use                                                 |
| -------------------------------------------------------------------------------------------------- | --------------------------------------------------- |
| `NeoColors.Base`                                                                                   | Page background for every screen.                   |
| `NeoColors.OnBase`                                                                                 | Default text on `Base`.                             |
| `NeoColors.OnBaseMuted` / `OnBaseSubtle`                                                           | Secondary / placeholder text.                       |
| `NeoColors.AccentBlue` / `AccentRose` / `AccentTeal` / `AccentViolet`                              | Status accents (links, missed, follow-up, unsaved). |
| `NeoColors.BorderAccent` / `BorderSoft`                                                            | Card borders (1dp).                                 |
| `SageColors.TextPrimary/Secondary/Tertiary`                                                        | Sage palette used inside cards.                     |
| Tab-specific: `TabBgHome / TabBgCalls / TabBgInquiries / TabBgMore` + `HeaderGrad{Tab}{Start,End}` | Per-tab background tinting and header gradient.     |
| `IconHomeTint / IconCallsTint / IconStatsTint / IconInquiriesTint / IconBackupTint / IconTagsTint` | More-row leading-emoji surface tints.               |

`Spacing.Xs/Sm/Md/Lg/Xl` (in `Spacing.kt`) — use these instead of literal `dp` for vertical/horizontal gaps.

`NeoElevation` — `Convex`, `ConvexSmall`, `ConcaveSmall` (3 variants only — don't invent more).

## Screen layout sketches

> Format: top-to-bottom blocks. Numbers in parens are the file name. "📦" = NeoCard, "▤" = LazyColumn item, "⊟" = NeoSurface.

### Splash → Login → (Onboarding | PermissionRationale | Main)

- **SplashScreen** — animated logo, transitions when `splashFinished && authState != Loading`.
- **LoginScreen** — header, EmailField, PasswordField, "Forgot password?" link (✗ unwired), "Sign in" button, "Create an account" link (✗ unwired). See AUDIT.

### Home tab (`HomeScreen`)

```
StandardPage(chromeless, headerGradHome)
├─ 📦 Today card  → 4 stat tiles (calls/missed/unsaved/followups due)
├─ 📦 Recent unsaved (last N + count badge)  → tap row = Inquiries
└─ 📦 Quick actions  → 4 chips: Calls · Stats · Backup · QuickExport
```

### Calls tab (`CallsScreen`)

```
NeoScaffold(no topBar — Phase III)
├─ Action row (right-aligned): 🔍 Search · ▤ Filter · 🔀 View-mode toggle
├─ PullToRefreshBox
│   ├─ UpdateBanner (when manifest says new version)
│   ├─ UnsavedPinnedSection (toggle-able)
│   ├─ ActiveFiltersRow (chips per active filter)
│   └─ Either flat-with-date-headers OR groupedByNumber LazyColumn
└─ BulkActionBar (only when bulkMode = true)
```

### Inquiries tab (`InquiriesScreen`)

LazyColumn of auto-saved inquiry rows + "Save selected to contacts" bulk action + BulkSaveProgressDialog.

### More tab (`MoreScreen`)

```
StandardPage(chromeless, headerGradMore)
├─ Group "Data": Export, Tags
├─ Group "Automation": Auto-tag rules, Lead scoring, Real-time, Auto-save
├─ Group "App": Stats, App updates, Help & docs, Settings
├─ Group "Account": Logout (with confirm dialog)
└─ "Made with ❤️ by Mahendra from India 🇮🇳" footer
```

### Call detail (`CallDetailScreen` + `sections/`)

```
StandardPage
├─ HeroCard (avatar, name/number, type icon, lead-score badge)
├─ ActionBar (Call · WhatsApp · SMS · Save to contacts)
├─ TagsSection (chips + add-tag → opens TagPickerSheet)
├─ FollowUpSection (date/time picker)
├─ NotesJournal (note rows + add-note opens NoteEditorDialog)
├─ HistoryTimeline (last N calls with this number)
└─ StatsCard (totals, avg duration, last contacted)
```

### Auto-tag rule editor (`RuleEditorScreen`)

```
StandardPage
├─ Name field + Active toggle
├─ LivePreviewBox ("matches X / 200 recent calls")
├─ Section "When all true" — list of ConditionRow + "Add condition" button → AlertDialog picker
├─ Section "Then" — list of ActionRow + "Add action" button → AlertDialog picker
└─ "Save" button
```

### Export wizard (`ExportScreen`)

```
NeoScaffold(topBar="Export", bottomBar=Back/Next/Generate)
├─ NeoPageHeader
└─ Step 0..4
    0: FormatStep (Excel only — 4 other formats are dead — see AUDIT)
    1: DateRangeStep
    2: ScopeStep
    3: ColumnsStep (skipped for non-table formats; today only Excel/CSV reach it)
    4: DestinationStep
```

### Settings (`SettingsScreen`)

LazyColumn of `SectionCard`:

- Sync (5 toggles)
- Notifications (3 toggles)
- Display (2 toggles)
- Privacy (2 toggles + Clear search history + Clear notes + Reset all data → typed-keyword confirm dialog)
- Help & docs (5 NavRows linking into in-app docs)
- About (version)

> `SettingsViewModel` still computes `autoSaveEnabled / bubbleEnabled / popupEnabled / leadScoringEnabled / autoBackupEnabled / autoBackupRetention / syncInterval` — none rendered in trimmed UI. Dead state — see AUDIT.

### Other screens

- **Stats** — header + 4 charts (DailyVolumeChart, HourlyHeatmap, TypeDonut, TopNumbersList). 6 more charts pending.
- **Tags manager** — list + edit dialog + delete.
- **Bookmarks** — list + reason dialog.
- **Follow-ups** — flat list, no calendar yet.
- **Search** — full-screen FTS overlay; result rows tap to call detail.
- **Onboarding** — 5 pages (Welcome, Features, Permissions, OemBattery, FirstSync).
- **Permission rationale / denied** — explainer + system permission launcher.
- **Update available** — release notes, download progress, install button.
- **Update settings** — version + last-checked + "Check now" button (trimmed; VM has dead surface — see AUDIT).
- **Backup** — list of `.cvb` backups + "Create now" + restore flow.
- **Auto-save settings, Lead-scoring settings, Real-time settings** — sliders + toggles for the corresponding subsystems.
- **Docs list / article** — in-app FAQ from `assets/docs/*.md`.

## Building a new screen — checklist

1. Pick `StandardPage` (default) or `NeoScaffold` (custom chrome).
2. Use `NeoCard` for grouped content, `NeoSurface(elevation = ConcaveSmall)` for input fields.
3. Strings → `R.string.*`. ViewModel collected via `collectAsStateWithLifecycle()`.
4. Add at least one `@Preview` (pass mock state, never inject Hilt into the previewed body).
5. Empty + loading + error states (see `NeoEmptyState`, `NeoLoader`, snackbar).
6. KDoc one line on the public composable.
7. Cross-check `AUDIT_2026-05-06.md` to make sure you're not landing on top of a known issue.
