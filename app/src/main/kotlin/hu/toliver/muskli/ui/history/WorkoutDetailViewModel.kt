package hu.toliver.muskli.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.repo.WorkoutRepository
import hu.toliver.muskli.domain.settings.WeightUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface WorkoutDetailUiState {
    data object Loading : WorkoutDetailUiState

    /** Deleted or never existed. */
    data object Missing : WorkoutDetailUiState

    data class Ready(
        val summary: WorkoutSummaryUi,
        val notes: String?,
        val exercises: List<ExerciseSummaryUi>,
        val weightUnit: WeightUnit,
    ) : WorkoutDetailUiState
}

/** One past workout, read-only (F-06 AC2); Edit and Delete are handled by the caller. */
@HiltViewModel(assistedFactory = WorkoutDetailViewModel.Factory::class)
class WorkoutDetailViewModel @AssistedInject constructor(
    @Assisted workoutId: String,
    workouts: WorkoutRepository,
    templates: ExerciseTemplateRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(workoutId: String): WorkoutDetailViewModel
    }

    val uiState: StateFlow<WorkoutDetailUiState> = combine(
        workouts.observe(workoutId),
        templates.observeAll().map { all -> all.associateBy(ExerciseTemplate::id) },
        settingsRepository.settings,
    ) { workout, templatesById, settings ->
        if (workout == null || workout.deletedAtEpochMs != null) {
            WorkoutDetailUiState.Missing
        } else {
            WorkoutDetailUiState.Ready(
                summary = summarizeWorkout(workout, templatesById, settings.weightUnit, settings.bodyWeightKg),
                notes = workout.notes,
                exercises = summarizeExercises(workout, templatesById, settings.weightUnit),
                weightUnit = settings.weightUnit,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutDetailUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
