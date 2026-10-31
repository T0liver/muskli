package hu.toliver.muskli.ui.finish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.WorkoutRepository
import hu.toliver.muskli.domain.usecase.workout.DeleteWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.FinishWorkoutResult
import hu.toliver.muskli.domain.usecase.workout.FinishWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.WorkoutDetails
import hu.toliver.muskli.domain.usecase.workout.finishDurationS
import hu.toliver.muskli.domain.usecase.workout.hasCompletedSet
import hu.toliver.muskli.domain.usecase.workout.uncompletedSetCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The finish screen (F-06 AC1) for the live draft, and the save step of an edited past workout.
 * The form is read once, so each opening needs its own instance: a second Finish after more
 * logging must get a fresh duration.
 */
@HiltViewModel(assistedFactory = FinishViewModel.Factory::class)
class FinishViewModel @AssistedInject constructor(
    @Assisted private val workoutId: String,
    private val workouts: WorkoutRepository,
    private val clock: Clock,
    private val finishWorkout: FinishWorkoutUseCase,
    private val deleteWorkout: DeleteWorkoutUseCase,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(workoutId: String): FinishViewModel
    }

    private val state = MutableStateFlow<FinishUiState>(FinishUiState.Loading)
    val uiState: StateFlow<FinishUiState> = state.asStateFlow()

    /** Measured when the screen opens, so a workout under a minute is not saved as 0 (D-051). */
    private var elapsedMs = 0L

    init {
        viewModelScope.launch {
            val workout = workouts.observe(workoutId).first() ?: return@launch
            elapsedMs = ((workout.finishedAtEpochMs ?: clock.nowEpochMs()) - workout.startedAtEpochMs).coerceAtLeast(0)
            val durationS = elapsedMs / 1000
            state.value = FinishUiState.Ready(
                title = workout.title,
                notes = workout.notes.orEmpty(),
                startedAtEpochMs = workout.startedAtEpochMs,
                hours = (durationS / 3600).toString(),
                minutes = (durationS % 3600 / 60).toString(),
                durationValid = true,
                editingPast = workout.finishedAtEpochMs != null,
            )
        }
    }

    fun onEvent(event: FinishEvent) {
        when (event) {
            is FinishEvent.TitleChanged -> form { it.copy(title = event.text) }
            is FinishEvent.NotesChanged -> form { it.copy(notes = event.text) }
            is FinishEvent.StartChanged -> form { it.copy(startedAtEpochMs = event.epochMs) }
            is FinishEvent.HoursChanged -> form { it.copy(hours = event.text).validated() }
            is FinishEvent.MinutesChanged -> form { it.copy(minutes = event.text).validated() }
            FinishEvent.SaveClicked -> saveClicked()
            FinishEvent.DiscardSetsConfirmed -> {
                form { it.copy(dialog = null) }
                finish()
            }
            FinishEvent.DiscardWorkoutConfirmed -> viewModelScope.launch {
                deleteWorkout(workoutId)
                state.value = FinishUiState.Done(FinishOutcome.Discarded(workoutId))
            }
            FinishEvent.DialogDismissed -> form { it.copy(dialog = null) }
        }
    }

    private fun saveClicked() {
        val ready = state.value as? FinishUiState.Ready ?: return
        if (!ready.durationValid) return
        viewModelScope.launch {
            val workout = workouts.observe(workoutId).first() ?: return@launch
            val uncompleted = workout.uncompletedSetCount()
            when {
                !workout.hasCompletedSet() -> form { it.copy(dialog = FinishDialog.DiscardWorkout) }
                uncompleted > 0 -> form { it.copy(dialog = FinishDialog.DiscardSets(uncompleted)) }
                else -> finish()
            }
        }
    }

    private fun finish() {
        val ready = state.value as? FinishUiState.Ready ?: return
        val durationS = finishDurationS(elapsedMs, ready.durationS() ?: return)
        viewModelScope.launch {
            val details = WorkoutDetails(ready.title, ready.notes, ready.startedAtEpochMs, durationS)
            when (finishWorkout(workoutId, details)) {
                is FinishWorkoutResult.Finished -> state.value = FinishUiState.Done(FinishOutcome.Saved(workoutId))
                FinishWorkoutResult.NothingCompleted -> form { it.copy(dialog = FinishDialog.DiscardWorkout) }
                null -> Unit
            }
        }
    }

    private fun form(transform: (FinishUiState.Ready) -> FinishUiState.Ready) =
        state.update { if (it is FinishUiState.Ready) transform(it) else it }
}

private fun FinishUiState.Ready.validated() = copy(durationValid = durationS() != null)

/** Empty fields count as 0; minutes must stay below an hour. */
private fun FinishUiState.Ready.durationS(): Long? {
    val h = hours.trim().ifEmpty { "0" }.toLongOrNull()?.takeIf { it in 0..MAX_HOURS } ?: return null
    val m = minutes.trim().ifEmpty { "0" }.toLongOrNull()?.takeIf { it in 0..59 } ?: return null
    return h * 3600 + m * 60
}

private const val MAX_HOURS = 99L
