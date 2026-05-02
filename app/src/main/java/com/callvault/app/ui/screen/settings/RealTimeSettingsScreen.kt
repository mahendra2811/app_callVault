package com.callvault.app.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.components.neo.NeoToggle
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.util.PermissionManager
import com.callvault.app.util.PermissionStatus
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

    NeoScaffold(
        modifier = modifier,
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.realtime_settings_title),
                navIcon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                onNavClick = onBack
            )
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
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
                    color = NeoColors.OnBaseMuted
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
                    color = NeoColors.OnBase,
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
            Text(label, color = NeoColors.OnBase)
            Text(description, color = NeoColors.OnBaseMuted)
        }
        NeoToggle(checked = value, onChange = onChange)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 640)
@Composable
private fun RealTimeSettingsPreview() {
    CallVaultTheme {
        // Render a static, non-VM-backed preview body.
        Column(Modifier.padding(20.dp)) {
            Text("Floating bubble", color = NeoColors.OnBase)
            Text("Post-call popup", color = NeoColors.OnBase)
        }
    }
}
