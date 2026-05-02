package com.callvault.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Locked neumorphic color tokens for CallVault.
 *
 * The base canvas is `#E8E8EC`; every surface in the app is tinted at least 4%
 * away from this base to preserve depth. Light source is virtual top-left at 45°.
 */
object NeoColors {
    val Base = Color(0xFFE8E8EC)
    val BasePressed = Color(0xFFE0E0E5)
    val Light = Color(0xFFFFFFFF)
    val Dark = Color(0xFFA3B1C6)

    /** Subtle elevated tint used for cards that need to read as raised. */
    val Raised = Color(0xFFEDEDF2)

    /** Subtle inset tint used for fields and progress tracks. */
    val Inset = Color(0xFFDFDFE5)

    /** Primary text on the neumorphic base. */
    val OnBase = Color(0xFF2A3441)

    /** Secondary text / icons. */
    val OnBaseMuted = Color(0xFF5C6A7A)

    /** Tertiary text — captions, helper labels. */
    val OnBaseSubtle = Color(0xFF8492A3)

    // Accent palette — used sparingly for badges, charts, lead-score chips.
    val AccentBlue = Color(0xFF4F7CFF)
    val AccentTeal = Color(0xFF1FB5A8)
    val AccentAmber = Color(0xFFE0A82E)
    val AccentRose = Color(0xFFE5536B)
    val AccentViolet = Color(0xFF8266E5)
    val AccentGreen = Color(0xFF34A853)
}
