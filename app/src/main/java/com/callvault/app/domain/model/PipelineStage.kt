package com.callvault.app.domain.model

/** Five-stage sales funnel. Default for any contact without an explicit row is [New]. */
enum class PipelineStage {
    New,
    Contacted,
    Qualified,
    Won,
    Lost;

    companion object {
        fun fromKey(key: String): PipelineStage = entries.firstOrNull { it.name == key } ?: New
    }
}
