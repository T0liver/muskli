package hu.toliver.muskli.domain.model

/**
 * A workout; a draft (live workout) while [finishedAtEpochMs] is null. [utcOffsetS] is the
 * offset at the start, so day and week boundaries do not shift when the phone changes time zone.
 * Order of [exercises] is the order in the workout.
 */
data class Workout(
    val id: String,
    val title: String,
    val notes: String?,
    val startedAtEpochMs: Long,
    val utcOffsetS: Int,
    val finishedAtEpochMs: Long?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val deletedAtEpochMs: Long?,
    val exercises: List<WorkoutExercise>,
    /** The running rest of the live workout, null when none (ARCHITECTURE §5). */
    val restTimer: RestTimer? = null,
    /** The routine the workout was started from; null for an empty start or once that routine is deleted (D-052). */
    val routineId: String? = null,
)

/**
 * A rest stored as its end time, so time left is derived from the clock and survives process
 * death. [durationS] is the full length after ±15 s adjustments; it only drives progress.
 */
data class RestTimer(
    val endsAtEpochMs: Long,
    val durationS: Int,
)

/** [restSeconds]: null falls back to the default rest, 0 turns rest off (see `restTimerEffect`). */
data class WorkoutExercise(
    val id: String,
    val exerciseTemplateId: String,
    val supersetGroup: Int?,
    val notes: String?,
    val restSeconds: Int?,
    val sets: List<WorkoutSet>,
)

data class WorkoutSet(
    val id: String,
    val data: SetData,
    val completedAtEpochMs: Long?,
)
