package com.callvault.app.ui.theme

// Phase II adds SageColors. NeoColors retained for back-compat during gradual migration.

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Locked neumorphic color tokens for CallVault (legacy — being migrated).
 *
 * Retained intact for back-compat with ~50 existing call-sites.
 * New code should prefer [SageColors].
 */
object NeoColors {
    val Base = Color(0xFFE8E8EC)
    val BasePressed = Color(0xFFE0E0E5)
    val Light = Color(0xFFFFFFFF)
    val Dark = Color(0xFFA3B1C6)
    val Raised = Color(0xFFEDEDF2)
    val Inset = Color(0xFFDFDFE5)
    val OnBase = Color(0xFF2A3441)
    val OnBaseMuted = Color(0xFF5C6A7A)
    val OnBaseSubtle = Color(0xFF8492A3)

    val AccentBlue = Color(0xFF4F7CFF)
    val AccentTeal = Color(0xFF1FB5A8)
    val AccentAmber = Color(0xFFE0A82E)
    val AccentRose = Color(0xFFE5536B)
    val AccentViolet = Color(0xFF8266E5)
    val AccentGreen = Color(0xFF34A853)
}

/** Soft neutral border for cards / dialogs / toggle tracks. */
val NeoColors.BorderSoft: Color get() = NeoColors.Dark.copy(alpha = 0.18f)

/** Subtle accent border for primary cards. */
val NeoColors.BorderAccent: Color get() = NeoColors.AccentBlue.copy(alpha = 0.20f)

/** iOS-style green for switched-on toggle tracks. */
val ToggleOn: Color = Color(0xFF34C759)

/** Cool gray for switched-off toggle tracks. */
val ToggleOff: Color = Color(0xFFC7C7CC)

val IconCallsTint: Color get() = NeoColors.AccentBlue
val IconInquiriesTint: Color get() = NeoColors.AccentViolet
val IconStatsTint: Color get() = NeoColors.AccentAmber
val IconBackupTint: Color get() = NeoColors.AccentTeal
val IconTagsTint: Color get() = NeoColors.AccentRose
val IconHomeTint: Color get() = NeoColors.AccentGreen

// Phase II — Sage/Earth tab background tints
val TabBgHome      = Color(0xFFF5F1EA)
val TabBgCalls     = Color(0xFFEFEAE0)
val TabBgInquiries = Color(0xFFFAF6EE)
val TabBgMore      = Color(0xFFEFEAE0)
val TabBgStats     = Color(0xFFEFEAE0)

// Header gradient pairs (top → bottom) — Sage/Earth variants
val HeaderGradHomeStart      = Color(0xFFB6C5BB)   // Sage at ~30%
val HeaderGradHomeEnd        = Color(0xFFF5F1EA)
val HeaderGradCallsStart     = Color(0xFFC7D2C9)   // SageMuted at ~25%
val HeaderGradCallsEnd       = Color(0xFFEFEAE0)
val HeaderGradInquiriesStart = Color(0xFFF1D2C0)   // OrangeSoft at ~25%
val HeaderGradInquiriesEnd   = Color(0xFFF5F1EA)
val HeaderGradMoreStart      = Color(0xFFEDD9A5)   // Gold at ~20%
val HeaderGradMoreEnd        = Color(0xFFEFEAE0)
val HeaderGradStatsStart     = Color(0xFFEFC8B4)   // Orange at ~25%
val HeaderGradStatsEnd       = Color(0xFFEFEAE0)

// Splash + Welcome gradient (sage → orange — Phase II palette)
val SplashGradStart = Color(0xFF3D5A4A)
val SplashGradEnd   = Color(0xFFD97757)

/**
 * Phase II — Sage/Earth palette tokens.
 *
 * Maps to UI-spec §3 (Color System). All new components consume these.
 */
object SageColors {
    // Surfaces
    val Canvas        = Color(0xFFF5F1EA)
    val Surface       = Color(0xFFFFFCF7)
    val SurfaceAlt    = Color(0xFFEFEAE0)
    val SurfaceElev   = Color(0xFFFAF6EE)

    // Text
    val TextPrimary   = Color(0xFF2C2A26)
    val TextSecondary = Color(0xFF6B6863)
    val TextTertiary  = Color(0xFF9A9690)
    val TextInverse   = Color(0xFFFFFCF7)
    val TextOnAccent  = Color(0xFFFFFCF7)

    // Brand
    val Sage          = Color(0xFF3D5A4A)
    val SageMuted     = Color(0xFF6B8576)
    val SageDeep      = Color(0xFF2A3F33)
    val Orange        = Color(0xFFD97757)
    val OrangeSoft    = Color(0xFFE8A584)
    val Gold          = Color(0xFFC68B16)

    // Borders
    val BorderDefault = Color(0xFFE8E4DA)
    val BorderMuted   = Color(0xFFEFEAE0)
    val Divider       = Color(0xFFEFEAE0)

    // Status
    val StatusSuccess = Color(0xFF5A7A4A)
    val StatusWarning = Color(0xFFC68B16)
    val StatusError   = Color(0xFFB5524A)
    val StatusInfo    = Color(0xFF5A7280)
}

/** Brand gradient: Sage → SageMuted → Orange at 135°. Splash, premium, app-icon. */
fun gradientBrand(): Brush = Brush.linearGradient(
    colors = listOf(SageColors.Sage, SageColors.SageMuted, SageColors.Orange),
    start = Offset(0f, 0f),
    end = Offset(1000f, 1000f)
)

/** Soft brand gradient — primary CTA + FAB. */
fun gradientBrandSoft(): Brush = Brush.linearGradient(
    colors = listOf(
        SageColors.Sage.copy(alpha = 0.85f),
        SageColors.Orange.copy(alpha = 0.85f)
    )
)

/** Hero text fill — sparingly. */
fun gradientText(): Brush = Brush.linearGradient(
    colors = listOf(SageColors.Sage, SageColors.Orange)
)
