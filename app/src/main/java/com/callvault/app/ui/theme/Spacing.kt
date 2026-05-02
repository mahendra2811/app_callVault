package com.callvault.app.ui.theme

import androidx.compose.ui.unit.dp

/** Single source of truth for spacing across CallVault screens. */
object Spacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 24.dp
    val Xxxl = 32.dp

    /** Horizontal padding applied to every screen body. */
    val PageHorizontal = 16.dp

    /** Gap below NeoTopBar before the page header. */
    val PageTopHeader = 8.dp

    /** Vertical gap between scroll sections. */
    val SectionGap = 24.dp

    /** Inner padding inside dialogs / sheets. */
    val DialogContent = 20.dp

    /** Hard cap for dialog width on tablets and foldables. */
    val DialogMaxWidth = 360.dp

    val BottomNavHeight = 72.dp
    val TopBarHeight = 64.dp
}
