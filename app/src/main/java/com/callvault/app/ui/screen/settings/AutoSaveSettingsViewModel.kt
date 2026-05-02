package com.callvault.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.prefs.SettingsDataStore
import com.callvault.app.data.system.ContactGroupManager
import com.callvault.app.domain.usecase.AutoSaveNameBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State + persistence orchestration for [AutoSaveSettingsScreen].
 *
 * Each control writes back through a debounced (400ms) coroutine job so a
 * fast-typing user doesn't trigger one DataStore write per keystroke.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class AutoSaveSettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    private val groupManager: ContactGroupManager
) : ViewModel() {

    // ---- Local mutable state mirrors (debounced into DataStore) ----
    private val _enabled = MutableStateFlow(true)
    private val _prefix = MutableStateFlow("callVault")
    private val _includeSimTag = MutableStateFlow(true)
    private val _suffix = MutableStateFlow("")
    private val _groupName = MutableStateFlow("CallVault Inquiries")
    private val _phoneLabel = MutableStateFlow("Mobile")
    private val _phoneLabelCustom = MutableStateFlow("")
    private val _region = MutableStateFlow("IN")

    val state: StateFlow<AutoSaveSettingsUiState> = combine(
        _enabled,
        _prefix,
        _includeSimTag,
        _suffix
    ) { enabled, prefix, includeSim, suffix ->
        Quad(enabled, prefix, includeSim, suffix)
    }.let { upper ->
        combine(
            upper,
            _groupName,
            _phoneLabel,
            _phoneLabelCustom,
            _region
        ) { u, group, label, custom, region ->
            AutoSaveSettingsUiState(
                enabled = u.a,
                prefix = u.b,
                includeSimTag = u.c,
                suffix = u.d,
                groupName = group,
                phoneLabel = label,
                phoneLabelCustom = custom,
                region = region,
                preview = AutoSaveNameBuilder.build(
                    prefix = u.b,
                    includeSimTag = u.c,
                    simSlot = 0,
                    suffix = u.d,
                    normalizedNumber = "+919876543210"
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AutoSaveSettingsUiState()
    )

    init {
        // Hydrate from DataStore once.
        viewModelScope.launch {
            _enabled.value = settings.autoSaveEnabled.first()
            _prefix.value = settings.autoSavePrefix.first()
            _includeSimTag.value = settings.autoSaveIncludeSimTag.first()
            _suffix.value = settings.autoSaveSuffix.first()
            _groupName.value = settings.autoSaveContactGroupName.first()
            _phoneLabel.value = settings.autoSavePhoneLabel.first()
            _phoneLabelCustom.value = settings.autoSavePhoneLabelCustom.first()
            _region.value = settings.defaultRegion.first()
        }
        // Debounced persistence on each field. StateFlow is already distinct.
        wire(_enabled) { settings.setAutoSaveEnabled(it) }
        wire(_prefix) { settings.setAutoSavePrefix(it) }
        wire(_includeSimTag) { settings.setAutoSaveIncludeSimTag(it) }
        wire(_suffix) { settings.setAutoSaveSuffix(it) }
        wire(_groupName) { settings.setAutoSaveContactGroupName(it) }
        wire(_phoneLabel) { settings.setAutoSavePhoneLabel(it) }
        wire(_phoneLabelCustom) { settings.setAutoSavePhoneLabelCustom(it) }
        wire(_region) { settings.setDefaultRegion(it) }
    }

    private fun <T> wire(
        flow: kotlinx.coroutines.flow.Flow<T>,
        write: suspend (T) -> Unit
    ) {
        flow.debounce(DEBOUNCE_MS)
            .onEach { write(it) }
            .launchIn(viewModelScope)
    }

    fun setEnabled(v: Boolean) { _enabled.value = v }
    fun setPrefix(v: String) { _prefix.value = v }
    fun setIncludeSimTag(v: Boolean) { _includeSimTag.value = v }
    fun setSuffix(v: String) { _suffix.value = v }
    fun setGroupName(v: String) { _groupName.value = v }
    fun setPhoneLabel(v: String) { _phoneLabel.value = v }
    fun setPhoneLabelCustom(v: String) { _phoneLabelCustom.value = v }
    fun setRegion(v: String) { _region.value = v }

    /**
     * Apply group name change immediately — renames the existing system group
     * if its id is known, else creates a new one.
     */
    fun applyGroupName() {
        viewModelScope.launch {
            val name = _groupName.value
            val id = settings.autoSaveContactGroupId.first()
            if (id > 0L) {
                groupManager.renameGroup(id, name)
            } else {
                groupManager.ensureGroup(name)
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/** Immutable state surface for [AutoSaveSettingsScreen]. */
data class AutoSaveSettingsUiState(
    val enabled: Boolean = true,
    val prefix: String = "callVault",
    val includeSimTag: Boolean = true,
    val suffix: String = "",
    val groupName: String = "CallVault Inquiries",
    val phoneLabel: String = "Mobile",
    val phoneLabelCustom: String = "",
    val region: String = "IN",
    val preview: String = "callVault-s1 +919876543210"
)
