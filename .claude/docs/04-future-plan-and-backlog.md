# Future Plan And Backlog

No `TODO.md` exists in this workspace right now. This backlog is derived from current docs, known limitations, and visible placeholders.

## P0: Build And Release Confidence

- Run `./gradlew lint assembleDebug` after the user approves network/build work.
- Fix compile, lint, KSP, Room, Hilt, and resource errors until green.
- Generate/commit Room schema JSON files under `app/schemas` if build creates them.
- Add or verify release signing through `keystore.properties`.
- Confirm release minification keeps update, export, backup, and Room serialization paths working.
- Install debug APK on a physical Android phone and walk the manual verification list in `DEVELOPING.md`.

## P1: Navigation And Main Shell

- Finish nested 5-tab shell integration if not already done in the running app.
- Confirm tabs: Calls, My Contacts, Inquiries, Stats, Settings.
- Replace `FilterPresets` placeholder route with a real saved-presets manager.
- Check all overflow/menu routes remain reachable after bottom navigation is introduced.
- Confirm notification deep links route correctly for `update_available` and `daily_summary`.

## P1: Tests

- Add DAO instrumentation tests for the most important queries:
  - call upsert and aggregate recompute
  - FTS search across calls/notes
  - tag cross-ref operations
  - rule score boost idempotency
  - note orphan attachment
- Add use-case tests:
  - `RuleConditionEvaluator`
  - `ComputeLeadScoreUseCase`
  - `FilterQueryBuilder`
  - `SyncCallLogUseCase` with fake readers/repositories
  - `BackupManager` encrypt/restore round trip
  - `UpdateChecker` manifest parsing/version comparison
- Add UI screenshot or Compose tests for:
  - onboarding permission states
  - calls list empty/loading/content/error
  - filter sheet
  - export wizard
  - backup restore confirmation

## P1: Stats Completion

Known deferred charts from v1.0.0:

- SIM utilization bar
- tag distribution
- saved/unsaved trend
- conversion funnel
- geo bars
- day-of-week bars

Also consider:

- PDF chart embedding once Compose canvas capture is implemented.
- Better custom date-range picker.
- More insight rules after stats coverage expands.

## P1: Update Hosting

- Host real `versions-stable.json` and `versions-beta.json`.
- Replace placeholder `https://callvault.app/dl/...` URLs if that domain is not live.
- Verify SHA-256 verification against a real APK.
- Test unknown-sources permission fallback on API 26+.
- Test skipped-version behavior and "clear skipped".

## P1: UX Polish

- Sweep all empty/loading/error states.
- Ensure every user-facing string lives in `strings.xml`.
- Place `NeoHelpIcon` across important screens and route to relevant in-app docs.
- Replace remaining direct Material 3 primitives in update screens with Neo components where appropriate.
- Review all Compose previews and restore missing high-value previews.
- Generate final launcher icons if current drawables are placeholders.

## P2: Export And Backup Polish

- Add custom date range UI in export.
- Embed chart images in PDF export.
- Add backup restore preview before destructive replace.
- Add stronger passphrase UX:
  - confirmation
  - warning for weak passphrase
  - recovery guidance
- Add import compatibility tests for future schema versions.

## P2: Real-Time Reliability

- Test overlay behavior across Xiaomi, Vivo, Oppo, Samsung, OnePlus, Honor, Huawei.
- Confirm service behavior with aggressive battery optimization.
- Add troubleshooting docs for overlay permission and OEM autostart.
- Verify orphan note attachment around edge cases:
  - private numbers
  - duplicate calls close together
  - call log insertion delay longer than 60 seconds

## P2: Data Safety

- Revisit `ResetAllDataUseCase`; `CHANGELOG.md` says it currently wipes notes/search/skipped only while calls/contacts/tags retain.
- Decide whether reset should:
  - clear local CallVault tables only
  - also reset DataStore watermarks
  - never touch system contacts
  - optionally remove auto-saved CallVault contacts
- Document final destructive behavior in in-app docs.

## P2: Performance

- Profile cold start against the app target.
- Test filters/search with 10k+ calls.
- Validate Room indexes for common filters:
  - date
  - normalized number
  - call type
  - bookmarked
  - follow-up date
  - lead score
  - tag joins
- Check export memory use for large histories.
- Check PDF/Excel generation on low-memory devices.

## P3: Product Roadmap

Possible future features, subject to user approval:

- Voice-to-text for notes, explicitly deferred to v2 in existing docs.
- More advanced lead scoring with configurable buckets.
- Rule templates for common small-business workflows.
- Better conversion funnel analytics.
- Optional local-only import from previous backups during onboarding.
- Contact cleanup tools for CallVault-created inquiries.
- More localized copy for Indian business users.

## Do Not Add Without Explicit Approval

- Cloud sync.
- Remote database.
- Analytics.
- Crash reporting SDK.
- Google Play Services.
- Firebase.
- New state-management framework.
- New Gradle module.
- Network feature beyond self-update.

