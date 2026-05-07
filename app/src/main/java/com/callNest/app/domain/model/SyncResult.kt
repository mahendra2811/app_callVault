package com.callNest.app.domain.model

/**
 * Outcome of a single sync run. Returned by `SyncCallLogUseCase` and
 * surfaced to UI as a transient toast/snackbar.
 */
sealed interface SyncResult {

    /** All available rows imported successfully. */
    data class Success(
        val insertedCount: Int,
        val totalCount: Int
    ) : SyncResult

    /**
     * Fatal failure — nothing was imported. [reason] is a user-friendly
     * message ready to render directly per spec §13.
     */
    data class Failure(val reason: String) : SyncResult

    /**
     * Some rows imported, some failed. Workers should still treat this as a
     * success outcome (no retry); the UI may show a soft warning.
     */
    data class PartialSuccess(
        val insertedCount: Int,
        val skippedCount: Int,
        val totalCount: Int,
        val firstErrorMessage: String?
    ) : SyncResult
}
