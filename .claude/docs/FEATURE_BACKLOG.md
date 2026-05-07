# callNest — Feature Backlog

Editable, prioritised feature backlog. Generated 2026-05-06 from a focused review of the spec + the call-out list Mahendra provided. Tick the box when an item ships; add a `→ TASK-xxx` reference if you split it into the main `TODO.md`.

Effort scale: **S** ≤ 1 day · **M** 1–3 days · **L** 4+ days. ICE = Impact × Confidence × Ease (1–10 each, 1000 max).

---

## Tier 1 — Quality-of-life wins (ship next)

- [ ] **Tag picker dropdown wired into auto-tag rules** ICE 9·10·9 = **810** · **Effort: S**
      Replace raw `tagId` long-input in `ConditionRow` (TagApplied / TagNotApplied) and `ActionRow` (ApplyTag) with the existing `TagPickerSheet`. Show resolved tag name as a chip after selection. Lands AUDIT HIGH-2.
      Layer: `ui/screen/autotagrules/components/`. No domain or data changes.

- [ ] **Search-by-name on calls + my-contacts** ICE 9·9·7 = **567** · **Effort: M**
      `FTS4` table already covers note + contact name. Extend `SearchScreen` to query both indexes and rank "name match" above "note text match". Add a `NeoSearchBar` to the `MyContactsScreen` top.
      Layer: `data/local/fts/` (verify columns), `ui/screen/search/`, `ui/screen/mycontacts/`.

- [ ] **Follow-up calendar view (week + day toggle)** ICE 8·9·6 = **432** · **Effort: M**
      Group `FollowUpsScreen` by day; sticky day headers; "Today / Tomorrow / This week / Later" sections. Add an icon button toggle to flip between list and a 7-day strip view at top.
      Layer: `ui/screen/followups/`. No domain changes.

- [ ] **WhatsApp deep-link from call detail** ICE 9·10·10 = **900** · **Effort: S**
      Add a "WhatsApp" button next to "Call" / "SMS" in `ActionBar` (call detail). `Intent.ACTION_VIEW` to `https://wa.me/<E.164 number>`. Detect missing app and disable if not installed.
      Layer: `ui/screen/calldetail/sections/ActionBar.kt`. No domain changes.

- [ ] **Voice-note quick capture** ICE 8·7·5 = **280** · **Effort: M**
      Hold-to-record voice note from `NotesJournal`. Save as `m4a` in app-private storage; reference path on `NoteEntity`. Plays back inline with a `NeoSurface` waveform.
      Layer: needs `data/local/entity/NoteEntity` migration (new column `audioPath`), `domain/model/Note.kt`, `data/repository/NoteRepositoryImpl`, `ui/screen/calldetail/sections/NoteEditorDialog.kt` + `NotesJournal.kt`. Asks for `RECORD_AUDIO` permission.

- [ ] **Contact merge from inquiries to my-contacts** ICE 8·8·6 = **384** · **Effort: M**
      In `InquiriesScreen`, long-press on an inquiry row → "Merge into existing contact". Sheet shows fuzzy candidates from `MyContactsScreen`; on confirm, `ContactSaver` updates the system contact and the auto-saved row is folded.
      Layer: `domain/usecase/MergeInquiryIntoContactUseCase.kt` (new), `data/system/ContactSaver`, `ui/screen/inquiries/`.

- [ ] **Bulk add tags from selection** ICE 7·9·8 = **504** · **Effort: S**
      `BulkActionBar` already has a "Tag" button that's a no-op (`/* Sprint 4 */`). Wire it to open `TagPickerSheet` and call `applyTagToCalls(ids, tagId)`.
      Layer: `ui/screen/calls/BulkActionBar.kt` + `CallsViewModel`. No domain changes.

- [ ] **First-export confirm dialog** ICE 7·10·10 = **700** · **Effort: S**
      Once-per-install, before generating the first export, show a `NeoDialog` summarising what will leave the device ("1,247 calls, 420 contact names, 87 notes — saved to a file you choose"). Lands AUDIT S-10.
      Layer: `ui/screen/export/ExportScreen.kt` + a flag in `SettingsDataStore`.

- [ ] **Sign-up + password recovery wired through** ICE 10·10·10 = **1000** · **Effort: S**
      Replace the two empty lambdas in `callNestNavHost.kt:155–156` with a wired `authGraph()` sub-graph. Lands AUDIT CRIT-1.
      Layer: `ui/navigation/callNestNavHost.kt`, `ui/screen/auth/AuthNavGraph.kt`.

---

## Tier 2 — Material UX upgrades (ship within a sprint)

- [ ] **Today / This-week metric strips on Home tab** ICE 7·8·7 = **392** · **Effort: S**
      Today card already shows 4 tiles. Add a horizontal `LazyRow` below it with "This week" / "This month" / "Top tag" tiles. Sourced from existing `StatsRepository`.
      Layer: `ui/screen/home/HomeScreen.kt` + `HomeViewModel`.

- [ ] **Inline filter chips on Calls list** ICE 6·8·9 = **432** · **Effort: S**
      ActiveFiltersRow already exists. Surface 3 quick chips above it: "Missed today", "Unsaved", "Bookmarked". One-tap apply / remove.
      Layer: `ui/screen/calls/CallsScreen.kt`.

- [ ] **Smart filter presets** ICE 7·7·5 = **245** · **Effort: M**
      `Destinations.FilterPresets` exists but routes to a placeholder. Implement: save the current filter as a named preset; pin presets to `CallsFilterSheet`. Backed by a new Room table.
      Layer: new `data/local/entity/FilterPresetEntity`, DAO, `domain/repository/FilterPresetRepository`, replace the placeholder route. Lands AUDIT HIGH-4.

- [ ] **OEM battery whitelisting wizard** ICE 8·6·5 = **240** · **Effort: M**
      Onboarding has an `OemBatteryPage`, but post-onboarding there's no nag. If sync misses for 24h, surface a banner on the Calls tab linking to the OEM-specific battery settings page (Xiaomi / Realme / OnePlus / Samsung detection).
      Layer: `data/system/OemDetector.kt` (new), `ui/screen/calls/components/UpdateBanner.kt` pattern reused.

- [ ] **Push notification on missed inquiry call** ICE 9·7·6 = **378** · **Effort: M**
      Cloud pivot brought FCM. Use the existing `data/push/` to surface a high-priority push: "Missed inquiry from +91xxxxx — tap to call back". Backed by a Supabase Edge Function watching the call-events row.
      Layer: `data/push/`, Supabase Edge Function (out of repo).

- [ ] **Daily summary push (replace local notification)** ICE 6·7·7 = **294** · **Effort: S**
      `DailySummaryWorker` currently posts a local notification. Move to FCM scheduled push so it survives Doze on aggressive OEMs. Keep local fallback.
      Layer: `data/work/DailySummaryWorker.kt`, Supabase scheduled function.

- [ ] **PDF export with chart images** ICE 7·6·5 = **210** · **Effort: M**
      `TODO.md` P1. Re-enable PDF in `FormatStep`, embed chart bitmaps via Compose `captureToBitmap` + iText 8.
      Layer: `data/export/PdfExporter.kt`, `ui/screen/export/steps/FormatStep.kt`.

- [ ] **Stats charts: complete the missing 6** ICE 6·7·5 = **210** · **Effort: L**
      `TODO.md` P1. Add SimUtilizationBar, TagDistribution, SavedUnsavedTrend, ConversionFunnel, GeoBars, DayOfWeekBars. Vico already in stack.
      Layer: `ui/screen/stats/charts/`.

---

## Tier 3 — Defer (good ideas, low urgency)

- [ ] **Multi-language support (Hindi first)** ICE 8·6·3 = **144** · **Effort: L** · Strings + RTL hardening + Devanagari font fallback.
- [ ] **Lead-score formula visualiser** ICE 5·8·5 = **200** · **Effort: M** · Show why a call scored 87 on the badge tap.
- [ ] **Auto-tag rule templates ("Cold lead", "Hot lead", "Spam")** ICE 7·8·7 = **392** · **Effort: M** · Pre-baked rules importable from a JSON catalog.
- [ ] **Call recording stub** (placeholder until India regulatory clarity) ICE 9·3·3 = **81** · **Effort: L**
- [ ] **Multi-device sync via Supabase** ICE 7·5·3 = **105** · **Effort: L** · Cross-device tag/note/follow-up sync.
- [ ] **Export to Google Sheets directly** ICE 6·6·5 = **180** · **Effort: M** · OAuth flow inside the export wizard.
- [ ] **Custom ringtone per tag** ICE 5·8·6 = **240** · **Effort: S** · Tag-bound ringtone using the in-app overlay service.
- [ ] **Per-contact follow-up cadence (auto-schedule next reminder)** ICE 7·6·5 = **210** · **Effort: M**

---

## How to use this file

- Re-rank periodically; ICE > 500 is the cut for the current sprint.
- When an item is split into actionable sub-tasks, move it (with its tag `→ TASK-xxx`) into `TODO.md`.
- When an item ships, leave the line, check the box, append a date.
- New ideas → append to Tier 3 with an ICE estimate; promote later.
