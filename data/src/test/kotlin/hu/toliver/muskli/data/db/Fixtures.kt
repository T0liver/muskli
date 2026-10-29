package hu.toliver.muskli.data.db

import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet

val benchPress = ExerciseTemplate(
    id = "bench-press",
    title = "Bench press",
    type = ExerciseType.WEIGHT_REPS,
    primaryMuscle = MuscleGroup.CHEST,
    secondaryMuscles = setOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
    equipment = Equipment.BARBELL,
    isCustom = false,
    archived = false,
    createdAtEpochMs = 1_000L,
    updatedAtEpochMs = 1_000L,
)

val dumbbellRow = ExerciseTemplate(
    id = "dumbbell-row",
    title = "Dumbbell row",
    type = ExerciseType.WEIGHT_REPS,
    primaryMuscle = MuscleGroup.UPPER_BACK,
    secondaryMuscles = setOf(MuscleGroup.LATS, MuscleGroup.BICEPS),
    equipment = Equipment.DUMBBELL,
    isCustom = true,
    archived = false,
    createdAtEpochMs = 2_000L,
    updatedAtEpochMs = 2_000L,
)

fun workout(
    id: String = "w1",
    startedAt: Long = 10_000L,
    finishedAt: Long? = 20_000L,
    deletedAt: Long? = null,
) = Workout(
    id = id,
    title = "Push",
    notes = "felt strong",
    startedAtEpochMs = startedAt,
    utcOffsetS = 3_600,
    finishedAtEpochMs = finishedAt,
    createdAtEpochMs = startedAt,
    updatedAtEpochMs = startedAt,
    deletedAtEpochMs = deletedAt,
    exercises = listOf(
        WorkoutExercise(
            id = "$id-e1",
            exerciseTemplateId = benchPress.id,
            supersetGroup = 1,
            notes = "pause reps",
            restSeconds = 120,
            sets = listOf(
                WorkoutSet("$id-e1-s1", SetData(SetType.WARMUP, weightKg = 40.0, reps = 10, completed = true), 11_000L),
                WorkoutSet("$id-e1-s2", SetData(SetType.NORMAL, weightKg = 80.0, reps = 5, completed = true), 12_000L),
                WorkoutSet("$id-e1-s3", SetData(SetType.FAILURE, weightKg = 80.0, reps = null), null),
            ),
        ),
        WorkoutExercise(
            id = "$id-e2",
            exerciseTemplateId = dumbbellRow.id,
            supersetGroup = 1,
            notes = null,
            restSeconds = 0,
            sets = listOf(
                WorkoutSet("$id-e2-s1", SetData(weightKg = 30.0, reps = 8, completed = true, side = Side.LEFT), 13_000L),
                WorkoutSet("$id-e2-s2", SetData(weightKg = 30.0, reps = 8, completed = true, side = Side.RIGHT), 14_000L),
            ),
        ),
    ),
)
