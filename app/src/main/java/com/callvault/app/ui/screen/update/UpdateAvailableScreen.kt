package com.callvault.app.ui.screen.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.domain.repository.UpdateState
import com.callvault.app.ui.screen.shared.StandardPage

/**
 * Full-screen presentation of the current [UpdateState]. Renders different
 * layouts for Available / Downloading / ReadyToInstall / Installing / Error.
 */
@Composable
fun UpdateAvailableScreen(
    onClose: () -> Unit,
    vm: UpdateViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    StandardPage(
        title = stringResource(R.string.cv_update_available_title),
        description = stringResource(R.string.cv_update_available_description),
        emoji = "🆕",
        onBack = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            when (val s = state) {
            is UpdateState.Available -> AvailableBody(s, vm)
            is UpdateState.Downloading -> {
                Text(
                    "Downloading… ${s.progress}%",
                    style = MaterialTheme.typography.titleMedium
                )
                LinearProgressIndicator(
                    progress = { s.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is UpdateState.ReadyToInstall -> {
                Text(
                    "Update ready",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("CallVault v${s.manifest.version} is ready to install.")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.onInstall(s.file) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Install now") }
            }
            UpdateState.Installing -> Text("Opening installer…")
            is UpdateState.Error -> {
                Text("Update failed", style = MaterialTheme.typography.titleMedium)
                Text(s.reason)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.onCheck() }) { Text("Retry") }
            }
            UpdateState.Checking -> Text("Checking for updates…")
            UpdateState.UpToDate -> {
                Text("You're up to date.")
                TextButton(onClick = onClose) { Text("Close") }
            }
            UpdateState.Idle -> {
                Text("No update in progress.")
                TextButton(onClick = onClose) { Text("Close") }
            }
            }
        }
    }
}

@Composable
private fun AvailableBody(s: UpdateState.Available, vm: UpdateViewModel) {
    Text(
        "Update to v${s.manifest.version}",
        style = MaterialTheme.typography.headlineSmall
    )
    if (s.manifest.releaseNotes.isNotBlank()) {
        Text(
            s.manifest.releaseNotes,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { vm.onUpdate(s.manifest) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Update now") }
    OutlinedButton(
        onClick = { vm.onSkip(s.manifest.versionCode) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Skip this version") }
    TextButton(
        onClick = { vm.onDismiss() },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Later") }
}
