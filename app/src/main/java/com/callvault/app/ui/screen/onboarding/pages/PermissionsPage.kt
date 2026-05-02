package com.callvault.app.ui.screen.onboarding.pages

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.IconCallsTint
import com.callvault.app.ui.theme.IconInquiriesTint
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Onboarding page 3 — explains every runtime permission CallVault asks for.
 *
 * Tapping "Grant" launches a single [androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions]
 * batch covering call log, contacts, phone state, and (API 33+) notifications.
 *
 * @param onContinue advances the pager regardless of grant outcome — denied
 *   users will see the rationale screen later in the flow.
 * @param launcher pre-built launcher (typically from [com.callvault.app.util.rememberPermissionLauncher]).
 */
@Composable
fun PermissionsPage(
    onContinue: () -> Unit,
    launcher: ActivityResultLauncher<Array<String>>?,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.onboarding_permissions_title),
            color = NeoColors.OnBase,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_permissions_subtitle),
            color = NeoColors.OnBaseMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        PermissionRow(
            icon = Icons.Filled.Phone,
            title = stringResource(R.string.onboarding_permissions_call_log_name),
            reason = stringResource(R.string.onboarding_permissions_call_log_reason),
            iconTint = IconCallsTint,
        )
        Spacer(Modifier.height(10.dp))
        PermissionRow(
            icon = Icons.Filled.Contacts,
            title = stringResource(R.string.onboarding_permissions_contacts_name),
            reason = stringResource(R.string.onboarding_permissions_contacts_reason),
            iconTint = IconInquiriesTint,
        )
        Spacer(Modifier.height(10.dp))
        PermissionRow(
            icon = Icons.Filled.PhonelinkRing,
            title = stringResource(R.string.onboarding_permissions_phone_state_name),
            reason = stringResource(R.string.onboarding_permissions_phone_state_reason),
            iconTint = IconCallsTint,
        )
        Spacer(Modifier.height(10.dp))
        PermissionRow(
            icon = Icons.Filled.Notifications,
            title = stringResource(R.string.onboarding_permissions_notifications_name),
            reason = stringResource(R.string.onboarding_permissions_notifications_reason),
            iconTint = NeoColors.AccentAmber,
        )
        Spacer(Modifier.height(28.dp))
        NeoButton(
            text = stringResource(R.string.onboarding_permissions_grant),
            onClick = {
                if (launcher != null) {
                    launcher.launch(permissionsToRequest(ctx))
                }
                // Caller advances the page after the launcher's result fires
                // (see OnboardingScreen). For preview / no-launcher case we
                // still call onContinue so the flow remains testable.
                if (launcher == null) onContinue()
            },
            variant = NeoButtonVariant.Primary
        )
        Spacer(Modifier.height(8.dp))
        NeoButton(
            text = stringResource(R.string.onboarding_permissions_grant_again),
            onClick = onContinue,
            variant = NeoButtonVariant.Tertiary
        )
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    reason: String,
    iconTint: Color = NeoColors.OnBase,
) {
    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeoSurface(
                modifier = Modifier.size(40.dp),
                elevation = NeoElevation.ConcaveSmall,
                shape = CircleShape
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(title, color = NeoColors.OnBase, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(reason, color = NeoColors.OnBaseMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Permission set requested by the page — POST_NOTIFICATIONS only on API 33+. */
fun permissionsToRequest(@Suppress("UNUSED_PARAMETER") ctx: Context): Array<String> {
    val base = mutableListOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        base += Manifest.permission.POST_NOTIFICATIONS
    }
    return base.toTypedArray()
}

@Suppress("unused")
private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is android.content.ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun PermissionsPagePreview() {
    CallVaultTheme { PermissionsPage(onContinue = {}, launcher = null) }
}
