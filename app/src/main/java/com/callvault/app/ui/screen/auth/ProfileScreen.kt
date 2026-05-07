package com.callvault.app.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.callvault.app.R
import com.callvault.app.domain.model.AuthState

/** Signed-in user's profile + sign-out + change-password entry. */
@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onChangePassword: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.authState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var deleteDialogOpen by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.SignedOut, AuthEvent.AccountDeleted -> {
                    deleteDialogOpen = false
                    onSignedOut()
                }
                is AuthEvent.Error -> {
                    // Keep dialog open so user can retry/cancel after a wrong-password failure.
                    snackbar.showSnackbar(evt.message)
                }
                else -> Unit
            }
        }
    }

    if (deleteDialogOpen) {
        DeleteAccountDialog(
            busy = busy,
            onConfirm = { password -> viewModel.deleteAccount(password) },
            onDismiss = { if (!busy) deleteDialogOpen = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_profile_title)) },
                navigationIcon = if (onBack != null) ({
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.auth_profile_back_cd),
                        )
                    }
                }) else ({}),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val s = state) {
                is AuthState.SignedIn -> {
                    Text(
                        s.session.displayName ?: stringResource(R.string.auth_profile_default_name),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    s.session.email?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.auth_profile_change_password))
                    }
                    Button(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.auth_profile_sign_out))
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(
                        onClick = { deleteDialogOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.auth_profile_delete_account),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> Text(stringResource(R.string.auth_profile_not_signed_in))
            }
        }
    }
}
