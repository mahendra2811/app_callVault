package com.callvault.app.ui.screen.pipeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callvault.app.data.local.dao.ContactMetaDao
import com.callvault.app.domain.model.PipelineStage
import com.callvault.app.domain.repository.PipelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A single lead card on the Kanban board. */
data class PipelineCard(
    val normalizedNumber: String,
    val displayName: String?,
    val leadScore: Int,
    val totalCalls: Int,
    val lastCallDate: Long,
    val stage: PipelineStage,
)

/** Cards bucketed by stage, in display order. */
data class PipelineBoard(val byStage: Map<PipelineStage, List<PipelineCard>>) {
    fun stageCount(stage: PipelineStage): Int = byStage[stage]?.size ?: 0
}

@HiltViewModel
class PipelineViewModel @Inject constructor(
    private val contactMetaDao: ContactMetaDao,
    private val callDao: com.callvault.app.data.local.dao.CallDao,
    private val pipeline: PipelineRepository,
    private val settings: com.callvault.app.data.prefs.SettingsDataStore,
    private val demoSeeder: com.callvault.app.data.demo.DemoSeeder,
) : ViewModel() {

    val demoActive: kotlinx.coroutines.flow.StateFlow<Boolean> =
        settings.demoSeedActive.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false,
        )

    fun clearDemo() {
        viewModelScope.launch { demoSeeder.clearDemo() }
    }

    val board: StateFlow<PipelineBoard> = combine(
        contactMetaDao.observeAll(),
        pipeline.stages,
    ) { contacts, stages ->
        val cards = contacts.map { c ->
            PipelineCard(
                normalizedNumber = c.normalizedNumber,
                displayName = c.displayName,
                leadScore = c.computedLeadScore,
                totalCalls = c.totalCalls,
                lastCallDate = c.lastCallDate,
                stage = stages[c.normalizedNumber] ?: PipelineStage.New,
            )
        }
        val grouped = PipelineStage.entries.associateWith { stage ->
            cards.filter { it.stage == stage }
        }
        PipelineBoard(grouped)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PipelineBoard(emptyMap()))

    fun moveTo(normalizedNumber: String, stage: PipelineStage) {
        viewModelScope.launch { pipeline.setStage(normalizedNumber, stage) }
    }

    /** Moves every contact with an overdue, undone follow-up to [PipelineStage.Lost]. Returns count. */
    suspend fun moveOverdueToLost(): Int {
        val now = System.currentTimeMillis()
        val overdueNumbers = callDao.observePendingFollowUps()
            .firstOrNull()
            .orEmpty()
            .filter { (it.followUpDate ?: Long.MAX_VALUE) < now }
            .map { it.normalizedNumber }
            .toSet()
        overdueNumbers.forEach { pipeline.setStage(it, PipelineStage.Lost) }
        return overdueNumbers.size
    }
}
