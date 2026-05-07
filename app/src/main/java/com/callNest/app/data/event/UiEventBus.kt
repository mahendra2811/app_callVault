package com.callNest.app.data.event

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide bus for transient, user-facing events such as snackbar messages
 * raised by background pipelines (sync, auto-save, bulk save).
 *
 * Producers call [emit]; consumers (typically a hosting screen) collect
 * [events] and surface them via a snackbar host.
 *
 * Every message must be actionable per spec §13.
 */
@Singleton
class UiEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )

    /** Hot stream of UI events. */
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    /** Non-suspending emit; drops the event if subscribers are slow. */
    fun emit(event: UiEvent) {
        _events.tryEmit(event)
    }
}

/**
 * Discriminated union of UI events surfaced by the bus. Kept narrow on
 * purpose — add cases here as new producers come online.
 */
sealed interface UiEvent {
    /** A short snackbar message. */
    data class Snackbar(val message: String) : UiEvent

    /** A snackbar with an action label + opaque action key consumers can match. */
    data class SnackbarWithAction(
        val message: String,
        val actionLabel: String,
        val actionKey: String
    ) : UiEvent
}
