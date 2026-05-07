package com.callNest.app.ui.screen.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.domain.model.MessageTemplate
import com.callNest.app.domain.repository.MessageTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Drives the Templates manage screen and the quick-reply sheet. */
@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repo: MessageTemplateRepository,
) : ViewModel() {

    val templates: StateFlow<List<MessageTemplate>> = repo.templates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(label: String, body: String) {
        viewModelScope.launch { repo.add(label, body) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }
}
