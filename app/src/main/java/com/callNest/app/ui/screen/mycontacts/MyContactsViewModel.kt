package com.callNest.app.ui.screen.mycontacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.work.SyncScheduler
import com.callNest.app.domain.model.ContactMeta
import com.callNest.app.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for [MyContactsScreen].
 *
 * Exposes the merged list of every `ContactMeta` row that
 * `isInSystemContacts=true && isAutoSaved=false`, optionally filtered by a
 * search query against display name + normalized number.
 */
@HiltViewModel
class MyContactsViewModel @Inject constructor(
    contactRepository: ContactRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val state: StateFlow<MyContactsUiState> =
        combine(contactRepository.observeMyContacts(), _query) { contacts, q ->
            val filtered = if (q.isBlank()) contacts else {
                val needle = q.trim().lowercase()
                contacts.filter {
                    (it.displayName?.lowercase()?.contains(needle) == true) ||
                        it.normalizedNumber.contains(needle)
                }
            }
            // Hide auto-saved (Inquiries) — they live on the Inquiries screen.
            val visible = filtered.filterNot { it.isAutoSaved }
            MyContactsUiState(contacts = visible)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyContactsUiState()
        )

    fun setQuery(q: String) { _query.value = q }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            runCatching { syncScheduler.triggerOnce() }
            delay(800)
            _isRefreshing.value = false
        }
    }
}

/** Immutable state surface for [MyContactsScreen]. */
data class MyContactsUiState(
    val contacts: List<ContactMeta> = emptyList()
)
