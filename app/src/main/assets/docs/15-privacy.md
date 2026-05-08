# Privacy

Last updated: 2026-05-08.

Call Nest is built local-first: your call log, contacts, notes, tags, and lead scores live on this device. A small amount of data leaves the device only for the features that genuinely need a server, and only with your consent.

This summary mirrors the full policy at <https://callnest.pooniya.com/privacy>.

## What stays on this device

- The full call log (read from the OS).
- Your contact metadata (read from the OS contact provider).
- Notes, tags, follow-ups, bookmarks, filter presets, search history.
- Lead-score weights and computed scores.
- Your Anthropic API key (BYOK), stored encrypted via the Android Keystore.
- Backup archives — encrypted with your passphrase, written to your Documents folder.

## What leaves the device

- **Email + auth token** — When you sign in, your email is stored at our auth provider (Supabase) so we can identify your account. Sign-in is required to enable cloud backup and follow-up reminders.
- **FCM push token** — A device-specific token from Firebase Cloud Messaging is stored against your account so reminders can be delivered.
- **Crash reports & usage analytics (opt-in, off by default)** — If you enable "Help improve Call Nest" in Settings, anonymised crash stack traces (Sentry) and screen-level usage events (PostHog) are sent. Phone numbers, contact names, notes, and call content are never sent.
- **AI features (BYOK)** — When you use the weekly digest or any AI feature, the relevant text is sent directly from your device to Anthropic using your own API key. We never see this data.

## What we do not do

- We do not record or transcribe calls. Call Nest reads the call-log metadata Android already stores; it never captures audio.
- We do not upload your call log, contacts, or notes to our servers.
- We do not run ads or third-party trackers beyond the opt-in analytics named above.
- We do not read your SMS, photos, or any account data outside the permissions you explicitly grant.

## What you control

- **Help improve Call Nest** (Settings) — toggles all crash reporting and analytics. Off by default.
- **Clear search history** — wipes the local search log immediately.
- **Clear all notes** — removes every note and its history.
- **Reset all data** — wipes everything Call Nest has stored on this device. Your OS call log and OS contacts are not touched.
- **Delete account** — email <support@pooniya.com> and we clear your server-side record (email + push tokens) within 30 days.

## Grievance officer (DPDP §10)

Mahendra Puniya · <mahendra.puniya@primathon.in>. We acknowledge complaints within 7 days.
