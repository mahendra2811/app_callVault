package com.callvault.app.ui.screen.export.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.callvault.app.data.export.ExportColumns
import com.callvault.app.ui.components.neo.NeoToggle
import com.callvault.app.ui.theme.NeoColors

/** Wizard step 3 — toggle each available column. CSV/Excel only. */
@Composable
fun ColumnsStep(
    columns: ExportColumns,
    onChange: (ExportColumns) -> Unit,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    Column(modifier = modifier.padding(16.dp).verticalScroll(scroll)) {
        Text("Columns", style = MaterialTheme.typography.titleLarge, color = NeoColors.OnBase)
        Spacer(Modifier.height(12.dp))
        row("Date", columns.date) { onChange(columns.copy(date = it)) }
        row("Number", columns.number) { onChange(columns.copy(number = it)) }
        row("Name", columns.name) { onChange(columns.copy(name = it)) }
        row("Type", columns.type) { onChange(columns.copy(type = it)) }
        row("Duration", columns.duration) { onChange(columns.copy(duration = it)) }
        row("SIM slot", columns.simSlot) { onChange(columns.copy(simSlot = it)) }
        row("Tags", columns.tags) { onChange(columns.copy(tags = it)) }
        row("Notes", columns.notes) { onChange(columns.copy(notes = it)) }
        row("Lead score", columns.leadScore) { onChange(columns.copy(leadScore = it)) }
        row("Location", columns.geocodedLocation) { onChange(columns.copy(geocodedLocation = it)) }
        row("Bookmarked", columns.isBookmarked) { onChange(columns.copy(isBookmarked = it)) }
        row("Archived", columns.isArchived) { onChange(columns.copy(isArchived = it)) }
    }
}

@Composable
private fun row(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = NeoColors.OnBase)
        NeoToggle(checked = checked, onChange = onChange)
    }
}
