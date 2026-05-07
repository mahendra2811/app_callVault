package com.callvault.app.ui.screen.pipeline

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.domain.model.MessageTemplate

/**
 * Step 1 of the bulk-blast flow: pick which template to send to all selected contacts.
 * The list is provided via [templates] from the caller (TemplatesViewModel).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkBlastTemplatePickerSheet(
    templates: List<MessageTemplate>,
    onPick: (MessageTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.bulk_blast_pick_template),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(templates, key = { it.id }) { tpl ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .let { mod ->
                                mod.then(
                                    Modifier.padding(horizontal = 4.dp)
                                )
                            },
                    ) {
                        Text(tpl.label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            tpl.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onPick(tpl) }) {
                            Text(stringResource(R.string.bulk_blast_open))
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * Step 2: progress sheet listing each selected contact with a per-row "Open" button.
 * WhatsApp doesn't allow true blast — the user taps Open, sends in WA, returns to this sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkBlastProgressSheet(
    targets: List<PipelineCard>,
    template: MessageTemplate,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var openedIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.bulk_blast_progress_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.bulk_blast_progress_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.bulk_blast_done_count_fmt, openedIds.size, targets.size),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(targets, key = { it.normalizedNumber }) { card ->
                    val opened = card.normalizedNumber in openedIds
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.padding(end = 8.dp)) {
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
                        }
                        if (opened) {
                            Text(
                                stringResource(R.string.bulk_blast_sent),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        } else {
                            val fallback = stringResource(R.string.template_fallback_name)
                            OutlinedButton(onClick = {
                                val digits = card.normalizedNumber.filter { it.isDigit() }
                                val rendered = com.callvault.app.util.TemplateInterpolator
                                    .interpolate(template.body, card.displayName, fallback)
                                val url = "https://wa.me/$digits?text=${Uri.encode(rendered)}"
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                                openedIds = openedIds + card.normalizedNumber
                            }) {
                                Text(stringResource(R.string.bulk_blast_open))
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.bulk_blast_close))
            }
        }
    }
}
