package com.callvault.app.ui.screen.autotagrules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.domain.model.AutoTagRule
import com.callvault.app.domain.repository.AutoTagRuleRepository
import com.callvault.app.domain.usecase.ApplyAutoTagRulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import timber.log.Timber

/**
 * UI row exposing a rule plus its live "match count" badge value (against the
 * latest 200 calls).
 */
data class RuleRow(
    val rule: AutoTagRule,
    val matchCount: Int
)

/** Aggregate state for [AutoTagRulesScreen]. */
data class AutoTagRulesUiState(
    val rules: List<RuleRow> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

/**
 * Drives the auto-tag rules list screen — toggling, reorder, and snapshot
 * match-count refresh.
 */
@HiltViewModel
class AutoTagRulesViewModel @Inject constructor(
    private val repo: AutoTagRuleRepository,
    private val applyUseCase: ApplyAutoTagRulesUseCase
) : ViewModel() {

    private val matchCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())

    val state: StateFlow<AutoTagRulesUiState> =
        combine(repo.observeAll(), matchCounts) { rules, counts ->
            AutoTagRulesUiState(
                rules = rules.map { RuleRow(it, counts[it.id] ?: 0) },
                loading = false
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AutoTagRulesUiState())

    init {
        viewModelScope.launch { refreshMatchCounts() }
    }

    /** Recompute the match-count badges against the latest 200 calls. */
    fun refreshMatchCounts() {
        viewModelScope.launch {
            runCatching {
                val rules = repo.activeRules()
                val map = HashMap<Long, Int>()
                for (rule in rules) {
                    map[rule.id] = applyUseCase.previewMatchCount(rule)
                }
                matchCounts.value = map
            }.onFailure { Timber.w(it, "refreshMatchCounts failed") }
        }
    }

    fun setActive(rule: AutoTagRule, active: Boolean) {
        viewModelScope.launch {
            runCatching { repo.upsert(rule.copy(isActive = active)) }
        }
    }

    fun moveUp(rule: AutoTagRule) = reorder(rule, delta = -1)
    fun moveDown(rule: AutoTagRule) = reorder(rule, delta = 1)

    private fun reorder(rule: AutoTagRule, delta: Int) {
        viewModelScope.launch {
            val newOrder = (rule.sortOrder + delta).coerceAtLeast(0)
            runCatching { repo.upsert(rule.copy(sortOrder = newOrder)) }
        }
    }

    fun delete(rule: AutoTagRule) {
        viewModelScope.launch { runCatching { repo.delete(rule) } }
    }

    @Suppress("unused")
    fun newDraft(): AutoTagRule = AutoTagRule(
        id = 0L,
        name = "",
        isActive = true,
        sortOrder = 0,
        conditions = emptyList(),
        actions = emptyList(),
        createdAt = Clock.System.now()
    )
}
