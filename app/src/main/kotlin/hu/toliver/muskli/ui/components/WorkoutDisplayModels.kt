package hu.toliver.muskli.ui.components

import androidx.compose.runtime.Immutable
import hu.toliver.muskli.domain.model.SetType

/**
 * What one set row shows. Values arrive already formatted for the display unit;
 * conversion from kg happens before this model is built.
 */
@Immutable
data class WorkoutSetUi(
    val id: String,
    val number: Int,
    val type: SetType,
    val previousWeight: String?,
    val previousReps: String?,
    val weight: String,
    val reps: String,
    val completed: Boolean,
)

@Immutable
data class WorkoutExerciseUi(
    val id: String,
    val name: String,
    val sets: List<WorkoutSetUi>,
)

@Immutable
data class RestTimerUi(
    val remaining: String,
    val progress: Float,
)
