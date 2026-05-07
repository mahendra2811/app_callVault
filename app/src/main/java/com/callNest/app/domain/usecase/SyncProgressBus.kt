package com.callNest.app.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide bus that broadcasts sync progress events.
 *
 * Lives outside [SyncCallLogUseCase] so the use-case stays focused on the
 * §8.1 algorithm and other consumers (onboarding, settings) can subscribe
 * without coupling to the use-case lifecycle.
 *
 * Sprint 2 wires this only for the onboarding "first sync" UX. Later
 * sprints can extend [SyncProgress] without breaking this contract.
 */
@Singleton
class SyncProgressBus @Inject constructor() {

    private val _events = MutableSharedFlow<SyncProgress>(
        replay = 1,
        extraBufferCapacity = 16
    )

    /** Hot stream of sync lifecycle events; replays the last value. */
    val events: SharedFlow<SyncProgress> = _events.asSharedFlow()

    /** Emit without suspending — drops the event if the buffer is full. */
    fun publish(event: SyncProgress) {
        _events.tryEmit(event)
    }
}

/**
 * High-level sync lifecycle states. Kept deliberately small for Sprint 2;
 * [Progress.current] / [Progress.total] are advisory and may be `0` if the
 * underlying source can't compute totals up-front.
 */
sealed interface SyncProgress {
    /** Sync just kicked off; totals not yet known. */
    data object Started : SyncProgress

    /** Incremental progress update. [total] of 0 means indeterminate. */
    data class Progress(val current: Int, val total: Int) : SyncProgress

    /** Sync finished successfully. */
    data class Done(val insertedCount: Int, val totalCount: Int) : SyncProgress

    /** Sync failed; [message] is user-facing per spec §13. */
    data class Error(val message: String) : SyncProgress
}
