package com.callvault.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.callvault.app.data.backup.DriveAuthManager
import com.callvault.app.ui.navigation.CallVaultNavHost
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.util.PermissionManager
import com.callvault.app.util.SplashGate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Single-activity host for the entire CallVault app.
 *
 * Compose owns all UI; navigation is wired up via [CallVaultNavHost] which
 * decides — at runtime — whether to start in onboarding, on the permission
 * rationale gate, or on the Calls home (Sprint 3 placeholder for now).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var permissionManager: PermissionManager
    @Inject lateinit var splashGate: SplashGate
    @Inject lateinit var driveAuthManager: DriveAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !splashGate.isReady.value }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // After 800ms, hand off the system splash to the in-app Compose splash.
        lifecycleScope.launch {
            delay(800)
            splashGate.markReady()
        }
        val deepLink = intent?.getStringExtra("route")?.takeIf { it.isNotBlank() }
        setContent {
            CallVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NeoColors.Base
                ) {
                    CallVaultNavHost(initialDeepLink = deepLink)
                }
            }
        }
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
    }
}
