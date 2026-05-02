package com.callvault.app.ui.screen.bookmarks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoTextField
import com.callvault.app.ui.theme.NeoColors

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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeoColors.Base,
        title = { Text(stringResource(R.string.bookmarks_reason_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.bookmarks_reason_body),
                    color = NeoColors.OnBaseMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                NeoTextField(
                    value = reason,
                    onChange = { reason = it },
                    label = "",
                    placeholder = stringResource(R.string.bookmarks_reason_placeholder)
                )
            }
        },
        confirmButton = {
            NeoButton(
                text = stringResource(R.string.bookmarks_reason_save),
                onClick = { onSubmit(reason.trim().takeIf { it.isNotEmpty() }) },
                variant = NeoButtonVariant.Primary
            )
        },
        dismissButton = {
            NeoButton(
                text = stringResource(R.string.bookmarks_reason_skip),
                onClick = { onSubmit(null) },
                variant = NeoButtonVariant.Tertiary
            )
        }
    )
}
