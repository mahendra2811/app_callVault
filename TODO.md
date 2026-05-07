# callNest — TODO

Outstanding work, ranked. Check items off (`- [x]`) as they ship. Add new items in the right tier.

Last refreshed: 2026-05-01.

## P0 — Blockers (won't ship without these)

- [x] First clean build: `./gradlew assembleDebug` ✅ green 2026-05-02. APK 37 MB at `app/build/outputs/apk/debug/app-debug.apk`. Fixes: added `gradle.properties` (AndroidX flag), `Icons.AutoMirrored.Filled.ArrowBack` import in RealTimeSettingsScreen, `Json` provider in `AppModule`.
- [ ] Generate release keystore + populate `keystore.properties`. Verify `assembleRelease` produces a signed APK.
- [ ] Stand up real `versions-stable.json` and `versions-beta.json` hosts; replace placeholder URLs in `BuildConfig` (`UPDATE_MANIFEST_STABLE_URL`, `UPDATE_MANIFEST_BETA_URL`).
- [ ] Replace launcher icons (currently a vector "C" placeholder) with final artwork at all densities + adaptive variants.
- [ ] Wire `MainScaffold` (5-tab `NeoTabBar`) into `callNestNavHost` as nested graph. Tabs: Home / Calls / My Contacts / Inquiries / More. Default = Calls.

## P1 — Functional spec misses

- [ ] Implement remaining 6 stats charts: SimUtilizationBar, TagDistribution, SavedUnsavedTrend, ConversionFunnel, GeoBars, DayOfWeekBars (§3.16).
- [ ] PDF export — embed chart images via Compose `captureToBitmap` (§3.15).
- [ ] Place `NeoHelpIcon` in every screen's app bar. Map article ids per `08-agent-spawn-templates.md` and the `NeoHelpIcon` callsite list.
- [ ] Swap remaining Material 3 primitives in `UpdateAvailableScreen` / `UpdateSettingsScreen` for `NeoCard` / `NeoButton` / `NeoProgressBar` / `NeoToggle`.
- [ ] Extend `ResetAllDataUseCase` to wipe all 13 user-data tables (currently 4). Add `deleteAll()` to remaining DAOs.
- [ ] `DailySummaryWorker` — replace generic notification with computed counts (today total, missed, unsaved, follow-ups due).
- [ ] Onboarding firstSync error path — verify retry/skip flows on cold deny / partial deny.
- [ ] Manifest hosting verification via Playwright (`/audit` confirms reachable + parseable JSON).

## P2 — Quality bar (spec §0)

- [ ] Unit tests for every ViewModel (currently 0 of ~14). Use `callNest-test-writer`.
- [ ] DAO instrumentation tests for every DAO (currently 0 of 9). Use `callNest-test-writer`.
- [ ] `@Preview` audit — ensure every shipped composable has at least one preview that renders.
- [ ] Lint warnings sweep post-build.
- [ ] Performance verification on a real device:
  - cold start < 1.5s
  - filter on 10k rows < 300ms
  - FTS query < 100ms
  - sync 5k entries < 8s
  - APK size < 25 MB
- [ ] Empty / loading / error state sweep — every screen has all three (`callNest-ui-builder`).
- [ ] Accessibility sweep — TalkBack labels, 48dp touch targets, WCAG AA contrast on lead-score badges.

## P3 — Nice-to-haves before public sideload

- [ ] Real privacy policy text (`assets/docs/15-privacy.md`) and terms.
- [ ] Real screenshots in `docs/screenshots/`.
- [ ] ProGuard / R8 release-mode validation.
- [ ] CI workflow file (`.github/workflows/android.yml`) for `lint + assembleDebug` on push.
- [ ] Defensive parsing fuzz tests for update manifest schema.
- [ ] Remove dead `LazyColumnItemsScopeShim` helper in `StatsScreen.kt` (Sprint 8 leftover; harmless).

## Rules for editing this file

- Add new items under the correct tier.
- Promote/demote tier when reality changes (e.g. P3 → P0 if a release date moves).
- Tick boxes (`- [x]`) when shipped. Don't delete completed items — they become the changelog source for the next release.
- Reference subagents by name (`callNest-android-engineer`, etc.) — `/next` will route based on phrasing.
- One sentence per item, max. If it needs a paragraph, link to a separate doc instead.
