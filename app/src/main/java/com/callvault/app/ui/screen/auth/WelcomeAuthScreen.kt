package com.callvault.app.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.callvault.app.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Entry point for unauthenticated users. */
@Composable
fun WelcomeAuthScreen(
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(64.dp))
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(
                id = com.callvault.app.R.mipmap.ic_launcher
            ),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
        Text(
            stringResource(R.string.auth_welcome_brand),
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.auth_welcome_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onSignUp, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.auth_welcome_create_account))
        }
        OutlinedButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.auth_welcome_have_account))
        }
        // Google sign-in — uncomment once GOOGLE_OAUTH_WEB_CLIENT_ID is configured.
        // OutlinedButton(onClick = onGoogle, modifier = Modifier.fillMaxWidth()) { Text("Continue with Google") }
    }
}

@Preview
@Composable
private fun PreviewWelcome() {
    WelcomeAuthScreen(onSignIn = {}, onSignUp = {})
}
