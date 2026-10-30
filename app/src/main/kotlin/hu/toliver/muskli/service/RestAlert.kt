package hu.toliver.muskli.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.usecase.workout.FinishRestTimerUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The end of a rest, reached by the service's handler or the backup alarm, whichever is first
 * (D-015). Only the path that clears the stored rest alerts, so it never fires twice.
 */
class RestAlert @Inject constructor(
    @ApplicationContext private val context: Context,
    private val finishRest: FinishRestTimerUseCase,
    private val settings: SettingsRepository,
) {
    suspend fun fire(workoutId: String, endsAtEpochMs: Long) {
        if (!finishRest(workoutId, endsAtEpochMs)) return
        val current = settings.settings.first()
        RestNotifications.showAlert(context, sound = current.soundEnabled, vibration = current.vibrationEnabled)
    }
}
