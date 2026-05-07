package com.callNest.app.data.demo

import com.callNest.app.data.local.dao.CallDao
import com.callNest.app.data.local.dao.ContactMetaDao
import com.callNest.app.data.local.dao.PipelineStageDao
import com.callNest.app.data.local.entity.CallEntity
import com.callNest.app.data.local.entity.ContactMetaEntity
import com.callNest.app.data.local.entity.PipelineStageEntity
import com.callNest.app.data.prefs.SettingsDataStore
import com.callNest.app.domain.model.PipelineStage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Inserts a small set of demo calls + leads so a brand-new install isn't a blank slate.
 * Idempotent: safe to call repeatedly. The demo systemIds use a high base so they never collide
 * with real CallLog entries (which start at 1 and increment).
 */
@Singleton
class DemoSeeder @Inject constructor(
    private val callDao: CallDao,
    private val contactMetaDao: ContactMetaDao,
    private val pipelineStageDao: PipelineStageDao,
    private val settings: SettingsDataStore,
) {

    suspend fun seedIfNeeded() {
        if (settings.demoSeedActive.first()) return
        if (settings.demoSeedDismissedOnce.first()) return
        runCatching {
            val now = System.currentTimeMillis()
            val day = 24L * 60 * 60 * 1000
            // 6 demo calls across 4 demo numbers.
            val demoCalls = listOf(
                demoCall(BASE + 1, NUM_PRIYA, NAME_PRIYA, now - 3 * day, 245, type = 1),
                demoCall(BASE + 2, NUM_PRIYA, NAME_PRIYA, now - day, 312, type = 1),
                demoCall(BASE + 3, NUM_RAJESH, NAME_RAJESH, now - 6 * day, 0, type = 3),
                demoCall(BASE + 4, NUM_RAJESH, NAME_RAJESH, now - 4 * day, 178, type = 1),
                demoCall(BASE + 5, NUM_AMIT, NAME_AMIT, now - 2 * day, 95, type = 2),
                demoCall(BASE + 6, NUM_NEHA, NAME_NEHA, now - 5 * day, 410, type = 1),
            )
            callDao.upsertAll(demoCalls)

            val metas = listOf(
                demoMeta(NUM_PRIYA, NAME_PRIYA, totalCalls = 2, total = 557, score = 82, now),
                demoMeta(NUM_RAJESH, NAME_RAJESH, totalCalls = 2, total = 178, score = 58, now),
                demoMeta(NUM_AMIT, NAME_AMIT, totalCalls = 1, total = 95, score = 35, now),
                demoMeta(NUM_NEHA, NAME_NEHA, totalCalls = 1, total = 410, score = 76, now),
            )
            metas.forEach { contactMetaDao.upsert(it) }

            // 2 leads with explicit pipeline stages (top 2 hot leads).
            pipelineStageDao.upsert(PipelineStageEntity(NUM_PRIYA, PipelineStage.Qualified.name))
            pipelineStageDao.upsert(PipelineStageEntity(NUM_NEHA, PipelineStage.Contacted.name))

            settings.setDemoSeedActive(true)
            Timber.i("Demo data seeded (6 calls, 4 contacts, 2 stages).")
        }.onFailure { Timber.w(it, "Demo seeding failed") }
    }

    suspend fun clearDemo() {
        runCatching {
            for (i in 1..6) callDao.deleteById(BASE + i)
            listOf(NUM_PRIYA, NUM_RAJESH, NUM_AMIT, NUM_NEHA).forEach { number ->
                contactMetaDao.deleteByNumber(number)
                pipelineStageDao.delete(number)
            }
            settings.setDemoSeedActive(false)
            settings.setDemoSeedDismissedOnce(true)
            Timber.i("Demo data cleared.")
        }.onFailure { Timber.w(it, "Demo clear failed") }
    }

    private fun demoCall(
        id: Long, number: String, name: String, dateMs: Long, durationSec: Int, type: Int,
    ): CallEntity = CallEntity(
        systemId = id,
        rawNumber = number,
        normalizedNumber = number,
        date = dateMs,
        duration = durationSec,
        type = type,
        cachedName = name,
        phoneAccountId = null,
        simSlot = null,
        carrierName = null,
        geocodedLocation = null,
        countryIso = "IN",
        isNew = false,
    )

    private fun demoMeta(
        number: String, name: String, totalCalls: Int, total: Int, score: Int, now: Long,
    ): ContactMetaEntity = ContactMetaEntity(
        normalizedNumber = number,
        displayName = name,
        isInSystemContacts = false,
        systemContactId = null,
        systemRawContactId = null,
        firstCallDate = now - 7 * 24 * 60 * 60 * 1000,
        lastCallDate = now - 24 * 60 * 60 * 1000,
        totalCalls = totalCalls,
        totalDuration = total,
        incomingCount = totalCalls,
        outgoingCount = 0,
        missedCount = 0,
        computedLeadScore = score,
    )

    companion object {
        private const val BASE = 9_000_000_000_000L
        private const val NUM_PRIYA = "+919812340001"
        private const val NUM_RAJESH = "+919812340002"
        private const val NUM_AMIT = "+919812340003"
        private const val NUM_NEHA = "+919812340004"
        private const val NAME_PRIYA = "Priya (demo)"
        private const val NAME_RAJESH = "Rajesh (demo)"
        private const val NAME_AMIT = "Amit (demo)"
        private const val NAME_NEHA = "Neha (demo)"
    }
}
