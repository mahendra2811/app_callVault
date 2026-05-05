package com.callvault.app.ui.screen.permission

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.SageColors

/**
 * Full-screen guard shown post-onboarding when one or more critical
 * permissions are missing but can still be re-requested via a system dialog.
 *
 * @param missing human-friendly names of missing permissions, e.g.
 *   ["Read call log", "Phone state"].
 * @param onGrant launches the bundled permission request.
 */
@Composable
fun PermissionRationaleScreen(
    missing: List<String>,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier
) {
    StandardPage(
        title = stringResource(R.string.cv_permission_rationale_title),
        description = stringResource(R.string.cv_permission_rationale_description),
        emoji = "🔐"
    ) {
        Text(
            text = stringResource(R.string.permission_rationale_title),
            color = SageColors.TextPrimary,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.permission_rationale_body),
            color = SageColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        if (missing.isNotEmpty()) {
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    missing.forEach { name ->
                        Text(
                            text = "• $name",
                            color = SageColors.TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
        NeoButton(
            text = stringResource(R.string.permission_rationale_cta),
            onClick = onGrant,
            variant = NeoButtonVariant.Primary
        )
    }
}

/** Convenience wrapper that fires a [launcher] directly. */
@Composable
fun PermissionRationaleScreen(
    missing: List<String>,
    permissions: Array<String>,
    launcher: ActivityResultLauncher<Array<String>>,
    modifier: Modifier = Modifier
) = PermissionRationaleScreen(
    missing = missing,
    onGrant = { launcher.launch(permissions) },
    modifier = modifier
)

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun PermissionRationaleScreenPreview() {
    CallVaultTheme {
        PermissionRationaleScreen(
            missing = listOf("Read call log", "Read contacts"),
            onGrant = {}
        )
    }
}
