package com.callvault.app.ui.screen.calldetail.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.components.neo.NeoAvatar
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.ui.theme.NeoElevation
import com.callvault.app.ui.util.PhoneNumberFormatter

/** Save status — drives the colored status pill on [HeroCard]. */
enum class SaveStatus { Saved, Unsaved, AutoSaved }

/**
 * Top hero card: avatar, name/number, save-status pill, lead-score badge,
 * and an optional "Save to contacts" CTA when the contact is unsaved.
 */
@Composable
fun HeroCard(
    displayName: String?,
    normalizedNumber: String,
    saveStatus: SaveStatus,
    leadScore: Int,
    onSaveContact: () -> Unit,
    summary: String? = null,
    modifier: Modifier = Modifier
) {
    NeoSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = NeoElevation.ConvexLarge,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeoAvatar(
                    name = displayName ?: normalizedNumber,
                    size = 56.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName?.takeIf { it.isNotBlank() }
                            ?: PhoneNumberFormatter.pretty(normalizedNumber),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = SageColors.TextPrimary
                    )
                    if (displayName != null) {
                        Text(
                            text = PhoneNumberFormatter.pretty(normalizedNumber),
                            color = SageColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                LeadScoreBadge(leadScore)
            }
            if (!summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = summary,
                    color = SageColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(saveStatus)
                Spacer(modifier = Modifier.weight(1f))
                if (saveStatus != SaveStatus.Saved) {
                    NeoButton(
                        text = "Save to contacts",
                        onClick = onSaveContact,
                        variant = NeoButtonVariant.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: SaveStatus) {
    val (label, color) = when (status) {
        SaveStatus.Saved -> "Saved" to NeoColors.AccentGreen
        SaveStatus.AutoSaved -> "Auto-saved" to NeoColors.AccentBlue
        SaveStatus.Unsaved -> "Unsaved" to NeoColors.AccentAmber
    }
    NeoSurface(
        elevation = NeoElevation.Flat,
        shape = CircleShape,
        color = color.copy(alpha = 0.18f)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun LeadScoreBadge(score: Int) {
    val color: Color = when {
        score < 30 -> SageColors.TextSecondary
        score < 70 -> NeoColors.AccentAmber
        else -> NeoColors.AccentRose
    }
    NeoSurface(
        elevation = NeoElevation.ConvexSmall,
        shape = CircleShape,
        color = color.copy(alpha = 0.18f)
    ) {
        Text(
            text = "$score",
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun HeroCardPreview() {
    CallVaultTheme {
        HeroCard(
            displayName = "Asha Kapoor",
            normalizedNumber = "+919876543210",
            saveStatus = SaveStatus.Unsaved,
            leadScore = 78,
            onSaveContact = {}
        )
    }
}
