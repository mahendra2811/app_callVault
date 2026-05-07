package com.callvault.app.ui.navigation

import android.net.Uri

/**
 * Type-safe enumeration of every top-level navigation destination in CallVault.
 *
 * Each entry exposes a stable [route] string used as the Compose Navigation key.
 */
sealed class Destinations(val route: String) {
    /** Tabbed surface — hosts Home/Calls/Inquiries/More inside a single Scaffold. */
    data object Main : Destinations("main")

    /** Splash route — populated in Phase C. */
    data object Splash : Destinations("splash")

    /** Email/password login — gates the entire app when there is no Supabase session. */
    data object Login : Destinations("login")

    /** First-launch tour — 5 pages including permissions + first sync. */
    data object Onboarding : Destinations("onboarding")

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

    /** Sprint 9 — Export wizard. */
    data object Export : Destinations("export")

    /** Sprint 9 — Backup & restore landing screen. */
    data object Backup : Destinations("backup")

    /** Sprint 10 — Full-screen update available / progress / install. */
    data object UpdateAvailable : Destinations("update")

    /** Sprint 10 — Update settings (channel, auto-check, manual). */
    data object UpdateSettings : Destinations("settings/updates")

    /** Sprint 11 — Master Settings screen (progressive disclosure). */
    data object Settings : Destinations("settings")

    /** Quick-reply templates manager (built-ins + user-added). */
    data object Templates : Destinations("settings/templates")

    /** Lead pipeline Kanban (5-column funnel). */
    data object Pipeline : Destinations("pipeline")

    /** CSV contact import. */
    data object CsvImport : Destinations("csv_import")

    /** Weekly digest screen. */
    data object WeeklyDigest : Destinations("weekly_digest")

    /** Sprint 11 — Docs/FAQ list. */
    data object DocsList : Destinations("docs")

    /** Sprint 11 — A single Docs article rendered from `assets/docs/{id}.md`. */
    data object DocsArticle : Destinations("docs/{articleId}") {
        const val ARG_ARTICLE_ID = "articleId"
        fun routeFor(articleId: String): String = "docs/${Uri.encode(articleId)}"
    }

    /** Tag library manager. */
    data object Tags : Destinations("tags")

    /** Stats dashboard. */
    data object Stats : Destinations("stats")
}

/**
 * Routes for the four bottom-nav tabs hosted by [MainScaffold].
 *
 * Labels are hardcoded for Phase B; Phase E migrates them to string resources.
 */
sealed class MainTabRoute(
    val route: String,
    val label: String,
    val emoji: String
) {
    data object Home : MainTabRoute("home", "Home", "🏠")
    data object Calls : MainTabRoute("calls", "Calls", "📞")
    data object Pipeline : MainTabRoute("pipeline_tab", "Pipeline", "📊")
    data object Inquiries : MainTabRoute("inquiries", "Inquiries", "📥")
    data object More : MainTabRoute("more", "More", "☰")
}
