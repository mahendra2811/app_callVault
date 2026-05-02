package com.callvault.app.ui.screen.calls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callvault.app.domain.model.CallType
import com.callvault.app.domain.model.FilterState
import com.callvault.app.domain.model.Tag
import com.callvault.app.domain.repository.CallRepository
import com.callvault.app.ui.components.neo.NeoBottomSheet
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoChip
import com.callvault.app.ui.theme.NeoColors
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Modal bottom sheet that lets the user compose a multi-axis filter against
 * the Calls list (spec §3.4). Each axis is rendered as chips for binary
 * choices (call types, SIM slots, saved/unsaved, bookmarked, follow-up).
 *
 * The Apply badge shows the live match count via [callRepo.observeFiltered]
 * collapsed to a `Flow<Int>`.
 *
 * @param initial seed filter (current applied filter)
 * @param availableTags tags fetched once for the include/exclude rows
 * @param callRepo used for the live preview count
 * @param onDismiss user closed the sheet without applying
 * @param onApply commits the draft filter to the calls list
 * @param onSavePreset persists the current draft as a named preset
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallsFilterSheet(
    initial: FilterState,
    availableTags: List<Tag>,
    callRepo: CallRepository,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
    onSavePreset: (FilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf(initial) }
    val previewCountFlow = remember(draft) {
        runCatching { callRepo.observeFiltered(draft).map { it.size } }
            .getOrElse { flowOf(0) }
    }
    val previewCount by previewCountFlow.collectAsState(initial = 0)

    NeoBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Filter calls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NeoColors.OnBase
            )
            Section("Type") {
                CallType.entries.filter { it != CallType.UNKNOWN }.forEach { t ->
                    NeoChip(
                        text = t.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = t.raw in draft.callTypes,
                        onClick = {
                            draft = draft.copy(
                                callTypes = if (t.raw in draft.callTypes)
                                    draft.callTypes - t.raw
                                else draft.callTypes + t.raw
                            )
                        }
                    )
                }
            }
            Section("Status") {
                NeoChip(
                    text = "Bookmarked",
                    selected = draft.onlyBookmarked,
                    onClick = { draft = draft.copy(onlyBookmarked = !draft.onlyBookmarked) }
                )
                NeoChip(
                    text = "With follow-up",
                    selected = draft.onlyWithFollowUp,
                    onClick = { draft = draft.copy(onlyWithFollowUp = !draft.onlyWithFollowUp) }
                )
                NeoChip(
                    text = "Unsaved only",
                    selected = draft.onlyUnsaved,
                    onClick = {
                        draft = draft.copy(
                            onlyUnsaved = !draft.onlyUnsaved,
                            onlySaved = false
                        )
                    }
                )
                NeoChip(
                    text = "Saved only",
                    selected = draft.onlySaved,
                    onClick = {
                        draft = draft.copy(
                            onlySaved = !draft.onlySaved,
                            onlyUnsaved = false
                        )
                    }
                )
            }
            Section("SIM") {
                listOf(1, 2).forEach { slot ->
                    NeoChip(
                        text = "SIM $slot",
                        selected = slot in draft.simSlots,
                        onClick = {
                            draft = draft.copy(
                                simSlots = if (slot in draft.simSlots) draft.simSlots - slot
                                else draft.simSlots + slot
                            )
                        }
                    )
                }
            }
            if (availableTags.isNotEmpty()) {
                Section("Tags include") {
                    availableTags.forEach { tag ->
                        NeoChip(
                            text = (tag.emoji?.plus(" ") ?: "") + tag.name,
                            selected = tag.id in draft.tagIds,
                            onClick = {
                                draft = draft.copy(
                                    tagIds = if (tag.id in draft.tagIds) draft.tagIds - tag.id
                                    else draft.tagIds + tag.id
                                )
                            }
                        )
                    }
                }
            }
            Section("Sort by") {
                FilterState.SortMode.entries.forEach { sort ->
                    NeoChip(
                        text = sortLabel(sort),
                        selected = sort == draft.sort,
                        onClick = { draft = draft.copy(sort = sort) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoButton(
                    text = "Reset",
                    onClick = { draft = FilterState() },
                    variant = NeoButtonVariant.Tertiary
                )
                Spacer(modifier = Modifier.weight(1f))
                NeoButton(
                    text = "Save preset",
                    onClick = { onSavePreset(draft) },
                    variant = NeoButtonVariant.Secondary
                )
                NeoButton(
                    text = "Apply ($previewCount)",
                    onClick = { onApply(draft) },
                    variant = NeoButtonVariant.Primary
                )
            }
        }
    }
}

private fun sortLabel(sort: FilterState.SortMode): String = when (sort) {
    FilterState.SortMode.DATE_DESC -> "Newest first"
    FilterState.SortMode.DATE_ASC -> "Oldest first"
    FilterState.SortMode.DURATION_DESC -> "Longest"
    FilterState.SortMode.LEAD_SCORE_DESC -> "Lead score"
    FilterState.SortMode.FREQUENCY_DESC -> "Most frequent"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = NeoColors.OnBaseMuted,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/** Removable chip row that displays which axes of a [FilterState] are active. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveFiltersRow(
    filter: FilterState,
    onClear: () -> Unit,
    onChange: (FilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = buildList {
        if (filter.callTypes.isNotEmpty()) add("Types: ${filter.callTypes.size}")
        if (filter.onlyBookmarked) add("Bookmarked")
        if (filter.onlyWithFollowUp) add("Follow-up")
        if (filter.onlyUnsaved) add("Unsaved")
        if (filter.onlySaved) add("Saved")
        if (filter.tagIds.isNotEmpty()) add("Tags: ${filter.tagIds.size}")
        if (filter.minDurationSec != null) add("≥${filter.minDurationSec}s")
        if (filter.minLeadScore != null) add("Score ≥${filter.minLeadScore}")
    }
    if (items.isEmpty()) return
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { label ->
            NeoChip(text = label, selected = true, onClick = { /* no-op */ })
        }
        NeoChip(text = "Clear", selected = false, onClick = onClear)
    }
}
