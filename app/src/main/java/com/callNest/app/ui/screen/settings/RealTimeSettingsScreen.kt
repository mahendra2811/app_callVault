package com.callNest.app.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoTopBar
import com.callNest.app.ui.components.neo.NeoToggle
import com.callNest.app.ui.screen.shared.StandardPage
import com.callNest.app.ui.screen.shared.NeoScaffold
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.util.PermissionManager
import com.callNest.app.util.PermissionStatus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RealTimeSettingsEntryPoint {
    fun permissionManager(): PermissionManager
}

/**
 * Real-Time Features settings screen — bubble + popup toggles, timeout
 * slider, "unsaved only" filter, and an overlay-permission status row.
 */
@Composable
fun RealTimeSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RealTimeSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val pm = remember(ctx) {
        EntryPointAccessors.fromApplication(ctx.applicationContext, RealTimeSettingsEntryPoint::class.java)
            .permissionManager()
    }
    val permState by pm.state.collectAsState()
    val overlayGranted = permState.systemAlertWindow == PermissionStatus.Granted

    var localTimeout by remember(state.timeoutSec) { mutableFloatStateOf(state.timeoutSec.toFloat()) }

    StandardPage(
        title = stringResource(R.string.cv_realtime_title_v2),
        description = stringResource(R.string.cv_realtime_description),
        emoji = "✨",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ToggleRow(
                label = stringResource(R.string.realtime_bubble_label),
                description = stringResource(R.string.realtime_bubble_desc),
                value = state.floatingBubble,
                onChange = viewModel::setFloatingBubble
            )
            ToggleRow(
                label = stringResource(R.string.realtime_popup_label),
                description = stringResource(R.string.realtime_popup_desc),
                value = state.postCallPopup,
                onChange = viewModel::setPostCallPopup
            )
            if (state.postCallPopup) {
                Text(
                    text = stringResource(R.string.realtime_timeout_label, localTimeout.toInt()),
                    color = SageColors.TextSecondary
                )
                Slider(
                    value = localTimeout,
                    onValueChange = { localTimeout = it; viewModel.setTimeoutSec(it.toInt()) },
                    valueRange = 3f..30f,
                    steps = 26
                )
                ToggleRow(
                    label = stringResource(R.string.realtime_unsaved_only_label),
                    description = stringResource(R.string.realtime_unsaved_only_desc),
                    value = state.unsavedOnly,
                    onChange = viewModel::setUnsavedOnly
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (overlayGranted)
                        stringResource(R.string.realtime_overlay_granted)
                    else stringResource(R.string.realtime_overlay_denied),
                    color = SageColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                NeoButton(
                    text = stringResource(R.string.realtime_open_overlay_settings),
                    onClick = { pm.openOverlaySettings(ctx) }
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = SageColors.TextPrimary)
            Text(description, color = SageColors.TextSecondary)
        }
        NeoToggle(checked = value, onChange = onChange)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 640)
@Composable
private fun RealTimeSettingsPreview() {
    CallNestTheme {
        // Render a static, non-VM-backed preview body.
        Column(Modifier.padding(20.dp)) {
            Text("Floating bubble", color = SageColors.TextPrimary)
            Text("Post-call popup", color = SageColors.TextPrimary)
        }
    }
}
