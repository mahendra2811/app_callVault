package com.callNest.app.ui.screen.auth

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
import androidx.compose.ui.unit.sp
import com.callNest.app.R
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
        Spacer(Modifier.height(48.dp))
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(
                id = R.drawable.ic_callnest_logo
            ),
            contentDescription = stringResource(R.string.auth_welcome_brand),
            modifier = Modifier.size(180.dp),
        )
        Spacer(Modifier.height(8.dp))
        // Wordmark + localized tagline as a single lockup. Auto-swaps via drawable-hi/.
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(
                id = R.drawable.img_wordmark_tagline
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
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
