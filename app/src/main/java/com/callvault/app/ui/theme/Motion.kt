package com.callvault.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Phase II — Motion tokens for CallVault.
 *
 * Maps to UI-spec §10. CallVault's standard duration (320ms) is intentionally
 * slower than Material's default to reinforce the calm philosophy. Easings and
 * spring configs are referenced from screen transitions, sheet open/close,
 * tab indicator slides, and press feedback.
 *
 * Phase II.5/II.6 will sweep existing call-sites onto these tokens.
 */
object MotionTokens {
    /** Quick — taps, ripples, color changes (~180ms). */
    const val DurationShort = 160

    /** Standard — page transitions, sheets (~320ms). */
    const val DurationStandard = 320

    /** Slow — hero entrance, splash logo (~480ms). */
    const val DurationLong = 500

    /** Default easing for all transitions. */
    val EaseStandard = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    /** Container transforms — emphasized exits/enters. */
    val EaseEmphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Snappy non-bouncy spring — calm by default. */
    val SpringSnappy = spring<Float>(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioNoBouncy
    )
}
