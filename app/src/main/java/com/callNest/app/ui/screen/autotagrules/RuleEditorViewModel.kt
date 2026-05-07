package com.callNest.app.ui.screen.autotagrules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.domain.model.AutoTagRule
import com.callNest.app.domain.model.RuleAction
import com.callNest.app.domain.model.RuleCondition
import com.callNest.app.domain.model.Tag
import com.callNest.app.domain.repository.AutoTagRuleRepository
import com.callNest.app.domain.repository.TagRepository
import com.callNest.app.domain.usecase.ApplyAutoTagRulesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.callNest.app.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import timber.log.Timber

/** Editable working copy used by [RuleEditorScreen]. */
data class RuleDraft(
    val id: Long = 0L,
    val name: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val conditions: List<RuleCondition> = emptyList(),
    val actions: List<RuleAction> = emptyList(),
    val createdAt: kotlinx.datetime.Instant = Clock.System.now()
) {
    fun toRule(): AutoTagRule = AutoTagRule(
        id = id,
        name = name.ifBlank { "Untitled rule" },
        isActive = isActive,
        sortOrder = sortOrder,
        conditions = conditions,
        actions = actions,
        createdAt = createdAt
    )
}

/** UI state for [RuleEditorScreen]. */
data class RuleEditorUiState(
    val draft: RuleDraft = RuleDraft(),
    val previewMatchCount: Int = 0,
    val isLoading: Boolean = true,
    val saveError: String? = null,
    val saved: Boolean = false
)

/**
 * Drives the rule editor. Holds an in-memory [RuleDraft], debounces live
 * preview recomputation by 400 ms, and persists via [AutoTagRuleRepository].
 */
@HiltViewModel
class RuleEditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: AutoTagRuleRepository,
    private val applyUseCase: ApplyAutoTagRulesUseCase,
    tagRepository: TagRepository
) : ViewModel() {

    private val ruleId: Long = savedState[Destinations.RuleEditor.ARG_RULE_ID] ?: -1L

    private val _state = MutableStateFlow(RuleEditorUiState())
    val state: StateFlow<RuleEditorUiState> = _state.asStateFlow()

    val tags: StateFlow<List<Tag>> = tagRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var previewJob: Job? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (ruleId <= 0L) {
            _state.value = RuleEditorUiState(draft = RuleDraft(), isLoading = false)
            schedulePreview()
            return
        }
        runCatching {
            // Repo currently exposes activeRules() + observeAll(); fetch via observeAll first.
            val all = repo.observeAll().first()
            val match = all.firstOrNull { it.id == ruleId }
            if (match != null) {
                _state.value = RuleEditorUiState(draft = match.toDraft(), isLoading = false)
            } else {
                _state.value = RuleEditorUiState(draft = RuleDraft(id = ruleId), isLoading = false)
            }
            schedulePreview()
        }.onFailure {
            Timber.w(it, "RuleEditor: load failed")
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun setName(name: String) = update { it.copy(name = name) }
    fun setActive(active: Boolean) = update { it.copy(isActive = active) }
    fun addCondition(c: RuleCondition) = update { it.copy(conditions = it.conditions + c) }
    fun removeCondition(index: Int) = update {
        it.copy(conditions = it.conditions.toMutableList().apply { removeAt(index) })
    }
    fun updateCondition(index: Int, c: RuleCondition) = update {
        it.copy(conditions = it.conditions.toMutableList().apply { set(index, c) })
    }
    fun addAction(a: RuleAction) = update { it.copy(actions = it.actions + a) }
    fun removeAction(index: Int) = update {
        it.copy(actions = it.actions.toMutableList().apply { removeAt(index) })
    }
    fun updateAction(index: Int, a: RuleAction) = update {
        it.copy(actions = it.actions.toMutableList().apply { set(index, a) })
    }

    private fun update(transform: (RuleDraft) -> RuleDraft) {
        _state.value = _state.value.copy(draft = transform(_state.value.draft))
        schedulePreview()
    }

    private fun schedulePreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(400)
            runCatching {
                val count = applyUseCase.previewMatchCount(_state.value.draft.toRule())
                _state.value = _state.value.copy(previewMatchCount = count)
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            runCatching { repo.upsert(_state.value.draft.toRule()) }
                .onSuccess { _state.value = _state.value.copy(saved = true, saveError = null) }
                .onFailure {
                    Timber.w(it, "RuleEditor: save failed")
                    _state.value = _state.value.copy(
                        saveError = "Couldn't save the rule. Try again."
                    )
                }
        }
    }
}

private fun AutoTagRule.toDraft(): RuleDraft = RuleDraft(
    id = id,
    name = name,
    isActive = isActive,
    sortOrder = sortOrder,
    conditions = conditions,
    actions = actions,
    createdAt = createdAt
)
