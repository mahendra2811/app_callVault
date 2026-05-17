package com.callNest.app.ui.screen.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.data.system.ContactsReader
import com.callNest.app.domain.model.Call
import com.callNest.app.domain.model.Note
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.util.PhoneNumberFormatter

/**
 * macOS Spotlight-style search overlay.
 *
 * - Centered floating card with auto-focused single-line input
 * - Live results in three sections (Contacts · Calls · Notes), debounced 200ms
 * - Dim scrim behind the card; tap outside or hit back to dismiss
 * - X icon clears the query; X with empty query closes the screen
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    BackHandler {
        viewModel.saveToHistory()
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                viewModel.saveToHistory()
                onBack()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search card — consumes its own clicks so taps inside don't dismiss.
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 24.dp, shape = RoundedCornerShape(20.dp))
                        .background(Color.White, shape = RoundedCornerShape(20.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { /* swallow */ }
                ) {
                    Column {
                        // Input row.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = SageColors.TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = state.query,
                                onValueChange = viewModel::setQuery,
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 22.sp,
                                    color = SageColors.TextPrimary,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(NeoColors.AccentBlue),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    viewModel.saveToHistory()
                                    keyboard?.hide()
                                }),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                decorationBox = { inner ->
                                    if (state.query.isEmpty()) {
                                        Text(
                                            text = "Search calls, contacts, notes…",
                                            color = SageColors.TextTertiary,
                                            fontSize = 22.sp
                                        )
                                    }
                                    inner()
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        if (state.query.isEmpty()) {
                                            viewModel.saveToHistory()
                                            onBack()
                                        } else {
                                            viewModel.clearQuery()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close search",
                                    tint = SageColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Results / recents.
                        if (state.query.isBlank()) {
                            RecentsBlock(
                                recent = state.recent,
                                onSelect = viewModel::selectRecent,
                                onClear = viewModel::clearHistory
                            )
                        } else {
                            ResultsBlock(
                                state = state,
                                onOpenDetail = { number ->
                                    viewModel.saveToHistory()
                                    onOpenDetail(number)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            // Hint pill — visible only when the card is empty + idle.
            if (state.query.isBlank() && state.recent.isEmpty()) {
                Text(
                    text = "Tip: type a name, number, or note keyword",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RecentsBlock(
    recent: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    if (recent.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = SageColors.TextSecondary,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelMedium,
                color = NeoColors.AccentBlue,
                modifier = Modifier
                    .clickable(onClick = onClear)
                    .padding(8.dp)
            )
        }
        recent.take(8).forEach { q ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(q) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = SageColors.TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = q,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SageColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ResultsBlock(
    state: SearchUiState,
    onOpenDetail: (String) -> Unit
) {
    if (state.isEmpty) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No matches for “${state.query}”",
                style = MaterialTheme.typography.bodyMedium,
                color = SageColors.TextSecondary
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .padding(bottom = 8.dp)
    ) {
        if (state.contacts.isNotEmpty()) {
            item("h-contacts") { SectionHeader("Contacts", state.contacts.size) }
            items(state.contacts, key = { "c|${it.normalizedNumber}|${it.displayName}" }) { c ->
                ResultRow(
                    icon = Icons.Filled.Person,
                    title = c.displayName,
                    subtitle = PhoneNumberFormatter.pretty(c.normalizedNumber),
                    onClick = { onOpenDetail(c.normalizedNumber) }
                )
            }
        }
        if (state.calls.isNotEmpty()) {
            item("h-calls") { SectionHeader("Calls", state.calls.size) }
            items(state.calls, key = { "call|${it.systemId}" }) { call ->
                ResultRow(
                    icon = Icons.Filled.Call,
                    title = call.cachedName?.takeIf { it.isNotBlank() }
                        ?: PhoneNumberFormatter.pretty(call.normalizedNumber),
                    subtitle = "${call.type.name.lowercase()} · ${PhoneNumberFormatter.pretty(call.normalizedNumber)}",
                    onClick = { onOpenDetail(call.normalizedNumber) }
                )
            }
        }
        if (state.notes.isNotEmpty()) {
            item("h-notes") { SectionHeader("Notes", state.notes.size) }
            items(state.notes, key = { "n|${it.id}" }) { note ->
                ResultRow(
                    icon = Icons.Filled.Note,
                    title = note.content.take(60),
                    subtitle = note.normalizedNumber?.let { PhoneNumberFormatter.pretty(it) } ?: "Note",
                    onClick = { note.normalizedNumber?.let(onOpenDetail) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = SageColors.TextTertiary
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = SageColors.TextTertiary
        )
    }
}

@Composable
private fun ResultRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(SageColors.SurfaceAlt, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeoColors.AccentBlue,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = SageColors.TextPrimary,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SageColors.TextSecondary,
                maxLines = 1
            )
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge,
            color = SageColors.TextTertiary
        )
    }
}

@Suppress("unused") private fun unusedCallRef(): Call? = null
@Suppress("unused") private fun unusedNoteRef(): Note? = null
@Suppress("unused") private fun unusedContactRef(): ContactsReader.ContactMatch? = null
