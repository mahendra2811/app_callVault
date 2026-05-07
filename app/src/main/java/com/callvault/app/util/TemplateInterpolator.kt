package com.callvault.app.util

/** Replaces `{name}` and `{firstName}` tokens; falls back to [fallback] when display name is blank. */
object TemplateInterpolator {
    fun interpolate(body: String, displayName: String?, fallback: String = ""): String {
        val name = displayName?.takeIf { it.isNotBlank() } ?: fallback
        val first = name.split(' ').firstOrNull()?.takeIf { it.isNotBlank() } ?: fallback
        return body.replace("{name}", name).replace("{firstName}", first)
    }
}
