package com.callNest.app.ui.components.neo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.callNest.app.ui.theme.CallNestTheme
import com.callNest.app.ui.theme.NeoColors
import com.callNest.app.ui.theme.NeoElevation
import com.callNest.app.ui.theme.SageColors

/**
 * Single tab spec passed to [NeoTabBar].
 *
 * @param label visible text under the icon
 * @param icon material icon to render
 * @param badge optional badge count (null hides the badge)
 */
data class NeoTab(
    val label: String,
    val icon: ImageVector,
    val badge: Int? = null,
    /** Optional accent tint used when this tab is active. Defaults to AccentBlue. */
    val activeTint: Color? = null,
)

/**
 * Bottom navigation bar in neumorphic style.
 *
 * The whole bar is a convex floating surface; the active tab is highlighted
 * via a small concave pill behind its icon, leaving the rest of the row flat.
 */
@Composable
fun NeoTabBar(
    tabs: List<NeoTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NeoSurface(
        modifier = modifier.fillMaxWidth(),
        elevation = NeoElevation.ConvexMedium,
        shape = RoundedCornerShape(24.dp),
        color = SageColors.Surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val active = index == selectedIndex
                Column(
                    modifier = Modifier
                        .clickable { onSelect(index) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (active) (tab.activeTint ?: SageColors.Sage) else SageColors.TextTertiary
                            )
                        }
                        if (tab.badge != null && tab.badge > 0) {
                            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                                NeoBadge(count = tab.badge)
                            }
                        }
                    }
                    Text(
                        text = tab.label,
                        color = if (active) (tab.activeTint ?: SageColors.Sage) else SageColors.TextTertiary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC)
@Composable
private fun NeoTabBarPreview() {
    CallNestTheme {
        NeoTabBar(
            tabs = listOf(
                NeoTab("Home", Icons.Filled.Home),
                NeoTab("Calls", Icons.Filled.Call),
                NeoTab("My Contacts", Icons.Filled.Person),
                NeoTab("Inquiries", Icons.Filled.Inbox, badge = 4),
                NeoTab("More", Icons.Filled.MoreHoriz)
            ),
            selectedIndex = 1,
            onSelect = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
