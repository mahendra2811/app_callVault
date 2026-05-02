package com.callvault.app.ui.screen.splash

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoSurface
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation
import kotlinx.coroutines.delay

private const val DiscAppearAt = 100L
private const val RingStartAt = 500L
private const val RingEndAt = 800L
private const val WordmarkStartAt = 800L
private const val WordmarkCharDelay = 50L
private const val SettleAt = 1200L
private const val FinishAt = 1500L

private const val Brand = "callVault"

/**
 * In-app splash screen run after the system splash hands off.
 *
 * Animation timeline:
 *  - 100ms — concave 160dp disc with a centered "C" appears.
 *  - 100→500ms — disc scales 0.85 → 1.0 (spring).
 *  - 500→800ms — ring traces a 360° arc around the disc.
 *  - 800→1200ms — "callVault" types out at 50ms/char below the disc.
 *  - 1200→1500ms — settle, then [onFinished] fires.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var stage by remember { mutableStateOf(0) } // 0 idle, 1 disc, 2 ring, 3 wordmark, 4 done
    var visibleChars by remember { mutableStateOf(0) }

    val discScale by animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0.85f,
        animationSpec = spring(stiffness = 600f, dampingRatio = 0.7f),
        label = "discScale"
    )
    val ringSweep by animateFloatAsState(
        targetValue = if (stage >= 2) 360f else 0f,
        animationSpec = tween(durationMillis = (RingEndAt - RingStartAt).toInt()),
        label = "ringSweep"
    )

    LaunchedEffect(Unit) {
        delay(DiscAppearAt)
        stage = 1
        delay(RingStartAt - DiscAppearAt)
        stage = 2
        delay(WordmarkStartAt - RingStartAt)
        stage = 3
        repeat(Brand.length) {
            visibleChars = it + 1
            delay(WordmarkCharDelay)
        }
        delay(SettleAt - (WordmarkStartAt + Brand.length * WordmarkCharDelay))
        delay(FinishAt - SettleAt)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeoColors.Base),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (stage >= 1) {
                    NeoSurface(
                        elevation = NeoElevation.ConcaveMedium,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(160.dp)
                            .scale(discScale)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.cv_logo),
                                contentDescription = stringResource(R.string.cv_splash_brand),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                if (stage >= 2) {
                    val strokePx = with(androidx.compose.ui.platform.LocalDensity.current) { 4.dp.toPx() }
                    Canvas(modifier = Modifier.size(176.dp)) {
                        drawArc(
                            color = NeoColors.AccentBlue,
                            startAngle = -90f,
                            sweepAngle = ringSweep,
                            useCenter = false,
                            topLeft = Offset(strokePx / 2f, strokePx / 2f),
                            size = Size(size.width - strokePx, size.height - strokePx),
                            style = Stroke(width = strokePx)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            if (stage >= 3) {
                Text(
                    text = Brand.substring(0, visibleChars),
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeoColors.OnBase,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun SplashScreenPreview() {
    CallVaultTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NeoColors.Base),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NeoSurface(
                        elevation = NeoElevation.ConcaveMedium,
                        shape = CircleShape,
                        modifier = Modifier.size(160.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "C",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = NeoColors.AccentBlue,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Canvas(modifier = Modifier.size(176.dp)) {
                        drawArc(
                            color = NeoColors.AccentBlue,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(4f, 4f),
                            size = Size(size.width - 8f, size.height - 8f),
                            style = Stroke(width = 8f)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = Brand,
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeoColors.OnBase,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
