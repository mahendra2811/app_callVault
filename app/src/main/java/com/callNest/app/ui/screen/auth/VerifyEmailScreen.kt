package com.callNest.app.ui.screen.auth

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.domain.model.AuthState

/**
 * Shown after sign-up while user clicks the email confirmation link.
 * Auto-routes to [onAuthenticated] the moment the session flips to signed-in
 * (Supabase emits this once the user clicks the email link on a different device).
 */
@Composable
fun VerifyEmailScreen(
    email: String,
    onAuthenticated: () -> Unit,
    onBackToSignIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        if (authState is AuthState.SignedIn) onAuthenticated()
    }

    val resentMsg = stringResource(R.string.auth_verify_resent_snackbar)
    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.VerificationResent -> snackbar.showSnackbar(resentMsg)
                is AuthEvent.Error -> snackbar.showSnackbar(evt.message)
                else -> Unit
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AuthFormScaffold(
            title = stringResource(R.string.auth_verify_title),
            subtitle = stringResource(R.string.auth_verify_subtitle, email),
            modifier = Modifier.padding(padding),
        ) {
            Button(
                onClick = { viewModel.refreshSession() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (busy) R.string.auth_verify_continue_busy else R.string.auth_verify_continue))
            }
            OutlinedButton(
                onClick = { viewModel.resendVerificationEmail(email) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.auth_verify_resend))
            }
            OutlinedButton(onClick = onBackToSignIn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.auth_forgot_back_to_signin))
            }
        }
    }
}
