package com.callNest.app.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.callNest.app.R
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.SplashGradEnd
import com.callNest.app.ui.theme.SplashGradStart
import kotlinx.coroutines.delay

private const val FallbackTimeoutMs = 3000L

/**
 * In-app splash run after the system splash hands off.
 *
 * Plays a Lottie composition from `assets/lottie/splash.json` over the brand
 * sage→orange gradient. If the composition fails to load or runs long, a 3s
 * fallback timer guarantees [onFinished] still fires so users never get stuck.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottie/splash.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 1f,
    )

    LaunchedEffect(progress, composition) {
        if (composition != null && progress >= 1f) {
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        delay(FallbackTimeoutMs)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashGradStart, SplashGradEnd))),
        contentAlignment = Alignment.Center,
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_callnest_logo),
                contentDescription = stringResource(R.string.cv_splash_brand),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(160.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF3D5A4A, widthDp = 360, heightDp = 720)
@Composable
private fun SplashScreenPreview() {
    CallNestTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(SplashGradStart, SplashGradEnd))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_callnest_logo),
                contentDescription = stringResource(R.string.cv_splash_brand),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(160.dp),
            )
        }
    }
}
