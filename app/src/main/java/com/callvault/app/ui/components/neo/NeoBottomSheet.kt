package com.callvault.app.ui.components.neo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Modal bottom sheet restyled to read on the neumorphic base.
 *
 * Built on Material 3's [ModalBottomSheet]; the inner content is wrapped in a
 * convex neumorphic card so the sheet doesn't read as pure white against the
 * tinted base (spec §3.23 "no pure white surfaces").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = NeoColors.Base,
        contentColor = NeoColors.OnBase,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            NeoSurface(
                modifier = Modifier.fillMaxWidth(),
                elevation = NeoElevation.ConvexSmall,
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) { content() }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoBottomSheetPreview() {
    CallVaultTheme {
        // Bottom sheets cannot meaningfully render in @Preview; the preview
        // shows the inner card so its styling can still be inspected.
        NeoSurface(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            elevation = NeoElevation.ConvexSmall,
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                Text("Bottom sheet content")
            }
        }
    }
}
