package hu.toliver.muskli.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.domain.calc.kgToLb
import hu.toliver.muskli.domain.calc.lbToKg
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToLong

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {
    // Null while the field shows the stored value; otherwise exactly what the user typed.
    private val restDraft = MutableStateFlow<String?>(null)
    private val bodyWeightDraft = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> =
        combine(repository.settings, restDraft, bodyWeightDraft, ::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.WeightUnitChanged -> {
                bodyWeightDraft.value = null
                save { setWeightUnit(event.unit) }
            }
            is SettingsEvent.RestEnabledChanged -> {
                restDraft.value = null
                save { setDefaultRestSeconds(if (event.enabled) Settings().defaultRestSeconds else null) }
            }
            is SettingsEvent.RestSecondsChanged -> {
                restDraft.value = event.text
                parseRestSeconds(event.text)?.let { save { setDefaultRestSeconds(it) } }
            }
            is SettingsEvent.PreviousModeChanged -> save { setPreviousMode(event.mode) }
            is SettingsEvent.KeepScreenAwakeChanged -> save { setKeepScreenAwake(event.enabled) }
            is SettingsEvent.SoundChanged -> save { setSoundEnabled(event.enabled) }
            is SettingsEvent.VibrationChanged -> save { setVibrationEnabled(event.enabled) }
            is SettingsEvent.WeekStartChanged -> save { setWeekStart(event.day) }
            is SettingsEvent.BodyWeightChanged -> {
                bodyWeightDraft.value = event.text
                when {
                    event.text.isBlank() -> save { setBodyWeightKg(null) }
                    else -> parsePositive(event.text)?.let { value ->
                        save {
                            val unit = settings.first().weightUnit
                            setBodyWeightKg(if (unit == WeightUnit.LB) lbToKg(value) else value)
                        }
                    }
                }
            }
        }
    }

    private fun save(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { repository.block() }
    }

    private fun toUiState(settings: Settings, restText: String?, bodyWeightText: String?) = SettingsUiState(
        weightUnit = settings.weightUnit,
        restEnabled = settings.defaultRestSeconds != null,
        restSecondsText = restText ?: settings.defaultRestSeconds?.toString().orEmpty(),
        restSecondsInvalid = restText != null && parseRestSeconds(restText) == null,
        previousMode = settings.previousMode,
        keepScreenAwake = settings.keepScreenAwake,
        soundEnabled = settings.soundEnabled,
        vibrationEnabled = settings.vibrationEnabled,
        weekStart = settings.weekStart,
        bodyWeightText = bodyWeightText ?: settings.bodyWeightKg?.let { formatWeight(it, settings.weightUnit) }.orEmpty(),
        bodyWeightInvalid = bodyWeightText != null && bodyWeightText.isNotBlank() && parsePositive(bodyWeightText) == null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        fun parseRestSeconds(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it > 0 }

        // Accepts a decimal comma too, since the phone's keyboard may offer only that.
        fun parsePositive(text: String): Double? =
            text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }
    }
}

/** Body weight in [unit], one decimal at most. */
internal fun formatWeight(kg: Double, unit: WeightUnit): String {
    val value = if (unit == WeightUnit.LB) kgToLb(kg) else kg
    val tenths = (value * 10).roundToLong()
    return if (tenths % 10 == 0L) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
}
