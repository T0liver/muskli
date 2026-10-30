package hu.toliver.muskli.ui.workout

import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.RestTimerUi
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.components.WorkoutExerciseUi

sealed interface LiveWorkoutUiState {
    /** The draft has not been read yet; the app does not know whether one exists. */
    data object Loading : LiveWorkoutUiState

    data object NoWorkout : LiveWorkoutUiState

    /** [volume] is formatted in [weightUnit]; [completedSets] and volume count completed sets only. */
    data class Active(
        val workoutId: String,
        val duration: String,
        val volume: String,
        val completedSets: Int,
        val weightUnit: WeightUnit,
        val exercises: List<WorkoutExerciseUi>,
        val keepScreenAwake: Boolean,
        /** Null when no rest is running; time left comes from the stored end and the clock. */
        val restTimer: RestTimerUi? = null,
        val exerciseMenuFor: String? = null,
        val setTypeMenuFor: String? = null,
        val reorderOpen: Boolean = false,
        /** Editing a past workout (T-016): the header saves instead of finishing. */
        val editing: Boolean = false,
    ) : LiveWorkoutUiState
}

sealed interface LiveWorkoutEvent {
    /** The screen came back into view; history may have been edited meanwhile, so PREVIOUS is re-read. */
    data object ScreenShown : LiveWorkoutEvent
    data object StartClicked : LiveWorkoutEvent
    data class ExercisesPicked(val templateIds: List<String>) : LiveWorkoutEvent
    data class ReplacementPicked(val workoutExerciseId: String, val templateId: String) : LiveWorkoutEvent
    data class ExerciseMenuClicked(val workoutExerciseId: String) : LiveWorkoutEvent
    data object ExerciseMenuDismissed : LiveWorkoutEvent
    data class RemoveClicked(val workoutExerciseId: String) : LiveWorkoutEvent
    data object ReorderClicked : LiveWorkoutEvent
    data class ExerciseMoved(val workoutExerciseId: String, val toIndex: Int) : LiveWorkoutEvent
    data object ReorderDismissed : LiveWorkoutEvent
    data class AddSetClicked(val workoutExerciseId: String) : LiveWorkoutEvent
    data class SetDeleted(val setId: String) : LiveWorkoutEvent
    data class SetTypeClicked(val setId: String) : LiveWorkoutEvent
    data class SetTypeChosen(val setId: String, val type: SetType) : LiveWorkoutEvent
    data object SetTypeMenuDismissed : LiveWorkoutEvent
    data class ValueChanged(val setId: String, val column: SetColumn, val text: String) : LiveWorkoutEvent
    data class CompletedChanged(val setId: String, val completed: Boolean) : LiveWorkoutEvent
    data object RestMinus15 : LiveWorkoutEvent
    data object RestPlus15 : LiveWorkoutEvent
    data object RestSkipped : LiveWorkoutEvent
}
