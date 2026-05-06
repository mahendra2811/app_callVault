package com.callvault.app.ui.screen.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/** Shown after sign-up while user clicks the email confirmation link. */
@Composable
fun VerifyEmailScreen(
    email: String,
    onBackToSignIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var resending by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.PasswordResetSent -> { resending = false; snackbar.showSnackbar("Verification email sent again.") }
                AuthEvent.SignedIn -> onBackToSignIn()
                is AuthEvent.Error -> { resending = false; snackbar.showSnackbar(evt.message) }
                else -> Unit
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AuthFormScaffold(
            title = "Verify your email",
            subtitle = "We sent a confirmation link to $email. Tap the link, then return here to sign in.",
            modifier = Modifier.padding(padding),
        ) {
            Button(onClick = onBackToSignIn, modifier = Modifier.fillMaxWidth()) {
                Text("I've verified — sign in")
            }
            OutlinedButton(
                onClick = { resending = true; viewModel.resendVerificationEmail(email) },
                enabled = !resending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (resending) "Sending…" else "Resend verification email")
            }
        }
    }
}
