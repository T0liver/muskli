package hu.toliver.muskli.domain.importer.hevy

import hu.toliver.muskli.domain.repo.Clock

private val LOCAL_TIME = Regex("""(\d{1,2}) ([A-Za-z]{3}) (\d{4}), (\d{2}):(\d{2})""")
private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private const val MS_PER_MINUTE = 60_000L
private const val MS_PER_DAY = 86_400_000L

/**
 * Hevy's `d MMM yyyy, HH:mm` (English months, 24 h) read as if it were UTC, in epoch ms;
 * null when the text is not exactly in that format or names an impossible date.
 */
internal fun parseHevyLocalTime(text: String): Long? {
    val (dayText, monthText, yearText, hourText, minuteText) = LOCAL_TIME.matchEntire(text)?.destructured ?: return null
    val month = MONTHS.indexOf(monthText) + 1
    val year = yearText.toInt()
    val day = dayText.toInt()
    val hour = hourText.toInt()
    val minute = minuteText.toInt()
    if (month == 0 || day !in 1..daysInMonth(year, month) || hour > 23 || minute > 59) return null
    return daysFromCivil(year, month, day) * MS_PER_DAY + (hour * 60L + minute) * MS_PER_MINUTE
}

/**
 * Resolves a local time (as if UTC) in the device zone (D-041, HEVY-IMPORT §5): in a DST overlap the
 * earliest instant wins; in a DST gap the time moves forward by the gap.
 */
internal fun localToEpochMs(localAsUtcMs: Long, clock: Clock): Long {
    val offsetBeforeS = clock.utcOffsetS(localAsUtcMs - MS_PER_DAY)
    val offsetAfterS = clock.utcOffsetS(localAsUtcMs + MS_PER_DAY)
    return listOf(offsetBeforeS, offsetAfterS)
        .map { localAsUtcMs - it * 1000L to it }
        .filter { (candidate, offsetS) -> clock.utcOffsetS(candidate) == offsetS }
        .minOfOrNull { it.first }
        ?: (localAsUtcMs - offsetBeforeS * 1000L)
}

private fun isLeapYear(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private fun daysInMonth(year: Int, month: Int) = when (month) {
    2 -> if (isLeapYear(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** Days since 1970-01-01 in the proleptic Gregorian calendar (H. Hinnant's `days_from_civil`). */
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = (if (month <= 2) year - 1 else year).toLong()
    val era = Math.floorDiv(y, 400L)
    val yearOfEra = y - era * 400
    val dayOfYear = (153 * ((month + 9) % 12) + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146_097 + dayOfEra - 719_468
}
