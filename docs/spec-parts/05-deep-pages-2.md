# callNest APP-SPEC — Part 05: Deep pages 2

> Tags · Auto-tag rules · Rule editor · Backup · Export wizard · Quick-export sheet
>
> Audience: a UX engineer rebuilding callNest from scratch with no prior context.
> This document is intentionally self-contained: every page lists its inputs,
> outputs, copy strings, ASCII wireframes, accessibility notes, and a
> performance budget.

---

## How to read this part

Each page below uses the canonical 15-subsection template that runs through
parts 02, 03, and 04 of the spec:

1. Purpose
2. Entry points
3. Exit points
4. Required inputs (data)
5. Required inputs (user)
6. Mandatory display
7. Optional display
8. Empty state
9. Loading state
10. Error state
11. Edge cases
12. Copy table
13. ASCII wireframe
14. Accessibility
15. Performance budget

Cross references in this part:

- `BackupManager` algorithm — see Part 01 §6.9.
- `RuleConditionEvaluator` — see Part 01 §6.4.
- `LeadScoreCalculator` formula — see Part 01 §6.7.
- `FilterState` model — see Part 02 §15.
- `NeoScaffold`, `StandardPage`, `NeoCard`, `NeoButton`, `NeoTextField`,
  `NeoBottomSheet`, `NeoLoader`, `NeoProgressBar`, `ColoredChip` — see
  Part 04 (component catalogue).
- `AutoTagRuleRepository` — see Part 01 §7.3.
- `TagRepository` — see Part 01 §7.2.

Implementation notes for spec deviations are flagged inline with the
prefix `> NOTE:`.

---

## 27 — TagsManagerScreen

### 27.1 Purpose

The TagsManagerScreen is the central place a user goes to organise the
vocabulary they apply to calls. A tag in callNest is a small, coloured,
emoji-prefixed label that a user — or an auto-tag rule — attaches to a
`CallEntity` row. Tags drive filtering on the Calls list, scoping on the
Stats screen, and routing in the auto-tag rules engine.

This screen lets the user:

- Browse every tag in the system in a single scrollable list.
- See, at a glance, how many calls each tag is currently applied to.
- Rename a tag.
- Recolour a tag (pick from a curated palette).
- Change a tag's emoji prefix.
- Merge two tags into one (cascading the merge across every call row
  and every rule that references the source tag).
- Delete a user-created tag.
- Create a brand-new user tag.

System tags (the nine seeded on first DB create) cannot be deleted, but
they CAN be renamed, recoloured, and re-emojied. The intent is that a
user can take a "Vendor" system tag, rename it to "Supplier", and the
underlying `tagId` stays stable so existing call assignments continue
to work.

### 27.2 Entry points

The TagsManagerScreen can be reached from any of the following points:

| From              | Trigger                                                                                 |
| ----------------- | --------------------------------------------------------------------------------------- |
| MoreScreen        | Tapping the "Tags" row under the "Organise" section.                                    |
| FilterSheet       | Tapping the "Manage tags…" link at the bottom of the tag chip group.                    |
| CallDetailsScreen | Tapping the pencil icon next to the tag chips, then tapping "Manage tags…" in the menu. |
| RuleEditor screen | Tapping the gear icon next to a tag picker dropdown.                                    |
| Onboarding tour   | The "Tags" step has a "Take a look" button.                                             |
| Deep link         | `callNest://tags` (used by the in-app docs cross-links).                                |

### 27.3 Exit points

| To                                     | Trigger                                         |
| -------------------------------------- | ----------------------------------------------- |
| Previous screen                        | Hardware back / nav-bar back arrow.             |
| TagEditorDialog (modal, in-screen)     | Tap a row, tap FAB, or tap a row's pencil icon. |
| MergeIntoDialog (modal, in-screen)     | Long-press a row.                               |
| DeleteConfirmDialog (modal, in-screen) | Swipe-left to delete (only if usage > 0).       |
| Snackbar (transient, stays on screen)  | Successful save / merge / delete.               |
| Toast (transient)                      | Attempt to delete a system tag.                 |

### 27.4 Required inputs (data)

The screen subscribes to a single `StateFlow<TagsManagerUiState>`
exposed by `TagsManagerViewModel`. The state shape:

```
data class TagsManagerUiState(
    val isLoading: Boolean,
    val tags: List<TagWithCount>,
    val searchQuery: String,
    val error: String?,
)

data class TagWithCount(
    val id: Long,
    val name: String,
    val colorHex: String,           // e.g. "#FF6B6B"
    val emoji: String,              // single grapheme cluster
    val isSystem: Boolean,
    val callCount: Int,             // computed via CallTagDao.countCallsForTag(id)
)
```

The ViewModel composes this state from:

- `TagRepository.observeAllTags(): Flow<List<TagEntity>>`
- `CallTagDao.observeCountsByTag(): Flow<Map<Long, Int>>` (joined on tag id)

Both flows are combined via `combine { … }` and emitted on
`Dispatchers.Default`. The ViewModel applies the search filter
client-side because the tag list is bounded (typically <100 entries).

### 27.5 Required inputs (user)

The user can:

- Type into the search field at the top.
- Tap a row to open `TagEditorDialog` for that tag.
- Long-press a row to open `MergeIntoDialog`.
- Swipe a row left to reveal the destructive "Delete" action.
- Tap the FAB (`+`) to create a new tag.
- Tap a tag's pencil/gear inline icon (synonym for tapping the row).
- Use the toolbar overflow → "Reset to defaults" (only resets the
  nine system tags' name/colour/emoji; never affects user tags).

### 27.6 Mandatory display

The screen body is wrapped in a `StandardPage` composable:

```
StandardPage(
    title = "Tags",
    description = "Categories for your calls",
    emoji = "🏷️",
)
```

Inside the page body, top-to-bottom:

1. A `NeoTextField` filter input with placeholder
   `"Search tags…"` and a clear-X icon when non-empty. 8 dp top padding.
2. A horizontal hairline divider (`BorderSoft`, 1 dp).
3. A `LazyColumn` of tag rows. Each row is laid out as:
   ```
   Row(
       modifier = Modifier
           .fillMaxWidth()
           .height(64.dp)
           .clickable { … },
       verticalAlignment = Alignment.CenterVertically,
   ) {
       Spacer(Modifier.width(16.dp))
       ColoredChip(
           text = "${tag.emoji} ${tag.name}",
           backgroundColor = Color(parseColor(tag.colorHex)),
           textColor = chooseContrastColor(tag.colorHex),
       )
       Spacer(Modifier.weight(1f))
       Text(
           text = "${tag.callCount} calls",
           style = MaterialTheme.typography.labelMedium,
           color = MaterialTheme.colorScheme.onSurfaceVariant,
       )
       Spacer(Modifier.width(8.dp))
       Icon(Icons.Default.ChevronRight, contentDescription = null)
       Spacer(Modifier.width(16.dp))
   }
   ```
4. A trailing FAB anchored to the screen's bottom-right corner with a
   `+` icon, label `"New tag"` (visually hidden, used by talkback).

The nine system tags are always present at the top of the list (they
sort first by `isSystem desc`, then by `name asc`). User tags follow,
sorted by `name asc`.

### 27.7 Optional display

These elements are shown when the corresponding state holds:

- An inline "9 system tags" hint as a `Text` in `labelSmall` between
  the search field and the list, only when `searchQuery.isBlank() &&
tags.size >= 9`.
- A "No matches" hint inline (not a full empty state) when
  `searchQuery.isNotBlank() && filteredTags.isEmpty()`. Renders inside
  the `LazyColumn` as a centred `Text` with 32 dp vertical padding and
  the copy `"No tags match \"{query}\""`.
- A subtle shimmer skeleton on the row's count badge while
  `CallTagDao.observeCountsByTag()` has not emitted yet. The chip
  itself renders immediately from the tag stream.

### 27.8 Empty state

The TagsManagerScreen should never be empty in a healthy install — the
nine system tags are seeded on first DB create via `TagSeeder.seed()` in
`AppDatabase.Callback.onCreate()`. If the list is observed empty for
any reason (e.g. a corrupted DB after a failed restore), the screen
shows:

```
StandardEmpty(
    emoji = "🏷️",
    title = "No tags yet",
    body = "Tap + to create your first tag.",
    primaryCta = NeoButton("Create tag") { openEditor(null) },
)
```

> NOTE: We deliberately do not auto-reseed here — re-seeding in the UI
> layer would mask the underlying corruption. Instead, surface the
> error path via the `BackupRestoreScreen` which has explicit reseed
> tools.

### 27.9 Loading state

While `uiState.isLoading == true`:

- The search field renders disabled (text colour
  `colorScheme.onSurfaceVariant`, alpha 0.6).
- The list area renders a `LazyColumn` of 6 `ShimmerRow` placeholders,
  each 64 dp tall, with a chip-shape shimmer block + count shimmer.
- The FAB is hidden (not just disabled — fully hidden, because tapping
  it before tags load would race the list).

The loading state is transient — it should resolve in <300 ms under
the performance budget below.

### 27.10 Error state

If the `TagRepository.observeAllTags()` flow emits an error:

- The list region renders a `StandardError` composable with copy
  `"Couldn't load your tags. Pull down or tap retry."` plus a primary
  `NeoButton("Retry")` that re-invokes the flow.
- The FAB stays hidden until tags load successfully, because the
  Editor dialog cannot persist without the parent list.
- Long-tap on the error region copies the underlying exception's
  `localizedMessage` to clipboard (debug-only; release builds skip).

### 27.11 Edge cases

| Case                                 | Handling                                                                                                                                                                                                                                                                                                        |
| ------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Deleting a tag applied to 1000 calls | Show `DeleteConfirmDialog` with copy `"This will remove the tag from 1,000 calls. Continue?"`. On confirm, delete is performed in a single Room transaction `(DELETE FROM call_tag_cross_ref WHERE tag_id = ?; DELETE FROM tag WHERE id = ?)`. UI shows a `NeoLoader` overlay until the transaction commits.    |
| Merge target = source                | Block at validation time. The `MergeIntoDialog` greys out the source tag from its picker; if the user somehow selects it (e.g. screen reader), the Save button stays disabled with copy `"Pick a different tag to merge into."`.                                                                                |
| Rename to an existing name           | Block at validation. The `TagEditorDialog`'s Save button is disabled and a helper text below the name field reads `"A tag named \"{name}\" already exists."` (case-insensitive compare, normalised whitespace).                                                                                                 |
| Emoji selection                      | Two paths: (a) a curated grid of 32 business emojis (see table below), (b) a free-text single-grapheme input that accepts any single emoji or unicode grapheme cluster. Validate via `androidx.emoji2.text.EmojiCompat.get().getEmojiMatch()`. Reject multi-codepoint inputs that don't form a single grapheme. |
| Deleting a system tag                | Toast: `"System tags can't be deleted. You can rename or recolour them instead."`                                                                                                                                                                                                                               |
| Concurrent edit (two devices)        | Last-write-wins. The repo uses `OnConflictStrategy.REPLACE` keyed on `id`.                                                                                                                                                                                                                                      |
| Tag colour invalid hex               | Default to `#888888`; log a `Timber.w` and store the corrected value back.                                                                                                                                                                                                                                      |
| Long tag name (>30 chars)            | Truncate with ellipsis on the row; full name shown in the editor. Save is blocked at >40 chars with helper text `"Keep tag names under 40 characters."`.                                                                                                                                                        |
| Search query containing emoji        | Match against both `name` and `emoji` fields, case-folded.                                                                                                                                                                                                                                                      |

The 32 curated business emojis (used in the editor's emoji grid):

| #   | Emoji | Suggested use     |
| --- | ----- | ----------------- |
| 1   | 🏷️    | Generic tag       |
| 2   | 📞    | Inbound call      |
| 3   | 📲    | Outbound call     |
| 4   | 💬    | Discussion        |
| 5   | 💼    | Business          |
| 6   | 💰    | Revenue           |
| 7   | 💸    | Refund / loss     |
| 8   | 🛒    | Order             |
| 9   | 📦    | Fulfilment        |
| 10  | 🚚    | Shipping          |
| 11  | 🧾    | Invoice           |
| 12  | 📝    | Note / quoted     |
| 13  | 📌    | Pinned            |
| 14  | ⭐    | VIP               |
| 15  | ⚠️    | Warning           |
| 16  | 🚫    | Spam              |
| 17  | 🛠️    | Vendor / supplier |
| 18  | 🤝    | Partner           |
| 19  | 👤    | Personal          |
| 20  | 👥    | Customer          |
| 21  | 🏢    | Company           |
| 22  | 📍    | Location          |
| 23  | 🕐    | Follow-up         |
| 24  | ✅    | Closed-won        |
| 25  | ❌    | Closed-lost       |
| 26  | 🔁    | Recurring         |
| 27  | 🎯    | Hot lead          |
| 28  | 🧊    | Cold lead         |
| 29  | 🔥    | Urgent            |
| 30  | 📅    | Scheduled         |
| 31  | 🎉    | Celebrate         |
| 32  | ❓    | Unknown           |

The nine seeded system tags:

| #   | Name        | Emoji | Default colour | Purpose                            |
| --- | ----------- | ----- | -------------- | ---------------------------------- |
| 1   | Inquiry     | ❓    | `#3B82F6`      | Default for unknown inbound calls. |
| 2   | Customer    | 👥    | `#10B981`      | Returning buyer.                   |
| 3   | Vendor      | 🛠️    | `#F59E0B`      | Supplier or service provider.      |
| 4   | Personal    | 👤    | `#8B5CF6`      | Family / friends.                  |
| 5   | Spam        | 🚫    | `#EF4444`      | Telemarketing / scam.              |
| 6   | Follow-up   | 🕐    | `#06B6D4`      | Needs callback.                    |
| 7   | Quoted      | 📝    | `#F97316`      | Quote sent.                        |
| 8   | Closed-won  | ✅    | `#22C55E`      | Deal won.                          |
| 9   | Closed-lost | ❌    | `#94A3B8`      | Deal lost.                         |

### 27.12 Copy table

| Key                   | Resource id                 | English                                                                         |
| --------------------- | --------------------------- | ------------------------------------------------------------------------------- |
| Title                 | `tags_title`                | Tags                                                                            |
| Subtitle              | `tags_subtitle`             | Categories for your calls                                                       |
| Search placeholder    | `tags_search_hint`          | Search tags…                                                                    |
| Count suffix          | `tags_count_suffix`         | %1$d calls                                                                      |
| FAB label             | `tags_new_fab_label`        | New tag                                                                         |
| FAB content desc      | `tags_new_fab_a11y`         | Create a new tag                                                                |
| Empty title           | `tags_empty_title`          | No tags yet                                                                     |
| Empty body            | `tags_empty_body`           | Tap + to create your first tag.                                                 |
| Empty CTA             | `tags_empty_cta`            | Create tag                                                                      |
| Error body            | `tags_error_body`           | Couldn't load your tags. Pull down or tap retry.                                |
| Error CTA             | `tags_error_cta`            | Retry                                                                           |
| Delete confirm title  | `tags_delete_title`         | Delete this tag?                                                                |
| Delete confirm body   | `tags_delete_body`          | This will remove the tag from %1$d calls. Continue?                             |
| Delete confirm cta    | `tags_delete_confirm`       | Delete                                                                          |
| Delete cancel         | `tags_delete_cancel`        | Cancel                                                                          |
| Cannot delete system  | `tags_cannot_delete_system` | System tags can't be deleted. You can rename or recolour them instead.          |
| Editor title (new)    | `tag_editor_title_new`      | New tag                                                                         |
| Editor title (edit)   | `tag_editor_title_edit`     | Edit tag                                                                        |
| Editor name label     | `tag_editor_name_label`     | Name                                                                            |
| Editor emoji label    | `tag_editor_emoji_label`    | Emoji                                                                           |
| Editor colour label   | `tag_editor_color_label`    | Colour                                                                          |
| Editor save           | `tag_editor_save`           | Save                                                                            |
| Editor cancel         | `tag_editor_cancel`         | Cancel                                                                          |
| Validation: empty     | `tag_editor_err_empty`      | Give your tag a name.                                                           |
| Validation: too long  | `tag_editor_err_too_long`   | Keep tag names under 40 characters.                                             |
| Validation: duplicate | `tag_editor_err_duplicate`  | A tag named "%1$s" already exists.                                              |
| Validation: bad emoji | `tag_editor_err_bad_emoji`  | Pick a single emoji.                                                            |
| Merge dialog title    | `tag_merge_title`           | Merge into…                                                                     |
| Merge dialog body     | `tag_merge_body`            | All %1$d calls tagged "%2$s" will move to the chosen tag. This can't be undone. |
| Merge dialog cta      | `tag_merge_cta`             | Merge                                                                           |
| Merge done snackbar   | `tag_merge_snack`           | Merged "%1$s" into "%2$s".                                                      |
| Saved snackbar        | `tag_saved_snack`           | Tag saved.                                                                      |
| Deleted snackbar      | `tag_deleted_snack`         | Tag deleted.                                                                    |
| Reset overflow        | `tags_reset_overflow`       | Reset system tags                                                               |
| Reset confirm         | `tags_reset_confirm`        | Restore the original 9 system tags? Your custom tags won't be touched.          |

### 27.13 ASCII wireframe

```
┌──────────────────────────────────────────────────┐
│ ←  Tags                                       ⋮  │
├──────────────────────────────────────────────────┤
│                                                  │
│  🏷️                                              │
│  Tags                                            │
│  Categories for your calls                       │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ 🔍  Search tags…                       ✕ │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  9 system tags                                   │
│  ────────────────────────────────────────────    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ [❓ Inquiry  ]                42 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [👥 Customer ]               118 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [🛠️ Vendor   ]                23 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [👤 Personal ]                 9 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [🚫 Spam     ]                17 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [🕐 Follow-up]                 6 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [📝 Quoted   ]                14 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [✅ Closed-won]                8 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [❌ Closed-lost]               5 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [🔥 Hot lead ]                 3 calls › │    │
│  ├──────────────────────────────────────────┤    │
│  │ [🎯 VIP      ]                 2 calls › │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│                                          ┌────┐  │
│                                          │  + │  │
│                                          └────┘  │
└──────────────────────────────────────────────────┘
```

Editor dialog wireframe:

```
┌──────────────────────────────────────────────┐
│  Edit tag                                  ✕ │
├──────────────────────────────────────────────┤
│                                              │
│  Name                                        │
│  ┌────────────────────────────────────────┐  │
│  │ Customer                              │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  Emoji                                       │
│  ┌────────────────────────────────────────┐  │
│  │ 🏷️ 📞 📲 💬 💼 💰 💸 🛒 📦 🚚 🧾 📝 │  │
│  │ 📌 ⭐ ⚠️ 🚫 🛠️ 🤝 👤 👥 🏢 📍 🕐 ✅ │  │
│  │ ❌ 🔁 🎯 🧊 🔥 📅 🎉 ❓ — Custom: [👥]│  │
│  └────────────────────────────────────────┘  │
│                                              │
│  Colour                                      │
│  ┌────────────────────────────────────────┐  │
│  │ ● ● ● ● ● ● ● ● ● ● ● ●                │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  Preview:  [👥 Customer]                     │
│                                              │
│           [ Cancel ]  [ Save ]               │
└──────────────────────────────────────────────┘
```

Merge dialog wireframe:

```
┌──────────────────────────────────────────────┐
│  Merge into…                              ✕  │
├──────────────────────────────────────────────┤
│  All 14 calls tagged "Quoted" will move to   │
│  the chosen tag. This can't be undone.       │
│                                              │
│  ○ ❓ Inquiry                                 │
│  ○ 👥 Customer                                │
│  ● ✅ Closed-won                              │
│  ○ ❌ Closed-lost                             │
│  ○ 🔥 Hot lead                                │
│  ────────────────────────────────────────    │
│           [ Cancel ]   [ Merge ]              │
└──────────────────────────────────────────────┘
```

### 27.14 Accessibility

- Every row has `contentDescription = "Tag {name}, applied to {count}
calls. Double-tap to edit. Long-press for more actions."`
- The FAB has `contentDescription = "Create a new tag"`.
- Search field announces `"Search tags. {n} of {m} tags shown."` after
  a 600 ms debounce.
- Colour preview chips use a 4.5:1 contrast minimum; the
  `chooseContrastColor()` helper picks black or white text based on
  the relative luminance of the chip background.
- Keyboard nav: `Tab` moves between search → first row → … → FAB.
  `Shift+Tab` reverses. `Enter` activates a row. `Delete` on a
  focused row triggers the swipe-delete confirmation.
- The emoji grid is announced as `"Emoji picker, 32 options, swipe
right to navigate."`
- Long-press is also exposed as an "Actions" menu via the row's
  custom-actions API (`CustomAction("Merge into another tag…")`).
- Live region: when the count badge updates after a merge or delete,
  it's announced via an `accessibilityLiveRegion = Polite`.
- All tap targets are ≥48×48 dp.
- The chip shapes do not rely solely on colour — the emoji prefix
  is the primary visual identifier.

### 27.15 Performance budget

- Initial render: ≤200 ms from navigation commit to first row painted
  (assumes ≤50 tags; with the warm Room cache).
- Scroll: 60 fps on a Pixel 4a (`maxItemsPerScroll = 50`).
- Search filter: debounce 200 ms; the filter pass is O(n) over an
  in-memory list bounded by ~100 tags, target <2 ms.
- Tag count subscription: backed by a single `Flow<Map<Long, Int>>`
  that emits at most once per upstream Room invalidation. On a
  100k-call DB the count query runs in <80 ms with the
  `idx_call_tag_cross_ref_tag` index.
- Editor save: <50 ms in-DB; UI dismisses the dialog optimistically.
- Merge: bounded by `UPDATE call_tag_cross_ref SET tag_id = ?
WHERE tag_id = ?` — runs in a single transaction, target <500 ms
  for 10k cross-refs on a Pixel 4a.
- Memory: the screen retains <500 KB beyond the baseline navigator
  stack.

---

## 28 — AutoTagRulesScreen

### 28.1 Purpose

The AutoTagRulesScreen lists every auto-tag rule the user has defined.
Rules are the engine that lets callNest automatically tag, score, or
follow-up incoming calls based on patterns the user has expressed. The
screen is read-mostly: the rich editing happens in `RuleEditor`. From
the list, the user can:

- See every rule, ordered by execution priority.
- See, per rule, how many of the latest 200 calls would match.
- Toggle a rule on or off without entering the editor.
- Reorder rules (priority order matters when actions conflict).
- Bulk-delete rules.
- Tap into a rule to edit it.
- Create a new rule.

Rules execute on every sync and on every new call event from the
`CallEventReceiver`.

### 28.2 Entry points

| From                   | Trigger                                                                                                          |
| ---------------------- | ---------------------------------------------------------------------------------------------------------------- |
| MoreScreen             | "Auto-tag rules" row.                                                                                            |
| FilterSheet            | "Why was this tagged?" link → `RuleEditor` directly via deep link, but back stack returns to AutoTagRulesScreen. |
| Onboarding             | "Try a rule" button on the onboarding-rules step.                                                                |
| Settings → Power tools | "Manage rules" link.                                                                                             |
| Deep link              | `callNest://rules`.                                                                                              |

### 28.3 Exit points

| To                         | Trigger                                               |
| -------------------------- | ----------------------------------------------------- |
| Previous screen            | Hardware back / nav-bar back.                         |
| `RuleEditor/{ruleId}`      | Tap a row.                                            |
| `RuleEditor/-1`            | Tap FAB.                                              |
| Bulk-delete confirm dialog | Long-press a row.                                     |
| Snackbar                   | Successful enable/disable toggle, reorder, or delete. |

### 28.4 Required inputs (data)

```
data class AutoTagRulesUiState(
    val isLoading: Boolean,
    val rules: List<RuleListItem>,
    val draggedFromIndex: Int?,
    val draggedToIndex: Int?,
    val error: String?,
)

data class RuleListItem(
    val id: Long,
    val name: String,
    val isActive: Boolean,
    val priority: Int,
    val matchCount: Int,            // computed live, capped to 200
    val isInvalid: Boolean,         // true if conditions JSON failed to parse
)
```

Sources:

- `AutoTagRuleRepository.observeAllRules(): Flow<List<AutoTagRule>>`
- For each emitted rule, `RuleConditionEvaluator.matchCountIn(latest200)`
  via a memoised computation on `Dispatchers.Default`.
- The `latest200` snapshot comes from
  `CallRepository.observeLatest(200): Flow<List<Call>>`.

The match-count computation is debounced 250 ms so a slider drag in
the editor doesn't thrash the list.

### 28.5 Required inputs (user)

- Tap a row → open editor.
- Tap a row's switch → toggle active.
- Long-press a row → enter selection mode (multi-select for delete).
- Drag the up/down handle → reorder.
- Tap FAB → new rule.
- Tap toolbar overflow → "Run rules now" (manually re-evaluates over
  the entire call history; shown only if the user has Power tools
  enabled).

### 28.6 Mandatory display

```
StandardPage(
    title = "Rules",
    description = "Automatic tagging that runs every sync",
    emoji = "🪄",
)
```

Body:

1. A `LazyColumn` of rule rows. Each row:
   ```
   Row(modifier = Modifier.height(72.dp)) {
       DragHandle(up, down)
       Column(weight = 1f) {
           Text(rule.name, titleMedium)
           Text("Applies to ${matchCount} calls", labelSmall, onSurfaceVariant)
       }
       Switch(rule.isActive)
       ChevronRight
   }
   ```
2. A FAB anchored bottom-right, label `"New rule"`.

When `rule.isInvalid`, the row's subtitle reads `"⚠ Couldn't read this
rule — tap to fix"` in the error colour.

### 28.7 Optional display

- A "Run rules now" button in the toolbar overflow, only when Power
  tools are enabled.
- A 1-line banner above the list: `"50+ rules — performance may
drop"` when `rules.size >= 50`.
- Selection-mode toolbar replaces the standard toolbar when ≥1 row is
  long-pressed: shows count selected + "Delete" + "Cancel".
- A shimmer loader over the count subtitle while match counts are
  computing.

### 28.8 Empty state

```
StandardEmpty(
    emoji = "🪄",
    title = "No rules yet",
    body = "Create your first rule to auto-tag incoming calls.",
    primaryCta = NeoButton("Create a rule") { navigate("RuleEditor/-1") },
    secondaryCta = NeoTextButton("Browse examples") { openDocs("rules-examples") },
)
```

The "Browse examples" link opens the in-app docs article
`docs/06-rules-examples.md` which walks through 8 starter rules
(prefix matchers, time-of-day rules, duration thresholds, etc.).

### 28.9 Loading state

- A `LazyColumn` of 4 `ShimmerRow`s, each 72 dp tall.
- Toolbar disabled.
- FAB hidden.

Loading is transient (<300 ms).

### 28.10 Error state

If the rules flow throws:

- Inline `StandardError` block with copy `"Couldn't load your rules.
Tap to retry."` and a `NeoButton("Retry")`.
- FAB hidden.
- The toolbar overflow's "Run rules now" is disabled with a tooltip
  explaining the error.

### 28.11 Edge cases

| Case                                  | Handling                                                                                                                                                                     |
| ------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Rule with invalid JSON conditions     | `isInvalid = true`, row subtitle shows the warning. Tapping opens the editor; the editor surfaces `"This rule's conditions couldn't be read. Rebuild them and save to fix."` |
| Rule with deleted tag id (in actions) | The action is auto-removed when loading into the editor; a snackbar fires `"Removed an action that referenced a deleted tag."`                                               |
| Reorder during sync                   | The reorder is queued — the running sync is allowed to finish using the old priorities, and the new priority order is applied to the next sync.                              |
| Rules count cap                       | No hard cap, but a warning banner shows at ≥50. At ≥100 the FAB is replaced with a disabled tooltip `"Reaching 100 rules — please consolidate or trim."`                     |
| Concurrent edit                       | Last-write-wins on `id`.                                                                                                                                                     |
| Match-count compute during DB write   | The compute coroutine catches `RoomDatabaseClosedException` and emits `null`; the row falls back to "—".                                                                     |
| User toggles 50 rules off in <1 s     | Each toggle posts to a `MutableSharedFlow<Long, Boolean>(replay = 0, extraBufferCapacity = 64)` and writes are batched in 100 ms windows.                                    |
| First boot with no rules              | Show empty state with a CTA. Do not seed any rule (unlike tags).                                                                                                             |

### 28.12 Copy table

| Key                      | Resource id                      | English                                                            |
| ------------------------ | -------------------------------- | ------------------------------------------------------------------ |
| Title                    | `rules_title`                    | Rules                                                              |
| Subtitle                 | `rules_subtitle`                 | Automatic tagging that runs every sync                             |
| Subtitle (count)         | `rules_subtitle_count`           | Applies to %1$d calls                                              |
| Subtitle (invalid)       | `rules_subtitle_invalid`         | ⚠ Couldn't read this rule — tap to fix                             |
| FAB label                | `rules_new_fab`                  | New rule                                                           |
| Empty title              | `rules_empty_title`              | No rules yet                                                       |
| Empty body               | `rules_empty_body`               | Create your first rule to auto-tag incoming calls.                 |
| Empty CTA primary        | `rules_empty_cta_primary`        | Create a rule                                                      |
| Empty CTA secondary      | `rules_empty_cta_secondary`      | Browse examples                                                    |
| Run now overflow         | `rules_run_now`                  | Run rules now                                                      |
| Run now started          | `rules_run_now_started`          | Running %1$d rules over your history…                              |
| Run now done             | `rules_run_now_done`             | Done. Re-tagged %1$d calls.                                        |
| Selection delete         | `rules_selection_delete`         | Delete %1$d rules                                                  |
| Selection delete confirm | `rules_selection_delete_confirm` | Delete %1$d rules? Calls already tagged by them won't be affected. |
| Reorder hint             | `rules_reorder_hint`             | Drag up/down to change priority                                    |
| Cap warning              | `rules_cap_warning`              | %1$d rules — performance may drop. Consider consolidating.         |
| Cap reached              | `rules_cap_reached`              | Reaching 100 rules — please consolidate or trim.                   |
| Toggle on snackbar       | `rules_toggle_on`                | "%1$s" enabled.                                                    |
| Toggle off snackbar      | `rules_toggle_off`               | "%1$s" disabled.                                                   |
| Reorder snackbar         | `rules_reorder_snack`            | Priority updated.                                                  |
| Deleted snackbar         | `rules_deleted_snack`            | Rule deleted.                                                      |
| Bulk deleted snackbar    | `rules_bulk_deleted_snack`       | %1$d rules deleted.                                                |
| Error body               | `rules_error_body`               | Couldn't load your rules. Tap to retry.                            |

### 28.13 ASCII wireframe

```
┌──────────────────────────────────────────────────┐
│ ←  Rules                                       ⋮  │
├──────────────────────────────────────────────────┤
│  🪄                                              │
│  Rules                                           │
│  Automatic tagging that runs every sync          │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ ⇅  Tag +91-9XXX as Customer            ▶ │    │
│  │    Applies to 87 calls          ●━━━○   │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  Mark short calls under 10s as Spam  ▶ │    │
│  │    Applies to 14 calls          ●━━━○   │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  Boost lead score for SIM 1          ▶ │    │
│  │    Applies to 122 calls         ○━━━●   │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  Auto-bookmark VIP numbers           ▶ │    │
│  │    Applies to 4 calls           ●━━━○   │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  Quoted → follow-up in 3 days        ▶ │    │
│  │    Applies to 18 calls          ●━━━○   │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  ⚠ Couldn't read this rule — tap…    ▶ │    │
│  │                                  ○━━━●   │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│                                          ┌────┐  │
│                                          │ +  │  │
│                                          └────┘  │
└──────────────────────────────────────────────────┘
```

### 28.14 Accessibility

- Each row's content description: `"Rule {name}, applies to {n}
calls, currently {on|off}. Double-tap to edit, swipe right with two
fingers to reorder."`
- The toggle's role is `Role.Switch`; talkback announces state changes.
- Drag handle has `contentDescription = "Reorder rule {name}.
Currently at position {i} of {n}."`
- Reorder handle exposed as custom actions: `"Move up", "Move down",
"Move to top", "Move to bottom"`.
- The error subtitle is announced with `accessibilityLiveRegion =
Polite`.
- Long-press alternative: row's overflow menu exposes "Select",
  "Delete", "Duplicate".

### 28.15 Performance budget

- Initial render: ≤300 ms with up to 30 rules.
- Match-count compute: capped to latest 200 calls, target <40 ms per
  rule on a Pixel 4a, parallelised across rules with
  `coroutineScope { rules.map { async { … } } }`.
- Reorder write: single `UPDATE rules SET priority = ? WHERE id = ?`
  per row; whole reorder in a transaction <100 ms.
- Toggle write: <20 ms.
- Memory: list state plus 200-call snapshot ≈ 600 KB.

---

## 29 — RuleEditor screen

### 29.1 Purpose

The RuleEditor is where the actual logic of an auto-tag rule is
defined. It has three jobs:

1. Let the user describe a set of conditions that must ALL be true for
   the rule to fire.
2. Let the user pick a list of actions to take when the rule fires.
3. Continuously preview, in real time, how many calls in the user's
   history would have matched the rule as currently expressed.

This screen is opened with either an existing `ruleId` or `-1` for
new-rule mode.

### 29.2 Entry points

| From               | Trigger                                                 |
| ------------------ | ------------------------------------------------------- |
| AutoTagRulesScreen | Tap a row (existing rule) or FAB (new rule).            |
| FilterSheet        | "Why was this tagged?" → opens the matching rule.       |
| Onboarding         | "Try this example" → seeds a rule and opens the editor. |
| Deep link          | `callNest://rules/{id}` or `callNest://rules/new`.      |

### 29.3 Exit points

| To                     | Trigger                                 |
| ---------------------- | --------------------------------------- |
| AutoTagRulesScreen     | Back / Save (saves and pops).           |
| `DiscardChangesDialog` | Back when the rule has unsaved changes. |
| Snackbar               | "Saved" / "Discarded".                  |

### 29.4 Required inputs (data)

```
data class RuleEditorUiState(
    val ruleId: Long?,                        // null when new
    val name: String,
    val conditions: List<Condition>,
    val actions: List<Action>,
    val previewCount: Int?,                   // null while computing
    val isPreviewLoading: Boolean,
    val isDirty: Boolean,
    val isValid: Boolean,
    val nameError: String?,
    val conditionErrors: Map<Int, String>,    // index → message
    val actionErrors: Map<Int, String>,
)
```

Sources:

- For existing rule: `AutoTagRuleRepository.getRule(id)` once at start.
- For preview: a `combine(snapshot200, conditionsFlow)` mapped through
  `RuleConditionEvaluator.matchCountIn(snapshot200, conditions)` with
  a 400 ms debounce on the conditions flow.

### 29.5 Required inputs (user)

- Type into the `Name` text field.
- Tap "+ Add condition" → choose a variant from a sheet → fill
  variant-specific inputs.
- Tap "+ Add action" → choose a variant → fill inputs.
- Re-order conditions / actions via drag handles.
- Remove a condition / action via the trailing trash icon.
- Tap Save (toolbar) — only enabled when `isValid`.
- Hardware back / nav back — opens discard-changes confirm if
  `isDirty`.

### 29.6 Mandatory display

```
StandardPage(
    title = uiState.name.ifBlank { "New rule" },
    description = "When all conditions match, do these actions",
    emoji = "⚙️",
)
```

Body in three sections, each rendered as a `NeoCard` with `BorderSoft`:

#### Section A — Name

- A single `NeoTextField`, label `"Name"`, hint `"Tag +91 prefix as
customer"`, max length 60. Helper text shows `nameError` if any.

#### Section B — When all of these are true…

- Header text: `"When all of these are true…"` (titleSmall) plus a
  caption `"All conditions must match for the rule to fire."`
- For each condition, a `ConditionRow` composable:
  ```
  Row {
      DragHandle
      VariantDropdown   // shows e.g. "Number prefix"
      VariantInputs     // variant-specific layout
      RemoveIconButton
  }
  ```
- A trailing `NeoTextButton("+ Add condition")` that opens a bottom
  sheet listing the 13 variants.

#### Section C — Then…

- Header `"Then…"` with caption `"These actions run when the rule
fires. Multiple actions can apply at once."`
- For each action, an `ActionRow` (mirrors `ConditionRow`).
- A trailing `NeoTextButton("+ Add action")` opening a sheet of 4
  variants.

#### LivePreviewBox

Below Section C, a fixed banner with green/grey backgrounds:

```
┌─────────────────────────────────────────────┐
│ 🔮  This rule would apply to 87 calls       │
│     in your last 200.                        │
└─────────────────────────────────────────────┘
```

When `isPreviewLoading`: shows `NeoLoader` and copy `"Working it
out…"`.
When count is 0: orange-tinted with copy `"No calls match yet — try
loosening a condition."`.
When count ≥1: green-tinted.
When evaluator throws (e.g. invalid regex): red-tinted with
`"Can't preview — fix the conditions above."`.

#### Top app bar

- Back arrow.
- Title (rule name in bar ellipsised).
- Save action (text button), enabled when:
  - `name` non-blank,
  - `conditions.size >= 1`,
  - `actions.size >= 1`,
  - `conditionErrors.isEmpty()`,
  - `actionErrors.isEmpty()`.
- Overflow: "Duplicate", "Delete", "View as JSON" (debug builds only).

### 29.7 Optional display

- "Reset to defaults" button next to the title bar's overflow when
  editing an existing rule that was modified.
- A small chip "Rule disabled" beside the title if the rule is saved
  but `!isActive` — tapping toggles via a confirmation snackbar with
  Undo.
- A help icon next to each variant dropdown that opens a 3-line tip
  in a tooltip.

### 29.8 Empty state

For a new rule:

- Section A: empty name field with placeholder.
- Section B: a placeholder card `"No conditions yet. Add at least one
to get started."` with an inline `NeoButton("Add condition")`.
- Section C: similar placeholder card.
- LivePreviewBox: hidden until ≥1 condition is added.

### 29.9 Loading state

- For an existing rule, the screen renders a `NeoLoader` filling the
  body region until the first `getRule(id)` returns. The toolbar shows
  the title `"Loading…"`.
- For new rules, no loader — render straight to the empty state.

### 29.10 Error state

- If `getRule(id)` fails, show a full-screen `StandardError` with copy
  `"Couldn't open this rule. It may have been deleted."` and CTAs
  `"Back to rules"` and `"Retry"`.
- If saving fails (e.g. DB constraint), the Save button re-enables and
  a snackbar shows `"Couldn't save. {error}"` with action `"Retry"`.

### 29.11 Edge cases

| Case                                                  | Handling                                                                                                            |
| ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| Invalid regex in `RegexMatches`                       | Live error `"That regex didn't compile: {message}"`. Save disabled. Preview red.                                    |
| Unreachable rule                                      | Preview shows 0 with the orange tip. Save still allowed — the rule simply won't ever fire.                          |
| Action references a tag the user later deletes        | On reload the action is removed; snackbar fires.                                                                    |
| Reordering rules                                      | Not done here — list screen handles priority.                                                                       |
| Conflicting actions (two `ApplyTag` for the same tag) | Allowed; the second is a no-op. UI shows a yellow info chip "Duplicate action".                                     |
| Editing a rule that's currently running               | Save queues until the current evaluator pass finishes (max wait 1 s, else falls back to fire-and-forget overwrite). |
| Two users on two devices                              | Last-write-wins on `id`.                                                                                            |
| Very long condition list (>20)                        | Allowed, but a banner reads `"Lots of conditions — make sure you really mean ALL of these."`                        |
| Time-of-day pickers crossing midnight                 | Allowed: `from = 22:00`, `to = 02:00` is interpreted as 22:00–02:00 next day.                                       |

#### Condition variants — the 13 documented types

| #   | Variant            | Sealed-class name      | Inputs                              | Valid range / format                                                 | Validation message                             |
| --- | ------------------ | ---------------------- | ----------------------------------- | -------------------------------------------------------------------- | ---------------------------------------------- |
| 1   | Number prefix      | `PrefixMatches`        | `prefix: String`                    | 1–10 chars, digits / `+` / leading zero allowed.                     | "Enter a phone-number prefix like +91 or 080." |
| 2   | Number regex       | `RegexMatches`         | `pattern: String`                   | Must compile via `Pattern.compile`. Cap length 200.                  | "That regex didn't compile: {message}"         |
| 3   | Country            | `CountryEquals`        | `iso2: String`                      | ISO 3166-1 alpha-2, picker UI.                                       | "Pick a country."                              |
| 4   | In contacts        | `IsInContacts`         | `(boolean — implicit)`              | Always valid.                                                        | —                                              |
| 5   | Call type          | `CallTypeIn`           | `types: Set<CallType>`              | At least one of: incoming / outgoing / missed / rejected / blocked.  | "Pick at least one call type."                 |
| 6   | Duration compare   | `DurationCompare`      | `op: <, ≤, =, ≥, >`; `seconds: Int` | 0–86 400.                                                            | "Enter seconds between 0 and 86,400."          |
| 7   | Time of day        | `TimeOfDayBetween`     | `fromMin: Int`, `toMin: Int`        | Each 0–1439. From may exceed to (wraps midnight).                    | "Pick a from and to time."                     |
| 8   | Day of week        | `DayOfWeekIn`          | `days: Set<DayOfWeek>`              | At least one.                                                        | "Pick at least one day."                       |
| 9   | SIM slot           | `SimSlotEquals`        | `slot: Int`                         | 0 or 1 (we only model two SIMs; eSIMs map to slot 1 currently).      | "Pick SIM 1 or SIM 2."                         |
| 10  | Tag applied        | `TagApplied`           | `tagId: Long`                       | Tag must exist.                                                      | "Pick a tag."                                  |
| 11  | Tag NOT applied    | `TagNotApplied`        | `tagId: Long`                       | Tag must exist.                                                      | "Pick a tag."                                  |
| 12  | Geo contains       | `GeoContains`          | `substring: String`                 | 1–60 chars (matches `geocodedLocation` substring, case-insensitive). | "Enter a place name (e.g. Pune)."              |
| 13  | Total call count > | `CallCountGreaterThan` | `n: Int`                            | 0–10 000. Counts calls from the same `e164Number`.                   | "Enter a number between 0 and 10,000."         |

#### Action variants — the 4 documented types

| #   | Variant          | Sealed-class name | Inputs                         | Side effect                                                                                            |
| --- | ---------------- | ----------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------ |
| 1   | Apply tag        | `ApplyTag`        | `tagId: Long`                  | Inserts into `call_tag_cross_ref`. Idempotent.                                                         |
| 2   | Lead score boost | `LeadScoreBoost`  | `delta: Int` (signed, −50…+50) | Adds delta to the call's `leadScore`, clamped to 0…100.                                                |
| 3   | Auto-bookmark    | `AutoBookmark`    | `(none)`                       | Sets `isBookmarked = true`.                                                                            |
| 4   | Mark follow-up   | `MarkFollowUp`    | `days: Int` (1–365)            | Sets `followUpAt = now + days*24h`. Replaces any existing follow-up for the call (we keep the latest). |

### 29.12 Copy table

| Key                    | Resource id                     | English                                                                    |
| ---------------------- | ------------------------------- | -------------------------------------------------------------------------- |
| Title (new)            | `rule_editor_title_new`         | New rule                                                                   |
| Title (edit)           | `rule_editor_title_edit`        | Edit rule                                                                  |
| Subtitle               | `rule_editor_subtitle`          | When all conditions match, do these actions                                |
| Section A label        | `rule_editor_name_label`        | Name                                                                       |
| Section A hint         | `rule_editor_name_hint`         | e.g. Tag +91 prefix as customer                                            |
| Section B header       | `rule_editor_when_header`       | When all of these are true…                                                |
| Section B caption      | `rule_editor_when_caption`      | All conditions must match for the rule to fire.                            |
| Section B add          | `rule_editor_when_add`          | + Add condition                                                            |
| Section B empty        | `rule_editor_when_empty`        | No conditions yet. Add at least one to get started.                        |
| Section C header       | `rule_editor_then_header`       | Then…                                                                      |
| Section C caption      | `rule_editor_then_caption`      | These actions run when the rule fires. Multiple actions can apply at once. |
| Section C add          | `rule_editor_then_add`          | + Add action                                                               |
| Section C empty        | `rule_editor_then_empty`        | No actions yet. Add at least one to get started.                           |
| Preview computing      | `rule_editor_preview_computing` | Working it out…                                                            |
| Preview none           | `rule_editor_preview_zero`      | No calls match yet — try loosening a condition.                            |
| Preview ok             | `rule_editor_preview_ok`        | This rule would apply to %1$d calls in your last 200.                      |
| Preview error          | `rule_editor_preview_error`     | Can't preview — fix the conditions above.                                  |
| Save                   | `rule_editor_save`              | Save                                                                       |
| Save success           | `rule_editor_saved_snack`       | Rule saved.                                                                |
| Save error             | `rule_editor_save_err`          | Couldn't save. %1$s                                                        |
| Discard title          | `rule_editor_discard_title`     | Discard changes?                                                           |
| Discard body           | `rule_editor_discard_body`      | Your edits will be lost.                                                   |
| Discard confirm        | `rule_editor_discard_confirm`   | Discard                                                                    |
| Discard cancel         | `rule_editor_discard_cancel`    | Keep editing                                                               |
| Duplicate overflow     | `rule_editor_duplicate`         | Duplicate                                                                  |
| Delete overflow        | `rule_editor_delete`            | Delete                                                                     |
| Delete confirm         | `rule_editor_delete_confirm`    | Delete this rule? Calls already tagged by it won't be affected.            |
| Disabled chip          | `rule_editor_disabled_chip`     | Rule disabled                                                              |
| Enable snackbar        | `rule_editor_enabled_snack`     | Rule enabled.                                                              |
| Variant: Prefix        | `rule_var_prefix`               | Number prefix                                                              |
| Variant: Regex         | `rule_var_regex`                | Number regex                                                               |
| Variant: Country       | `rule_var_country`              | Country                                                                    |
| Variant: InContacts    | `rule_var_in_contacts`          | Caller is in contacts                                                      |
| Variant: CallType      | `rule_var_call_type`            | Call type                                                                  |
| Variant: Duration      | `rule_var_duration`             | Duration                                                                   |
| Variant: TimeOfDay     | `rule_var_time_of_day`          | Time of day                                                                |
| Variant: DayOfWeek     | `rule_var_day_of_week`          | Day of week                                                                |
| Variant: Sim           | `rule_var_sim`                  | SIM slot                                                                   |
| Variant: TagApplied    | `rule_var_tag_applied`          | Already has tag                                                            |
| Variant: TagNotApplied | `rule_var_tag_not_applied`      | Doesn't have tag                                                           |
| Variant: Geo           | `rule_var_geo`                  | Location contains                                                          |
| Variant: CallCount     | `rule_var_call_count`           | Total calls from this number                                               |
| Action: ApplyTag       | `rule_act_apply_tag`            | Apply tag                                                                  |
| Action: LeadScore      | `rule_act_lead_score`           | Adjust lead score                                                          |
| Action: Bookmark       | `rule_act_bookmark`             | Bookmark the call                                                          |
| Action: FollowUp       | `rule_act_followup`             | Mark follow-up in N days                                                   |
| Add condition sheet    | `rule_pick_condition_sheet`     | Add a condition                                                            |
| Add action sheet       | `rule_pick_action_sheet`        | Add an action                                                              |

### 29.13 ASCII wireframes

#### Wireframe — empty new rule

```
┌──────────────────────────────────────────────────┐
│ ←  New rule                                Save  │
├──────────────────────────────────────────────────┤
│  ⚙️                                              │
│  New rule                                        │
│  When all conditions match, do these actions     │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Name                                     │    │
│  │ ┌──────────────────────────────────────┐ │    │
│  │ │ e.g. Tag +91 prefix as customer      │ │    │
│  │ └──────────────────────────────────────┘ │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ When all of these are true…              │    │
│  │ All conditions must match for the rule   │    │
│  │ to fire.                                 │    │
│  │                                          │    │
│  │   No conditions yet. Add at least one    │    │
│  │   to get started.                        │    │
│  │                                          │    │
│  │ + Add condition                          │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Then…                                    │    │
│  │   No actions yet. Add at least one to    │    │
│  │   get started.                           │    │
│  │ + Add action                             │    │
│  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
```

#### Wireframe — rule with conditions (preview many)

```
┌──────────────────────────────────────────────────┐
│ ←  Tag +91 prefix as customer              Save  │
├──────────────────────────────────────────────────┤
│  ⚙️                                              │
│  Tag +91 prefix as customer                      │
│  When all conditions match, do these actions     │
│                                                  │
│  Name: Tag +91 prefix as customer                │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ When all of these are true…              │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  [Number prefix ▾]  +91          🗑     │    │
│  │ ⇅  [Call type ▾]      ☑ inbound    🗑     │    │
│  │ ⇅  [Duration ▾]       > 20s        🗑     │    │
│  │ + Add condition                          │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Then…                                    │    │
│  ├──────────────────────────────────────────┤    │
│  │ ⇅  [Apply tag ▾]      👥 Customer   🗑    │    │
│  │ ⇅  [Lead score ▾]     +10           🗑    │    │
│  │ + Add action                             │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ 🔮  This rule would apply to 87 calls    │    │
│  │     in your last 200.                    │    │
│  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
```

#### Wireframe — preview zero

```
│  ┌──────────────────────────────────────────┐    │
│  │ 🔮  No calls match yet — try loosening   │    │
│  │     a condition.                         │    │
│  └──────────────────────────────────────────┘    │
```

#### Wireframe — preview error

```
│  ┌──────────────────────────────────────────┐    │
│  │ ⚠  Can't preview — fix the conditions    │    │
│  │     above.                               │    │
│  └──────────────────────────────────────────┘    │
```

### 29.14 Accessibility

- Each section has a `Modifier.semantics { heading() }` on its header.
- Drag handles expose custom actions: `"Move up"`, `"Move down"`,
  `"Remove"`.
- Variant dropdowns use `Role.Button`; the chosen variant is read out
  on focus.
- Time pickers fall back to the platform `TimePicker` dialog with full
  TalkBack support.
- Day-of-week chip group: each chip exposes `selected` state.
- Live preview banner uses `accessibilityLiveRegion = Polite` so
  changes announce on debounce.
- Save button announces its disabled reason via `stateDescription`:
  e.g. `"Save unavailable. Add at least one condition."`
- Discard dialog is announced as a `Role.Alert`.
- Min 48×48 dp tap targets on every dropdown and remove button.
- Regex error helper text is associated to its field via
  `Modifier.semantics { error("…") }`.

### 29.15 Performance budget

- Initial render: ≤350 ms even for a 20-condition rule.
- Preview compute: <300 ms over 200 calls × 20 conditions on a
  Pixel 4a. Debounce 400 ms; cancel-on-new-edit.
- Save: serialise via kotlinx.serialization, single Room upsert, total
  <80 ms.
- Memory: editor holds <800 KB beyond the navigator baseline.
- The variant chooser sheet hydrates lazily; its first open triggers a
  one-time tag-list snapshot via `TagRepository.snapshot()`.
- Regex compile is cached per pattern in `RuleConditionEvaluator`.

---

## 30 — BackupScreen

### 30.1 Purpose

The BackupScreen lets the user protect their callNest data — locally
and (optionally) to Google Drive — and restore from backup if their
device is lost or the app is reinstalled. The screen is the single
control surface for:

- Toggling automatic local backups on a daily schedule.
- Setting the retention window (number of recent backups kept).
- Triggering an on-demand backup right now.
- Restoring from a `.cvb` file picked by the user.
- Setting / changing / clearing the backup encryption passphrase.
- Connecting / disconnecting a Google Drive account and toggling
  upload-after-backup.
- Viewing the current backup status (last backup time, last upload
  time, size).

Backups are encrypted at rest with AES-256-GCM keyed off a
PBKDF2-HMAC-SHA256-derived key from the user's passphrase.

> NOTE: The mega-spec mentions Tink keysets in passing. Implementation
> chose PBKDF2 + raw GCM for two reasons: (1) the user passphrase is
> the only secret material we want to persist offline, so a Tink
> keyset would itself need to be wrapped by the same passphrase, and
> (2) PBKDF2 is built into Android since API 1 and avoids dragging
> Tink's full surface into the backup module. This deviation is
> recorded in `DECISIONS.md`.

### 30.2 Entry points

| From               | Trigger                                  |
| ------------------ | ---------------------------------------- |
| MoreScreen         | "Backup & restore" row.                  |
| Onboarding         | "Set up backups" step.                   |
| Settings → Storage | "Manage backups".                        |
| Update screen      | "Backup before updating" CTA (one-shot). |
| Deep link          | `callNest://backup`.                     |

### 30.3 Exit points

| To                                  | Trigger                                               |
| ----------------------------------- | ----------------------------------------------------- |
| Previous screen                     | Back.                                                 |
| `PassphraseSetupDialog`             | "Set/Change passphrase".                              |
| `PassphraseEntryDialog`             | "Manual backup now" / "Restore from file" (when set). |
| System file picker (`OpenDocument`) | "Restore from file".                                  |
| System Google Sign-In flow          | "Sign in with Google".                                |
| `RestoreConfirmDialog`              | Mid-restore confirmation.                             |
| `BackupCompleteSheet`               | After a successful manual backup.                     |

### 30.4 Required inputs (data)

```
data class BackupUiState(
    val autoBackupEnabled: Boolean,
    val retentionDays: Int,             // 3..14
    val passphraseStatus: PassphraseStatus,   // NotSet, Set
    val lastLocalBackupAt: Long?,
    val lastLocalBackupSizeBytes: Long?,
    val localBackupCount: Int,

    val driveEnabled: Boolean,
    val driveSignedInEmail: String?,
    val autoUploadAfterBackup: Boolean,
    val lastDriveUploadAt: Long?,
    val driveOauthConfigured: Boolean,

    val isManualBackupRunning: Boolean,
    val isRestoreRunning: Boolean,
    val isUploadRunning: Boolean,
    val error: BackupError?,
)

sealed interface PassphraseStatus { object NotSet; object Set }

sealed interface BackupError {
    data class PassphraseMissing(val for: String): BackupError
    data class DriveQuotaExceeded(val message: String): BackupError
    data class DriveOauthExpired(val message: String): BackupError
    data class DriveFolderMissing(val message: String): BackupError
    data class FilePickerCancelled(val message: String): BackupError
    data class WrongPassphrase(val message: String): BackupError
    data class CorruptArchive(val message: String): BackupError
    data class IoFailure(val message: String): BackupError
}
```

Sources:

- `SettingsDataStore.observe(…)` for the toggles and retention.
- `BackupRepository.observeLatest(): Flow<BackupSummary?>` for last
  backup metadata (read from `backup_history` table).
- `DriveRepository.observeAccountStatus(): Flow<DriveAccountStatus>`
  for sign-in / quota / folder existence.
- `BuildConfig.DRIVE_OAUTH_CONFIGURED` for whether the Drive section
  should be shown vs replaced with a "not configured" warning.

### 30.5 Required inputs (user)

- Toggle "Auto-backup".
- Drag retention slider (3–14).
- Tap "Manual backup now".
- Tap "Restore from file" → file picker → passphrase prompt → confirm.
- Tap "Set passphrase" / "Change passphrase" → dialog with two fields
  - show-as-text toggle.
- Tap "Save to Google Drive" toggle.
- Tap "Sign in with Google" / "Sign out".
- Tap "Upload now".
- Toggle "Auto-upload after each local backup".

### 30.6 Mandatory display

```
StandardPage(
    title = "Backup & restore",
    description = "Keep your data safe — locally and in your Drive.",
    emoji = "🛡️",
)
```

Body composed of two `NeoCard`s:

#### Card A — Local

- Header row: `Local` (titleSmall) + status pill `Last backup • 2h
ago • 1.4 MB` (or `Never` when null).
- `Auto-backup` switch row.
- `Keep last N backups` slider row, with live label `Keep last 7
backups`.
- `Manual backup now` `NeoButton` (full width).
- `Restore from file` `NeoButton` (full width, secondary).
- `Backup encryption passphrase` row:
  ```
  Row {
      Column {
          Text("Passphrase")
          Text(if (set) "Set" else "Not set", labelSmall, onSurfaceVariant)
      }
      Spacer(weight = 1f)
      NeoTextButton(if (set) "Change" else "Set") { … }
  }
  ```

#### Card B — Cloud (Google Drive)

When `driveOauthConfigured == false`:

```
NeoCard(BorderSoft) {
  Column {
    Header "Cloud (Google Drive)"
    Text("Drive isn't configured. See docs/locale/06-google-cloud-setup.md.")
    NeoButton("Open setup docs") { openDocs("06-google-cloud-setup") }
  }
}
```

When `driveOauthConfigured == true`:

- Master toggle: `Save to Google Drive` (default OFF). When OFF, the
  rest of the card collapses with a single row left visible:
  `"When enabled, encrypted backups can be uploaded to your Drive."`
- When ON and not signed in: `Sign in with Google` button.
- When ON and signed in:
  - Status row: `"Signed in as you@example.com"` + `Sign out`.
  - `Upload now` button (disabled when passphrase not set).
  - `Auto-upload after each local backup` switch.
  - Status pill: `Last upload • yesterday at 21:04`.
- Explainer text at the bottom of the card:
  `"Backups are encrypted with your passphrase before upload — nothing
readable leaves your device."`

### 30.7 Optional display

- A "Verify last backup" link under Card A — runs a non-destructive
  decrypt + integrity check on the most recent file.
- A "View backup history" link → opens `BackupHistorySheet` (a
  bottom sheet listing the retained backups with size, age,
  passphrase-version label, plus a "Delete" action per entry).
- An info chip "Battery saver mode is on — backup may be delayed"
  when `PowerManager.isPowerSaveMode == true`.
- A red banner above Card A when `passphraseStatus == NotSet &&
autoBackupEnabled`: `"Auto-backup is on but no passphrase is set.
Set one to start backing up."`

### 30.8 Empty state

There's no true empty state — the screen always renders both cards.
However, when no backup has ever been made:

- Status pill in Card A reads `"Never backed up."`.
- Card B's last-upload pill is hidden.

### 30.9 Loading state

- While the ViewModel hydrates: a skeleton placeholder for the two
  cards (44 dp shimmer rows × 4 each).
- During a manual backup (`isManualBackupRunning`): the "Manual backup
  now" button shows a `NeoLoader` (24 dp) and the label changes to
  `"Backing up…"`. Other controls disable.
- During a restore (`isRestoreRunning`): the entire screen overlays a
  modal `NeoProgressBar` with copy `"Restoring — don't close the
app."`. All UI is blocked.
- During an upload: the "Upload now" button shows a loader; the
  auto-upload switch disables.

### 30.10 Error state

Errors render as banners at the top of the affected card:

| BackupError                   | Banner copy                                                                                    |
| ----------------------------- | ---------------------------------------------------------------------------------------------- |
| `PassphraseMissing("backup")` | "Set a passphrase before backing up." with CTA "Set passphrase".                               |
| `PassphraseMissing("upload")` | "Set a passphrase before uploading." with CTA.                                                 |
| `DriveQuotaExceeded`          | "Your Drive is full. Free space, or change account."                                           |
| `DriveOauthExpired`           | "Your Drive sign-in expired. Sign in again." with CTA "Sign in".                               |
| `DriveFolderMissing`          | "Your callNest folder in Drive is gone — we'll recreate it on next upload." (info, not error). |
| `FilePickerCancelled`         | "Restore cancelled." (transient snackbar, not banner).                                         |
| `WrongPassphrase`             | "That passphrase doesn't match this backup."                                                   |
| `CorruptArchive`              | "This file isn't a callNest backup, or it's corrupted."                                        |
| `IoFailure(msg)`              | "Couldn't write the backup: {msg}." with CTA "Retry".                                          |

### 30.11 Edge cases

| Case                                          | Handling                                                                                                                                                                                                                                                                                                                                                                  |
| --------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Passphrase forgotten                          | Lost forever. The Set/Change dialog explicitly states `"There's no recovery — if you forget this passphrase, your backups can't be opened. Write it down somewhere safe."`                                                                                                                                                                                                |
| Backup during sync                            | The backup waits for the running sync to finish (single-flight `Mutex` shared with `SyncService`).                                                                                                                                                                                                                                                                        |
| Restore replacing existing data               | Double-confirm dialog: confirm 1 reads `"This will REPLACE everything in callNest with the contents of {fileName}. Type DELETE to continue."` confirm 2 reads `"Last chance — your current calls, tags, rules, and notes will be wiped. Continue?"`. The actual operation is a single Room transaction: drop user data, restore from archive, commit. Failure rolls back. |
| Drive OAuth expired                           | Refresh-token flow attempted silently; if it fails, surface the `DriveOauthExpired` banner.                                                                                                                                                                                                                                                                               |
| Drive quota exceeded                          | Cache the upload in `cache/pending-uploads/` (capped at 3 entries) and retry next time the quota check passes.                                                                                                                                                                                                                                                            |
| Drive folder deleted by the user              | Recreate `callNest/` at next upload; never touch the user's other Drive content.                                                                                                                                                                                                                                                                                          |
| Low disk space (<50 MB)                       | Refuse to start a manual backup; show a `Snackbar("Not enough space — free at least 50 MB.")`.                                                                                                                                                                                                                                                                            |
| Battery saver                                 | Auto-backups still run via `WorkManager` constraints `setRequiresBatteryNotLow(true)`; manual backups always run.                                                                                                                                                                                                                                                         |
| App killed mid-backup                         | The `.cvb.tmp` file is not promoted to `.cvb`; on next launch the temp is deleted.                                                                                                                                                                                                                                                                                        |
| Restore from a `.cvb` made by a newer schema  | Detected via the magic header version byte. We refuse and show `"This backup was made on a newer version of callNest. Update the app and try again."`                                                                                                                                                                                                                     |
| Restore from a `.cvb` made by an older schema | We run the same Room migration chain on the restored DB before commit.                                                                                                                                                                                                                                                                                                    |
| User signs out of Drive                       | We do NOT delete the cloud copies — we just disconnect the account.                                                                                                                                                                                                                                                                                                       |
| User toggles auto-upload OFF                  | Pending uploads are cleared.                                                                                                                                                                                                                                                                                                                                              |
| Two devices with same Google account          | Both read/write the same `callNest/` folder; backup filenames include device hostname + timestamp so they don't clash.                                                                                                                                                                                                                                                    |

### 30.12 Backup file format

```
+-----------+-------+------+----------------------------+--------+
| Magic     | Salt  | IV   | AES-256-GCM ciphertext     | Tag    |
| "CVB1"    | 16 B  | 12 B | variable                    | 16 B   |
+-----------+-------+------+----------------------------+--------+
4 bytes
```

- Magic header: ASCII bytes `0x43 0x56 0x42 0x31` (`CVB1`). Version 1.
- Salt: 16 random bytes from `SecureRandom`.
- IV: 12 random bytes.
- Key derivation: PBKDF2-HMAC-SHA256, 120,000 iterations, salt above,
  output 32 bytes (256 bits).
- Cipher: AES-256-GCM, 128-bit auth tag.
- Plaintext: a gzip-compressed JSON snapshot produced by
  `BackupSnapshotBuilder` (calls / tags / rules / notes / settings /
  schema version / device id).
- Filename: `callNest-YYYYMMDD-HHmmss-{hostname}.cvb`.

### 30.13 Copy table

| Key                        | Resource id                    | English                                                                                                          |
| -------------------------- | ------------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| Title                      | `backup_title`                 | Backup & restore                                                                                                 |
| Subtitle                   | `backup_subtitle`              | Keep your data safe — locally and in your Drive.                                                                 |
| Local card header          | `backup_local_header`          | Local                                                                                                            |
| Auto-backup label          | `backup_auto_label`            | Auto-backup                                                                                                      |
| Auto-backup caption        | `backup_auto_caption`          | Run a backup once a day when charging and on Wi-Fi.                                                              |
| Retention label            | `backup_retention_label`       | Keep last %1$d backups                                                                                           |
| Manual now                 | `backup_manual_now`            | Manual backup now                                                                                                |
| Manual now running         | `backup_manual_running`        | Backing up…                                                                                                      |
| Restore from file          | `backup_restore_file`          | Restore from file                                                                                                |
| Passphrase row label       | `backup_passphrase_label`      | Backup encryption passphrase                                                                                     |
| Passphrase set             | `backup_passphrase_set`        | Set                                                                                                              |
| Passphrase not set         | `backup_passphrase_not_set`    | Not set                                                                                                          |
| Set                        | `backup_passphrase_set_cta`    | Set                                                                                                              |
| Change                     | `backup_passphrase_change_cta` | Change                                                                                                           |
| Last backup never          | `backup_last_never`            | Never backed up.                                                                                                 |
| Last backup at             | `backup_last_at`               | Last backup • %1$s • %2$s                                                                                        |
| Cloud header               | `backup_cloud_header`          | Cloud (Google Drive)                                                                                             |
| Drive master toggle        | `backup_drive_master`          | Save to Google Drive                                                                                             |
| Drive sign in              | `backup_drive_signin`          | Sign in with Google                                                                                              |
| Drive signed in as         | `backup_drive_signed_in`       | Signed in as %1$s                                                                                                |
| Drive sign out             | `backup_drive_signout`         | Sign out                                                                                                         |
| Drive upload now           | `backup_drive_upload_now`      | Upload now                                                                                                       |
| Drive upload running       | `backup_drive_upload_running`  | Uploading…                                                                                                       |
| Drive auto-upload          | `backup_drive_auto_upload`     | Auto-upload after each local backup                                                                              |
| Drive last upload          | `backup_drive_last_upload`     | Last upload • %1$s                                                                                               |
| Drive explainer            | `backup_drive_explainer`       | Backups are encrypted with your passphrase before upload — nothing readable leaves your device.                  |
| Drive not configured       | `backup_drive_not_configured`  | Drive isn't configured. See docs/locale/06-google-cloud-setup.md.                                                |
| Drive open docs            | `backup_drive_open_docs`       | Open setup docs                                                                                                  |
| Drive when disabled        | `backup_drive_when_disabled`   | When enabled, encrypted backups can be uploaded to your Drive.                                                   |
| Banner: passphrase missing | `backup_banner_passphrase`     | Auto-backup is on but no passphrase is set. Set one to start backing up.                                         |
| Banner: low space          | `backup_banner_low_space`      | Not enough space — free at least 50 MB.                                                                          |
| Restore confirm 1          | `backup_restore_confirm1`      | This will REPLACE everything in callNest with the contents of %1$s. Type DELETE to continue.                     |
| Restore confirm 2          | `backup_restore_confirm2`      | Last chance — your current calls, tags, rules, and notes will be wiped. Continue?                                |
| Restore done               | `backup_restore_done`          | Restored from %1$s.                                                                                              |
| Restore wrong passphrase   | `backup_restore_wrong_pass`    | That passphrase doesn't match this backup.                                                                       |
| Restore corrupt            | `backup_restore_corrupt`       | This file isn't a callNest backup, or it's corrupted.                                                            |
| Backup done                | `backup_done`                  | Backup saved to your Downloads folder.                                                                           |
| Passphrase dialog title    | `backup_pass_dialog_title`     | Backup passphrase                                                                                                |
| Passphrase dialog body     | `backup_pass_dialog_body`      | There's no recovery — if you forget this passphrase, your backups can't be opened. Write it down somewhere safe. |
| Passphrase dialog field 1  | `backup_pass_dialog_f1`        | New passphrase                                                                                                   |
| Passphrase dialog field 2  | `backup_pass_dialog_f2`        | Confirm passphrase                                                                                               |
| Passphrase show            | `backup_pass_dialog_show`      | Show as text                                                                                                     |
| Passphrase save            | `backup_pass_dialog_save`      | Save                                                                                                             |
| Passphrase mismatch        | `backup_pass_dialog_mismatch`  | The two entries don't match.                                                                                     |
| Passphrase too short       | `backup_pass_dialog_short`     | Use at least 8 characters.                                                                                       |

### 30.14 ASCII wireframes

#### Wireframe — defaults, no passphrase, drive off

```
┌──────────────────────────────────────────────────┐
│ ←  Backup & restore                              │
├──────────────────────────────────────────────────┤
│  🛡️                                              │
│  Backup & restore                                │
│  Keep your data safe — locally and in your       │
│  Drive.                                          │
│                                                  │
│  ⚠ Auto-backup is on but no passphrase is set.   │
│    Set one to start backing up.   [Set passphrase]│
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Local             Last backup • Never    │    │
│  ├──────────────────────────────────────────┤    │
│  │ Auto-backup                       [ ●━○ ]│    │
│  │ Keep last 7 backups                       │    │
│  │ ───────────────●───────────────           │    │
│  │ 3              7              14          │    │
│  │                                          │    │
│  │ [    Manual backup now    ]              │    │
│  │ [    Restore from file    ]              │    │
│  │                                          │    │
│  │ Backup passphrase                        │    │
│  │ Not set                            [Set] │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Cloud (Google Drive)              [ ○━━●]│    │
│  │ Save to Google Drive            (off)    │    │
│  │ When enabled, encrypted backups can be   │    │
│  │ uploaded to your Drive.                  │    │
│  └──────────────────────────────────────────┘    │
└──────────────────────────────────────────────────┘
```

#### Wireframe — passphrase set, signed in, all on

```
│  ┌──────────────────────────────────────────┐    │
│  │ Local        Last backup • 2h ago • 1.4MB│    │
│  ├──────────────────────────────────────────┤    │
│  │ Auto-backup                       [ ●━○ ]│    │
│  │ Keep last 7 backups                      │    │
│  │ [   Manual backup now   ]                │    │
│  │ [   Restore from file   ]                │    │
│  │ Backup passphrase                        │    │
│  │ Set                            [Change]  │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ Cloud (Google Drive)         [ ●━○ ]     │    │
│  │ Signed in as you@example.com  [Sign out] │    │
│  │ [   Upload now   ]                       │    │
│  │ Auto-upload after each backup    [ ●━○ ] │    │
│  │ Last upload • yesterday at 21:04         │    │
│  │ ────────────────────────────────────     │    │
│  │ Backups are encrypted with your passphrase│   │
│  │ before upload — nothing readable leaves   │   │
│  │ your device.                              │   │
│  └──────────────────────────────────────────┘    │
```

#### Wireframe — Drive not configured

```
│  ┌──────────────────────────────────────────┐    │
│  │ Cloud (Google Drive)                     │    │
│  ├──────────────────────────────────────────┤    │
│  │ Drive isn't configured. See              │    │
│  │ docs/locale/06-google-cloud-setup.md.    │    │
│  │ [ Open setup docs ]                      │    │
│  └──────────────────────────────────────────┘    │
```

#### Wireframe — restore in progress

```
┌──────────────────────────────────────────────────┐
│                                                  │
│                                                  │
│           Restoring — don't close the app.       │
│                                                  │
│           ▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░  62 %              │
│                                                  │
│           Reading callNest-2026-04-…            │
│                                                  │
└──────────────────────────────────────────────────┘
```

### 30.15 Accessibility

- All toggles have `Role.Switch` with explicit on/off state strings.
- The retention slider has `valueRange = 3f..14f`,
  `steps = 10`, and a `stateDescription` that announces
  `"Keep last {n} backups"`.
- The two cards are headings (`Modifier.semantics { heading() }`).
- Status pills use `accessibilityLiveRegion = Polite`.
- The restore overlay is announced as `Role.ProgressBar` with
  percentage; talkback pauses gestures until it's gone.
- Set/Change dialog announces the passphrase warning before the
  fields, so screen-reader users hear the no-recovery rule first.
- Passphrase fields default to `KeyboardType.Password`; the show-as-
  text toggle is announced as `"Show passphrase"`.
- Min 48×48 dp tap targets across the screen.
- Drive sign-in is the system flow — it inherits Google's a11y.

### 30.15 Performance budget

- Initial render: ≤300 ms (cards have <30 children combined).
- Manual backup: ≤2 s for 10k calls, ≤6 s for 100k calls on a
  Pixel 4a. Compression dominates; we run on `Dispatchers.IO`.
- Restore: ≤4 s for 10k calls on a Pixel 4a. Single transaction.
- Upload: bounded by network. We use `okhttp` resumable uploads.
- Memory: peak <40 MB during compress; we stream gzip.
- DataStore writes (toggle/slider): <20 ms each.
- Drive status flow: emits at most once per minute under steady
  state.

---

## 31 — Export screen (5-step wizard)

### 31.1 Purpose

The Export screen is a guided wizard that walks the user through:

1. Picking an export format (Excel / CSV / PDF / JSON / vCard).
2. Picking a date range.
3. Picking the scope (current filter vs all data).
4. Picking columns (Excel/CSV only).
5. Picking a destination (Downloads vs custom URI).

…and then runs the export. It's distinct from the QuickExport sheet
(§32), which is a one-tap shortcut with no choices.

### 31.2 Entry points

| From                  | Trigger                                                          |
| --------------------- | ---------------------------------------------------------------- |
| MoreScreen            | "Export…" row.                                                   |
| Calls list overflow   | "Export this view" → opens with scope pre-set to current filter. |
| Stats screen overflow | "Export stats as PDF" → format pre-set to PDF.                   |
| In-app docs           | "Try exporting now" link.                                        |
| Deep link             | `callNest://export`.                                             |

### 31.3 Exit points

| To                                     | Trigger                       |
| -------------------------------------- | ----------------------------- |
| Previous screen                        | Cancel / back at step 1.      |
| File picker (`ACTION_CREATE_DOCUMENT`) | At step 5 if "Pick location". |
| `ProgressDialog`                       | After Generate.               |
| Snackbar with Open / Share             | On success.                   |
| `StandardError` snackbar               | On failure.                   |

### 31.4 Required inputs (data)

```
data class ExportUiState(
    val step: Int,                 // 1..5
    val format: ExportFormat?,
    val rangePreset: RangePreset?,
    val customRange: LongRange?,
    val scope: Scope,              // CurrentFilter | AllData
    val columns: Set<Column>,      // valid for Excel/CSV
    val destination: Destination,  // Downloads | Pick(uri)
    val customUri: Uri?,
    val isGenerating: Boolean,
    val progress: Float,
    val rowsExported: Int,
    val totalRows: Int?,
    val resultPath: String?,
    val resultSizeBytes: Long?,
    val error: ExportError?,
)
```

The screen also needs the upstream FilterState to show the user how
many rows their "current filter" scope represents:

- `FilterRepository.observeFilterState(): Flow<FilterState>`.
- `CallRepository.countByFilter(filterState): suspend Long`.

### 31.5 Required inputs (user)

- Step 1: Tap one of 5 format cards.
- Step 2: Tap a preset chip OR pick a custom range.
- Step 3: Tap one of 2 radios.
- Step 4 (Excel/CSV only): Toggle 12 column switches.
- Step 5: Tap a radio; if "Pick location", launch the file picker.
- Bottom bar: Back / Next-or-Generate.

### 31.6 Mandatory display

```
NeoScaffold(
    topBar = TopAppBar(title = "Export", actions = [Cancel]),
    bottomBar = WizardNav(back, primary),
)
```

Each step occupies the full body. Step indicator at top: `1 / 5`,
`2 / 5`, etc., with a thin progress underline.

#### Step 1 — Format

A 2-column grid of 5 `NeoCard`s:

| Index | Emoji | Title | Subtitle                           |
| ----- | ----- | ----- | ---------------------------------- |
| 1     | 📊    | Excel | "Multi-sheet workbook (.xlsx)"     |
| 2     | 📄    | CSV   | "Single comma-separated file"      |
| 3     | 📑    | PDF   | "Printable, includes stats"        |
| 4     | 💾    | JSON  | "Structured, optionally encrypted" |
| 5     | 📇    | vCard | "Contacts only (vCard 3.0)"        |

Tapping selects; selection is shown as a 2 dp coloured border.

#### Step 2 — Date range

Preset chips: `Today`, `Last 7 days`, `Last 30 days`, `This month`,
`Last month`, `Custom`. When `Custom` is chosen, two date pickers
appear (from / to), and a row reads `"X days, Y calls"` showing the
live count.

#### Step 3 — Scope

```
Radio:
  ●  Current filter         (12 calls)
  ○  All data               (1,872 calls)
```

When the user has no active filter, "Current filter" is greyed out
with a helper text `"You haven't filtered the calls list, so this is
the same as All data."`

#### Step 4 — Columns (Excel / CSV only)

Skipped automatically for PDF / JSON / vCard. A list of 12 toggles:

| Toggle               | Default |
| -------------------- | ------- |
| Date & time          | ON      |
| Number               | ON      |
| Contact name         | ON      |
| Type (in/out/missed) | ON      |
| Duration             | ON      |
| SIM slot             | ON      |
| Tags                 | ON      |
| Notes                | OFF     |
| Lead score           | OFF     |
| Geocoded location    | OFF     |
| Bookmarked           | OFF     |
| Archived             | OFF     |

A subtle helper at the bottom reads `"At least one column must be
selected."`. The Generate button disables otherwise.

#### Step 5 — Destination

Radios:

- `●  Downloads folder` — saves to public `Downloads/callNest/`.
- `○  Pick location…` — launches `ACTION_CREATE_DOCUMENT` with a
  suggested filename `callNest-2026-04-30-1430.xlsx`.

Below the radios, a 1-line preview: `"Saving as
callNest-2026-04-30-1430.xlsx (~ 240 KB est.)"`.

#### Bottom bar

| State      | Left button | Right button                        |
| ---------- | ----------- | ----------------------------------- |
| Step 1     | (hidden)    | Next (disabled until format chosen) |
| Step 2..4  | Back        | Next                                |
| Step 5     | Back        | Generate                            |
| Generating | (disabled)  | Cancel                              |

#### Progress dialog

Shown over the screen during `isGenerating`:

```
NeoCard {
    NeoProgressBar(progress)
    Text("Exporting %d / %d…")
    NeoTextButton("Cancel")
}
```

#### Success snackbar

`"Saved callNest-2026-04-30-1430.xlsx (240 KB)"` with actions
`Open` and `Share`. Stays 8 s. Tapping Open uses
`FileProvider` + `ACTION_VIEW`. Tapping Share uses `ACTION_SEND`.

### 31.7 Optional display

- A tiny "Estimate" pill under each step's title showing how big the
  output will be. Updates as choices change.
- A "Save these settings as a preset" link at step 5 (deferred —
  Sprint 13+).
- A history of last 5 exports under the toolbar overflow (deferred —
  Sprint 13+).

### 31.8 Empty state

There's no empty state — the wizard always renders. However, at step
3 if `countByFilter == 0L` AND `scope == CurrentFilter`, show a
warning `"No calls match your current filter — pick All data or
adjust your filter first."` and disable Next.

### 31.9 Loading state

- The "current filter" count and "all data" count are shown as
  shimmer placeholders while loading.
- During generate, the screen is overlaid by the progress dialog and
  bottom bar disables.

### 31.10 Error state

| Error                                      | Surface                                                                                                        |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------- |
| Zero rows after filter                     | Inline at step 3 (see above).                                                                                  |
| Pick-location URI revoked                  | At generate time, snackbar `"Couldn't write to that location. Pick again or use Downloads."`                   |
| Low disk space                             | Pre-flight check at generate; snackbar `"Not enough space — free %d MB and try again."`                        |
| Format-specific error (e.g. POI exception) | Snackbar `"Export failed: {message}"` with Retry.                                                              |
| User cancels mid-run                       | Toast `"Export cancelled."`                                                                                    |
| 100k rows                                  | Allowed but with a banner before generate `"This is a big export — it may take a minute and 50+ MB of space."` |

### 31.11 Edge cases

| Case                            | Handling                                                                                                                                                           |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 100k row export                 | Excel: chunked write, 5,000 rows per `SXSSFWorkbook` flush. CSV: streamed line-by-line. PDF: pages capped at 50 by default; user can opt into "Full" via overflow. |
| Zero rows after filter          | See above.                                                                                                                                                         |
| Pick-location URI revoked       | We fall back to Downloads after a confirm.                                                                                                                         |
| Custom range from > to          | Validation blocks Next at step 2.                                                                                                                                  |
| Non-default locale              | Excel / CSV use locale-aware date format with explicit ISO-8601 column option in advanced mode.                                                                    |
| User toggles all 12 columns off | Generate disables.                                                                                                                                                 |
| Pause during background work    | The progress survives configuration change because the use-case runs in a `WorkManager` job; UI re-binds to the same job by id.                                    |
| Cancel mid-run                  | The use-case checks `isActive` between row chunks; partial files are deleted.                                                                                      |
| Encryption (JSON only)          | Optional checkbox at step 1 if JSON is chosen — `"Encrypt with backup passphrase"`. Reuses backup KDF.                                                             |

### 31.12 Format behaviour table

| Format                   | Sheets                                                                                                                                                                         | Notes |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----- |
| Excel (.xlsx)            | Sheet 1 "Calls" (one row per call), Sheet 2 "Tags" (one row per tag with usage), Sheet 3 "Summary" (totals). Uses `SXSSFWorkbook` for streaming.                               |
| CSV (.csv)               | Single sheet. UTF-8 with BOM (so Excel reads correctly). RFC 4180 quoting.                                                                                                     |
| PDF (.pdf)               | A4 portrait. Cover page with date range + total counts. Body: 1 row per call, 25 rows per page, with optional stats charts on the cover (currently 4/10 charts implemented).   |
| JSON (.json or .json.cv) | Top-level keys: `meta`, `calls`, `tags`, `rules`, `notes`. Pretty-printed. Optional encryption uses the backup passphrase + the same `.cvb` envelope but with extension `.cv`. |
| vCard (.vcf)             | vCard 3.0. One `BEGIN:VCARD` block per unique contact in the selected range. Includes FN, TEL (multiple), CATEGORIES (tags), NOTE.                                             |

> NOTE: The mega-spec lists 10 stats charts for the PDF. The current
> implementation ships 4: calls-by-day, calls-by-tag, calls-by-type,
> top-numbers. The remaining 6 (avg duration / hour-of-day heatmap /
> SIM split / lead-score distribution / win-rate by tag / streak
> calendar) are deferred. Recorded in `DECISIONS.md`.

### 31.13 Copy table

| Key                    | Resource id               | English                                                                         |
| ---------------------- | ------------------------- | ------------------------------------------------------------------------------- |
| Title                  | `export_title`            | Export                                                                          |
| Cancel                 | `export_cancel`           | Cancel                                                                          |
| Step indicator         | `export_step_indicator`   | %1$d of 5                                                                       |
| Step 1 header          | `export_step1_header`     | Pick a format                                                                   |
| Format Excel           | `export_fmt_xlsx`         | Excel                                                                           |
| Format Excel sub       | `export_fmt_xlsx_sub`     | Multi-sheet workbook (.xlsx)                                                    |
| Format CSV             | `export_fmt_csv`          | CSV                                                                             |
| Format CSV sub         | `export_fmt_csv_sub`      | Single comma-separated file                                                     |
| Format PDF             | `export_fmt_pdf`          | PDF                                                                             |
| Format PDF sub         | `export_fmt_pdf_sub`      | Printable, includes stats                                                       |
| Format JSON            | `export_fmt_json`         | JSON                                                                            |
| Format JSON sub        | `export_fmt_json_sub`     | Structured, optionally encrypted                                                |
| Format vCard           | `export_fmt_vcf`          | vCard                                                                           |
| Format vCard sub       | `export_fmt_vcf_sub`      | Contacts only (vCard 3.0)                                                       |
| Step 2 header          | `export_step2_header`     | Pick a date range                                                               |
| Range Today            | `export_range_today`      | Today                                                                           |
| Range 7 days           | `export_range_7d`         | Last 7 days                                                                     |
| Range 30 days          | `export_range_30d`        | Last 30 days                                                                    |
| Range this month       | `export_range_this_month` | This month                                                                      |
| Range last month       | `export_range_last_month` | Last month                                                                      |
| Range custom           | `export_range_custom`     | Custom                                                                          |
| Range live count       | `export_range_count`      | %1$d days, %2$d calls                                                           |
| Range invalid          | `export_range_invalid`    | Pick a from-date before the to-date.                                            |
| Step 3 header          | `export_step3_header`     | Pick a scope                                                                    |
| Scope filter           | `export_scope_filter`     | Current filter (%1$d calls)                                                     |
| Scope all              | `export_scope_all`        | All data (%1$d calls)                                                           |
| Scope no filter helper | `export_scope_no_filter`  | You haven't filtered the calls list, so this is the same as All data.           |
| Scope zero warn        | `export_scope_zero`       | No calls match your current filter — pick All data or adjust your filter first. |
| Step 4 header          | `export_step4_header`     | Pick columns                                                                    |
| Step 4 caption         | `export_step4_caption`    | At least one column must be selected.                                           |
| Col date               | `export_col_date`         | Date & time                                                                     |
| Col number             | `export_col_number`       | Number                                                                          |
| Col name               | `export_col_name`         | Contact name                                                                    |
| Col type               | `export_col_type`         | Type                                                                            |
| Col duration           | `export_col_duration`     | Duration                                                                        |
| Col sim                | `export_col_sim`          | SIM slot                                                                        |
| Col tags               | `export_col_tags`         | Tags                                                                            |
| Col notes              | `export_col_notes`        | Notes                                                                           |
| Col lead               | `export_col_lead`         | Lead score                                                                      |
| Col geo                | `export_col_geo`          | Geocoded location                                                               |
| Col bookmark           | `export_col_bookmark`     | Bookmarked                                                                      |
| Col archive            | `export_col_archive`      | Archived                                                                        |
| Step 5 header          | `export_step5_header`     | Pick a destination                                                              |
| Dest downloads         | `export_dest_downloads`   | Downloads folder                                                                |
| Dest pick              | `export_dest_pick`        | Pick location…                                                                  |
| Dest preview           | `export_dest_preview`     | Saving as %1$s (~ %2$s est.)                                                    |
| Big warning            | `export_big_warn`         | This is a big export — it may take a minute and 50+ MB of space.                |
| Generate               | `export_generate`         | Generate                                                                        |
| Generating             | `export_generating`       | Exporting %1$d / %2$d…                                                          |
| Cancel run             | `export_cancel_run`       | Cancel                                                                          |
| Cancelled              | `export_cancelled`        | Export cancelled.                                                               |
| Success                | `export_success`          | Saved %1$s (%2$s)                                                               |
| Open                   | `export_open`             | Open                                                                            |
| Share                  | `export_share`            | Share                                                                           |
| Failure                | `export_fail`             | Export failed: %1$s                                                             |
| Retry                  | `export_retry`            | Retry                                                                           |
| Encryption optional    | `export_json_encrypt`     | Encrypt with backup passphrase                                                  |

### 31.14 ASCII wireframes

#### Step 1 — Format

```
┌──────────────────────────────────────────────────┐
│ ←  Export                                        │
│ ────────────────────────                         │
│  1 of 5                                          │
│                                                  │
│  Pick a format                                   │
│                                                  │
│  ┌────────────────┐  ┌────────────────┐          │
│  │ 📊 Excel       │  │ 📄 CSV         │          │
│  │ Multi-sheet    │  │ Comma-sep…     │          │
│  └────────────────┘  └────────────────┘          │
│  ┌────────────────┐  ┌────────────────┐          │
│  │ 📑 PDF         │  │ 💾 JSON        │          │
│  │ Printable…     │  │ Structured…    │          │
│  └────────────────┘  └────────────────┘          │
│  ┌────────────────┐                              │
│  │ 📇 vCard       │                              │
│  │ Contacts only… │                              │
│  └────────────────┘                              │
│                                                  │
│ ───────────────────────────────────────────      │
│                                       [ Next ]   │
└──────────────────────────────────────────────────┘
```

#### Step 2 — Date range

```
│  2 of 5                                          │
│  Pick a date range                               │
│                                                  │
│  [Today] [Last 7 days] [Last 30 days]            │
│  [This month] [Last month] [Custom ●]            │
│                                                  │
│  From  ┌──────────────┐   To  ┌──────────────┐   │
│        │ 2026-04-01   │      │ 2026-04-30   │   │
│        └──────────────┘      └──────────────┘   │
│                                                  │
│  30 days, 1,124 calls                            │
│                                                  │
│ [ Back ]                              [ Next ]   │
```

#### Step 3 — Scope

```
│  3 of 5                                          │
│  Pick a scope                                    │
│                                                  │
│  ●  Current filter            (87 calls)         │
│  ○  All data                  (1,872 calls)      │
│                                                  │
│ [ Back ]                              [ Next ]   │
```

#### Step 4 — Columns (Excel/CSV)

```
│  4 of 5                                          │
│  Pick columns                                    │
│                                                  │
│  Date & time              [ ●━○ ]                │
│  Number                   [ ●━○ ]                │
│  Contact name             [ ●━○ ]                │
│  Type                     [ ●━○ ]                │
│  Duration                 [ ●━○ ]                │
│  SIM slot                 [ ●━○ ]                │
│  Tags                     [ ●━○ ]                │
│  Notes                    [ ○━━●]                │
│  Lead score               [ ○━━●]                │
│  Geocoded location        [ ○━━●]                │
│  Bookmarked               [ ○━━●]                │
│  Archived                 [ ○━━●]                │
│                                                  │
│  At least one column must be selected.           │
│                                                  │
│ [ Back ]                              [ Next ]   │
```

#### Step 5 — Destination

```
│  5 of 5                                          │
│  Pick a destination                              │
│                                                  │
│  ●  Downloads folder                             │
│  ○  Pick location…                               │
│                                                  │
│  Saving as callNest-2026-04-30-1430.xlsx        │
│  (~ 240 KB est.)                                 │
│                                                  │
│ [ Back ]                          [ Generate ]   │
```

#### Generating

```
┌──────────────────────────────────────────────────┐
│            ┌──────────────────────────────┐      │
│            │   Exporting 1,234 / 1,872…   │      │
│            │   ▓▓▓▓▓▓▓▓░░░░░░░  66 %       │      │
│            │            [ Cancel ]         │      │
│            └──────────────────────────────┘      │
└──────────────────────────────────────────────────┘
```

### 31.15 Accessibility

- Step indicator is read aloud at each step transition: `"Step 3 of
5: Pick a scope"`. `accessibilityLiveRegion = Polite`.
- Format cards have `Role.RadioButton` + `selected` state.
- Range chip group has `Role.RadioButton` semantics.
- Date pickers fall back to platform pickers.
- Column toggles are `Role.Switch`.
- Bottom-bar primary button announces its disabled reason via
  `stateDescription` (e.g. `"Generate unavailable. Pick at least one
column."`).
- The progress dialog uses `Role.ProgressBar` and announces
  percentage every 10%.
- Snackbar is announced as `accessibilityLiveRegion = Assertive`.
- Min 48×48 dp on every chip and toggle.
- Cancel during run is reachable in two taps.

### 31.16 Performance budget

- Step transitions: ≤16 ms (single frame).
- Live count queries (steps 2–3): ≤80 ms on 100k DB; debounced 150 ms.
- Excel export: ≤8 s for 10k rows; ≤90 s for 100k rows on a Pixel 4a.
  Streamed via `SXSSFWorkbook(100)`.
- CSV export: ≤2 s for 10k rows; ≤25 s for 100k rows.
- PDF export: ≤6 s for 1,000 rows + 4 charts.
- JSON export: ≤1 s for 10k rows.
- vCard export: ≤1 s for 1,000 contacts.
- Memory: peak <80 MB during Excel export thanks to streaming.
- Cancel responsiveness: ≤500 ms from tap to UI dismiss.

---

## 32 — QuickExport sheet

### 32.1 Purpose

The QuickExport sheet is a bottom-sheet shortcut for the three most
common export types. It exists so power users can dump their current
view without walking through five wizard steps. It's a sheet, not a
route — it's mounted inside whichever screen invokes it, and dismissed
by sliding down or tapping outside.

The three options are deliberate:

1. CSV of the current filter.
2. Excel workbook of the current filter.
3. JSON of the entire DB (for archival / migration).

### 32.2 Entry points

| From                  | Trigger                              |
| --------------------- | ------------------------------------ |
| Home (Dashboard)      | "Quick actions" chip "Quick export". |
| MainScaffold overflow | Top-right kebab → "Quick export".    |
| MoreScreen            | "Quick export" row.                  |

The sheet is implemented as a parent-controlled `NeoBottomSheet`. It
is NOT a route in the navigation graph — opening / closing is driven
by a `MutableStateFlow<Boolean>` in `MainViewModel`.

### 32.3 Exit points

| To                                   | Trigger                                    |
| ------------------------------------ | ------------------------------------------ |
| Caller (sheet dismisses)             | Slide down, tap outside, hardware back.    |
| Caller (sheet stays, status updates) | Tap any of the 3 cards while idle / error. |
| File viewer                          | Tap "Open" in success state.               |
| System share sheet                   | Tap "Share" in success state.              |

### 32.4 Required inputs (data)

```
sealed interface QuickExportState {
    object Idle : QuickExportState
    data class Running(val format: ExportFormat) : QuickExportState
    data class Success(val format: ExportFormat, val path: String,
                        val sizeBytes: Long, val openIntent: Intent,
                        val shareIntent: Intent) : QuickExportState
    data class Error(val format: ExportFormat, val message: String) : QuickExportState
}
```

`QuickExportViewModel` owns this state. It also reads
`FilterRepository.observeFilterState()` to know what "current filter"
means at the moment of the click.

### 32.5 Required inputs (user)

- Tap one of three cards.
- Tap "Open" / "Share" / "Retry" in success / error.

### 32.6 Mandatory display

```
NeoBottomSheet(
    isVisible = ...,
    onDismiss = ...,
) {
    Header(title = "Quick Export", subtitle = "Saves to your Downloads folder.")
    NeoCard("📄 CSV (current filter)") { onClick = startCsv }
    NeoCard("📊 Excel workbook (current filter)") { onClick = startXlsx }
    NeoCard("💾 Whole DB as JSON") { onClick = startJson }
    StatusRow(state)
}
```

The status row at the bottom takes 64 dp and switches between four
visuals:

- Idle: empty (just a 1 dp top border).
- Running: `NeoLoader` 36 dp + `"Exporting…"` text.
- Success: `NeoCard` with green border, copy `"Exported %1$s (%2$d
KB)"`, and three buttons: `Open`, `Share`, `Retry`.
- Error: `NeoCard` with red border, copy `"Couldn't export: %1$s"`,
  and one button: `Retry`.

#### Auto-dismiss

In Success state, after 3 s of inactivity, the sheet auto-dismisses.
If the user taps Open / Share / Retry, the timer is cancelled.

### 32.7 Optional display

- A "Last quick export 2h ago" caption under the header, only when a
  previous quick export exists in DataStore (`lastQuickExportAt`).
- A "Wizard?" link in the top-right of the sheet header that closes
  the sheet and opens the full Export wizard preserving any picked
  format.

### 32.8 Empty state

There's no empty state for the sheet itself. However, if the current
filter would yield zero rows (CSV / Excel cards), tapping the card
shows the Error state without running the export, with copy `"No
calls match your current filter."`

### 32.9 Loading state

While `Running`:

- The three cards are disabled (alpha 0.5).
- The status row shows the loader.
- The sheet cannot be dismissed via tap outside (we treat in-flight
  exports as needing acknowledgement). Hardware back triggers a
  "Cancel?" confirm.

### 32.10 Error state

| Cause                          | Surface                                                                                                   |
| ------------------------------ | --------------------------------------------------------------------------------------------------------- |
| Filter not yet hydrated (race) | We fall back to default `FilterState` and proceed. No error shown.                                        |
| Downloads dir unwritable       | Auto-fallback to `cacheDir/quick-exports/`; the path label changes accordingly and we still show success. |
| Export failed (POI / IO)       | Error state with `Retry`.                                                                                 |
| Cancelled mid-run              | Returns to Idle, no banner.                                                                               |
| Zero rows for CSV/Excel        | Error: `"No calls match your current filter."` with Retry disabled.                                       |

### 32.11 Edge cases

| Case                              | Handling                                                                            |
| --------------------------------- | ----------------------------------------------------------------------------------- |
| Filter hydration race             | Use `FilterState.Default` (no filters).                                             |
| Downloads dir unwritable          | Fallback to `cacheDir`.                                                             |
| Export cancelled mid-run          | Sheet returns to Idle, no toast.                                                    |
| User reopens sheet during Running | Sheet stays in Running; no second job is queued.                                    |
| User reopens sheet after Success  | Sheet shows Success again with the same Open/Share intents (so they can re-share).  |
| User reopens after Error          | Sheet shows Error with Retry.                                                       |
| Two concurrent quick exports      | Blocked by the ViewModel's `Mutex`; second tap is a no-op.                          |
| Whole DB JSON > 100 MB            | Allowed; we stream-write. Sheet shows progress percentage in the status row's text. |

### 32.12 Copy table

| Key             | Resource id         | English                             |
| --------------- | ------------------- | ----------------------------------- |
| Header title    | `qe_title`          | Quick Export                        |
| Header subtitle | `qe_subtitle`       | Saves to your Downloads folder.     |
| CSV card        | `qe_card_csv`       | 📄 CSV (current filter)             |
| Excel card      | `qe_card_xlsx`      | 📊 Excel workbook (current filter)  |
| JSON card       | `qe_card_json`      | 💾 Whole DB as JSON                 |
| Status running  | `qe_status_running` | Exporting…                          |
| Status success  | `qe_status_success` | Exported %1$s (%2$s)                |
| Status error    | `qe_status_error`   | Couldn't export: %1$s               |
| Status zero     | `qe_status_zero`    | No calls match your current filter. |
| Open            | `qe_open`           | Open                                |
| Share           | `qe_share`          | Share                               |
| Retry           | `qe_retry`          | Retry                               |
| Last export     | `qe_last_export`    | Last quick export %1$s ago          |
| Wizard link     | `qe_wizard_link`    | Use the full wizard                 |

### 32.13 ASCII wireframe (4 states)

#### Idle

```
            ┌──────────────────────────────────┐
            │  Quick Export             ─      │
            │  Saves to your Downloads folder. │
            ├──────────────────────────────────┤
            │ ┌──────────────────────────────┐ │
            │ │ 📄 CSV (current filter)      │ │
            │ └──────────────────────────────┘ │
            │ ┌──────────────────────────────┐ │
            │ │ 📊 Excel workbook            │ │
            │ │    (current filter)          │ │
            │ └──────────────────────────────┘ │
            │ ┌──────────────────────────────┐ │
            │ │ 💾 Whole DB as JSON          │ │
            │ └──────────────────────────────┘ │
            ├──────────────────────────────────┤
            │ (idle — no status)               │
            └──────────────────────────────────┘
```

#### Running

```
            ├──────────────────────────────────┤
            │  ◐  Exporting…                   │
            ├──────────────────────────────────┤
```

#### Success

```
            ├──────────────────────────────────┤
            │ ┌──────────────────────────────┐ │
            │ │ ✓ Exported callNest-2026-…  │ │
            │ │   (240 KB)                   │ │
            │ │ [ Open ]  [ Share ]  [ Retry]│ │
            │ └──────────────────────────────┘ │
            └──────────────────────────────────┘
   (auto-dismisses after 3 s if no tap)
```

#### Error

```
            ├──────────────────────────────────┤
            │ ┌──────────────────────────────┐ │
            │ │ ⚠ Couldn't export: not enough│ │
            │ │   space in Downloads.        │ │
            │ │ [ Retry ]                    │ │
            │ └──────────────────────────────┘ │
            └──────────────────────────────────┘
```

### 32.14 Accessibility

- The sheet is announced as `Role.Dialog` with title `"Quick Export.
Saves to your Downloads folder."`.
- Each card has `Role.Button` and `contentDescription` like
  `"Export CSV of the current filter, 87 calls."` (the count is read
  from the live filter snapshot).
- Status row uses `accessibilityLiveRegion = Assertive` so success /
  error announce immediately.
- The 3-second auto-dismiss is reset by any focus event from the
  user, so screen-reader users always have time to act.
- The sheet's swipe-down dismiss has a fallback `Close` action
  exposed via `customActions`.
- Min 48×48 dp tap targets.

### 32.15 Performance budget

- Sheet open animation: 250 ms.
- First card tap → first byte written: ≤200 ms.
- CSV (10k rows): ≤2 s end-to-end.
- Excel (10k rows): ≤8 s end-to-end.
- JSON whole DB (10k rows total): ≤1 s end-to-end.
- Memory: <30 MB extra during run.
- Auto-dismiss timer: lightweight `LaunchedEffect` keyed on the
  Success state.

---

## Appendix A — Cross-references

| Topic                                                                                                                                             | See                           |
| ------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------- |
| `BackupManager` algorithm                                                                                                                         | Part 01 §6.9                  |
| `RuleConditionEvaluator`                                                                                                                          | Part 01 §6.4                  |
| `LeadScoreCalculator`                                                                                                                             | Part 01 §6.7                  |
| `FilterState` model                                                                                                                               | Part 02 §15                   |
| `NeoCard` / `NeoButton` / `NeoTextField` / `NeoLoader` / `NeoProgressBar` / `NeoBottomSheet` / `StandardPage` / `StandardEmpty` / `StandardError` | Part 04 (component catalogue) |
| `AutoTagRuleRepository`                                                                                                                           | Part 01 §7.3                  |
| `TagRepository`                                                                                                                                   | Part 01 §7.2                  |
| `BackupRepository`                                                                                                                                | Part 01 §7.5                  |
| `DriveRepository`                                                                                                                                 | Part 01 §7.6                  |
| `ExportUseCase`                                                                                                                                   | Part 01 §6.10                 |
| `QuickExportViewModel`                                                                                                                            | Part 01 §8.4                  |

## Appendix B — Implementation deviations (recap)

1. **PBKDF2 over Tink keysets** — chosen because the only secret
   material is the user-entered passphrase; a Tink keyset would
   itself need wrapping by that passphrase, adding a layer with no
   security benefit. Recorded in `DECISIONS.md`.
2. **6/10 PDF stats charts deferred** — initial release ships
   calls-by-day, calls-by-tag, calls-by-type, top-numbers. Avg
   duration / hour-of-day heatmap / SIM split / lead-score
   distribution / win-rate by tag / streak calendar are deferred to
   post-v1. Recorded in `DECISIONS.md`.
3. **Quick Export "Whole DB JSON" is plaintext** — encryption is
   reserved for the wizard JSON path. Quick Export is a power-user
   shortcut; a passphrase prompt would defeat the "one-tap" promise.
4. **Tag emoji uses EmojiCompat for grapheme validation** — no Tink,
   no third-party emoji libs. Falls back to length check on devices
   without EmojiCompat init complete.
5. **Drive integration is feature-flagged** by `BuildConfig
.DRIVE_OAUTH_CONFIGURED`. The default release sets this `false`
   until the OAuth client is provisioned per the docs in
   `docs/locale/06-google-cloud-setup.md`.
6. **Rule preview cap of 200 calls** — keeps live recompute under
   300 ms even on a low-end Pixel 4a. Users with very rare match
   patterns can still see the preview number jump on next sync.
7. **Backup `.cvb` magic header bytes** — `CVB1`. Future versions
   bump to `CVB2`, etc., and the version-byte check refuses
   newer-format archives with a clear error.
8. **System tags are preserved across resets** — `Reset system tags`
   only restores the original 9 names/colours/emojis; user tags are
   never deleted by any UI affordance other than explicit per-tag
   deletion or merge.
9. **Rule actions order matters within a single rule** — `ApplyTag`
   then `LeadScoreBoost` differs subtly from the reverse only in
   logging order. The persisted JSON preserves insertion order via
   `kotlinx.serialization`'s default behaviour.
10. **Export wizard cancel is best-effort** — partial files are
    deleted on cancel; if cancellation arrives mid-write the file is
    truncated and removed before the snackbar fires.

## Appendix C — DataStore keys touched by this part

| Key                        | Type      | Default      | Owner screen  |
| -------------------------- | --------- | ------------ | ------------- |
| `auto_backup_enabled`      | Boolean   | `false`      | BackupScreen  |
| `backup_retention_days`    | Int       | `7`          | BackupScreen  |
| `backup_passphrase_hash`   | String?   | null         | BackupScreen  |
| `backup_drive_enabled`     | Boolean   | `false`      | BackupScreen  |
| `backup_drive_account`     | String?   | null         | BackupScreen  |
| `backup_auto_upload`       | Boolean   | `false`      | BackupScreen  |
| `last_local_backup_at`     | Long?     | null         | BackupScreen  |
| `last_drive_upload_at`     | Long?     | null         | BackupScreen  |
| `last_quick_export_at`     | Long?     | null         | QuickExport   |
| `last_quick_export_format` | String?   | null         | QuickExport   |
| `export_default_columns`   | StringSet | (7 defaults) | Export wizard |
| `export_last_destination`  | String    | `downloads`  | Export wizard |
| `export_last_range_preset` | String    | `last_30d`   | Export wizard |
| `rules_run_now_last_at`    | Long?     | null         | AutoTagRules  |
| `tags_last_reset_at`       | Long?     | null         | TagsManager   |

## Appendix D — Snackbar / toast inventory for this part

| Surface        | Trigger                   | Duration                  |
| -------------- | ------------------------- | ------------------------- |
| Snackbar       | Tag saved                 | Short                     |
| Snackbar       | Tag deleted               | Short                     |
| Snackbar       | Tags merged               | Long (with Undo)          |
| Toast          | Cannot delete system tag  | Short                     |
| Snackbar       | Rule enabled / disabled   | Short (with Undo)         |
| Snackbar       | Rule saved                | Short                     |
| Snackbar       | Rule deleted              | Long (with Undo)          |
| Snackbar       | Rule discard              | Short                     |
| Banner         | Backup passphrase missing | Persistent until resolved |
| Snackbar       | Backup done               | Long (with Open/Share)    |
| Snackbar       | Restore done              | Long                      |
| Snackbar       | Backup error              | Long (with Retry)         |
| Snackbar       | Drive sign-in expired     | Long (with Sign in)       |
| Snackbar       | Export success            | Long (with Open/Share)    |
| Snackbar       | Export error              | Long (with Retry)         |
| Snackbar       | Export cancelled          | Short                     |
| Inline (sheet) | Quick export success      | Auto-dismiss 3s           |
| Inline (sheet) | Quick export error        | Until acknowledged        |

## Appendix E — Failure modes summary table

| Failure                                | Detected by                                               | Recovery                                                  |
| -------------------------------------- | --------------------------------------------------------- | --------------------------------------------------------- |
| DB corruption affecting tags           | `TagRepository.observeAllTags` error                      | Show empty state with manual reseed CTA in BackupRestore. |
| Invalid rule JSON                      | Deserialiser exception in `AutoTagRuleRepository.loadAll` | Mark `isInvalid`, show in row.                            |
| Regex compile failure                  | `Pattern.compile` in `RegexMatches.matches`               | Editor live-error; runtime skip + `Timber.w`.             |
| Backup decryption failure (wrong pass) | `AEADBadTagException`                                     | "That passphrase doesn't match this backup."              |
| Backup magic mismatch                  | First 4 bytes != `CVB1`                                   | "This file isn't a callNest backup, or it's corrupted."   |
| Backup version too new                 | Magic bytes `CVB2`+                                       | "This backup was made on a newer version of callNest."    |
| Drive 401                              | OAuth token rejected                                      | Refresh; if fail, banner.                                 |
| Drive 403 quota                        | HTTP 403 reason `quotaExceeded`                           | "Your Drive is full."                                     |
| Drive 404 folder                       | HTTP 404 on folder GET                                    | Recreate folder.                                          |
| Export disk full                       | `IOException: ENOSPC`                                     | "Not enough space — free %d MB."                          |
| Export PI failure                      | `OutOfMemoryError` during POI                             | Fallback to CSV with banner explaining.                   |
| Export cancelled                       | Coroutine cancellation                                    | Snackbar "Export cancelled."                              |
| Quick export concurrent                | `Mutex.tryLock` returns false                             | No-op (already running).                                  |
| Quick export zero rows                 | `count == 0` after filter                                 | Error in status row.                                      |

## Appendix F — Telemetry (intentionally none)

Per the project directive in `CLAUDE.md` (§Don'ts):

> Don't add Firebase, Crashlytics, GA, or any analytics SDK. Spec
> §13: "Nothing leaves the device except update version checks."

Therefore none of the screens in this part emit any analytic events.
The only network egress paths are:

- The update-manifest fetch (covered in Part 06).
- Google Drive uploads (only when explicitly enabled by the user).

All other failure surfacing is local — Timber logs, in-memory state,
and snackbar copy.

## Appendix G — String-resource summary count

| Screen             | Strings (this part)                                |
| ------------------ | -------------------------------------------------- |
| TagsManagerScreen  | 30                                                 |
| AutoTagRulesScreen | 22                                                 |
| RuleEditor         | 49                                                 |
| BackupScreen       | 41                                                 |
| Export wizard      | 53                                                 |
| QuickExport sheet  | 12                                                 |
| **Total**          | **207 new strings to add to `values/strings.xml`** |

## Appendix H — Glossary (this part only)

- **System tag** — one of the 9 tags seeded on first DB create. Cannot
  be deleted; can be renamed/recoloured.
- **Cross-ref** — the `call_tag_cross_ref` join table; rows here are
  the source of truth for "which tags are on which calls".
- **Match count** — the number of calls in the latest 200 that satisfy
  every condition of a rule. Recomputed on edit and on sync.
- **`.cvb`** — callNest Backup. The file extension and magic header
  for our encrypted backup blobs.
- **PBKDF2** — Password-Based Key Derivation Function 2; we use HMAC-
  SHA256 with 120,000 iterations.
- **Quick export** — a one-tap shortcut for CSV/Excel/JSON without the
  wizard.
- **Wizard** — the 5-step Export screen.
- **Live preview** — the rule-editor's running match-count, debounced
  400 ms.
- **Discard** — drop unsaved edits; opposite of Save.
- **Reset system tags** — restore the 9 seeded tags' name/colour/emoji
  to defaults; user tags untouched.

## Appendix I — Outstanding TODOs surfaced by this part

These are the items this part adds to `TODO.md`. They are NOT
implementation requirements for this spec — they are followups that
become apparent when the spec is read in full.

1. P1 — Implement the remaining 6 PDF stats charts.
2. P2 — Add a "Save these export settings as a preset" affordance.
3. P2 — Add a "Last 5 exports" history sheet.
4. P3 — Allow a third SIM slot (eSIM-only devices that expose >2).
5. P3 — Allow "Apply tag" rule action to apply multiple tags in one
   action (currently one tag per action).
6. P3 — Localise the curated business-emoji list per locale (e.g.
   Indian-context emojis variant).
7. P3 — Add a "Test this passphrase" affordance in the Set/Change
   dialog that decrypts the latest backup as a sanity check.
8. P3 — Allow custom backup folder pickers for users who prefer a
   non-Downloads location.
9. P3 — Surface the `auto-tag rule "Run now" history` as a sheet so
   users can see when a manual re-run last touched their data.
10. P3 — Add a "Why was this tagged?" affordance on call rows that
    deep-links into the matching rule's editor.

## Appendix J — Keyboard shortcut map (hardware keyboard users)

callNest is an Android phone app, but a non-trivial fraction of
power users plug in a Bluetooth keyboard. The screens in this part
react to the following key combinations when an external keyboard is
attached:

| Screen       | Key                        | Action                                       |
| ------------ | -------------------------- | -------------------------------------------- |
| TagsManager  | `/`                        | Focus the search field.                      |
| TagsManager  | `Esc`                      | Clear search if focused, else navigate back. |
| TagsManager  | `Enter` (on row)           | Open editor.                                 |
| TagsManager  | `Delete` (on row)          | Trigger swipe-delete confirm.                |
| TagsManager  | `N`                        | New tag (FAB).                               |
| AutoTagRules | `N`                        | New rule.                                    |
| AutoTagRules | `Space` (on row)           | Toggle active.                               |
| AutoTagRules | `Alt+Up/Down` (on row)     | Reorder.                                     |
| RuleEditor   | `Ctrl+S`                   | Save.                                        |
| RuleEditor   | `Esc`                      | Discard / back.                              |
| RuleEditor   | `Ctrl+D`                   | Duplicate.                                   |
| Backup       | `B`                        | Trigger manual backup.                       |
| Backup       | `R`                        | Open restore picker.                         |
| Export       | `Right Arrow` / `Tab`      | Next step.                                   |
| Export       | `Left Arrow` / `Shift+Tab` | Previous step.                               |
| Export       | `Enter`                    | Activate primary button.                     |
| QuickExport  | `1` / `2` / `3`            | Trigger CSV / Excel / JSON.                  |
| QuickExport  | `Esc`                      | Dismiss sheet.                               |

These are wired via `Modifier.onKeyEvent { … }` at the screen-level
composables, gated by the platform's `KeyEvent.isCtrlPressed` and
`isAltPressed` helpers.

## Appendix K — Notes on dark mode

All screens in this part respect `MaterialTheme.colorScheme` and
follow these conventions:

- Tag chips use the user's configured `colorHex` as background; the
  text colour is derived via WCAG-grade luminance check, not the
  theme. So a "Customer" tag stays green-on-white in light mode and
  green-on-white in dark mode (the chip is filled).
- Rule rows in the AutoTagRules screen draw on `surface` with `1 dp`
  hairlines on `outlineVariant`.
- The BackupScreen banners use `errorContainer` /
  `tertiaryContainer` for warning / success states, which automatically
  pick the dark-mode-appropriate token.
- The Export wizard's bottom bar uses `surfaceContainerLowest` so it
  reads as a separate plane in both modes.
- The QuickExport sheet inherits the platform sheet container colour
  (`surfaceContainerHigh`).
- All emojis render identically across modes (Android System UI
  emoji font handles its own contrast).
- Selection state on format cards uses a 2 dp `primary` border in
  both modes; in light mode the fill is `primaryContainer`, in dark
  mode the fill is `primary` at 20 % alpha.

## Appendix L — Notes on right-to-left support

callNest's launch locale is en-IN, but the spec requires layout
direction support for future localisation. Specific notes:

- `LazyColumn` rows in TagsManager and AutoTagRules already mirror
  correctly because they use `Row { … }` with `Spacer(weight = 1f)`
  and trailing chevrons.
- The drag handles in AutoTagRules are mirrored (the up/down arrows
  do not flip — they're vertical).
- The Export wizard's progress bar at the top reverses direction in
  RTL.
- The Backup sliders use the platform `Slider` which already reverses.
- The QuickExport sheet's status row mirrors so the loader sits on
  the right edge.
- Emoji prefixes in tag chips stay before the text in both LTR and
  RTL because the chip composes them as a single `Text` line; if a
  future locale needs them after, the chip layout is updated.

## Appendix M — Decisions explicitly NOT taken in this part

We deliberately deferred or rejected the following:

1. **No tag colour picker with arbitrary RGB** — the curated palette
   (12 hand-picked colours) keeps the look consistent. Users wanting
   custom hex can edit the DB via backup/restore, but the UI doesn't
   expose it.
2. **No tag icons beyond emojis** — the design system commits to an
   emoji-prefix tag affordance. Custom drawables would explode the
   asset surface.
3. **No rule-priority numerical input** — priority is set purely by
   list reorder. Showing numbers risks mismatched user mental
   models when conditions overlap.
4. **No "AND/OR" toggle in rule conditions** — all conditions are
   AND. Users wanting OR create a second rule with the same actions.
   This was a deliberate simplification recorded in `DECISIONS.md`.
5. **No pre-canned rule templates inside the editor** — the
   "Browse examples" link routes to in-app docs instead. Templates
   inside the editor would duplicate the docs and bloat the
   editor's UI.
6. **No Drive-only mode (Drive without local backup)** — local is
   always primary. Users who don't want local files can disable
   auto-backup and use only manual + upload, but local is always
   the staging path.
7. **No third cloud (Dropbox / OneDrive)** — Drive is the only cloud
   target by design. Sideloaded apps + Drive's free quota cover the
   target user.
8. **No PDF password protection** — the JSON path covers that
   need. PDF passwords would require iText pro features we're not
   licensing.
9. **No vCard 4.0** — vCard 3.0 has the broadest contact-app
   compatibility. 4.0 is rejected by some Android contacts apps.
10. **No CSV without BOM toggle** — the spec mandates BOM so Excel
    on Windows reads UTF-8 correctly. Tools that don't want BOM can
    strip the first 3 bytes.

## Appendix N — Module-level dependencies introduced by this part

This part exercises the following Hilt modules:

- `RepositoryModule` — provides `TagRepository`, `AutoTagRuleRepository`,
  `BackupRepository`, `DriveRepository`, `ExportRepository`,
  `FilterRepository`.
- `WorkerModule` — `BackupWorker`, `DriveUploadWorker`,
  `ExportWorker`.
- `EvaluatorModule` — `RuleConditionEvaluator`.
- `KeyDerivationModule` — `Pbkdf2KeyDeriver`.
- `EmojiModule` — `EmojiCompatLoader`.
- `DriveOauthModule` (feature-flagged) — `DriveAuthClient`.
- `ExporterModule` — `ExcelExporter`, `CsvExporter`, `PdfExporter`,
  `JsonExporter`, `VCardExporter`.

All ViewModels are bound as `@HiltViewModel` and built with
`@Inject constructor(...)` per the project rules in `CLAUDE.md`.

## Appendix O — End-of-spec checklist for the rebuilding engineer

Before declaring this part of the spec "done" in your build, verify:

- [ ] Nine system tags seeded on first DB create.
- [ ] Tags screen renders, search filters, FAB opens editor, swipe-
      delete confirms only when usage > 0, system tags blocked from
      delete.
- [ ] Tag merge cascades across `call_tag_cross_ref` and any rule
      action referencing the source tag.
- [ ] AutoTagRules screen renders empty state, FAB creates rule,
      switch toggles `isActive`, drag reorders priority.
- [ ] AutoTagRules match-counts capped to latest 200, debounced.
- [ ] RuleEditor supports all 13 condition variants and 4 action
      variants.
- [ ] RuleEditor live preview debounced 400 ms; shows 0/many/error
      tints.
- [ ] RuleEditor Save disabled when invalid; discard dialog on
      back-with-dirty.
- [ ] BackupScreen shows correct status pill in all four states
      (never / never+passphrase / set / drive-on).
- [ ] BackupScreen passphrase dialog enforces ≥8 chars + match.
- [ ] BackupScreen restore double-confirms with "Type DELETE".
- [ ] Backup `.cvb` magic, salt, IV, AES-256-GCM, 120k PBKDF2 iters.
- [ ] BackupScreen Drive section gated by `BuildConfig
    .DRIVE_OAUTH_CONFIGURED`.
- [ ] Export wizard 5 steps with bottom-bar nav, format cards,
      preset chips, scope radios, 12 column toggles, destination
      picker.
- [ ] Export wizard cancellable mid-run, partial files cleaned up.
- [ ] Export Excel uses `SXSSFWorkbook(100)` for streaming.
- [ ] Export CSV writes BOM + RFC 4180 quotes.
- [ ] QuickExport sheet has 3 cards + 4-state status row + 3-second
      auto-dismiss in Success.
- [ ] QuickExport falls back to `cacheDir` if Downloads unwritable.
- [ ] All 207 new strings added to `values/strings.xml`.
- [ ] All ViewModels are `@HiltViewModel` and use `StateFlow`.
- [ ] All Compose composables that ship have at least one `@Preview`.
- [ ] All errors are user-friendly per `CLAUDE.md` rules.
- [ ] Timber-only logging; no `Log.d` / `println`.
- [ ] No mock data outside `@Preview`.
- [ ] No `TODO(` in user-reachable paths.
- [ ] All copy in tables matches `values/strings.xml`.
- [ ] All ASCII wireframes match the implemented layouts.

---

_End of Part 05._
