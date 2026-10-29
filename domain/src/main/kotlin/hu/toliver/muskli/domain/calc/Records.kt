package hu.toliver.muskli.domain.calc

import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType

enum class RecordType { HEAVIEST_WEIGHT, ESTIMATED_ONE_REP_MAX, MOST_REPS, BEST_SET_VOLUME, BEST_DURATION }

/** A set of one exercise from a workout in its history. */
data class HistorySet(
    val workoutId: String,
    val performedAtEpochMs: Long,
    val set: SetData,
)

data class Record(
    val type: RecordType,
    val value: Double,
    val workoutId: String,
)

private val heaviestWeightTypes =
    setOf(ExerciseType.WEIGHT_REPS, ExerciseType.WEIGHT_DURATION, ExerciseType.SHORT_DISTANCE_WEIGHT)
private val mostRepsTypes = setOf(
    ExerciseType.WEIGHT_REPS,
    ExerciseType.REPS_ONLY,
    ExerciseType.BODYWEIGHT_REPS,
    ExerciseType.BODYWEIGHT_ASSISTED_REPS,
)
private val bestDurationTypes =
    setOf(ExerciseType.DURATION, ExerciseType.WEIGHT_DURATION, ExerciseType.DISTANCE_DURATION)

private fun SetData.qualifies(): Boolean = completed && type != SetType.WARMUP

private fun Double.positiveOrNull(): Double? = takeIf { it > 0.0 }

/** Which exercise types each category applies to follows research/05 §5 (owner-approved, D-027). */
private fun recordValue(
    record: RecordType,
    set: SetData,
    exerciseType: ExerciseType,
    formula: OneRmFormula,
): Double? {
    val weight = set.weightKg?.positiveOrNull()
    val reps = set.reps?.takeIf { it > 0 }
    return when (record) {
        RecordType.HEAVIEST_WEIGHT -> weight.takeIf { exerciseType in heaviestWeightTypes }
        RecordType.ESTIMATED_ONE_REP_MAX ->
            if (exerciseType == ExerciseType.WEIGHT_REPS && weight != null && reps != null) {
                estimateOneRepMax(weight, reps, formula)
            } else {
                null
            }
        RecordType.MOST_REPS -> reps?.toDouble().takeIf { exerciseType in mostRepsTypes }
        RecordType.BEST_SET_VOLUME ->
            if (exerciseType == ExerciseType.WEIGHT_REPS && weight != null && reps != null) weight * reps else null
        RecordType.BEST_DURATION ->
            set.durationS?.takeIf { it > 0 }?.toDouble().takeIf { exerciseType in bestDurationTypes }
    }
}

/**
 * Records recomputed from [history] every time, so editing or deleting a workout is reflected
 * without stored state. Ties go to the earliest set, because a record needs a strictly greater value.
 */
fun currentRecords(
    history: List<HistorySet>,
    exerciseType: ExerciseType,
    formula: OneRmFormula,
): Map<RecordType, Record> {
    val records = mutableMapOf<RecordType, Record>()
    history
        .filter { it.set.qualifies() }
        .sortedBy { it.performedAtEpochMs }
        .forEach { entry ->
            RecordType.entries.forEach { type ->
                val value = recordValue(type, entry.set, exerciseType, formula) ?: return@forEach
                val current = records[type]
                if (current == null || value > current.value) {
                    records[type] = Record(type, value, entry.workoutId)
                }
            }
        }
    return records
}

/** The records [candidate] would break against [history]; empty for warm-up or uncompleted sets. */
fun recordsBrokenBy(
    candidate: SetData,
    history: List<HistorySet>,
    exerciseType: ExerciseType,
    formula: OneRmFormula,
): Set<RecordType> {
    if (!candidate.qualifies()) return emptySet()
    val records = currentRecords(history, exerciseType, formula)
    return RecordType.entries.filterTo(mutableSetOf()) { type ->
        val value = recordValue(type, candidate, exerciseType, formula) ?: return@filterTo false
        val current = records[type]
        current == null || value > current.value
    }
}

/** Best weight lifted for N **or more** reps (D-020), keyed by N from 1 to the most reps logged. */
fun setRecords(history: List<HistorySet>): Map<Int, Double> {
    val sets = history.map { it.set }.filter { it.qualifies() }
    val bestByExactReps = sets
        .mapNotNull { set ->
            val weight = set.weightKg?.positiveOrNull() ?: return@mapNotNull null
            val reps = set.reps?.takeIf { it > 0 } ?: return@mapNotNull null
            reps to weight
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, weights) -> weights.max() }
    val maxReps = bestByExactReps.keys.maxOrNull() ?: return emptyMap()
    val records = sortedMapOf<Int, Double>()
    var best = 0.0
    for (reps in maxReps downTo 1) {
        best = maxOf(best, bestByExactReps[reps] ?: 0.0)
        records[reps] = best
    }
    return records
}
