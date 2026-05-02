package com.callvault.app.ui.screen.backup

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import com.callvault.app.ui.theme.NeoColors

/** Backup & restore landing screen. */
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

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

    NeoScaffold(
        topBar = {
            NeoTopBar(
                title = "Backup & restore",
                navIcon = Icons.Filled.ArrowBack,
                onNavClick = onBack
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Manual backup card
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Manual backup now", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Encrypt every call, tag, contact and rule into a single .cvb file in Downloads.",
                        style = MaterialTheme.typography.bodySmall, color = NeoColors.OnBaseMuted
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
                        style = MaterialTheme.typography.bodySmall, color = NeoColors.OnBaseMuted
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
                        style = MaterialTheme.typography.bodySmall, color = NeoColors.OnBaseMuted
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
                                style = MaterialTheme.typography.bodySmall, color = NeoColors.OnBaseMuted
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
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("Replace all data?") },
            text = { Text("This will replace all your data. Continue?") },
            confirmButton = {
                NeoButton(
                    text = "Replace",
                    onClick = { viewModel.runRestore(uri, pass); confirmRestore = null },
                    variant = NeoButtonVariant.Primary
                )
            },
            dismissButton = {
                NeoButton(text = "Cancel", onClick = { confirmRestore = null })
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
}

private enum class PassphraseDialogReason { SetOnly, SetForBackup, Restore }

@Composable
private fun PassphraseDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text("Passphrase") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        },
        confirmButton = {
            NeoButton(
                text = "OK",
                onClick = { if (value.isNotBlank()) onConfirm(value) },
                variant = NeoButtonVariant.Primary
            )
        },
        dismissButton = { NeoButton(text = "Cancel", onClick = onDismiss) }
    )
}
