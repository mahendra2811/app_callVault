package com.callNest.app.domain.usecase

import com.callNest.app.data.local.dao.RuleScoreBoostDao
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.domain.model.ContactMeta
import com.callNest.app.domain.model.LeadScore
import com.callNest.app.domain.model.LeadScoreWeights
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Implements spec §8.2 Lead score formula.
 *
 * ```
 * score = normalize(callCount, 50)         * weightFreq
 *       + normalize(totalDurationSec, 7200) * weightDuration
 *       + recencyDecay(lastCallDate)        * weightRecency
 *       + (hasFollowUp        ? bonusFollowUp        : 0)
 *       + (customerTagApplied ? bonusCustomerTag     : 0)
 *       + (savedInRealContacts? bonusSavedContact    : 0)
 *       + sum(ruleBoosts)
 * ```
 *
 * Weights and bonuses are loaded from [SettingsDataStore.leadScoreWeights]
 * (JSON-encoded [LeadScoreWeights]) at every invocation; if the JSON is
 * blank or invalid, [LeadScoreWeights.DEFAULT] is used.
 *
 * If the `ContactMeta`-bound call carries a `leadScoreManualOverride`,
 * that value is returned (clamped to 0..100) and the manual flag is set on
 * the [LeadScore] result so callers can render the "Manual" badge.
 */
@Singleton
class ComputeLeadScoreUseCase @Inject constructor(
    private val settings: SettingsDataStore,
    private val ruleScoreBoostDao: RuleScoreBoostDao
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Compute the score for [meta]. Suspend so we can read the latest
     * weights JSON and the per-number rule boost total without forcing the
     * caller to plumb them.
     */
    suspend operator fun invoke(
        meta: ContactMeta,
        hasFollowUp: Boolean = false,
        customerTagApplied: Boolean = false,
        manualOverride: Int? = null,
        nowMs: Long = Clock.System.now().toEpochMilliseconds()
    ): LeadScore {
        val weights = loadWeights()
        val boosts = runCatching { ruleScoreBoostDao.totalForNumber(meta.normalizedNumber) }
            .onFailure { Timber.w(it, "ruleScoreBoostDao.totalForNumber failed") }
            .getOrDefault(0)
        return compute(
            meta = meta,
            weights = weights,
            hasFollowUp = hasFollowUp,
            customerTagApplied = customerTagApplied,
            ruleBoosts = boosts,
            manualOverride = manualOverride,
            nowMs = nowMs
        )
    }

    /** Pure variant — useful for testing and for the live preview. */
    fun compute(
        meta: ContactMeta,
        weights: LeadScoreWeights = LeadScoreWeights.DEFAULT,
        hasFollowUp: Boolean = false,
        customerTagApplied: Boolean = false,
        ruleBoosts: Int = 0,
        manualOverride: Int? = null,
        nowMs: Long = Clock.System.now().toEpochMilliseconds()
    ): LeadScore {
        val freq = normalize(meta.totalCalls.toDouble(), MAX_CALL_COUNT) * weights.weightFreq
        val dur = normalize(meta.totalDuration.toDouble(), MAX_DURATION_SEC) * weights.weightDuration
        val rec = recencyDecay(meta.lastCallDate.toEpochMilliseconds(), nowMs) * weights.weightRecency

        val followBonus = if (hasFollowUp) weights.bonusFollowUp else 0
        val customerBonus = if (customerTagApplied) weights.bonusCustomerTag else 0
        val savedBonus = if (meta.isInSystemContacts) weights.bonusSavedContact else 0

        val raw = freq + dur + rec + followBonus + customerBonus + savedBonus + ruleBoosts
        val computed = raw.toInt().coerceIn(0, 100)
        val total = manualOverride?.coerceIn(0, 100) ?: computed

        return LeadScore(
            total = total,
            frequency = freq,
            duration = dur,
            recency = rec,
            followUpBonus = followBonus,
            customerTagBonus = customerBonus,
            savedContactBonus = savedBonus,
            ruleBoosts = ruleBoosts,
            manualOverride = manualOverride
        )
    }

    private suspend fun loadWeights(): LeadScoreWeights {
        val raw = runCatching { settings.leadScoreWeights.first() }.getOrNull().orEmpty()
        if (raw.isBlank()) return LeadScoreWeights.DEFAULT
        return runCatching { json.decodeFromString(LeadScoreWeights.serializer(), raw) }
            .onFailure { Timber.w(it, "Failed to parse lead-score weights JSON; using defaults.") }
            .getOrDefault(LeadScoreWeights.DEFAULT)
    }

    private fun normalize(value: Double, max: Double): Double =
        ((value / max).coerceAtMost(1.0)) * 100.0

    /** `100 * exp(-daysSince / 14)` per spec §8.2 (~10-day half-life). */
    private fun recencyDecay(lastCallMs: Long, nowMs: Long): Double {
        val daysSince = ((nowMs - lastCallMs).coerceAtLeast(0L)) / DAY_MS.toDouble()
        return 100.0 * exp(-daysSince / 14.0)
    }

    private companion object {
        const val MAX_CALL_COUNT = 50.0
        const val MAX_DURATION_SEC = 7200.0
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
