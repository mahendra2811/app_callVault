# CallVault — Feature Backlog v2 (Net-New Sweep)

Generated 2026-05-06. **All ideas below are net-new** — not present in `FEATURE_BACKLOG.md` (v1), even paraphrased.

ICE = Impact × Confidence × Ease (each 1–10, 1000 max). Effort: **S** ≤ 1d · **M** 1–3d · **L** 4+ d.
Layer = `ui/` / `domain/` / `data/` per CLAUDE.md §"Layering — strict".

---

## Items considered and rejected as "already in v1"

| Considered | Why rejected |
|---|---|
| Tag picker in auto-tag rules | v1 Tier 1 (shipped) |
| WhatsApp `wa.me` deep-link from call detail | v1 Tier 1 (shipped) |
| Voice note capture (record only) | v1 Tier 1 — but **transcription** kept as net-new |
| FCM missed-call push | v1 Tier 2 |
| Daily summary push | v1 Tier 2 |
| PDF / chart export | v1 Tier 2 |
| Hindi i18n | v1 Tier 3 — but **Indic numeral toggle, Marathi/Tamil voice notes, festival surge** are net-new angles |
| Lead score visualiser | v1 Tier 3 |
| Auto-tag rule templates | v1 Tier 3 |
| Multi-device Supabase sync | v1 Tier 3 |
| Custom ringtone per tag | v1 Tier 3 |
| Per-contact follow-up cadence | v1 Tier 3 |
| Bulk tag from selection / merge inquiry to contact / smart filter presets / OEM battery wizard | v1 |

Locked-spec conflicts flagged inline as **[FLAG]** — moved on without proposing.

---

## Tier A — High-leverage net-new (ICE > 400)

| # | Feature | One-line | Why for Indian SMB / sideloaded / offline-first | Layer | Effort | I·C·E | ICE |
|---|---|---|---|---|---|---|---|
| A1 | **Deal-value field per call/contact** | Optional ₹ amount on a call or contact, summed in stats and lead-score input. | Shopkeeper instantly sees pipeline ₹, not just call count. Pure local Room column — no cloud. | `domain/model`, `data/local/entity`, `ui/screen/calldetail` | S | 9·9·8 | **648** |
| A2 | **Auto-aging buckets for inquiries** | Inquiries grouped as Fresh (<24h) / Warm (1–7d) / Cooling (8–30d) / Cold (30d+). | A 3-month-old inquiry is a different beast from yesterday's; visual aging beats manual scrubbing. Computed on read. | `domain/usecase`, `ui/screen/inquiries` | S | 9·9·8 | **648** |
| A3 | **Win / Loss / Pending outcome marking** | One-tap on a contact: Won (₹ value) / Lost (reason chip) / Pending. Powers conversion %. | Funnel visibility is the #1 thing a 50-call/day SMB lacks; chip set keeps it field-friendly. | `domain/model`, `data/local`, `ui/screen/calldetail` | M | 10·8·7 | **560** |
| A4 | **Post-call popup auto-suggest tag + outcome** | The existing post-call popup proposes the most-likely tag + outcome based on history with this number. | Two taps replaced by one confirm; matters when calls bunch between customers. Heuristic: most-frequent tag in last 5 calls with this number. | `domain/usecase`, `ui/` (popup already exists) | S | 9·8·8 | **576** |
| A5 | **Quick-tag from notification action** | Notification on missed call has 3 inline-action chips (top tags) — tap = tag applied, no app open. | Owner answering customers in shop can't unlock phone; 1-tap tagging is gold. Uses `Notification.Action`. | `data/notification`, `ui/notification` | M | 9·8·7 | **504** |
| A6 | **AI contact summary (on-device)** | "3 calls about wholesale, last asked for samples on 12 Apr." Generated from notes + tags + call meta. | Replaces re-reading 8 notes before a callback. Template-based summarizer (no LLM needed v1) keeps it offline-first. | `domain/usecase`, `ui/screen/calldetail` | M | 9·8·6 | **432** |
| A7 | **GST number capture per contact** | `gstin` field on contact, validated 15-char format, surfaced in export. | Every B2B SMB needs GSTIN for invoicing; today they keep it in a paper diary. India-specific, zero-cost. | `domain/model`, `data/local`, `ui/screen/contactdetail` | S | 9·9·8 | **648** |
| A8 | **UPI payment link generator from call detail** | "Request ₹X" → builds `upi://pay?pa=...&am=...&tn=...` deep-link, copyable + shareable to WhatsApp. | Most owners ask for payment over WhatsApp anyway; one button shortens the loop. No PSP integration needed. | `util/`, `ui/screen/calldetail` | S | 9·9·8 | **648** |
| A9 | **Stale-lead nudge banner** | Daily worker surfaces "5 inquiries you haven't followed up in 14+ days". | Re-engagement is where pipeline ₹ leaks; nudge replaces a missing CRM. Reuses `WorkManager`. | `data/work`, `ui/screen/home` | S | 9·8·8 | **576** |
| A10 | **WhatsApp Business templates per tag** | Per-tag "send template" — pre-fills `wa.me` text for tags like "Pricing asked" / "Payment reminder". | One owner sends the same 4 messages all day; tag-bound templates remove typing. Strings stored locally. | `domain/model` (Tag.template), `ui/screen/calldetail` | S | 9·9·7 | **567** |
| A11 | **Per-SIM ledger (multi-line split)** | Toggle in stats / calls list to view by SIM-1 vs SIM-2 (already captured in `simSlot`). | Many SMBs run business on SIM-1 + personal on SIM-2; today both pollute the same ledger. Field already in DB. | `ui/screen/calls`, `ui/screen/stats` | S | 9·8·8 | **576** |
| A12 | **Incremental encrypted backup** | Backup only rows changed since last successful backup, chained via Tink AEAD. | 10 000-call users today re-export the full set nightly; incremental is the only sustainable path. Builds on existing Tink. | `data/backup` | M | 8·8·7 | **448** |
| A13 | **Restore-smoke-test verifier** | After backup, silently decrypt + count rows + verify hash; surface "Last backup verified" badge. | "I have backups but they don't restore" is a real SMB horror story; trust badge solves it. | `data/backup` | S | 8·8·8 | **512** |
| A14 | **Bulk WhatsApp from inquiry list** | Multi-select inquiries → "Send same WhatsApp message to all" via sequential `wa.me` opens with a confirm-each loop. | Diwali greetings, payment reminders, stock-back announcements — all 1-tap broadcasts today require copy-paste. | `ui/screen/inquiries` | S | 9·8·7 | **504** |
| A15 | **Day-1 import wizard for 10k historical calls** | Onboarding step: "Found 10 247 historical calls. Auto-tag rules will run on these — proceed?" with ETA + cancel. | Today the first sync silently chews CPU for minutes on Realme/Xiaomi devices; visible progress + opt-in is trust. | `ui/screen/onboarding`, `data/work` | M | 8·9·7 | **504** |

---

## Tier B — Worth scoping (ICE 200–400)

| # | Feature | One-line | Why for Indian SMB | Layer | Effort | I·C·E | ICE |
|---|---|---|---|---|---|---|---|
| B1 | **Lost-reason chip set** (extension of A3) | Predefined chips: "Price too high" / "Went with competitor" / "No response" / custom. | Reasons aggregated → top-3 reasons widget on Stats; pricing feedback for owner. | `domain/model`, `ui/screen/stats` | S | 7·8·7 | 392 |
| B2 | **Voice-note transcription (Hindi / English / Marathi / Tamil)** | After voice-note saved, run on-device speech-to-text via Android `SpeechRecognizer` offline package. | Owners record in regional languages; searchable transcripts unlock FTS. **[FLAG]** offline package availability per OEM is uneven — verify before commit. | `data/voice`, `domain/usecase` | M | 8·6·7 | 336 |
| B3 | **Call-intent classification** | Classify each call (price / delivery / complaint / follow-up) from note keywords + tag history. | Stats by intent → "60% complaints this week" is a leading indicator. Rules-based v1, no ML. | `domain/usecase`, `ui/screen/stats` | M | 7·7·7 | 343 |
| B4 | **Indic numeral toggle (en-IN ↔ hi-IN)** | Setting flips digits (1234 → १२३४) across counts, ₹ amounts, dates. | Some owners read Devanagari faster; trivial via `NumberFormat` + locale override. | `util/`, `ui/` | S | 6·8·8 | 384 |
| B5 | **Festival-day surge insight** | Stats card: "Diwali spike: 3.2× normal call volume". Built-in Indian calendar of major festivals. | Helps owner staff up next year; differentiator over global apps. | `data/local/asset`, `ui/screen/stats` | M | 7·7·6 | 294 |
| B6 | **Local TrueCaller-style cache** | When a number rings, look up name from previous CallVault encounters across all users on this device (already partially via inquiries). Add optional crowdsourced opt-in pool keyed by hashed numbers. **[FLAG]** crowd pool conflicts with CLAUDE.md privacy posture — keep as device-local only. | Spam awareness without depending on external cache. | `data/repository`, `domain/usecase` | M | 7·7·6 | 294 |
| B7 | **Spam learning from user blocks** | When user blocks a number, similar numbers (same prefix + call pattern) get a "likely spam" badge. | Marketing/loan-spam plagues SMB lines; on-device pattern match avoids 3rd-party spam SDK. | `domain/usecase`, `data/repository` | M | 7·7·6 | 294 |
| B8 | **"Last seen" recency badge on contact** | Chip on contact row: "Spoke 2 days ago" / "No call in 47 days". | Faster than scanning history; reuses existing call meta. | `ui/screen/mycontacts`, `domain/usecase` | S | 7·9·8 | 504 ⇒ promote to A? — keep here; impact on conversion unproven |
| B9 | **Per-business profile (multi-business mode)** | Owner runs 2 businesses on same phone; toggle business → filters calls/tags/exports to that business. | Common: kirana + tiffin service on same SIM; today they pollute one ledger. **[FLAG]** big data-model change — re-key calls by `businessId`; verify against §4. | `domain/model`, `data/local` | L | 8·6·4 | 192 |
| B10 | **PDF quote generator from call detail** | "Generate quote PDF" with line items + GSTIN + UPI link, share via WhatsApp. | Owners today write quotes on paper or in plain WhatsApp text. Reuses iText 8. | `data/export`, `ui/screen/calldetail` | M | 7·7·6 | 294 |
| B11 | **Bluetooth keyboard shortcuts in calls list** | `j` / `k` to navigate, `t` to tag, `b` to bookmark, `?` for help overlay. | Power users on tablet + BT keyboard. Tiny code, big delight. | `ui/screen/calls` | S | 5·8·9 | 360 |
| B12 | **Share contact's full call history as image** | Render `CallHistoryView` to bitmap, share via Android share sheet (image, not text). | Owner texts colleague: "see this customer's history". Image survives WhatsApp text limits. | `util/`, `ui/screen/contactdetail` | S | 6·8·8 | 384 |
| B13 | **Catalog-PDF auto-reply hint on missed call** | Post-call popup: "Send your catalog?" with 1-tap WhatsApp send (catalog PDF stored in app settings). | Repeat use case for retailers; popup already exists. | `domain/usecase`, `ui/notification` | S | 7·8·6 | 336 |
| B14 | **Working-hours mute** | Auto-suppress popup + bubble outside set hours (e.g., 10am–9pm). | Owner doesn't want CallVault overlay during family dinner. DataStore flag. | `data/prefs`, `ui/service` | S | 6·9·8 | 432 |
| B15 | **Stats compare period-over-period** | Stats cards show ↑/↓ vs previous week / month with %. | "Are inquiries growing?" is the question owners actually ask. | `ui/screen/stats`, `domain/usecase` | S | 7·8·7 | 392 |

---

## Tier C — Speculative / nice-to-have (ICE < 200)

| # | Feature | One-line | Why | Layer | Effort | ICE |
|---|---|---|---|---|---|---|
| C1 | **Voice command "tag last call as X"** | Hands-free post-call tagging via SpeechRecognizer. | Field worker, hands full. | `data/voice`, `ui/` | M | 5·5·5 = 125 |
| C2 | **Map view of inquiries** (city-level only, no GPS) | Aggregate inquiries by inferred city from STD code prefix. | Soft signal; STD-code mapping is brittle. | `data/asset`, `ui/screen/stats` | M | 5·6·5 = 150 |
| C3 | **Festival greeting auto-broadcast** | On Diwali/Eid, prompt to send greeting to all "Customer" tagged contacts. | Loyalty boost; can feel spammy. | `domain/usecase`, `ui/` | M | 5·6·5 = 150 |
| C4 | **Wear OS companion: incoming-call quick-tag from watch** | Wear notification mirrors quick-tag chips. | Tiny audience among Indian SMB. | new module — **[FLAG]** CLAUDE.md "Don't add new Gradle modules" → blocked unless rule revisited. | L | 4·5·3 = 60 |
| C5 | **Customer NPS micro-survey** auto-sent 7 days post-call | Single-emoji reply via WhatsApp. | Most won't reply; cool when they do. | `domain/usecase` | M | 5·5·5 = 125 |
| C6 | **CSV import of paper-diary contacts** | Wizard-imports a CSV exported from Excel/Google Sheets into MyContacts. | Owners with legacy registers; one-time use. | `data/import`, `ui/screen/import` | M | 6·7·4 = 168 |
| C7 | **Encrypted local search index integrity check** | Periodic FTS-vs-Room divergence check + repair. | Resilience win; rarely visible. | `data/work` | S | 4·8·6 = 192 |
| C8 | **Tasker / automation broadcasts** | Send Intents on tag-applied / outcome-marked for power users on Tasker. | Niche delight. | `data/system` | S | 4·7·7 = 196 |

---

## Ship-next pick

**Ship A1 (Deal-value field per call/contact) next.**

It is a single Room column + form field + sum aggregator — under one focused day for a solo dev — yet it unlocks A3 (Won/Lost), A9 (Stale-lead nudge), A8 (UPI payment link sized to a real ₹), and B15 (period-over-period in ₹ not just calls). Everything else in Tier A that touches funnel value (A3, A8, A9, A10, A14) becomes meaningfully better the moment ₹ exists in the data model, so A1 is the load-bearing prerequisite that compounds. It is also pure-local, offline-first, and conflicts with nothing in the locked spec — lowest risk, highest downstream multiplier.
