package com.callNest.app.ui.screen.calls

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.domain.model.Call
import com.callNest.app.domain.model.CallType
import com.callNest.app.domain.model.Tag
import com.callNest.app.ui.components.neo.LeadScoreBadge
import com.callNest.app.ui.components.neo.NeoAvatar
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.util.DateFormatter
import com.callNest.app.ui.util.DurationFormatter
import com.callNest.app.ui.util.PhoneNumberFormatter
import kotlinx.datetime.Instant

/**
 * Renders a single call row used by both the main Calls list and the
 * unsaved-pinned section.
 *
 * @param row presentation row carrying the underlying [Call] and resolved metadata
 * @param onClick fires when the user taps the row (navigates to detail)
 * @param onLongPress enters bulk-edit mode
 * @param onToggleBookmark fires when the trailing star is tapped
 * @param selected when true, renders a concave pressed style for selection
 */
@Composable
fun CallRowItem(
    row: CallRow,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val title = row.displayName?.takeIf { it.isNotBlank() }
        ?: PhoneNumberFormatter.pretty(row.call.normalizedNumber.ifBlank { row.call.rawNumber })
    val subtitleParts = listOfNotNull(
        DateFormatter.rowTime(row.call.date),
        DurationFormatter.short(row.call.durationSec).takeIf { row.call.durationSec > 0 }
    )

    NeoSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(onLongClick = onLongPress, onClick = onClick),
        elevation = if (selected) NeoElevation.ConcaveSmall else NeoElevation.ConvexSmall,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoAvatar(name = title, size = 36.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = typeIcon(row.call.type),
                        contentDescription = row.call.type.name,
                        tint = typeColor(row.call.type),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = SageColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitleParts.joinToString(" · "),
                        color = SageColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                    if (row.call.leadScore > 0) {
                        Spacer(Modifier.width(8.dp))
                        LeadScoreBadge(score = row.call.leadScore)
                    }
                }
                if (row.tags.isNotEmpty() || row.tagOverflowCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.tags.forEach { tag -> TagPill(tag) }
                        if (row.tagOverflowCount > 0) {
                            TagOverflow(count = row.tagOverflowCount)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .combinedClickable(onClick = onToggleBookmark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (row.call.isBookmarked) Icons.Filled.Star
                    else Icons.Filled.StarBorder,
                    contentDescription = if (row.call.isBookmarked) "Remove bookmark" else "Bookmark",
                    tint = if (row.call.isBookmarked) NeoColors.AccentAmber else NeoColors.OnBaseSubtle
                )
            }
        }
    }
}

@Composable
private fun TagPill(tag: Tag) {
    val color = runCatching { Color(android.graphics.Color.parseColor(tag.colorHex)) }
        .getOrDefault(NeoColors.AccentBlue)
    Box(
        modifier = Modifier
            .height(20.dp)
            .padding(end = 0.dp)
    ) {
        NeoSurface(
            elevation = NeoElevation.Flat,
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = (tag.emoji?.plus(" ") ?: "") + tag.name,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TagOverflow(count: Int) {
    NeoSurface(
        elevation = NeoElevation.Flat,
        shape = CircleShape,
        color = NeoColors.Inset
    ) {
        Text(
            text = "+$count",
            color = NeoColors.OnBaseMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun typeIcon(type: CallType): ImageVector = when (type) {
    CallType.INCOMING, CallType.ANSWERED_EXTERNALLY -> Icons.AutoMirrored.Filled.CallReceived
    CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
    CallType.MISSED, CallType.REJECTED, CallType.BLOCKED, CallType.VOICEMAIL -> Icons.AutoMirrored.Filled.CallMissed
    CallType.UNKNOWN -> Icons.AutoMirrored.Filled.CallReceived
}

private fun typeColor(type: CallType): Color = when (type) {
    CallType.INCOMING -> SageColors.Sage
    CallType.OUTGOING -> SageColors.SageMuted
    CallType.MISSED, CallType.REJECTED -> SageColors.StatusError
    else -> SageColors.TextTertiary
}

// ---------- Previews ----------

private fun previewCall(
    type: CallType,
    name: String?,
    bookmarked: Boolean = false
) = Call(
    systemId = 1,
    rawNumber = "+919876543210",
    normalizedNumber = "+919876543210",
    date = Instant.fromEpochMilliseconds(System.currentTimeMillis() - 600_000),
    durationSec = 84,
    type = type,
    cachedName = name,
    phoneAccountId = null,
    simSlot = 1,
    carrierName = null,
    geocodedLocation = null,
    countryIso = "IN",
    isNew = false,
    isBookmarked = bookmarked,
    bookmarkReason = null,
    followUpAt = null,
    followUpMinuteOfDay = null,
    followUpNote = null,
    followUpDoneAt = null,
    leadScore = 42,
    leadScoreManualOverride = null,
    isArchived = false,
    deletedAt = null,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0)
)

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun CallRowIncomingPreview() {
    CallNestTheme {
        CallRowItem(
            row = CallRow(
                call = previewCall(CallType.INCOMING, "Asha Kapoor", bookmarked = true),
                displayName = "Asha Kapoor",
                isUnsaved = false,
                tags = emptyList(),
                tagOverflowCount = 0
            ),
            onClick = {}, onLongPress = {}, onToggleBookmark = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun CallRowMissedUnsavedPreview() {
    CallNestTheme {
        CallRowItem(
            row = CallRow(
                call = previewCall(CallType.MISSED, null),
                displayName = null,
                isUnsaved = true,
                tags = emptyList(),
                tagOverflowCount = 0
            ),
            onClick = {}, onLongPress = {}, onToggleBookmark = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun CallRowWithTagsPreview() {
    CallNestTheme {
        CallRowItem(
            row = CallRow(
                call = previewCall(CallType.OUTGOING, "Vikram Iyer"),
                displayName = "Vikram Iyer",
                isUnsaved = false,
                tags = listOf(
                    Tag(1, "Hot lead", "#E5536B", "🔥", false, 0),
                    Tag(2, "Wholesale", "#4F7CFF", null, false, 1),
                    Tag(3, "Mumbai", "#1FB5A8", null, false, 2)
                ),
                tagOverflowCount = 2
            ),
            onClick = {}, onLongPress = {}, onToggleBookmark = {}
        )
    }
}
