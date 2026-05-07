package com.callNest.app.ui.screen.bookmarks

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.ui.components.neo.NeoEmptyState
import com.callNest.app.ui.components.neo.NeoIconButton
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.components.neo.NeoTopBar
import com.callNest.app.ui.screen.shared.NeoScaffold
import com.callNest.app.ui.screen.shared.StandardPage
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.util.DateFormatter
import com.callNest.app.ui.util.PhoneNumberFormatter
import androidx.compose.runtime.getValue
import android.widget.Toast

/**
 * Full-screen list of bookmarked calls with a Pinned section the user can
 * reorder via up/down chevrons (max 5).
 */
@Composable
fun BookmarksScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        viewModel.consumeError()
    }

    StandardPage(
        title = stringResource(R.string.cv_bookmarks_title),
        description = stringResource(R.string.cv_bookmarks_description),
        emoji = "⭐",
        onBack = onBack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.pinned.isEmpty() && state.others.isEmpty()) {
                NeoEmptyState(
                    icon = Icons.Filled.Bookmark,
                    title = stringResource(R.string.bookmarks_empty_title),
                    message = stringResource(R.string.bookmarks_empty_body)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
                ) {
                    if (state.pinned.isNotEmpty()) {
                        item("pinned-hdr") {
                            Text(
                                stringResource(R.string.bookmarks_pinned_section),
                                color = SageColors.TextSecondary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(state.pinned, key = { "pin-" + it.call.normalizedNumber }) { row ->
                            BookmarkRowItem(
                                row = row,
                                onClick = { onOpenDetail(row.call.normalizedNumber) },
                                onTogglePin = { viewModel.togglePin(row.call.normalizedNumber) },
                                onMoveUp = { viewModel.moveUp(row.call.normalizedNumber) },
                                onMoveDown = { viewModel.moveDown(row.call.normalizedNumber) }
                            )
                        }
                        item("pinned-spacer") { Spacer(Modifier.height(8.dp)) }
                    }
                    if (state.others.isNotEmpty()) {
                        items(state.others, key = { "row-" + it.call.systemId }) { row ->
                            BookmarkRowItem(
                                row = row,
                                onClick = { onOpenDetail(row.call.normalizedNumber) },
                                onTogglePin = { viewModel.togglePin(row.call.normalizedNumber) },
                                onMoveUp = null,
                                onMoveDown = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkRowItem(
    row: BookmarkRow,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    NeoSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = NeoElevation.ConvexSmall,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.displayName ?: PhoneNumberFormatter.pretty(row.call.normalizedNumber),
                    color = SageColors.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                val sub = buildString {
                    append(DateFormatter.rowTime(row.call.date))
                    val reason = row.call.bookmarkReason
                    if (!reason.isNullOrBlank()) {
                        append(" · ")
                        append(reason)
                    }
                }
                Text(
                    text = sub,
                    color = SageColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (row.pinned && onMoveUp != null && onMoveDown != null) {
                NeoIconButton(
                    icon = Icons.Filled.ArrowUpward,
                    onClick = onMoveUp,
                    contentDescription = stringResource(R.string.bookmarks_action_move_up),
                    size = 32.dp
                )
                Spacer(Modifier.width(4.dp))
                NeoIconButton(
                    icon = Icons.Filled.ArrowDownward,
                    onClick = onMoveDown,
                    contentDescription = stringResource(R.string.bookmarks_action_move_down),
                    size = 32.dp
                )
                Spacer(Modifier.width(4.dp))
            }
            NeoIconButton(
                icon = if (row.pinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                onClick = onTogglePin,
                contentDescription = if (row.pinned)
                    stringResource(R.string.bookmarks_action_unpin)
                else stringResource(R.string.bookmarks_action_pin),
                size = 36.dp
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun BookmarksEmptyPreview() {
    CallNestTheme {
        NeoEmptyState(
            icon = Icons.Filled.Bookmark,
            title = "Nothing pinned yet",
            message = "Tap the star on any call to bookmark it."
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun BookmarksPopulatedPreview() {
    CallNestTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Pinned",
                color = SageColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            // Visual stand-in is omitted — full preview needs domain types.
        }
    }
}
