package hu.toliver.muskli.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class WorkoutDateTimeTest {
    private val budapest = TimeZone.getTimeZone("Europe/Budapest")

    // 2026-10-30 23:30 in Budapest (UTC+1) is 22:30 UTC the same day.
    private val lateEvening = 1_793_399_400_000L

    @Test
    fun `the picker date is the local date as UTC midnight`() {
        assertEquals(1_793_318_400_000L, pickerDateMillis(lateEvening, budapest))
    }

    @Test
    fun `picking a date keeps the local time of day`() {
        val oct28 = 1_793_145_600_000L

        val moved = withPickedDate(lateEvening, oct28, budapest)

        assertEquals(lateEvening - 2 * 86_400_000L, moved)
        assertEquals(23, localHour(moved, budapest))
        assertEquals(30, localMinute(moved, budapest))
    }

    @Test
    fun `picking a date across the DST change keeps the wall-clock time`() {
        // 2026-10-20 is still summer time (UTC+2): 23:30 local is 21:30 UTC.
        val oct20 = 1_792_454_400_000L

        val moved = withPickedDate(lateEvening, oct20, budapest)

        assertEquals(1_792_531_800_000L, moved)
    }

    @Test
    fun `setting the time keeps the local date and clears seconds`() {
        val moved = withTimeOfDay(lateEvening + 42_000L, hour = 7, minute = 5, zone = budapest)

        assertEquals(1_793_340_300_000L, moved)
    }
}
