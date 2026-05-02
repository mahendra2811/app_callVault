package com.callvault.app.domain.model

import kotlinx.datetime.Instant

/**
 * Configuration the user assembles in the Export wizard (spec §6 export
 * steps). Concrete exporters live in `data/export/` and translate this into
 * Excel/CSV/PDF/JSON/vCard.
 */
data class ExportConfig(
    val format: Format,
    val dateFrom: Instant?,
    val dateTo: Instant?,
    val scope: Scope,
    val columns: List<Column>,
    val destinationUri: String?
) {
    enum class Format { EXCEL, CSV, PDF, JSON, VCARD }

    enum class Scope { ALL, FILTERED, BOOKMARKED, MY_CONTACTS, INQUIRIES }

    enum class Column {
        DATE,
        NUMBER,
        DISPLAY_NAME,
        TYPE,
        DURATION,
        SIM_SLOT,
        TAGS,
        NOTES,
        LEAD_SCORE,
        COUNTRY,
        GEO
    }
}
