package com.callNest.app.ui.components.neo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.theme.Spacing

/**
 * Modal bottom sheet restyled to read on the neumorphic base.
 *
 * Wraps Material 3's [ModalBottomSheet] with a convex neumorphic content card
 * and a centered drag handle. Inner content is capped at
 * [Spacing.DialogMaxWidth] on screens wider than 480dp; full-width on phones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val widthModifier =
        if (screenWidth > 480.dp) Modifier.widthIn(max = Spacing.DialogMaxWidth)
        else Modifier.fillMaxWidth()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = NeoColors.Base,
        contentColor = NeoColors.OnBase,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            NeoSurface(
                modifier = widthModifier,
                elevation = NeoElevation.ConvexSmall,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(NeoColors.OnBaseSubtle)
                        )
                    }
                    Box(modifier = Modifier.padding(16.dp)) { content() }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoBottomSheetPreview() {
    CallNestTheme {
        NeoSurface(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            elevation = NeoElevation.ConvexSmall,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(NeoColors.OnBaseSubtle)
                    )
                }
                Box(modifier = Modifier.padding(24.dp)) {
                    Text("Bottom sheet content")
                }
            }
        }
    }
}
