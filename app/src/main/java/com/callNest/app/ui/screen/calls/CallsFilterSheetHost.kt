package com.callNest.app.ui.screen.calls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.callNest.app.domain.model.FilterState
import com.callNest.app.domain.model.Tag
import com.callNest.app.domain.repository.CallRepository
import com.callNest.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

/**
 * Tiny VM that exists solely to expose Hilt-injected repositories to the
 * filter sheet composable. The sheet itself stays stateless and pure.
 */
@HiltViewModel
class FilterSheetHostViewModel @Inject constructor(
    val callRepo: CallRepository,
    tagRepo: TagRepository
) : ViewModel() {
    val tags: StateFlow<List<Tag>> =
        tagRepo.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}

/**
 * Hilt-injecting wrapper around [CallsFilterSheet]. Keeps the sheet itself
 * dependency-free and previewable.
 */
@Composable
fun CallsFilterSheetHost(
    initial: FilterState,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
    viewModel: FilterSheetHostViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsState()
    CallsFilterSheet(
        initial = initial,
        availableTags = tags,
        callRepo = viewModel.callRepo,
        onDismiss = onDismiss,
        onApply = onApply,
        onSavePreset = { /* persisting presets to FilterPresetEntity — Sprint 3.1 */ }
    )
}
