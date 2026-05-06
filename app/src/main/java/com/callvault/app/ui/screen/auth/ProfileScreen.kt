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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callvault.app.domain.model.AuthState

/** Signed-in user's profile + sign-out + change-password entry. */
@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onChangePassword: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.authState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { evt ->
            when (evt) {
                AuthEvent.SignedOut -> onSignedOut()
                is AuthEvent.Error -> snackbar.showSnackbar(evt.message)
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profile") }) },
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
                        s.session.displayName ?: "CallVault user",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    s.session.email?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
                        Text("Change password")
                    }
                    Button(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign out")
                    }
                }
                else -> Text("Not signed in.")
            }
        }
    }
}
