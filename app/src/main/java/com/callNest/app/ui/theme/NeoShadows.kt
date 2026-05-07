package com.callNest.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mirror of [NeoColors] kept under the historical name `NeoShadows`
 * for the spec's literal `NeoShadows.Light` / `NeoShadows.Dark` lookups.
 *
 * Both objects expose the same four canonical tokens.
 */
object NeoShadows {
    val Light: Color = NeoColors.Light
    val Dark: Color = NeoColors.Dark
    val Base: Color = NeoColors.Base
    val BasePressed: Color = NeoColors.BasePressed
}
