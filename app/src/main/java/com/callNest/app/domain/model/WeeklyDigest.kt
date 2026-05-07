package com.callNest.app.domain.model

/** A 7-day rollup of call activity, ready to render or share. */
data class WeeklyDigest(
    val fromMs: Long,
    val toMs: Long,
    val totalCalls: Int,
    val incoming: Int,
    val outgoing: Int,
    val missed: Int,
    val uniqueContacts: Int,
    val hotLeads: Int,
    val topCallers: List<TopCaller>,
    val topTags: List<TagCount> = emptyList(),
    /** Optional AI-generated narrative; empty when the user hasn't enabled the AI hook. */
    val aiNarrative: String? = null,
) {
    /** Top-N tag with application count in the window. */
    data class TagCount(val name: String, val count: Int)
    data class TopCaller(
        val normalizedNumber: String,
        val displayName: String?,
        val callCount: Int,
        val leadScore: Int,
    )
}
