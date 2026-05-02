package com.callvault.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale for CallVault.
 *
 * Neumorphic surfaces lean on rounded corners (12 / 16 / 24 dp) so light/dark
 * shadow gradients wrap continuously without sharp seams.
 */
val CallVaultShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
