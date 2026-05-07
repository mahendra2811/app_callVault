package com.callvault.app.domain.usecase

import com.callvault.app.data.local.dao.CallDao
import com.callvault.app.data.local.dao.ContactMetaDao
import com.callvault.app.data.local.dao.TagDao
import com.callvault.app.domain.model.WeeklyDigest
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Builds a [WeeklyDigest] from local DB only. No network, no LLM. */
class ComputeWeeklyDigestUseCase @Inject constructor(
    private val callDao: CallDao,
    private val contactMetaDao: ContactMetaDao,
    private val tagDao: TagDao,
) {
    suspend operator fun invoke(nowMs: Long = System.currentTimeMillis()): WeeklyDigest {
        val fromMs = nowMs - 7L * 24 * 60 * 60 * 1000
        val calls = callDao.observeBetween(fromMs, nowMs).first()

        var incoming = 0
        var outgoing = 0
        var missed = 0
        // CallEntity.type stores Android CallLog.Calls.TYPE_* raw ints; see CallType enum.
        calls.forEach { c ->
            when (com.callvault.app.domain.model.CallType.fromRaw(c.type)) {
                com.callvault.app.domain.model.CallType.INCOMING -> incoming++
                com.callvault.app.domain.model.CallType.OUTGOING -> outgoing++
                com.callvault.app.domain.model.CallType.MISSED,
                com.callvault.app.domain.model.CallType.REJECTED,
                com.callvault.app.domain.model.CallType.BLOCKED -> missed++
                else -> Unit
            }
        }

        val byNumber = calls.groupBy { it.normalizedNumber }
        val unique = byNumber.size

        // Resolve contact metas in one batch loop (DAO doesn't expose getByNumbers).
        val metas = byNumber.keys.mapNotNull { num ->
            num to (runCatching { contactMetaDao.getByNumber(num) }.getOrNull() ?: return@mapNotNull null)
        }.toMap()

        val hotLeads = metas.values.count { it.computedLeadScore >= HOT_THRESHOLD }

        val top = byNumber.entries
            .sortedByDescending { it.value.size }
            .take(5)
            .map { (number, callsForNum) ->
                val meta = metas[number]
                WeeklyDigest.TopCaller(
                    normalizedNumber = number,
                    displayName = meta?.displayName,
                    callCount = callsForNum.size,
                    leadScore = meta?.computedLeadScore ?: 0,
                )
            }

        val topTags = runCatching { tagDao.topTagsBetween(fromMs, nowMs, limit = 5) }
            .getOrDefault(emptyList())
            .map { WeeklyDigest.TagCount(it.name, it.count) }

        return WeeklyDigest(
            fromMs = fromMs,
            toMs = nowMs,
            totalCalls = calls.size,
            incoming = incoming,
            outgoing = outgoing,
            missed = missed,
            uniqueContacts = unique,
            hotLeads = hotLeads,
            topCallers = top,
            topTags = topTags,
        )
    }

    companion object {
        private const val HOT_THRESHOLD = 70
    }
}
