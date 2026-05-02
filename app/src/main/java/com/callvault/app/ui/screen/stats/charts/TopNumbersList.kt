package com.callvault.app.ui.screen.stats.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.domain.model.LeaderboardEntry
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoChip
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.util.DurationFormatter

/**
 * Top-numbers leaderboard with a By count / By duration toggle.
 *
 * The entries arrive sorted by [LeaderboardEntry.callCount]; when
 * [sortByDuration] is true we re-sort client-side instead of refetching.
 */
@Composable
fun TopNumbersList(
    entries: List<LeaderboardEntry>,
    sortByDuration: Boolean,
    onToggleSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sorted = if (sortByDuration) entries.sortedByDescending { it.totalDurationSec }
    else entries
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeoChip(
                text = "By count",
                selected = !sortByDuration,
                onClick = { if (sortByDuration) onToggleSort() }
            )
            NeoChip(
                text = "By duration",
                selected = sortByDuration,
                onClick = { if (!sortByDuration) onToggleSort() }
            )
        }
        Spacer(Modifier.height(8.dp))
        if (sorted.isEmpty()) {
            Text(
                text = "No numbers in this range yet.",
                color = NeoColors.OnBaseMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            sorted.forEachIndexed { i, e ->
                NeoCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${i + 1}",
                            color = NeoColors.OnBaseMuted,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = e.displayName ?: e.normalizedNumber,
                            color = NeoColors.OnBase,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (sortByDuration)
                                DurationFormatter.short(e.totalDurationSec.toInt())
                            else "${e.callCount} calls",
                            color = NeoColors.AccentBlue,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360)
@Composable
private fun TopNumbersListPreview() {
    val rows = listOf(
        LeaderboardEntry("+919876500001", "Alice", 24, 3600),
        LeaderboardEntry("+919876500002", "Bob", 19, 2200),
        LeaderboardEntry("+919876500003", null, 11, 1500)
    )
    CallVaultTheme {
        TopNumbersList(entries = rows, sortByDuration = false, onToggleSort = {})
    }
}
