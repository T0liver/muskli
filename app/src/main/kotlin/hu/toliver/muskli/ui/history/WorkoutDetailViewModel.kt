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
import hu.toliver.muskli.domain.usecase.routine.SaveWorkoutAsRoutineUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WorkoutDetailUiState {
    data object Loading : WorkoutDetailUiState

    /** Deleted or never existed. */
    data object Missing : WorkoutDetailUiState

    data class Ready(
        val summary: WorkoutSummaryUi,
        val notes: String?,
        val exercises: List<ExerciseSummaryUi>,
        val weightUnit: WeightUnit,
        val saveAsRoutine: SaveAsRoutineUi? = null,
        val routineSaved: Boolean = false,
    ) : WorkoutDetailUiState
}

/** The "Save as routine" name dialog; [showError] once Save was tapped with a blank name. */
data class SaveAsRoutineUi(val title: String, val showError: Boolean = false)

sealed interface WorkoutDetailEvent {
    data object SaveAsRoutineClicked : WorkoutDetailEvent
    data class SaveAsRoutineTitleChanged(val text: String) : WorkoutDetailEvent
    data object SaveAsRoutineConfirmed : WorkoutDetailEvent
    data object SaveAsRoutineDismissed : WorkoutDetailEvent
    data object RoutineSavedShown : WorkoutDetailEvent
}

/** One past workout, read-only (F-06 AC2); Edit and Delete are handled by the caller. */
@HiltViewModel(assistedFactory = WorkoutDetailViewModel.Factory::class)
class WorkoutDetailViewModel @AssistedInject constructor(
    @Assisted private val workoutId: String,
    private val workouts: WorkoutRepository,
    templates: ExerciseTemplateRepository,
    settingsRepository: SettingsRepository,
    private val saveWorkoutAsRoutine: SaveWorkoutAsRoutineUseCase,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(workoutId: String): WorkoutDetailViewModel
    }

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<WorkoutDetailUiState> = combine(
        workouts.observe(workoutId),
        templates.observeAll().map { all -> all.associateBy(ExerciseTemplate::id) },
        settingsRepository.settings,
        local,
    ) { workout, templatesById, settings, local ->
        if (workout == null || workout.deletedAtEpochMs != null) {
            WorkoutDetailUiState.Missing
        } else {
            WorkoutDetailUiState.Ready(
                summary = summarizeWorkout(workout, templatesById, settings.weightUnit, settings.bodyWeightKg),
                notes = workout.notes,
                exercises = summarizeExercises(workout, templatesById, settings.weightUnit),
                weightUnit = settings.weightUnit,
                saveAsRoutine = local.saveAsRoutine,
                routineSaved = local.routineSaved,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutDetailUiState.Loading)

    fun onEvent(event: WorkoutDetailEvent) {
        when (event) {
            WorkoutDetailEvent.SaveAsRoutineClicked -> viewModelScope.launch {
                val title = workouts.observe(workoutId).first()?.title.orEmpty()
                local.update { it.copy(saveAsRoutine = SaveAsRoutineUi(title)) }
            }
            is WorkoutDetailEvent.SaveAsRoutineTitleChanged -> local.update { it.copy(saveAsRoutine = it.saveAsRoutine?.copy(title = event.text)) }
            WorkoutDetailEvent.SaveAsRoutineConfirmed -> confirmSave()
            WorkoutDetailEvent.SaveAsRoutineDismissed -> local.update { it.copy(saveAsRoutine = null) }
            WorkoutDetailEvent.RoutineSavedShown -> local.update { it.copy(routineSaved = false) }
        }
    }

    private fun confirmSave() {
        val dialog = local.value.saveAsRoutine ?: return
        if (dialog.title.isBlank()) {
            local.update { it.copy(saveAsRoutine = dialog.copy(showError = true)) }
            return
        }
        viewModelScope.launch {
            val saved = saveWorkoutAsRoutine(workoutId, dialog.title) != null
            local.update { it.copy(saveAsRoutine = null, routineSaved = saved) }
        }
    }

    private data class Local(val saveAsRoutine: SaveAsRoutineUi? = null, val routineSaved: Boolean = false)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
