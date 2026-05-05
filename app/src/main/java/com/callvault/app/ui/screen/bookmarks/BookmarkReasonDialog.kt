package com.callvault.app.ui.screen.bookmarks

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoDialog
import com.callvault.app.ui.components.neo.NeoTextField
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.ui.theme.Spacing

/**
 * Inline dialog shown the **first** time a number is bookmarked, prompting
 * the user to record a quick reason. Skipping is allowed and persists the
 * bookmark with `bookmarkReason = null`.
 */
@Composable
fun BookmarkReasonDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: String?) -> Unit
) {
    var reason by rememberSaveable { mutableStateOf("") }
    NeoDialog(
        onDismissRequest = onDismiss,
        header = {
            Text(
                stringResource(R.string.bookmarks_reason_title),
                style = MaterialTheme.typography.titleLarge,
                color = SageColors.TextPrimary
            )
        },
        body = {
            Spacer(Modifier.height(Spacing.Sm))
            Text(
                stringResource(R.string.bookmarks_reason_body),
                color = SageColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(Spacing.Md))
            NeoTextField(
                value = reason,
                onChange = { reason = it },
                label = "",
                placeholder = stringResource(R.string.bookmarks_reason_placeholder)
            )
        },
        footer = {
            NeoButton(
                text = stringResource(R.string.bookmarks_reason_skip),
                onClick = { onSubmit(null) },
                variant = NeoButtonVariant.Tertiary
            )
            Spacer(Modifier.width(Spacing.Sm))
            NeoButton(
                text = stringResource(R.string.bookmarks_reason_save),
                onClick = { onSubmit(reason.trim().takeIf { it.isNotEmpty() }) },
                variant = NeoButtonVariant.Primary
            )
        }
    )
}
