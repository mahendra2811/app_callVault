package com.callvault.app.ui.screen.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Set a new password after returning from the email reset link (Supabase recovery deep link). */
@Composable
fun ResetPasswordScreen(
    onPasswordUpdated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var submitting by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.PasswordUpdated -> { submitting = false; onPasswordUpdated() }
                is AuthEvent.Error -> { submitting = false; snackbar.showSnackbar(evt.message) }
                else -> Unit
            }
        }
    }

    val match = password.isNotEmpty() && password == confirm
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AuthFormScaffold(
            title = "Set new password",
            subtitle = "Choose a password you'll remember.",
            modifier = Modifier.padding(padding),
        ) {
            PasswordField(value = password, onValueChange = { password = it }, label = "New password (min 6)")
            PasswordField(value = confirm, onValueChange = { confirm = it }, label = "Confirm password")
            if (confirm.isNotEmpty() && !match) {
                Text("Passwords don't match", color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { submitting = true; viewModel.updatePassword(password) },
                enabled = match && password.length >= 6 && !submitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (submitting) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text("Update password")
            }
        }
    }
}
