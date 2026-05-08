package com.callNest.app.ui.screen.onboarding.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.R
import com.callNest.app.ui.components.neo.NeoButton
import com.callNest.app.ui.components.neo.NeoButtonVariant
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors

/**
 * Onboarding page 1 — friendly hero with the app's value proposition.
 *
 * Clean canvas (theme background), padded brand logo, headline + subtext,
 * brand tagline, and primary "Continue" CTA. The vertical-gradient + Neo
 * concave disc treatment was retired with the new logo identity.
 */
@Composable
fun WelcomePage(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp).padding(top = 4.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_callnest_logo),
            contentDescription = stringResource(R.string.onboarding_welcome_headline),
            modifier = Modifier.size(180.dp),
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_headline),
            color = NeoColors.OnBase,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_subtext),
            color = NeoColors.OnBaseMuted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.brand_tagline),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        NeoButton(
            text = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            variant = NeoButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F1EA, widthDp = 360, heightDp = 720)
@Composable
private fun WelcomePagePreview() {
    CallNestTheme { WelcomePage(onContinue = {}) }
}
