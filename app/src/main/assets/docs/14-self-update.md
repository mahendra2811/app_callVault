# Self-update channel

CallVault ships outside the Play Store on some installs. When it does, the in-app updater handles new releases.

## How it works
A daily background job checks a signed manifest at the URL configured for your update channel. If a newer build is available, you see a banner on the Calls screen. Tap **View** to see release notes; tap **Update** to start the download. Installation uses Android's standard package-installer prompt.

## Channels
- **Stable** — the same builds we publish to wider audiences. Recommended for everyone.
- **Beta** — slightly newer builds with the latest fixes. Expect occasional rough edges.

Switch via **Settings → App updates → Channel**.

## Skipping a release
Choose **Skip this version** in the update screen. CallVault will not bother you about that exact build again — but the next one still notifies as normal.

## Manual check
Open **Settings → App updates** and tap **Check now**. The last-checked timestamp confirms whether the daily job is running.

## Privacy
The check is a single HTTPS GET for the manifest. No identifiers, no telemetry. The download itself is the APK over HTTPS.

## Verification
Every release is signed; the installer rejects tampered files. CallVault will refuse to install an APK whose signature does not match the version already installed.
