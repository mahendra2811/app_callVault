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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.R

/** Sends a password-reset email via Supabase. */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var email by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(false) }
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.PasswordResetSent -> sent = true
                is AuthEvent.Error -> snackbar.showSnackbar(evt.message)
                else -> Unit
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AuthFormScaffold(
            title = stringResource(
                if (sent) R.string.auth_forgot_sent_title else R.string.auth_forgot_title
            ),
            subtitle = if (sent)
                stringResource(R.string.auth_forgot_sent_subtitle, email)
            else
                stringResource(R.string.auth_forgot_subtitle),
            modifier = Modifier.padding(padding),
        ) {
            if (!sent) {
                EmailField(value = email, onValueChange = { email = it })
                PrimaryAuthButton(
                    text = stringResource(R.string.auth_forgot_submit),
                    onClick = { viewModel.sendPasswordReset(email) },
                    enabled = isValidEmail(email.trim()),
                    busy = busy,
                )
            } else {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.auth_forgot_back_to_signin))
                }
                TextButton(
                    onClick = { sent = false; viewModel.sendPasswordReset(email) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.auth_forgot_resend)) }
            }
        }
    }
}
