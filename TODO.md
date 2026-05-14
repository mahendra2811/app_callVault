# callNest — TODO

Outstanding work, ranked. Check items off (`- [x]`) as they ship.

Last refreshed: 2026-05-14.

## P0 — Blockers (won't ship without these)

- [x] First clean build (2026-05-02).
- [x] Generate release keystore + populate `keystore.properties` (2026-05-08).
- [x] Signed `assembleRelease` builds — `CallNest-1.0.0.apk` shipped to `~/Releases/` (2026-05-08).
- [x] Wire `MainScaffold` (5-tab) — later trimmed to 4 tabs: Calls / Insights / Inquiries / More (2026-05-14).
- [x] Real launcher icons + adaptive variants (brand work, 2026-05-07).
- [ ] Host `versions-stable.json` at `https://callnest.pooniya.com/versions-stable.json` (file is already in `callNest-web/public/`, just needs Cloudflare Pages deploy).

## P1 — Functional spec misses

- [x] `ResetAllDataUseCase` extended to all 13 user-data tables (2026-05-14).
- [x] `DailySummaryWorker` real counts in notification (today / missed / unsaved / follow-ups due) (2026-05-14).
- [x] `NeoHelpIcon` plumbed into `StandardPage`; wired into 6 highest-value screens (2026-05-14).
- [x] Onboarding firstSync error path — Skip + Retry both persist `onboardingComplete` (verified 2026-05-14).
- [x] Calls filter UI redesign — quick-filter chip strip above the list (2026-05-14).
- [x] Search redesign — OS-contact live matches alongside FTS results (2026-05-14).
- [x] Per-SIM auto-save filter — include SIM 1 / SIM 2 (2026-05-08).
- [x] In-call popup — WhatsApp + WA Business deep links (2026-05-08).
- [x] Saved/Unsaved badge on every Calls row (2026-05-14).
- [ ] **B1 follow-ups**: 5 of 6 deferred stats charts (SimUtilization, TagDistribution, SavedUnsavedTrend, ConversionFunnel, GeoBars). Each needs a new DAO query + StatsSnapshot field + ComputeStatsUseCase extension. DayOfWeekBars shipped 2026-05-14 using existing heatmap data.
- [ ] **B2**: PDF export — embed chart images via Compose `captureToBitmap`. Blocked on B1 follow-ups since charts must exist first.
- [ ] Material 3 → Neo swap on Update screens (no longer relevant — Update flow deleted 2026-05-14).

## P2 — Quality bar (deferred to v1.1)

- [ ] Unit tests for every ViewModel (~14 missing).
- [ ] DAO instrumentation tests (9 missing).
- [ ] `@Preview` audit — every shipped composable renders.
- [ ] Lint warnings sweep.
- [ ] Performance verification on device (cold start, filter on 10k rows, FTS, sync 5k, APK size).
- [ ] Empty / loading / error state sweep across every screen.
- [ ] Accessibility sweep (TalkBack, 48dp touch targets, AA contrast).

## P3 — Polish

- [x] Real privacy policy text + hosted page at https://callnest.pooniya.com/privacy (2026-05-08).
- [x] Release script `scripts/release.sh` (2026-05-08).
- [x] Sentry crash reporting wired, consent-gated, no PII (2026-05-08).
- [x] GitHub Actions release CI on `v*` tag push (2026-05-08).
- [x] Pipeline + lead-scoring UI stripped; backend tables kept as dead code for a future DB-version migration (2026-05-14).
- [x] In-app update flow fully removed; Check-for-updates opens website (2026-05-14).
- [x] Demo data seeding disabled + auto-clears on launch if previously seeded (2026-05-08).
- [x] Auto-save defaults OFF + "Coming soon" badge; manual Save-now in Inquiries is the only path (2026-05-08).
- [x] Excel exporter switched to SXSSF streaming + MediaStore IS_PENDING dance + per-bucket sheets (2026-05-08).
- [x] In-app self-update dead code removed (UpdateAvailableScreen, UpdateSettingsScreen, UpdateCheckWorker, UpdateChecker, UpdateRepository*, UpdateInstaller, UpdateDownloader, UpdateManifest, UpdateNotifier, UpdateBanner, UpdateSettingsViewModel, UpdateUseCases — 12 files) on 2026-05-14.
- [ ] Real screenshots in `docs/screenshots/` (for store / web).
- [ ] Schema migration to drop the now-unused `pipeline_stage` table + DB version bump.
- [ ] Defensive parsing fuzz tests for update-manifest schema (no longer relevant — flow removed).

## Rules for editing this file

- Add new items under the correct tier.
- Promote/demote tier when reality changes (e.g. P3 → P0 if a release date moves).
- Tick boxes (`- [x]`) when shipped. Don't delete completed items — they become the changelog source for the next release.
- One sentence per item, max. If it needs a paragraph, link to a separate doc instead.
