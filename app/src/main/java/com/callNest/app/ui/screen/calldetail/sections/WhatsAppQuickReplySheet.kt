package com.callNest.app.ui.screen.calldetail.sections

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.domain.model.MessageTemplate
import com.callNest.app.ui.screen.templates.TemplatesViewModel

/**
 * Bottom sheet that lists templates and opens a pre-filled WhatsApp chat with [normalizedNumber].
 * Tapping a template fires `wa.me/<digits>?text=<body>`. "Open WhatsApp chat" sends with no body.
 */
@Composable
fun WhatsAppQuickReplySheet(
    normalizedNumber: String,
    displayName: String? = null,
    onDismiss: () -> Unit,
    onManageTemplates: (() -> Unit)? = null,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val digits = normalizedNumber.filter { it.isDigit() }

    val fallback = stringResource(R.string.template_fallback_name)
    fun launch(text: String?) {
        val rendered = text?.let {
            com.callNest.app.util.TemplateInterpolator.interpolate(it, displayName, fallback)
        }
        val url = if (rendered.isNullOrBlank()) "https://wa.me/$digits"
        else "https://wa.me/$digits?text=${Uri.encode(rendered)}"
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.quickreply_sheet_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))

            templates.forEach { template ->
                TemplateRow(template = template, onClick = { launch(template.body) })
                HorizontalDivider()
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { launch(null) }) {
                    Text(stringResource(R.string.quickreply_open_chat))
                }
                if (onManageTemplates != null) {
                    TextButton(onClick = { onDismiss(); onManageTemplates() }) {
                        Text(stringResource(R.string.quickreply_manage))
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(template: MessageTemplate, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(template.label, style = MaterialTheme.typography.bodyLarge)
        Text(
            template.body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}
