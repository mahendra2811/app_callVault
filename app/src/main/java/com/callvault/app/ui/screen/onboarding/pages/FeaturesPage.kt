package com.callvault.app.ui.screen.onboarding.pages

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
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoButtonVariant
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.IconCallsTint
import com.callvault.app.ui.theme.IconInquiriesTint
import com.callvault.app.ui.theme.IconStatsTint
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

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
            .padding(horizontal = 24.dp, vertical = 32.dp),
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
        FeatureRow(
            icon = Icons.AutoMirrored.Filled.PhoneCallback,
            title = stringResource(R.string.onboarding_features_capture_title),
            body = stringResource(R.string.onboarding_features_capture_body),
            iconTint = IconCallsTint,
        )
        Spacer(Modifier.height(12.dp))
        FeatureRow(
            icon = Icons.Filled.PersonAddAlt1,
            title = stringResource(R.string.onboarding_features_autosave_title),
            body = stringResource(R.string.onboarding_features_autosave_body),
            iconTint = IconInquiriesTint,
        )
        Spacer(Modifier.height(12.dp))
        FeatureRow(
            icon = Icons.Filled.Insights,
            title = stringResource(R.string.onboarding_features_insights_title),
            body = stringResource(R.string.onboarding_features_insights_body),
            iconTint = IconStatsTint,
        )
        Spacer(Modifier.height(32.dp))
        NeoButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            variant = NeoButtonVariant.Primary
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
    CallVaultTheme { FeaturesPage(onContinue = {}) }
}
