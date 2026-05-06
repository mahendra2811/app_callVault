package com.callvault.app.ui.screen.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.SageColors
import com.callvault.app.util.PermissionManager

/**
 * Full-screen guard shown when a critical permission is permanently denied.
 *
 * The only path forward is opening the app's system Settings page; CallVault
 * cannot re-prompt once the user picked "Don't ask again".
 *
 * @param onOpenSettings invoked when the user taps the CTA — wires through to
 *   [PermissionManager.openAppSettings].
 */
@Composable
fun PermissionDeniedScreen(
    onOpenSettings: () -> Unit,
    onAskAgain: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    StandardPage(
        title = stringResource(R.string.permission_denied_v2_title),
        description = stringResource(R.string.permission_denied_v2_body_1),
        emoji = "🔒"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(R.string.permission_denied_v2_body_2),
                color = SageColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(28.dp))
            NeoButton(
                text = stringResource(R.string.permission_denied_cta),
                onClick = onOpenSettings,
                variant = NeoButtonVariant.Primary
            )
            Spacer(Modifier.height(8.dp))
            NeoButton(
                text = stringResource(R.string.permission_denied_v2_retry),
                onClick = onAskAgain,
                variant = NeoButtonVariant.Tertiary
            )
        }
    }
}

/** Variant that wires straight to a [PermissionManager] singleton. */
@Composable
fun PermissionDeniedScreen(
    permissionManager: PermissionManager,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    PermissionDeniedScreen(
        onOpenSettings = { permissionManager.openAppSettings(ctx) },
        onAskAgain = { permissionManager.openAppSettings(ctx) },
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun PermissionDeniedScreenPreview() {
    CallVaultTheme { PermissionDeniedScreen(onOpenSettings = {}) }
}
