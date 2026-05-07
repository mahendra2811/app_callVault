---
description: Show how to inspect callNest's Room database on a connected device.
---

Print a quick guide for the user — don't run anything destructive.

Tell the user:

> **Inspecting callNest's database**
>
> Best path: **Android Studio → View → Tool Windows → App Inspection → Database Inspector**. Pick `com.callNest.app` → `callNest.db`. Browse tables, run ad-hoc SQL.
>
> Schema highlights:
>
> - `calls` — every call from CallLog, indexed on `normalizedNumber`, `date`, `type`, `isBookmarked`, `followUpDate`
> - `tags` + `call_tag_cross_ref` — tag M:N
> - `contact_meta` — per-number aggregates + auto-save flags
> - `notes` + `note_history` — last 5 versions per note
> - `auto_tag_rules` — JSON-stored conditions/actions
> - `rule_score_boosts` — lead-score deltas per (call, rule)
> - `call_fts` + `note_fts` — virtual FTS4 mirrors
>
> Useful queries:
>
> ```sql
> -- Top-10 most-called numbers in the last 30 days
> SELECT normalizedNumber, COUNT(*) c FROM calls
> WHERE date > strftime('%s','now','-30 days')*1000
> GROUP BY normalizedNumber ORDER BY c DESC LIMIT 10;
>
> -- Auto-saved contacts
> SELECT displayName, totalCalls, computedLeadScore FROM contact_meta
> WHERE isAutoSaved = 1 ORDER BY lastCallDate DESC;
>
> -- Search notes by content (FTS)
> SELECT n.content FROM notes n JOIN note_fts f ON n.id = f.docid
> WHERE note_fts MATCH 'price*';
> ```
>
> CLI alternative (rooted device or debug build):
>
> ```bash
> adb shell run-as com.callNest.app cat databases/callNest.db > /tmp/callNest.db
> sqlite3 /tmp/callNest.db
> ```

Do not run any of these commands automatically. The user opens Android Studio.
