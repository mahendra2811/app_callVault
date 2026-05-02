package com.callvault.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Single, locked Material 3 light color scheme keyed off the neumorphic base.
 *
 * The app intentionally does **not** track system dark mode — neumorphism's
 * depth illusion only works on a single tinted base. See spec §3.23.
 */
private val CallVaultColorScheme = lightColorScheme(
    primary = NeoColors.AccentBlue,
    onPrimary = NeoColors.Light,
    primaryContainer = NeoColors.Raised,
    onPrimaryContainer = NeoColors.OnBase,

    secondary = NeoColors.AccentTeal,
    onSecondary = NeoColors.Light,
    secondaryContainer = NeoColors.Raised,
    onSecondaryContainer = NeoColors.OnBase,

    tertiary = NeoColors.AccentViolet,
    onTertiary = NeoColors.Light,

    background = NeoColors.Base,
    onBackground = NeoColors.OnBase,

    surface = NeoColors.Base,
    onSurface = NeoColors.OnBase,
    surfaceVariant = NeoColors.Inset,
    onSurfaceVariant = NeoColors.OnBaseMuted,

    outline = NeoColors.Dark,
    outlineVariant = NeoColors.OnBaseSubtle,

    error = NeoColors.AccentRose,
    onError = NeoColors.Light
)

/**
 * Root theme wrapper.
 *
 * Wrap the entire app (including previews) in [CallVaultTheme] so all
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
