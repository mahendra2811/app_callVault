package com.callvault.app.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.BuildConfig
import com.callvault.app.R
import com.callvault.app.ui.screen.shared.StandardPage
import java.text.DateFormat
import java.util.Date

/** Settings: channel, auto-check, manual check, last-checked, clear-skipped. */
@Composable
fun UpdateSettingsScreen(
    onBack: () -> Unit = {},
    vm: UpdateSettingsViewModel = hiltViewModel()
) {
    val last by vm.lastChecked.collectAsStateWithLifecycle()

    StandardPage(
        title = stringResource(R.string.cv_update_settings_title),
        description = stringResource(R.string.cv_update_settings_description),
        emoji = "🆙",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "Last checked: ${formatTimestamp(last)}",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = { vm.checkNow() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Check for updates") }
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    if (ts <= 0L) return "Never"
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ts))
}
