# Exporting your data

CallVault can export any slice of your call history to CSV or JSON — useful for spreadsheets, CRM imports, or one-off audits.

## The wizard
**Calls overflow → Export** opens a four-step flow:
1. **Format** — CSV, JSON.
2. **Date range** — today, last 7 days, last 30 days, this month, last month, last 90 days, or a custom picker.
3. **Scope** — all calls, only saved, only unsaved, only inquiries, or only a tag.
4. **Columns** — pick exactly which fields to include. Notes can be flattened or split across rows.

A preview at the bottom shows the first three rows so you can sanity-check before kicking the job off.

## Where files land
Exports are saved to `Documents/CallVault/exports/` by default. Tap **Open** or **Share** from the success card to use the file straight away.

## Notes inside CSV
Notes can contain commas and newlines. We escape them with RFC 4180 quoting, so any spreadsheet tool will parse the file correctly.

## Encryption
CSV exports are plain text. JSON exports are also plain text by default. If you need encrypted output, use the Backup & Restore feature instead — those archives use AES-GCM with your passphrase.
