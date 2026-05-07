package com.callNest.app.ui.screen.followups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.domain.model.Call
import com.callNest.app.domain.model.ContactMeta
import com.callNest.app.domain.repository.CallRepository
import com.callNest.app.domain.repository.ContactRepository
import com.callNest.app.domain.usecase.ScheduleFollowUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** A single follow-up row, joined with the contact display name. */
data class FollowUpRow(
    val call: Call,
    val displayName: String?,
    val triggerEpochMs: Long,
    val isDone: Boolean
)

/** Tab-bucketed follow-ups. */
data class FollowUpsUiState(
    val today: List<FollowUpRow> = emptyList(),
    val overdue: List<FollowUpRow> = emptyList(),
    val upcoming: List<FollowUpRow> = emptyList(),
    val done: List<FollowUpRow> = emptyList(),
    val selectedTab: FollowUpTab = FollowUpTab.TODAY,
    val errorMessage: String? = null
)

/** Tabs in display order. */
enum class FollowUpTab { TODAY, OVERDUE, UPCOMING, DONE }

/**
 * Drives the [FollowUpsScreen]. Reactively buckets every call with a
 * follow-up date into Today / Overdue / Upcoming / Completed using the
 * device's current local timezone.
 */
@HiltViewModel
class FollowUpsViewModel @Inject constructor(
    private val callRepo: CallRepository,
    private val contactRepo: ContactRepository,
    private val scheduleFollowUp: ScheduleFollowUpUseCase
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(FollowUpTab.TODAY)
    private val _error = MutableStateFlow<String?>(null)

    /** All calls with a non-null `followUpDate`, including done ones. */
    private val withFollowUpFlow = combine(
        callRepo.observePendingFollowUps(),
        callRepo.observeRecent(limit = 1000)
    ) { pending, recent ->
        // observePendingFollowUps excludes done — union with recent calls that have followUpDoneAt.
        val doneOnes = recent.filter { it.followUpDoneAt != null && it.followUpAt != null }
        (pending + doneOnes).distinctBy { it.systemId }
    }

    val state: StateFlow<FollowUpsUiState> = combine(
        withFollowUpFlow,
        contactRepo.observeAll(),
        _selectedTab,
        _error
    ) { calls, contacts, tab, err ->
        val byNumber: Map<String, ContactMeta> = contacts.associateBy { it.normalizedNumber }
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val today: LocalDate = now.toLocalDateTime(tz).date

        val rows = calls
            .filter { it.followUpAt != null }
            .map { c ->
                FollowUpRow(
                    call = c,
                    displayName = byNumber[c.normalizedNumber]?.displayName ?: c.cachedName,
                    triggerEpochMs = c.followUpAt!!.toEpochMilliseconds(),
                    isDone = c.followUpDoneAt != null
                )
            }

        val (doneRows, activeRows) = rows.partition { it.isDone }

        val todayBucket = activeRows.filter {
            val d = it.call.followUpAt!!.toLocalDateTime(tz).date
            d == today
        }.sortedBy { it.triggerEpochMs }

        val overdueBucket = activeRows.filter {
            val d = it.call.followUpAt!!.toLocalDateTime(tz).date
            d < today
        }.sortedBy { it.triggerEpochMs }

        val upcomingBucket = activeRows.filter {
            val d = it.call.followUpAt!!.toLocalDateTime(tz).date
            d > today
        }.sortedBy { it.triggerEpochMs }

        FollowUpsUiState(
            today = todayBucket,
            overdue = overdueBucket,
            upcoming = upcomingBucket,
            done = doneRows.sortedByDescending { it.triggerEpochMs },
            selectedTab = tab,
            errorMessage = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FollowUpsUiState())

    fun setTab(tab: FollowUpTab) { _selectedTab.value = tab }

    fun markDone(callSystemId: Long) {
        viewModelScope.launch {
            runCatching { scheduleFollowUp.markDone(callSystemId) }
                .onFailure { _error.value = "We couldn't mark that follow-up done." }
        }
    }

    fun cancel(callSystemId: Long) {
        viewModelScope.launch {
            runCatching { scheduleFollowUp.cancel(callSystemId) }
                .onFailure { _error.value = "We couldn't cancel that follow-up." }
        }
    }

    fun consumeError() { _error.value = null }
}
