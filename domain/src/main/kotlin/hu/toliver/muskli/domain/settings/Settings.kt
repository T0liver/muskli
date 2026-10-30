package hu.toliver.muskli.domain.settings

import hu.toliver.muskli.domain.calc.PreviousMode

enum class WeightUnit { KG, LB }

/** Own enum rather than `java.time.DayOfWeek`: minSdk 24 has no `java.time` without desugaring. */
enum class WeekStartDay { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

/** User settings with the D-009 defaults. Body weight is a single current value in kg, unset by default (D-017). */
data class Settings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    /** Null means the default rest timer is off. */
    val defaultRestSeconds: Int? = 90,
    /** Stored now; only [PreviousMode.ANYWHERE] is active until routines exist (T-024). */
    val previousMode: PreviousMode = PreviousMode.ANYWHERE,
    val keepScreenAwake: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val weekStart: WeekStartDay = WeekStartDay.MONDAY,
    val bodyWeightKg: Double? = null,
)
