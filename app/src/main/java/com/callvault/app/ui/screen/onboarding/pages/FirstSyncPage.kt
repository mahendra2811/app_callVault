package com.callvault.app.ui.screen.onboarding.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoEmptyState
import com.callvault.app.ui.components.neo.NeoProgressBar
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors

/**
 * Onboarding page 5 — runs the very first call-log import in the background
 * and renders live progress.
 *
 * Behaviour:
 * - On first composition, [onStart] is invoked exactly once to kick off the sync.
 * - While [total] is `0`, an indeterminate-style full progress bar is shown.
 * - Once [total] > 0, progress is rendered as `current/total`.
 * - When [done] flips `true`, [onCompleted] auto-fires so the host can pop
 *   onboarding off the back stack.
 * - On [error], a [NeoEmptyState] with Skip / Retry actions is rendered.
 *
 * @param progress current count of imported rows (mirrors [com.callvault.app.domain.usecase.SyncProgress.Progress.current]).
 * @param total declared total rows; `0` means indeterminate.
 * @param done set true after [com.callvault.app.domain.usecase.SyncProgress.Done] arrives.
 * @param error optional user-facing error string.
 */
@Composable
fun FirstSyncPage(
    progress: Int,
    total: Int,
    done: Boolean,
    error: String?,
    onStart: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) { onStart() }
    LaunchedEffect(done) { if (done) onCompleted() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            error != null -> {
                NeoEmptyState(
                    icon = Icons.Filled.SyncProblem,
                    title = stringResource(R.string.onboarding_first_sync_error_title),
                    message = error,
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NeoButton(
                                text = stringResource(R.string.onboarding_skip),
                                onClick = onSkip,
                                variant = NeoButtonVariant.Tertiary
                            )
                            NeoButton(
                                text = stringResource(R.string.onboarding_retry),
                                onClick = onRetry,
                                variant = NeoButtonVariant.Primary
                            )
                        }
                    }
                )
            }
            done -> {
                Text(
                    text = stringResource(R.string.onboarding_first_sync_done),
                    color = NeoColors.OnBase,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                Text(
                    text = stringResource(R.string.onboarding_first_sync_title),
                    color = NeoColors.OnBase,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                val fraction = if (total > 0) progress.toFloat() / total.toFloat() else 1f
                NeoProgressBar(
                    progress = if (total > 0) fraction else 0.85f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (total > 0) {
                        stringResource(R.string.onboarding_first_sync_progress, progress, total)
                    } else {
                        stringResource(R.string.onboarding_first_sync_indeterminate)
                    },
                    color = NeoColors.OnBaseMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun FirstSyncPageProgressPreview() {
    CallVaultTheme {
        FirstSyncPage(
            progress = 42, total = 128, done = false, error = null,
            onStart = {}, onRetry = {}, onSkip = {}, onCompleted = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun FirstSyncPageErrorPreview() {
    CallVaultTheme {
        FirstSyncPage(
            progress = 0, total = 0, done = false,
            error = "Couldn't read your call log right now. Try again.",
            onStart = {}, onRetry = {}, onSkip = {}, onCompleted = {}
        )
    }
}
