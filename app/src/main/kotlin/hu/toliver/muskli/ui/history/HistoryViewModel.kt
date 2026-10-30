package hu.toliver.muskli.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.repo.WorkoutRepository
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.domain.usecase.workout.DeleteWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.RestoreWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.StartPastWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.hasCompletedSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val loading: Boolean = true,
    val workouts: List<WorkoutSummaryUi> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    /** The workout just deleted, while its Undo is offered. */
    val undoWorkoutId: String? = null,
)

sealed interface HistoryEvent {
    data class DeleteClicked(val workoutId: String) : HistoryEvent

    /** Deleted elsewhere (discarded from the finish screen); only the Undo is offered here. */
    data class Deleted(val workoutId: String) : HistoryEvent
    data object UndoClicked : HistoryEvent
    data object UndoDismissed : HistoryEvent
}

/**
 * The history list (F-06 AC2), finished workouts newest first. Also owns delete and its undo,
 * since the list is where the undo is shown, and starting a past workout (F-06 AC3).
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val workouts: WorkoutRepository,
    templates: ExerciseTemplateRepository,
    settingsRepository: SettingsRepository,
    private val deleteWorkout: DeleteWorkoutUseCase,
    private val restoreWorkout: RestoreWorkoutUseCase,
    private val startPastWorkout: StartPastWorkoutUseCase,
) : ViewModel() {
    private val undo = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        workouts.observeHistory(),
        templates.observeAll().map { all -> all.associateBy(ExerciseTemplate::id) },
        settingsRepository.settings,
        undo,
    ) { history, templatesById, settings, undoId ->
        HistoryUiState(
            loading = false,
            workouts = history.map { summarizeWorkout(it, templatesById, settings.weightUnit, settings.bodyWeightKg) },
            weightUnit = settings.weightUnit,
            undoWorkoutId = undoId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState())

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.DeleteClicked -> viewModelScope.launch {
                deleteWorkout(event.workoutId)
                undo.value = event.workoutId
            }
            is HistoryEvent.Deleted -> undo.value = event.workoutId
            HistoryEvent.UndoClicked -> {
                val id = undo.value ?: return
                undo.value = null
                viewModelScope.launch { restoreWorkout(id) }
            }
            HistoryEvent.UndoDismissed -> undo.value = null
        }
    }

    fun logPastWorkout(startedAtEpochMs: Long, onCreated: (workoutId: String) -> Unit) {
        viewModelScope.launch { onCreated(startPastWorkout(startedAtEpochMs).id) }
    }

    /** Leaving a just-created past workout with nothing completed drops it, so no empty entry is left behind. */
    fun leaveNewPastWorkout(workoutId: String, onResult: (discarded: Boolean) -> Unit) {
        viewModelScope.launch {
            val workout = workouts.observe(workoutId).first()
            val discard = workout != null && !workout.hasCompletedSet()
            if (discard) deleteWorkout(workoutId)
            onResult(discard)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
