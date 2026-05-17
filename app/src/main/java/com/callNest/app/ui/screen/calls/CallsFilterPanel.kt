package com.callNest.app.ui.screen.calls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.callNest.app.domain.model.CallType
import com.callNest.app.domain.model.FilterState
import com.callNest.app.domain.model.Tag
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoButtonVariant
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.theme.SageColors

/**
 * Inline horizontal filter panel — replaces the old modal bottom sheet. The
 * bottom-sheet implementation was unreliable on some Samsung builds (state
 * race with [PullToRefreshBox], rare "doesn't open" reports). This panel
 * lives in the normal compose tree, animates with [AnimatedVisibility], and
 * has no modal stack to fight with.
 *
 * Layout: each filter axis is a horizontally-scrollable chip row. Apply +
 * Reset live in a sticky row at the bottom of the panel.
 */
@Composable
fun CallsFilterPanel(
    visible: Boolean,
    initial: FilterState,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
    viewModel: FilterSheetHostViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsState()
    // Reset the draft each time the panel re-opens so the user starts from
    // the currently-applied filter, not their last canceled draft.
    var draft by remember(visible, initial) { mutableStateOf(initial) }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
    ) {
        NeoSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            elevation = NeoElevation.ConvexSmall,
            shape = RoundedCornerShape(18.dp),
            color = SageColors.SurfaceAlt
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterAxis(
                    title = "Type",
                    chips = CallType.entries.filter { it != CallType.UNKNOWN }.map { t ->
                        ChipSpec(
                            label = t.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = t.raw in draft.callTypes,
                            onTap = {
                                draft = draft.copy(
                                    callTypes = if (t.raw in draft.callTypes) draft.callTypes - t.raw
                                    else draft.callTypes + t.raw
                                )
                            }
                        )
                    }
                )
                FilterAxis(
                    title = "Status",
                    chips = listOf(
                        ChipSpec("Bookmarked", draft.onlyBookmarked) {
                            draft = draft.copy(onlyBookmarked = !draft.onlyBookmarked)
                        },
                        ChipSpec("Follow-up", draft.onlyWithFollowUp) {
                            draft = draft.copy(onlyWithFollowUp = !draft.onlyWithFollowUp)
                        },
                        ChipSpec("Unsaved", draft.onlyUnsaved) {
                            draft = draft.copy(onlyUnsaved = !draft.onlyUnsaved, onlySaved = false)
                        },
                        ChipSpec("Saved", draft.onlySaved) {
                            draft = draft.copy(onlySaved = !draft.onlySaved, onlyUnsaved = false)
                        }
                    )
                )
                FilterAxis(
                    title = "SIM",
                    chips = listOf(1, 2).map { slot ->
                        ChipSpec("SIM $slot", slot in draft.simSlots) {
                            draft = draft.copy(
                                simSlots = if (slot in draft.simSlots) draft.simSlots - slot
                                else draft.simSlots + slot
                            )
                        }
                    }
                )
                if (tags.isNotEmpty()) {
                    FilterAxis(
                        title = "Tags",
                        chips = tags.map { tag ->
                            ChipSpec(
                                label = (tag.emoji?.plus(" ") ?: "") + tag.name,
                                selected = tag.id in draft.tagIds,
                                onTap = {
                                    draft = draft.copy(
                                        tagIds = if (tag.id in draft.tagIds) draft.tagIds - tag.id
                                        else draft.tagIds + tag.id
                                    )
                                }
                            )
                        }
                    )
                }
                FilterAxis(
                    title = "Sort by",
                    chips = FilterState.SortMode.entries.map { sort ->
                        ChipSpec(sortLabel(sort), sort == draft.sort) {
                            draft = draft.copy(sort = sort)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeoButton(
                        text = "Reset",
                        onClick = { draft = FilterState() },
                        variant = NeoButtonVariant.Tertiary
                    )
                    Spacer(Modifier.weight(1f))
                    NeoButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = NeoButtonVariant.Secondary
                    )
                    NeoButton(
                        text = "Apply",
                        onClick = { onApply(draft) },
                        variant = NeoButtonVariant.Primary
                    )
                }
            }
        }
    }
}

private data class ChipSpec(
    val label: String,
    val selected: Boolean,
    val onTap: () -> Unit
)

@Composable
private fun FilterAxis(title: String, chips: List<ChipSpec>) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = SageColors.TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips.forEach { c ->
                PanelChip(label = c.label, selected = c.selected, onTap = c.onTap)
            }
        }
    }
}

@Composable
private fun PanelChip(label: String, selected: Boolean, onTap: () -> Unit) {
    val bg = if (selected) NeoColors.AccentBlue else SageColors.Canvas
    val fg = if (selected) Color.White else SageColors.TextPrimary
    NeoSurface(
        modifier = Modifier
            .height(34.dp)
            .clickable(onClick = onTap),
        elevation = NeoElevation.Flat,
        shape = RoundedCornerShape(17.dp),
        color = bg
    ) {
        // Box must fillMaxSize so Alignment.Center actually centers against
        // the 34dp pill height. With a wrap-content Box the text rode high
        // because the chip used baseline alignment for the line box, not
        // the visual centre.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = fg,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun sortLabel(sort: FilterState.SortMode): String = when (sort) {
    FilterState.SortMode.DATE_DESC -> "Newest"
    FilterState.SortMode.DATE_ASC -> "Oldest"
    FilterState.SortMode.DURATION_DESC -> "Longest"
    FilterState.SortMode.LEAD_SCORE_DESC -> "Score"
    FilterState.SortMode.FREQUENCY_DESC -> "Frequent"
}

@Suppress("unused") private fun unusedTagsRef(): Tag? = null
