package com.callNest.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.callNest.app.data.auth.AppLockState
import com.callNest.app.data.auth.SupabaseClientProvider
import com.callNest.app.data.backup.DriveAuthManager
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.ui.navigation.CallNestNavHost
import com.callNest.app.ui.screen.auth.AuthDestinations
import com.callNest.app.ui.screen.lock.LockScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import io.github.jan.supabase.auth.handleDeeplinks
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.SageColors
import com.callNest.app.util.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity host for the entire callNest app.
 *
 * Compose owns all UI; navigation is wired up via [CallNestNavHost] which
 * decides — at runtime — whether to start in onboarding, on the permission
 * rationale gate, or on the Calls home (Sprint 3 placeholder for now).
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var permissionManager: PermissionManager
    @Inject lateinit var driveAuthManager: DriveAuthManager
    @Inject lateinit var supabase: SupabaseClientProvider
    @Inject lateinit var appLockState: AppLockState
    @Inject lateinit var settings: SettingsDataStore

    private val deepLinkChannel = Channel<String>(capacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        resolveInitialDeepLink(intent)?.let { deepLinkChannel.trySend(it) }
        if (intent?.isSupabaseAuthLink() == true) {
            supabase.client.handleDeeplinks(intent)
        }
        setContent {
            CallNestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SageColors.Canvas
                ) {
                    val lockEnabled by settings.biometricLockEnabled.collectAsState(initial = false)
                    val unlocked by appLockState.unlocked.collectAsState()
                    if (lockEnabled && !unlocked) {
                        LockScreen(
                            onUnlocked = { appLockState.markUnlocked() },
                            onDisableAppLock = {
                                lifecycleScope.launch { settings.setBiometricLockEnabled(false) }
                            },
                        )
                    } else {
                        val deepLink by produceStateFromChannel(deepLinkChannel)
                        CallNestNavHost(initialDeepLink = deepLink)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Re-lock the app whenever it leaves the foreground.
        if (!isChangingConfigurations) appLockState.lock()
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions in case the user toggled them in Settings.
        permissionManager.recheckAll(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // AppAuth redirects come back as a VIEW intent with our package's scheme.
        if (intent.action == Intent.ACTION_VIEW &&
            intent.data?.scheme == packageName &&
            intent.data?.path == "/oauth2redirect"
        ) {
            driveAuthManager.handleAuthResult(intent)
        }
        if (intent.isSupabaseAuthLink()) {
            supabase.client.handleDeeplinks(intent)
            resolveInitialDeepLink(intent)?.let { deepLinkChannel.trySend(it) }
        } else {
            resolveInitialDeepLink(intent)?.let { deepLinkChannel.trySend(it) }
        }
    }

    private fun resolveInitialDeepLink(intent: Intent?): String? {
        intent?.getStringExtra("route")?.takeIf { it.isNotBlank() }?.let { return it }
        if (intent?.isSupabaseAuthLink() == true && intent.data?.host == "auth"
            && intent.data?.path?.contains("recovery") == true
        ) return AuthDestinations.RESET
        return null
    }

    private fun Intent.isSupabaseAuthLink(): Boolean =
        action == Intent.ACTION_VIEW && data?.scheme == "callNest"
}

/** Each emission produces a fresh stable key so LaunchedEffect re-fires even on duplicate values. */
@androidx.compose.runtime.Composable
private fun produceStateFromChannel(
    channel: kotlinx.coroutines.channels.Channel<String>,
): androidx.compose.runtime.State<String?> {
    return androidx.compose.runtime.produceState<String?>(initialValue = null) {
        channel.receiveAsFlow().collect { value = it }
    }
}
