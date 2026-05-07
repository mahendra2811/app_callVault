package com.callNest.app.ui.screen.auth

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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callNest.app.R
import com.callNest.app.domain.model.AuthState

/** Sign-up: full name + email + password + confirm + terms. */
@Composable
fun SignupScreen(
    onAccountCreated: (email: String) -> Unit,
    onAuthenticated: () -> Unit,
    onSignInInstead: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.SignedUp -> {
                    if (authState is AuthState.SignedIn) onAuthenticated()
                    else onAccountCreated(email.trim())
                }
                AuthEvent.SignedIn -> onAuthenticated()
                is AuthEvent.Error -> snackbar.showSnackbar(evt.message)
                else -> Unit
            }
        }
    }

    val passwordsMatch = password.isNotEmpty() && password == confirm
    val canSubmit = fullName.isNotBlank() &&
        isValidEmail(email.trim()) &&
        password.length >= 8 &&
        passwordsMatch &&
        acceptedTerms &&
        !busy

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        AuthFormScaffold(
            title = stringResource(R.string.auth_signup_title),
            subtitle = stringResource(R.string.auth_signup_subtitle),
            modifier = Modifier.padding(padding),
        ) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(stringResource(R.string.auth_signup_full_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            EmailField(value = email, onValueChange = { email = it })
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.auth_signup_password_min8_label),
            )
            PasswordStrengthBar(password = password)
            PasswordField(
                value = confirm,
                onValueChange = { confirm = it },
                label = stringResource(R.string.auth_signup_confirm_label),
            )
            if (confirm.isNotEmpty() && !passwordsMatch) {
                Text(
                    stringResource(R.string.auth_signup_passwords_mismatch),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
                Text(
                    stringResource(R.string.auth_signup_terms),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            PrimaryAuthButton(
                text = stringResource(R.string.auth_signup_submit),
                onClick = { viewModel.signUp(email, password, fullName) },
                enabled = canSubmit,
                busy = busy,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.auth_signup_already_member), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onSignInInstead) {
                    Text(stringResource(R.string.auth_signin_link))
                }
            }
        }
    }
}
