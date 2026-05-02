package com.callvault.app.ui.screen.docs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.callvault.app.R
import com.callvault.app.ui.components.neo.NeoButton
import com.callvault.app.ui.components.neo.NeoCard
import com.callvault.app.ui.components.neo.NeoTopBar
import com.callvault.app.ui.navigation.Destinations
import com.callvault.app.ui.screen.shared.NeoScaffold
import com.callvault.app.ui.screen.shared.StandardPage
import com.callvault.app.ui.theme.CallVaultTheme
import com.callvault.app.ui.theme.NeoColors
import com.callvault.app.ui.util.MarkdownText
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Sprint 11 — renders a single help article from `assets/docs/{id}.md`.
 *
 * Below the body, a "Was this helpful?" prompt records 👍/👎 to [DocFeedbackEntity]
 * for offline review. Selection is remembered for this composition only.
 */
@Composable
fun DocsArticleScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    vm: DocsViewModel = hiltViewModel()
) {
    val backStack by navController.currentBackStackEntryAsState()
    val articleId = backStack?.arguments?.getString(Destinations.DocsArticle.ARG_ARTICLE_ID).orEmpty()
    val current by vm.current.collectAsStateWithLifecycle()
    var rated by rememberSaveable(articleId) { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(articleId) { if (articleId.isNotBlank()) vm.open(articleId) }

    StandardPage(
        title = current?.title ?: " ",
        description = stringResource(R.string.cv_docs_article_description_fallback),
        emoji = "📖",
        onBack = { navController.popBackStack() }
    ) {
        val a = current
        if (a == null) {
            Text(
                text = "Loading…",
                color = NeoColors.OnBaseMuted,
                modifier = Modifier.padding(24.dp)
            )
            return@StandardPage
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            MarkdownText(source = a.markdown, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            NeoCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = stringResource(R.string.docs_helpful_q),
                        color = NeoColors.OnBase,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeoButton(
                            text =stringResource(R.string.docs_helpful_yes),
                            onClick = {
                                rated = true
                                vm.rate(a.id, true)
                            }
                        )
                        NeoButton(
                            text =stringResource(R.string.docs_helpful_no),
                            onClick = {
                                rated = false
                                vm.rate(a.id, false)
                            }
                        )
                    }
                    if (rated != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Thanks for the feedback.",
                            color = NeoColors.OnBaseMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE8E8EC, widthDp = 360, heightDp = 720)
@Composable
private fun DocsArticlePreview() {
    CallVaultTheme {
        DocsArticleScreen(navController = rememberNavController())
    }
}
