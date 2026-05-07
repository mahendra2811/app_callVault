package com.callNest.app.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.datetime.Instant

/**
 * Renders timestamps for the Calls list and detail screens.
 *
 * - Calls today  → `Today, 14:32`
 * - Calls yesterday → `Yesterday, 09:10`
 * - Within last 6 days → `Mon 18:00`
 * - Older → `12 Mar 2025`
 *
 * Sticky-header labels strip the time portion: "Today", "Yesterday",
 * "Wednesday" (within 6 days) or `12 Mar 2025`.
 */
object DateFormatter {

    private val timeFmt = ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    private val weekdayTimeFmt =
        ThreadLocal.withInitial { SimpleDateFormat("EEE HH:mm", Locale.getDefault()) }
    private val dateFmt =
        ThreadLocal.withInitial { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    private val weekdayFmt =
        ThreadLocal.withInitial { SimpleDateFormat("EEEE", Locale.getDefault()) }

    /** Localized row time, including a relative day prefix when recent. */
    fun rowTime(instant: Instant, now: Long = System.currentTimeMillis()): String {
        val ms = instant.toEpochMilliseconds()
        return when (relativeBucket(ms, now)) {
            Bucket.TODAY -> "Today, ${timeFmt.get()!!.format(Date(ms))}"
            Bucket.YESTERDAY -> "Yesterday, ${timeFmt.get()!!.format(Date(ms))}"
            Bucket.LAST_WEEK -> weekdayTimeFmt.get()!!.format(Date(ms))
            Bucket.OLDER -> dateFmt.get()!!.format(Date(ms))
        }
    }

    /** Sticky header label without the time component. */
    fun headerLabel(instant: Instant, now: Long = System.currentTimeMillis()): String {
        val ms = instant.toEpochMilliseconds()
        return when (relativeBucket(ms, now)) {
            Bucket.TODAY -> "Today"
            Bucket.YESTERDAY -> "Yesterday"
            Bucket.LAST_WEEK -> weekdayFmt.get()!!.format(Date(ms))
            Bucket.OLDER -> dateFmt.get()!!.format(Date(ms))
        }
    }

    /** A stable bucket key suitable for `groupBy` of a sorted list of calls. */
    fun headerKey(instant: Instant, now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = instant.toEpochMilliseconds()
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** Long-form "Friday, 12 March 2025" used in detail headers. */
    fun longDate(instant: Instant): String {
        val fmt = SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault())
        return fmt.format(Date(instant.toEpochMilliseconds()))
    }

    private enum class Bucket { TODAY, YESTERDAY, LAST_WEEK, OLDER }

    private fun relativeBucket(ms: Long, now: Long): Bucket {
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val targetCal = Calendar.getInstance().apply { timeInMillis = ms }
        val sameDay = nowCal.sameDay(targetCal)
        if (sameDay) return Bucket.TODAY
        val yesterdayCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (yesterdayCal.sameDay(targetCal)) return Bucket.YESTERDAY
        val sixAgo = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
        return if (targetCal.timeInMillis >= sixAgo.timeInMillis) Bucket.LAST_WEEK else Bucket.OLDER
    }

    private fun Calendar.sameDay(other: Calendar): Boolean =
        get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}
