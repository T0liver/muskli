package hu.toliver.muskli.ui.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.domain.history.exerciseSessions
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.repo.WorkoutRepository
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.components.formatWorkoutDateTime
import hu.toliver.muskli.ui.workout.columnsFor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class ExerciseSessionUi(
    val workoutId: String,
    val workoutTitle: String,
    val dateTime: String,
    val sets: List<SetSummaryUi>,
)

sealed interface ExerciseHistoryUiState {
    data object Loading : ExerciseHistoryUiState

    data class Ready(
        val name: String,
        val columns: List<SetColumn>,
        val sessions: List<ExerciseSessionUi>,
        val weightUnit: WeightUnit,
    ) : ExerciseHistoryUiState
}

/** Every finished session of one exercise, newest first (F-06 AC4). */
@HiltViewModel(assistedFactory = ExerciseHistoryViewModel.Factory::class)
class ExerciseHistoryViewModel @AssistedInject constructor(
    @Assisted exerciseTemplateId: String,
    workouts: WorkoutRepository,
    templates: ExerciseTemplateRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(exerciseTemplateId: String): ExerciseHistoryViewModel
    }

    val uiState: StateFlow<ExerciseHistoryUiState> = combine(
        workouts.observeHistory(),
        templates.observeAll().map { all -> all.firstOrNull { it.id == exerciseTemplateId } },
        settingsRepository.settings,
    ) { history, template, settings ->
        val columns = columnsFor(template?.type ?: ExerciseType.WEIGHT_REPS)
        ExerciseHistoryUiState.Ready(
            name = template?.title.orEmpty(),
            columns = columns,
            sessions = exerciseSessions(history, exerciseTemplateId).map {
                ExerciseSessionUi(
                    workoutId = it.workoutId,
                    workoutTitle = it.workoutTitle,
                    dateTime = formatWorkoutDateTime(it.startedAtEpochMs, it.utcOffsetS),
                    sets = summarizeSets(it.sets, columns, settings.weightUnit),
                )
            },
            weightUnit = settings.weightUnit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ExerciseHistoryUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
