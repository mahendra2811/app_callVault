# callNest APP-SPEC — Part 03

## Main scaffold + 4 tab pages

> Audience: a UX engineer rebuilding the callNest Android UI from scratch.
> This part is self-contained for the four primary tabs (Home, Calls,
> Inquiries, More) and the chrome that hosts them (top app bar +
> bottom navigation). Cross-references to other parts:
>
> - Data model and entities → Part 01 §5
> - App boot, permissions, splash → Part 02 §9
> - Call Detail screen (deep-link target from Calls tab) → Part 04
> - Settings + Stats + Updates → Part 05
> - Bottom sheets (Filter, Tag picker, Quick Export) → Part 06
> - Theme tokens (TabBg*, HeaderGrad*, BorderAccent, BorderSoft,
>   NeoSurface, NeoCard, NeoChip, NeoButton, NeoTabBar, NeoTopBar,
>   NeoAvatar, LeadScoreBadge) → Part 00
>
> Phase note: this document targets the post-Phase-I.6 UI which collapses
> "My Contacts" into Inquiries (Inquiries tab is the only contact-management
> surface), and reduces the top-bar overflow menu to two items.

---

## Table of contents

- §16 — MainScaffold (top bar + bottom nav)
- §17 — Home tab
- §18 — Calls tab
- §19 — Inquiries tab
- §20 — More tab

---

# §16 — MainScaffold (top bar + bottom nav)

The MainScaffold is not a "page". It is the persistent chrome that hosts
all four tab pages inside a nested NavHost. It owns the top app bar, the
bottom navigation, the back-press exit guard, the per-tab background tint,
and two CompositionLocals (`LocalRootNav`, `LocalMainTabNav`) that nested
screens use for navigation.

## 16.1 — Purpose

- Provide a single, durable Scaffold for the four primary tabs so
  headers, bottom nav, and system bar insets are consistent across every
  tab page.
- Host a **nested** `NavHost` (`mainTabNav`) so switching tabs does not
  destroy the back-stack of the other tabs. Each tab is a `composable`
  destination inside `mainTabNav`.
- Share top-level chrome controls (Search, Profile menu, badge counts)
  rather than reimplementing them per tab.
- Coordinate cross-tab side effects: refresh on tab change, exit guard
  on back-press, badge maintenance, sync banner.

## 16.2 — Entry points

| From                         | Behavior                                                                                     |
| ---------------------------- | -------------------------------------------------------------------------------------------- |
| App cold start (Launcher)    | After splash + permission gate (Part 02 §9.4), `rootNav` navigates to `route=main_tabs`.     |
| Process death restoration    | `MainScaffold` is rebuilt; `mainTabNav` restores its saved state and lands on last tab.      |
| Deep-link `callNest://calls` | Routes through rootNav → MainScaffold → mainTabNav.navigate("calls").                        |
| Notification tap (follow-up) | Routes through rootNav → MainScaffold → mainTabNav.navigate("home"), then opens detail.      |
| Returning from CallDetail    | rootNav.popBackStack() drops the detail; MainScaffold reappears on whichever tab was active. |

## 16.3 — Exit points

| To                             | Trigger                                                                                      |
| ------------------------------ | -------------------------------------------------------------------------------------------- |
| `route=call_detail/{number}`   | Row tap inside Calls tab or Home tab "Recent unsaved" list.                                  |
| `route=stats`                  | Quick-action chip on Home, or "Stats" row in More.                                           |
| `route=tag_manager`            | "Tags" row in More.                                                                          |
| `route=auto_tag_rules`         | "Auto-tag rules" row in More.                                                                |
| `route=lead_scoring`           | "Lead scoring" row in More.                                                                  |
| `route=realtime_features`      | "Real-time features" row in More.                                                            |
| `route=auto_save`              | "Auto-save" row in More.                                                                     |
| `route=app_updates`            | "App updates" row in More.                                                                   |
| `route=help_docs`              | "Help & docs" row in More.                                                                   |
| `route=settings`               | "Settings" row in More, or top-bar overflow → Settings.                                      |
| `route=backup`                 | "Backup & restore" row in More, or Home → Quick actions → Backup.                            |
| `route=export`                 | "Export" row in More.                                                                        |
| `route=search` (full-screen)   | Top-bar Search icon (any tab).                                                               |
| Quick Export bottom sheet      | Home → "Quick Export" chip OR More → "Quick Export" row. Modal overlay, **not** a route.     |
| Sign-out confirmation dialog   | Top-bar overflow → Sign out (only visible when Drive account is linked).                     |
| Android home (process visible) | Single back-press on root tab → snackbar "Press back again to exit"; second within 2s exits. |

## 16.4 — Required inputs (data)

| Source                    | Type / Flow                              | Default     | Used for                                         |
| ------------------------- | ---------------------------------------- | ----------- | ------------------------------------------------ |
| `mainTabNav`              | `NavHostController` (remembered)         | tab=`calls` | Nested navigation between the 4 tabs.            |
| `rootNav`                 | `NavHostController` (provided by parent) | n/a         | Routing OUT of the scaffold to detail screens.   |
| `currentRoute`            | `State<String?>`                         | `"calls"`   | Tab background tint + selected-tab indicator.    |
| `inquiriesBadge`          | `StateFlow<Int>`                         | 0           | Bottom-nav badge on Inquiries tab.               |
| `moreBadge`               | `StateFlow<Boolean>`                     | false       | Bottom-nav "1" dot on More tab when update due.  |
| `signedInToDrive`         | `StateFlow<Boolean>`                     | false       | Show / hide Sign-out menu item.                  |
| `syncProgress`            | `SharedFlow<SyncProgressEvent>`          | n/a         | Top-line linear progress bar inside the top bar. |
| `WindowInsets.systemBars` | Compose insets                           | n/a         | Padding for status bar and navigation bar.       |
| `BackHandler` state       | local (lastBackPressMs: Long)            | 0L          | Double-press-to-exit gate.                       |

ViewModel: `MainScaffoldViewModel @HiltViewModel constructor(contactRepo, updateRepo, driveAuthRepo, syncProgressBus)`.

State class:

```kotlin
data class MainScaffoldUiState(
    val inquiriesBadge: Int = 0,
    val moreBadge: Boolean = false,
    val signedInToDrive: Boolean = false,
    val syncing: Boolean = false,
    val syncProgressFraction: Float? = null
)
```

Combinators:

- `inquiriesBadge` ← `contactRepo.observeUnsavedLast7Days().map { it.size }`.
- `moreBadge` ← `combine(updateRepo.state, settings.observeUpdateSkipped()) { s, skipped -> s is UpdateState.Available && !skipped }`.
- `syncing` ← `syncProgressBus.flow.map { it !is SyncProgressEvent.Idle }.distinctUntilChanged()`.

## 16.5 — Required inputs (user)

| Trigger                          | Behavior                                                                                                                                    | State change                       |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- |
| Tap a bottom-nav tab             | `mainTabNav.navigate(tab) { popUpTo(start) { saveState = true } }`                                                                          | `currentRoute` updates.            |
| Tap currently-selected tab       | Scroll to top (each tab observes a `LocalScrollToTopBus`).                                                                                  | Per-tab list scrolls to index 0.   |
| Long-press a bottom-nav tab      | Reserved for future "switcher". Currently no-op + light haptic.                                                                             | None.                              |
| Tap Search icon (top bar)        | `rootNav.navigate("search")` — opens full-screen search overlay.                                                                            | None.                              |
| Tap overflow (⋮)                 | Open `DropdownMenu` with Profile + (optional) Sign out.                                                                                     | `menuExpanded = true`.             |
| Tap "Sign out" menu item         | Open AlertDialog → confirm → `driveAuthRepo.signOut()`.                                                                                     | `signedInToDrive` becomes `false`. |
| Tap "Profile" menu item          | `rootNav.navigate("settings?focus=profile")`.                                                                                               | None at scaffold.                  |
| System back-press at root        | If `lastBackPressMs` within 2000 ms → `(activity as Activity).finish()`. Else show snackbar "Press back again to exit" + record press time. | `lastBackPressMs` set.             |
| System back-press inside a sheet | Sheet handles its own back; scaffold does not consume.                                                                                      | None.                              |
| Pull-to-refresh inside a tab     | Tab forwards to `SyncScheduler.triggerOnce()`. Scaffold reflects via top-line.                                                              | `syncing = true` until `Idle`.     |

## 16.6 — Mandatory display elements

### 16.6.1 — NeoTopBar

- `showBrand = true` → leading slot renders the small callNest wordmark
  (`logoMonochrome` painter at 18.sp height).
- `title = "callNest"` (lowercase brand). Style: `MaterialTheme.typography.titleMedium`,
  `letterSpacing = 0.5.sp`. Color: `Theme.OnSurfaceStrong`.
- Leading icon: app glyph (24.dp NeoSurface concave circle, tinted `BrandTeal`).
- Trailing actions:
  1. Search icon (`Icons.Outlined.Search`, 24.dp). Touch target 48.dp.
  2. Overflow menu (⋮ `Icons.Outlined.MoreVert`, 24.dp). Touch target 48.dp.
- Top-line indeterminate `LinearProgressIndicator` rendered just below
  the bar when `syncing == true`. Height 2.dp. Color `BrandTeal`. Hidden
  otherwise (no layout reservation; renders in an overlay box so the
  page below does not jump).

### 16.6.2 — NeoTabBar (bottom)

- Four tabs in fixed order:

  | Index | Route       | Label     | Icon (selected)          | Icon (unselected)          |
  | ----- | ----------- | --------- | ------------------------ | -------------------------- |
  | 0     | `home`      | Home      | `Icons.Filled.Home`      | `Icons.Outlined.Home`      |
  | 1     | `calls`     | Calls     | `Icons.Filled.Call`      | `Icons.Outlined.Call`      |
  | 2     | `inquiries` | Inquiries | `Icons.Filled.Inbox`     | `Icons.Outlined.Inbox`     |
  | 3     | `more`      | More      | `Icons.Filled.MoreHoriz` | `Icons.Outlined.MoreHoriz` |

- Selected indicator: 4.dp pill behind the selected icon, color
  `BrandTeal.copy(alpha=0.18f)`.
- Inquiries badge: small pill displaying `inquiriesBadge`. When 0, hide.
  When > 99, show `99+`.
- More badge: 8.dp dot, color `Accent.Warning`, no number. Visible only
  when `moreBadge == true`.
- Bottom inset: `Modifier.windowInsetsPadding(WindowInsets.navigationBars)`
  ensures the bar floats above gesture/3-button nav.
- Height: 64.dp content + system inset.

### 16.6.3 — Per-tab background tint

The Scaffold container sets `containerColor` based on `currentRoute`:

| Route       | Container color  |
| ----------- | ---------------- |
| `home`      | `TabBgHome`      |
| `calls`     | `TabBgCalls`     |
| `inquiries` | `TabBgInquiries` |
| `more`      | `TabBgMore`      |

The tint changes are animated with `animateColorAsState(durationMillis = 240)`.

### 16.6.4 — System bar handling

- `Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))`
  on the top bar.
- `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` on the bottom bar.
- Edge-to-edge enabled at Activity level (`enableEdgeToEdge()`).
- Light/dark status icons follow `MaterialTheme.colorScheme.surface` luminance.

### 16.6.5 — Snackbar host

`SnackbarHostState` is hoisted at scaffold level so any tab page may post
a snackbar via `LocalSnackbarHost.current.showSnackbar(...)`. Position:
above the bottom nav, with 8.dp gap.

### 16.6.6 — CompositionLocals

```kotlin
val LocalRootNav = staticCompositionLocalOf<NavHostController> { error("rootNav not provided") }
val LocalMainTabNav = staticCompositionLocalOf<NavHostController> { error("mainTabNav not provided") }
val LocalSnackbarHost = staticCompositionLocalOf<SnackbarHostState> { error("snackbarHost not provided") }
val LocalScrollToTopBus = staticCompositionLocalOf<MutableSharedFlow<String>> { error("scrollToTopBus not provided") }
```

These are provided once, at the top of `MainScaffold { ... }`, and consumed by
every tab page.

## 16.7 — Optional display elements

| Element                  | Condition                                               |
| ------------------------ | ------------------------------------------------------- |
| Top-line linear progress | `syncing == true`.                                      |
| Sign out menu item       | `signedInToDrive == true`.                              |
| Inquiries badge          | `inquiriesBadge > 0`.                                   |
| More dot                 | `moreBadge == true` (update available AND not skipped). |
| Snackbar                 | When any tab posts a message.                           |
| Update banner (in-tab)   | Tabs may render their own banners; scaffold does not.   |

## 16.8 — Empty state

Scaffold itself has no empty state. Each tab handles its own. If
`mainTabNav` is somehow without a current destination (should be
unreachable), fall back to `home` and log a Timber warning.

## 16.9 — Loading state

The scaffold renders immediately (no blocking IO at construction). The
top-line `LinearProgressIndicator` is the only loading affordance owned
at this level. Tab pages have their own skeletons / spinners.

## 16.10 — Error state

The scaffold has no error UI of its own. If `MainScaffoldViewModel`
encounters a flow error from a repository, it is caught with `.catch { Timber.e(it) }`
and the badge silently falls back to the last known value (or 0). The
scaffold never blocks the user from seeing the tabs because of a
transient repo error.

## 16.11 — Edge cases

1. **Process death during sync**: when restored, `syncing` is recomputed
   from `SyncProgressBus.replayCache.last()`. If empty, default to false.
2. **Tab switch mid-scroll**: each tab uses `rememberSaveable(key="tab-$route") { LazyListState() }`
   so scroll position is preserved across tab switches.
3. **Back from a deep tab destination**: `rootNav` handles the pop. If the
   user is on `home` and presses back twice within 2s, the activity
   finishes; otherwise a snackbar shows.
4. **System bar collision with FAB**: there is no FAB at scaffold level.
   Tab pages own any FABs and apply `windowInsetsPadding` themselves.
5. **Right-to-left languages**: the bottom nav uses `LocalLayoutDirection`.
   Tab order remains logical (Home first), which mirrors visually for RTL.
6. **Foldable / large screens**: `compactScreen = maxWidth < 600.dp`. On
   non-compact, the scaffold delegates to a `MainScaffoldExpanded`
   composable (rail nav). Out of scope here; documented in Part 07.
7. **Deep-link to a tab while another tab is still loading**: the target
   tab navigates regardless of loading state. The previous tab's loading
   state survives via `saveState = true`.
8. **Snackbar collision with bottom sheet**: snackbar layer sits above
   bottom nav and **below** modal sheets, so a sheet hides any active
   snackbar.
9. **Update arrives while user is on More tab**: the More-tab dot lights
   up immediately, and the App-update row also gains its own inline
   "Available — install now" affordance (see §20).
10. **Drive sign-out while on More tab**: the overflow menu's Sign-out
    item disappears mid-render. Use `AnimatedVisibility` to fade it.

## 16.12 — Copy table

| String id                  | English                                    | Notes                         |
| -------------------------- | ------------------------------------------ | ----------------------------- |
| `app_brand`                | callNest                                   | Lowercase. Do not localize.   |
| `top_bar_search_a11y`      | Search                                     | TalkBack action label.        |
| `top_bar_overflow_a11y`    | More options                               | TalkBack.                     |
| `top_bar_menu_profile`     | Profile                                    | Routes to settings#profile.   |
| `top_bar_menu_signout`     | Sign out                                   | Drive only.                   |
| `signout_confirm_title`    | Sign out of Google Drive?                  | AlertDialog title.            |
| `signout_confirm_body`     | Backups will pause until you sign back in. |                               |
| `signout_confirm_cta`      | Sign out                                   | Destructive.                  |
| `signout_cancel`           | Cancel                                     |                               |
| `nav_home`                 | Home                                       |                               |
| `nav_calls`                | Calls                                      |                               |
| `nav_inquiries`            | Inquiries                                  |                               |
| `nav_more`                 | More                                       |                               |
| `inquiries_badge_overflow` | 99+                                        |                               |
| `back_press_exit`          | Press back again to exit                   | Snackbar.                     |
| `sync_in_progress_a11y`    | Syncing your call log                      | TalkBack on the progress bar. |

## 16.13 — ASCII wireframe

```
┌──────────────────────────────────────┐
│ ▒ callNest              🔍   ⋮      │  ← NeoTopBar (52.dp + status inset)
│ ──────────────────────────────────── │  ← optional 2.dp progress line
│                                      │
│           [ TAB CONTENT ]            │  ← nested NavHost
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
│                                      │
│ ┌──────────────────────────────────┐ │  ← snackbar host (above nav)
│ │ Press back again to exit         │ │
│ └──────────────────────────────────┘ │
├──────────────────────────────────────┤
│  🏠     📞     📥³    ⋯•             │  ← NeoTabBar (64.dp + nav inset)
│ Home  Calls  Inquiries  More         │
└──────────────────────────────────────┘
```

The `³` after Inquiries is the badge count; the `•` after More is the
update dot.

## 16.14 — Accessibility

- Every icon-only button has a `contentDescription` from string resources.
- Bottom-nav tabs use `Modifier.semantics { selected = (route == currentRoute); role = Role.Tab }`.
  TalkBack reads "Calls, tab, 2 of 4, selected".
- Touch targets: 48.dp minimum for tabs, top-bar actions, and overflow
  menu items.
- Contrast: `BrandTeal` on `TabBg*` ≥ 4.5:1 verified for selected indicator.
- Snackbar uses `LiveRegion.Polite`.
- The double-press exit snackbar is non-dismissible by tap (3s duration)
  to prevent accidental dismiss.
- Dynamic type up to 200% does not break the bottom nav: labels truncate
  with `…` and ellipsize one line.
- High-contrast mode swaps NeoSurface concave shadows for solid 1.dp
  borders (handled in NeoTheme).

## 16.15 — Performance budget

- First paint of MainScaffold after splash: < 80 ms on Pixel 4a (cold
  composition; tab pages composed lazily).
- Tab switch: < 16 ms recomposition. Achieve via `key(currentRoute) { ... }`
  on the per-tab background animator, not the whole scaffold.
- Top-bar progress overlay must not re-layout the page. Render in a
  `Box` over the content, not in the column flow.
- Snackbar host limits concurrent messages to 1 (Material default).
- Memory: scaffold itself holds no list state — all heavy state lives in
  tab ViewModels that are scoped to the destination via `hiltViewModel()`.

---

# §17 — Home tab

The Home tab is the at-a-glance landing surface for the day. It provides
a snapshot of activity, the topmost unsaved callers, and quick links to
the most-used flows. It does NOT host any navigation logic of its own;
all routing goes through `LocalRootNav` or `LocalMainTabNav`.

## 17.1 — Purpose

Give the user a 10-second readout of "what happened today and what
needs my attention" without scrolling more than one screen on a Pixel
4a. Provide three first-class entry points:

1. The numeric snapshot of today.
2. The shortlist of recent unsaved callers (deep-link into Inquiries).
3. The four quick actions (Calls, Stats, Backup, Quick Export).

## 17.2 — Entry points

| From                        | Behavior                                                                     |
| --------------------------- | ---------------------------------------------------------------------------- |
| App start                   | Land on `calls` (default tab). User taps `home` tab to arrive here.          |
| Notification: follow-up due | rootNav → main_tabs; mainTabNav → home; pulse "Follow-ups due" tile briefly. |
| Notification: backup result | rootNav → main_tabs → home (no in-app dialog).                               |
| Tap `home` in bottom nav    | Direct.                                                                      |
| Re-tap `home` while on Home | LocalScrollToTopBus emits "home" → list scrolls to top.                      |
| Process restart             | Restored to whichever tab was active; if "home", state restored from VM.     |

## 17.3 — Exit points

| To                           | Trigger                                              |
| ---------------------------- | ---------------------------------------------------- |
| `inquiries` tab              | Tap "Save all" inside Recent-unsaved card.           |
| `calls` tab                  | Tap Quick Action chip "Calls".                       |
| `route=stats`                | Tap Quick Action chip "Stats".                       |
| `route=backup`               | Tap Quick Action chip "Backup".                      |
| Quick Export bottom sheet    | Tap Quick Action chip "Quick Export". Modal overlay. |
| `route=call_detail/{number}` | Tap any row in Recent-unsaved list.                  |
| `route=follow_ups`           | Tap "Follow-ups due" tile (scoped to upcoming list). |

## 17.4 — Required inputs (data)

ViewModel: `HomeViewModel @HiltViewModel constructor(callRepo: CallRepository, contactRepo: ContactRepository, settings: SettingsDataStore)`.

State:

```kotlin
data class HomeUiState(
    val callsToday: Int = 0,
    val missedToday: Int = 0,
    val unsavedTotal: Int = 0,
    val followUpsDue: Int = 0,
    val recentUnsaved: List<RecentUnsavedItem> = emptyList(),
    val loading: Boolean = true,
    val syncRunning: Boolean = false,
    val permissionGranted: Boolean = true
)

data class RecentUnsavedItem(
    val normalizedNumber: String,
    val displayLabel: String,
    val initial: Char,
    val accentSeed: Int,
    val lastCallEpochMs: Long,
    val callCount: Int
)
```

Source flows:

| State field         | Type                    | Source                                               | Default |
| ------------------- | ----------------------- | ---------------------------------------------------- | ------- |
| `callsToday`        | Int                     | `callRepo.observeRecent(200).map { dayBucket(it) }`  | 0       |
| `missedToday`       | Int                     | derived from same window, type==MISSED               | 0       |
| `unsavedTotal`      | Int                     | `callRepo.observeUnsavedLast7Days().map { it.size }` | 0       |
| `followUpsDue`      | Int                     | `followUpRepo.observeDueWithin24h().map { it.size }` | 0       |
| `recentUnsaved`     | List<RecentUnsavedItem> | `callRepo.observeUnsavedLast7Days().map { take(3) }` | empty   |
| `loading`           | Boolean                 | `combine` first emission                             | true    |
| `syncRunning`       | Boolean                 | `SyncProgressBus.flow.map { it !is Idle }`           | false   |
| `permissionGranted` | Boolean                 | `permissionAdapter.observeReadCallLog()`             | true    |

Combine:

```kotlin
val state: StateFlow<HomeUiState> = combine(
    callRepo.observeRecent(200),
    callRepo.observeUnsavedLast7Days(),
    followUpRepo.observeDueWithin24h(),
    syncProgressBus.flow.onStart { emit(SyncProgressEvent.Idle) },
    permissionAdapter.observeReadCallLog()
) { recent, unsaved, dueFollowUps, syncEvt, granted ->
    val (todayStart, now) = todayBounds()
    val todayCalls = recent.filter { it.startedAtMs in todayStart..now }
    HomeUiState(
        callsToday = todayCalls.size,
        missedToday = todayCalls.count { it.type == CallType.MISSED },
        unsavedTotal = unsaved.size,
        followUpsDue = dueFollowUps.size,
        recentUnsaved = unsaved.take(3).map { it.toItem() },
        loading = false,
        syncRunning = syncEvt !is SyncProgressEvent.Idle,
        permissionGranted = granted
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
```

## 17.5 — Required inputs (user)

| Trigger                            | Behavior                                                          | State change                  |
| ---------------------------------- | ----------------------------------------------------------------- | ----------------------------- |
| Tap "Calls today" stat tile        | `mainTabNav.navigate("calls")` with filter preset = today.        | None (navigation event).      |
| Tap "Missed" stat tile             | `mainTabNav.navigate("calls")` with filter preset = missed,today. | None.                         |
| Tap "Unsaved" stat tile            | `mainTabNav.navigate("inquiries")`.                               | None.                         |
| Tap "Follow-ups due" stat tile     | `rootNav.navigate("follow_ups")`.                                 | None.                         |
| Tap a Recent-unsaved row           | `rootNav.navigate("call_detail/$number")`.                        | None.                         |
| Tap "Save all" CTA                 | `mainTabNav.navigate("inquiries")` and pulse the inquiries tab.   | None.                         |
| Tap Quick Action: 📞 Calls         | `mainTabNav.navigate("calls")`.                                   | None.                         |
| Tap Quick Action: 📊 Stats         | `rootNav.navigate("stats")`.                                      | None.                         |
| Tap Quick Action: 💾 Backup        | `rootNav.navigate("backup")`.                                     | None.                         |
| Tap Quick Action: 📥 Quick Export  | Parent-controlled `quickExportSheetVisible = true`.               | Modal overlay opens.          |
| Pull to refresh                    | `SyncScheduler.triggerOnce()`.                                    | `syncRunning = true` briefly. |
| Long-press a stat tile             | Show contextual hint tooltip (e.g. "Calls received today").       | None.                         |
| Long-press "Recent unsaved" header | Open a hidden dev menu (only when build is debug).                | None in release.              |
| Re-tap Home tab                    | Scroll to top via `LocalScrollToTopBus`.                          | List `firstVisibleItem = 0`.  |

## 17.6 — Mandatory display elements

The Home tab uses `StandardPage` as its container:

```kotlin
StandardPage(
    title = "Home",
    description = "Today at a glance",
    emoji = "🏠",
    backgroundColor = TabBgHome,
    headerGradient = HeaderGradHome
) { padding ->
    LazyColumn(
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TodaysSnapshotCard(state) }
        item { RecentUnsavedCard(state) }
        item { QuickActionsCard(...) }
    }
}
```

### 17.6.1 — Card 1: Today's snapshot

- Container: `NeoCard(border = BorderAccent)`. Padding 16.dp.
- Header row:
  - Emoji 📅, label "Today's snapshot" (`titleSmall` bold).
  - Trailing relative timestamp "as of HH:mm" (muted).
- Body: a `Row(horizontalArrangement = Arrangement.SpaceBetween)` with
  four equal-width tiles. Each tile is a `Column(horizontalAlignment = CenterHorizontally)`:
  - Emoji (24.sp): `📞 / ❌ / 📥 / 🔔`.
  - Count: `headlineSmall` bold, color `OnSurfaceStrong`.
  - Label: `labelSmall`, color `OnSurfaceMuted`.

| Tile | Emoji | Count source   | Label          |
| ---- | ----- | -------------- | -------------- |
| 1    | 📞    | `callsToday`   | Calls          |
| 2    | ❌    | `missedToday`  | Missed         |
| 3    | 📥    | `unsavedTotal` | Unsaved        |
| 4    | 🔔    | `followUpsDue` | Follow-ups due |

Each tile is a `Modifier.clickable` target with 48.dp min height.

### 17.6.2 — Card 2: Recent unsaved

- Container: `NeoCard(border = BorderSoft)`.
- Header row: "Recent unsaved" + count badge `({unsavedTotal})`.
- Body: top 3 rows from `recentUnsaved`. Each row:
  - `NeoAvatar(initial = item.initial, accentSeed = item.accentSeed)`
    (40.dp; deterministic tint).
  - `Column`:
    - Line 1: `item.displayLabel` (`bodyMedium` bold).
    - Line 2: `formatRelative(item.lastCallEpochMs)` + " · " + `"${item.callCount} calls"` (`bodySmall`, muted).
  - Trailing chevron (`KeyboardArrowRight`, 20.dp, muted).
- Inline `NeoButton` "Save all" pinned at the bottom-right of the card,
  visible only when `unsavedTotal > 0`.
- Empty body (when `unsavedTotal == 0`): single line "Nothing pending. ✨" in
  italic muted, height 48.dp.

### 17.6.3 — Card 3: Quick actions

- Container: `NeoCard(border = BorderSoft)`.
- Header: "Quick actions".
- Body: a 2×2 grid of `NeoChip` buttons (or a `FlowRow` if width permits
  4 across; on Pixel 4a it wraps to 2×2 because each chip is 144.dp wide).
- Chips:

  | Chip emoji | Label        | Action                                      |
  | ---------- | ------------ | ------------------------------------------- |
  | 📞         | Calls        | `mainTabNav.navigate("calls")`              |
  | 📊         | Stats        | `rootNav.navigate("stats")`                 |
  | 💾         | Backup       | `rootNav.navigate("backup")`                |
  | 📥         | Quick Export | `onQuickExportClick()` (opens parent sheet) |

Each chip: 56.dp height. Concave NeoSurface. Press state ripples to
convex briefly (200 ms).

## 17.7 — Optional display elements

| Element                     | Condition                                                           |
| --------------------------- | ------------------------------------------------------------------- |
| Top-line sync progress      | `syncRunning == true` (rendered by scaffold).                       |
| Permission rationale banner | `permissionGranted == false`. Pinned above Card 1.                  |
| "First-day welcome" banner  | If user has < 5 total calls in DB. Dismissible.                     |
| Insight card (rotating)     | Reserved for Phase II; behind feature flag `home_insights_enabled`. |
| "Backup overdue" banner     | If lastBackupAt is older than 14 days AND Drive is connected.       |

## 17.8 — Empty state

If `state == HomeUiState()` (initial, post-loading) and DB is empty:

```
┌────────────────────────────────────┐
│           (centered)               │
│            📭                      │
│      No calls yet                  │
│  Pull down to sync your call log   │
│    [ Grant permission ]   <- only  │
│           if needed                │
└────────────────────────────────────┘
```

- Icon: `📭` at 64.sp.
- Title: `titleMedium`, "No calls yet."
- Subtitle: `bodyMedium` muted, "Pull down to sync your call log."
- CTA `NeoButton`: text varies — "Grant permission" if not granted else
  "Sync now" → triggers `SyncScheduler.triggerOnce()`.

## 17.9 — Loading state

- First emission: render three skeleton placeholders shaped like the
  three cards (Card 1: header line + 4 placeholder rectangles; Card 2:
  header + 3 placeholder rows; Card 3: header + 4 chip-shaped boxes).
- Use `Modifier.shimmer()` (Coil 3 + custom shimmer modifier).
- Skeleton duration cap: 800 ms. If state has not arrived, switch to
  spinner `CircularProgressIndicator` to avoid eternal shimmer.

## 17.10 — Error state

If `combine` throws (`.catch { ... }`):

- Render a `NeoCard` (border = BorderSoft) with:
  - Title "Something's off."
  - Body "We couldn't load your snapshot. Pull down to retry."
  - `NeoButton` "Retry" → triggers a re-collect (`viewModel.retry()`).
- Snackbar: "Snapshot unavailable. Retrying…" (auto-dismiss 4s).
- Cards 2 and 3 still render with whatever partial state is available.

## 17.11 — Edge cases

1. **0 calls today** — Stat tile shows "0", color muted. Tap is still
   active and routes to Calls tab; the user will see whatever pre-today
   data exists.
2. **100+ unsaved** — `unsavedTotal` rendered as the literal number,
   not capped. The Recent-unsaved list still shows top 3.
3. **Follow-up firing during render** — A new `followUpsDue` value
   arrives while the user looks at Home. The tile pulses (one-shot
   `animateFloatAsState` brightening for 600 ms).
4. **Sync in progress** — Scaffold shows the top-line bar; Home does
   nothing extra. Pull-to-refresh is disabled until current sync ends.
5. **Permission missing** — Render the banner above Card 1: "We need
   call log access to show your snapshot." with `Grant` CTA which
   re-triggers the permission rationale sheet.
6. **Low memory pressure** — `recentUnsaved` is hard-capped at 3, regardless
   of source size. List items are stable-keyed by normalizedNumber so
   recompositions skip.
7. **Day rollover at midnight** — `todayBounds()` is recomputed on every
   emission, so right after midnight the snapshot resets.
8. **DST transition** — `todayBounds()` uses `ZoneId.systemDefault()` and
   `LocalDate.now()` for safety.
9. **No network and Drive backup overdue** — banner still shows; tapping
   it navigates to Backup screen which displays a "no network" hint there.
10. **User opened app at 23:59** — Snapshot will reset within the minute.
    No client action; the next state emission will reflect the new day.

## 17.12 — Copy table

| String id                      | English                                             | Placeholders            |
| ------------------------------ | --------------------------------------------------- | ----------------------- |
| `home_title`                   | Home                                                |                         |
| `home_description`             | Today at a glance                                   |                         |
| `home_card_snapshot_title`     | Today's snapshot                                    |                         |
| `home_snapshot_as_of`          | as of %1$s                                          | %1$s = local time HH:mm |
| `home_tile_calls`              | Calls                                               |                         |
| `home_tile_missed`             | Missed                                              |                         |
| `home_tile_unsaved`            | Unsaved                                             |                         |
| `home_tile_follow_ups`         | Follow-ups due                                      |                         |
| `home_card_recent_title`       | Recent unsaved                                      |                         |
| `home_recent_count`            | (%1$d)                                              | %1$d = unsavedTotal     |
| `home_recent_empty`            | Nothing pending. ✨                                 |                         |
| `home_recent_save_all`         | Save all                                            |                         |
| `home_recent_row_calls`        | %1$d calls                                          | %1$d = call count       |
| `home_card_quickactions_title` | Quick actions                                       |                         |
| `home_quick_calls`             | Calls                                               |                         |
| `home_quick_stats`             | Stats                                               |                         |
| `home_quick_backup`            | Backup                                              |                         |
| `home_quick_export`            | Quick Export                                        |                         |
| `home_empty_title`             | No calls yet                                        |                         |
| `home_empty_subtitle`          | Pull down to sync your call log.                    |                         |
| `home_empty_cta_grant`         | Grant permission                                    |                         |
| `home_empty_cta_sync`          | Sync now                                            |                         |
| `home_perm_banner`             | We need call log access to show your snapshot.      |                         |
| `home_perm_grant`              | Grant                                               |                         |
| `home_backup_overdue`          | Last backup was %1$s ago. Tap to back up now.       | %1$s = relative time    |
| `home_error_title`             | Something's off.                                    |                         |
| `home_error_body`              | We couldn't load your snapshot. Pull down to retry. |                         |
| `home_error_retry`             | Retry                                               |                         |

## 17.13 — ASCII wireframe

```
┌──────────────────────────────────────┐
│ ▒ callNest            🔍   ⋮        │
├──────────────────────────────────────┤
│  🏠  Home                            │
│      Today at a glance               │
│ ──────────────────────────────────── │
│ ┌──────────────────────────────────┐ │
│ │ 📅 Today's snapshot   as of 14:32│ │
│ │ ┌────┐ ┌────┐ ┌────┐ ┌────┐     │ │
│ │ │ 📞 │ │ ❌ │ │ 📥 │ │ 🔔 │     │ │
│ │ │ 47 │ │  3 │ │  9 │ │  2 │     │ │
│ │ │Call│ │Miss│ │Unsv│ │FU  │     │ │
│ │ └────┘ └────┘ └────┘ └────┘     │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ Recent unsaved              (9)  │ │
│ │ ─────────────────────────────────│ │
│ │ ◉ +91 98765 43210  · 12 m · 3 c >│ │
│ │ ◉ +91 99887 76655  · 35 m · 1 c >│ │
│ │ ◉ +91 90909 90909  · 2 h  · 2 c >│ │
│ │              [ Save all ]        │ │
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │ Quick actions                    │ │
│ │ [ 📞 Calls ] [ 📊 Stats ]        │ │
│ │ [ 💾 Backup ] [ 📥 Quick Export ]│ │
│ └──────────────────────────────────┘ │
├──────────────────────────────────────┤
│  🏠   📞   📥³   ⋯                   │
└──────────────────────────────────────┘
```

## 17.14 — Accessibility

- TalkBack reads each stat tile as: "Calls today, 47, button". Achieved
  via `Modifier.semantics(mergeDescendants = true) { contentDescription = "..." ; role = Role.Button }`.
- Stat tiles have a focus order that flows left-to-right then down.
- Recent-unsaved rows: "Unsaved caller, +91 98765 43210, last call 12
  minutes ago, 3 calls. Double-tap to open."
- "Save all" button: "Save all unsaved callers, button. Opens Inquiries."
- Quick-action chips: "Backup, button" — includes destination hint via
  `onClickLabel = "Open backup screen"`.
- Touch targets: all interactive elements ≥ 48.dp.
- Dynamic type: `headlineSmall` allowed up to 200% scale before tiles
  switch to a 1-up vertical layout (handled by `BoxWithConstraints`).
- Contrast: count text on `NeoCard` ≥ 7:1 (AAA).

## 17.15 — Performance budget

- First paint of Home: < 120 ms after tab switch on Pixel 4a.
- `LazyColumn` items: 3 (always). No virtualization concerns.
- Recompositions on stat update: limited to the changed tile via
  derivedStateOf-keyed slot composables.
- Initial data load: ≤ 200 ms for the combine emission against a
  500-row DB (Room with index on `started_at`).
- Memory: < 1 MB for the Home VM state.
- No image loading on Home (avatars are vector initials).

---

# §18 — Calls tab

The Calls tab is the primary work surface and the most complex tab.
It shows the user's call log, lets them filter, search, switch between
flat and grouped-by-number views, pin recent unsaved inquiries at the
top, and operate on multiple calls at once via bulk-select.

## 18.1 — Purpose

Be the daily driver. The user spends the majority of in-app time here.
The page must:

- Load fast (skeleton in < 80 ms; first visible row in < 250 ms on a
  10k-row DB).
- Surface the most-actionable items first (pinned unsaved last 7 days).
- Make every row tap-rich: row-tap to detail, long-press to bulk-select,
  swipe for the two configurable quick actions.
- Support filters that survive process restart (saved presets) and
  active filters as removable chips.

## 18.2 — Entry points

| From                        | Behavior                                    |
| --------------------------- | ------------------------------------------- |
| App start                   | Calls is the default tab (locked).          |
| Bottom nav tap              | Direct.                                     |
| Home → "Calls" stat tile    | Pre-applies filter preset = today.          |
| Home → "Missed" stat tile   | Pre-applies filter preset = today + missed. |
| Home → Quick action: Calls  | Direct.                                     |
| Notification: missed call   | rootNav → main_tabs → calls; row pulses.    |
| Search overlay → result tap | Opens Call Detail; back lands here.         |
| Process restart             | Restored to Calls if it was active.         |
| Re-tap Calls in bottom nav  | Scroll to top.                              |

## 18.3 — Exit points

| To                           | Trigger                                                         |
| ---------------------------- | --------------------------------------------------------------- |
| `route=call_detail/{number}` | Tap a row.                                                      |
| `route=search`               | Tap Search icon in top bar.                                     |
| Calls filter sheet (modal)   | Tap Filter icon in top bar.                                     |
| Tag picker bottom sheet      | Bulk action "Tag", or row-3-dot "Tag".                          |
| Share intent (system)        | Bulk action "Export" → render then `ACTION_SEND`.               |
| Confirmation dialog (delete) | Bulk action "Delete".                                           |
| Inquiries tab                | Bulk action "Save" (auto-saves selected unsaved → flips later). |
| Pinned section "Hide" tap    | Updates settings; section unmounts.                             |

## 18.4 — Required inputs (data)

ViewModel: `CallsViewModel @HiltViewModel constructor(callRepo, tagRepo, contactRepo, settings, syncProgressBus, updateRepo, scheduler: SyncScheduler)`.

State:

```kotlin
data class CallsUiState(
    val filter: CallFilter = CallFilter(),
    val viewMode: ViewMode = ViewMode.Flat,           // Flat | GroupedByNumber
    val pinnedSectionVisible: Boolean = true,
    val pinnedUnsaved: List<CallRow> = emptyList(),
    val flatCalls: List<CallSection> = emptyList(),   // sticky-date sectioned
    val groupedByNumber: List<NumberGroup> = emptyList(),
    val totalMatches: Int = 0,
    val tagsById: Map<Long, Tag> = emptyMap(),
    val savedContactNumbers: Set<String> = emptySet(),
    val bulkMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isRefreshing: Boolean = false,
    val updateBanner: UpdateBannerState = UpdateBannerState.Hidden,
    val empty: EmptyKind = EmptyKind.None,             // None | NoCalls | NoMatches | NoPermission
    val loading: Boolean = true
)
```

Sources:

| Field                  | Source                                                                 | Default      |
| ---------------------- | ---------------------------------------------------------------------- | ------------ |
| `filter`               | local; persisted to `SavedStateHandle` for process death               | empty filter |
| `viewMode`             | `settings.observe { displayGroupedByNumber }` mapped                   | Flat         |
| `pinnedSectionVisible` | `settings.observe { displayShowUnsavedPinned }`                        | true         |
| `pinnedUnsaved`        | `callRepo.observeUnsavedLast7Days().take(20)` mapped to CallRow        | empty        |
| `flatCalls`            | `callRepo.observeFiltered(filter)` → groupBy date bucket → CallSection | empty        |
| `groupedByNumber`      | `callRepo.observeFilteredGrouped(filter)` from a DAO view              | empty        |
| `tagsById`             | `tagRepo.observeAll().associateBy { it.id }`                           | empty        |
| `savedContactNumbers`  | `contactRepo.observeSavedNormalizedNumbers().toSet()`                  | empty        |
| `bulkMode`             | local                                                                  | false        |
| `selectedIds`          | local                                                                  | empty        |
| `isRefreshing`         | combine SwipeRefresh state ⊕ syncProgressBus                           | false        |
| `updateBanner`         | `combine(updateRepo.state, settings.observeUpdateSkipped())`           | Hidden       |
| `empty`                | derived (size + permission + filter)                                   | None         |
| `loading`              | first emission                                                         | true         |

`CallFilter`:

```kotlin
data class CallFilter(
    val callTypes: Set<CallType> = emptySet(),
    val dateRange: DateRangePreset = DateRangePreset.None,
    val customRange: ClosedRange<Long>? = null,
    val durationBucket: Set<DurationBucket> = emptySet(),
    val sim: Set<SimSlot> = emptySet(),
    val contactStatus: Set<ContactStatus> = emptySet(),
    val tagIds: Set<Long> = emptySet(),
    val tagMode: TagMode = TagMode.AnyOf,
    val bookmarkedOnly: Boolean = false,
    val hasNotes: TriState = TriState.Any,
    val hasFollowUp: TriState = TriState.Any,
    val leadScoreRange: IntRange? = null
)
```

`CallRow`:

```kotlin
data class CallRow(
    val id: Long,
    val normalizedNumber: String,
    val displayName: String,           // contact name OR formatted number
    val initial: Char,
    val accentSeed: Int,
    val type: CallType,
    val startedAtMs: Long,
    val durationSec: Int,
    val sim: SimSlot?,
    val tagIds: List<Long>,
    val visibleTags: List<Tag>,        // first 3 resolved
    val overflowTagsCount: Int,
    val isBookmarked: Boolean,
    val leadScore: Int,
    val isContactSaved: Boolean,
    val isAutoSaved: Boolean
)
```

`CallSection`:

```kotlin
data class CallSection(
    val header: DateHeader,            // Today / Yesterday / Mon / Tue / yyyy-MM-dd
    val rows: List<CallRow>
)
```

## 18.5 — Required inputs (user)

| Trigger                               | Behavior                                             | State change                          |
| ------------------------------------- | ---------------------------------------------------- | ------------------------------------- |
| Tap row                               | `rootNav.navigate("call_detail/$normalizedNumber")`. | None.                                 |
| Long-press row                        | Enter bulk mode; select that row.                    | `bulkMode = true; selectedIds += id`. |
| Tap row in bulk mode                  | Toggle selection.                                    | `selectedIds ± id`.                   |
| Swipe row right                       | Toggle bookmark (default).                           | row.isBookmarked flips.               |
| Swipe row left                        | Archive (default; configurable to Delete).           | row hidden until next sync.           |
| Tap Filter icon                       | Open `CallsFilterSheet` (modal).                     | None.                                 |
| Tap Search icon                       | `rootNav.navigate("search")`.                        | None.                                 |
| Tap view-mode toggle                  | Flip between Flat ↔ Grouped.                         | `viewMode` toggled and persisted.     |
| Tap chip "Today" (active filter)      | Remove it from the filter.                           | `filter` updated; query re-runs.      |
| Tap "Clear filters" (empty match CTA) | Reset filter to empty.                               | `filter = CallFilter()`.              |
| Tap pinned-section header             | Collapse/expand.                                     | local `pinnedExpanded` flips.         |
| Tap pinned-section X                  | Hide section permanently (settings).                 | setting updated; section unmounts.    |
| Pull to refresh                       | `scheduler.triggerOnce()`.                           | `isRefreshing = true`.                |
| Tap bulk: Tag                         | Open tag picker bottom sheet for `selectedIds`.      | None at tab.                          |
| Tap bulk: Bookmark                    | `callRepo.bulkSetBookmarked(selectedIds, true)`.     | rows update.                          |
| Tap bulk: Save                        | `contactRepo.autoSaveAll(selectedIds)`.              | inquiries badge increments.           |
| Tap bulk: Export                      | Open Quick Export sheet pre-scoped to selection.     | None at tab.                          |
| Tap bulk: Delete                      | Confirm dialog → `callRepo.bulkDelete(selectedIds)`. | rows removed.                         |
| Tap bulk: Done                        | Exit bulk mode.                                      | `bulkMode = false; selectedIds = ∅`.  |
| System back in bulk mode              | Same as Done.                                        | Same.                                 |

## 18.6 — Mandatory display elements

Container: `StandardPage(title="Calls", description="Your call log", emoji="📞", backgroundColor=TabBgCalls, headerGradient=HeaderGradCalls)`.

> Implementation note: when bulk mode is active, the page swaps to a
> `NeoScaffold` whose `bottomBar` slot hosts the `BulkActionBar`. This
> avoids stacking the bar on top of the bottom nav (the bottom nav is
> hidden while bulk mode is active). The page background remains
> `TabBgCalls`.

### 18.6.1 — Top-bar actions (Calls-specific)

The scaffold's top-bar trailing slot is augmented for this tab via
`LocalTopBarActions.current.set { ... }`. The full action set:

1. Search icon — routes to search.
2. Filter icon — opens filter sheet. Badge with active-filter count.
3. View-mode toggle — `Icons.Outlined.ViewAgenda` ↔ `Icons.Outlined.ViewList`.
4. Overflow ⋮ (already present from scaffold).

### 18.6.2 — Pinned-unsaved section (collapsible)

Visible iff `pinnedSectionVisible == true` AND `pinnedUnsaved.isNotEmpty()`.

- Container: `NeoCard(border = BorderAccent)`. Margin top 8.dp.
- Header row:
  - Title "Unsaved inquiries — last 7 days" (`titleSmall` bold).
  - Count badge `({n})`.
  - Chevron (`KeyboardArrowDown` when expanded; rotates 180° when collapsed).
  - Trailing X (`Icons.Outlined.Close`, 20.dp) → permanently hides.
- Body (when expanded): a vertically-stacked sub-list of `CallRow`s,
  capped at 7. If `pinnedUnsaved.size > 7`, show "Show all (N)" link
  → routes to Inquiries.
- Animations: header chevron rotates with `animateFloatAsState`. Body
  collapses with `AnimatedVisibility(slideInVertically + fadeIn)`.

### 18.6.3 — Active filter chips row

Visible iff `filter` has any non-default value. Renders just below the
pinned section, above the main list.

- Horizontal `LazyRow` with 8.dp gaps.
- Each chip: `NeoChip(label, trailingIcon = Close)` → tap removes the
  filter facet, X removes only that facet.
- Chip examples: "Today", "Missed", "Has notes", "Tag: Inquiry", "Lead 70+".
- Trailing chip "Clear all" → resets filter.

### 18.6.4 — Sticky date headers

Headers: `Today`, `Yesterday`, `Monday`, `Tuesday`, ... weekday names for
the most recent 6 days, then the literal date `EEE, MMM d` for older.

- `LazyColumn` `stickyHeader { ... }` (Compose 1.7+).
- Header style: `labelMedium` bold, color `OnSurfaceMuted`, background
  `TabBgCalls.copy(alpha=0.96f)`. Height 32.dp. Padding 16.dp horizontal.

### 18.6.5 — CallRow

Row layout (from leading to trailing):

```
[ NeoAvatar 40dp ][ pad 12dp ][ name + meta + tags ][ flex spacer ][ trailing icons ]
```

- `NeoAvatar(initial, accentSeed)`: deterministic tint from
  `accentSeed = name.hashCode().rem(8).absoluteValue`.
- Center column:
  - Line 1: `displayName` (`bodyLarge` bold, ellipsize 1 line).
  - Line 2: type icon (12.sp, colored: incoming green, outgoing teal,
    missed red) + " · " + `formatRelative(startedAtMs)` + " · " + `formatDuration(durationSec)`.
  - Line 3 (only if `tagIds.isNotEmpty()`): tag pill row, `FlowRow`,
    `maxLines = 1`. First 3 pills shown; if `overflowTagsCount > 0`,
    append `+N` chip.
- Trailing column:
  - Bookmark star (`Icons.Filled.Star` if `isBookmarked`, otherwise
    `Icons.Outlined.StarBorder`). Color `Accent.Yellow` when filled.
  - `LeadScoreBadge(leadScore)`: pill 32.dp wide showing the score with
    color → cold gray (<30), warm amber (30–70), hot red (>70).

Row height: 80.dp (with tag pills) or 64.dp (without). Min touch target
48.dp.

Selected-state visual (bulk mode):

- Convex NeoSurface flips to inset/concave with `BrandTeal.copy(alpha=0.10f)` overlay.
- Leading avatar replaced with `Icons.Filled.CheckCircle` (24.dp, BrandTeal).

### 18.6.6 — BulkActionBar (bottom)

Visible iff `bulkMode == true`. Replaces the bottom nav.

- Height 64.dp. NeoSurface convex.
- Contents (left to right): "(N) selected" label, then six icon buttons
  with labels:
  1. Tag (`Icons.Outlined.LocalOffer`)
  2. Bookmark (`Icons.Outlined.StarOutline`)
  3. Save (`Icons.Outlined.PersonAdd`)
  4. Export (`Icons.Outlined.IosShare`)
  5. Delete (`Icons.Outlined.Delete`, color destructive)
  6. Done (`Icons.Outlined.Check`)

### 18.6.7 — Empty placements

- No calls at all: full-screen empty state.
- Filter no-match: empty state with "Clear filters" CTA.
- Permission denied: full-screen permission rationale.

(See §18.8.)

## 18.7 — Optional display elements

| Element                   | Condition                                             |
| ------------------------- | ----------------------------------------------------- |
| Update banner (in-tab)    | `updateBanner != Hidden` AND user is on Calls tab.    |
| Pinned section            | `pinnedSectionVisible && pinnedUnsaved.isNotEmpty()`. |
| Active filter chips row   | Filter is non-default.                                |
| SIM badge on row          | Device has dual SIM AND `sim != null`.                |
| Tag pills row             | `tagIds.isNotEmpty()`.                                |
| LeadScoreBadge            | `leadScore > 0` (always shown by default).            |
| Pull-to-refresh indicator | Pulldown gesture.                                     |
| Sticky date headers       | Always present in flat mode; absent in grouped mode.  |
| Group expansion chevron   | Only in grouped mode.                                 |

## 18.8 — Empty state

Three flavors, mutually exclusive:

### 18.8.1 — No calls

```
        📭
   No calls yet
Pull down to sync your call log
   [ Sync now ]
```

### 18.8.2 — Filter no-match

```
        🔎
No calls match these filters
   [ Clear filters ]
```

### 18.8.3 — Permission denied

```
        🔒
Call log access is needed
We use this to read your incoming/outgoing calls.
Nothing leaves your device.
   [ Grant permission ]
```

Each is a `Column(Modifier.fillMaxSize().padding(24.dp), Center, Center)`.

## 18.9 — Loading state

- Initial composition: render 6 skeleton rows shaped like a CallRow
  (avatar circle + 2 lines + trailing pill).
- Shimmer at 60 fps for at most 800 ms, then `CircularProgressIndicator`
  if still empty.
- The pinned section shows its own 2-row skeleton if `pinnedUnsaved` is
  loading.

## 18.10 — Error state

If the filtered query throws (e.g., FTS table missing after an aborted
migration):

- Snackbar "Couldn't load calls. Tap to retry." (action button "Retry").
- Body renders the no-calls empty state with the title swapped to
  "Couldn't load calls" and CTA "Retry".
- Timber error log; never crash.

## 18.11 — Edge cases

1. **0 calls** — empty state §18.8.1; pinned section absent.
2. **Exactly 1 call** — single row with sticky header "Today"; no pinned section unless that call is unsaved.
3. **10,000 calls** — `LazyColumn` virtualizes; ensure stable keys (`key = { it.id }`); avoid `derivedStateOf` per row.
4. **All calls unsaved** — pinned section shows top 7 unsaved; main list shows all.
5. **All calls saved** — pinned section absent.
6. **Single-SIM device** — hide SIM badge entirely (`telephonyAdapter.activeSimCount == 1`).
7. **Private number** — `displayName = "Private number"`; avatar `?`.
8. **Archived calls** — hidden by default; visible only when filter has `Archived = true`.
9. **Mid-sync** — top-line bar visible; rows still render from cache; new rows insert at top with `animateItem()`.
10. **Bulk mode + tab switch attempt** — bottom nav is hidden in bulk mode, but if user uses gestures: leaving Calls auto-exits bulk mode and clears selection.
11. **Bulk mode + back press** — exits bulk mode (does not pop the page).
12. **Filter sheet open + back press** — closes sheet only.
13. **Swipe right on a saved-contact row in the pinned section** — that
    row no longer qualifies as unsaved; it disappears from the pinned
    section but remains in the main list.

## 18.12 — Copy table

| String id                         | English                                                                                            |
| --------------------------------- | -------------------------------------------------------------------------------------------------- |
| `calls_title`                     | Calls                                                                                              |
| `calls_description`               | Your call log                                                                                      |
| `calls_top_search_a11y`           | Search                                                                                             |
| `calls_top_filter_a11y`           | Filter                                                                                             |
| `calls_top_view_mode_flat`        | Switch to flat list                                                                                |
| `calls_top_view_mode_grouped`     | Switch to grouped by number                                                                        |
| `calls_pinned_title`              | Unsaved inquiries — last 7 days                                                                    |
| `calls_pinned_count`              | (%1$d)                                                                                             |
| `calls_pinned_show_all`           | Show all (%1$d)                                                                                    |
| `calls_pinned_hide_a11y`          | Hide pinned section                                                                                |
| `calls_active_filter_clear`       | Clear all                                                                                          |
| `calls_header_today`              | Today                                                                                              |
| `calls_header_yesterday`          | Yesterday                                                                                          |
| `calls_header_weekday_format`     | EEEE                                                                                               |
| `calls_header_older_format`       | EEE, MMM d                                                                                         |
| `calls_row_calls_count`           | %1$d calls                                                                                         |
| `calls_row_overflow_tags`         | +%1$d                                                                                              |
| `calls_row_bookmark_a11y`         | Bookmarked                                                                                         |
| `calls_row_unbookmark_a11y`       | Not bookmarked                                                                                     |
| `calls_lead_cold`                 | Cold                                                                                               |
| `calls_lead_warm`                 | Warm                                                                                               |
| `calls_lead_hot`                  | Hot                                                                                                |
| `calls_swipe_right_bookmark`      | Bookmark                                                                                           |
| `calls_swipe_left_archive`        | Archive                                                                                            |
| `calls_bulk_count`                | %1$d selected                                                                                      |
| `calls_bulk_tag`                  | Tag                                                                                                |
| `calls_bulk_bookmark`             | Bookmark                                                                                           |
| `calls_bulk_save`                 | Save                                                                                               |
| `calls_bulk_export`               | Export                                                                                             |
| `calls_bulk_delete`               | Delete                                                                                             |
| `calls_bulk_delete_confirm_title` | Delete %1$d calls?                                                                                 |
| `calls_bulk_delete_confirm_body`  | This removes the entries from your callNest database. Your phone's system call log isn't affected. |
| `calls_bulk_delete_cta`           | Delete                                                                                             |
| `calls_bulk_done`                 | Done                                                                                               |
| `calls_empty_no_calls_title`      | No calls yet                                                                                       |
| `calls_empty_no_calls_body`       | Pull down to sync your call log.                                                                   |
| `calls_empty_no_calls_cta`        | Sync now                                                                                           |
| `calls_empty_no_match_title`      | No calls match these filters                                                                       |
| `calls_empty_no_match_cta`        | Clear filters                                                                                      |
| `calls_empty_perm_title`          | Call log access is needed                                                                          |
| `calls_empty_perm_body`           | We use this to read your incoming and outgoing calls. Nothing leaves your device.                  |
| `calls_empty_perm_cta`            | Grant permission                                                                                   |
| `calls_error_snack`               | Couldn't load calls. Tap to retry.                                                                 |
| `calls_error_retry`               | Retry                                                                                              |

## 18.13 — ASCII wireframes

### Default (with pinned + active filter)

```
┌──────────────────────────────────────┐
│ ▒ callNest    🔍  ⛃²  ⬚  ⋮          │  filter badge=2
├──────────────────────────────────────┤
│ 📞 Calls                              │
│    Your call log                      │
│ ──────────────────────────────────── │
│ ┌──────────────────────────────────┐ │
│ │ Unsaved inquiries — last 7 days  │ │
│ │ (5)                       ▾   ✕  │ │
│ │ ◉ +91 98765 43210 ↘ 2m  💬     ★ │ │
│ │ ◉ +91 99887 76655 ↘ 7m         ★ │ │
│ │ … Show all (5)                   │ │
│ └──────────────────────────────────┘ │
│ [ Today × ][ Missed × ][ Clear all ] │
│                                      │
│ ─── Today ─────────────────────────  │
│ ◉ Ramesh Mobile     ↘ 14:32 · 2:14 ★ │
│ ◉ +91 90909 90909   ✗ 14:01 · 0:00 ★ │
│ ─── Yesterday ─────────────────────  │
│ ◉ Anita Wholesale   ↗ 17:50 · 1:02   │
├──────────────────────────────────────┤
│   🏠     📞     📥³    ⋯              │
└──────────────────────────────────────┘
```

### Pinned collapsed

```
┌──────────────────────────────────────┐
│ ┌──────────────────────────────────┐ │
│ │ Unsaved inquiries — last 7 days  │ │
│ │ (5)                       ▸   ✕  │ │
│ └──────────────────────────────────┘ │
```

### Bulk mode

```
┌──────────────────────────────────────┐
│ ▒ callNest    🔍  ⛃   ⬚  ⋮          │
├──────────────────────────────────────┤
│ ◉ Ramesh Mobile     ↘ 14:32 · 2:14   │  unselected
│ ✔ +91 90909 90909   ✗ 14:01 · 0:00   │  selected
│ ✔ Anita Wholesale   ↗ 17:50 · 1:02   │  selected
├──────────────────────────────────────┤
│ 2 selected                            │
│ [🏷] [★] [👤+] [⤴] [🗑] [✓]           │
└──────────────────────────────────────┘
```

### No-match empty

```
┌──────────────────────────────────────┐
│ [ Today × ][ Missed × ][ Tag: VIP ×]│
│                                      │
│             🔎                       │
│   No calls match these filters       │
│       [ Clear filters ]              │
└──────────────────────────────────────┘
```

### Grouped-by-number

```
┌──────────────────────────────────────┐
│ ◉ Ramesh Mobile     latest 14:32  3 ▸│
│   ↳ 3 calls · last 14:32 · total 8m  │
│ ◉ +91 90909 90909   latest 14:01  2 ▸│
│   ↳ 2 calls · last 14:01 · total 0m  │
└──────────────────────────────────────┘
```

## 18.14 — Accessibility

- Each row's `contentDescription`:
  "Call from Ramesh Mobile, incoming, today at 2:32 PM, 2 minutes 14 seconds, bookmarked, lead score 78 hot. Double-tap to open. Long-press for bulk select."
- The bookmark trailing icon is exposed as a toggle action via
  `customAction(label = "Bookmark") { ... }` so TalkBack users can
  toggle without swiping.
- Sticky headers announce "Date header, Today, in list".
- Bulk-mode selection state is exposed via `selected = true/false` with
  `role = Role.Checkbox`.
- BulkActionBar buttons each have `contentDescription` and `onClickLabel`.
- Filter icon includes the active count: "Filter, 2 active".
- All swipe targets have alternative `customAction`s so non-gesture users
  can perform the action via TalkBack.
- Color is never the sole signal: bookmark uses both color and icon
  shape; lead-score badge uses both color and the literal score number.
- Touch targets ≥ 48.dp; row height ≥ 64.dp accommodates this.

## 18.15 — Performance budget

- First skeleton paint: < 80 ms after tab switch.
- First row visible: < 250 ms on a 10k-row DB (Room with composite index
  on `(started_at DESC, id)`).
- Scrolling 60 fps maintained for a 10k-row list. Achieved by:
  - `LazyColumn` with stable keys.
  - Pre-resolved tag pills (no per-row DB query).
  - `derivedStateOf` for the active-filter chip set.
  - Avoiding `Modifier.shadow` per row (use `NeoSurface` which paints to a `RenderEffect` once).
- Memory: < 6 MB for 10k CallRow objects (estimate: ~600 bytes/row).
- Eager render budget: at most 1 screen-worth (≈ 12 rows) materialized
  on first composition; the rest stream in via `LazyColumn`.
- Filter recompute cost: O(N) on N matching rows; queries below 5k rows
  complete < 30 ms with proper indices.

---

# §19 — Inquiries tab

The Inquiries tab is the management surface for auto-saved contacts —
unsaved callers that callNest automatically promoted into a system
contact group using a configurable name pattern (e.g. `callNest-s1 +91…`).
This tab surfaces those auto-saved entries, lets the user search and
bulk-convert them into "real" contacts, and observes the auto-saved/manual
flip when the user renames an entry in the system Contacts app.

## 19.1 — Purpose

Be the single management surface for inquiries. Make it trivial to:

- See every unsaved-but-auto-saved caller in one list, sorted by recency.
- Search by partial number or pattern label.
- Convert one or many to real contacts (rename → triggers auto-flip).
- Long-press to enter bulk mode for batch conversion.

After Phase I.6, this tab replaces the older "My Contacts ↔ Inquiries"
two-tab section. My Contacts is no longer present in the bottom nav;
real contacts live in the system Contacts app and are referenced by
callNest transparently when the row's number matches.

## 19.2 — Entry points

| From                           | Behavior                                                       |
| ------------------------------ | -------------------------------------------------------------- |
| Bottom nav: tap "Inquiries"    | Direct.                                                        |
| Home → "Unsaved" stat tile     | Navigates here with no preset.                                 |
| Home → "Save all" CTA          | Navigates here, no preset, snackbar "Tap a row to convert".    |
| Calls bulk action: Save        | After save action completes, snackbar with "View" → goes here. |
| Notification: auto-saved batch | Routes here.                                                   |
| Process restart                | Restored if active.                                            |
| Re-tap Inquiries               | Scroll to top.                                                 |

## 19.3 — Exit points

| To                           | Trigger                                     |
| ---------------------------- | ------------------------------------------- |
| `route=call_detail/{number}` | Tap a row.                                  |
| Convert dialog               | Tap "Convert" inline, or bulk Convert.      |
| System Contacts app (intent) | "Convert" → ACTION_EDIT on the contact URI. |
| Tag picker bottom sheet      | Bulk action "Tag".                          |

## 19.4 — Required inputs (data)

ViewModel: `InquiriesViewModel @HiltViewModel constructor(contactRepo, callRepo, settings)`.

State:

```kotlin
data class InquiriesUiState(
    val query: String = "",
    val items: List<InquiryRow> = emptyList(),
    val totalCount: Int = 0,
    val bulkMode: Boolean = false,
    val selectedNumbers: Set<String> = emptySet(),
    val loading: Boolean = true,
    val empty: Boolean = false
)

data class InquiryRow(
    val normalizedNumber: String,
    val displayLabel: String,           // "callNest-s1 +91…"
    val initial: Char,
    val accentSeed: Int,
    val totalCalls: Int,
    val lastCallAtMs: Long,
    val firstSeenAtMs: Long
)
```

Sources:

- `items` ← `combine(contactRepo.observeAutoSaved(), query) { all, q ->
 if (q.isBlank()) all else all.filter { it.matches(q) } }`.
- `totalCount` ← `items.size`.
- `empty` ← `items.isEmpty() && !loading`.

## 19.5 — Required inputs (user)

| Trigger                  | Behavior                                                                   | State change                   |
| ------------------------ | -------------------------------------------------------------------------- | ------------------------------ |
| Type in search bar       | `query` updates with 250 ms debounce.                                      | `query` updates; list filters. |
| Tap row                  | `rootNav.navigate("call_detail/$normalizedNumber")`.                       | None.                          |
| Tap "Convert" inline     | Open Convert dialog with prefilled name suggestion.                        | None.                          |
| Long-press row           | Enter bulk mode; select that row.                                          | `bulkMode = true`.             |
| Tap row in bulk mode     | Toggle selection.                                                          | `selectedNumbers ± n`.         |
| Bulk action: Convert all | Open Convert sheet for many; loops through ACTION_EDIT.                    | None on success.               |
| Bulk action: Bulk save   | **Disabled** with tooltip "Already saved".                                 | None.                          |
| Bulk action: Tag         | Open tag picker.                                                           | None.                          |
| Bulk action: Done        | Exit bulk mode.                                                            | `bulkMode = false`.            |
| Pull to refresh          | `contactRepo.reconcileAutoSaved()` (re-runs DetectAutoSavedRenameUseCase). | rows may flip out.             |

## 19.6 — Mandatory display elements

Container: `StandardPage(title="Inquiries", description="Auto-saved callers waiting for a name", emoji="📥", backgroundColor=TabBgInquiries, headerGradient=HeaderGradInquiries)`.

### 19.6.1 — Search bar

- Sticky at the top of the list (below the header). 48.dp height.
- Leading icon `Icons.Outlined.Search`.
- Placeholder: "Search by number or label".
- Trailing X visible when query is non-empty → clears query.
- IME action `Search`.

### 19.6.2 — Inquiry rows (LazyColumn)

Each row layout:

```
[ NeoAvatar (auto-saved tint) ][ name + meta ][ Convert button ]
```

- `NeoAvatar` uses a fixed `Accent.Inquiry` tint (deterministic across all auto-saved rows so they read as a cohort), with the initial letter `?` if the displayLabel begins with "+91".
- Center column:
  - Line 1: `displayLabel` (`bodyLarge` bold, ellipsize).
  - Line 2: `formatRelative(lastCallAtMs)` + " · " + `"$totalCalls calls"` (`bodySmall`, muted).
- Trailing: `NeoButton(label="Convert", small=true)`.

### 19.6.3 — Bulk batch toolbar (when bulk mode)

- Same NeoSurface bar as Calls' BulkActionBar.
- Buttons: Tag · Convert all · Bulk save (disabled, with tooltip "Already saved") · Done.

### 19.6.4 — Empty state

```
        📥
   No inquiries yet
New unsaved callers will appear here automatically.
```

## 19.7 — Optional display elements

| Element                         | Condition                                                                                   |
| ------------------------------- | ------------------------------------------------------------------------------------------- |
| "Pattern updated" banner        | If user changed the auto-save pattern in settings recently and rows are reconciling.        |
| "Pattern not configured" banner | If `settings.autoSavePrefix` is empty AND there are auto-saved entries from a prior config. |
| Search results count            | Below search bar when query is non-empty.                                                   |
| Loading shimmer                 | During first emission.                                                                      |

## 19.8 — Empty state

See §19.6.4. Two variants:

- **Truly empty** — no auto-saved rows exist. Copy: "No inquiries yet."
- **Filtered to empty** — query did not match. Copy: "No matches for ‘%1$s’." with "Clear search" CTA.

## 19.9 — Loading state

- 5 skeleton rows shaped like inquiry rows.
- 800 ms cap, then spinner.

## 19.10 — Error state

If `contactRepo.observeAutoSaved()` throws (rare; CP query failed):

- Snackbar: "Couldn't read inquiries. Tap to retry."
- Body: empty state with title "Couldn't load inquiries" and CTA "Retry".

## 19.11 — Edge cases

1. **User renamed in system Contacts** — `DetectAutoSavedRenameUseCase`
   flips `isAutoSaved=false` on next sync. On tab visit, the row
   disappears with `animateItem(fadeOut)`. Verify state correctness on
   `LifecycleEventObserver(ON_RESUME)` by re-collecting.
2. **Pattern matcher mismatch** — A contact whose name superficially
   matches the pattern but was actually a manual entry: trust the DB
   `isAutoSaved` flag, not pattern alone (the flag was set at creation).
3. **Group not yet created** — On first auto-save, the contact group is
   created lazily. The row still appears here because the DB row exists
   even before the system contact does. If the system contact is missing,
   "Convert" surfaces an error: "Contact unavailable. Try again after the
   next sync."
4. **User uninstalled callNest group from system** — `reconcileAutoSaved()`
   detects missing system contacts and offers to recreate them via a
   banner.
5. **10k auto-saved entries** — `LazyColumn` virtualizes; search uses
   `Flow<List>` debounce 250 ms; query runs in-memory because the list
   is bounded.
6. **Bulk Convert across many** — Convert opens system Contacts intents
   serially. If the user cancels mid-loop, partial conversions remain;
   show a snackbar "Converted N of M".
7. **Convert succeeds but flip doesn't happen** — re-run reconcile on
   next ON_RESUME.
8. **System Contacts app is disabled** — Convert button shows error
   snackbar "Contacts app is unavailable on this device."
9. **User searches with special chars** — query is sanitized; no regex.
10. **Tab visited during initial sync** — show shimmer; do not block UI.

## 19.12 — Copy table

| String id                        | English                                                  |
| -------------------------------- | -------------------------------------------------------- |
| `inquiries_title`                | Inquiries                                                |
| `inquiries_description`          | Auto-saved callers waiting for a name                    |
| `inquiries_search_placeholder`   | Search by number or label                                |
| `inquiries_search_clear_a11y`    | Clear search                                             |
| `inquiries_row_calls`            | %1$d calls                                               |
| `inquiries_row_convert`          | Convert                                                  |
| `inquiries_bulk_count`           | %1$d selected                                            |
| `inquiries_bulk_tag`             | Tag                                                      |
| `inquiries_bulk_convert_all`     | Convert all                                              |
| `inquiries_bulk_save`            | Bulk save                                                |
| `inquiries_bulk_save_disabled`   | Already saved                                            |
| `inquiries_bulk_done`            | Done                                                     |
| `inquiries_empty_title`          | No inquiries yet                                         |
| `inquiries_empty_body`           | New unsaved callers will appear here automatically.      |
| `inquiries_search_empty_title`   | No matches for ‘%1$s’                                    |
| `inquiries_search_empty_cta`     | Clear search                                             |
| `inquiries_pattern_updated`      | Pattern updated. Reconciling…                            |
| `inquiries_pattern_missing`      | Auto-save pattern is not configured. Set it in Settings. |
| `inquiries_convert_dialog_title` | Convert to a real contact                                |
| `inquiries_convert_dialog_body`  | We'll open Contacts so you can rename %1$s.              |
| `inquiries_convert_cta`          | Open Contacts                                            |
| `inquiries_convert_cancel`       | Cancel                                                   |
| `inquiries_convert_partial`      | Converted %1$d of %2$d                                   |
| `inquiries_error_snack`          | Couldn't read inquiries. Tap to retry.                   |
| `inquiries_error_retry`          | Retry                                                    |

## 19.13 — ASCII wireframe

```
┌──────────────────────────────────────┐
│ ▒ callNest            🔍   ⋮        │
├──────────────────────────────────────┤
│ 📥 Inquiries                          │
│    Auto-saved callers waiting…        │
│ ──────────────────────────────────── │
│ ┌──────────────────────────────────┐ │
│ │ 🔍 Search by number or label  ✕  │ │
│ └──────────────────────────────────┘ │
│ ◉ callNest-s1 +91 98765 43210       │
│   2h ago · 4 calls    [ Convert ]    │
│ ◉ callNest-s1 +91 99887 76655       │
│   1d ago · 1 call     [ Convert ]    │
│ ◉ callNest-s2 +91 90909 90909       │
│   3d ago · 7 calls    [ Convert ]    │
│ …                                    │
├──────────────────────────────────────┤
│   🏠     📞     📥³    ⋯              │
└──────────────────────────────────────┘
```

Bulk mode replaces the bottom nav with the batch toolbar (same pattern
as Calls).

## 19.14 — Accessibility

- Row description: "Inquiry, callNest-s1 plus 91 98765 43210, last call 2 hours ago, 4 calls. Double-tap to open. Long-press for bulk select."
- "Convert" button description: "Convert callNest-s1 plus 91 98765 43210 to a real contact, button. Opens Contacts."
- Search bar uses `imeAction = Search`. TalkBack reads "Search inquiries, edit text".
- Bulk-mode "Bulk save" button is disabled with `Modifier.semantics { stateDescription = "Already saved" ; disabled() }`.
- Sticky search bar maintains 48.dp touch target.

## 19.15 — Performance budget

- First paint: < 100 ms.
- Search debounce: 250 ms; in-memory filter on a list of 10k entries
  completes in < 8 ms.
- LazyColumn with stable keys (`normalizedNumber`).
- No image loading; avatars are vector initials.
- Memory: < 2 MB for 10k inquiry rows.

---

# §20 — More tab

The More tab is the catch-all settings/management menu. It is visually
the simplest tab — three grouped cards of routed rows — but it is the
single most-touched tab for power users because every advanced feature
lives one tap away from here.

## 20.1 — Purpose

Provide a clear, scannable index of every callNest feature that does
not have a primary tab of its own. Group rows into three semantic
buckets (Data, Automation, App), keep iconography consistent, and surface
the App-update affordance prominently when applicable.

## 20.2 — Entry points

| From                        | Behavior                                |
| --------------------------- | --------------------------------------- |
| Bottom nav: tap "More"      | Direct.                                 |
| Home → Quick action: Backup | Routes to Backup directly (skips More). |
| Process restart             | Restored if active.                     |
| Re-tap More                 | Scroll to top.                          |

## 20.3 — Exit points

Each row routes to a specific destination, listed in §20.6.

## 20.4 — Required inputs (data)

ViewModel: `MoreViewModel @HiltViewModel constructor(updateRepo, settings, driveAuthRepo)`.

State:

```kotlin
data class MoreUiState(
    val updateAvailable: Boolean = false,
    val updateVersion: String? = null,
    val driveSignedIn: Boolean = false,
    val autoSaveEnabled: Boolean = false,
    val autoTagRulesCount: Int = 0,
    val tagsCount: Int = 0
)
```

Sources:

- `updateAvailable` ← `updateRepo.state.map { it is UpdateState.Available && !skipped }`.
- `updateVersion` ← latest version string when available.
- `driveSignedIn` ← `driveAuthRepo.observeSignedIn()`.
- Counts come from respective repos for subtitle text.

## 20.5 — Required inputs (user)

| Trigger                            | Behavior                                                 | State change |
| ---------------------------------- | -------------------------------------------------------- | ------------ |
| Tap a row                          | Route to the destination.                                | None at tab. |
| Tap Quick Export row               | Open Quick Export sheet (parent-controlled).             | sheet opens. |
| Tap App updates row when available | Routes to update screen with banner state.               | None.        |
| Pull to refresh                    | No-op (More has nothing to refresh; show subtle haptic). | None.        |

## 20.6 — Mandatory display elements

Container: `StandardPage(title="More", description="Everything else", emoji="⋯", backgroundColor=TabBgMore, headerGradient=HeaderGradMore)`.

The body is a single `Column` with three `NeoCard`s, each containing
`MoreRow`s. A `MoreRow` is:

- Leading: 40.dp NeoSurface concave circle, with a 20.dp `Icon` tinted
  per row.
- Center: `Column`:
  - Title (`bodyLarge`).
  - Optional subtitle (`bodySmall`, muted) — only present for select
    rows (e.g. "Last backup: 3h ago").
- Trailing: `KeyboardArrowRight` chevron (20.dp, muted).

Row touch target: 56.dp height.

### 20.6.1 — The 12 rows

| #   | Group      | Emoji | Title              | Subtitle (when present)                 | Icon tint        | Destination               |
| --- | ---------- | ----- | ------------------ | --------------------------------------- | ---------------- | ------------------------- |
| 1   | Data       | 📤    | Export             | CSV · Excel · PDF                       | `Accent.Blue`    | `route=export`            |
| 2   | Data       | ⚡    | Quick Export       | Last 7 days · Excel                     | `Accent.Teal`    | `onQuickExport()` (sheet) |
| 3   | Data       | 💾    | Backup & restore   | "Last backup: %1$s" or "Not configured" | `Accent.Green`   | `route=backup`            |
| 4   | Data       | 🏷    | Tags               | "%1$d tags"                             | `Accent.Purple`  | `route=tag_manager`       |
| 5   | Automation | 🤖    | Auto-tag rules     | "%1$d rules"                            | `Accent.Indigo`  | `route=auto_tag_rules`    |
| 6   | Automation | 🎯    | Lead scoring       | —                                       | `Accent.Red`     | `route=lead_scoring`      |
| 7   | Automation | ⚡    | Real-time features | "Bubble · Post-call popup"              | `Accent.Yellow`  | `route=realtime_features` |
| 8   | Automation | 📥    | Auto-save          | "On" / "Off"                            | `Accent.Inquiry` | `route=auto_save`         |
| 9   | App        | 📊    | Stats              | —                                       | `Accent.Cyan`    | `route=stats`             |
| 10  | App        | 🚀    | App updates        | "Update %1$s available" or "Up to date" | `Accent.Warning` | `route=app_updates`       |
| 11  | App        | 📚    | Help & docs        | —                                       | `Accent.Slate`   | `route=help_docs`         |
| 12  | App        | ⚙️    | Settings           | —                                       | `Accent.Gray`    | `route=settings`          |

Each `NeoCard` group has:

- Header label (small caps, muted): `Data`, `Automation`, `App`.
- Vertical list of rows with 1.dp dividers (NeoDivider).

## 20.7 — Optional display elements

| Element                          | Condition                                                                                            |
| -------------------------------- | ---------------------------------------------------------------------------------------------------- |
| "Update %1$s available" subtitle | `updateAvailable && updateVersion != null`. Renders as small accent pill rather than muted subtitle. |
| Sign-out CTA in overflow         | Only if `driveSignedIn`.                                                                             |
| Backup last-run subtitle         | Present iff `lastBackupAt != null`.                                                                  |
| Auto-save subtitle "On"/"Off"    | Always present (state).                                                                              |
| Tags count subtitle              | Always present.                                                                                      |

## 20.8 — Empty state

The More tab is never empty — all 12 rows always render.

## 20.9 — Loading state

The More tab does not show a global loader. Subtitles that need data
("Tags: %d tags", "Last backup: %s") render with a tiny shimmer until
the underlying flow has emitted (≤ 200 ms). Rows are tappable
immediately.

## 20.10 — Error state

If `updateRepo.state` errors, the App-update row falls back to "Up to
date" with a `Timber.w` log. No user-facing error.

## 20.11 — Edge cases

1. **Drive not signed in** — Sign-out menu item in top-bar overflow is
   hidden. Backup row remains tappable; the Backup screen handles its
   own onboarding.
2. **Updates available** — Row 10 shows the update pill; the bottom-nav
   More dot is also lit (handled by scaffold §16).
3. **All 12 rows tappable** — verify with screen reader that none are
   accidentally `enabled = false`.
4. **Scroll behavior with bottom inset** — `LazyColumn` consumes the
   inset; the last row is fully tappable above the bottom nav.
5. **Subtitle text overflow** — subtitles ellipsize at 1 line.
6. **High dynamic type** — row height grows; chevron stays right-aligned.
7. **Dark mode** — Accent tints are theme-aware via `MaterialTheme.colorScheme`.
8. **User rotates device mid-tap** — row tap is debounced 300 ms to
   prevent double navigation.
9. **Tags repo errors** — subtitle gracefully falls back to the title
   only.
10. **Right-to-left** — chevron icon swaps to `KeyboardArrowLeft` via
    `Modifier.scale(scaleX = -1f, scaleY = 1f)` when LayoutDirection is
    Rtl.

## 20.12 — Copy table

| String id                       | English                  | Notes          |
| ------------------------------- | ------------------------ | -------------- |
| `more_title`                    | More                     |                |
| `more_description`              | Everything else          |                |
| `more_group_data`               | Data                     |                |
| `more_group_automation`         | Automation               |                |
| `more_group_app`                | App                      |                |
| `more_row_export`               | Export                   |                |
| `more_row_export_sub`           | CSV · Excel · PDF        |                |
| `more_row_quick_export`         | Quick Export             |                |
| `more_row_quick_export_sub`     | Last 7 days · Excel      |                |
| `more_row_backup`               | Backup & restore         |                |
| `more_row_backup_sub_last`      | Last backup: %1$s ago    | %1$s relative  |
| `more_row_backup_sub_none`      | Not configured           |                |
| `more_row_tags`                 | Tags                     |                |
| `more_row_tags_sub`             | %1$d tags                |                |
| `more_row_auto_tag`             | Auto-tag rules           |                |
| `more_row_auto_tag_sub`         | %1$d rules               |                |
| `more_row_lead_scoring`         | Lead scoring             |                |
| `more_row_realtime`             | Real-time features       |                |
| `more_row_realtime_sub`         | Bubble · Post-call popup |                |
| `more_row_auto_save`            | Auto-save                |                |
| `more_row_auto_save_on`         | On                       |                |
| `more_row_auto_save_off`        | Off                      |                |
| `more_row_stats`                | Stats                    |                |
| `more_row_app_updates`          | App updates              |                |
| `more_row_app_updates_avail`    | Update %1$s available    | %1$s = version |
| `more_row_app_updates_uptodate` | Up to date               |                |
| `more_row_help`                 | Help & docs              |                |
| `more_row_settings`             | Settings                 |                |

## 20.13 — ASCII wireframe

```
┌──────────────────────────────────────┐
│ ▒ callNest            🔍   ⋮        │
├──────────────────────────────────────┤
│ ⋯ More                               │
│   Everything else                    │
│ ──────────────────────────────────── │
│ DATA                                 │
│ ┌──────────────────────────────────┐ │
│ │ ⓘ📤 Export                     ▸│ │
│ │       CSV · Excel · PDF          │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ⚡ Quick Export                ▸│ │
│ │       Last 7 days · Excel        │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ💾 Backup & restore            ▸│ │
│ │       Last backup: 3h ago        │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ🏷 Tags                        ▸│ │
│ │       12 tags                    │ │
│ └──────────────────────────────────┘ │
│ AUTOMATION                           │
│ ┌──────────────────────────────────┐ │
│ │ ⓘ🤖 Auto-tag rules              ▸│ │
│ │       4 rules                    │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ🎯 Lead scoring                ▸│ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ⚡ Real-time features          ▸│ │
│ │       Bubble · Post-call popup   │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ📥 Auto-save                   ▸│ │
│ │       On                         │ │
│ └──────────────────────────────────┘ │
│ APP                                  │
│ ┌──────────────────────────────────┐ │
│ │ ⓘ📊 Stats                       ▸│ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ🚀 App updates                 ▸│ │
│ │       Update 1.2.0 available     │ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ📚 Help & docs                 ▸│ │
│ │ ──────────────────────────────── │ │
│ │ ⓘ⚙️ Settings                    ▸│ │
│ └──────────────────────────────────┘ │
├──────────────────────────────────────┤
│   🏠     📞     📥³    ⋯•             │
└──────────────────────────────────────┘
```

## 20.14 — Accessibility

- Each row description: "Backup and restore. Last backup 3 hours ago. Button."
- Card group headers expose `headingLevel = 2`.
- Chevrons are decorative (`contentDescription = null`).
- The "Update available" pill on row 10 is announced as "Update one point two point zero available."
- Touch targets ≥ 56.dp.
- Quick Export row's destination is a sheet, not a route — TalkBack
  announces "Quick Export, opens sheet, button" via `onClickLabel`.

## 20.15 — Performance budget

- First paint: < 80 ms.
- Subtitle data loads asynchronously without blocking the row layout.
- No images; icons are vector.
- Memory: trivial (< 200 KB).

---

> End of Part 03.
>
> Next: Part 04 — Call Detail screen (deep target from Calls and Home),
> tag picker bottom sheet, follow-up scheduling, notes journal, and
> share-contact-card flow.
