package com.callvault.app.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Sign-up: full name + email + password + confirm + terms. */
@Composable
fun SignupScreen(
    onAccountCreated: (email: String) -> Unit,
    onSignInInstead: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    var submitting by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.SignedUp -> { submitting = false; onAccountCreated(email.trim()) }
                AuthEvent.SignedIn -> { submitting = false; onAccountCreated(email.trim()) }
                is AuthEvent.Error -> { submitting = false; snackbar.showSnackbar(evt.message) }
                else -> Unit
            }
        }
    }

    val passwordsMatch = password.isNotEmpty() && password == confirm
    val canSubmit = fullName.isNotBlank() &&
        isValidEmail(email.trim()) &&
        password.length >= 6 &&
        passwordsMatch &&
        acceptedTerms &&
        !submitting

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AuthFormScaffold(
            title = "Create account",
            subtitle = "Start tracking every inquiry call.",
            modifier = Modifier.padding(padding),
        ) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            EmailField(value = email, onValueChange = { email = it })
            PasswordField(value = password, onValueChange = { password = it }, label = "Password (min 6)")
            PasswordField(
                value = confirm,
                onValueChange = { confirm = it },
                label = "Confirm password"
            )
            if (confirm.isNotEmpty() && !passwordsMatch) {
                Text(
                    "Passwords don't match",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
                Text(
                    "I agree to the Terms of Service and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    submitting = true
                    viewModel.signUp(email, password, fullName)
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (submitting) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text("Create account")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Already a member?", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onSignInInstead) { Text("Sign in") }
            }
        }
    }
}
