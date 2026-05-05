package com.callvault.app.ui.screen.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.callvault.app.ui.theme.SageColors

/**
 * Lightweight scaffold that paints [containerColor] full-bleed (status-bar +
 * nav-bar areas included) and stacks an optional [topBar], primary [content]
 * and an optional [bottomBar]. System-bar insets are absorbed inside the
 * scaffold via [windowInsetsPadding] so the caller does not need to pass any
 * inset padding values.
 *
 * The background is painted on **both** the outer Box and the inner Column so
 * that no parent's surface color (e.g. the activity's `Surface`) can leak
 * through gaps caused by inset padding.
 */
@Composable
fun NeoScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    containerColor: Color = SageColors.Canvas,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(containerColor)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            if (topBar != null) topBar()
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues())
            }
            if (bottomBar != null) bottomBar()
        }
    }
}
