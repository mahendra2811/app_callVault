package com.callvault.app.ui.screen.calldetail.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callvault.app.domain.model.Call
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.util.DateFormatter
import com.callvault.app.ui.util.DurationFormatter

/** Chronological history of calls for the current number. */
@Composable
fun HistoryTimeline(history: List<Call>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "History",
            color = NeoColors.OnBaseMuted,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Column(
            modifier = Modifier.padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            history.forEach { c ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        DateFormatter.rowTime(c.date),
                        color = NeoColors.OnBase,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        c.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = NeoColors.OnBaseMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "  ${DurationFormatter.short(c.durationSec)}",
                        color = NeoColors.OnBaseMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
