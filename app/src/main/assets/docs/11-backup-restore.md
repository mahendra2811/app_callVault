# Backup and restore

callNest keeps an encrypted on-device backup of every database table so you can recover from device wipes, factory resets, or a bad app update.

## Setting up

Open **Settings → Backup & restore** and tap **Set passphrase**. Pick something you can remember — without it, the encrypted archive is unreadable, even by us. The passphrase is stored only in your head.

## Auto-backups

Daily at 2 AM (local time) when the device is charging and idle. The most recent N copies are kept on disk; older ones are deleted automatically. Adjust N with the retention slider (1 to 30, default 7).

## Manual backup

Tap **Backup now** any time. The job runs in the foreground for 5–30 seconds depending on your data volume.

## Restore

Tap **Restore from file**, pick a `.cvbk` archive, and enter the passphrase. callNest confirms with "Replace all data?" before overwriting. There is no undo — the existing database is replaced, not merged.

## Where files live

`Documents/callNest/backups/`. Move them to your computer, cloud drive, or another device for off-site safety.

## Trouble

- **Wrong passphrase** — there is no recovery path. Try every variant you remember.
- **File corrupted** — restore aborts; nothing is overwritten.
