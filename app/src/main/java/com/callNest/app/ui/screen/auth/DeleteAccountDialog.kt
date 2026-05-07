package com.callNest.app.ui.screen.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callNest.app.R

/** Re-auth + confirm dialog. Returns the typed password to the caller for verification. */
@Composable
fun DeleteAccountDialog(
    busy: Boolean,
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.auth_delete_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.auth_delete_dialog_warning))
                Spacer(Modifier.height(12.dp))
                PasswordField(value = password, onValueChange = { password = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = !busy && password.isNotBlank(),
            ) {
                Text(
                    stringResource(R.string.auth_delete_dialog_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) {
                Text(stringResource(R.string.auth_delete_dialog_cancel))
            }
        },
    )
}
