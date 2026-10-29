package hu.toliver.muskli.domain.calc

import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side

/** The sets of one exercise in a workout, with what volume needs to know about the exercise. */
data class ExerciseSets(
    val type: ExerciseType,
    val sets: List<SetData>,
    val unilateral: Boolean = false,
)

/**
 * Volume of a single set in kg (D-010), ignoring whether it is completed or a warm-up.
 * [bodyweightKg] null means body weight is unset (D-017): bodyweight and assisted sets give 0.
 * Only [unilateral] exercises count a [Side.BOTH] set twice (D-014), because every set
 * defaults to [Side.BOTH] (D-022).
 */
fun setVolumeKg(
    set: SetData,
    type: ExerciseType,
    bodyweightKg: Double?,
    unilateral: Boolean = false,
): Double {
    val reps = set.reps?.takeIf { it > 0 } ?: return 0.0
    val loadKg = when (type) {
        ExerciseType.WEIGHT_REPS -> set.weightKg ?: return 0.0
        ExerciseType.BODYWEIGHT_REPS -> (bodyweightKg ?: return 0.0) + (set.weightKg ?: 0.0)
        ExerciseType.BODYWEIGHT_ASSISTED_REPS -> (bodyweightKg ?: return 0.0) - (set.weightKg ?: 0.0)
        else -> return 0.0
    }
    if (loadKg <= 0.0) return 0.0
    val sides = if (unilateral && set.side == Side.BOTH) 2 else 1
    return loadKg * reps * sides
}

/** Sum of completed sets across exercises; warm-ups are excluded unless [includeWarmUps]. */
fun sessionVolumeKg(
    exercises: List<ExerciseSets>,
    bodyweightKg: Double?,
    includeWarmUps: Boolean = false,
): Double = exercises.sumOf { exercise ->
    exercise.sets
        .filter { it.completed && (includeWarmUps || it.type != SetType.WARMUP) }
        .sumOf { setVolumeKg(it, exercise.type, bodyweightKg, exercise.unilateral) }
}
