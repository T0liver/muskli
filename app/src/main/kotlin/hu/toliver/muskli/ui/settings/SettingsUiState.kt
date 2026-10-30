package hu.toliver.muskli.ui.settings

import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit

/** Text fields keep what the user typed, so an invalid entry stays visible (and unsaved) instead of snapping back. */
data class SettingsUiState(
    val weightUnit: WeightUnit = Settings().weightUnit,
    val restEnabled: Boolean = true,
    val restSecondsText: String = Settings().defaultRestSeconds.toString(),
    val restSecondsInvalid: Boolean = false,
    val previousMode: PreviousMode = Settings().previousMode,
    val keepScreenAwake: Boolean = Settings().keepScreenAwake,
    val soundEnabled: Boolean = Settings().soundEnabled,
    val vibrationEnabled: Boolean = Settings().vibrationEnabled,
    val weekStart: WeekStartDay = Settings().weekStart,
    /** In [weightUnit]; empty when unset. */
    val bodyWeightText: String = "",
    val bodyWeightInvalid: Boolean = false,
)

sealed interface SettingsEvent {
    data class WeightUnitChanged(val unit: WeightUnit) : SettingsEvent
    data class RestEnabledChanged(val enabled: Boolean) : SettingsEvent
    data class RestSecondsChanged(val text: String) : SettingsEvent
    data class PreviousModeChanged(val mode: PreviousMode) : SettingsEvent
    data class KeepScreenAwakeChanged(val enabled: Boolean) : SettingsEvent
    data class SoundChanged(val enabled: Boolean) : SettingsEvent
    data class VibrationChanged(val enabled: Boolean) : SettingsEvent
    data class WeekStartChanged(val day: WeekStartDay) : SettingsEvent
    data class BodyWeightChanged(val text: String) : SettingsEvent
}
