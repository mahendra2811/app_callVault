package com.callNest.app.ui.screen.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.components.neo.NeoTextField
import com.callNest.app.ui.components.neo.NeoToggle
import com.callNest.app.ui.components.neo.NeoTopBar
import com.callNest.app.ui.screen.shared.NeoScaffold
import com.callNest.app.ui.screen.shared.StandardPage
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.theme.NeoElevation

/**
 * Sprint 5 — auto-save configuration surface (spec §3.11 / §8.4).
 *
 * Persists every change through the debounced wiring inside
 * [AutoSaveSettingsViewModel]; the live preview is rebuilt synchronously
 * via [com.callNest.app.domain.usecase.AutoSaveNameBuilder] so the user
 * sees exactly what their next inquiry will look like.
 */
@Composable
fun AutoSaveSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AutoSaveSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val simFilter by viewModel.simFilterState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setEnabled(granted)
    }
    val onToggleEnabled: (Boolean) -> Unit = { wantsOn ->
        if (!wantsOn) {
            viewModel.setEnabled(false)
        } else {
            val hasWrite = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasWrite) {
                viewModel.setEnabled(true)
            } else {
                writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
            }
        }
    }
    AutoSaveSettingsContent(
        state = state,
        simFilter = simFilter,
        onBack = onBack,
        onToggleEnabled = onToggleEnabled,
        onPrefix = viewModel::setPrefix,
        onIncludeSimTag = viewModel::setIncludeSimTag,
        onSuffix = viewModel::setSuffix,
        onGroupName = viewModel::setGroupName,
        onApplyGroup = viewModel::applyGroupName,
        onPhoneLabel = viewModel::setPhoneLabel,
        onPhoneLabelCustom = viewModel::setPhoneLabelCustom,
        onRegion = viewModel::setRegion,
        onIncludeSim1 = viewModel::setIncludeSim1,
        onIncludeSim2 = viewModel::setIncludeSim2,
        modifier = modifier
    )
}

@Composable
private fun AutoSaveSettingsContent(
    state: AutoSaveSettingsUiState,
    simFilter: SimFilterState = SimFilterState(),
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onPrefix: (String) -> Unit,
    onIncludeSimTag: (Boolean) -> Unit,
    onSuffix: (String) -> Unit,
    onGroupName: (String) -> Unit,
    onApplyGroup: () -> Unit,
    onPhoneLabel: (String) -> Unit,
    onPhoneLabelCustom: (String) -> Unit,
    onRegion: (String) -> Unit,
    onIncludeSim1: (Boolean) -> Unit = {},
    onIncludeSim2: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    StandardPage(
        title = stringResource(R.string.cv_autosave_title),
        description = stringResource(R.string.cv_autosave_description),
        emoji = "💡",
        onBack = onBack,
        helpArticleId = "03-auto-save"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master toggle is temporarily replaced with a "Coming soon" card.
            // For v1.0.0 auto-save is manual-only — see Inquiries tab for the
            // Save-now flow. Restore by uncommenting the Row below.
            NeoSurface(
                modifier = Modifier.fillMaxWidth(),
                elevation = NeoElevation.ConcaveSmall,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.auto_save_master_toggle),
                            style = MaterialTheme.typography.titleSmall,
                            color = SageColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Coming soon",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeoColors.AccentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Auto-save is temporarily manual. Open Inquiries and tap Save now (per row or bulk).",
                        style = MaterialTheme.typography.bodySmall,
                        color = SageColors.TextSecondary
                    )
                }
            }
            @Suppress("UNUSED_EXPRESSION") run { onToggleEnabled }
            // Row(
            //     modifier = Modifier.fillMaxWidth(),
            //     verticalAlignment = Alignment.CenterVertically
            // ) {
            //     Column(modifier = Modifier.weight(1f)) {
            //         Text(
            //             text = stringResource(R.string.auto_save_master_toggle),
            //             style = MaterialTheme.typography.titleSmall,
            //             color = SageColors.TextPrimary,
            //             fontWeight = FontWeight.SemiBold
            //         )
            //         Text(
            //             text = stringResource(R.string.auto_save_master_subtitle),
            //             style = MaterialTheme.typography.bodySmall,
            //             color = SageColors.TextSecondary
            //         )
            //     }
            //     NeoToggle(checked = state.enabled, onChange = onToggleEnabled)
            // }

            // Prefix
            NeoTextField(
                value = state.prefix,
                onChange = onPrefix,
                label = stringResource(R.string.auto_save_prefix_label),
                placeholder = stringResource(R.string.auto_save_prefix_hint)
            )

            // SIM tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auto_save_sim_tag_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SageColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                NeoToggle(checked = state.includeSimTag, onChange = onIncludeSimTag)
            }

            // Per-SIM include filter — opt out of one SIM line entirely while
            // keeping the other flowing into auto-save.
            Column {
                Text(
                    text = "Apply auto-save to",
                    style = MaterialTheme.typography.labelLarge,
                    color = SageColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SIM 1 calls",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SageColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    NeoToggle(checked = simFilter.includeSim1, onChange = onIncludeSim1)
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SIM 2 calls",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SageColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    NeoToggle(checked = simFilter.includeSim2, onChange = onIncludeSim2)
                }
            }

            // Suffix is brand-locked to "callNest" and applied silently when saving;
            // no UI surface anymore. (See AutoSaveContactUseCase.BRAND_SUFFIX.)

            // Live preview
            NeoSurface(
                modifier = Modifier.fillMaxWidth(),
                elevation = NeoElevation.ConcaveSmall,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.auto_save_preview_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = SageColors.TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.preview,
                        style = MaterialTheme.typography.titleMedium,
                        color = NeoColors.AccentBlue
                    )
                }
            }

            // Group name + Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                NeoTextField(
                    value = state.groupName,
                    onChange = onGroupName,
                    label = stringResource(R.string.auto_save_group_label),
                    placeholder = stringResource(R.string.auto_save_group_hint),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                NeoButton(
                    text = stringResource(R.string.auto_save_group_apply),
                    onClick = onApplyGroup
                )
            }

            // Phone label radios
            Column {
                Text(
                    text = stringResource(R.string.auto_save_phone_label_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = SageColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LabelRadioRow("Mobile", stringResource(R.string.auto_save_phone_label_mobile), state.phoneLabel, onPhoneLabel)
                LabelRadioRow("Work", stringResource(R.string.auto_save_phone_label_work), state.phoneLabel, onPhoneLabel)
                LabelRadioRow("Home", stringResource(R.string.auto_save_phone_label_home), state.phoneLabel, onPhoneLabel)
                LabelRadioRow("Custom", stringResource(R.string.auto_save_phone_label_other), state.phoneLabel, onPhoneLabel)
                if (state.phoneLabel == "Custom") {
                    Spacer(Modifier.height(6.dp))
                    NeoTextField(
                        value = state.phoneLabelCustom,
                        onChange = onPhoneLabelCustom,
                        label = stringResource(R.string.auto_save_phone_label_custom_hint)
                    )
                }
            }

            // Region
            NeoTextField(
                value = state.region,
                onChange = onRegion,
                label = stringResource(R.string.auto_save_region_label),
                placeholder = stringResource(R.string.auto_save_region_hint)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LabelRadioRow(
    value: String,
    text: String,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected == value, onClick = { onSelect(value) })
        Spacer(Modifier.width(4.dp))
        Text(text = text, color = SageColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun AutoSaveSettingsPreview() {
    CallNestTheme {
        AutoSaveSettingsContent(
            state = AutoSaveSettingsUiState(),
            onBack = {},
            onToggleEnabled = {},
            onPrefix = {},
            onIncludeSimTag = {},
            onSuffix = {},
            onGroupName = {},
            onApplyGroup = {},
            onPhoneLabel = {},
            onPhoneLabelCustom = {},
            onRegion = {}
        )
    }
}
