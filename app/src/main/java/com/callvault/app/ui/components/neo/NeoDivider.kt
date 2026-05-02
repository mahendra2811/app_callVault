package com.callvault.app.ui.components.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors

/**
 * Thin two-tone divider.
 *
 * Renders a 1dp dark line with a 1dp light line beneath it so the seam reads
 * as a soft groove rather than a hard rule.
 */
@Composable
fun NeoDivider(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NeoColors.Dark.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NeoColors.Light.copy(alpha = 0.6f))
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoDividerPreview() {
    CallVaultTheme {
        NeoDivider(modifier = Modifier.padding(24.dp))
    }
}
