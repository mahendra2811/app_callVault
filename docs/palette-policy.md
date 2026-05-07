# Palette policy

## Source of truth: Sage / Neo tokens

`SageColors` (in `ui/theme/Color.kt`) is the canonical brand palette. `NeoColors` is a derived
accent set used by the bespoke `Neo*` components (NeoSurface, NeoButton, NeoCard, NeoElevation).

The Material 3 `ColorScheme` is **derived from Sage tokens** in `ui/theme/Theme.kt`. This means
every M3 widget — `Button`, `TopAppBar`, `Switch`, `Snackbar`, `AlertDialog`, `ModalBottomSheet`,
`Card` defaults — already renders in Sage.

## When to use which

| Surface                                                                                              | Use                                                                  |
| ---------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| Hand-built brand cards (Hero, Lead pipeline cards, Calls list rows)                                  | `NeoSurface` + `NeoColors` / `SageColors` directly                   |
| System widgets (Buttons, Switches, AlertDialog, BottomSheet, TopAppBar, Cards in non-brand surfaces) | `MaterialTheme.colorScheme.*`                                        |
| Containers (`primaryContainer`, `tertiaryContainer`, `errorContainer`)                               | `MaterialTheme.colorScheme.*Container` — they map back to Sage tints |
| Direct hex / `Color(0xFF…)`                                                                          | Forbidden in new code. Add a token first.                            |

## What was fixed in this audit pass

1. `tertiaryContainer` / `onTertiaryContainer` were unset — now map to `SurfaceAlt` / `TextPrimary`.
   The digest AI summary card, the demo-data banner, and the Why-Score sheet's tertiary surface
   no longer fall back to default M3 pinkish tones.
2. `errorContainer` / `onErrorContainer` were unset — now map to a sage-tinted error container.
   The DeleteAccountDialog's destructive button uses these.
3. `MaterialTheme.colorScheme.surfaceVariant` is `SageColors.SurfaceAlt` — the Pipeline column
   background and template list separators are now consistent.

## Rule for new screens

- Reach for `MaterialTheme.colorScheme.*` first.
- If the surface is one of the bespoke brand cards, use `NeoSurface` (which itself draws from the
  same theme).
- Avoid mixing both on the same widget (e.g., `NeoSurface` background + an M3 `Card` inside).

## Dark mode

The app is light-only. `darkColorScheme` is intentionally not provided — system dark-mode setting
is ignored by `callNestTheme`. If a future iteration wants dark mode, fork `callNestColorScheme`
into a dark variant and select via `isSystemInDarkTheme()`.
