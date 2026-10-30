package hu.toliver.muskli.ui.components

import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.SimpleTimeZone
import java.util.TimeZone

// Dates at the UI edge. Calendar and DateFormat rather than java.time: minSdk 24 has no java.time without desugaring.

/** A workout's start as shown in history, in the workout's own offset so travel does not move it. */
fun formatWorkoutDateTime(epochMs: Long, utcOffsetS: Int): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .apply { timeZone = offsetZone(utcOffsetS) }
        .format(Date(epochMs))

/** Date and time in [zone], for the finish screen's start field. */
fun formatDateTime(epochMs: Long, zone: TimeZone = TimeZone.getDefault()): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .apply { timeZone = zone }
        .format(Date(epochMs))

/** The local date of [epochMs] as UTC midnight, the form the Material date picker selects in. */
fun pickerDateMillis(epochMs: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val local = calendar(epochMs, zone)
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        timeInMillis
    }
}

/** [epochMs] moved to the date picked as [pickerDateMillis], keeping its local time of day. */
fun withPickedDate(epochMs: Long, pickerDateMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val picked = calendar(pickerDateMillis, TimeZone.getTimeZone("UTC"))
    return calendar(epochMs, zone).run {
        set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
        timeInMillis
    }
}

/** [epochMs] at [hour]:[minute] local time on the same local date, seconds cleared. */
fun withTimeOfDay(epochMs: Long, hour: Int, minute: Int, zone: TimeZone = TimeZone.getDefault()): Long =
    calendar(epochMs, zone).run {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

fun localHour(epochMs: Long, zone: TimeZone = TimeZone.getDefault()): Int = calendar(epochMs, zone).get(Calendar.HOUR_OF_DAY)

fun localMinute(epochMs: Long, zone: TimeZone = TimeZone.getDefault()): Int = calendar(epochMs, zone).get(Calendar.MINUTE)

private fun calendar(epochMs: Long, zone: TimeZone): Calendar = Calendar.getInstance(zone).apply { timeInMillis = epochMs }

private fun offsetZone(utcOffsetS: Int): TimeZone = SimpleTimeZone(utcOffsetS * 1_000, "workout")
