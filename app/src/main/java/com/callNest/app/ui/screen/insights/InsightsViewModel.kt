package com.callNest.app.ui.screen.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.dao.ContactMetaDao
import com.callNest.app.domain.model.DateRange
import com.callNest.app.domain.model.StatsSnapshot
import com.callNest.app.domain.usecase.ComputeStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Drives the Insights dashboard. Re-aggregates the same numbers
 * [DailySummaryWorker] sends in its notification, plus the 7-day
 * [StatsSnapshot] for charts and leaderboards.
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val callDao: CallDao,
    private val contactMetaDao: ContactMetaDao,
    private val computeStats: ComputeStatsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(InsightsUiState(loading = true))
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    init { refresh(userInitiated = false) }

    fun refresh(userInitiated: Boolean = true) {
        viewModelScope.launch {
            _state.update {
                if (userInitiated) it.copy(isRefreshing = true, error = null)
                else it.copy(loading = true, error = null)
            }
            try {
                val (todayFrom, todayTo) = todayWindow()
                val total = callDao.totalCount(todayFrom, todayTo)
                val missed = callDao.missedCount(todayFrom, todayTo)
                val unsaved = contactMetaDao.unsavedCountInRange(todayFrom, todayTo)
                val followUps = callDao.followUpsDueCount(todayFrom, todayTo)
                val sevenDay = computeStats(DateRange.last7Days())

                _state.update {
                    it.copy(
                        loading = false,
                        isRefreshing = false,
                        error = null,
                        today = TodayMetrics(total, missed, unsaved, followUps),
                        sevenDay = sevenDay
                    )
                }
            } catch (t: Throwable) {
                Timber.w(t, "InsightsViewModel.refresh failed")
                _state.update {
                    it.copy(
                        loading = false,
                        isRefreshing = false,
                        error = t.message ?: "Couldn't load insights."
                    )
                }
            }
        }
    }

    private fun todayWindow(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = start + 24L * 60 * 60 * 1000 - 1
        return start to end
    }
}

data class TodayMetrics(
    val totalCalls: Int,
    val missed: Int,
    val unsaved: Int,
    val followUpsDue: Int
)

data class InsightsUiState(
    val loading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val today: TodayMetrics = TodayMetrics(0, 0, 0, 0),
    val sevenDay: StatsSnapshot? = null
)
