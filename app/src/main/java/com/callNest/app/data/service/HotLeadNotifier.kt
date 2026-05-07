package com.callNest.app.data.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.callNest.app.CallNestApp
import com.callNest.app.MainActivity
import com.callNest.app.R
import com.callNest.app.data.local.dao.ContactMetaDao
import com.callNest.app.data.local.dao.PipelineStageDao
import com.callNest.app.data.local.dao.TagDao
import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.mapper.toDomain
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.domain.model.PipelineStage
import com.callNest.app.domain.usecase.ComputeLeadScoreUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import timber.log.Timber

/** Posts a high-priority "Hot lead is calling" notification when [maybeAlert] hits the criteria. */
@Singleton
class HotLeadNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactMetaDao: ContactMetaDao,
    private val pipelineStageDao: PipelineStageDao,
    private val tagDao: TagDao,
    private val callDao: CallDao,
    private val computeLeadScore: ComputeLeadScoreUseCase,
    private val settings: SettingsDataStore,
) {
    private val nm = NotificationManagerCompat.from(context)

    /** Inspect the incoming number; post an alert iff hot-lead criteria match and setting is on. */
    suspend fun maybeAlert(normalizedNumber: String?) {
        if (normalizedNumber.isNullOrBlank()) return
        if (!settings.hotLeadAlertsEnabled.first()) return
        val metaEntity = runCatching { contactMetaDao.getByNumber(normalizedNumber) }.getOrNull() ?: return
        val stage = runCatching { pipelineStageDao.get(normalizedNumber) }.getOrNull()
            ?.let { PipelineStage.fromKey(it.stageKey) } ?: PipelineStage.New

        // Recompute the score live so follow-up + customer-tag bonuses count (audit P0-3).
        val tagsForLatest = runCatching {
            val latestId = callDao.latestForNumber(normalizedNumber)?.systemId
            if (latestId != null) tagDao.tagIdsForCall(latestId) else emptyList()
        }.getOrDefault(emptyList())
        val tagNames = runCatching {
            tagsForLatest.mapNotNull { tagDao.getById(it)?.name }
        }.getOrDefault(emptyList())
        val customerTagApplied = tagNames.any { it.equals("customer", ignoreCase = true) }
        val hasFollowUp = runCatching {
            callDao.activeFollowUpForNumber(normalizedNumber) != null
        }.getOrDefault(false)
        val live = runCatching {
            computeLeadScore(
                meta = metaEntity.toDomain(),
                hasFollowUp = hasFollowUp,
                customerTagApplied = customerTagApplied,
            )
        }.getOrNull()
        val effectiveScore = live?.total ?: metaEntity.computedLeadScore

        val isHot = effectiveScore >= HOT_THRESHOLD ||
            stage == PipelineStage.Qualified ||
            stage == PipelineStage.Won
        if (!isHot) return
        val name = metaEntity.displayName?.takeIf { it.isNotBlank() } ?: normalizedNumber
        val meta = metaEntity.copy(computedLeadScore = effectiveScore)
        val body = context.getString(
            R.string.hot_lead_notif_body_named_fmt, name, meta.computedLeadScore
        )
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("route", "call_detail/$normalizedNumber")
        }
        val pi = PendingIntent.getActivity(
            context, normalizedNumber.hashCode(), tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CallNestApp.CHANNEL_HOT_LEAD)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.hot_lead_notif_title))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                nm.notify(NOTIF_ID_BASE + normalizedNumber.hashCode(), notif)
            }
        }.onFailure { Timber.w(it, "Hot-lead notify failed") }
    }

    companion object {
        private const val HOT_THRESHOLD = 70
        private const val NOTIF_ID_BASE = 9001
    }
}
