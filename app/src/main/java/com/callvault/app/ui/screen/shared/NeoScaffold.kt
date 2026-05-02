package com.callvault.app.ui.screen.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.callvault.app.ui.theme.NeoColors

/**
 * Lightweight scaffold that paints the neumorphic base and stacks an optional
 * [topBar], primary [content] and an optional [bottomBar] (used by the
 * Calls bulk-edit action bar). Honours system bar insets so content is never
 * clipped on edge-to-edge displays.
 */
@Composable
fun NeoScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = WindowInsets.systemBars.asPaddingValues(),
    content: @Composable (PaddingValues) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeoColors.Base)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            if (topBar != null) topBar()
            Box(modifier = Modifier.weight(1f)) {
                content(PaddingValues())
            }
            if (bottomBar != null) bottomBar()
        }
    }
}
