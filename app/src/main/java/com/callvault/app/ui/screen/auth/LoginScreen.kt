package com.callvault.app.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Email + password sign-in. */
@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var submitting by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.SignedIn -> { submitting = false; onAuthenticated() }
                is AuthEvent.Error -> { submitting = false; snackbar.showSnackbar(evt.message) }
                else -> Unit
            }
        }
    }

    val canSubmit = email.isNotBlank() && password.length >= 6 && !submitting

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AuthFormScaffold(
            title = "Welcome back",
            subtitle = "Sign in to your CallVault account.",
            modifier = Modifier.padding(padding),
        ) {
            EmailField(value = email, onValueChange = { email = it })
            PasswordField(value = password, onValueChange = { password = it })

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onForgotPassword) { Text("Forgot password?") }
            }

            Button(
                onClick = { submitting = true; viewModel.signIn(email, password) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (submitting) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text("Sign in")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("New here?", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onCreateAccount) { Text("Create an account") }
            }
        }
    }
}

