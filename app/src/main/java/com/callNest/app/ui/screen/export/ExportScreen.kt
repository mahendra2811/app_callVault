package com.callNest.app.ui.screen.export

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoButtonVariant
import androidx.compose.ui.res.stringResource
import com.callNest.app.R
import com.callNest.app.ui.components.neo.NeoPageHeader
import com.callNest.app.ui.components.neo.NeoProgressBar
import com.callNest.app.ui.components.neo.NeoTopBar
import com.callNest.app.ui.screen.export.steps.ColumnsStep
import com.callNest.app.ui.screen.export.steps.DateRangeStep
import com.callNest.app.ui.screen.export.steps.DestinationStep
import com.callNest.app.ui.screen.export.steps.ScopeStep
import com.callNest.app.ui.screen.shared.NeoScaffold

/**
 * Top-level export wizard screen. Hosts the 5-step flow and the
 * progress / result feedback layer.
 */
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    val maxStep = 3 // date → scope → columns → destination (Excel-only)

    NeoScaffold(
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.cv_export_title),
                navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavClick = onBack
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeoButton(
                    text = "Back",
                    onClick = viewModel::back,
                    enabled = state.step > 0,
                    variant = NeoButtonVariant.Secondary
                )
                Spacer(Modifier.fillMaxWidth(0f))
                if (state.step >= maxStep) {
                    NeoButton(
                        text = "Generate",
                        onClick = viewModel::generate,
                        enabled = !state.isGenerating,
                        variant = NeoButtonVariant.Primary
                    )
                } else {
                    NeoButton(
                        text = "Next",
                        onClick = viewModel::next,
                        variant = NeoButtonVariant.Primary
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
        NeoPageHeader(
            title = stringResource(R.string.cv_export_title),
            description = stringResource(R.string.cv_export_description),
            emoji = "📤"
        )
        Box(Modifier.fillMaxSize()) {
            when (state.step) {
                0 -> DateRangeStep(state.dateRange, viewModel::setDateRange)
                1 -> ScopeStep(state.scope, viewModel::setScope)
                2 -> ColumnsStep(state.columns, viewModel::setColumns)
                3 -> DestinationStep(
                    selected = state.destination,
                    onSelect = viewModel::setDestination,
                    suggestedFileName = "callNest.${state.format.ext}",
                    suggestedMime = mimeFor(state.format)
                )
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
            if (state.isGenerating) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Exporting…", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.fillMaxWidth(0f))
                    NeoProgressBar(progress = 0.5f, modifier = Modifier.fillMaxWidth(0.7f))
                }
            }
        }
        }
    }

    LaunchedEffect(state.error) {
        val msg = state.error ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        viewModel.consumeError()
    }
    LaunchedEffect(state.result) {
        val r = state.result ?: return@LaunchedEffect
        val outcome = snackbar.showSnackbar(
            "Saved ${r.fileName}", actionLabel = "Open"
        )
        if (outcome == SnackbarResult.ActionPerformed) {
            try {
                val view = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(r.uri, mimeFor(ExportFormat.entries.first { it.ext == r.format }))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(Intent.createChooser(view, "Open with"))
            } catch (_: Throwable) { /* no app, ignore */ }
        }
        viewModel.consumeResult()
    }
}

private fun mimeFor(f: ExportFormat): String = when (f) {
    ExportFormat.Csv -> "text/csv"
    ExportFormat.Excel -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ExportFormat.Pdf -> "application/pdf"
    ExportFormat.Json -> "application/json"
    ExportFormat.Vcard -> "text/vcard"
}

