package com.callvault.app.ui.screen.docs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.local.dao.DocFeedbackDao
import com.callvault.app.data.local.entity.DocFeedbackEntity
import com.callvault.app.util.ArticleContent
import com.callvault.app.util.ArticleMeta
import com.callvault.app.util.AssetDocsLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Drives the docs list + article screens (Sprint 11). */
@HiltViewModel
class DocsViewModel @Inject constructor(
    app: Application,
    private val loader: AssetDocsLoader,
    private val feedback: DocFeedbackDao
) : AndroidViewModel(app) {

    private val _articles = MutableStateFlow<List<ArticleMeta>>(emptyList())
    val articles: StateFlow<List<ArticleMeta>> = _articles.asStateFlow()

    private val _current = MutableStateFlow<ArticleContent?>(null)
    val current: StateFlow<ArticleContent?> = _current.asStateFlow()

    init { refresh() }

    /** (Re)loads the article list from assets. */
    fun refresh() {
        viewModelScope.launch {
            _articles.value = loader.listArticles(getApplication())
        }
    }

    /** Loads a single article into [current]. */
    fun open(id: String) {
        viewModelScope.launch {
            _current.value = loader.loadArticle(getApplication(), id)
        }
    }

    /** Records "was this helpful?" outcome. */
    fun rate(id: String, helpful: Boolean) {
        viewModelScope.launch {
            runCatching {
                feedback.insert(
                    DocFeedbackEntity(
                        articleId = id,
                        isHelpful = helpful
                    )
                )
            }
        }
    }
}
