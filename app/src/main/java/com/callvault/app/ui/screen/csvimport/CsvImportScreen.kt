package com.callvault.app.ui.screen.csvimport

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R

@Composable
fun CsvImportScreen(
    onBack: () -> Unit,
    viewModel: CsvImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.onPicked(uri) }

    val doneFmt = stringResource(R.string.csv_import_done_fmt)
    val failFmt = stringResource(R.string.csv_import_failed_fmt)
    val permDeniedMsg = stringResource(R.string.csv_import_perm_denied)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.import(doneFmt, failFmt)
        else viewModel.setError(permDeniedMsg)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.csv_import_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.csv_import_format_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { launcher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.csv_import_pick_file)) }

            if (state.sourceUri == null) {
                com.callvault.app.ui.components.EmptyState(
                    emoji = "📥",
                    title = stringResource(R.string.csv_empty_title),
                    body = stringResource(R.string.csv_empty_body),
                )
            } else {
                Text(
                    stringResource(R.string.csv_import_parsed_fmt, state.rows.size, state.skipped),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (state.skipped > 0) {
                    Text(
                        stringResource(R.string.csv_import_skipped_reasons),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.rows.isNotEmpty()) {
                Text(
                    stringResource(R.string.csv_import_preview_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                HorizontalDivider()
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(state.rows.take(50)) { row ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(row.name ?: row.normalized, style = MaterialTheme.typography.bodyLarge)
                            if (row.name != null) {
                                Text(
                                    row.normalized,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) viewModel.import(doneFmt, failFmt)
                    else permissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                },
                enabled = state.rows.isNotEmpty() && !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.busy) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(stringResource(R.string.csv_import_button))
            }

            state.resultMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
