# Backing up to Google Drive

CallVault can optionally upload your encrypted backup file (`.cvb`) to your
own Google Drive — handy for surviving a lost or wiped device. The feature
is **off by default** and only activates after you sign in to Google.

## What gets uploaded

Exactly the same `.cvb` file that the local backup writes to Downloads.
Before anything leaves your phone, the file is encrypted with your
backup passphrase using AES-256-GCM (key derived via PBKDF2). Google
sees opaque ciphertext, not your call log.

## Where it lives

Files land in a folder named **CallVault Backups** at the root of your
Drive. CallVault creates this folder the first time it uploads. You can
move or rename it — CallVault recreates it as needed.

## Revoking access

Open <https://myaccount.google.com/permissions> on any device and revoke
**CallVault**. Existing backups stay in Drive; new uploads will require
signing in again.

## Spec note

This feature departs from the project's "nothing leaves the device" rule
in spec §13. Because the payload is encrypted client-side and Drive only
holds ciphertext, it preserves the original privacy intent while giving
you a recovery path. The toggle is opt-in and can be turned off at any
time without affecting local backups.
