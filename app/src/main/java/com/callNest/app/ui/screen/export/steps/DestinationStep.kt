package com.callNest.app.ui.screen.export.steps

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.callNest.app.ui.components.neo.NeoCard
import com.callNest.app.ui.screen.export.DestinationChoice
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors

/** Wizard step 4 — choose Downloads or a SAF-picked URI. */
@Composable
fun DestinationStep(
    selected: DestinationChoice,
    onSelect: (DestinationChoice) -> Unit,
    suggestedFileName: String,
    suggestedMime: String,
    modifier: Modifier = Modifier
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(suggestedMime)
    ) { uri ->
        if (uri != null) onSelect(DestinationChoice.PickedUri(uri))
    }
    Column(modifier = modifier.padding(16.dp)) {
        Text("Where to save", style = MaterialTheme.typography.titleLarge, color = SageColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        NeoCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            onClick = { onSelect(DestinationChoice.Downloads) }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Downloads folder",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected is DestinationChoice.Downloads) NeoColors.AccentBlue else SageColors.TextPrimary
                )
                Text(
                    "Saved as $suggestedFileName",
                    style = MaterialTheme.typography.bodySmall,
                    color = SageColors.TextSecondary
                )
            }
        }
        NeoCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            onClick = { picker.launch(suggestedFileName) }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Pick a location…",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected is DestinationChoice.PickedUri) NeoColors.AccentBlue else SageColors.TextPrimary
                )
                Text(
                    if (selected is DestinationChoice.PickedUri) "Selected: ${selected.uri.lastPathSegment ?: "ready"}"
                    else "Choose any folder via the system picker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SageColors.TextSecondary
                )
            }
        }
    }
}
