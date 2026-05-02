# Permissions troubleshooting

CallVault asks for three runtime permissions. Each one is required for a different part of the app, and the OS hides the corresponding feature if you say no.

## Why we need each one
- **Read call log** — without this, the Calls tab is empty. We use the system call log as the source of truth.
- **Read contacts** — used to map phone numbers to names and to power the My Contacts tab. We never modify a contact unless you tap "Save".
- **Phone state** — lets us know when a call starts and ends, so the floating bubble and post-call popup can appear at the right moment.

## If the permission dialog never appears
Some launchers cache permission state. Long-press the CallVault icon, choose **App info**, then **Permissions**, and grant any that show as denied.

## "Don't ask again" recovery
If you tapped *Don't ask again*, Android won't show the prompt again. Open **Settings → Apps → CallVault → Permissions** and toggle the item on. CallVault checks the state every time you bring it to the foreground, so the gate clears as soon as you return.

## Background limits
Some OEMs aggressively kill background services. See the OEM battery setup article to whitelist CallVault.
