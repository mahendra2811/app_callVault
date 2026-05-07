package com.callNest.app.ui.components.neo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.R
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.theme.SageColors

/**
 * Top app bar for callNest screens.
 *
 * @param title screen title rendered centered.
 * @param subtitle optional second-row caption shown beneath the title bar.
 * @param showBrand when true, prepends a 32.dp concave brand chip (the
 *   monogram "C") to the left of the title — used by [MainScaffold] for the
 *   app's primary bar.
 * @param navIcon optional navigation icon (typically back arrow).
 * @param onNavClick callback when the nav icon is tapped.
 * @param actions trailing icon buttons.
 */
@Composable
fun NeoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showBrand: Boolean = false,
    navIcon: ImageVector? = null,
    onNavClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    NeoSurface(
        modifier = modifier.fillMaxWidth(),
        elevation = NeoElevation.Flat,
        shape = RoundedCornerShape(0.dp),
        color = SageColors.Canvas
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (navIcon != null) {
                    NeoIconButton(
                        icon = navIcon,
                        onClick = onNavClick,
                        contentDescription = "Back",
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showBrand) {
                        NeoSurface(
                            modifier = Modifier.size(32.dp),
                            elevation = NeoElevation.ConcaveSmall,
                            shape = CircleShape
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.cv_logo),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = title,
                        color = NeoColors.OnBase,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
            if (subtitle != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = subtitle,
                        color = NeoColors.OnBaseMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F1EA, name = "brand + actions")
@Composable
private fun NeoTopBarBrandPreview() {
    CallNestTheme {
        NeoTopBar(
            title = "callNest",
            showBrand = true,
            modifier = Modifier.padding(8.dp),
            actions = {
                NeoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = {},
                    contentDescription = "Search"
                )
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F1EA, name = "with subtitle")
@Composable
private fun NeoTopBarSubtitlePreview() {
    CallNestTheme {
        NeoTopBar(
            title = "Calls",
            subtitle = "This week",
            navIcon = Icons.AutoMirrored.Filled.ArrowBack,
            modifier = Modifier.padding(8.dp)
        )
    }
}
