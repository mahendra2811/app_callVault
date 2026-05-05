package com.callvault.app.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.domain.model.LeadScoreWeights
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoSlider
import com.callvault.app.ui.components.neo.NeoToggle
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors

/**
 * Sprint 6 — Lead-scoring settings: master toggle plus six tunables that
 * control [LeadScoreWeights]. Edits debounce-save 400 ms after the user stops
 * adjusting and enqueue [com.callvault.app.data.work.LeadScoreRecomputeWorker]
 * so cached scores update.
 *
 * @param onBack pops back to the calling screen
 */
@Composable
fun LeadScoringSettingsScreen(
    onBack: () -> Unit,
    viewModel: LeadScoringSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StandardPage(
        title = stringResource(R.string.cv_leadscore_title),
        description = stringResource(R.string.cv_leadscore_description),
        emoji = "🎯",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.lead_scoring_master_toggle),
                    color = SageColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                NeoToggle(checked = state.enabled, onChange = viewModel::setEnabled)
            }

            WeightSlider(
                label = stringResource(R.string.lead_scoring_weight_freq),
                value = state.weights.weightFreq.toFloat(),
                onChange = { v -> viewModel.setWeights { it.copy(weightFreq = v.toDouble()) } }
            )
            WeightSlider(
                label = stringResource(R.string.lead_scoring_weight_duration),
                value = state.weights.weightDuration.toFloat(),
                onChange = { v -> viewModel.setWeights { it.copy(weightDuration = v.toDouble()) } }
            )
            WeightSlider(
                label = stringResource(R.string.lead_scoring_weight_recency),
                value = state.weights.weightRecency.toFloat(),
                onChange = { v -> viewModel.setWeights { it.copy(weightRecency = v.toDouble()) } }
            )
            BonusSlider(
                label = stringResource(R.string.lead_scoring_bonus_followup),
                value = state.weights.bonusFollowUp,
                onChange = { v -> viewModel.setWeights { it.copy(bonusFollowUp = v) } }
            )
            BonusSlider(
                label = stringResource(R.string.lead_scoring_bonus_customer_tag),
                value = state.weights.bonusCustomerTag,
                onChange = { v -> viewModel.setWeights { it.copy(bonusCustomerTag = v) } }
            )
            BonusSlider(
                label = stringResource(R.string.lead_scoring_bonus_saved_contact),
                value = state.weights.bonusSavedContact,
                onChange = { v -> viewModel.setWeights { it.copy(bonusSavedContact = v) } }
            )

            Spacer(Modifier.height(8.dp))
            NeoButton(
                text = stringResource(R.string.lead_scoring_reset),
                onClick = viewModel::resetDefaults,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WeightSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, color = SageColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                text = "%.2f".format(value),
                color = SageColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
        NeoSlider(value = value.coerceIn(0f, 1f), onChange = onChange, range = 0f..1f)
    }
}

@Composable
private fun BonusSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, color = SageColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                text = "+$value",
                color = SageColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
        NeoSlider(
            value = value.toFloat().coerceIn(0f, 50f),
            onChange = { onChange(it.toInt()) },
            range = 0f..50f,
            steps = 49
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 640)
@Composable
private fun LeadScoringSettingsPreview() {
    CallVaultTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WeightSlider(label = "Frequency weight", value = 0.25f, onChange = {})
            BonusSlider(label = "Follow-up bonus", value = 10, onChange = {})
        }
    }
}
