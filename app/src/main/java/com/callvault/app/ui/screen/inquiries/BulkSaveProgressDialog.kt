package com.callvault.app.ui.screen.inquiries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.callvault.app.R
import com.callvault.app.domain.usecase.BulkSaveProgress
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoProgressBar
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Modal dialog that mirrors a [BulkSaveProgress] stream.
 *
 * - `Running` → determinate progress bar with "X of Y".
 * - `Done`    → summary + close button.
 * - `Idle`    → not rendered (the parent decides when to show this).
 */
@Composable
fun BulkSaveProgressDialog(
    progress: BulkSaveProgress,
    onDismiss: () -> Unit
) {
    if (progress is BulkSaveProgress.Idle) return
    Dialog(onDismissRequest = onDismiss) {
        NeoSurface(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            elevation = NeoElevation.ConvexLarge,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                when (progress) {
                    is BulkSaveProgress.Running -> {
                        Text(
                            text = stringResource(R.string.bulk_save_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = NeoColors.OnBase
                        )
                        Spacer(Modifier.height(4.dp))
                        NeoProgressBar(
                            progress = if (progress.total == 0) 0f
                                else progress.current.toFloat() / progress.total,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(
                                R.string.bulk_save_progress,
                                progress.current,
                                progress.total
                            ),
                            color = NeoColors.OnBaseMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        progress.lastError?.let {
                            Text(
                                text = it,
                                color = NeoColors.AccentRose,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    is BulkSaveProgress.Done -> {
                        Text(
                            text = stringResource(R.string.bulk_save_done_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = NeoColors.OnBase
                        )
                        Text(
                            text = stringResource(
                                R.string.bulk_save_done_body,
                                progress.savedCount,
                                progress.skippedCount
                            ),
                            color = NeoColors.OnBaseMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        progress.firstError?.let {
                            Text(
                                text = it,
                                color = NeoColors.AccentRose,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        NeoButton(
                            text = stringResource(R.string.bulk_save_close),
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    BulkSaveProgress.Idle -> Unit
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun BulkSaveProgressRunningPreview() {
    CallVaultTheme {
        BulkSaveProgressDialog(
            progress = BulkSaveProgress.Running(current = 3, total = 8),
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun BulkSaveProgressDonePreview() {
    CallVaultTheme {
        BulkSaveProgressDialog(
            progress = BulkSaveProgress.Done(
                savedCount = 6,
                skippedCount = 2,
                totalCount = 8,
                firstError = null
            ),
            onDismiss = {}
        )
    }
}
