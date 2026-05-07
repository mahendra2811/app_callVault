package com.callNest.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale for callNest — Phase II.
 *
 * Aligned with UI-spec §8.3 corner-radius tokens.
 */
val callNestShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/** Pill — used for primary CTA, FAB, filter chips. */
val PillShape = RoundedCornerShape(percent = 50)
