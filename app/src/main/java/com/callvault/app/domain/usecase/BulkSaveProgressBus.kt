package com.callvault.app.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Singleton bus that broadcasts progress while [BulkSaveContactsUseCase] is
 * running so the UI (e.g. `BulkSaveProgressDialog`) can render a determinate
 * progress bar without owning any business logic.
 */
@Singleton
class BulkSaveProgressBus @Inject constructor() {

    private val _events = MutableSharedFlow<BulkSaveProgress>(
        replay = 1,
        extraBufferCapacity = 64
    )

    /** Hot stream of bulk-save lifecycle events; replays the last value. */
    val events: SharedFlow<BulkSaveProgress> = _events.asSharedFlow()

    /** Non-suspending publish; drops if the buffer is full. */
    fun publish(event: BulkSaveProgress) {
        _events.tryEmit(event)
    }
}

/** State of a bulk-save run. */
sealed interface BulkSaveProgress {
    data object Idle : BulkSaveProgress

    data class Running(
        val current: Int,
        val total: Int,
        val lastError: String? = null
    ) : BulkSaveProgress

    data class Done(
        val savedCount: Int,
        val skippedCount: Int,
        val totalCount: Int,
        val firstError: String? = null
    ) : BulkSaveProgress
}
