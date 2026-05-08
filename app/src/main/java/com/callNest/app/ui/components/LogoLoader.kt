package com.callNest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callNest.app.R

/**
 * Compact logo + ring loader. Used as a replacement for full-screen splashes —
 * shown only while a quick decision is in flight (e.g. resolving auth state).
 *
 * The logo sits inside the ring with safe inner padding so it never clips.
 */
@Composable
fun LogoLoader(
    modifier: Modifier = Modifier,
    sizeDp: Int = 96,
    showTagline: Boolean = true,
) {
    val ringSize = sizeDp.dp
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(ringSize),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            // Logo sits inside the ring with ~22% inset so the ring never clips it.
            Image(
                painter = painterResource(R.drawable.ic_callnest_logo),
                contentDescription = null,
                modifier = Modifier.size(ringSize * 0.62f),
            )
        }
        if (showTagline) {
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Inline (non-fullscreen) compact variant — for in-card / button-row loading states. */
@Composable
fun InlineLogoLoader(
    modifier: Modifier = Modifier,
    sizeDp: Int = 36,
) {
    val ringSize = sizeDp.dp
    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(ringSize),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Image(
            painter = painterResource(R.drawable.ic_callnest_logo),
            contentDescription = null,
            modifier = Modifier.size(ringSize * 0.6f),
        )
    }
}
