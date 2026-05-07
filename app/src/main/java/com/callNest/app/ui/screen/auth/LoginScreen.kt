package com.callNest.app.ui.screen.auth

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.callNest.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.SignedIn -> onAuthenticated()
                is AuthEvent.Error -> snackbar.showSnackbar(evt.message)
                else -> Unit
            }
        }
    }

    val canSubmit = email.isNotBlank() && password.length >= 6 && !busy

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AuthFormScaffold(
            title = stringResource(R.string.auth_login_title),
            subtitle = stringResource(R.string.auth_login_subtitle),
            modifier = Modifier.padding(padding),
        ) {
            EmailField(value = email, onValueChange = { email = it })
            PasswordField(
                value = password,
                onValueChange = { password = it },
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                onImeAction = { if (canSubmit) viewModel.signIn(email, password) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onForgotPassword) {
                    Text(stringResource(R.string.auth_login_forgot))
                }
            }

            PrimaryAuthButton(
                text = stringResource(R.string.auth_login_submit),
                onClick = { viewModel.signIn(email, password) },
                enabled = canSubmit,
                busy = busy,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.auth_login_new_here), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onCreateAccount) {
                    Text(stringResource(R.string.auth_welcome_create_account))
                }
            }
        }
    }
}

