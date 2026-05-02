package com.callvault.app.util

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Single article descriptor loaded from `assets/docs/{id}.md`. */
data class ArticleMeta(
    val id: String,
    val title: String,
    val excerpt: String
)

/** Full article body + metadata. */
data class ArticleContent(
    val id: String,
    val title: String,
    val markdown: String
)

/**
 * Sprint 11 — reads the in-app help articles bundled under `assets/docs/`.
 *
 * The list is derived from the asset directory at runtime so adding a new file
 * is enough — no manifest needed. Results are cached for the process lifetime.
 */
@Singleton
class AssetDocsLoader @Inject constructor() {

    @Volatile private var cache: List<ArticleMeta>? = null
    private val articleCache = mutableMapOf<String, ArticleContent>()

    /** Lists all available articles ordered by file name (which is numbered). */
    suspend fun listArticles(ctx: Context): List<ArticleMeta> = withContext(Dispatchers.IO) {
        cache?.let { return@withContext it }
        val files = runCatching { ctx.assets.list("docs")?.toList() }.getOrNull().orEmpty()
            .filter { it.endsWith(".md", ignoreCase = true) }
            .sorted()
        val out = files.map { fileName ->
            val id = fileName.removeSuffix(".md")
            val md = readAsset(ctx, "docs/$fileName")
            val title = firstH1(md) ?: id
            val excerpt = firstParagraph(md, skipTitle = title)
            ArticleMeta(id, title, excerpt)
        }
        cache = out
        out
    }

    /** Loads a single article (cached) by file id (without `.md` suffix). */
    suspend fun loadArticle(ctx: Context, id: String): ArticleContent? =
        withContext(Dispatchers.IO) {
            articleCache[id]?.let { return@withContext it }
            val md = runCatching { readAsset(ctx, "docs/$id.md") }.getOrNull()
                ?: return@withContext null
            val title = firstH1(md) ?: id
            ArticleContent(id, title, md).also { articleCache[id] = it }
        }

    private fun readAsset(ctx: Context, path: String): String =
        ctx.assets.open(path).bufferedReader().use { it.readText() }

    private fun firstH1(md: String): String? =
        md.lineSequence().firstOrNull { it.trimStart().startsWith("# ") }
            ?.trimStart()?.removePrefix("#")?.trim()

    private fun firstParagraph(md: String, skipTitle: String?): String =
        md.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .filter { skipTitle == null || it != skipTitle }
            .firstOrNull()
            .orEmpty()
            .take(160)
}
