package com.callvault.app.ui.screen.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Sends a password-reset email via Supabase. */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var email by rememberSaveable { mutableStateOf("") }
    var submitting by rememberSaveable { mutableStateOf(false) }
    var sent by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.PasswordResetSent -> { submitting = false; sent = true }
                is AuthEvent.Error -> { submitting = false; snackbar.showSnackbar(evt.message) }
                else -> Unit
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AuthFormScaffold(
            title = if (sent) "Check your email" else "Forgot password",
            subtitle = if (sent)
                "We sent a reset link to $email. Tap the link to set a new password."
            else
                "Enter the email you signed up with — we'll send a reset link.",
            modifier = Modifier.padding(padding),
        ) {
            if (!sent) {
                EmailField(value = email, onValueChange = { email = it })
                Button(
                    onClick = { submitting = true; viewModel.sendPasswordReset(email) },
                    enabled = isValidEmail(email.trim()) && !submitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (submitting) CircularProgressIndicator(strokeWidth = 2.dp)
                    else Text("Send reset link")
                }
            } else {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to sign in")
                }
                TextButton(
                    onClick = { sent = false; submitting = true; viewModel.sendPasswordReset(email) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Resend link") }
            }
        }
    }
}
