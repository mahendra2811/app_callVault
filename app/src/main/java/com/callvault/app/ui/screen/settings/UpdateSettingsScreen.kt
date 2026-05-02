package com.callvault.app.ui.screen.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.BuildConfig
import com.callvault.app.data.work.UpdateCheckWorker
import java.text.DateFormat
import java.util.Date

/** Settings: channel, auto-check, manual check, last-checked, clear-skipped. */
@Composable
fun UpdateSettingsScreen(
    vm: UpdateSettingsViewModel = hiltViewModel()
) {
    val ctx: Context = LocalContext.current
    val channel by vm.channel.collectAsStateWithLifecycle()
    val auto by vm.autoCheck.collectAsStateWithLifecycle()
    val last by vm.lastChecked.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("App updates", style = MaterialTheme.typography.headlineSmall)

        Text(
            "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text("Update channel", style = MaterialTheme.typography.titleMedium)
        ChannelOption("stable", "Stable", channel) { vm.setChannel("stable") }
        ChannelOption("beta", "Beta", channel) { vm.setChannel("beta") }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Check for updates automatically", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Once a week over Wi-Fi or cellular.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = auto,
                onCheckedChange = {
                    vm.setAutoCheck(it)
                    if (it) UpdateCheckWorker.schedule(ctx) else UpdateCheckWorker.cancel(ctx)
                }
            )
        }

        Button(onClick = { vm.checkNow() }) { Text("Check now") }

        Text(
            "Last checked: ${formatTimestamp(last)}",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()

        TextButton(onClick = { vm.clearSkipped() }) {
            Text("Clear skipped versions")
        }
    }
}

@Composable
private fun ChannelOption(
    value: String,
    label: String,
    selected: String,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected == value, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected == value, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun formatTimestamp(ts: Long): String {
    if (ts <= 0L) return "Never"
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ts))
}
