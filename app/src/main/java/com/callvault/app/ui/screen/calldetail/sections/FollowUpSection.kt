package com.callvault.app.ui.screen.calldetail.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.util.DateFormatter
import kotlinx.datetime.Instant

/**
 * Follow-up controls. Sprint 3 only persists to DB; Sprint 4 wires
 * `AlarmManager` reminders.
 */
@Composable
fun FollowUpSection(
    followUpAt: Instant?,
    onSet: () -> Unit,
    onClear: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "Follow-up",
            color = NeoColors.OnBaseMuted,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (followUpAt != null) {
            Text(
                text = DateFormatter.longDate(followUpAt),
                color = NeoColors.OnBase,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoButton(text = "Edit", onClick = onSet, variant = NeoButtonVariant.Secondary)
                NeoButton(text = "Snooze", onClick = onSnooze, variant = NeoButtonVariant.Tertiary)
                NeoButton(text = "Cancel", onClick = onClear, variant = NeoButtonVariant.Tertiary)
            }
        } else {
            NeoButton(
                text = "Set follow-up",
                onClick = onSet,
                variant = NeoButtonVariant.Primary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
