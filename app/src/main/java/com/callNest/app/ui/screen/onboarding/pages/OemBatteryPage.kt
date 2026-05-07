package com.callNest.app.ui.screen.onboarding.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.R
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoButtonVariant
import com.callNest.app.ui.components.neo.NeoCard
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.util.OemBatteryGuide
import com.callNest.app.util.OemVendor

/**
 * Onboarding page 4 — vendor-specific autostart / battery instructions.
 *
 * Detects the running OEM via [OemBatteryGuide.detect] and shows tailored
 * step-by-step instructions plus a deep-link button to the relevant settings
 * screen (with safe fallbacks for unknown ROMs).
 *
 * @param onContinue invoked when the user taps "Done".
 * @param vendorOverride optional vendor for previews/tests.
 */
@Composable
fun OemBatteryPage(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    vendorOverride: OemVendor? = null
) {
    val ctx = LocalContext.current
    val vendor = remember(vendorOverride) { vendorOverride ?: OemBatteryGuide.detect() }
    val steps = remember(vendor) { OemBatteryGuide.stepsFor(vendor) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = stringResource(R.string.onboarding_oem_title),
            color = NeoColors.OnBase,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_oem_subtitle, vendor.displayName),
            color = NeoColors.OnBaseMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                steps.forEachIndexed { idx, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "${idx + 1}.",
                            color = NeoColors.AccentBlue,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = step,
                            color = NeoColors.OnBase,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (idx != steps.lastIndex) Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        NeoButton(
            text = stringResource(R.string.onboarding_oem_open_settings),
            onClick = { OemBatteryGuide.openAutostartSettings(ctx, vendor) },
            variant = NeoButtonVariant.Secondary
        )
        Spacer(Modifier.height(8.dp))
        NeoButton(
            text = stringResource(R.string.onboarding_done),
            onClick = onContinue,
            variant = NeoButtonVariant.Primary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun OemBatteryPagePreview() {
    CallNestTheme {
        OemBatteryPage(onContinue = {}, vendorOverride = OemVendor.Xiaomi)
    }
}
