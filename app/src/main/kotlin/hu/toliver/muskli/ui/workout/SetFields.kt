package hu.toliver.muskli.ui.workout

import hu.toliver.muskli.domain.calc.kgToLb
import hu.toliver.muskli.domain.calc.lbToKg
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.SetColumn
import java.math.BigDecimal
import java.math.RoundingMode

// Display and input of set values. Storage stays kg, metres and seconds; the unit applies only here.

fun columnsFor(type: ExerciseType): List<SetColumn> = when (type) {
    ExerciseType.WEIGHT_REPS -> listOf(SetColumn.WEIGHT, SetColumn.REPS)
    ExerciseType.REPS_ONLY -> listOf(SetColumn.REPS)
    ExerciseType.BODYWEIGHT_REPS -> listOf(SetColumn.ADDED_WEIGHT, SetColumn.REPS)
    ExerciseType.BODYWEIGHT_ASSISTED_REPS -> listOf(SetColumn.ASSISTANCE, SetColumn.REPS)
    ExerciseType.DURATION -> listOf(SetColumn.DURATION)
    ExerciseType.WEIGHT_DURATION -> listOf(SetColumn.WEIGHT, SetColumn.DURATION)
    ExerciseType.DISTANCE_DURATION -> listOf(SetColumn.DISTANCE_KM, SetColumn.DURATION)
    ExerciseType.SHORT_DISTANCE_WEIGHT -> listOf(SetColumn.WEIGHT, SetColumn.DISTANCE_M)
}

/** The column's value of [data] as shown in the field; empty when unset. */
fun SetColumn.format(data: SetData, unit: WeightUnit): String = when (this) {
    SetColumn.WEIGHT, SetColumn.ADDED_WEIGHT, SetColumn.ASSISTANCE ->
        data.weightKg?.let { formatDecimal(if (unit == WeightUnit.LB) kgToLb(it) else it) }
    SetColumn.REPS -> data.reps?.toString()
    SetColumn.DURATION -> data.durationS?.let(::formatDuration)
    SetColumn.DISTANCE_KM -> data.distanceM?.let { formatDecimal(it / METRES_PER_KM) }
    SetColumn.DISTANCE_M -> data.distanceM?.let(::formatDecimal)
}.orEmpty()

/** [data] with this column set from typed [text]; text that does not parse clears the value. */
fun SetColumn.parseInto(data: SetData, text: String, unit: WeightUnit): SetData = when (this) {
    SetColumn.WEIGHT, SetColumn.ADDED_WEIGHT, SetColumn.ASSISTANCE ->
        data.copy(weightKg = parseDecimal(text)?.let { if (unit == WeightUnit.LB) lbToKg(it) else it })
    SetColumn.REPS -> data.copy(reps = text.trim().toIntOrNull()?.takeIf { it >= 0 })
    SetColumn.DURATION -> data.copy(durationS = parseDuration(text))
    SetColumn.DISTANCE_KM -> data.copy(distanceM = parseDecimal(text)?.times(METRES_PER_KM))
    SetColumn.DISTANCE_M -> data.copy(distanceM = parseDecimal(text))
}

/** Up to two decimals so 1.25 kg steps survive; trailing zeros dropped. */
fun formatDecimal(value: Double): String =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** m:ss, or h:mm:ss from one hour. */
fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatDuration(seconds: Int): String = formatDuration(seconds.toLong())

// Accepts a decimal comma too, since the phone's keyboard may offer only that.
private fun parseDecimal(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }

/**
 * "m:ss" or "h:mm:ss". Digits alone read like a timer keypad, the last two being seconds
 * ("130" is 1:30), because the number keyboard has no colon.
 */
private fun parseDuration(text: String): Int? {
    val trimmed = text.trim()
    if (':' in trimmed) {
        val parts = trimmed.split(':').map { part -> part.toIntOrNull()?.takeIf { it >= 0 } ?: return null }
        if (parts.size > 3) return null
        return parts.fold(0) { total, part -> total * 60 + part }
    }
    val digits = trimmed.toIntOrNull()?.takeIf { it >= 0 } ?: return null
    return if (trimmed.length <= 2) digits else digits / 100 * 60 + digits % 100
}

private const val METRES_PER_KM = 1000.0
