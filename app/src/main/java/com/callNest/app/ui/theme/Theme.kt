package com.callNest.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Phase II — Sage/Earth color scheme wired into Material 3.
 *
 * App is light-only (no system dark-mode tracking). NeoColors retained
 * elsewhere for transition; new surfaces should resolve through MaterialTheme.
 */
private val callNestColorScheme = lightColorScheme(
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
    tertiaryContainer = SageColors.SurfaceAlt,
    onTertiaryContainer = SageColors.TextPrimary,

    background = SageColors.Canvas,
    onBackground = SageColors.TextPrimary,

    surface = SageColors.Canvas,
    onSurface = SageColors.TextPrimary,
    surfaceVariant = SageColors.SurfaceAlt,
    onSurfaceVariant = SageColors.TextSecondary,

    outline = SageColors.BorderDefault,
    outlineVariant = SageColors.BorderMuted,

    error = SageColors.StatusError,
    onError = SageColors.TextOnAccent,
    errorContainer = SageColors.SurfaceAlt,
    onErrorContainer = SageColors.StatusError,
)

/**
 * Root theme wrapper. Wrap the entire app (including previews) so all
 * composables inherit the locked colors, typography, and shapes.
 */
@Composable
fun CallNestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = callNestColorScheme,
        typography = callNestTypography,
        shapes = callNestShapes,
        content = content
    )
}
