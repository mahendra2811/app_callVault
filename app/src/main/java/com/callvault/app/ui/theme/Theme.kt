package com.callvault.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Phase II — Sage/Earth color scheme wired into Material 3.
 *
 * App is light-only (no system dark-mode tracking). NeoColors retained
 * elsewhere for transition; new surfaces should resolve through MaterialTheme.
 */
private val CallVaultColorScheme = lightColorScheme(
    primary = SageColors.Sage,
    onPrimary = SageColors.TextOnAccent,
    primaryContainer = SageColors.SurfaceElev,
    onPrimaryContainer = SageColors.TextPrimary,

    secondary = SageColors.Orange,
    onSecondary = SageColors.TextOnAccent,
    secondaryContainer = SageColors.SurfaceAlt,
    onSecondaryContainer = SageColors.TextPrimary,

    tertiary = SageColors.Gold,
    onTertiary = SageColors.TextOnAccent,

    background = SageColors.Canvas,
    onBackground = SageColors.TextPrimary,

    surface = SageColors.Canvas,
    onSurface = SageColors.TextPrimary,
    surfaceVariant = SageColors.SurfaceAlt,
    onSurfaceVariant = SageColors.TextSecondary,

    outline = SageColors.BorderDefault,
    outlineVariant = SageColors.BorderMuted,

    error = SageColors.StatusError,
    onError = SageColors.TextOnAccent
)

/**
 * Root theme wrapper. Wrap the entire app (including previews) so all
 * composables inherit the locked colors, typography, and shapes.
 */
@Composable
fun CallVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CallVaultColorScheme,
        typography = CallVaultTypography,
        shapes = CallVaultShapes,
        content = content
    )
}
