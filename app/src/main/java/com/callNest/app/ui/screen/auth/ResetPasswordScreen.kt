package com.callNest.app.ui.screen.auth

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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.domain.model.AuthState

/** Set a new password after returning from the email reset link (Supabase recovery deep link). */
@Composable
fun ResetPasswordScreen(
    onPasswordUpdated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.PasswordUpdated -> onPasswordUpdated()
                is AuthEvent.Error -> snackbar.showSnackbar(evt.message)
                else -> Unit
            }
        }
    }

    val match = password.isNotEmpty() && password == confirm
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AuthFormScaffold(
            title = stringResource(R.string.auth_reset_title),
            subtitle = stringResource(R.string.auth_reset_subtitle),
            modifier = Modifier.padding(padding),
        ) {
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.auth_reset_new_password_label),
            )
            PasswordField(
                value = confirm,
                onValueChange = { confirm = it },
                label = stringResource(R.string.auth_signup_confirm_label),
            )
            if (confirm.isNotEmpty() && !match) {
                Text(
                    stringResource(R.string.auth_signup_passwords_mismatch),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            val sessionReady = authState is AuthState.SignedIn
            if (!sessionReady) {
                Text(
                    stringResource(
                        if (authState is AuthState.Loading) R.string.auth_reset_waiting
                        else R.string.auth_reset_session_missing
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PrimaryAuthButton(
                text = stringResource(R.string.auth_reset_submit),
                onClick = { viewModel.updatePassword(password) },
                enabled = sessionReady && match && password.length >= 8,
                busy = busy,
            )
        }
    }
}
