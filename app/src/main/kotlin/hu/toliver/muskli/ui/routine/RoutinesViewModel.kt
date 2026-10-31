package hu.toliver.muskli.ui.routine

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.usecase.routine.ObserveRoutinesUseCase
import hu.toliver.muskli.domain.usecase.routine.StartWorkoutFromRoutineUseCase
import hu.toliver.muskli.domain.usecase.workout.StartWorkoutResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class RoutineSummaryUi(
    val id: String,
    val title: String,
    val exerciseNames: List<String>,
)

@Immutable
data class RoutinesUiState(
    val loading: Boolean = true,
    val routines: List<RoutineSummaryUi> = emptyList(),
    /** Start was tapped while a draft exists; nothing was created (D-052). */
    val inProgressShown: Boolean = false,
)

sealed interface RoutinesEvent {
    data object InProgressDismissed : RoutinesEvent
}

/** The routines list (T-034). Starting goes through [StartWorkoutFromRoutineUseCase], which never makes a second draft. */
@HiltViewModel
class RoutinesViewModel @Inject constructor(
    observeRoutines: ObserveRoutinesUseCase,
    templates: ExerciseTemplateRepository,
    private val startWorkoutFromRoutine: StartWorkoutFromRoutineUseCase,
) : ViewModel() {
    private val inProgressShown = MutableStateFlow(false)

    val uiState: StateFlow<RoutinesUiState> = combine(
        observeRoutines(),
        templates.observeAll().map { all -> all.associateBy(ExerciseTemplate::id) },
        inProgressShown,
    ) { routines, templatesById, inProgress ->
        RoutinesUiState(
            loading = false,
            routines = routines.map { routine ->
                RoutineSummaryUi(
                    id = routine.id,
                    title = routine.title,
                    exerciseNames = routine.exercises.mapNotNull { templatesById[it.exerciseTemplateId]?.title },
                )
            },
            inProgressShown = inProgress,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RoutinesUiState())

    /** [onStarted] runs once the new draft is stored; with a draft already running the screen says so instead. */
    fun startRoutine(routineId: String, onStarted: () -> Unit) {
        viewModelScope.launch {
            when (startWorkoutFromRoutine(routineId)) {
                is StartWorkoutResult.Started -> onStarted()
                is StartWorkoutResult.Resumed -> inProgressShown.value = true
                null -> Unit
            }
        }
    }

    fun onEvent(event: RoutinesEvent) {
        when (event) {
            RoutinesEvent.InProgressDismissed -> inProgressShown.value = false
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
