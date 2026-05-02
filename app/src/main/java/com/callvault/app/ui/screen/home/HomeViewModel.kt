package com.callvault.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.domain.model.CallType
import com.callvault.app.domain.repository.CallRepository
import com.callvault.app.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * Backs the redesigned [HomeScreen]. Surfaces today's call counters, recent
 * unsaved numbers and the count of follow-ups due today.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val callRepo: CallRepository,
    @Suppress("UNUSED_PARAMETER") contactRepo: ContactRepository,
) : ViewModel() {

    /** Snapshot rendered by Home. */
    data class HomeUiState(
        val callsToday: Int = 0,
        val missedToday: Int = 0,
        val unsavedTotal: Int = 0,
        val followUpsDue: Int = 0,
        val recentUnsaved: List<RecentUnsavedItem> = emptyList(),
        val loading: Boolean = true,
    )

    /** Compact row spec for the "Recent unsaved" list on Home. */
    data class RecentUnsavedItem(
        val normalizedNumber: String,
        val lastCallEpochMs: Long
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = combine(
        callRepo.observeRecent(limit = 200),
        callRepo.observeUnsavedLast7Days()
    ) { recent, unsaved ->
        val tz = TimeZone.currentSystemDefault()
        val nowInstant = Clock.System.now()
        val today = nowInstant.toLocalDateTime(tz).date
        val startOfDay = today.atStartOfDayIn(tz)
        val callsToday = recent.filter { it.date >= startOfDay }
        val missedToday = callsToday.count { it.type == CallType.MISSED }
        val followUpsDue = recent.count { c ->
            val ts = c.followUpAt
            ts != null && c.followUpDoneAt == null &&
                ts.toLocalDateTime(tz).date == today
        }
        val grouped = unsaved
            .groupBy { it.normalizedNumber }
            .map { (num, calls) ->
                RecentUnsavedItem(
                    normalizedNumber = num,
                    lastCallEpochMs = calls.maxOf { it.date.toEpochMilliseconds() }
                )
            }
            .sortedByDescending { it.lastCallEpochMs }

        HomeUiState(
            callsToday = callsToday.size,
            missedToday = missedToday,
            unsavedTotal = grouped.size,
            followUpsDue = followUpsDue,
            recentUnsaved = grouped.take(3),
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

}
