package com.callvault.app.ui.components.neo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.theme.NeoElevation

/**
 * Top app bar for CallVault screens.
 *
 * @param title screen title rendered centered (left-aligned if [navIcon] absent
 *              and there are no actions)
 * @param navIcon optional navigation icon (typically back arrow)
 * @param onNavClick callback when the nav icon is tapped — required if [navIcon] is set
 * @param actions trailing icon buttons; the slot is a [Row] so callers can lay out multiple
 */
@Composable
fun NeoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navIcon: ImageVector? = null,
    onNavClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    NeoSurface(
        modifier = modifier.fillMaxWidth(),
        elevation = NeoElevation.Flat,
        shape = RoundedCornerShape(0.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (navIcon != null) {
                NeoIconButton(
                    icon = navIcon,
                    onClick = onNavClick,
                    contentDescription = "Back",
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
            Text(
                text = title,
                color = NeoColors.OnBase,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoTopBarPreview() {
    CallVaultTheme {
        NeoTopBar(
            title = "Calls",
            navIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}
