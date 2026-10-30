package hu.toliver.muskli.ui.history

import androidx.compose.runtime.Immutable
import hu.toliver.muskli.domain.calc.ExerciseSets
import hu.toliver.muskli.domain.calc.completedSetCount
import hu.toliver.muskli.domain.calc.kgToLb
import hu.toliver.muskli.domain.calc.sessionVolumeKg
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.components.formatWorkoutDateTime
import hu.toliver.muskli.ui.workout.columnsFor
import hu.toliver.muskli.ui.workout.format
import java.text.NumberFormat
import kotlin.math.roundToLong

/** One logged set, read-only. [values] follow the exercise's columns, already in the display unit. */
@Immutable
data class SetSummaryUi(
    val number: Int,
    val type: SetType,
    val values: List<String>,
)

@Immutable
data class ExerciseSummaryUi(
    val templateId: String,
    val name: String,
    val columns: List<SetColumn>,
    val sets: List<SetSummaryUi>,
)

/** A history row. [title] is empty for an unnamed workout; the screen shows a fallback. */
@Immutable
data class WorkoutSummaryUi(
    val id: String,
    val title: String,
    val dateTime: String,
    val durationS: Long,
    val volume: String,
    val completedSets: Int,
    val exerciseNames: List<String>,
)

/** Numbered like the live screen: warm-ups carry a marker and take no number. */
fun summarizeSets(sets: List<SetData>, columns: List<SetColumn>, unit: WeightUnit): List<SetSummaryUi> {
    var working = 0
    return sets.map { set ->
        if (set.type != SetType.WARMUP) working++
        SetSummaryUi(
            number = if (set.type == SetType.WARMUP) working + 1 else working,
            type = set.type,
            values = columns.map { it.format(set, unit) },
        )
    }
}

fun summarizeExercises(workout: Workout, templatesById: Map<String, ExerciseTemplate>, unit: WeightUnit): List<ExerciseSummaryUi> =
    workout.exercises.map { exercise ->
        val template = templatesById[exercise.exerciseTemplateId]
        val columns = columnsFor(template?.type ?: ExerciseType.WEIGHT_REPS)
        ExerciseSummaryUi(
            templateId = exercise.exerciseTemplateId,
            name = template?.title.orEmpty(),
            columns = columns,
            sets = summarizeSets(exercise.sets.map { it.data }, columns, unit),
        )
    }

fun summarizeWorkout(workout: Workout, templatesById: Map<String, ExerciseTemplate>, unit: WeightUnit, bodyWeightKg: Double?): WorkoutSummaryUi {
    val exerciseSets = workout.exercises.map { exercise ->
        ExerciseSets(templatesById[exercise.exerciseTemplateId]?.type ?: ExerciseType.WEIGHT_REPS, exercise.sets.map { it.data })
    }
    return WorkoutSummaryUi(
        id = workout.id,
        title = workout.title,
        dateTime = formatWorkoutDateTime(workout.startedAtEpochMs, workout.utcOffsetS),
        durationS = workoutDurationS(workout),
        volume = formatVolume(sessionVolumeKg(exerciseSets, bodyWeightKg), unit),
        completedSets = completedSetCount(exerciseSets),
        exerciseNames = workout.exercises.map { templatesById[it.exerciseTemplateId]?.title.orEmpty() },
    )
}

fun workoutDurationS(workout: Workout): Long =
    ((workout.finishedAtEpochMs ?: workout.startedAtEpochMs) - workout.startedAtEpochMs).coerceAtLeast(0) / 1000

/** Whole numbers in [unit], like the live header. */
fun formatVolume(volumeKg: Double, unit: WeightUnit): String =
    NumberFormat.getIntegerInstance().format((if (unit == WeightUnit.LB) kgToLb(volumeKg) else volumeKg).roundToLong())
