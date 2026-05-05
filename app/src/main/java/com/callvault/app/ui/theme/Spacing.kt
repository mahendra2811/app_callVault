package com.callvault.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale for CallVault — Phase II.
 *
 * Single source of truth. Phase-1 density tokens added (list rows, top bar,
 * tab bar, FAB, primary/secondary buttons). Legacy keys preserved for back-compat.
 */
object Spacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
    val Xxxl = 48.dp

    /** Horizontal padding applied to every screen body. */
    val PageHorizontal = 16.dp

    /** Gap below NeoTopBar before the page header. */
    val PageTopHeader = 8.dp

    /** Vertical gap between scroll sections. */
    val SectionGap = 24.dp

    // Phase-1 density (Phase II.1)
    val ListRowHeight = 64.dp
    val TopBarHeight = 48.dp
    val TabBarHeight = 64.dp
    val FabSize = 56.dp
    val SearchBarHeight = 44.dp
    val PrimaryButtonHeight = 52.dp
    val SecondaryButtonHeight = 44.dp

    /** Inner padding inside dialogs / sheets. */
    val DialogContent = 20.dp

    /** Hard cap for dialog width on tablets and foldables. */
    val DialogMaxWidth = 360.dp

    // Legacy back-compat aliases — do not remove.
    val BottomNavHeight = TabBarHeight
}
