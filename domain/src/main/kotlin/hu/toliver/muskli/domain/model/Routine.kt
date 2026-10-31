package hu.toliver.muskli.domain.model

/**
 * A saved plan a live workout can start from (D-052). It is edited and stored as a whole, so its
 * exercises and sets have no ids of their own; list order is the order in the routine.
 */
data class Routine(
    val id: String,
    val title: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val exercises: List<RoutineExercise>,
)

/** [notes] and [restSeconds] are copied to the workout exercise; [restSeconds] follows [WorkoutExercise.restSeconds]. */
data class RoutineExercise(
    val exerciseTemplateId: String,
    val supersetGroup: Int? = null,
    val notes: String? = null,
    val restSeconds: Int? = null,
    val sets: List<PlannedSet>,
)

/** The planned values of one set, in the same units as [SetData]. A null value is left empty in the workout. */
data class PlannedSet(
    val type: SetType = SetType.NORMAL,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationS: Int? = null,
    val distanceM: Double? = null,
)
