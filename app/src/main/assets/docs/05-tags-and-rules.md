# Tags and auto-tag rules

Tags are how you slice your call activity into something useful. Apply them by hand from the call detail screen, or let auto-tag rules do the work.

## Built-in tags

callNest ships with system tags like _New Lead_, _Hot Lead_, _Customer_, _Unanswered_, _Blocked_. They cannot be deleted but you can rename or recolour them.

## Auto-tag rules

Open **Settings → Auto-tag rules** to compose rules. Each rule has:

- **Conditions** — when to apply: number prefix, time of day, day of week, missed/answered, duration, or "incoming from unsaved number".
- **Actions** — what to do: add one or more tags, optionally bookmark, optionally adjust lead-score weight.

A live preview shows how many existing calls the rule would match before you save.

## Order matters

Rules run top-to-bottom. Drag the handle on the rule list to re-order. The first rule that matches wins — later ones still run unless you tick **Stop on match**.

## Editing existing matches

By default new rules apply only to incoming calls. Tap **Re-apply to history** if you want them to back-fill. The job runs in the background and shows progress in the notification shade.
