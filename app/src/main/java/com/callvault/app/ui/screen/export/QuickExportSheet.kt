package com.callvault.app.ui.screen.export

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoBottomSheet
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoLoader
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.IconBackupTint
import com.callvault.app.ui.theme.NeoColors
import kotlinx.coroutines.delay

/**
 * One-tap export sheet for the user's current filter (CSV / Excel) plus a
 * full-database JSON dump. Files land in `Downloads/`.
 */
@Composable
fun QuickExportSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickExportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state is QuickExportViewModel.QuickExportUiState.Success) {
            delay(3000)
            onDismiss()
        }
    }

    NeoBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.cv_quick_export_title),
                style = MaterialTheme.typography.titleLarge,
                color = NeoColors.OnBase
            )
            Text(
                text = stringResource(R.string.cv_quick_export_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = NeoColors.OnBaseSubtle
            )

            ExportRow(
                icon = Icons.Filled.Description,
                iconTint = IconBackupTint,
                label = stringResource(R.string.cv_quick_export_csv_label),
                enabled = state !is QuickExportViewModel.QuickExportUiState.Running,
                onClick = viewModel::exportCsv
            )
            ExportRow(
                icon = Icons.Filled.TableChart,
                iconTint = IconBackupTint,
                label = stringResource(R.string.cv_quick_export_excel_label),
                enabled = state !is QuickExportViewModel.QuickExportUiState.Running,
                onClick = viewModel::exportExcel
            )
            ExportRow(
                icon = Icons.Filled.Save,
                iconTint = IconBackupTint,
                label = stringResource(R.string.cv_quick_export_json_label),
                enabled = state !is QuickExportViewModel.QuickExportUiState.Running,
                onClick = viewModel::exportJson
            )

            Spacer(Modifier.height(4.dp))
            StatusRow(
                state = state,
                onOpen = { uri ->
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, context.contentResolver.getType(uri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                },
                onShare = { uri ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = context.contentResolver.getType(uri) ?: "*/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                },
                onRetry = viewModel::reset
            )
        }
    }
}

@Composable
private fun ExportRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (enabled) onClick else null,
        border = NeoColors.BorderSoft
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            Spacer(Modifier.size(16.dp))
            Text(
                text = label,
                color = NeoColors.OnBase,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun StatusRow(
    state: QuickExportViewModel.QuickExportUiState,
    onOpen: (Uri) -> Unit,
    onShare: (Uri) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        QuickExportViewModel.QuickExportUiState.Idle -> Unit
        QuickExportViewModel.QuickExportUiState.Running -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoLoader(size = 36.dp)
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.cv_quick_export_running),
                    color = NeoColors.OnBaseMuted
                )
            }
        }
        is QuickExportViewModel.QuickExportUiState.Success -> {
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                border = Color(0xFF2E7D32)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.cv_quick_export_success,
                            state.fileName,
                            (state.sizeBytes / 1024L).coerceAtLeast(0L)
                        ),
                        color = NeoColors.OnBase,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(onClick = { onOpen(state.uri) }) {
                            Text(stringResource(R.string.cv_quick_export_open))
                        }
                        Spacer(Modifier.size(8.dp))
                        TextButton(onClick = { onShare(state.uri) }) {
                            Text(stringResource(R.string.cv_quick_export_share))
                        }
                    }
                }
            }
        }
        is QuickExportViewModel.QuickExportUiState.Error -> {
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                border = Color(0xFFC62828)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.cv_quick_export_error, state.reason),
                        color = NeoColors.OnBase,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.cv_quick_export_retry))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "Idle")
@Composable
private fun QuickExportSheetIdlePreview() {
    CallVaultTheme {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Quick Export", style = MaterialTheme.typography.titleLarge)
            Text(
                "Saves to your Downloads folder.",
                style = MaterialTheme.typography.bodySmall,
                color = NeoColors.OnBaseSubtle
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "Running")
@Composable
private fun QuickExportSheetRunningPreview() {
    CallVaultTheme {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoLoader(size = 36.dp)
            Spacer(Modifier.size(12.dp))
            Text("Exporting…", color = NeoColors.OnBaseMuted)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "Success")
@Composable
private fun QuickExportSheetSuccessPreview() {
    CallVaultTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Exported callvault-quick-20260430-1130.csv (42 KB)")
            Row {
                TextButton(onClick = {}) { Text("Open") }
                TextButton(onClick = {}) { Text("Share") }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, name = "Error")
@Composable
private fun QuickExportSheetErrorPreview() {
    CallVaultTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Couldn't export. Storage permission missing.")
            TextButton(onClick = {}) { Text("Retry") }
        }
    }
}
