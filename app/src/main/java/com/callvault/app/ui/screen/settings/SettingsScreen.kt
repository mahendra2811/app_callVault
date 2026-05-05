package com.callvault.app.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoToggle
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.navigation.Destinations
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors
import kotlinx.coroutines.launch

/**
 * Sprint 11 — master Settings screen.
 *
 * Each top-level section is a [NeoCard]; toggles persist to
 * [com.callvault.app.data.prefs.SettingsDataStore] via the view-model.
 *
 * Dedicated existing screens (Auto-Save, Real-Time, Lead-Scoring, Auto-Tag,
 * Backup, Updates) are reached by tapping a "Configure ▸" row instead of
 * duplicating their controls here.
 */
@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var resetOpen by remember { mutableStateOf(false) }
    var resetText by remember { mutableStateOf("") }
    var resetBusy by remember { mutableStateOf(false) }

    StandardPage(
        title = stringResource(R.string.cv_settings_title),
        description = stringResource(R.string.cv_settings_description),
        emoji = "⚙️",
        onBack = { navController.popBackStack() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Sync ----
            item("sync") {
                SectionCard(stringResource(R.string.settings_section_sync)) {
                    ToggleRow(stringResource(R.string.settings_sync_master), state.syncEnabled, vm::setSyncEnabled)
                    ToggleRow(stringResource(R.string.settings_sync_wifi_only), state.syncWifiOnly, vm::setSyncWifiOnly)
                    ToggleRow(stringResource(R.string.settings_sync_charging_only), state.syncChargingOnly, vm::setSyncChargingOnly)
                    ToggleRow(stringResource(R.string.settings_sync_on_open), state.syncOnAppOpen, vm::setSyncOnAppOpen)
                    ToggleRow(stringResource(R.string.settings_sync_on_reboot), state.syncOnReboot, vm::setSyncOnReboot)
                }
            }

            // ---- Auto-save ----
            item("auto_save") {
                SectionCard(stringResource(R.string.settings_section_auto_save)) {
                    ToggleRow(
                        stringResource(R.string.settings_auto_save_master),
                        state.autoSaveEnabled, vm::setAutoSaveEnabled
                    )
                    NavRow("Configure auto-save…") {
                        navController.navigate(Destinations.AutoSaveSettings.route)
                    }
                }
            }

            // ---- Real-time ----
            item("realtime") {
                SectionCard(stringResource(R.string.settings_section_realtime)) {
                    ToggleRow(stringResource(R.string.settings_realtime_bubble), state.bubbleEnabled, vm::setBubbleEnabled)
                    ToggleRow(stringResource(R.string.settings_realtime_popup), state.popupEnabled, vm::setPopupEnabled)
                    NavRow("Configure real-time…") {
                        navController.navigate(Destinations.RealTimeSettings.route)
                    }
                }
            }

            // ---- Notifications ----
            item("notifications") {
                SectionCard(stringResource(R.string.settings_section_notifications)) {
                    ToggleRow(stringResource(R.string.settings_notif_follow_ups), state.followUpReminders, vm::setFollowUpReminders)
                    ToggleRow(stringResource(R.string.settings_notif_daily_summary), state.dailySummary, vm::setDailySummary)
                    ToggleRow(stringResource(R.string.settings_notif_update_alerts), state.updateAlerts, vm::setUpdateAlerts)
                }
            }

            // ---- Lead scoring ----
            item("lead_scoring") {
                SectionCard(stringResource(R.string.settings_section_lead_scoring)) {
                    ToggleRow(stringResource(R.string.settings_lead_master), state.leadScoringEnabled, vm::setLeadScoringEnabled)
                    NavRow("Configure scoring weights…") {
                        navController.navigate(Destinations.LeadScoringSettings.route)
                    }
                }
            }

            // ---- Auto-tag rules ----
            item("auto_tag") {
                SectionCard(stringResource(R.string.settings_section_auto_tag)) {
                    NavRow("Manage auto-tag rules…") {
                        navController.navigate(Destinations.AutoTagRules.route)
                    }
                }
            }

            // ---- Backup ----
            item("backup") {
                SectionCard(stringResource(R.string.settings_section_backup)) {
                    ToggleRow(stringResource(R.string.settings_backup_master), state.autoBackupEnabled, vm::setAutoBackupEnabled)
                    Text(
                        text = stringResource(R.string.settings_backup_retention, state.autoBackupRetention),
                        color = SageColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    NavRow("Open backup & restore…") {
                        navController.navigate(Destinations.Backup.route)
                    }
                }
            }

            // ---- Display ----
            item("display") {
                SectionCard(stringResource(R.string.settings_section_display)) {
                    ToggleRow(stringResource(R.string.settings_display_pinned), state.pinnedAtTop, vm::setPinnedAtTop)
                    ToggleRow(stringResource(R.string.settings_display_grouped), state.groupedByNumber, vm::setGroupedByNumber)
                }
            }

            // ---- Privacy ----
            item("privacy") {
                SectionCard(stringResource(R.string.settings_section_privacy)) {
                    ToggleRow(stringResource(R.string.settings_privacy_block_hidden), state.blockHidden, vm::setBlockHidden)
                    ToggleRow(stringResource(R.string.settings_privacy_hide_blocked), state.hideBlocked, vm::setHideBlocked)
                    Spacer(Modifier.height(8.dp))
                    NeoButton(
                        text = stringResource(R.string.settings_privacy_clear_history),
                        onClick = { vm.clearSearchHistory() }
                    )
                    Spacer(Modifier.height(6.dp))
                    NeoButton(
                        text = stringResource(R.string.settings_privacy_clear_notes),
                        onClick = { vm.clearAllNotes() }
                    )
                    Spacer(Modifier.height(6.dp))
                    NeoButton(
                        text = stringResource(R.string.settings_privacy_reset_all),
                        onClick = { resetOpen = true; resetText = "" }
                    )
                }
            }

            // ---- App updates ----
            item("updates") {
                SectionCard(stringResource(R.string.settings_section_updates)) {
                    NavRow("App update settings…") {
                        navController.navigate(Destinations.UpdateSettings.route)
                    }
                }
            }

            // ---- Help & docs ----
            item("help") {
                SectionCard(stringResource(R.string.settings_section_help)) {
                    NavRow(stringResource(R.string.settings_help_getting_started)) {
                        navController.navigate(Destinations.DocsArticle.routeFor("01-getting-started"))
                    }
                    NavRow(stringResource(R.string.settings_help_faq)) {
                        navController.navigate(Destinations.DocsList.route)
                    }
                    NavRow(stringResource(R.string.settings_help_permissions)) {
                        navController.navigate(Destinations.DocsArticle.routeFor("02-permissions"))
                    }
                    NavRow(stringResource(R.string.settings_help_oem)) {
                        navController.navigate(Destinations.DocsArticle.routeFor("12-oem-battery"))
                    }
                    NavRow(stringResource(R.string.settings_help_privacy_policy)) {
                        navController.navigate(Destinations.DocsArticle.routeFor("15-privacy"))
                    }
                }
            }

            // ---- About ----
            item("about") {
                SectionCard(stringResource(R.string.settings_section_about)) {
                    val pkg = com.callvault.app.BuildConfig.VERSION_NAME
                    val build = com.callvault.app.BuildConfig.VERSION_CODE
                    Text(
                        stringResource(R.string.settings_about_version, pkg, build),
                        color = SageColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (resetOpen) {
        com.callvault.app.ui.components.neo.NeoDialog(
            onDismissRequest = { if (!resetBusy) resetOpen = false },
            header = {
                Text(
                    stringResource(R.string.settings_privacy_reset_confirm_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            body = {
                Spacer(Modifier.height(com.callvault.app.ui.theme.Spacing.Sm))
                Text(stringResource(R.string.settings_privacy_reset_confirm_body))
                Spacer(Modifier.height(com.callvault.app.ui.theme.Spacing.Sm))
                TextField(
                    value = resetText,
                    onValueChange = { resetText = it },
                    singleLine = true,
                    enabled = !resetBusy
                )
            },
            footer = {
                TextButton(enabled = !resetBusy, onClick = { resetOpen = false }) {
                    Text(stringResource(R.string.cv_common_cancel))
                }
                Spacer(Modifier.width(com.callvault.app.ui.theme.Spacing.Sm))
                TextButton(
                    enabled = resetText == stringResource(R.string.settings_privacy_reset_keyword) && !resetBusy,
                    onClick = {
                        resetBusy = true
                        scope.launch {
                            vm.resetAllData()
                            resetBusy = false
                            resetOpen = false
                        }
                    }
                ) { Text("Wipe") }
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = title,
                color = SageColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = SageColors.TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        NeoToggle(checked = checked, onChange = onChange)
    }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = NeoColors.AccentBlue,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text("›", color = SageColors.TextSecondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun SettingsPreview() {
    CallVaultTheme {
        SettingsScreen(navController = rememberNavController())
    }
}
