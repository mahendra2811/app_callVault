# Post-call popup

When a call ends, callNest can pop up a quick action sheet so you can capture the moment without opening the app.

## What it offers

- One-tap **Add note** with voice-to-text support.
- Tag selector for the most-used tags.
- Set a follow-up reminder (Tomorrow / Next week / Custom).
- **Save contact** if the number is unsaved.

## When it appears

By default, the popup shows after every call that lasted more than 10 seconds. You can restrict it to unsaved numbers only via **Settings → Real-time → Post-call popup → Unsaved only**.

The popup auto-dismisses after a configurable timeout (default 8 seconds). Anything you started typing is preserved, so dismissing accidentally is not a loss.

## Permissions

Same overlay permission as the floating bubble. callNest asks once and reuses it for both.

## Why it might not show

- **Do Not Disturb** silences the popup; that is intentional.
- The phone's screen-off path bypasses overlays on some OEMs. We surface a system notification as a fallback in that case — tap it within the timeout to capture.
- The popup is disabled while another popup-style app is in focus.
