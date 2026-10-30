package hu.toliver.muskli.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Missing keys and unknown enum names read as the [Settings] defaults, so older or newer files never crash. */
class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {
    override val settings: Flow<Settings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    override suspend fun setWeightUnit(unit: WeightUnit) = put { it[WEIGHT_UNIT] = unit.name }

    override suspend fun setDefaultRestSeconds(seconds: Int?) {
        require(seconds == null || seconds > 0) { "Default rest must be positive, was $seconds" }
        put { it[DEFAULT_REST_S] = seconds ?: REST_OFF }
    }

    override suspend fun setPreviousMode(mode: PreviousMode) = put { it[PREVIOUS_MODE] = mode.name }

    override suspend fun setKeepScreenAwake(enabled: Boolean) = put { it[KEEP_SCREEN_AWAKE] = enabled }

    override suspend fun setSoundEnabled(enabled: Boolean) = put { it[SOUND] = enabled }

    override suspend fun setVibrationEnabled(enabled: Boolean) = put { it[VIBRATION] = enabled }

    override suspend fun setWeekStart(day: WeekStartDay) = put { it[WEEK_START] = day.name }

    override suspend fun setBodyWeightKg(kg: Double?) {
        require(kg == null || kg > 0) { "Body weight must be positive, was $kg" }
        put { if (kg == null) it.remove(BODY_WEIGHT_KG) else it[BODY_WEIGHT_KG] = kg }
    }

    private suspend fun put(change: (MutablePreferences) -> Unit) {
        store.edit(change)
    }

    private fun Preferences.toSettings(): Settings {
        val defaults = Settings()
        return Settings(
            weightUnit = enumOrDefault(this[WEIGHT_UNIT], defaults.weightUnit),
            defaultRestSeconds = when (val rest = this[DEFAULT_REST_S]) {
                null -> defaults.defaultRestSeconds
                REST_OFF -> null
                else -> rest
            },
            previousMode = enumOrDefault(this[PREVIOUS_MODE], defaults.previousMode),
            keepScreenAwake = this[KEEP_SCREEN_AWAKE] ?: defaults.keepScreenAwake,
            soundEnabled = this[SOUND] ?: defaults.soundEnabled,
            vibrationEnabled = this[VIBRATION] ?: defaults.vibrationEnabled,
            weekStart = enumOrDefault(this[WEEK_START], defaults.weekStart),
            bodyWeightKg = this[BODY_WEIGHT_KG],
        )
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private companion object {
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val DEFAULT_REST_S = intPreferencesKey("default_rest_s")
        val PREVIOUS_MODE = stringPreferencesKey("previous_mode")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val WEEK_START = stringPreferencesKey("week_start")
        val BODY_WEIGHT_KG = doublePreferencesKey("body_weight_kg")

        // A missing key means "never set" (default 90 s), so "off" needs its own stored value.
        const val REST_OFF = 0
    }
}
