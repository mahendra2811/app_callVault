package com.callvault.app.ui.navigation

import android.net.Uri

/**
 * Type-safe enumeration of every top-level navigation destination in CallVault.
 *
 * Each entry exposes a stable [route] string used as the Compose Navigation key.
 * Sub-routes (call detail, settings sub-screens, etc.) are added in later
 * sprints as the surface grows.
 */
sealed class Destinations(val route: String) {
    /** First-launch tour — 5 pages including permissions + first sync. */
    data object Onboarding : Destinations("onboarding")

    /** Calls tab home (Sprint 3). */
    data object Calls : Destinations("calls")

    /** Shown when a critical permission is missing post-onboarding. */
    data object PermissionRationale : Destinations("permission_rationale")

    /** Shown when a critical permission is permanently denied. */
    data object PermissionDenied : Destinations("permission_denied")

    /** Per-number call detail (Sprint 3). */
    data object CallDetail : Destinations("call_detail/{normalizedNumber}") {
        const val ARG_NUMBER = "normalizedNumber"
        fun routeFor(normalizedNumber: String): String =
            "call_detail/${Uri.encode(normalizedNumber)}"
    }

    /** Full-screen Search overlay (Sprint 3). */
    data object Search : Destinations("search")

    /** Filter presets manager (Sprint 3). */
    data object FilterPresets : Destinations("filter_presets")

    /** Sprint 5 — saved system contacts (excluding auto-saved inquiries). */
    data object MyContacts : Destinations("my_contacts")

    /** Sprint 5 — auto-saved inquiry bucket. */
    data object Inquiries : Destinations("inquiries")

    /** Sprint 5 — Auto-Save settings (prefix / SIM tag / suffix / group). */
    data object AutoSaveSettings : Destinations("settings/auto_save")

    /** Sprint 6 — Auto-tag rules manager (list of rules with reorder + toggle). */
    data object AutoTagRules : Destinations("auto_tag_rules")

    /**
     * Sprint 6 — Rule editor (conditions + actions + live preview).
     * Pass `-1L` via [routeFor] to create a new rule.
     */
    data object RuleEditor : Destinations("auto_tag_rules/edit/{ruleId}") {
        const val ARG_RULE_ID = "ruleId"
        fun routeFor(ruleId: Long): String = "auto_tag_rules/edit/$ruleId"
    }

    /** Sprint 6 — Lead scoring settings (master toggle + 6 sliders). */
    data object LeadScoringSettings : Destinations("settings/lead_scoring")

    /** Sprint 7 — Real-time features (floating bubble + post-call popup). */
    data object RealTimeSettings : Destinations("settings/real_time")

    /** Sprint 9 — Export wizard (format → date → scope → columns → destination). */
    data object Export : Destinations("export")

    /** Sprint 9 — Backup & restore landing screen. */
    data object Backup : Destinations("backup")

    /** Sprint 10 — Full-screen update available / progress / install. */
    data object UpdateAvailable : Destinations("update")

    /** Sprint 10 — Update settings (channel, auto-check, manual). */
    data object UpdateSettings : Destinations("settings/updates")

    /** Sprint 11 — Master Settings screen (progressive disclosure). */
    data object Settings : Destinations("settings")

    /** Sprint 11 — Docs/FAQ list. */
    data object DocsList : Destinations("docs")

    /** Sprint 11 — A single Docs article rendered from `assets/docs/{id}.md`. */
    data object DocsArticle : Destinations("docs/{articleId}") {
        const val ARG_ARTICLE_ID = "articleId"
        fun routeFor(articleId: String): String = "docs/${Uri.encode(articleId)}"
    }

    /** Sprint 11 — Home tab (greeting, today snapshot, quick links). */
    data object Home : Destinations("home")

    /** Sprint 11 — More tab (overflow of secondary surfaces). */
    data object More : Destinations("more")
}
