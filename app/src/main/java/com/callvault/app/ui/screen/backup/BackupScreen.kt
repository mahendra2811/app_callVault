package com.callvault.app.ui.screen.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoSlider
import androidx.compose.material3.OutlinedTextField
import com.callvault.app.ui.components.neo.NeoToggle
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.screen.shared.StandardPage
import androidx.compose.ui.res.stringResource
import com.callvault.app.R
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors

/** Backup & restore landing screen. */
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val activity = LocalContext.current as? Activity

    var passDialog by remember { mutableStateOf<PassphraseDialogReason?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var confirmRestore by remember { mutableStateOf<Pair<android.net.Uri, String>?>(null) }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            passDialog = PassphraseDialogReason.Restore
        }
    }

    StandardPage(
        title = stringResource(R.string.cv_backup_title),
        description = stringResource(R.string.cv_backup_description),
        emoji = "💾",
        onBack = onBack
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Manual backup card
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Manual backup now", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Encrypt every call, tag, contact and rule into a single .cvb file in Downloads.",
                        style = MaterialTheme.typography.bodySmall, color = SageColors.TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    NeoButton(
                        text = if (state.isWorking) "Working…" else "Back up",
                        onClick = {
                            if (state.passphraseSet) viewModel.runBackup()
                            else passDialog = PassphraseDialogReason.SetForBackup
                        },
                        enabled = !state.isWorking,
                        variant = NeoButtonVariant.Primary
                    )
                }
            }
            // Restore card
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Restore from file", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "This will replace all your data. Pick a .cvb file and enter its passphrase.",
                        style = MaterialTheme.typography.bodySmall, color = SageColors.TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    NeoButton(
                        text = "Restore…",
                        onClick = { openDocLauncher.launch(arrayOf("*/*")) },
                        enabled = !state.isWorking,
                        variant = NeoButtonVariant.Secondary
                    )
                }
            }
            // Passphrase card
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Backup encryption passphrase", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.passphraseSet) "Status: set (•••••••)" else "Status: not set",
                        style = MaterialTheme.typography.bodySmall, color = SageColors.TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    NeoButton(
                        text = if (state.passphraseSet) "Change" else "Set",
                        onClick = { passDialog = PassphraseDialogReason.SetOnly },
                        variant = NeoButtonVariant.Secondary
                    )
                }
            }
            // Auto-backup card
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Auto-backup daily at 2 AM", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Runs in the background when your phone is idle.",
                                style = MaterialTheme.typography.bodySmall, color = SageColors.TextSecondary
                            )
                        }
                        NeoToggle(
                            checked = state.autoBackupEnabled,
                            onChange = viewModel::setAutoBackupEnabled
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Keep last ${state.retention} backups", style = MaterialTheme.typography.bodyMedium)
                    NeoSlider(
                        value = state.retention.toFloat(),
                        onChange = { viewModel.setRetention(it.toInt()) },
                        range = 3f..14f,
                        steps = 10
                    )
                }
            }
            // Cloud (Google Drive)
            CloudBackupCard(
                state = state,
                onToggleEnabled = { v -> viewModel.setDriveEnabled(v, activity) },
                onSignIn = { activity?.let(viewModel::signIn) },
                onSignOut = viewModel::signOut,
                onUploadNow = viewModel::uploadToDrive,
                onAutoUploadChange = viewModel::setDriveAutoUpload
            )
            SnackbarHost(snackbar)
        }
    }

    // Passphrase dialog
    passDialog?.let { reason ->
        PassphraseDialog(
            title = when (reason) {
                PassphraseDialogReason.SetOnly -> "Set passphrase"
                PassphraseDialogReason.SetForBackup -> "Set passphrase to back up"
                PassphraseDialogReason.Restore -> "Enter passphrase to restore"
            },
            onConfirm = { value ->
                when (reason) {
                    PassphraseDialogReason.SetOnly -> viewModel.setPassphrase(value)
                    PassphraseDialogReason.SetForBackup -> {
                        viewModel.setPassphrase(value); viewModel.runBackup(value)
                    }
                    PassphraseDialogReason.Restore -> {
                        val uri = pendingRestoreUri
                        if (uri != null) confirmRestore = uri to value
                    }
                }
                passDialog = null
            },
            onDismiss = { passDialog = null; pendingRestoreUri = null }
        )
    }

    confirmRestore?.let { (uri, pass) ->
        com.callvault.app.ui.components.neo.NeoDialog(
            onDismissRequest = { confirmRestore = null },
            header = {
                Text(
                    "Replace all data?",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            body = {
                Spacer(Modifier.height(com.callvault.app.ui.theme.Spacing.Sm))
                Text("This will replace all your data. Continue?")
            },
            footer = {
                NeoButton(text = "Cancel", onClick = { confirmRestore = null }, variant = NeoButtonVariant.Tertiary)
                Spacer(Modifier.width(com.callvault.app.ui.theme.Spacing.Sm))
                NeoButton(
                    text = "Replace",
                    onClick = { viewModel.runRestore(uri, pass); confirmRestore = null },
                    variant = NeoButtonVariant.Primary
                )
            }
        )
    }

    LaunchedEffect(state.message) {
        val m = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(m); viewModel.consumeMessage()
    }
    LaunchedEffect(state.error) {
        val e = state.error ?: return@LaunchedEffect
        snackbar.showSnackbar(e); viewModel.consumeError()
    }
    LaunchedEffect(state.driveError) {
        val e = state.driveError ?: return@LaunchedEffect
        snackbar.showSnackbar(e); viewModel.consumeDriveError()
    }
}

/** Cloud-backup section. Hoist-friendly so previews can render every state. */
@Composable
private fun CloudBackupCard(
    state: BackupUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onUploadNow: () -> Unit,
    onAutoUploadChange: (Boolean) -> Unit
) {
    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.cv_backup_cloud_title),
                    style = MaterialTheme.typography.titleMedium
                )
                NeoToggle(
                    checked = state.driveEnabled,
                    onChange = onToggleEnabled
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.cv_backup_drive_toggle),
                style = MaterialTheme.typography.bodySmall,
                color = SageColors.TextSecondary
            )
            val email = state.driveSignedInEmail
            if (email != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.cv_backup_drive_signed_in, email),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    NeoButton(
                        text = stringResource(R.string.cv_backup_drive_signout),
                        onClick = onSignOut,
                        variant = NeoButtonVariant.Tertiary,
                        enabled = !state.driveBusy
                    )
                }
                Spacer(Modifier.height(12.dp))
                NeoButton(
                    text = if (state.driveBusy) "Working…"
                    else stringResource(R.string.cv_backup_drive_upload_now),
                    onClick = onUploadNow,
                    enabled = state.passphraseSet && !state.driveBusy,
                    variant = NeoButtonVariant.Primary
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.cv_backup_drive_auto_upload),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    NeoToggle(
                        checked = state.driveAutoUpload,
                        onChange = onAutoUploadChange
                    )
                }
            } else if (state.driveEnabled) {
                Spacer(Modifier.height(12.dp))
                NeoButton(
                    text = "Sign in to Google",
                    onClick = onSignIn,
                    enabled = !state.driveBusy,
                    variant = NeoButtonVariant.Primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.cv_backup_drive_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = SageColors.TextSecondary
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Cloud — Signed out", showBackground = true)
@Composable
private fun PreviewCloudSignedOut() {
    com.callvault.app.ui.theme.CallVaultTheme {
        CloudBackupCard(
            state = BackupUiState(passphraseSet = true, driveEnabled = false),
            onToggleEnabled = {}, onSignIn = {}, onSignOut = {}, onUploadNow = {}, onAutoUploadChange = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Cloud — Signed in", showBackground = true)
@Composable
private fun PreviewCloudSignedIn() {
    com.callvault.app.ui.theme.CallVaultTheme {
        CloudBackupCard(
            state = BackupUiState(
                passphraseSet = true, driveEnabled = true,
                driveSignedInEmail = "owner@example.com"
            ),
            onToggleEnabled = {}, onSignIn = {}, onSignOut = {}, onUploadNow = {}, onAutoUploadChange = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Cloud — Uploading", showBackground = true)
@Composable
private fun PreviewCloudUploading() {
    com.callvault.app.ui.theme.CallVaultTheme {
        CloudBackupCard(
            state = BackupUiState(
                passphraseSet = true, driveEnabled = true,
                driveSignedInEmail = "owner@example.com", driveBusy = true
            ),
            onToggleEnabled = {}, onSignIn = {}, onSignOut = {}, onUploadNow = {}, onAutoUploadChange = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Cloud — Error", showBackground = true)
@Composable
private fun PreviewCloudError() {
    com.callvault.app.ui.theme.CallVaultTheme {
        CloudBackupCard(
            state = BackupUiState(
                passphraseSet = true, driveEnabled = true,
                driveSignedInEmail = "owner@example.com",
                driveError = "Couldn't upload to Drive. Network unreachable."
            ),
            onToggleEnabled = {}, onSignIn = {}, onSignOut = {}, onUploadNow = {}, onAutoUploadChange = {}
        )
    }
}

private enum class PassphraseDialogReason { SetOnly, SetForBackup, Restore }

@Composable
private fun PassphraseDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf("") }
    com.callvault.app.ui.components.neo.NeoDialog(
        onDismissRequest = onDismiss,
        header = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        body = {
            Spacer(Modifier.height(com.callvault.app.ui.theme.Spacing.Sm))
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text("Passphrase") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        },
        footer = {
            NeoButton(text = "Cancel", onClick = onDismiss, variant = NeoButtonVariant.Tertiary)
            Spacer(Modifier.width(com.callvault.app.ui.theme.Spacing.Sm))
            NeoButton(
                text = "OK",
                onClick = { if (value.isNotBlank()) onConfirm(value) },
                variant = NeoButtonVariant.Primary
            )
        }
    )
}
