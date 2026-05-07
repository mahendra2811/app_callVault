package com.callvault.app.ui.screen.digest

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R
import com.callvault.app.domain.model.WeeklyDigest
import java.text.DateFormat
import java.util.Date

@Composable
fun WeeklyDigestScreen(
    onBack: () -> Unit,
    viewModel: WeeklyDigestViewModel = hiltViewModel(),
) {
    val digest by viewModel.digest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareSubject = stringResource(R.string.digest_share_subject)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.digest_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            digest?.let { d ->
                                val text = formatPlainText(context, d)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, shareSubject))
                            }
                        },
                        enabled = digest != null,
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.digest_share_button))
                    }
                },
            )
        },
    ) { padding ->
        val d = digest
        if (d == null) {
            Text(
                "…",
                modifier = Modifier.padding(padding).padding(24.dp),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
            Text(
                stringResource(R.string.digest_subtitle_fmt, df.format(Date(d.fromMs)), df.format(Date(d.toMs))),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.digest_total_calls_fmt, d.totalCalls),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        stringResource(R.string.digest_breakdown_fmt, d.incoming, d.outgoing, d.missed),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    DigestBreakdownBars(
                        incoming = d.incoming,
                        outgoing = d.outgoing,
                        missed = d.missed,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.digest_unique_contacts_fmt, d.uniqueContacts))
                    Text(
                        stringResource(R.string.digest_hot_leads_fmt, d.hotLeads),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            val aiBusy by viewModel.aiBusy.collectAsStateWithLifecycle()
            if (d.aiNarrative != null || aiBusy) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.digest_ai_section_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (aiBusy) stringResource(R.string.digest_ai_busy)
                                else (d.aiNarrative ?: ""),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (!aiBusy && d.aiNarrative != null) {
                            OutlinedButton(
                                onClick = { viewModel.regenerateNarrative() },
                                modifier = Modifier.padding(top = 8.dp),
                            ) { Text(stringResource(R.string.digest_ai_regenerate)) }
                        }
                    }
                }
            }

            if (d.topTags.isNotEmpty()) {
                Text(
                    stringResource(R.string.digest_top_tags_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                d.topTags.forEach { tag ->
                    Text(
                        stringResource(R.string.digest_top_tag_row_fmt, tag.name, tag.count),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                HorizontalDivider()
            }

            if (d.topCallers.isNotEmpty()) {
                Text(
                    stringResource(R.string.digest_top_callers_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                    items(d.topCallers, key = { it.normalizedNumber }) { caller ->
                        val label = caller.displayName ?: caller.normalizedNumber
                        Text(
                            stringResource(
                                R.string.digest_top_caller_row_fmt,
                                label, caller.callCount, caller.leadScore,
                            ),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        HorizontalDivider()
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val text = formatPlainText(context, d)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, shareSubject))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.padding(start = 8.dp))
                Text(stringResource(R.string.digest_share_button))
            }
        }
    }
}

/** Lightweight proportional 3-segment bar (incoming / outgoing / missed). No Vico dependency. */
@Composable
private fun DigestBreakdownBars(incoming: Int, outgoing: Int, missed: Int) {
    val total = (incoming + outgoing + missed).coerceAtLeast(1)
    val inFrac = incoming.toFloat() / total
    val outFrac = outgoing.toFloat() / total
    val missedFrac = missed.toFloat() / total
    val inColor = MaterialTheme.colorScheme.primary
    val outColor = MaterialTheme.colorScheme.tertiary
    val missedColor = MaterialTheme.colorScheme.error
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().height(10.dp),
    ) {
        if (inFrac > 0f) androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(inFrac)
                .fillMaxHeight()
                .background(inColor),
        )
        if (outFrac > 0f) androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(outFrac)
                .fillMaxHeight()
                .background(outColor),
        )
        if (missedFrac > 0f) androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(missedFrac)
                .fillMaxHeight()
                .background(missedColor),
        )
    }
}

private fun formatPlainText(ctx: android.content.Context, d: WeeklyDigest): String {
    val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
    val sb = StringBuilder()
    sb.append(ctx.getString(R.string.digest_screen_title)).append('\n')
    sb.append(
        ctx.getString(R.string.digest_subtitle_fmt, df.format(Date(d.fromMs)), df.format(Date(d.toMs)))
    ).append("\n\n")
    sb.append(ctx.getString(R.string.digest_total_calls_fmt, d.totalCalls)).append('\n')
    sb.append(ctx.getString(R.string.digest_breakdown_fmt, d.incoming, d.outgoing, d.missed)).append('\n')
    sb.append(ctx.getString(R.string.digest_unique_contacts_fmt, d.uniqueContacts)).append('\n')
    sb.append(ctx.getString(R.string.digest_hot_leads_fmt, d.hotLeads)).append("\n\n")
    if (d.topCallers.isNotEmpty()) {
        sb.append(ctx.getString(R.string.digest_top_callers_title)).append('\n')
        d.topCallers.forEach { caller ->
            val label = caller.displayName ?: caller.normalizedNumber
            sb.append("• ").append(
                ctx.getString(R.string.digest_top_caller_row_fmt, label, caller.callCount, caller.leadScore)
            ).append('\n')
        }
    }
    return sb.toString()
}
