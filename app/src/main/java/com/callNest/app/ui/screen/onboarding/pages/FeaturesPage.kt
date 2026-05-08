package com.callNest.app.ui.screen.onboarding.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.R
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoButtonVariant
import com.callNest.app.ui.components.neo.NeoCard
import com.callNest.app.ui.components.neo.NeoSurface
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.IconCallsTint
import com.callNest.app.ui.theme.IconInquiriesTint
import com.callNest.app.ui.theme.IconStatsTint
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation

/**
 * Onboarding page 2 — three benefit cards stacked vertically.
 *
 * Each card pairs a Material icon with a short headline + one-line body.
 *
 * @param onContinue invoked when the user taps the primary button.
 */
@Composable
fun FeaturesPage(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp).padding(top = 4.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_features_title),
            color = NeoColors.OnBase,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        // All three rows share the brand-green tint for consistency with the new logo.
        val brand = MaterialTheme.colorScheme.primary
        FeatureRow(
            icon = Icons.AutoMirrored.Filled.PhoneCallback,
            title = stringResource(R.string.onboarding_features_capture_title),
            body = stringResource(R.string.onboarding_features_capture_body),
            iconTint = brand,
        )
        Spacer(Modifier.height(12.dp))
        FeatureRow(
            icon = Icons.Filled.PersonAddAlt1,
            title = stringResource(R.string.onboarding_features_autosave_title),
            body = stringResource(R.string.onboarding_features_autosave_body),
            iconTint = brand,
        )
        Spacer(Modifier.height(12.dp))
        FeatureRow(
            icon = Icons.Filled.Insights,
            title = stringResource(R.string.onboarding_features_insights_title),
            body = stringResource(R.string.onboarding_features_insights_body),
            iconTint = brand,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.brand_tagline),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        NeoButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            variant = NeoButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    body: String,
    iconTint: Color = NeoColors.AccentBlue,
) {
    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeoSurface(
                modifier = Modifier.size(44.dp),
                elevation = NeoElevation.ConcaveSmall,
                shape = CircleShape
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                }
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    color = NeoColors.OnBase,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = body,
                    color = NeoColors.OnBaseMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun FeaturesPagePreview() {
    CallNestTheme { FeaturesPage(onContinue = {}) }
}
