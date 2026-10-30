package hu.toliver.muskli.ui.components

import androidx.compose.runtime.Immutable
import hu.toliver.muskli.domain.model.SetType

/** One input column of a set row; which ones an exercise shows depends on its type (F-03 AC2). */
enum class SetColumn { WEIGHT, ADDED_WEIGHT, ASSISTANCE, REPS, DURATION, DISTANCE_KM, DISTANCE_M }

/**
 * What one set row shows. Values arrive already formatted for the display unit;
 * conversion from kg happens before this model is built. [values] and [previous] follow the
 * exercise's columns; [previous] is null when there is no previous value for this position.
 */
@Immutable
data class WorkoutSetUi(
    val id: String,
    val number: Int,
    val type: SetType,
    val previous: List<String>?,
    val values: List<String>,
    val completed: Boolean,
)

@Immutable
data class WorkoutExerciseUi(
    val id: String,
    val name: String,
    val columns: List<SetColumn>,
    val sets: List<WorkoutSetUi>,
)

@Immutable
data class RestTimerUi(
    val remaining: String,
    val progress: Float,
)
