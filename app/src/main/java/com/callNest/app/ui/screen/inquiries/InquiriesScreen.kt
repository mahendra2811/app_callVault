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
import com.callNest.app.domain.usecase.AutoSaveContactUseCase.Companion.BRAND_SUFFIX
import com.callNest.app.ui.components.neo.NeoAvatar
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoEmptyState
import com.callNest.app.ui.components.neo.NeoSearchBar
import com.callNest.app.ui.components.neo.NeoTextField
import com.callNest.app.ui.components.neo.NeoPageHeader
import com.callNest.app.ui.components.neo.NeoTopBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.callNest.app.ui.screen.shared.NeoScaffold
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.util.DateFormatter
import com.callNest.app.ui.util.PhoneNumberFormatter
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/**
 * Sprint 5 — Inquiries (auto-saved bucket).
 *
 * Long-press a row to enter bulk-select mode; the action bar then exposes
 * "Bulk save" (re-runs auto-save against the current call snapshot) and
 * "Convert all" (promotes every selected row to My Contacts).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InquiriesScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenAutoSaveSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: InquiriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val bulkProgress by viewModel.bulkProgress.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var convertTarget by remember { mutableStateOf<ContactMeta?>(null) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var saveTarget by remember { mutableStateOf<ContactMeta?>(null) }
    var savePreview by remember { mutableStateOf("") }
    var showSaveAllConfirm by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
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
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NeoEmptyState(
                        icon = Icons.Filled.Inbox,
                        title = "No unsaved numbers",
                        message = "Every caller that isn't in your contacts will show up here.",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    NeoButton(
                        text = "Open Auto-save settings",
                        onClick = onOpenAutoSaveSettings
                    )
                }
            } else {
                // Header strip: Save-all-now CTA + count + link to settings.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${state.inquiries.size} unsaved",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SageColors.TextPrimary
                        )
                        Text(
                            text = "Tap Save now to add to contacts. Long-press to bulk-select.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SageColors.TextSecondary
                        )
                    }
                    TextButton(onClick = onOpenAutoSaveSettings) {
                        Text("Settings", color = SageColors.Sage)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeoButton(
                        text = "Save all now",
                        onClick = { showSaveAllConfirm = true },
                        modifier = Modifier.weight(1f)
                    )
                }
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
                                onSaveNow = {
                                    coroutineScope.launch {
                                        savePreview = viewModel.previewSavedName(row.normalizedNumber)
                                        saveTarget = row
                                    }
                                }
                            )
                        }
                    }
                }
            }
            }
        }
        }
    }

    convertTarget?.let { target ->
        // Strip the brand suffix + any "-s1/-s2" SIM tag + prefix from the
        // prefilled name so the user types the contact's real name from a
        // clean starting point ("Priya"), not "Priya callNest".
        val raw = target.displayName ?: target.normalizedNumber
        val cleanInitial = raw
            .substringAfterLast('|')            // legacy format snapshot
            .replace(Regex("\\s*${Regex.escape(BRAND_SUFFIX)}\\s*$"), "")  // brand suffix
            .replace(Regex("\\s*-?s[12]\\s*"), " ")  // SIM tag fragments
            .replace(Regex("\\+\\d+"), "")      // E.164 number
            .trim()
            .ifEmpty { "" }
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

    // Per-row Save-now confirmation. Shows the exact name the contact will be
    // saved under so the user knows what to expect before tapping Confirm.
    saveTarget?.let { target ->
        SaveConfirmDialog(
            title = "Save this number?",
            previewName = savePreview,
            phoneNumber = PhoneNumberFormatter.pretty(target.normalizedNumber),
            confirmLabel = "Save",
            onCancel = { saveTarget = null },
            onConfirm = {
                viewModel.saveOneNow(target.normalizedNumber)
                saveTarget = null
            }
        )
    }

    if (showSaveAllConfirm) {
        SaveConfirmDialog(
            title = "Save all ${state.inquiries.size} numbers?",
            previewName = null,
            phoneNumber = "Each number will be saved using your current Auto-save settings " +
                "(prefix, SIM tag, suffix).",
            confirmLabel = "Save all",
            onCancel = { showSaveAllConfirm = false },
            onConfirm = {
                showSaveAllConfirm = false
                showBulkDialog = true
                viewModel.saveAllNow()
            }
        )
    }
}

/**
 * Confirmation dialog used before saving an unsaved inquiry. Shows the
 * proposed display name so the user can see exactly what will land in their
 * contacts.
 */
@Composable
private fun SaveConfirmDialog(
    title: String,
    previewName: String?,
    phoneNumber: String,
    confirmLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    com.callNest.app.ui.components.neo.NeoDialog(
        onDismissRequest = onCancel,
        header = {
            Text(title, style = MaterialTheme.typography.titleLarge)
        },
        body = {
            Spacer(Modifier.height(com.callNest.app.ui.theme.Spacing.Sm))
            Text(
                "Number",
                style = MaterialTheme.typography.labelSmall,
                color = SageColors.TextSecondary
            )
            Text(
                phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = SageColors.TextPrimary
            )
            if (previewName != null) {
                Spacer(Modifier.height(com.callNest.app.ui.theme.Spacing.Md))
                Text(
                    "Will be saved as",
                    style = MaterialTheme.typography.labelSmall,
                    color = SageColors.TextSecondary
                )
                Text(
                    previewName,
                    style = MaterialTheme.typography.titleMedium,
                    color = NeoColors.AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        footer = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.inquiries_cancel))
            }
            Spacer(Modifier.width(com.callNest.app.ui.theme.Spacing.Sm))
            com.callNest.app.ui.components.neo.NeoButton(
                text = confirmLabel,
                onClick = onConfirm
            )
        }
    )
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

/** A single inquiry row — pretty number + "Unsaved" badge + Save-now CTA. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InquiryRow(
    meta: ContactMeta,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSaveNow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeoAvatar(name = meta.displayName ?: meta.normalizedNumber, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = PhoneNumberFormatter.pretty(meta.normalizedNumber),
                color = if (selected) SageColors.Sage else SageColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Unsaved",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = NeoColors.AccentBlue
                )
                Text(
                    text = "  ·  " + stringResource(R.string.inquiries_total_calls, meta.totalCalls) +
                        " · " + DateFormatter.rowTime(meta.lastCallDate),
                    color = SageColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        TextButton(onClick = onSaveNow) {
            Text(text = "Save now", color = SageColors.Sage)
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
                        onSaveNow = {}
                    )
                }
            }
        }
    }
}
