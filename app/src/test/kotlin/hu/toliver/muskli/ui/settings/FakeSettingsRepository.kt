package hu.toliver.muskli.ui.settings

import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun setWeightUnit(unit: WeightUnit) = settings.update { it.copy(weightUnit = unit) }

    override suspend fun setDefaultRestSeconds(seconds: Int?) {
        require(seconds == null || seconds > 0)
        settings.update { it.copy(defaultRestSeconds = seconds) }
    }

    override suspend fun setPreviousMode(mode: PreviousMode) = settings.update { it.copy(previousMode = mode) }

    override suspend fun setKeepScreenAwake(enabled: Boolean) = settings.update { it.copy(keepScreenAwake = enabled) }

    override suspend fun setSoundEnabled(enabled: Boolean) = settings.update { it.copy(soundEnabled = enabled) }

    override suspend fun setVibrationEnabled(enabled: Boolean) = settings.update { it.copy(vibrationEnabled = enabled) }

    override suspend fun setWeekStart(day: WeekStartDay) = settings.update { it.copy(weekStart = day) }

    override suspend fun setBodyWeightKg(kg: Double?) {
        require(kg == null || kg > 0)
        settings.update { it.copy(bodyWeightKg = kg) }
    }
}
