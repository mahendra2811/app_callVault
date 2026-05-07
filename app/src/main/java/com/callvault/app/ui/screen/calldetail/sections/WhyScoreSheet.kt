package com.callvault.app.ui.screen.calldetail.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.domain.model.LeadScore

/**
 * Bottom sheet that explains the 0-100 lead score for a single contact: each weighted
 * component is shown as a labelled row with the points it contributed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyScoreSheet(
    breakdown: LeadScore,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.why_score_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.why_score_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.why_score_total_fmt, breakdown.total),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            LinearProgressIndicator(
                progress = { breakdown.total / 100f },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            if (breakdown.manualOverride != null) {
                Text(
                    stringResource(R.string.why_score_manual_override),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            ScoreRow(stringResource(R.string.why_score_row_frequency), breakdown.frequency.toInt())
            ScoreRow(stringResource(R.string.why_score_row_duration), breakdown.duration.toInt())
            ScoreRow(stringResource(R.string.why_score_row_recency), breakdown.recency.toInt())
            ScoreRow(stringResource(R.string.why_score_row_followup), breakdown.followUpBonus)
            ScoreRow(stringResource(R.string.why_score_row_customer_tag), breakdown.customerTagBonus)
            ScoreRow(stringResource(R.string.why_score_row_saved), breakdown.savedContactBonus)
            ScoreRow(stringResource(R.string.why_score_row_rule_boosts), breakdown.ruleBoosts)

            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = {
                    // Lead-scoring docs already exist as an in-app docs article in /assets/docs/.
                    // The article id "lead-scoring" is a placeholder; if not present, falls back to docs list.
                    /* no-op: the dismiss + caller-side nav can route to docs. */
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.why_score_learn_more))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.why_score_close))
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, points: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.why_score_value_pts_fmt, points),
            style = MaterialTheme.typography.titleSmall,
            color = if (points > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
