package com.callNest.app.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.domain.usecase.SyncCallLogUseCase
import com.callNest.app.domain.usecase.SyncProgress
import com.callNest.app.domain.usecase.SyncProgressBus
import com.callNest.app.util.PermissionManager
import com.callNest.app.util.PermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Aggregate UI state for the 5-page onboarding flow.
 *
 * @param currentPage zero-indexed page the pager is parked on.
 * @param permissionState mirror of [PermissionManager.state] for direct access in composables.
 * @param firstSyncProgress current "imported so far" count.
 * @param firstSyncTotal total rows the sync expects to process; `0` while indeterminate.
 * @param firstSyncDone `true` once the first sync ran (success or partial).
 * @param firstSyncError optional user-facing error if the sync threw.
 */
data class OnboardingUiState(
    val currentPage: Int = 0,
    val permissionState: PermissionState = PermissionState(),
    val firstSyncProgress: Int = 0,
    val firstSyncTotal: Int = 0,
    val firstSyncDone: Boolean = false,
    val firstSyncError: String? = null
)

/**
 * Hilt-injected view-model that drives the onboarding flow.
 *
 * Holds the page index, exposes the live [PermissionManager] state, and runs
 * the first-time sync in the background while the [FirstSyncPage] is visible.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val syncCallLogUseCase: SyncCallLogUseCase,
    private val syncProgressBus: SyncProgressBus,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Total number of pages in the pager. */
    val pageCount: Int = 5

    init {
        // Mirror the PermissionManager's reactive state into our own UI state.
        viewModelScope.launch {
            permissionManager.state.collect { perms ->
                _uiState.update { it.copy(permissionState = perms) }
            }
        }
        // Mirror sync progress events while the user is on the FirstSync page.
        viewModelScope.launch {
            syncProgressBus.events.collect { evt ->
                when (evt) {
                    SyncProgress.Started -> _uiState.update {
                        it.copy(firstSyncProgress = 0, firstSyncTotal = 0, firstSyncError = null)
                    }
                    is SyncProgress.Progress -> _uiState.update {
                        it.copy(firstSyncProgress = evt.current, firstSyncTotal = evt.total)
                    }
                    is SyncProgress.Done -> _uiState.update {
                        it.copy(
                            firstSyncProgress = evt.insertedCount,
                            firstSyncTotal = evt.totalCount,
                            firstSyncDone = true,
                            firstSyncError = null
                        )
                    }
                    is SyncProgress.Error -> _uiState.update {
                        it.copy(firstSyncDone = false, firstSyncError = evt.message)
                    }
                }
            }
        }
    }

    /** Advance to the next page, clamped to the last index. */
    fun next() {
        _uiState.update {
            it.copy(currentPage = (it.currentPage + 1).coerceAtMost(pageCount - 1))
        }
    }

    /** Move back one page, clamped to 0. */
    fun back() {
        _uiState.update { it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(0)) }
    }

    /** Force-jump to a specific page (e.g. from a deep link). */
    fun goTo(page: Int) {
        _uiState.update { it.copy(currentPage = page.coerceIn(0, pageCount - 1)) }
    }

    /** Re-read permissions after a system dialog finishes. */
    fun markPermissionsRequested() {
        permissionManager.recheckAll()
    }

    /**
     * Kicks off the very first sync. Safe to call repeatedly — if a sync is
     * already in flight the use case will short-circuit to a no-op.
     */
    fun startFirstSync() {
        // Reset error so the page can swap from error → progress smoothly.
        _uiState.update { it.copy(firstSyncError = null, firstSyncDone = false) }
        viewModelScope.launch {
            try {
                syncCallLogUseCase.invoke()
                // Done is also broadcast via the bus — but if the bus dropped
                // the event we still mark completion here as a safety net.
                _uiState.update { it.copy(firstSyncDone = true) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        firstSyncError = "Couldn't finish the first import. Try again.",
                        firstSyncDone = false
                    )
                }
            }
        }
    }

    /** Persist that onboarding finished so the NavHost can route past it. */
    fun complete() {
        viewModelScope.launch {
            settings.setOnboardingComplete(true)
        }
    }
}
