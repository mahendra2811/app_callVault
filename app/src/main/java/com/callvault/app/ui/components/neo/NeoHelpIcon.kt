package com.callvault.app.ui.components.neo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.callvault.app.R
import com.callvault.app.ui.navigation.Destinations

/**
 * Tiny "?" icon button that deep-links into the matching docs article.
 *
 * Drop into any top-app-bar `actions` slot to give users one-tap access to
 * contextual help for that screen.
 *
 * @param articleId the file id under `assets/docs/` (without `.md`).
 */
@Composable
fun NeoHelpIcon(
    articleId: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    NeoIconButton(
        icon = Icons.AutoMirrored.Filled.HelpOutline,
        onClick = { navController.navigate(Destinations.DocsArticle.routeFor(articleId)) },
        contentDescription = stringResource(R.string.docs_help_icon_cd),
        size = 40.dp,
        modifier = modifier
    )
}
