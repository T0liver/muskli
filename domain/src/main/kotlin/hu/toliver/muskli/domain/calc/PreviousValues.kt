package hu.toliver.muskli.domain.calc

import hu.toliver.muskli.domain.model.SetData

enum class PreviousMode { ANYWHERE, ROUTINE_SCOPED }

/** One occurrence of an exercise in a past workout, with what the lookup needs about that workout. */
data class WorkoutExerciseRecord(
    val workoutId: String,
    val workoutStartedAtEpochMs: Long,
    val workoutFinishedAtEpochMs: Long?,
    val workoutDeletedAtEpochMs: Long?,
    val positionInWorkout: Int,
    val sets: List<SetData>,
    val routineId: String? = null,
)

/**
 * The previous set for position [setIndex] of one exercise, given that exercise's [history].
 * Aligns by raw index, warm-ups included; an uncompleted set at that index is not a previous
 * value and does not shift the sets after it (D-019). A repeated exercise uses its first
 * occurrence in the workout. Workouts are ordered by start time so a back-dated workout does
 * not become "last time".
 */
fun previousFor(
    history: List<WorkoutExerciseRecord>,
    setIndex: Int,
    mode: PreviousMode,
    routineId: String?,
): SetData? {
    if (mode == PreviousMode.ROUTINE_SCOPED) TODO("T-024: routine-scoped previous values (TC-PREV-03)")
    val finished = history.filter { it.workoutFinishedAtEpochMs != null && it.workoutDeletedAtEpochMs == null }
    val lastWorkoutId = finished.maxByOrNull { it.workoutStartedAtEpochMs }?.workoutId ?: return null
    val occurrence = finished
        .filter { it.workoutId == lastWorkoutId }
        .minBy { it.positionInWorkout }
    return occurrence.sets.getOrNull(setIndex)?.takeIf { it.completed }
}
