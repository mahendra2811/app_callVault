package com.callNest.app.ui.screen.lock

import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.callNest.app.R
import timber.log.Timber

/**
 * Full-screen lock gate. Shown when [com.callNest.app.data.auth.AppLockState.unlocked] is false
 * AND the user has enabled biometric lock. If the device has no biometrics or device credential
 * set up, shows an explainer instead of auto-unlocking (P0-4 fix).
 */
@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    onDisableAppLock: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canAuth = remember(activity) {
        activity?.let {
            BiometricManager.from(it).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } ?: BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
    }
    val noCredentials = canAuth != BiometricManager.BIOMETRIC_SUCCESS

    var prompted by rememberSaveable { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activity, noCredentials) {
        if (activity != null && !noCredentials && !prompted) {
            prompted = true
            promptForUnlock(activity, onUnlocked) { lastError = it }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(
                id = com.callNest.app.R.drawable.ic_callnest_logo
            ),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Call Nest",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(com.callNest.app.R.string.brand_tagline),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(if (noCredentials) R.string.lock_no_credentials_title else R.string.lock_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(if (noCredentials) R.string.lock_no_credentials_subtitle else R.string.lock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        lastError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))

        if (noCredentials) {
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.lock_open_security_settings)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDisableAppLock, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.lock_disable_app_lock))
            }
        } else {
            Button(
                onClick = {
                    activity ?: return@Button
                    lastError = null
                    promptForUnlock(activity, onUnlocked) { lastError = it }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.lock_unlock)) }
        }
    }
}

private fun promptForUnlock(
    activity: FragmentActivity,
    onUnlocked: () -> Unit,
    onError: (String?) -> Unit,
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlocked()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    Timber.d("Biometric prompt cancelled by user")
                    onError(null)
                } else {
                    onError(errString.toString())
                }
            }
        })

    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.lock_prompt_title))
        .setSubtitle(activity.getString(R.string.lock_prompt_subtitle))
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()
    prompt.authenticate(info)
}
