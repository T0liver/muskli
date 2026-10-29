package hu.toliver.muskli.domain.model

enum class SetType { NORMAL, WARMUP, DROPSET, FAILURE }

enum class ExerciseType {
    WEIGHT_REPS,
    REPS_ONLY,
    BODYWEIGHT_REPS,
    BODYWEIGHT_ASSISTED_REPS,
    DURATION,
    WEIGHT_DURATION,
    DISTANCE_DURATION,
    SHORT_DISTANCE_WEIGHT,
}

/** Which side a set was performed on (D-014). Only meaningful for unilateral exercises. */
enum class Side { LEFT, RIGHT, BOTH }

/**
 * One logged set. [weightKg] is the load for weight types, the added weight for
 * [ExerciseType.BODYWEIGHT_REPS] and the assistance for [ExerciseType.BODYWEIGHT_ASSISTED_REPS].
 */
data class SetData(
    val type: SetType = SetType.NORMAL,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationS: Int? = null,
    val distanceM: Double? = null,
    val completed: Boolean = false,
    val side: Side = Side.BOTH,
)
