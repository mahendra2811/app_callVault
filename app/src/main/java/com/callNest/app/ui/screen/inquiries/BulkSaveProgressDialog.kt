package com.callNest.app.ui.screen.inquiries

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.callNest.app.R
import com.callNest.app.domain.usecase.BulkSaveProgress
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoDialog
import com.callNest.app.ui.components.neo.NeoProgressBar
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.Spacing

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
    NeoDialog(
        onDismissRequest = onDismiss,
        body = {
            when (progress) {
                is BulkSaveProgress.Running -> {
                    Text(
                        text = stringResource(R.string.bulk_save_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = NeoColors.OnBase
                    )
                    Spacer(Modifier.height(Spacing.Md))
                    NeoProgressBar(
                        progress = if (progress.total == 0) 0f
                            else progress.current.toFloat() / progress.total,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.Sm))
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
                        Spacer(Modifier.height(Spacing.Xs))
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
                        style = MaterialTheme.typography.titleLarge,
                        color = NeoColors.OnBase
                    )
                    Spacer(Modifier.height(Spacing.Sm))
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
                        Spacer(Modifier.height(Spacing.Xs))
                        Text(
                            text = it,
                            color = NeoColors.AccentRose,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(Spacing.Md))
                    NeoButton(
                        text = stringResource(R.string.bulk_save_close),
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BulkSaveProgress.Idle -> Unit
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun BulkSaveProgressRunningPreview() {
    CallNestTheme {
        BulkSaveProgressDialog(
            progress = BulkSaveProgress.Running(current = 3, total = 8),
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun BulkSaveProgressDonePreview() {
    CallNestTheme {
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
