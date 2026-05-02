package com.callvault.app.ui.screen.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.data.work.LeadScoreRecomputeWorker
import com.callvault.app.domain.model.LeadScoreWeights
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import timber.log.Timber

/** UI state for [LeadScoringSettingsScreen]. */
data class LeadScoringSettingsUiState(
    val enabled: Boolean = true,
    val weights: LeadScoreWeights = LeadScoreWeights.DEFAULT,
    val loading: Boolean = true
)

/**
 * Drives the lead-scoring settings screen — master toggle, six sliders, and
 * "Reset to defaults". Persists changes to [SettingsDataStore] with a 400 ms
 * debounce, then enqueues [LeadScoreRecomputeWorker].
 */
@HiltViewModel
class LeadScoringSettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val workingState = MutableStateFlow<LeadScoringSettingsUiState?>(null)

    val state: StateFlow<LeadScoringSettingsUiState> = combine(
        settings.leadScoreEnabled,
        settings.leadScoreWeights,
        workingState
    ) { enabled, weightsJson, working ->
        if (working != null) return@combine working
        val weights = if (weightsJson.isBlank()) LeadScoreWeights.DEFAULT
        else runCatching { json.decodeFromString(LeadScoreWeights.serializer(), weightsJson) }
            .getOrDefault(LeadScoreWeights.DEFAULT)
        LeadScoringSettingsUiState(enabled = enabled, weights = weights, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeadScoringSettingsUiState())

    private var saveJob: Job? = null

    fun setEnabled(enabled: Boolean) {
        val curr = workingState.value ?: state.value
        val next = curr.copy(enabled = enabled)
        workingState.value = next
        scheduleSave(next)
    }

    fun setWeights(transform: (LeadScoreWeights) -> LeadScoreWeights) {
        val curr = workingState.value ?: state.value
        val next = curr.copy(weights = transform(curr.weights))
        workingState.value = next
        scheduleSave(next)
    }

    fun resetDefaults() {
        val next = LeadScoringSettingsUiState(
            enabled = true,
            weights = LeadScoreWeights.DEFAULT,
            loading = false
        )
        workingState.value = next
        scheduleSave(next)
    }

    private fun scheduleSave(snapshot: LeadScoringSettingsUiState) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            runCatching {
                settings.setLeadScoreEnabled(snapshot.enabled)
                settings.setLeadScoreWeights(json.encodeToString(LeadScoreWeights.serializer(), snapshot.weights))
                LeadScoreRecomputeWorker.enqueue(appContext)
            }.onFailure { Timber.w(it, "LeadScoring save failed") }
        }
    }
}
