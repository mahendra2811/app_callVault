package com.callvault.app.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
        Text(
            "CallVault",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            "Capture every call. Never lose a lead.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onSignUp, modifier = Modifier.fillMaxWidth()) {
            Text("Create an account")
        }
        OutlinedButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
            Text("I already have an account")
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
