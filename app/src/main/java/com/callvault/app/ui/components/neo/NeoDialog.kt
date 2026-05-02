package com.callvault.app.ui.components.neo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.callvault.app.ui.theme.BorderSoft
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation
import com.callvault.app.ui.theme.Spacing

/**
 * Neumorphic dialog scaffold.
 *
 * Wraps [Dialog] with a [NeoSurface] capped at [Spacing.DialogMaxWidth],
 * outlined by a 1.dp [NeoColors.BorderSoft] border, and padded by
 * [Spacing.DialogContent]. Slot-based: provide [header], [body], and [footer].
 *
 * @param onDismissRequest fires on backdrop tap or back button.
 * @param header optional title row, rendered inside the surface column.
 * @param body required content body — typically labels, fields, lists.
 * @param footer optional trailing action row (e.g. cancel/confirm buttons).
 */
@Composable
fun NeoDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
    footer: (@Composable RowScope.() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismissRequest) {
        val shape = RoundedCornerShape(20.dp)
        NeoSurface(
            modifier = modifier
                .widthIn(max = Spacing.DialogMaxWidth)
                .fillMaxWidth()
                .border(1.dp, NeoColors.BorderSoft, shape),
            elevation = NeoElevation.ConvexMedium,
            shape = shape
        ) {
            Column(modifier = Modifier.padding(Spacing.DialogContent)) {
                if (header != null) {
                    header()
                }
                body()
                if (footer != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.Md),
                        horizontalArrangement = Arrangement.End,
                        content = footer
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "confirm")
@Composable
private fun NeoDialogConfirmPreview() {
    CallVaultTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            NeoDialog(
                onDismissRequest = {},
                header = {
                    Text(
                        "Delete tag?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                body = {
                    Text(
                        "This removes the tag from 12 calls. You can re-add it later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeoColors.OnBaseMuted,
                        modifier = Modifier.padding(top = Spacing.Sm)
                    )
                },
                footer = {
                    Text("Cancel", color = NeoColors.OnBaseMuted, modifier = Modifier.padding(end = Spacing.Lg))
                    Text("Delete", color = NeoColors.AccentRose, fontWeight = FontWeight.Bold)
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "info")
@Composable
private fun NeoDialogInfoPreview() {
    CallVaultTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            NeoDialog(
                onDismissRequest = {},
                header = {
                    Text(
                        "About lead score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                body = {
                    Text(
                        "We weigh recency, frequency, duration, and your tags to compute 0–100.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeoColors.OnBaseMuted,
                        modifier = Modifier.padding(top = Spacing.Sm)
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "progress")
@Composable
private fun NeoDialogProgressPreview() {
    CallVaultTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            NeoDialog(
                onDismissRequest = {},
                header = {
                    Text(
                        "Saving inquiries…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                body = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.Lg),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        NeoLoader()
                    }
                }
            )
        }
    }
}
