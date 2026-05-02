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
import com.callvault.app.ui.screen.export.ExportFormat
import com.callvault.app.ui.theme.NeoColors

/** Wizard step 0 — pick the output format. */
@Composable
fun FormatStep(
    selected: ExportFormat,
    onSelect: (ExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Choose a format", style = MaterialTheme.typography.titleLarge, color = NeoColors.OnBase)
        Spacer(Modifier.height(12.dp))
        ExportFormat.entries.forEach { fmt ->
            NeoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                onClick = { onSelect(fmt) }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        fmt.display,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (fmt == selected) NeoColors.AccentBlue else NeoColors.OnBase
                    )
                    Text(
                        when (fmt) {
                            ExportFormat.Excel -> "Multi-sheet workbook with calls, contacts, tags & stats."
                            ExportFormat.Csv -> "Plain CSV honoring your column choices."
                            ExportFormat.Pdf -> "Cover page, totals and a paginated calls table."
                            ExportFormat.Json -> "Full database dump for backups or migration."
                            ExportFormat.Vcard -> "Contacts in vCard 3.0 — ready for any address book."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = NeoColors.OnBaseMuted
                    )
                }
            }
        }
    }
}
