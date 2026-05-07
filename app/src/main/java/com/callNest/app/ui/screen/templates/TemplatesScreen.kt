package com.callNest.app.ui.screen.templates

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.domain.model.MessageTemplate

/** Manage built-in + user-added quick-reply templates. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onBack: () -> Unit,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    var addOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.templates_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addOpen = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.templates_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (templates.all { it.builtIn }) {
                com.callNest.app.ui.components.EmptyState(
                    emoji = "💬",
                    title = stringResource(R.string.templates_empty),
                    body = "Use the + button to add your own. Built-ins below.",
                )
            }
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(templates, key = { it.id }) { template ->
                    TemplateRow(template = template, onDelete = { viewModel.delete(template.id) })
                    HorizontalDivider()
                }
            }
        }
    }

    if (addOpen) {
        AddTemplateDialog(
            onDismiss = { addOpen = false },
            onSave = { label, body ->
                viewModel.add(label, body)
                addOpen = false
            },
        )
    }
}

@Composable
private fun TemplateRow(template: MessageTemplate, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(template.label, style = MaterialTheme.typography.bodyLarge)
                if (template.builtIn) {
                    Spacer(Modifier.padding(start = 8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.templates_built_in_chip)) },
                        colors = AssistChipDefaults.assistChipColors(),
                    )
                }
            }
            Text(
                template.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!template.builtIn) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.templates_delete_cd),
                )
            }
        }
    }
}

@Composable
private fun AddTemplateDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.templates_add)) },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.templates_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(stringResource(R.string.templates_body_hint)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (body.contains("{name}") || body.contains("{firstName}")) {
                    Spacer(Modifier.height(8.dp))
                    val rendered = com.callNest.app.util.TemplateInterpolator.interpolate(body, "Rajesh")
                    Text(
                        stringResource(R.string.templates_preview_label_fmt, rendered),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label, body) },
                enabled = label.isNotBlank() && body.isNotBlank(),
            ) { Text(stringResource(R.string.templates_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.templates_cancel)) }
        },
    )
}

