package com.callvault.app.ui.screen.export.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.screen.export.ExportScope
import com.callvault.app.ui.theme.NeoColors

/** Wizard step 2 — pick scope: current filter vs all data. */
@Composable
fun ScopeStep(
    selected: ExportScope,
    onSelect: (ExportScope) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("What to export", style = MaterialTheme.typography.titleLarge, color = NeoColors.OnBase)
        Spacer(Modifier.height(12.dp))
        ExportScope.entries.forEach { s ->
            NeoCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                onClick = { onSelect(s) }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        when (s) {
                            ExportScope.CurrentFilter -> "Current filter"
                            ExportScope.AllData -> "All data"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (s == selected) NeoColors.AccentBlue else NeoColors.OnBase
                    )
                    Text(
                        when (s) {
                            ExportScope.CurrentFilter -> "Honor the filter applied on the Calls screen."
                            ExportScope.AllData -> "Every call in your vault."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = NeoColors.OnBaseMuted
                    )
                }
            }
        }
    }
}
