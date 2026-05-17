package com.callNest.app.ui.screen.mycontacts

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.domain.model.ContactMeta
import com.callNest.app.ui.components.neo.NeoAvatar
import com.callNest.app.ui.components.neo.NeoChip
import com.callNest.app.ui.components.neo.NeoEmptyState
import com.callNest.app.ui.components.neo.NeoSearchBar
import com.callNest.app.ui.components.neo.NeoTopBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.callNest.app.ui.screen.shared.NeoScaffold
import com.callNest.app.ui.screen.shared.StandardPage
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.ui.util.PhoneNumberFormatter
import kotlinx.datetime.Instant

/**
 * Sprint 5 — list of system contacts (`isInSystemContacts=true &&
 * isAutoSaved=false`). Tapping a row opens the per-number Call Detail screen.
 *
 * @param onBack pop the navigation stack.
 * @param onOpenDetail navigate to call detail for [ContactMeta.normalizedNumber].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyContactsScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyContactsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    StandardPage(
        title = stringResource(R.string.cv_mycontacts_title),
        description = stringResource(R.string.cv_mycontacts_description),
        emoji = "👥",
        onBack = onBack
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(8.dp))
            NeoSearchBar(
                query = query,
                onQueryChange = viewModel::setQuery,
                placeholder = stringResource(R.string.my_contacts_search_hint),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            if (state.contacts.isEmpty()) {
                NeoEmptyState(
                    icon = Icons.Filled.PersonOutline,
                    title = stringResource(R.string.my_contacts_empty_title),
                    message = stringResource(R.string.my_contacts_empty_body),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.contacts, key = { it.normalizedNumber }) { row ->
                        MyContactRow(row, onClick = { onOpenDetail(row.normalizedNumber) })
                    }
                }
            }
        }
        }
    }
}

/** A single My-Contacts list row. */
@Composable
private fun MyContactRow(meta: ContactMeta, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeoAvatar(name = meta.displayName ?: meta.normalizedNumber)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meta.displayName ?: meta.normalizedNumber,
                color = SageColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = PhoneNumberFormatter.pretty(meta.normalizedNumber),
                color = SageColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (meta.autoSavedAt != null) {
            NeoChip(
                text = stringResource(R.string.my_contacts_promoted_badge),
                selected = false,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun MyContactsEmptyPreview() {
    CallNestTheme {
        // Placeholder UI without the VM.
        NeoScaffold(topBar = {
            NeoTopBar(title = "My Contacts")
        }) {
            NeoEmptyState(
                icon = Icons.Filled.PersonOutline,
                title = "No contacts yet",
                message = "Saved contacts from your phone will appear here once your call log syncs.",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun MyContactsPopulatedPreview() {
    CallNestTheme {
        val sample = listOf(
            previewMeta("+919876543210", "Asha Kapoor", promoted = false),
            previewMeta("+12025550199", "Diego Lopez", promoted = true),
            previewMeta("+447700900123", "Priya Singh", promoted = false)
        )
        NeoScaffold(topBar = { NeoTopBar(title = "My Contacts") }) {
            Column(Modifier.padding(16.dp)) {
                sample.forEach { MyContactRow(it, onClick = {}) }
            }
        }
    }
}

private fun previewMeta(number: String, name: String, promoted: Boolean): ContactMeta {
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    return ContactMeta(
        normalizedNumber = number,
        displayName = name,
        isInSystemContacts = true,
        systemContactId = null,
        systemRawContactId = null,
        isAutoSaved = false,
        autoSavedAt = if (promoted) now else null,
        autoSavedFormat = null,
        firstCallDate = now,
        lastCallDate = now,
        totalCalls = 3,
        totalDuration = 200,
        incomingCount = 2,
        outgoingCount = 1,
        missedCount = 0,
        computedLeadScore = 50,
        source = null,
        updatedAt = now
    )
}
