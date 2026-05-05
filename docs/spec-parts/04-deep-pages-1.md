# CallVault APP-SPEC — Part 04: Deep Pages I

> Section: Call detail · Search · Stats · Bookmarks · Follow-ups · My Contacts
> Audience: a UX engineer rebuilding CallVault from scratch.
> Status: locked. Cross-references: Part 02 (Tabs & Home), Part 03 (Library tab),
> Part 05 (Deep pages II), Part 06 (Appendices — Neo* components, CallRow, color
> tokens, formatters).
> Sister files: see `docs/spec-parts/02-tabs-and-home.md`,
> `docs/spec-parts/03-library.md`.

This part covers six "deep" pages — pages reached by drilling into a row from
one of the four bottom tabs. Each page follows the same 15-section template
introduced in Part 02. Where the template repeats verbatim wording, it has been
expanded with the specifics that distinguish that page.

The six pages, numbered to continue the global section counter from Parts 02/03:

| #  | Page         | Route                              | Spec section |
|----|--------------|------------------------------------|--------------|
| 21 | CallDetail   | `callDetail/{normalizedNumber}`    | §21          |
| 22 | Search       | `search` (overlay)                 | §22          |
| 23 | Stats        | `stats`                            | §23          |
| 24 | Bookmarks    | `bookmarks`                        | §24          |
| 25 | FollowUps    | `followUps`                        | §25          |
| 26 | MyContacts   | `myContacts`                       | §26          |

Per-page template (15 subsections, identical to Parts 02 + 03):

1. Purpose
2. Entry points
3. Exit points
4. Required inputs (data) — route args + ViewModel state
5. Required inputs (user) — taps, swipes, gestures
6. Mandatory display elements
7. Optional display elements
8. Empty state
9. Loading state
10. Error state
11. Edge cases (≥5)
12. Copy table
13. ASCII wireframe
14. Accessibility
15. Performance budget

Conventions used throughout:
- `Neo*` = the in-house component library documented in Part 06 Appendix A.
- `StandardPage` = the screen scaffold (top bar + colored tab background +
  header gradient + body slot) documented in Part 06 Appendix B.
- `CallRow` = the canonical call list item documented in Part 06 Appendix C.
- "deterministic color" = HSL hash of normalized number — see Part 06
  Appendix F.
- "phase I.2" = the second hardening pass on detail surfaces; see
  `RELEASE-PLAN.md` and `CHANGELOG.md` v0.18.x.

---

## §21 — CallDetail screen

`com.callvault.app.ui.screen.detail.CallDetailScreen`

### 21.1 Purpose

CallDetail is the most important deep page in the app. It is the *single number
of truth* for a phone number: who they are (name + avatar + saved/unsaved state),
how the user has interacted with them (every call, every tag, every note, every
follow-up), and what the user is going to do next about them (call, message,
WhatsApp, save, block, schedule).

Every other surface in CallVault eventually delegates here. From Calls, the user
taps a row → CallDetail. From Library → CallDetail. From Search → CallDetail.
From the post-call popup "Open" button → CallDetail. From a notification's
"View" action → CallDetail. From an exported PDF link (deep-linked via
`callvault://detail/<num>`) → CallDetail.

Because of that fan-in, CallDetail must answer in a single glance:

1. *Who is this?* Hero card — name, avatar, status pill, lead score.
2. *What can I do right now?* Action bar — call, message, WhatsApp, save, block.
3. *What's the history at a glance?* Stats card — totals + averages + dates.
4. *How have I categorized them?* Tags section.
5. *What did I write down about them?* Notes journal.
6. *What did I promise to do?* Follow-up section.
7. *What's the full timeline?* Call history timeline.
8. *What admin levers do I have?* Manage section.

The page is a vertically scrolled `LazyColumn`; the user reads top-down and
scrolls until they find what they need. Nothing is collapsed by default.

### 21.2 Entry points

| Source                                         | Args passed                       | Notes                    |
|------------------------------------------------|-----------------------------------|--------------------------|
| Calls tab → CallRow tap                        | `normalizedNumber`                | Most common (~70%)       |
| Library tab → ContactRow tap                   | `normalizedNumber`                | ~15%                     |
| Search overlay → result tap                    | `normalizedNumber`                | ~7%                      |
| Bookmarks → row or pinned-bookmark tap         | `normalizedNumber`                |                          |
| FollowUps → row tap                            | `normalizedNumber`                |                          |
| MyContacts → row tap                           | `normalizedNumber`                |                          |
| Post-call popup → "Open" button                | `normalizedNumber`                |                          |
| Floating in-call bubble → expand → tap header  | `normalizedNumber`                |                          |
| Notification ("Missed call from …") → tap      | `normalizedNumber` (PendingIntent)|                          |
| External deep link (`callvault://detail/<n>`)  | `normalizedNumber`                | Phase I.2                |
| Exported PDF link clicked while app open       | `normalizedNumber`                | Same handler as above    |

The route is `callDetail/{normalizedNumber}` where `normalizedNumber` is
URL-encoded E.164 (e.g. `+919876543210` → `%2B919876543210`). The
`NavController` decodes back to the raw E.164 string before passing into the
ViewModel.

For unknown / private numbers, the upstream caller passes an empty string. The
ViewModel detects this and renders a degraded "Private number" hero card; see
edge case 21.11.b.

### 21.3 Exit points

- **Back arrow** → `popBackStack()` to whichever surface invoked it.
- **System back gesture** → same as above.
- **Share contact card** (top app bar trailing icon) → `Intent.ACTION_SEND` with
  vCard text/x-vcard MIME; chooser dialog. Stays on CallDetail after share.
- **Action bar Call** → `Intent.ACTION_DIAL` with `tel:<number>`. Phone app
  becomes foreground.
- **Action bar Message** → `Intent.ACTION_SENDTO` with `smsto:<number>`. SMS app
  becomes foreground.
- **Action bar WhatsApp** → `Intent.ACTION_VIEW` with `https://wa.me/<num>`.
  WhatsApp opens; if not installed, browser opens chooser.
- **Action bar Save** → `ContactsContract.Intents.Insert.ACTION` prefilled with
  number. After Contacts returns, the screen re-fetches and the hero card flips
  from "Unsaved" to "Saved".
- **Action bar Block** → confirmation dialog → if confirmed, calls a stub
  wrapper around `TelecomManager.blockNumber()` (Phase I.2 stub: writes to local
  block list only; deep system block deferred to v1.1).
- **"Save to contacts" CTA** in hero card → same as action bar Save.
- **"Add tag"** → opens `TagPickerSheet` (a `ModalBottomSheet`); on dismiss
  returns to CallDetail.
- **Tag chip ×** → removes tag inline; no navigation.
- **"Add note" / Edit / Delete** → opens `NoteEditorDialog` (full-screen dialog);
  on confirm/dismiss returns to CallDetail.
- **"Set follow-up"** → DatePicker → TimePicker → returns to CallDetail.
- **Follow-up Edit / Cancel / Snooze** → inline mutate; no navigation.
- **Manage → Edit notes** → scrolls to and focuses the notes journal.
- **Manage → Clear all data for this number** → confirmation dialog → on
  confirm, ViewModel issues `ClearNumberDataUseCase` then `popBackStack()`.
- **Manage → Report spam** → applies the predefined `Spam` tag, shows snackbar
  "Reported as spam.", stays on screen.

### 21.4 Required inputs (data)

Route arg: `normalizedNumber: String` (E.164, may be empty for private).

`CallDetailViewModel` state — single `StateFlow<CallDetailUiState>`:

| Field                | Type                              | Source                                         |
|----------------------|-----------------------------------|------------------------------------------------|
| `normalizedNumber`   | `String`                          | savedStateHandle                               |
| `displayName`        | `String?`                         | `contactsRepo.observeName(num)`                |
| `formattedNumber`    | `String`                          | `PhoneFormatter.formatForDisplay(num)`         |
| `geocodedLocation`   | `String?`                         | `callRepo.observeMostRecentGeocoded(num)`      |
| `avatarSeed`         | `String`                          | derived = normalizedNumber                     |
| `status`             | `enum SavedStatus`                | derived: Saved / Unsaved / AutoSaved           |
| `leadScore`          | `Int` 0..100                      | `LeadScoreUseCase.observe(num)`                |
| `leadBucket`         | `enum LeadBucket`                 | derived from `leadScore`                       |
| `stats`              | `NumberStats`                     | `NumberStatsUseCase.observe(num)`              |
| `tags`               | `List<TagApplication>`            | `tagRepo.observeApplied(num)`                  |
| `notes`              | `List<NoteEntry>`                 | `noteRepo.observe(num)` (newest first)         |
| `followUp`           | `FollowUp?`                       | `callRepo.observeFollowUp(num)`                |
| `history`            | `PagingData<CallEntity>`          | `callRepo.pagedHistory(num, pageSize=50)`      |
| `historyTotalCount`  | `Int`                             | counted upfront                                |
| `isLoading`          | `Boolean`                         | true until first emission of stats             |
| `isError`            | `String?`                         | non-null = banner copy                         |
| `permissionMissing`  | `Boolean`                         | true if call-log perm revoked while open       |

`NumberStats` (computed by `NumberStatsUseCase`):

| Field              | Type               | Notes                              |
|--------------------|--------------------|------------------------------------|
| `totalCalls`       | `Int`              |                                    |
| `incomingCount`    | `Int`              |                                    |
| `outgoingCount`    | `Int`              |                                    |
| `missedCount`      | `Int`              | includes rejected per spec §3.4    |
| `totalDurationSec` | `Long`             |                                    |
| `firstCallAtMs`    | `Long`             |                                    |
| `lastCallAtMs`     | `Long`             |                                    |
| `avgDurationSec`   | `Long`             | `totalDurationSec / answeredCount` |
| `missedRatio`      | `Float` 0..1       | `missedCount / totalCalls`         |

The ViewModel exposes a `SharedFlow<CallDetailEvent>` for one-shot side effects:
snackbars, toast on share, "Copied number" feedback, navigation pops.

```kotlin
sealed interface CallDetailEvent {
    data class Snackbar(val text: String) : CallDetailEvent
    data class StartIntent(val intent: Intent) : CallDetailEvent
    data object PopBack : CallDetailEvent
}
```

### 21.5 Required inputs (user)

| Gesture                                   | Effect                                       |
|-------------------------------------------|----------------------------------------------|
| Tap back                                  | popBackStack                                 |
| Tap share-contact (top bar)               | fire vCard share intent                      |
| Tap any of the 5 action-bar buttons       | fire corresponding intent                    |
| Tap "Save to contacts" CTA in hero card   | fire ContactsContract insert intent          |
| Long-press hero number                    | copy to clipboard, snackbar "Copied"         |
| Tap LeadScoreBadge                        | tooltip popover with score breakdown         |
| Tap a tag chip                            | no-op (visual feedback only)                 |
| Tap × on a tag chip                       | remove that tag (`RemoveTagUseCase`)         |
| Tap "Add tag"                             | open TagPickerSheet                          |
| Tap a note's Edit                         | open NoteEditorDialog prefilled              |
| Tap a note's Delete                       | confirmation → `DeleteNoteUseCase`           |
| Tap "Add note"                            | open NoteEditorDialog blank                  |
| Tap "Set follow-up"                       | DatePicker → TimePicker chain                |
| Tap follow-up Edit                        | DatePicker → TimePicker prefilled            |
| Tap follow-up Cancel                      | confirmation → `CancelFollowUpUseCase`       |
| Tap follow-up Snooze                      | menu (1h / 1d / pick…)                       |
| Tap a history-timeline row                | no-op (already on this number's detail)      |
| Long-press a history-timeline row         | popup menu: Copy timestamp, Delete this call |
| Tap Manage → Edit notes                   | scroll + focus notes journal                 |
| Tap Manage → Clear all data               | dialog → clear → pop                         |
| Tap Manage → Report spam                  | apply Spam tag                               |
| Pull-to-refresh on the LazyColumn         | re-fetch stats + history                     |
| Scroll                                    | normal vertical scroll                       |

There is no horizontal swipe, no edge swipe, no shake gesture.

### 21.6 Mandatory display elements

In strict top-to-bottom order:

1. **Top app bar** — height 64dp, background TabBgCalls, leading `Icons.AutoMirrored.Filled.ArrowBack`, title `displayName ?: formattedNumber`, trailing single `IconButton(Icons.Filled.IosShare)` "Share contact card". No overflow ⋮ in Phase I.2 (removed because every action it hosted got promoted to either the action bar or the manage section).
2. **Hero card** — `NeoCard` with `TabBgCalls` background, padding 20dp, contains:
   - Top row: `NeoAvatar(64dp, seed=avatarSeed)` on the left; on its right a column with `displayName` (`Typography.titleLarge`) above `formattedNumber` (`Typography.bodyMedium`, secondary color). If `displayName == null`, the formatted number is rendered as the title and a smaller "Unsaved number" subtitle appears below.
   - Second row: `StatusPill(status)` + `LeadScoreBadge(leadScore)`.
   - Third row (only if `status == Unsaved`): `NeoButton.Primary(text = "Save to contacts", icon = PersonAdd)` full width.
   - Fourth row (only if `geocodedLocation != null`): `Icon(LocationOn) + " " + geocodedLocation`, body small, secondary color.
3. **Action bar** — single `Row(modifier = horizontalScroll)` of 5 `NeoIconButton`s, each 56dp × 56dp, 12dp gap:
   - Call (`Icons.Filled.Call`, accent green tint)
   - Message (`Icons.AutoMirrored.Filled.Message`, accent blue tint)
   - WhatsApp (custom vector, brand green tint)
   - Save (`Icons.Filled.PersonAdd`, accent purple tint) — disabled when `status == Saved`
   - Block (`Icons.Filled.Block`, error tint)
   Each button has a 12sp label below the icon.
4. **Stats card** — `NeoCard`, 16dp padding, two-column grid of stats:
   - Total calls · Talk time
   - First call · Last call
   - Avg duration · Missed rate
   Values are large; labels small. Empty values render `—` (em dash), not `0`.
5. **Tags section** — Section header "Tags" (titleSmall) with trailing "Add tag" `NeoButton.Tertiary`. Below: a `FlowRow(spacing = 8dp)` of applied tag chips, each with leading colored dot + text + trailing × icon. If no tags applied, a single greyed placeholder chip "No tags yet" (non-interactive).
6. **Notes journal** — Section header "Notes" with right-aligned count "(N)". Below: vertical list of note cards, newest first. Each note card:
   - Top row: timestamp (e.g. "Apr 14, 3:42 PM") + Edit + Delete inline icon buttons.
   - Body: markdown-rendered note (bold/italic/bullets/links via `MarkdownRenderer`, which supports a strict subset — see Part 06 Appendix M).
   At the end of the list: `NeoButton.Secondary("Add note", icon = NoteAdd)` full width.
7. **Follow-up section** — Section header "Follow-up". Below:
   - If `followUp != null && followUp.doneAt == null`: a `NeoCard` with the date+time on the left, and three `NeoIconButton`s on the right: Edit / Cancel / Snooze. A small "in 3 days" relative-time hint underneath.
   - If `followUp != null && followUp.doneAt != null`: a strikethrough card showing "Completed on …".
   - Else: a `NeoButton.Primary("Set follow-up", icon = NotificationsActive)` full width.
8. **Call history timeline** — Section header "Call history" with count "(M)". Below: a vertical list (capped at 50 with a "Show more" loader for older pages — see edge case 21.11.d). Each row:
   - Leading: type icon (incoming = down-arrow green, outgoing = up-arrow blue, missed = red strike, rejected = orange strike, voicemail = purple).
   - Center column: date · time · duration (e.g. "Apr 14 · 3:42 PM · 4m 12s").
   - Trailing: SIM badge (SIM 1 / SIM 2 / single-SIM = nothing).
9. **Manage section** — last vertical block, separated by a 32dp top spacer and a horizontal divider. Section header "Manage". Below: three `NeoButton.Tertiary`s, full-width, stacked:
   - Edit notes (icon = NoteEdit) — scrolls to notes journal.
   - Clear all data for this number (icon = DeleteSweep, danger tint) — opens confirmation.
   - Report spam (icon = Report, warning tint) — applies Spam tag.
10. Trailing 24dp bottom spacer so the last button isn't flush against the system nav bar.

### 21.7 Optional display elements

- **Trend arrow on Lead Score** if delta vs 30d prior is computable: tiny ▲/▼ next to the score.
- **"Auto-saved on Apr 12" subtitle** in the hero card when `status == AutoSaved`.
- **Geocoded location row** in hero card (only if available).
- **"3 unread notes" badge** (Phase II) — not in v1.0.
- **Recent activity sparkline** (24h heatmap) under stats card — Phase II.

### 21.8 Empty state

CallDetail itself is never "empty" in the global sense — there is always at least
one call that brought the user here. But individual sections can be empty:

| Section       | Empty copy / element                                                        |
|---------------|------------------------------------------------------------------------------|
| Tags          | Greyed placeholder chip: `"No tags yet"`                                    |
| Notes         | Light card: `"No notes yet."` + small body: `"Tap Add note to capture context."` |
| Follow-up     | Just the `Set follow-up` button (no separate empty card).                    |
| History       | Cannot be empty by definition; if it ever is, render `"No calls in history. This is unusual — try refreshing."` and a refresh button. |

The screen-level "404" state is documented in 21.11.a.

### 21.9 Loading state

- First emission: `isLoading = true`. The screen shows the **top bar fully**
  (back + title placeholder shimmer + share icon disabled), then a single
  full-screen `CircularProgressIndicator` centered in the body.
- After the hero info has loaded but stats/history haven't, the hero card
  renders for real and the rest of the body uses skeleton shimmers (one
  rectangle per section) for up to 600ms.
- Pull-to-refresh: `RefreshIndicator` at top; the existing data stays visible
  underneath; no full-screen spinner.

### 21.10 Error state

- **Permission revoked while open** (`READ_CALL_LOG` denied externally): the
  body is replaced with a `NeoErrorState` containing icon `LockOutline`,
  title `"Call log access was turned off."`, body `"Re-grant permission to see this number's history."`, and a primary button `"Grant permission"`.
- **DB read error** (catch-all): `NeoErrorState` with `ErrorOutline`,
  title `"Couldn't load this contact."`, body `"Pull to refresh or try again in a moment."`, and a `"Retry"` button.
- **Intent has no handler** (e.g. WhatsApp not installed): snackbar
  `"WhatsApp isn't installed on this device."`. The screen is unchanged.
- **Block stub failure**: snackbar `"Couldn't block this number. Try again."`.
- **Save intent cancelled by user**: snackbar `"Save cancelled."` (informational, not an error).

### 21.11 Edge cases

a. **Number not in DB.** ViewModel sees `historyTotalCount == 0` after first
   refresh. Render a dedicated "404" body: icon `SearchOff`, title `"This number isn't in your call log yet."`, body `"It may have been deleted, or this is a deep link from a stale source."`, button `"Go back"`. The hero card and action bar still render — calling/messaging an unknown number is still useful.

b. **Private number** (caller passed empty string for `normalizedNumber`). The
   ViewModel synthesizes a degraded state: `displayName = "Private number"`,
   action bar shows only the Block button enabled (Call/Message/WhatsApp/Save
   are disabled with a tooltip `"Number not available."`). History timeline
   shows all rows whose `phoneNumber.isBlank() == true`.

c. **0 calls in history but contact exists.** Defensive — shouldn't happen since
   we sourced the contact from a call. If it does, fall through to (a).

d. **1000+ calls in history.** Use `Pager(pageSize = 50, prefetchDistance = 10)`.
   The first page (50 rows) renders inline; the next page is appended on scroll.
   Compose key on `callId` to keep scroll position stable. Do **not** lazily
   recompute totals while paging — totals come from `NumberStatsUseCase` which
   queries with `COUNT(*)` once.

e. **Tag picker offline / DB locked.** TagPickerSheet shows skeleton rows for
   up to 1s then a `"Couldn't load tags. Tap to retry."` row.

f. **Follow-up scheduled in the past.** Allowed (the user may be back-dating a
   note-style reminder). The follow-up card simply shows "Overdue · 2 days ago"
   in red. The notification worker (which only fires on future timestamps) will
   ignore it.

g. **Note > 5000 characters.** The dialog's TextField has `maxLength = 5000`. The
   counter turns red at 4900. Pasting a longer string truncates with a snackbar.

h. **Markdown with malicious link** (`javascript:`). Renderer strips any non-`http(s)`
   schemes silently; `tel:`/`mailto:` are allowed; everything else is treated as
   plaintext.

i. **Concurrent edit** (user edits a note in this screen while the call sync
   worker writes a new call). Room observers re-emit independently; the notes
   list and the history list refresh independently without blocking each other.

j. **Hot-reload during edit.** If a configuration change occurs (rotation, dark
   mode toggle), the open NoteEditorDialog persists its draft via
   `rememberSaveable`.

k. **Share contact card with no display name.** vCard text uses the formatted
   number as `FN`; `N` field is left blank. The share text body reads
   `"Contact from CallVault: <number>"`.

l. **Number is the user's own number** (Telephony `getLine1Number()`). Action bar
   Call/Message remain enabled (the user might want to leave themselves a voicemail).
   No special UI — there is no reliable way to detect this on Android 10+.

m. **Block confirmation race.** If the user taps Block twice in <300ms, the
   second tap is debounced (button enters loading state on first press).

n. **Clear all data for number** while the number is currently in an active call
   (in-call bubble visible). Allowed: the in-call bubble survives; only DB rows
   are cleared. A new call entry will be re-inserted on `CALL_STATE_IDLE`.

### 21.12 Copy table

| Key                                  | Copy                                                                  |
|--------------------------------------|-----------------------------------------------------------------------|
| `cd_top_bar_title_unsaved`           | (formatted number)                                                    |
| `cd_top_bar_title_saved`             | (display name)                                                        |
| `cd_share_icon_cd`                   | Share contact card                                                    |
| `cd_back_icon_cd`                    | Back                                                                  |
| `cd_status_pill_saved`               | Saved                                                                 |
| `cd_status_pill_unsaved`             | Unsaved                                                               |
| `cd_status_pill_autosaved`           | Auto-saved                                                            |
| `cd_status_pill_blocked`             | Blocked                                                               |
| `cd_save_cta_label`                  | Save to contacts                                                      |
| `cd_lead_cold`                       | Cold lead                                                             |
| `cd_lead_warm`                       | Warm lead                                                             |
| `cd_lead_hot`                        | Hot lead                                                              |
| `cd_lead_score_tooltip_title`        | How this score is calculated                                          |
| `cd_lead_score_tooltip_body`         | Recency, frequency, answered ratio, follow-up activity.               |
| `cd_action_call`                     | Call                                                                  |
| `cd_action_message`                  | Message                                                               |
| `cd_action_whatsapp`                 | WhatsApp                                                              |
| `cd_action_save`                     | Save                                                                  |
| `cd_action_block`                    | Block                                                                 |
| `cd_action_block_confirm_title`      | Block this number?                                                    |
| `cd_action_block_confirm_body`       | You won't get calls or messages from this number until you unblock it. |
| `cd_action_block_confirm_yes`        | Block                                                                 |
| `cd_action_block_confirm_no`         | Cancel                                                                |
| `cd_stats_title`                     | At a glance                                                           |
| `cd_stats_total_calls`               | Total calls                                                           |
| `cd_stats_talk_time`                 | Talk time                                                             |
| `cd_stats_first_call`                | First call                                                            |
| `cd_stats_last_call`                 | Last call                                                             |
| `cd_stats_avg_duration`              | Avg duration                                                          |
| `cd_stats_missed_rate`               | Missed rate                                                           |
| `cd_tags_title`                      | Tags                                                                  |
| `cd_tags_add`                        | Add tag                                                               |
| `cd_tags_empty_chip`                 | No tags yet                                                           |
| `cd_tags_remove_cd`                  | Remove tag %1$s                                                       |
| `cd_notes_title`                     | Notes                                                                 |
| `cd_notes_count`                     | (%1$d)                                                                |
| `cd_notes_empty_title`               | No notes yet.                                                         |
| `cd_notes_empty_body`                | Tap Add note to capture context.                                      |
| `cd_notes_add`                       | Add note                                                              |
| `cd_notes_edit`                      | Edit                                                                  |
| `cd_notes_delete`                    | Delete                                                                |
| `cd_notes_delete_confirm_title`      | Delete this note?                                                     |
| `cd_notes_delete_confirm_body`       | This can't be undone.                                                 |
| `cd_notes_delete_confirm_yes`        | Delete                                                                |
| `cd_notes_dialog_title_new`          | New note                                                              |
| `cd_notes_dialog_title_edit`         | Edit note                                                             |
| `cd_notes_dialog_placeholder`        | What did you discuss? Markdown supported.                             |
| `cd_notes_dialog_save`               | Save                                                                  |
| `cd_notes_dialog_cancel`             | Cancel                                                                |
| `cd_notes_dialog_counter`            | %1$d / 5000                                                           |
| `cd_followup_title`                  | Follow-up                                                             |
| `cd_followup_set`                    | Set follow-up                                                         |
| `cd_followup_edit`                   | Edit                                                                  |
| `cd_followup_cancel`                 | Cancel                                                                |
| `cd_followup_snooze`                 | Snooze                                                                |
| `cd_followup_snooze_1h`              | 1 hour                                                                |
| `cd_followup_snooze_1d`              | 1 day                                                                 |
| `cd_followup_snooze_pick`            | Pick a time…                                                          |
| `cd_followup_overdue`                | Overdue · %1$s                                                        |
| `cd_followup_done_prefix`            | Completed on %1$s                                                     |
| `cd_history_title`                   | Call history                                                          |
| `cd_history_count`                   | (%1$d)                                                                |
| `cd_history_show_more`               | Show older calls                                                      |
| `cd_manage_title`                    | Manage                                                                |
| `cd_manage_edit_notes`               | Edit notes                                                            |
| `cd_manage_clear_all`                | Clear all data for this number                                        |
| `cd_manage_clear_confirm_title`      | Clear all data?                                                       |
| `cd_manage_clear_confirm_body`       | This deletes every call, tag, note, bookmark, and follow-up for %1$s. The contact in your phone book is not touched. |
| `cd_manage_clear_confirm_yes`        | Clear                                                                 |
| `cd_manage_clear_confirm_no`         | Keep                                                                  |
| `cd_manage_report_spam`              | Report spam                                                           |
| `cd_manage_report_spam_snackbar`     | Reported as spam.                                                     |
| `cd_error_perm_title`                | Call log access was turned off.                                       |
| `cd_error_perm_body`                 | Re-grant permission to see this number's history.                     |
| `cd_error_perm_button`               | Grant permission                                                      |
| `cd_error_db_title`                  | Couldn't load this contact.                                           |
| `cd_error_db_body`                   | Pull to refresh or try again in a moment.                             |
| `cd_error_db_button`                 | Retry                                                                 |
| `cd_error_no_whatsapp`               | WhatsApp isn't installed on this device.                              |
| `cd_error_block_failed`              | Couldn't block this number. Try again.                                |
| `cd_error_save_cancelled`            | Save cancelled.                                                       |
| `cd_404_title`                       | This number isn't in your call log yet.                               |
| `cd_404_body`                        | It may have been deleted, or this is a deep link from a stale source. |
| `cd_404_button`                      | Go back                                                               |
| `cd_private_label`                   | Private number                                                        |
| `cd_private_disabled_tooltip`        | Number not available.                                                 |
| `cd_copied_snackbar`                 | Copied                                                                |

### 21.13 ASCII wireframe

Default state — a saved contact with notes and a follow-up:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Ravi (Wholesale)                              ⤴ share  │  top bar (TabBgCalls)
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ╭───╮  Ravi (Wholesale)                                 │ │
│ │ │ R │  +91 98765 43210                                  │ │  hero card
│ │ ╰───╯  ┌──────┐ ┌────────────┐                          │ │
│ │        │Saved │ │ Hot · 82   │                          │ │
│ │        └──────┘ └────────────┘                          │ │
│ │  📍 Mumbai, Maharashtra                                 │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐                             │
│  │📞 │ │💬 │ │🟢 │ │👤+│ │🚫 │   action bar                │
│  └───┘ └───┘ └───┘ └───┘ └───┘                             │
│  Call  Msg   WA    Save  Block                              │
│                                                             │
│ ┌─ At a glance ─────────────────────────────────────────┐  │
│ │ Total calls   42      │  Talk time     2h 14m         │  │  stats card
│ │ First call    Mar 02  │  Last call     Apr 14         │  │
│ │ Avg duration  3m 11s  │  Missed rate   12%            │  │
│ └────────────────────────────────────────────────────────┘  │
│                                                             │
│  Tags                                       [+ Add tag]     │
│  ● Wholesale ×   ● VIP ×   ● Repeat ×                       │
│                                                             │
│  Notes (3)                                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Apr 14, 3:42 PM                       [Edit] [Del]   │  │
│  │ **Quoted ₹42,000** for the bulk order.               │  │
│  │ - Pickup Friday                                      │  │
│  │ - Asked to revisit pricing in 2 weeks                │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Apr 10, 11:15 AM                      [Edit] [Del]   │  │
│  │ Discussed payment terms.                             │  │
│  └──────────────────────────────────────────────────────┘  │
│  [ + Add note ]                                             │
│                                                             │
│  Follow-up                                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Apr 28 · 10:00 AM    in 3 days   [Edit][Cancel][Snz]│  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  Call history (42)                                          │
│  ↓ Apr 14 · 3:42 PM · 4m 12s                       SIM 1   │
│  ↑ Apr 12 · 9:08 AM · 1m 30s                       SIM 1   │
│  ✗ Apr 11 · 6:55 PM · missed                       SIM 2   │
│  ↓ Apr 10 · 11:15 AM · 12m 04s                     SIM 1   │
│  …                                                          │
│  [ Show older calls ]                                       │
│                                                             │
│  ────────────────────────────────────────────────────       │
│  Manage                                                     │
│  [ Edit notes                                          ]    │
│  [ Clear all data for this number                      ]    │
│  [ Report spam                                         ]    │
└─────────────────────────────────────────────────────────────┘
```

Unsaved-number state:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   +91 98201 11111                              ⤴ share  │
├─────────────────────────────────────────────────────────────┤
│ ┌─ hero ─────────────────────────────────────────────────┐ │
│ │ ╭───╮  +91 98201 11111                                  │ │
│ │ │ # │  Unsaved number                                   │ │
│ │ ╰───╯  ┌────────┐ ┌────────────┐                        │ │
│ │        │Unsaved │ │ Cold · 14   │                       │ │
│ │        └────────┘ └────────────┘                        │ │
│ │  [ + Save to contacts ]                                 │ │
│ └─────────────────────────────────────────────────────────┘ │
│  ... (rest identical)                                       │
└─────────────────────────────────────────────────────────────┘
```

Loading state:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   ░░░░░░░░░░░░                                ⤴         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                       ◐  loading…                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

Error (permission revoked):

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Ravi (Wholesale)                              ⤴       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                       🔒                                    │
│         Call log access was turned off.                     │
│   Re-grant permission to see this number's history.         │
│              [  Grant permission  ]                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

404 state (number not in DB):

```
┌─────────────────────────────────────────────────────────────┐
│  ←   +91 90000 00000                              ⤴        │
├─────────────────────────────────────────────────────────────┤
│  hero card (degraded — no stats)                            │
│  action bar                                                 │
│                                                             │
│                       🔍 SearchOff                          │
│        This number isn't in your call log yet.              │
│  It may have been deleted, or this is a stale deep link.    │
│                  [   Go back   ]                            │
└─────────────────────────────────────────────────────────────┘
```

### 21.14 Accessibility

- Every icon-only button has a `contentDescription`.
- Hero number is a `selectable Text` so VoiceOver/TalkBack can read individual
  digits; `semantics { contentDescription = "Phone number, $spelledOutDigits" }`.
- Lead score badge: `semantics { contentDescription = "Lead score $score out of 100, $bucketLabel" }`.
- Action bar buttons: minimum 48dp touch target (we use 56dp).
- Notes section is a `LazyColumn` item with `heading()` semantics on the section
  header; each note is a list item.
- Markdown links open with confirmation if scheme is non-https/non-tel.
- All snackbars are `LiveRegion.Polite`.
- Contrast: pill colors and badges meet WCAG AA against the TabBgCalls
  background. Dark mode uses pre-tested swap palette in Part 06 Appendix D.
- Talkback reading order: top bar → hero → action bar → stats → tags → notes →
  follow-up → history → manage. Verified via `mergeDescendants = true` on each
  card.

### 21.15 Performance budget

| Metric                                  | Budget                |
|-----------------------------------------|-----------------------|
| Time to first paint of top bar + hero   | ≤ 120 ms              |
| Time to stats card filled               | ≤ 250 ms              |
| Time to first 50 history rows           | ≤ 350 ms              |
| Memory (steady state)                   | ≤ 18 MB above baseline|
| Frame rate during scroll                | 60 fps p95            |
| Pull-to-refresh round trip              | ≤ 600 ms              |

`NumberStatsUseCase` issues a single SQL query with grouped aggregates; it does
not iterate on JVM. Notes and tags are observed via Flow; updates re-emit only
the changed list.

---

## §22 — Search overlay

`com.callvault.app.ui.screen.search.SearchScreen`

### 22.1 Purpose

Search is the universal find-anything page. It is reached from the persistent
search icon in the Calls and Library top bars, and from the global keyboard
shortcut (`Ctrl+K` on hardware-keyboard devices).

It is intentionally *not* wrapped in `StandardPage`. The colored tab background
and the header gradient would add chrome that fights focus. Instead the page is
pure white (or pure surface in dark mode), edge-to-edge, with only the search
field at the top. This is the one place in the app where the user wants tunnel
vision.

The search runs against the FTS4 virtual table `call_search_fts` (built from the
columns `phoneNumber`, `displayName`, `geocodedLocation`, plus indexed
joined-in fields `noteText` and `tagName`). Token prefix matching is enabled
(`tokenize = unicode61 "remove_diacritics=2"`); query rewriting handles single-
character no-op, special-character escaping, and per-token trim.

### 22.2 Entry points

| Source                                             | Effect                |
|----------------------------------------------------|-----------------------|
| Top-bar search icon on Calls / Library             | navigate to `search`  |
| Hardware keyboard `Ctrl + K` from any tab          | navigate to `search`  |
| Empty-state CTA on the Calls tab "Search calls"    | navigate to `search`  |
| Stats insight card "View calls in this range"      | navigate to `search`  |

The route is the literal string `search` — no args.

### 22.3 Exit points

- Back arrow / system back → `popBackStack()`.
- Tap a result → navigate to `callDetail/{normalizedNumber}` (popping search off
  the back stack so back from CallDetail returns to the original tab).
- Tap a recent search row → fills the field with that query (does not navigate).
- Tap the trailing × → clears the field; does not exit.
- Tap "Clear" next to recent searches → empties recent history; does not exit.

### 22.4 Required inputs (data)

`SearchViewModel` state:

| Field            | Type                       | Notes                              |
|------------------|----------------------------|------------------------------------|
| `query`          | `String`                   | trimmed, length-capped at 80 chars |
| `debouncedQuery` | `String`                   | derived, 300ms debounce             |
| `recents`        | `List<RecentSearch>`       | last 10 from `SearchHistoryDao`    |
| `results`        | `List<CallEntity>`         | capped at 200 (see edge case)      |
| `isLoading`      | `Boolean`                  | true while FTS in flight           |
| `activeFilters`  | `List<SearchFilter>`       | populated from advanced filters; empty in v1.0 |

Source: `callRepo.searchFts(q: String): Flow<List<CallEntity>>`. The
implementation:

```sql
SELECT c.* FROM call c
JOIN call_search_fts fts ON fts.rowid = c.id
WHERE call_search_fts MATCH :tokens
ORDER BY c.timestampUtc DESC
LIMIT 200
```

### 22.5 Required inputs (user)

| Gesture                  | Effect                                             |
|--------------------------|----------------------------------------------------|
| Type into field          | updates `query`; debounced 300ms triggers search   |
| Tap × in field           | clears query                                       |
| Tap leading back arrow   | popBackStack                                       |
| Tap a recent-search row  | fills field with that query                        |
| Long-press recent-search | offers "Remove" item                               |
| Tap "Clear" recents      | empties history                                    |
| Tap a result row         | navigate to CallDetail; persist the query as recent|
| Pull-to-refresh          | re-runs the current query                          |
| Hardware keyboard Enter  | dismisses keyboard; query still runs (already debounced) |

### 22.6 Mandatory display elements

- **Inline search field** (replaces the standard top app bar). 56dp tall.
  Background = surface; leading `IconButton(ArrowBack)`; trailing `IconButton(Clear)`
  visible only when `query.isNotEmpty()`. Cursor color = `AccentBlue`.
  Placeholder `"Search number, name, note, tag…"`. Single-line.
- **Active filter chips row** (only if `activeFilters.isNotEmpty()` — empty in
  v1.0 but reserved space).
- **Body — empty query state**:
  - Section "Recent" (titleSmall) with trailing `TextButton("Clear")`.
  - List of up to 10 `RecentSearchRow`s, each = leading `History` icon +
    query text + trailing × (per-row remove).
  - If recents is empty: a single placeholder "Try a number, name, or note keyword."
- **Body — query non-empty, no results**:
  - Centered `NeoEmptyState`: icon `SearchOff`, title `"No matches."`, body
    `"Try a number, name, or note keyword. CallVault searches across notes, tags, names, and numbers."`.
- **Body — query non-empty, results**:
  - `LazyColumn` of `CallRow`s (the canonical row, see Part 06 Appendix C).
    Each row gets a subtle highlight on the matched text token (Compose
    `AnnotatedString` with `SpanStyle(background = AccentBlue.copy(alpha = 0.16f))`).
  - Trailing footer "Showing top 200 results — narrow your query for more." if
    cap was hit.

### 22.7 Optional display elements

- **Voice search mic** (Phase II) — placeholder space reserved on the right of
  the field.
- **"Did you mean?" suggestion** — Phase II.
- **Filter chips** (date range, type) — Phase II.

### 22.8 Empty state

The "no recents and no query" state is the canonical empty state:

> Tip: "Try a number, name, or note keyword."

Icon: `SearchOutline` 48dp, secondary tint, centered with the tip 24dp below.

When the query has run and yielded zero rows, the `SearchOff` empty state above
is shown.

### 22.9 Loading state

A 2dp `LinearProgressIndicator` directly under the search field, indeterminate,
visible while `isLoading == true`. The previous results list (if any) remains
visible underneath (do not blank it).

For the very first search of a session (cold FTS), the indicator may show for up
to 800ms while SQLite warms its index.

### 22.10 Error state

- **FTS syntax error** (defensive — the sanitizer should prevent this): show a
  snackbar `"Search hit an error. Try simplifying your query."` and re-render
  the empty/recents state.
- **DB unavailable** (rare): full-screen `NeoErrorState` with `"Search is unavailable right now. Try again."`.

### 22.11 Edge cases

a. **Single character query.** Do not run FTS until length ≥ 2. Show the recents
   list still.

b. **Query is all whitespace.** Treated as empty.

c. **Special characters** (`'`, `"`, `*`, `(`, `)`). Sanitizer strips them
   before passing to FTS `MATCH`; otherwise SQLite would throw.

d. **200+ results.** Hard cap. Footer message tells the user to narrow.

e. **No notes or tags exist on this device.** FTS still works on number, name,
   and geocoded location only — query something matching those.

f. **User pastes a 500-character string.** Field clamps to 80; pastes over
   trigger a snackbar `"Search query truncated."`.

g. **Quick consecutive typing.** Debounce 300ms means only the final query in a
   burst is run. The loading indicator only shows for queries actually in flight.

h. **Result tapped while next debounce is pending.** Cancel the pending search;
   navigate immediately.

i. **Recent searches contain stale numbers.** A recent like "+91 98765 43210"
   that no longer matches anything in the DB still runs and yields the empty
   state (it is not auto-pruned).

### 22.12 Copy table

| Key                          | Copy                                                                         |
|------------------------------|------------------------------------------------------------------------------|
| `srch_field_placeholder`     | Search number, name, note, tag…                                              |
| `srch_field_clear_cd`        | Clear search                                                                 |
| `srch_back_cd`               | Back                                                                         |
| `srch_recents_title`         | Recent                                                                       |
| `srch_recents_clear`         | Clear                                                                        |
| `srch_recents_remove_cd`     | Remove %1$s from recents                                                     |
| `srch_empty_tip`             | Try a number, name, or note keyword.                                         |
| `srch_no_results_title`      | No matches.                                                                  |
| `srch_no_results_body`       | Try a number, name, or note keyword. CallVault searches across notes, tags, names, and numbers. |
| `srch_cap_footer`            | Showing top 200 results — narrow your query for more.                        |
| `srch_truncated_paste`       | Search query truncated.                                                      |
| `srch_error_syntax`          | Search hit an error. Try simplifying your query.                             |
| `srch_error_unavailable`     | Search is unavailable right now. Try again.                                  |
| `srch_loading_cd`            | Searching                                                                    |

### 22.13 ASCII wireframes

Empty query, with recents:

```
┌─────────────────────────────────────────────────────────────┐
│ ←  ┌─────────────────────────────────────────┐              │
│    │  Search number, name, note, tag…       │              │
│    └─────────────────────────────────────────┘              │
├─────────────────────────────────────────────────────────────┤
│ Recent                                            [Clear]   │
│                                                             │
│  ↺  Ravi wholesale                                  ×       │
│  ↺  +91 98765                                       ×       │
│  ↺  payment                                         ×       │
│  ↺  follow up                                       ×       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

Empty query, no recents:

```
┌─────────────────────────────────────────────────────────────┐
│ ←  ┌─ Search number, name, note, tag… ─────────┐            │
│    └────────────────────────────────────────────┘            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                       🔍                                    │
│         Try a number, name, or note keyword.                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

Active query with results:

```
┌─────────────────────────────────────────────────────────────┐
│ ←  ┌─ ravi ─────────────────────────────────── × ┐          │
│    └────────────────────────────────────────────┘          │
│ ──── (loading bar) ────                                    │
├─────────────────────────────────────────────────────────────┤
│ ╭───╮  Ravi (Wholesale)                  ↓ Apr 14 3:42 PM   │
│ │ R │  +91 98765 43210                   4m 12s   SIM 1     │
│ ╰───╯                                                       │
│ ╭───╮  Ravi Mehta                        ↑ Apr 13 11:00 AM  │
│ │ R │  +91 90000 11111                   2m 03s   SIM 2     │
│ ╰───╯                                                       │
│ ...                                                         │
└─────────────────────────────────────────────────────────────┘
```

No results:

```
┌─────────────────────────────────────────────────────────────┐
│ ←  ┌─ qwxlk  ──────────────────────────────── × ┐           │
│    └─────────────────────────────────────────────┘          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                       🔍✗                                   │
│                  No matches.                                │
│  Try a number, name, or note keyword. CallVault searches    │
│  across notes, tags, names, and numbers.                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 22.14 Accessibility

- The field is a `TextField` with proper `imeAction = ImeAction.Search` and
  semantics `role = SearchField`.
- Cursor and selection colors are AA against surface in both themes.
- Result rows expose the same a11y surface as the canonical `CallRow` (see
  Part 06 Appendix C).
- Empty/no-result states are announced via `LiveRegion.Assertive` only on
  *transition* into the state (not on every recomposition).
- Highlighted match span uses background color *and* underline so colorblind
  users can still see the match.

### 22.15 Performance budget

| Metric                                       | Budget       |
|----------------------------------------------|--------------|
| Keystroke → debounced query fire             | 300 ms ± 20  |
| FTS round trip for typical 50k-call DB       | ≤ 80 ms      |
| FTS round trip for 200k-call DB              | ≤ 220 ms     |
| First result paint after query fire          | ≤ 350 ms p95 |
| Memory                                       | ≤ 8 MB       |

The 200-result cap, the FTS index, and the 300ms debounce are jointly tuned to
hit these numbers on a Snapdragon 660-class device. If the user has fewer than
1000 calls total, the entire query path is ≤ 30ms.

---

## §23 — Stats dashboard

`com.callvault.app.ui.screen.stats.StatsScreen`

### 23.1 Purpose

Stats is the chart-heavy visualisation page. It answers business-owner questions
in the form: "How am I doing this week vs last? When are calls coming in? Who
are my top inquiries? What share am I missing?"

The spec in §3.10 of the master prompt lists 10 possible charts. v1.0 ships
*4 of those 10*; the rest are scaffolded for v1.1+. The 4 v1.0 charts are:
DailyVolume (line + 7d MA), TypeDonut (incoming/outgoing/missed/voicemail),
HourlyHeatmap (24×7), and TopNumbersList (segmented by count or duration).

The page is also where automated **Insights** surface — small, pre-computed
notices generated by `GenerateInsightsUseCase` that highlight things the user
might miss (e.g. "Missed rate jumped from 12% to 31% this week" or "You haven't
followed up with 4 hot leads from last week").

### 23.2 Entry points

| Source                                    | Effect              |
|-------------------------------------------|---------------------|
| Bottom nav → Stats tab → "Open dashboard" | navigate to `stats` |
| Home shortcut tile "View stats"           | navigate to `stats` |
| Notification "Weekly summary" tap         | deep link to `stats?range=last7d` |

### 23.3 Exit points

- Back arrow → popBackStack to whichever tab launched it.
- Top-bar `DateRangeChip` → opens DateRangeSheet (modal bottom sheet).
- Insight card "View" buttons → may navigate to Search (with prefilled query) or
  to FollowUps (with prefilled tab).
- TopNumbersList row tap → CallDetail.
- "Export PDF" button → currently shows snackbar `"Available in v1.1"` (no
  navigation). The button still renders so testers can locate the future hook.

### 23.4 Required inputs (data)

`StatsViewModel` state:

| Field        | Type              | Notes                                              |
|--------------|-------------------|----------------------------------------------------|
| `range`      | `DateRange`       | sealed: `Today`, `Last7d`, `Last30d` (default), `ThisMonth`, `LastMonth`, `Last90d`, `Custom(from, to)` |
| `snapshot`   | `StatsSnapshot?`  | computed by `BuildStatsSnapshotUseCase(range)`     |
| `prevSnapshot` | `StatsSnapshot?`| same range shifted back, for trend arrows         |
| `insights`   | `List<Insight>`   | ≤ 5, from `GenerateInsightsUseCase(snapshot)`     |
| `isLoading`  | `Boolean`         |                                                    |
| `error`      | `String?`         |                                                    |

`StatsSnapshot`:

| Field                | Type                       |
|----------------------|----------------------------|
| `range`              | `DateRange`                |
| `totalCalls`         | `Int`                      |
| `talkTimeSec`        | `Long`                     |
| `avgDurationSec`     | `Long`                     |
| `missedRatio`        | `Float`                    |
| `byType`             | `Map<CallType, Int>`       |
| `byHourDay`          | `IntArray` size 24*7 = 168 |
| `dailyVolume`        | `List<DailyPoint>`         |
| `topByCount`         | `List<LeaderboardEntry>`   |
| `topByDuration`      | `List<LeaderboardEntry>`   |
| `leadDistribution`   | `Triple<Int, Int, Int>`    | cold / warm / hot         |

### 23.5 Required inputs (user)

| Gesture                              | Effect                                |
|--------------------------------------|---------------------------------------|
| Tap back                             | popBackStack                          |
| Tap DateRangeChip                    | opens DateRangeSheet                  |
| Tap a preset chip in the sheet       | sets range, dismiss sheet, recompute  |
| Tap "Custom…"                        | opens DatePickerDialog (from + to)    |
| Tap a TopNumbers row                 | navigate to CallDetail                |
| Toggle TopNumbers segmented control  | switches between byCount / byDuration |
| Tap an insight's primary CTA         | as defined by insight type            |
| Tap "Export PDF"                     | snackbar "Available in v1.1"          |
| Pull-to-refresh                      | re-run snapshot                       |
| Pinch-zoom on a chart                | no-op (Phase II)                      |

### 23.6 Mandatory display elements

`StandardPage` with `TabBgStats` (warm purple) + `HeaderGradStats` (purple →
blue gradient) header.

Top app bar: leading back arrow, title "Stats", trailing `NeoChip(label = range.displayLabel, trailingIcon = ExpandMore)`.

Body — `LazyColumn`, items in this order:

1. **Overview row** — `Row(modifier = horizontalScroll)` of 4 `NeoCard`s, each
   180dp wide × 96dp tall:
   - Total calls (icon: PhoneInTalk, accent blue) + value + delta arrow
   - Talk time (icon: Timer, accent green) + formatted "Xh Ym"
   - Avg duration (icon: HourglassBottom, accent amber)
   - Missed rate (icon: PhoneMissed, accent red) + percent

2. **Lead distribution mini-bar** — `NeoCard` with section title "Lead mix".
   Inside: a single 16dp tall `Row` of 3 `Box`es, weighted by cold/warm/hot
   counts, colored gray/amber/red. Below the bar a 3-column legend with counts.

3. **Insights** — 0 to 5 `NeoCard`s stacked, each with:
   - Severity-colored 4dp left border (Info=base, Warn=amber, Critical=red).
   - Leading icon matching severity.
   - Title (titleSmall).
   - Body (bodyMedium).
   - Optional primary CTA `NeoButton.Tertiary` aligned right.

4. **Daily volume chart** — `NeoCard`, height 220dp:
   - Header row: title "Calls per day" + small legend "● Volume   — 7d avg".
   - Compose `Canvas` rendering: vertical bars (or line — implementation choice
     per Part 08 §8.4) for daily totals, plus an overlaid 7-day moving average
     line. X-axis date ticks at start, midpoint, end of range. Y-axis hidden.

5. **Type donut** — `NeoCard`, height 220dp:
   - Compose `Canvas` arcs for incoming / outgoing / missed / voicemail.
   - Legend on the right with percentage and count per slice.
   - Center label: total count.

6. **Hourly heatmap** — `NeoCard`, height 320dp:
   - 24 columns (hours 0–23) × 7 rows (Sun–Sat).
   - Each cell colored by intensity (white → AccentBlue gradient, log-scaled).
   - Row labels on left (Sun, Mon, …); column labels on bottom (12a, 6a, 12p, 6p).
   - Tap a cell → highlights it and shows "12 calls · Tue 3-4 PM" tooltip.

7. **Top numbers list** — `NeoCard`:
   - Header: title "Top numbers" + segmented control "By count / By duration".
   - Below: 10 `LeaderboardEntry` rows. Each row = NeoAvatar (sm) + name/number
     + numeric value (count or duration) + trailing chevron.

8. **Export PDF** — `NeoButton.Primary("Export PDF as report")` full width, in
   its own bottom block. Disabled affordance because v1.0 only shows a snackbar.

Trailing 32dp spacer.

### 23.7 Optional display elements

- **Trend arrows** on overview cards (only when `prevSnapshot` is non-null).
- **Insight dismiss icon** (Phase II — currently insights regenerate each load).
- **Range delta sub-line** on overview cards: "vs prev. period: +12%".

### 23.8 Empty state

If `snapshot.totalCalls == 0` for the chosen range:

- Overview cards still render with `0` / `—` values (no trend arrows).
- Lead mix bar collapses to a single greyed bar with label `"No data."`.
- Insights section is hidden entirely.
- Each chart card renders its placeholder (see per-chart copy below).

Copy: `stats_empty_range_title` = "No calls in this range." · `stats_empty_range_body` = "Try a wider range from the chip above."

### 23.9 Loading state

- First load: full-screen `CircularProgressIndicator` centered. Top app bar
  rendered.
- Range change: keep the previous snapshot visible; show a 2dp linear progress
  bar at the very top of the body. Each chart card shows a shimmer overlay until
  its data is available.
- Pull-to-refresh: standard `RefreshIndicator`.

### 23.10 Error state

- **Date range invalid** (custom from > to): swap them silently (logged via
  Timber.w) and recompute.
- **Range exceeds 90 days**: warn snackbar `"Wide ranges may take longer to compute."` but proceed.
- **Snapshot computation error**: full-screen `NeoErrorState` with title
  `"Couldn't load stats."`, body `"Pull to refresh."`, button `"Retry"`.
- **Single-SIM device**: SIM-utilization chart (when added in v1.1) will simply
  hide; v1.0 has no SIM chart so this is a no-op.

### 23.11 Edge cases

a. **0 calls in range** — see Empty state. Each chart shows its own placeholder
   ("No data in this range") instead of an empty plot area.

b. **Single-SIM device** — no SIM chart in v1.0; future-proofed.

c. **200k+ calls in range** — `BuildStatsSnapshotUseCase` does a single SQL
   pass with grouped aggregates; daily volume is computed by `GROUP BY date(timestampUtc, 'unixepoch', 'localtime')`. Sampling kicks in *only* for the heatmap render (it draws once per cell, not per call).

d. **Custom range with from > to** — swap in ViewModel before passing to use
   case; emit a `Snackbar("Swapped your dates so 'from' comes first.")`.

e. **Custom range with from == to** — treated as a single-day range; daily
   volume chart is degenerate (one bar) but renders.

f. **Range > 365 days** — clamped to 365 with snackbar `"Range capped at 365 days. Use export for longer history."`.

g. **All calls are missed in the range** — donut renders with one slice (red
   "Missed 100%"); incoming/outgoing slices render as 1px hairlines so the
   legend still resolves them.

h. **DST transition inside the range** — `dailyVolume` aggregation uses the
   device's current zone; days during DST shift may show 23 or 25 hours of data.
   Acceptable for v1.0; flagged in DECISIONS.md.

i. **Insight CTA target removed** (e.g. an insight references a number whose
   data was cleared) — the CTA shows a snackbar `"That contact is no longer in your data."` instead of navigating.

j. **Export PDF tapped** — v1.0 always shows `"Available in v1.1"`. No work is
   queued.

### 23.12 Copy table

| Key                              | Copy                                                            |
|----------------------------------|-----------------------------------------------------------------|
| `stats_title`                    | Stats                                                           |
| `stats_back_cd`                  | Back                                                            |
| `stats_range_chip_cd`            | Change date range                                               |
| `stats_range_today`              | Today                                                           |
| `stats_range_last7d`             | Last 7 days                                                     |
| `stats_range_last30d`            | Last 30 days                                                    |
| `stats_range_this_month`         | This month                                                      |
| `stats_range_last_month`         | Last month                                                      |
| `stats_range_last90d`            | Last 90 days                                                    |
| `stats_range_custom`             | Custom…                                                         |
| `stats_overview_total`           | Total calls                                                     |
| `stats_overview_talk`            | Talk time                                                       |
| `stats_overview_avg`             | Avg duration                                                    |
| `stats_overview_missed`          | Missed rate                                                     |
| `stats_overview_delta_up`        | +%1$s vs prev                                                   |
| `stats_overview_delta_down`      | %1$s vs prev                                                    |
| `stats_lead_mix_title`           | Lead mix                                                        |
| `stats_lead_cold`                | Cold                                                            |
| `stats_lead_warm`                | Warm                                                            |
| `stats_lead_hot`                 | Hot                                                             |
| `stats_insights_title`           | Insights                                                        |
| `stats_chart_daily_title`        | Calls per day                                                   |
| `stats_chart_daily_legend_vol`   | Volume                                                          |
| `stats_chart_daily_legend_avg`   | 7d avg                                                          |
| `stats_chart_daily_empty`        | No daily data in this range.                                    |
| `stats_chart_donut_title`        | Call mix                                                        |
| `stats_chart_donut_empty`        | No call mix to show.                                            |
| `stats_chart_heatmap_title`      | When calls happen                                               |
| `stats_chart_heatmap_empty`      | Not enough data to map by hour.                                 |
| `stats_chart_top_title`          | Top numbers                                                     |
| `stats_chart_top_segment_count`  | By count                                                        |
| `stats_chart_top_segment_dur`    | By duration                                                     |
| `stats_chart_top_empty`          | No top numbers in this range.                                   |
| `stats_export_button`            | Export PDF as report                                            |
| `stats_export_unavailable`       | Available in v1.1                                               |
| `stats_empty_range_title`        | No calls in this range.                                         |
| `stats_empty_range_body`         | Try a wider range from the chip above.                          |
| `stats_warn_wide_range`          | Wide ranges may take longer to compute.                         |
| `stats_warn_swap_dates`          | Swapped your dates so 'from' comes first.                       |
| `stats_warn_clamp_range`         | Range capped at 365 days. Use export for longer history.        |
| `stats_error_title`              | Couldn't load stats.                                            |
| `stats_error_body`               | Pull to refresh.                                                |
| `stats_error_retry`              | Retry                                                           |
| `stats_insight_target_missing`   | That contact is no longer in your data.                         |

### 23.13 ASCII wireframes

Default state (Last 30 days):

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Stats                              [ Last 30 days ⌄ ]  │
├─────────────────────────────────────────────────────────────┤
│ ┌─Total─┐ ┌─Talk─┐ ┌─Avg──┐ ┌Missed┐                       │
│ │ 412 ▲ │ │ 18h  │ │ 2m 38│ │ 18%▼ │   overview row →     │
│ └───────┘ └──────┘ └──────┘ └──────┘                       │
│                                                             │
│ Lead mix                                                    │
│ ████████████████░░░░░░░░░░░░░░  64% Cold · 28% Warm · 8% Hot│
│                                                             │
│ Insights                                                    │
│ ┃ ⚠ Missed rate jumped to 31% this week.       [View]      │
│ ┃ ℹ 4 hot leads have no follow-up scheduled.   [Plan]      │
│                                                             │
│ ┌─ Calls per day ──────────────────────────────────────┐   │
│ │   ▁▂▆█▆▃▂  ▂▄▆▆▄▂   line + bars                     │   │
│ │ Mar 15           Mar 30           Apr 14            │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                             │
│ ┌─ Call mix ──────────────────────────────────────────┐    │
│ │       ◔     412     ● Incoming  62%                 │    │
│ │      ◐  ◑   total   ● Outgoing  20%                 │    │
│ │             calls   ● Missed    16%                 │    │
│ │                     ● Voicemail  2%                 │    │
│ └──────────────────────────────────────────────────────┘   │
│                                                             │
│ ┌─ When calls happen ──────────────────────────────────┐   │
│ │     12a  6a  12p  6p                                 │   │
│ │ Sun ░░░ ░░  ▓▓▓ ▓░                                   │   │
│ │ Mon ░░░ ▓▓  ███ ██                                   │   │
│ │ ...                                                  │   │
│ └──────────────────────────────────────────────────────┘   │
│                                                             │
│ ┌─ Top numbers ─────────── [By count │ By duration] ──┐    │
│ │  1  Ravi Wholesale       42 calls                   │    │
│ │  2  Suresh                28 calls                  │    │
│ │  …                                                  │    │
│ └──────────────────────────────────────────────────────┘   │
│                                                             │
│  [ Export PDF as report ]                                   │
└─────────────────────────────────────────────────────────────┘
```

Empty range state:

```
┌─ Stats ───────── [ Today ⌄ ] ─┐
│  Total 0   Talk —   Avg —    │
│                              │
│  No calls in this range.     │
│  Try a wider range from the  │
│  chip above.                 │
└──────────────────────────────┘
```

Loading:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Stats                              [ Last 30 days ⌄ ]  │
│ ──── (linear progress) ──── (top of body)                  │
│                                                             │
│      shimmers everywhere                                    │
└─────────────────────────────────────────────────────────────┘
```

Error:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Stats                              [ Last 30 days ⌄ ]  │
├─────────────────────────────────────────────────────────────┤
│                       ⚠                                     │
│              Couldn't load stats.                           │
│                Pull to refresh.                             │
│                  [ Retry ]                                  │
└─────────────────────────────────────────────────────────────┘
```

DateRangeSheet:

```
┌── Choose a range ─────────────────────────────────┐
│ [Today] [7d] [30d✓] [This mo] [Last mo] [90d]    │
│ [ Custom… ]                                       │
└───────────────────────────────────────────────────┘
```

### 23.14 Accessibility

- Each chart card is a single semantics merge with a textual fallback summary
  (e.g. "Calls per day from Mar 15 to Apr 14: average 14, peak 32 on Apr 02.").
  TalkBack reads the summary instead of trying to traverse the canvas.
- Heatmap exposes per-cell semantics on focus only (otherwise 168 cells would
  spam reading order).
- Insight cards: severity is announced as part of the title prefix
  ("Warning: …", "Info: …").
- DateRangeSheet preset chips have role `RadioButton` and a single selection.
- Color contrast: heatmap cells use a luminance-pair pre-tested for AA.
- Charts respect `Configuration.fontScale`: chart axis ticks scale with text;
  the canvas itself is unscaled.

### 23.15 Performance budget

| Metric                                | Budget        |
|---------------------------------------|---------------|
| Snapshot for 30k-call DB              | ≤ 220 ms      |
| Snapshot for 200k-call DB             | ≤ 800 ms      |
| Chart paint per frame                 | ≤ 4 ms        |
| Memory                                | ≤ 22 MB       |
| Range-change → first chart updated    | ≤ 280 ms p95  |

Heatmap painting uses a single `Canvas.drawRect` per cell with pre-computed
colors. DailyVolume uses a precomputed `Path` recreated only on data change.
TopNumbers uses Compose `LazyColumn` with `key = entry.normalizedNumber`.

---

## §24 — Bookmarks screen

`com.callvault.app.ui.screen.bookmarks.BookmarksScreen`

### 24.1 Purpose

Bookmarks lets the user *star* individual calls (not contacts) so they can
return to them quickly. The semantic is closer to "this specific conversation
mattered" than "this contact matters" — which is why it's call-scoped, not
number-scoped.

The first bookmark for a number prompts the user for a free-text "reason"
which gets stored alongside; this is shown as the row's subtitle in the list.
Subsequent bookmarks for the same number reuse the existing reason silently.

The page also supports up to 5 *pinned* bookmarks — the user's favorite
favorites — that float at the top in a horizontal carousel.

### 24.2 Entry points

| Source                                     | Effect                |
|--------------------------------------------|-----------------------|
| Library tab → Bookmarks list item          | navigate to `bookmarks`|
| Home shortcut tile "Bookmarks"             | navigate to `bookmarks`|
| CallDetail history → long-press → bookmark | applies & toast       |

### 24.3 Exit points

- Back arrow → popBackStack.
- Tap a bookmark row → `callDetail/{normalizedNumber}`.
- Tap a pinned-bookmark carousel item → same as above.
- Long-press a row → action sheet (Unpin/Pin · Remove · Open).
- Long-press a pinned item → drag mode begins; on drop, `ReorderPinUseCase`.

### 24.4 Required inputs (data)

`BookmarksViewModel` state:

| Field             | Type                       | Source                                         |
|-------------------|----------------------------|------------------------------------------------|
| `pinnedBookmarks` | `List<BookmarkEntry>`      | `settings.observePinnedBookmarks()` (≤ 5)      |
| `allBookmarks`    | `List<BookmarkEntry>`      | `bookmarkRepo.observeAll()` (sorted by `bookmarkedAt DESC`) |
| `isLoading`       | `Boolean`                  |                                                |
| `firstReasonPrompt` | `BookmarkEntry?`         | non-null when first bookmark of a number is being created |

`BookmarkEntry`:

| Field              | Type      |
|--------------------|-----------|
| `id`               | `Long`    |
| `callId`           | `Long`    |
| `normalizedNumber` | `String`  |
| `displayName`      | `String?` |
| `bookmarkedAt`     | `Long`    |
| `reason`           | `String?` |
| `pinPosition`      | `Int?`    | null = not pinned; else 1..5 |

### 24.5 Required inputs (user)

| Gesture                          | Effect                                    |
|----------------------------------|-------------------------------------------|
| Tap row                          | navigate to CallDetail                    |
| Long-press row                   | bottom sheet: Pin / Unpin / Remove / Open |
| Tap Pin in sheet                 | promote to pinned (if < 5 already)        |
| Tap Remove in sheet              | confirmation → delete                     |
| Tap pinned-carousel item         | navigate to CallDetail                    |
| Long-press pinned item           | enters drag mode (haptic feedback)        |
| Drag pinned item to new slot     | reorder; reorder is committed on drop     |
| Tap up/down arrows on pinned     | alternative to drag, accessible           |
| Pull-to-refresh on the main list | re-fetch                                  |

### 24.6 Mandatory display elements

`StandardPage` with:
- Title: `"Bookmarks"`
- Subtitle: `"Calls you've starred"`
- Header glyph: ⭐ (or `Icons.Filled.Star` in actual code)

Body — `LazyColumn`:

1. **Pinned section** (only if `pinnedBookmarks.isNotEmpty()`):
   - Header titleSmall "Pinned" + count.
   - Below: a `Row(modifier = horizontalScroll)` of up to 5 pinned items, each:
     - 96dp wide × 120dp tall `NeoCard` with NeoAvatar at top,
       displayName (or formatted number) below, and a tiny "Top N" badge.
     - In drag mode, two small `↑` and `↓` `NeoIconButton`s overlay the bottom
       edge for keyboard/non-drag accessibility.

2. **All bookmarks section**:
   - Header titleSmall "All" + count.
   - List of `CallRow`s (the canonical row, see Part 06 Appendix C), one per
     bookmark, sorted by `bookmarkedAt DESC`.
   - Trailing star-filled icon at the row end indicates bookmark state (always
     filled here).
   - Subtitle line shows `reason` when present, else the row's normal subtitle.

### 24.7 Optional display elements

- **First-bookmark prompt dialog** (`NeoDialog`) opens automatically the first
  time a number is bookmarked anywhere in the app. Asks for an optional reason
  string (≤ 120 chars). "Skip" or "Save" buttons. Subsequent bookmarks for the
  same number do not re-prompt.

- **Bookmark-collected animation** — when a row is freshly created, it pulses
  gold once on first paint. Implementation: `AnimatedContent` with a shimmer
  overlay 600ms, then settles.

### 24.8 Empty state

When `allBookmarks.isEmpty()`:

- Centered `NeoEmptyState`: animated star icon (gentle scale-pulse 1.0 ↔ 1.1
  every 1.6s), title `"No bookmarks yet."`, body `"Star a call to save it here. Long-press any row in your call list to bookmark it."`. No CTA button — bookmarks are created elsewhere.

### 24.9 Loading state

- First load: shimmers — 1 pinned-carousel placeholder strip + 6 row shimmers.
- Pull-to-refresh: standard indicator.

### 24.10 Error state

- DB read fails: full-screen `NeoErrorState`, title `"Couldn't load bookmarks."`,
  body `"Try again."`, button `"Retry"`.
- Pin failed (e.g. >5 pinned attempted): snackbar `"You can pin up to 5 bookmarks."` and the pin attempt is rolled back.

### 24.11 Edge cases

a. **0 bookmarks** → empty state above.

b. **1 bookmark** → no pinned section (the carousel is hidden); a single-row
   "All" list. The page subtitle remains "Calls you've starred".

c. **5 pinned bookmarks** → the pin action in the action sheet is disabled with
   a tooltip `"Unpin one first."`.

d. **Drag during scroll** — when the user is mid-drag on a pinned item, the
   outer LazyColumn is locked from scrolling (we set `userScrollEnabled = false`
   for the duration of the drag).

e. **Bookmark removed mid-scroll** by another window of the app — Compose key
   on bookmarkId; the row animates out with a 220ms fade.

f. **First-bookmark prompt dismissed via system back** — treated as Skip;
   bookmark is saved without a reason.

g. **Reason text contains emojis** — supported (rendered with default emoji
   font). Emoji + RTL mix renders correctly (Compose default).

h. **Pinned bookmark refers to a deleted call** (the call row was hard-deleted
   via Manage→Clear) — entry is auto-pruned by `BookmarkRepository.observeAll`
   join semantics; a snackbar `"Removed a bookmark whose call no longer exists."`
   appears once per session.

i. **Configuration change while reorder drag is in progress** — drag is
   cancelled; pinned positions revert to last committed state.

### 24.12 Copy table

| Key                              | Copy                                                                         |
|----------------------------------|------------------------------------------------------------------------------|
| `bm_title`                       | Bookmarks                                                                    |
| `bm_subtitle`                    | Calls you've starred                                                         |
| `bm_back_cd`                     | Back                                                                         |
| `bm_pinned_section`              | Pinned                                                                       |
| `bm_all_section`                 | All                                                                          |
| `bm_pinned_badge`                | Top %1$d                                                                     |
| `bm_action_pin`                  | Pin                                                                          |
| `bm_action_unpin`                | Unpin                                                                        |
| `bm_action_remove`               | Remove                                                                       |
| `bm_action_open`                 | Open                                                                         |
| `bm_remove_confirm_title`        | Remove this bookmark?                                                        |
| `bm_remove_confirm_body`         | The call itself stays in your history.                                       |
| `bm_remove_confirm_yes`          | Remove                                                                       |
| `bm_remove_confirm_no`           | Cancel                                                                       |
| `bm_first_prompt_title`          | Why this call?                                                               |
| `bm_first_prompt_body`           | Add a quick reason so you remember later. (optional)                         |
| `bm_first_prompt_placeholder`    | e.g. "Quoted ₹42k for bulk order"                                             |
| `bm_first_prompt_save`           | Save                                                                         |
| `bm_first_prompt_skip`           | Skip                                                                         |
| `bm_first_prompt_counter`        | %1$d / 120                                                                   |
| `bm_pin_limit_snackbar`          | You can pin up to 5 bookmarks.                                               |
| `bm_pin_limit_tooltip`           | Unpin one first.                                                             |
| `bm_drag_arrow_up_cd`            | Move up                                                                      |
| `bm_drag_arrow_down_cd`          | Move down                                                                    |
| `bm_empty_title`                 | No bookmarks yet.                                                            |
| `bm_empty_body`                  | Star a call to save it here. Long-press any row in your call list to bookmark it. |
| `bm_error_title`                 | Couldn't load bookmarks.                                                     |
| `bm_error_body`                  | Try again.                                                                   |
| `bm_error_retry`                 | Retry                                                                        |
| `bm_pruned_snackbar`             | Removed a bookmark whose call no longer exists.                              |
| `bm_loading_cd`                  | Loading bookmarks                                                            |

### 24.13 ASCII wireframes

Default with pinned + all:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Bookmarks                                              │
│      Calls you've starred                          ⭐        │
├─────────────────────────────────────────────────────────────┤
│ Pinned (3)                                                  │
│ ┌──────┐ ┌──────┐ ┌──────┐                                  │
│ │ ╭R╮  │ │ ╭S╮  │ │ ╭#╮  │   horizontal carousel           │
│ │ Ravi │ │Sures │ │+9182 │                                  │
│ │ Top1 │ │Top 2 │ │Top 3 │                                  │
│ └──────┘ └──────┘ └──────┘                                  │
│                                                             │
│ All (12)                                                    │
│ ╭───╮ Ravi (Wholesale)                Apr 14 3:42 PM ★      │
│ │ R │ Quoted ₹42k for bulk order                            │
│ ╰───╯                                                       │
│ ╭───╮ Suresh                          Apr 13 11:00 AM ★     │
│ │ S │ —                                                     │
│ ╰───╯                                                       │
│ ...                                                         │
└─────────────────────────────────────────────────────────────┘
```

Empty:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Bookmarks                                              │
│      Calls you've starred                          ⭐        │
├─────────────────────────────────────────────────────────────┤
│                       ✦                                     │
│                  No bookmarks yet.                          │
│   Star a call to save it here. Long-press any row in        │
│   your call list to bookmark it.                            │
└─────────────────────────────────────────────────────────────┘
```

First-bookmark prompt:

```
┌── Why this call? ──────────────────────────────┐
│ Add a quick reason so you remember later.      │
│ ┌──────────────────────────────────────────┐   │
│ │ e.g. "Quoted ₹42k for bulk order"       │   │
│ └──────────────────────────────────────────┘   │
│                                       0 / 120  │
│           [ Skip ]        [ Save ]             │
└────────────────────────────────────────────────┘
```

Drag mode (pinned reorder):

```
Pinned (3) — tap arrows or drag to reorder
┌──────┐ ┌──────┐ ┌──────┐
│ ╭R╮  │ │ ╭S╮  │ │ ╭#╮  │
│ Ravi │ │Sures │ │+9182 │
│ ▲ ▼  │ │ ▲ ▼  │ │ ▲ ▼  │
└──────┘ └──────┘ └──────┘
```

### 24.14 Accessibility

- Pinned carousel items have `role = Button` and `contentDescription = "Pinned bookmark, position N: $name"`.
- Drag mode is keyboard-accessible via the up/down arrow buttons; their
  `onClick` calls the same reorder use case.
- Empty state's pulsing star respects `Settings.Global.ANIMATOR_DURATION_SCALE` —
  if scale is 0, the pulse is disabled.
- First-bookmark dialog uses `Modifier.semantics { isDialog = true }`.
- Star icons in rows have `contentDescription = "Bookmarked"`.

### 24.15 Performance budget

| Metric                            | Budget   |
|-----------------------------------|----------|
| First paint                       | ≤ 180 ms |
| Drag-to-drop reorder commit       | ≤ 50 ms  |
| Memory                            | ≤ 9 MB   |
| Bookmark add propagation to list  | ≤ 200 ms |

---

## §25 — FollowUps screen

`com.callvault.app.ui.screen.followups.FollowUpsScreen`

### 25.1 Purpose

FollowUps is the proactive-action page. It groups every scheduled reminder into
four buckets (Today / Overdue / Upcoming / Completed) and lets the user knock
through them one at a time.

The page is opinionated: the default tab on entry is **Overdue** if any overdue
follow-ups exist, otherwise **Today**, otherwise **Upcoming**. This means the
user always lands on the bucket that needs attention, not on an empty Today.

Bulk operations (snooze all / mark done / clear) are available via long-press
of any row, which enters multi-select.

### 25.2 Entry points

| Source                                     | Effect                  |
|--------------------------------------------|-------------------------|
| Library tab → Follow-ups list item         | navigate to `followUps` |
| Home shortcut tile "Follow-ups today"      | navigate to `followUps?tab=today` |
| Notification "3 follow-ups today" tap      | deep link to `followUps?tab=today` |
| Notification "Overdue follow-up" tap       | deep link to `followUps?tab=overdue` |
| CallDetail follow-up section "View all"    | navigate to `followUps` |

### 25.3 Exit points

- Back arrow → popBackStack.
- Tap a row → `callDetail/{normalizedNumber}`.
- Long-press → enters multi-select; "Done" button in app bar exits multi-select.
- Bulk Snooze all → BottomSheet (1h / 1d / pick…) → applies → snackbar.
- Bulk Mark done → applies `MarkFollowUpDoneUseCase` for each → snackbar.
- Bulk Clear → confirmation → applies `CancelFollowUpUseCase` for each.

### 25.4 Required inputs (data)

`FollowUpsViewModel` state:

| Field            | Type                          | Notes                              |
|------------------|-------------------------------|------------------------------------|
| `today`          | `List<FollowUpRow>`           | due today (00:00 ≤ due < 24:00 local)|
| `overdue`        | `List<FollowUpRow>`           | due < now and not done             |
| `upcoming`       | `List<FollowUpRow>`           | due > end of today and not done    |
| `completed`      | `List<FollowUpRow>`           | doneAt != null, sorted desc        |
| `selectedTab`    | `enum FollowUpTab`            | initialised by initial-tab logic   |
| `multiSelectIds` | `Set<Long>`                   | empty = single-select mode         |
| `isLoading`      | `Boolean`                     |                                    |

`FollowUpRow`:

| Field              | Type      |
|--------------------|-----------|
| `followUpId`       | `Long`    |
| `callId`           | `Long`    |
| `normalizedNumber` | `String`  |
| `displayName`      | `String?` |
| `dueAt`            | `Long`    |
| `snoozedFromAt`    | `Long?`   |
| `doneAt`           | `Long?`   |

Source: `callRepo.observeFollowUps()`. The use case classifies into the four
buckets in pure Kotlin.

### 25.5 Required inputs (user)

| Gesture                            | Effect                                  |
|------------------------------------|-----------------------------------------|
| Tap a tab                          | switches `selectedTab`                  |
| Tap a row (non-multi)              | navigate to CallDetail                  |
| Long-press a row                   | enters multi-select with that row checked |
| Tap a row (multi)                  | toggles its selection                   |
| Tap "Done" in app bar (multi)      | exits multi-select                      |
| Tap bulk Snooze (multi)            | bottom sheet → snooze all selected      |
| Tap bulk Mark done (multi)         | mark all selected done                  |
| Tap bulk Clear (multi)             | confirmation → cancel all selected      |
| Pull-to-refresh                    | re-fetch                                |
| Tap snooze icon on a single row    | mini-menu: 1h / 1d / pick…              |

### 25.6 Mandatory display elements

`StandardPage` with:
- Title `"Follow-ups"`
- Subtitle `"Reminders due today and ahead"`
- Header glyph 🔔

Below the header: `TabRow` with 4 tabs:
- Today (badge = count)
- Overdue (badge = count, red dot if > 0)
- Upcoming (badge = count)
- Completed (no badge)

Body — depending on `selectedTab`, a `LazyColumn` of follow-up rows. Each row:
- Leading: `NeoAvatar` (sm).
- Title: displayName or formattedNumber.
- Subtitle line 1: due date+time (e.g. "Today · 4:30 PM" or "Apr 22 · 9:00 AM").
- Subtitle line 2 (only if snoozed): `Snoozed from ${snoozedFromAt}` (italic).
- Trailing: snooze icon button + checkbox (in multi-select).

In multi-select, the top app bar swaps to a count + Done button + overflow
(Snooze all / Mark done / Clear).

### 25.7 Optional display elements

- **Trailing "in 3 hours" relative time hint** in the subtitle line.
- **Strikethrough** on title for completed-tab rows.
- **Soft red highlight** on overdue rows (background AccentRed @ 8% alpha).

### 25.8 Empty state per tab

| Tab        | Title (icon)                    | Body                                          |
|------------|---------------------------------|-----------------------------------------------|
| Today      | "All caught up for today!" 🎉   | "Future reminders will show up here."         |
| Overdue    | "No overdue follow-ups." ✓      | "Keep it up."                                 |
| Upcoming   | "Nothing scheduled." 📅          | "Schedule a follow-up from any call detail."  |
| Completed  | "No completed follow-ups." 📝   | "Done items will appear here."                |

Each empty state is a centered NeoEmptyState within the body slot; the TabRow
remains visible.

### 25.9 Loading state

- First load: 1 TabRow (with placeholder badges) + 5 row shimmers in the body.
- Pull-to-refresh: standard indicator.
- Tab switch: instant — all 4 lists are already in state.

### 25.10 Error state

- DB read failure: replace body with `NeoErrorState`. TabRow remains.
- Snooze/done/cancel use case failure: snackbar
  `"Couldn't update that follow-up."`; the row reverts.

### 25.11 Edge cases

a. **Follow-up at exactly midnight 00:00:00.** Belongs to Today (the day it
   begins). Belongs to Overdue once `now > dueAt`. Boundary handled by
   half-open intervals: `[startOfDay, startOfDay + 24h)`.

b. **Follow-up with no time (date only).** Defaults to 9:00 AM in the device
   local zone. Set in `ScheduleFollowUpUseCase` when `time == null`.

c. **`doneAt != null` but `dueAt` in the future.** Goes into Completed (the
   user marked it done early). Not "Upcoming". Verified via order-of-checks:
   `doneAt != null` first, then bucketize by `dueAt`.

d. **Snooze 1h on an overdue item that becomes due in <1h still in past.**
   Allowed; the new `dueAt = now + 1h` so the row leaves Overdue and lands in
   Today (or Upcoming if 1h crosses midnight).

e. **Snooze 1d on an item that's already 5 days overdue.** Sets `dueAt = now + 24h`.
   Not "1 day from original dueAt". Documented in spec §3.7.

f. **Bulk snooze with mixed buckets selected.** All selected items get the
   same snooze offset applied to *now*. Buckets recompute on next emit.

g. **Bulk clear of 50+ items.** Performed in a single Room transaction; the
   undo snackbar offers `"Undo"` for 8s with the count `"Cleared 53."`.

h. **Tab switched mid-multi-select.** Multi-select persists across tabs (the
   ids carry over).

i. **DST transition between snoozedFromAt and dueAt.** Display uses local zone
   formatter; no special handling.

### 25.12 Copy table

| Key                          | Copy                                                                |
|------------------------------|---------------------------------------------------------------------|
| `fu_title`                   | Follow-ups                                                          |
| `fu_subtitle`                | Reminders due today and ahead                                       |
| `fu_back_cd`                 | Back                                                                |
| `fu_tab_today`               | Today                                                               |
| `fu_tab_overdue`             | Overdue                                                             |
| `fu_tab_upcoming`            | Upcoming                                                            |
| `fu_tab_completed`           | Completed                                                           |
| `fu_due_today_at`            | Today · %1$s                                                        |
| `fu_due_tomorrow_at`         | Tomorrow · %1$s                                                     |
| `fu_due_at`                  | %1$s · %2$s                                                         |
| `fu_overdue_label`           | Overdue · %1$s                                                      |
| `fu_snoozed_from`            | Snoozed from %1$s                                                   |
| `fu_snooze_cd`               | Snooze                                                              |
| `fu_snooze_1h`               | 1 hour                                                              |
| `fu_snooze_1d`               | 1 day                                                               |
| `fu_snooze_pick`             | Pick a time…                                                        |
| `fu_done_cd`                 | Mark done                                                           |
| `fu_multi_count`             | %1$d selected                                                       |
| `fu_multi_done`              | Done                                                                |
| `fu_multi_snooze_all`        | Snooze all                                                          |
| `fu_multi_mark_done`         | Mark done                                                           |
| `fu_multi_clear`             | Clear                                                               |
| `fu_multi_clear_confirm_title` | Clear %1$d follow-ups?                                            |
| `fu_multi_clear_confirm_body` | This removes the reminders only — the calls stay.                  |
| `fu_multi_clear_confirm_yes` | Clear                                                               |
| `fu_multi_clear_confirm_no`  | Cancel                                                              |
| `fu_multi_cleared_snackbar`  | Cleared %1$d.                                                       |
| `fu_multi_cleared_undo`      | Undo                                                                |
| `fu_today_empty_title`       | All caught up for today!                                            |
| `fu_today_empty_body`        | Future reminders will show up here.                                 |
| `fu_overdue_empty_title`     | No overdue follow-ups.                                              |
| `fu_overdue_empty_body`      | Keep it up.                                                         |
| `fu_upcoming_empty_title`    | Nothing scheduled.                                                  |
| `fu_upcoming_empty_body`     | Schedule a follow-up from any call detail.                          |
| `fu_completed_empty_title`   | No completed follow-ups.                                            |
| `fu_completed_empty_body`    | Done items will appear here.                                        |
| `fu_error_title`             | Couldn't load follow-ups.                                           |
| `fu_error_body`              | Try again.                                                          |
| `fu_error_retry`             | Retry                                                               |
| `fu_action_failed_snackbar`  | Couldn't update that follow-up.                                     |

### 25.13 ASCII wireframes

Default — Today tab:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   Follow-ups                                             │
│      Reminders due today and ahead                  🔔      │
├─────────────────────────────────────────────────────────────┤
│ [Today (3)] [Overdue (1●)] [Upcoming (8)] [Completed]      │
├─────────────────────────────────────────────────────────────┤
│ ╭───╮ Ravi (Wholesale)                Today · 4:30 PM   🔔  │
│ │ R │                                                       │
│ ╰───╯                                                       │
│ ╭───╮ Suresh                          Today · 6:00 PM   🔔  │
│ │ S │ Snoozed from 2:00 PM                                  │
│ ╰───╯                                                       │
│ ╭───╮ +91 99000 11111                 Today · 8:30 PM   🔔  │
│ │ # │                                                       │
│ ╰───╯                                                       │
└─────────────────────────────────────────────────────────────┘
```

Overdue tab with one item:

```
┌────────────────────────────────────────────┐
│ [Today] [Overdue (1●)*] [Upcoming] [Done] │
├────────────────────────────────────────────┤
│ ╭───╮ Anil  (red bg)   Overdue · 2 days   │
│ │ A │                                  🔔 │
│ ╰───╯                                     │
└────────────────────────────────────────────┘
```

Multi-select:

```
┌─────────────────────────────────────────────┐
│ ←  3 selected   [Snooze all][Done][Clear]✕ │
├─────────────────────────────────────────────┤
│ ☑ Ravi          Today · 4:30 PM             │
│ ☑ Suresh        Today · 6:00 PM             │
│ ☐ +91 99000     Today · 8:30 PM             │
│ ☑ Anil          Overdue · 2 days            │
└─────────────────────────────────────────────┘
```

Empty Today:

```
┌────────────────────────────────────────────┐
│ [Today] [Overdue] [Upcoming] [Completed]  │
├────────────────────────────────────────────┤
│                  🎉                        │
│        All caught up for today!            │
│   Future reminders will show up here.     │
└────────────────────────────────────────────┘
```

### 25.14 Accessibility

- TabRow exposes proper `selected` semantics; tab labels include the badge
  count ("Today, 3 due").
- Each row has `role = Button` (single-tap mode) or `role = Checkbox` (multi-select mode), switched at runtime.
- Snooze icon button has explicit `contentDescription = "Snooze ${displayName} for 1 hour"` after a choice is committed.
- Empty-state icons are `decorative = true`; emoji titles are spoken.
- Multi-select count is announced via `LiveRegion.Polite` on transitions.

### 25.15 Performance budget

| Metric                       | Budget   |
|------------------------------|----------|
| First paint                  | ≤ 200 ms |
| Tab switch                   | < 16 ms  |
| Bulk action of 50 items      | ≤ 350 ms |
| Memory                       | ≤ 10 MB  |

---

## §26 — MyContacts screen

`com.callvault.app.ui.screen.contacts.MyContactsScreen`

### 26.1 Purpose

MyContacts shows the user's *human-saved* contacts — i.e. people they have
explicitly chosen to put in their phone book. Auto-saved inquiry numbers
(`isAutoSaved == true`) are explicitly excluded; those live under "All people"
in the Library tab. The page exists so the user has a quick lens on "who matters
most" without the noise of every cold inquiry that CallVault auto-promoted.

The selector is `isInSystemContacts == true && isAutoSaved == false`.

A small inline "promoted from inquiry" badge appears on contacts whose
`autoSavedAt` is non-null but who have *also* been promoted manually since (i.e.
auto-saved first, then later edited / re-saved through Contacts).

### 26.2 Entry points

| Source                                  | Effect                  |
|-----------------------------------------|-------------------------|
| Library tab → My Contacts list item     | navigate to `myContacts`|
| Home shortcut tile "My contacts"        | navigate to `myContacts`|
| Settings → "Manage contact group"       | navigate to `myContacts`|

### 26.3 Exit points

- Back arrow → popBackStack.
- Tap a contact row → `callDetail/{normalizedNumber}`.
- Long-press a contact row → action sheet (Open · Open in Phone book · Copy number).

### 26.4 Required inputs (data)

`MyContactsViewModel` state:

| Field         | Type                  | Notes                                       |
|---------------|-----------------------|---------------------------------------------|
| `query`       | `String`              | filter; debounced 200ms                     |
| `contacts`    | `List<ContactRow>`    | filtered; sorted by displayName ASC, locale |
| `isLoading`   | `Boolean`             |                                             |
| `isError`     | `String?`             |                                             |

`ContactRow`:

| Field              | Type       |
|--------------------|------------|
| `normalizedNumber` | `String`   |
| `displayName`      | `String`   | non-null (rows without name skipped)        |
| `formattedNumber`  | `String`   |                                             |
| `avatarSeed`       | `String`   |                                             |
| `wasPromoted`      | `Boolean`  | `autoSavedAt != null`                       |

Source: `contactsRepo.observeMyContacts()` filtered by selector above.

### 26.5 Required inputs (user)

| Gesture                  | Effect                                           |
|--------------------------|--------------------------------------------------|
| Type into search field   | filters list                                     |
| Tap × in search          | clears query                                     |
| Tap a contact row        | navigate to CallDetail                           |
| Long-press a contact row | action sheet (Open · Open in Phone book · Copy)  |
| Pull-to-refresh          | resync from Contacts provider                    |
| Scroll                   | normal scroll                                    |

### 26.6 Mandatory display elements

`StandardPage`:
- Title `"My Contacts"`
- Subtitle `"People you've saved"`
- Header glyph 👥

Body:

1. **Search field** — inline, 48dp tall, full-width, rounded. Leading `Search`
   icon, trailing `Clear` icon when non-empty. Placeholder `"Filter by name or number"`.

2. **Contact list** — `LazyColumn` of `ContactRow`s, each:
   - Leading: `NeoAvatar` (md) deterministic-colored.
   - Title: displayName (`Typography.titleSmall`).
   - Subtitle: formattedNumber (`Typography.bodySmall`, secondary).
   - Trailing: small text "promoted from inquiry" badge if `wasPromoted == true`.

   Sticky alphabetical headers (Compose `stickyHeader`) for letters A..Z, '#' for non-letter prefixes.

### 26.7 Optional display elements

- **Section count** in the subtitle: `"People you've saved · 124"`.
- **First-letter quick scrubber** on the right edge — Phase II.
- **Avatar deduplication** — when 2+ contacts share an initial, the seed is
  still per-number so colors differ.

### 26.8 Empty state

When `contacts.isEmpty()` (and query is empty):

- Centered NeoEmptyState: icon 👥, title `"No contacts yet."`, body
  `"CallVault auto-saves new inquiries to your phone. Once you've saved someone in your phone book, they'll appear here."`. No CTA — saving happens elsewhere.

When query is non-empty and yields nothing:

- Centered: title `"No contacts match \"$query\""`, body
  `"Try a shorter name or number."`.

### 26.9 Loading state

- 8 row shimmers + 1 search field placeholder.
- Pull-to-refresh: standard indicator.

### 26.10 Error state

- Contacts permission revoked: full-screen `NeoErrorState`, icon `LockOutline`,
  title `"Contacts access was turned off."`, body `"Re-grant permission to see your saved contacts here."`, button `"Grant permission"`.
- Provider read failure: `NeoErrorState` with `"Couldn't load contacts."` + Retry.

### 26.11 Edge cases

a. **1000+ contacts** — `LazyColumn` keys on `normalizedNumber`; sticky headers
   are O(1) per header; no full re-sort on scroll. First paint stays under
   budget.

b. **Contact without a phone number** — skipped (we have nothing to navigate
   to). A footer line `"Hidden N contacts without a phone number."` appears below
   the list.

c. **Name with emoji** (e.g. "Ravi 🌟") — rendered correctly; sticky header is
   computed from the *first letter character* of the unicode-stripped name. If
   the name *starts* with an emoji, the header is `'#'`.

d. **Name with RTL chars** (Hebrew/Arabic) — Compose default LTR/RTL handling
   applies; sticky header uses the first strong directional letter.

e. **Two contacts with same normalized number** — Contacts provider sometimes
   returns duplicates after sync conflicts. We dedupe by `normalizedNumber`
   keeping the first (displayName-wise) occurrence; the rest go into a
   "Duplicates" footer list (Phase II will offer merge).

f. **Contact provider permission lost mid-session** — observer detects
   `SecurityException`; ViewModel transitions to error state.

g. **Search query is a number partial** — matches against both `formattedNumber`
   (with stripped formatting) and `displayName`.

h. **Search query exact-matches one contact** — that row scrolls into view if
   off-screen.

i. **Configuration change while scrolled deep** — `LazyListState` is
   `rememberSaveable`; position is preserved.

### 26.12 Copy table

| Key                          | Copy                                                                            |
|------------------------------|---------------------------------------------------------------------------------|
| `mc_title`                   | My Contacts                                                                     |
| `mc_subtitle`                | People you've saved                                                             |
| `mc_subtitle_count`          | People you've saved · %1$d                                                      |
| `mc_back_cd`                 | Back                                                                            |
| `mc_search_placeholder`      | Filter by name or number                                                        |
| `mc_search_clear_cd`         | Clear filter                                                                    |
| `mc_promoted_badge`          | promoted from inquiry                                                           |
| `mc_action_open`             | Open                                                                            |
| `mc_action_open_phonebook`   | Open in Phone book                                                              |
| `mc_action_copy`             | Copy number                                                                     |
| `mc_copied_snackbar`         | Copied                                                                          |
| `mc_hidden_no_phone_footer`  | Hidden %1$d contacts without a phone number.                                    |
| `mc_empty_title`             | No contacts yet.                                                                |
| `mc_empty_body`              | CallVault auto-saves new inquiries to your phone. Once you've saved someone in your phone book, they'll appear here. |
| `mc_no_match_title`          | No contacts match "%1$s"                                                        |
| `mc_no_match_body`           | Try a shorter name or number.                                                   |
| `mc_error_perm_title`        | Contacts access was turned off.                                                 |
| `mc_error_perm_body`         | Re-grant permission to see your saved contacts here.                            |
| `mc_error_perm_button`       | Grant permission                                                                |
| `mc_error_load_title`        | Couldn't load contacts.                                                         |
| `mc_error_load_body`         | Try again.                                                                      |
| `mc_error_load_retry`        | Retry                                                                           |

### 26.13 ASCII wireframes

Default:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   My Contacts                                            │
│      People you've saved · 124                       👥      │
├─────────────────────────────────────────────────────────────┤
│ ┌─ Filter by name or number ───────────────── 🔍 ┐          │
│ └─────────────────────────────────────────────────┘          │
├─────────────────────────────────────────────────────────────┤
│ A                                                           │
│ ╭───╮ Anil Kumar                  +91 90000 11111           │
│ │ A │                                                       │
│ ╰───╯                                                       │
│ ╭───╮ Anjali  (promoted from inquiry)  +91 90000 22222      │
│ │ A │                                                       │
│ ╰───╯                                                       │
│ R                                                           │
│ ╭───╮ Ravi (Wholesale)             +91 98765 43210          │
│ │ R │                                                       │
│ ╰───╯                                                       │
│ ...                                                         │
│ Hidden 3 contacts without a phone number.                   │
└─────────────────────────────────────────────────────────────┘
```

Filtered:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   My Contacts                                            │
├─────────────────────────────────────────────────────────────┤
│ ┌─ rav ─────────────────────────────────────── × ┐          │
│ └─────────────────────────────────────────────────┘          │
│ ╭───╮ Ravi (Wholesale)             +91 98765 43210          │
│ │ R │                                                       │
│ ╰───╯                                                       │
│ ╭───╮ Ravi Mehta                   +91 90000 33333          │
│ │ R │                                                       │
│ ╰───╯                                                       │
└─────────────────────────────────────────────────────────────┘
```

No matches:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   My Contacts                                            │
├─────────────────────────────────────────────────────────────┤
│ ┌─ qzx ─────────────────────────────────────── × ┐          │
│ └─────────────────────────────────────────────────┘          │
│                                                             │
│             No contacts match "qzx"                         │
│         Try a shorter name or number.                       │
└─────────────────────────────────────────────────────────────┘
```

Permission error:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   My Contacts                                            │
├─────────────────────────────────────────────────────────────┤
│                       🔒                                    │
│         Contacts access was turned off.                     │
│   Re-grant permission to see your saved contacts here.      │
│              [  Grant permission  ]                         │
└─────────────────────────────────────────────────────────────┘
```

Empty:

```
┌─────────────────────────────────────────────────────────────┐
│  ←   My Contacts                                            │
├─────────────────────────────────────────────────────────────┤
│                       👥                                    │
│                  No contacts yet.                           │
│   CallVault auto-saves new inquiries to your phone.         │
│   Once you've saved someone in your phone book,             │
│   they'll appear here.                                      │
└─────────────────────────────────────────────────────────────┘
```

### 26.14 Accessibility

- Each row exposes `contentDescription = "${displayName}, ${spelledNumber}${if (wasPromoted) ", promoted from inquiry"}"`.
- Sticky header has `heading()` semantics so TalkBack announces "Section A".
- Search field uses `imeAction = ImeAction.Search`; pressing Enter dismisses
  the keyboard.
- Contrast: promoted badge uses the warning-tinted token which is AA on the
  TabBgLibrary background.
- Permission error CTA has a clear hint about what tapping it does (opens
  system settings).

### 26.15 Performance budget

| Metric                            | Budget   |
|-----------------------------------|----------|
| First paint (≤500 contacts)       | ≤ 220 ms |
| First paint (5000 contacts)       | ≤ 600 ms |
| Search filter recompute           | ≤ 30 ms  |
| Memory                            | ≤ 14 MB  |
| Scroll fps                        | 60 p95   |

The list is filtered in-memory (we hold all contacts in `StateFlow`); scaling
beyond ~20k contacts would warrant a SQLite-backed page; outside scope for v1.0.

---

## Cross-references

- **Shared `CallRow`** used by §22, §24, §25 — see Part 06 Appendix C.
- **`NeoCard`, `NeoButton`, `NeoChip`, `NeoIconButton`, `NeoAvatar`,
  `NeoSearchBar`, `NeoEmptyState`, `NeoErrorState`, `NeoDialog`** — Part 06
  Appendix A.
- **`StandardPage` scaffold** — Part 06 Appendix B.
- **Color tokens** (`TabBgCalls`, `TabBgLibrary`, `TabBgStats`, `AccentBlue`,
  `AccentGreen`, `AccentRed`, `AccentAmber`, `AccentPurple`) — Part 06 Appendix D.
- **Formatters** (`PhoneFormatter`, `DateTimeFormatter`, `DurationFormatter`,
  `RelativeTimeFormatter`) — Part 06 Appendix E.
- **`avatarSeed` → HSL hash** — Part 06 Appendix F.
- **`MarkdownRenderer` rules** — Part 06 Appendix M.
- **Use cases**: `LeadScoreUseCase`, `NumberStatsUseCase`,
  `BuildStatsSnapshotUseCase`, `GenerateInsightsUseCase`,
  `ScheduleFollowUpUseCase`, `CancelFollowUpUseCase`, `MarkFollowUpDoneUseCase`,
  `RemoveTagUseCase`, `DeleteNoteUseCase`, `ClearNumberDataUseCase` — Part 05 §5.x.
- **DAOs**: `CallDao`, `NoteDao`, `TagDao`, `BookmarkDao`, `FollowUpDao`,
  `SearchHistoryDao` — Part 06 §6.x.
- **Phase I.2 changes** that affect this part: top-bar overflow removal, manage
  section addition, deep-link route normalization. See `CHANGELOG.md` v0.18.x.

---

## Implementation notes

- All scrolling surfaces use a single `LazyColumn`; no nested scrollables. The
  pinned-bookmarks carousel uses `Modifier.horizontalScroll` inside a
  non-lazy item to keep the item count predictable.
- Stats charts are *Compose Canvas*, not Vico, despite Vico being on the
  dependency list. We started with Vico in the Stats sprint and ran into
  layout-pass thrash on the heatmap; Canvas was the path of least resistance.
  Documented in `DECISIONS.md` D-024.
- The CallDetail page uses `rememberLazyListState()` keyed on the
  `normalizedNumber` so navigating from CallA → CallB → back preserves CallA's
  scroll position correctly.
- `MarkdownRenderer` is a 200-line in-house implementation; we explicitly avoid
  Markwon to keep the dependency footprint small. It supports: `**bold**`,
  `*italic*`, `- bullet`, `1. ordered`, `[text](url)`, inline code, and
  headings up to `##`. Anything else renders as plaintext.
- The Stats range chip's persistence: the last selected range is saved to
  DataStore (`stats.last_range`) so the user doesn't have to re-pick on every
  visit.
- The FollowUps initial-tab logic runs *once* per cold start; subsequent visits
  in the same session honor whatever tab the user last left.
- The SearchScreen does not save its query across navigations. This is
  intentional — Search is "scratch space".

---

## Sprint pointers (where this code lives now)

| Page          | Screen file                                                | ViewModel                                                |
|---------------|------------------------------------------------------------|----------------------------------------------------------|
| CallDetail    | `ui/screen/detail/CallDetailScreen.kt`                     | `ui/screen/detail/CallDetailViewModel.kt`                |
| Search        | `ui/screen/search/SearchScreen.kt`                         | `ui/screen/search/SearchViewModel.kt`                    |
| Stats         | `ui/screen/stats/StatsScreen.kt`                           | `ui/screen/stats/StatsViewModel.kt`                      |
| Bookmarks     | `ui/screen/bookmarks/BookmarksScreen.kt`                   | `ui/screen/bookmarks/BookmarksViewModel.kt`              |
| FollowUps     | `ui/screen/followups/FollowUpsScreen.kt`                   | `ui/screen/followups/FollowUpsViewModel.kt`              |
| MyContacts    | `ui/screen/contacts/MyContactsScreen.kt`                   | `ui/screen/contacts/MyContactsViewModel.kt`              |

End of Part 04. See Part 05 for: Tag picker, Note editor, Tag manager, Settings,
About, Update flow, Backup/restore, Onboarding, In-app docs, Floating in-call
bubble, Post-call popup.
