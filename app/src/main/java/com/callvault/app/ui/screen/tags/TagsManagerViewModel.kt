package com.callvault.app.ui.screen.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.domain.model.Tag
import com.callvault.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** UI state for the [TagsManagerScreen]. */
data class TagsManagerUiState(
    val rows: List<TagRow> = emptyList(),
    val errorMessage: String? = null
)

/** A single row in the manager — tag plus how many calls reference it. */
data class TagRow(
    val tag: Tag,
    val usageCount: Int
)

/**
 * Drives the tags manager screen.
 *
 * Combines the tag list with the usage-count flow so the badge stays in sync
 * with cross-ref edits made from anywhere in the app.
 */
@HiltViewModel
class TagsManagerViewModel @Inject constructor(
    private val tagRepo: TagRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val state: StateFlow<TagsManagerUiState> = combine(
        tagRepo.observeAll(),
        tagRepo.observeUsageCounts(),
        _error
    ) { tags, counts, err ->
        TagsManagerUiState(
            rows = tags.map { TagRow(tag = it, usageCount = counts[it.id] ?: 0) },
            errorMessage = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagsManagerUiState())

    fun upsert(tag: Tag) {
        viewModelScope.launch {
            runCatching { tagRepo.upsert(tag) }
                .onFailure { _error.value = "We couldn't save that tag." }
        }
    }

    fun delete(tag: Tag) {
        if (tag.isSystem) {
            _error.value = "System tags can't be deleted."
            return
        }
        viewModelScope.launch {
            runCatching { tagRepo.delete(tag) }
                .onFailure { _error.value = "We couldn't delete that tag." }
        }
    }

    fun merge(source: Tag, targetId: Long) {
        viewModelScope.launch {
            runCatching { tagRepo.mergeInto(source.id, targetId) }
                .onFailure { _error.value = "We couldn't merge those tags." }
        }
    }

    fun consumeError() { _error.value = null }
}
