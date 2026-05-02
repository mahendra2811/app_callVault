package com.callvault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.callvault.app.ui.navigation.CallVaultNavHost
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.util.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}
