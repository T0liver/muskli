package hu.toliver.muskli.domain.repo

import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Emits the current settings, defaults for anything never set, and again on every change. */
    val settings: Flow<Settings>

    suspend fun setWeightUnit(unit: WeightUnit)

    /** Null turns the default rest timer off. @throws IllegalArgumentException when not positive. */
    suspend fun setDefaultRestSeconds(seconds: Int?)

    suspend fun setPreviousMode(mode: PreviousMode)

    suspend fun setKeepScreenAwake(enabled: Boolean)

    suspend fun setSoundEnabled(enabled: Boolean)

    suspend fun setVibrationEnabled(enabled: Boolean)

    suspend fun setWeekStart(day: WeekStartDay)

    /** Null clears it. @throws IllegalArgumentException when not positive. */
    suspend fun setBodyWeightKg(kg: Double?)
}
