package com.callvault.app.ui.screen.export.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.components.neo.NeoChip
import com.callvault.app.ui.screen.export.DateRangeChoice
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors

/** Wizard step 1 — pick the date range with quick chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateRangeStep(
    selected: DateRangeChoice,
    onSelect: (DateRangeChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Date range", style = MaterialTheme.typography.titleLarge, color = SageColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chip("All time", selected is DateRangeChoice.AllTime) { onSelect(DateRangeChoice.AllTime) }
            chip("Last 7 days", selected is DateRangeChoice.Last7) { onSelect(DateRangeChoice.Last7) }
            chip("Last 30 days", selected is DateRangeChoice.Last30) { onSelect(DateRangeChoice.Last30) }
            chip("Last 90 days", selected is DateRangeChoice.Last90) { onSelect(DateRangeChoice.Last90) }
            chip("This month", selected is DateRangeChoice.ThisMonth) { onSelect(DateRangeChoice.ThisMonth) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Custom date pickers land in a follow-up — pick a preset for now.",
            style = MaterialTheme.typography.bodySmall,
            color = SageColors.TextSecondary
        )
    }
}

@Composable
private fun chip(label: String, selected: Boolean, onClick: () -> Unit) {
    NeoChip(text = label, selected = selected, onClick = onClick)
}
