package com.callNest.app.ui.screen.inquiries

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.domain.model.ContactMeta
import com.callNest.app.ui.components.neo.NeoAvatar
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoEmptyState
import com.callNest.app.ui.components.neo.NeoSearchBar
import com.callNest.app.ui.components.neo.NeoTextField
import com.callNest.app.ui.components.neo.NeoPageHeader
import com.callNest.app.ui.components.neo.NeoTopBar
import com.callNest.app.ui.screen.shared.NeoScaffold
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.util.DateFormatter
import com.callNest.app.ui.util.PhoneNumberFormatter
import kotlinx.datetime.Instant

/**
 * Sprint 5 — Inquiries (auto-saved bucket).
 *
 * Long-press a row to enter bulk-select mode; the action bar then exposes
 * "Bulk save" (re-runs auto-save against the current call snapshot) and
 * "Convert all" (promotes every selected row to My Contacts).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InquiriesScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InquiriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val bulkProgress by viewModel.bulkProgress.collectAsStateWithLifecycle()

    var convertTarget by remember { mutableStateOf<ContactMeta?>(null) }
    var showBulkDialog by remember { mutableStateOf(false) }

    NeoScaffold(
        modifier = modifier,
        // Phase III — page top bar (title + back arrow) hidden on this main tab.
        // Restore by uncommenting the topBar block below.
        /*
        topBar = {
            NeoTopBar(
                title = stringResource(R.string.cv_inquiries_title),
                navIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavClick = onBack
            )
        },
        */
        bottomBar = if (state.bulkMode) {
            {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeoButton(
                        text = stringResource(R.string.inquiries_action_bulk_save),
                        onClick = {
                            showBulkDialog = true
                            viewModel.bulkSaveSelected()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NeoButton(
                        text = stringResource(R.string.inquiries_action_convert_all),
                        onClick = viewModel::convertAllSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else null
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Phase III — page header (emoji + title + description) hidden. Restore by uncommenting.
            /*
            NeoPageHeader(
                title = stringResource(R.string.cv_inquiries_title),
                description = stringResource(R.string.cv_inquiries_description),
                emoji = "📥"
            )
            */
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            NeoSearchBar(
                query = query,
                onQueryChange = viewModel::setQuery,
                placeholder = stringResource(R.string.inquiries_search_hint),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            if (state.inquiries.isEmpty()) {
                NeoEmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.inquiries_empty_title),
                    message = stringResource(R.string.inquiries_empty_body),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val nowMs = remember(state.inquiries) { System.currentTimeMillis() }
                val grouped = remember(state.inquiries, nowMs) {
                    state.inquiries
                        .groupBy { ageBucket(nowMs, it.lastCallDate.toEpochMilliseconds()) }
                        .toSortedMap(compareBy { it.order })
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (bucket, rows) ->
                        item(key = "header-${bucket.name}") {
                            BucketHeader(label = bucket.label, count = rows.size)
                        }
                        items(rows, key = { it.normalizedNumber }) { row ->
                            InquiryRow(
                                meta = row,
                                selected = row.normalizedNumber in state.selected,
                                onClick = {
                                    if (state.bulkMode) viewModel.toggleSelect(row.normalizedNumber)
                                    else onOpenDetail(row.normalizedNumber)
                                },
                                onLongPress = { viewModel.enterBulkMode(row.normalizedNumber) },
                                onConvert = { convertTarget = row }
                            )
                        }
                    }
                }
            }
            }
        }
    }

    convertTarget?.let { target ->
        // The auto-save format embeds the brand prefix + SIM tag; strip those before
        // showing the user a "rename" prompt — they want a clean starting point, not chrome.
        val cleanInitial = (target.displayName ?: target.normalizedNumber)
            .substringAfterLast('|')
            .trim()
            .ifEmpty { target.normalizedNumber }
        ConvertDialog(
            initialName = cleanInitial,
            onCancel = { convertTarget = null },
            onConfirm = { newName ->
                viewModel.convert(target.normalizedNumber, newName)
                convertTarget = null
            }
        )
    }

    if (showBulkDialog) {
        BulkSaveProgressDialog(
            progress = bulkProgress,
            onDismiss = { showBulkDialog = false }
        )
    }
}

private enum class AgeBucket(val order: Int, val label: String) {
    Today(0, "Today"),
    ThisWeek(1, "This week"),
    ThisMonth(2, "This month"),
    Stale(3, "Stale (30 days+)")
}

private fun ageBucket(nowMs: Long, lastCallMs: Long): AgeBucket {
    val ageDays = ((nowMs - lastCallMs).coerceAtLeast(0L)) / 86_400_000L
    return when {
        ageDays <= 0 -> AgeBucket.Today
        ageDays <= 7 -> AgeBucket.ThisWeek
        ageDays <= 30 -> AgeBucket.ThisMonth
        else -> AgeBucket.Stale
    }
}

@Composable
private fun BucketHeader(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = SageColors.TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            color = SageColors.TextTertiary,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** A single inquiry row (display name + count + last-call timestamp + convert CTA). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InquiryRow(
    meta: ContactMeta,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onConvert: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeoAvatar(name = meta.displayName ?: meta.normalizedNumber, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meta.displayName ?: PhoneNumberFormatter.pretty(meta.normalizedNumber),
                color = if (selected) SageColors.Sage else SageColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.inquiries_total_calls, meta.totalCalls) +
                    " · " + DateFormatter.rowTime(meta.lastCallDate),
                color = SageColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        TextButton(onClick = onConvert) {
            Text(
                text = stringResource(R.string.inquiries_action_convert),
                color = SageColors.Sage
            )
        }
    }
}

@Composable
private fun ConvertDialog(
    initialName: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialName) }
    com.callNest.app.ui.components.neo.NeoDialog(
        onDismissRequest = onCancel,
        header = {
            Text(
                stringResource(R.string.inquiries_convert_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        body = {
            Spacer(Modifier.height(com.callNest.app.ui.theme.Spacing.Sm))
            Text(
                stringResource(R.string.inquiries_convert_dialog_body),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(com.callNest.app.ui.theme.Spacing.Md))
            NeoTextField(
                value = value,
                onChange = { value = it },
                label = stringResource(R.string.inquiries_convert_dialog_label)
            )
        },
        footer = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.inquiries_cancel))
            }
            Spacer(Modifier.width(com.callNest.app.ui.theme.Spacing.Sm))
            com.callNest.app.ui.components.neo.NeoButton(
                text = stringResource(R.string.inquiries_convert_cta),
                onClick = { onConfirm(value.trim().ifEmpty { initialName }) }
            )
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun InquiriesEmptyPreview() {
    CallNestTheme {
        NeoScaffold(topBar = { NeoTopBar(title = "Inquiries") }) {
            NeoEmptyState(
                icon = Icons.Filled.Inbox,
                title = "No inquiries yet",
                message = "Auto-saved unsaved callers land here.",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun InquiriesPopulatedPreview() {
    CallNestTheme {
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val sample = listOf(
            ContactMeta(
                normalizedNumber = "+919876543210",
                displayName = "callNest-s1 +919876543210",
                isInSystemContacts = true,
                systemContactId = null,
                systemRawContactId = 42L,
                isAutoSaved = true,
                autoSavedAt = now,
                autoSavedFormat = "callNest|sim=true|",
                firstCallDate = now,
                lastCallDate = now,
                totalCalls = 4,
                totalDuration = 230,
                incomingCount = 3,
                outgoingCount = 1,
                missedCount = 0,
                computedLeadScore = 60,
                source = null,
                updatedAt = now
            )
        )
        NeoScaffold(topBar = { NeoTopBar(title = "Inquiries") }) {
            Column(Modifier.padding(16.dp)) {
                sample.forEach {
                    InquiryRow(
                        meta = it,
                        selected = false,
                        onClick = {},
                        onLongPress = {},
                        onConvert = {}
                    )
                }
            }
        }
    }
}
