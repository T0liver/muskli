package hu.toliver.muskli.domain.importer.hevy

import hu.toliver.muskli.domain.model.SetData

/** Result of reading one Hevy export file (docs/HEVY-IMPORT.md, D-040). */
sealed interface HevyParseResult {
    data class Workouts(val workouts: List<HevyWorkout>) : HevyParseResult

    /** A measurement file; only the latest body weight is used (D-045). */
    data class Measurements(val latestBodyWeight: HevyBodyWeight?) : HevyParseResult

    /** All or nothing: any problem rejects the whole file. */
    data class Rejected(val errors: List<HevyParseError>) : HevyParseResult
}

data class HevyWorkout(
    val title: String,
    val notes: String?,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val exercises: List<HevyExercise>,
)

/** One exercise occurrence; [supersetGroup] is renumbered per workout from 0. */
data class HevyExercise(
    val title: String,
    val supersetGroup: Int?,
    val notes: String?,
    val sets: List<HevySet>,
)

/** [rpe] is kept only here; Muskli has no RPE field, so it is dropped on import (D-042). */
data class HevySet(val data: SetData, val rpe: Double?)

data class HevyBodyWeight(val weightKg: Double, val measuredAtEpochMs: Long)

/** [record] is the 1-based CSV record number (header is 1); [column] is null when not column-specific. */
data class HevyParseError(val problem: HevyProblem, val record: Int, val column: String?)

enum class HevyProblem {
    NOT_HEVY_EXPORT,
    MISSING_COLUMN,
    FIELD_COUNT,
    INVALID_DATE,
    INVALID_NUMBER,
    NEGATIVE_NUMBER,
    UNKNOWN_SET_TYPE,
    END_BEFORE_START,
}
