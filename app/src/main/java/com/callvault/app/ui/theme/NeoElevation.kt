package com.callvault.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Neumorphic elevation tokens.
 *
 * Each variant describes how the dual-shadow modifier should paint a surface.
 * - [Convex] — surface appears raised above the base (light TL highlight, dark BR shadow).
 * - [Concave] — surface appears pressed into the base (dark TL inset, light BR inset).
 * - [Flat] — no shadow (used while pressing or for non-elevated dividers).
 *
 * Levels (1..3) tune offset/blur/spread for small chips, cards, and floating elements.
 */
sealed class NeoElevation(
    val offset: Dp,
    val blur: Dp,
    val spread: Dp
) {

    sealed class Convex(offset: Dp, blur: Dp, spread: Dp) : NeoElevation(offset, blur, spread) {
        data object Level1 : Convex(2.dp, 4.dp, 0.dp)
        data object Level2 : Convex(4.dp, 10.dp, 0.dp)
        data object Level3 : Convex(8.dp, 20.dp, 0.dp)
    }

    sealed class Concave(offset: Dp, blur: Dp, spread: Dp) : NeoElevation(offset, blur, spread) {
        data object Level1 : Concave(2.dp, 4.dp, 0.dp)
        data object Level2 : Concave(4.dp, 10.dp, 0.dp)
        data object Level3 : Concave(6.dp, 14.dp, 0.dp)
    }

    data object Flat : NeoElevation(0.dp, 0.dp, 0.dp)

    companion object {
        val ConvexSmall: NeoElevation = Convex.Level1
        val ConvexMedium: NeoElevation = Convex.Level2
        val ConvexLarge: NeoElevation = Convex.Level3
        val ConcaveSmall: NeoElevation = Concave.Level1
        val ConcaveMedium: NeoElevation = Concave.Level2
        val ConcaveLarge: NeoElevation = Concave.Level3
    }
}
