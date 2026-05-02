package com.callvault.app.ui.screen.calls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.domain.model.CallType
import com.callvault.app.ui.components.neo.NeoBadge
import com.callvault.app.ui.components.neo.NeoIconButton
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import kotlinx.datetime.Instant

/**
 * Collapsible "Unsaved inquiries — last 7 days" section.
 *
 * Header shows the section title, a count badge, and a hide-section button
 * that disables the pinned section persistently via [onHideSection].
 *
 * @param rows pre-resolved [CallRow]s; expected to be in date-desc order
 * @param onRowClick navigate to call detail
 * @param onLongPressRow enter bulk-edit mode
 * @param onToggleBookmark trailing star tap
 * @param onHideSection user dismissed the section permanently
 */
@Composable
fun UnsavedPinnedSection(
    rows: List<CallRow>,
    onRowClick: (CallRow) -> Unit,
    onLongPressRow: (CallRow) -> Unit,
    onToggleBookmark: (CallRow) -> Unit,
    onHideSection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Unsaved inquiries — last 7 days",
                color = NeoColors.OnBase,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (rows.isNotEmpty()) {
                NeoBadge(count = rows.size)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = NeoColors.OnBaseMuted
            )
            Spacer(modifier = Modifier.width(4.dp))
            NeoIconButton(
                icon = Icons.Filled.VisibilityOff,
                onClick = onHideSection,
                contentDescription = "Hide section",
                size = 36.dp
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                rows.forEach { row ->
                    CallRowItem(
                        row = row,
                        onClick = { onRowClick(row) },
                        onLongPress = { onLongPressRow(row) },
                        onToggleBookmark = { onToggleBookmark(row) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun UnsavedPinnedSectionPreview() {
    CallVaultTheme {
        val sample = remember {
            listOf(
                CallRow(
                    call = previewCallForSection(CallType.MISSED),
                    displayName = null,
                    isUnsaved = true,
                    tags = emptyList(),
                    tagOverflowCount = 0
                ),
                CallRow(
                    call = previewCallForSection(CallType.INCOMING),
                    displayName = null,
                    isUnsaved = true,
                    tags = emptyList(),
                    tagOverflowCount = 0
                )
            )
        }
        UnsavedPinnedSection(
            rows = sample,
            onRowClick = {}, onLongPressRow = {}, onToggleBookmark = {}, onHideSection = {}
        )
    }
}

private fun previewCallForSection(type: CallType) =
    com.callvault.app.domain.model.Call(
        systemId = type.raw.toLong(),
        rawNumber = "+919812345678",
        normalizedNumber = "+919812345678",
        date = Instant.fromEpochMilliseconds(System.currentTimeMillis() - 3_600_000),
        durationSec = 0,
        type = type,
        cachedName = null,
        phoneAccountId = null,
        simSlot = null,
        carrierName = null,
        geocodedLocation = null,
        countryIso = "IN",
        isNew = true,
        isBookmarked = false,
        bookmarkReason = null,
        followUpAt = null,
        followUpMinuteOfDay = null,
        followUpNote = null,
        followUpDoneAt = null,
        leadScore = 0,
        leadScoreManualOverride = null,
        isArchived = false,
        deletedAt = null,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0)
    )
