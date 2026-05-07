package com.callvault.app.ui.screen.pipeline

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.hilt.navigation.compose.hiltViewModel as hiltVm
import com.callvault.app.ui.screen.templates.TemplatesViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.callvault.app.R
import com.callvault.app.domain.model.PipelineStage
/** Five-column Kanban for the sales funnel. Tap a card → bottom sheet to move stage. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineScreen(
    onBack: (() -> Unit)? = null,
    onCardOpenCallDetail: (normalizedNumber: String) -> Unit,
    viewModel: PipelineViewModel = hiltViewModel(),
) {
    val board by viewModel.board.collectAsStateWithLifecycle()
    val demoActive by viewModel.demoActive.collectAsStateWithLifecycle()
    var moveTarget by remember { mutableStateOf<PipelineCard?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedNumbers by remember { mutableStateOf(setOf<String>()) }
    var pickerOpen by remember { mutableStateOf(false) }
    var blastTemplate by remember { mutableStateOf<com.callvault.app.domain.model.MessageTemplate?>(null) }
    var overdueDialogOpen by remember { mutableStateOf(false) }
    var dragInProgress by remember { mutableStateOf(false) }
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val overdueDoneFmt = stringResource(R.string.pipeline_overdue_done_fmt)
    val overdueNoneMsg = stringResource(R.string.pipeline_overdue_none)
    val templatesVm: TemplatesViewModel = hiltVm()
    val templates by templatesVm.templates.collectAsStateWithLifecycle()
    val allCards = remember(board) { board.byStage.values.flatten() }
    val selectedCards = remember(allCards, selectedNumbers) {
        allCards.filter { it.normalizedNumber in selectedNumbers }
    }

    androidx.activity.compose.BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedNumbers = emptySet<String>()
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.pipeline_selected_count_fmt, selectedNumbers.size))
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedNumbers = emptySet<String>()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.pipeline_clear_selection_cd),
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { pickerOpen = true },
                            enabled = selectedNumbers.isNotEmpty(),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = stringResource(R.string.pipeline_bulk_send_cd),
                            )
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.pipeline_screen_title)) },
                    navigationIcon = if (onBack != null) ({
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.pipeline_back_cd),
                            )
                        }
                    }) else ({}),
                    actions = {
                        IconButton(onClick = { overdueDialogOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = stringResource(R.string.pipeline_overdue_to_lost_cd),
                            )
                        }
                        IconButton(
                            onClick = { selectionMode = true },
                            enabled = !dragInProgress,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlaylistAddCheck,
                                contentDescription = stringResource(R.string.pipeline_bulk_send_cd),
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (demoActive) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.demo_banner_title), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.demo_banner_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        TextButton(onClick = { viewModel.clearDemo() }) {
                            Text(stringResource(R.string.demo_banner_clear))
                        }
                    }
                }
            }
        if (allCards.isEmpty()) {
            com.callvault.app.ui.components.EmptyState(
                emoji = "🎯",
                title = stringResource(R.string.pipeline_empty_title),
                body = stringResource(R.string.pipeline_empty_body),
            )
            return@Column
        }
        val widthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
        val compact = widthDp < 600
        val onCardTap: (PipelineCard) -> Unit = { card ->
            if (selectionMode) selectedNumbers = selectedNumbers.toggle(card.normalizedNumber)
            else onCardOpenCallDetail(card.normalizedNumber)
        }
        val onCardLongPress: (PipelineCard) -> Unit = { card ->
            if (selectionMode) selectedNumbers = selectedNumbers.toggle(card.normalizedNumber)
            else moveTarget = card
        }
        if (compact) {
            CompactPipeline(
                board = board,
                selectedNumbers = selectedNumbers,
                selectionMode = selectionMode,
                onCardTap = onCardTap,
                onCardLongPress = onCardLongPress,
                onDropToStage = { stage, number -> viewModel.moveTo(number, stage); dragInProgress = false },
                onDragStarted = { dragInProgress = true },
                onDragEnded = { dragInProgress = false },
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(PipelineStage.entries.toList(), key = { it.name }) { stage ->
                    StageColumn(
                        stage = stage,
                        cards = board.byStage[stage].orEmpty(),
                        selectedNumbers = selectedNumbers,
                        selectionMode = selectionMode,
                        onCardTap = onCardTap,
                        onCardLongPress = onCardLongPress,
                        onDropToStage = { number -> viewModel.moveTo(number, stage); dragInProgress = false },
                        onDragStarted = { dragInProgress = true },
                        onDragEnded = { dragInProgress = false },
                    )
                }
            }
        }
        }
    }

    moveTarget?.let { card ->
        StagePickerSheet(
            current = card.stage,
            onPick = { stage ->
                viewModel.moveTo(card.normalizedNumber, stage)
                moveTarget = null
            },
            onDismiss = { moveTarget = null },
        )
    }

    if (pickerOpen) {
        BulkBlastTemplatePickerSheet(
            templates = templates,
            onPick = { tpl ->
                blastTemplate = tpl
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false },
        )
    }

    blastTemplate?.let { tpl ->
        BulkBlastProgressSheet(
            targets = selectedCards,
            template = tpl,
            onDismiss = {
                blastTemplate = null
                selectionMode = false
                selectedNumbers = emptySet<String>()
            },
        )
    }

    if (overdueDialogOpen) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { overdueDialogOpen = false },
            title = { Text(stringResource(R.string.pipeline_overdue_dialog_title)) },
            text = { Text(stringResource(R.string.pipeline_overdue_dialog_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    overdueDialogOpen = false
                    scope.launch {
                        val n = viewModel.moveOverdueToLost()
                        snackbar.showSnackbar(
                            if (n == 0) overdueNoneMsg else String.format(overdueDoneFmt, n)
                        )
                    }
                }) { Text(stringResource(R.string.pipeline_stage_lost)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { overdueDialogOpen = false }) {
                    Text(stringResource(R.string.bulk_blast_close))
                }
            },
        )
    }
}

private fun Set<String>.toggle(item: String): Set<String> =
    if (item in this) this - item else this + item

/** Vertical sectioned Kanban for narrow screens. Each stage is collapsible. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactPipeline(
    board: PipelineBoard,
    selectedNumbers: Set<String>,
    selectionMode: Boolean,
    onCardTap: (PipelineCard) -> Unit,
    onCardLongPress: (PipelineCard) -> Unit,
    onDropToStage: (PipelineStage, String) -> Unit,
    onDragStarted: () -> Unit,
    onDragEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var collapsed by remember {
        mutableStateOf(setOf(PipelineStage.Lost))
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(PipelineStage.entries.toList(), key = { it.name }) { stage ->
            CompactStageSection(
                stage = stage,
                cards = board.byStage[stage].orEmpty(),
                isCollapsed = stage in collapsed,
                onToggleCollapse = {
                    collapsed = if (stage in collapsed) collapsed - stage else collapsed + stage
                },
                selectedNumbers = selectedNumbers,
                selectionMode = selectionMode,
                onCardTap = onCardTap,
                onCardLongPress = onCardLongPress,
                onDropHere = { number -> onDropToStage(stage, number) },
                onDragStarted = onDragStarted,
                onDragEnded = onDragEnded,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactStageSection(
    stage: PipelineStage,
    cards: List<PipelineCard>,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    selectedNumbers: Set<String>,
    selectionMode: Boolean,
    onCardTap: (PipelineCard) -> Unit,
    onCardLongPress: (PipelineCard) -> Unit,
    onDropHere: (String) -> Unit,
    onDragStarted: () -> Unit,
    onDragEnded: () -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    val target = remember(stage) {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) { onDragStarted() }
            override fun onEntered(event: DragAndDropEvent) { hovered = true }
            override fun onExited(event: DragAndDropEvent) { hovered = false }
            override fun onEnded(event: DragAndDropEvent) {
                hovered = false
                onDragEnded()
            }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                hovered = false
                val text = runCatching {
                    event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                }.getOrNull() ?: return false
                if (text.isBlank()) return false
                onDropHere(text)
                return true
            }
        }
    }
    val container = if (hovered) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = container, shape = RoundedCornerShape(12.dp))
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.toAndroidDragEvent().clipDescription
                        ?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
                },
                target = target,
            )
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleCollapse).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stage.label(), style = MaterialTheme.typography.titleMedium)
            Text("${cards.size}", style = MaterialTheme.typography.bodyMedium)
        }
        if (!isCollapsed && cards.isNotEmpty()) {
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                cards.forEach { card ->
                    LeadCard(
                        card = card,
                        selected = card.normalizedNumber in selectedNumbers,
                        draggable = !selectionMode,
                        onClick = { onCardTap(card) },
                        onLongClick = { onCardLongPress(card) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StageColumn(
    stage: PipelineStage,
    cards: List<PipelineCard>,
    selectedNumbers: Set<String>,
    selectionMode: Boolean,
    onCardTap: (PipelineCard) -> Unit,
    onCardLongPress: (PipelineCard) -> Unit,
    onDropToStage: (normalizedNumber: String) -> Unit,
    onDragStarted: () -> Unit = {},
    onDragEnded: () -> Unit = {},
) {
    var hovered by remember { mutableStateOf(false) }
    val target = remember(stage) {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) { onDragStarted() }
            override fun onEntered(event: DragAndDropEvent) { hovered = true }
            override fun onExited(event: DragAndDropEvent) { hovered = false }
            override fun onEnded(event: DragAndDropEvent) {
                hovered = false
                onDragEnded()
            }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                hovered = false
                val text = runCatching {
                    event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()
                }.getOrNull() ?: return false
                if (text.isBlank()) return false
                onDropToStage(text)
                return true
            }
        }
    }
    val container = if (hovered) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(color = container, shape = RoundedCornerShape(12.dp))
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.toAndroidDragEvent().clipDescription
                        ?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true
                },
                target = target,
            )
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stage.label(), style = MaterialTheme.typography.titleMedium)
            Text(cards.size.toString(), style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider()
        if (cards.isEmpty()) {
            Text(
                stringResource(R.string.pipeline_empty_column),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cards, key = { it.normalizedNumber }) { card ->
                    LeadCard(
                        card = card,
                        selected = card.normalizedNumber in selectedNumbers,
                        draggable = !selectionMode,
                        onClick = { onCardTap(card) },
                        onLongClick = { onCardLongPress(card) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LeadCard(
    card: PipelineCard,
    selected: Boolean,
    draggable: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.primary
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val cardModifier = Modifier
        .fillMaxWidth()
        .let { mod ->
            // In selection mode (non-draggable), keep long-press for selection toggle.
            // When draggable, the long-press is reserved for starting the drag transfer.
            if (draggable) mod.clickable(onClick = onClick)
            else mod.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        }
        .let { mod ->
            if (!draggable) mod else mod.dragAndDropSource(
                drawDragDecoration = {
                    drawRoundRect(
                        color = outline,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                        style = Stroke(width = 4f),
                    )
                },
            ) {
                detectTapGestures(onLongPress = {
                    haptics.performHapticFeedback(
                        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                    )
                    startTransfer(
                        DragAndDropTransferData(
                            clipData = ClipData.newPlainText("cv_pipeline_number", card.normalizedNumber),
                        )
                    )
                })
            }
        }
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                card.displayName ?: card.normalizedNumber,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (card.displayName != null) {
                Text(
                    card.normalizedNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.pipeline_card_calls_fmt, card.totalCalls),
                    style = MaterialTheme.typography.bodySmall,
                )
                ScoreBadge(score = card.leadScore)
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    val color = when {
        score >= 70 -> MaterialTheme.colorScheme.primary
        score >= 40 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(
            score.toString(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StagePickerSheet(
    current: PipelineStage,
    onPick: (PipelineStage) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.pipeline_move_to),
                style = MaterialTheme.typography.titleMedium,
            )
            Column(modifier = Modifier.padding(top = 8.dp)) {
                PipelineStage.entries.forEach { stage ->
                    val selected = stage == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !selected) { onPick(stage) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stage.label(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PipelineStage.label(): String = stringResource(
    when (this) {
        PipelineStage.New -> R.string.pipeline_stage_new
        PipelineStage.Contacted -> R.string.pipeline_stage_contacted
        PipelineStage.Qualified -> R.string.pipeline_stage_qualified
        PipelineStage.Won -> R.string.pipeline_stage_won
        PipelineStage.Lost -> R.string.pipeline_stage_lost
    }
)
