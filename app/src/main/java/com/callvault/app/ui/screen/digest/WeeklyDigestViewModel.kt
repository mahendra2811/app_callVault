package com.callvault.app.ui.screen.digest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.ai.AnthropicClient
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.data.secrets.SecretStore
import com.callvault.app.domain.model.WeeklyDigest
import com.callvault.app.domain.usecase.ComputeWeeklyDigestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class WeeklyDigestViewModel @Inject constructor(
    private val computeDigest: ComputeWeeklyDigestUseCase,
    private val settings: SettingsDataStore,
    private val anthropic: AnthropicClient,
    private val secrets: SecretStore,
) : ViewModel() {

    private val _digest = MutableStateFlow<WeeklyDigest?>(null)
    val digest: StateFlow<WeeklyDigest?> = _digest.asStateFlow()

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    init {
        viewModelScope.launch { migrateLegacyKey() }
        refresh()
    }

    private suspend fun migrateLegacyKey() {
        val legacy = settings.anthropicApiKeyPlaintextLegacy.first()
        if (legacy.isNotBlank() && secrets.get(SecretStore.K_ANTHROPIC_KEY).isBlank()) {
            secrets.set(SecretStore.K_ANTHROPIC_KEY, legacy)
            settings.clearLegacyAnthropicKey()
            Timber.i("Migrated Anthropic key from plaintext to SecretStore")
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { computeDigest() }
                .onSuccess { d ->
                    _digest.value = d
                    if (settings.aiDigestEnabled.first()) {
                        generateNarrative(d)
                    }
                }
                .onFailure { Timber.w(it, "Compute digest failed") }
        }
    }

    fun regenerateNarrative() {
        val d = _digest.value ?: return
        viewModelScope.launch { generateNarrative(d) }
    }

    private suspend fun generateNarrative(d: WeeklyDigest) {
        val key = secrets.get(SecretStore.K_ANTHROPIC_KEY)
        if (key.isBlank()) return
        _aiBusy.value = true
        _aiError.value = null
        try {
            val result = anthropic.summarizeDigest(key, d)
            when (result) {
                is AnthropicClient.Result.Ok -> {
                    _digest.value = _digest.value?.copy(aiNarrative = result.text)
                }
                is AnthropicClient.Result.Failure -> {
                    _aiError.value = result.message
                }
            }
        } finally {
            _aiBusy.value = false
        }
    }
}
