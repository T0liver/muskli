package hu.toliver.muskli.data.db

import hu.toliver.muskli.data.db.entity.ExerciseTemplateEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateSecondaryMuscleEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateWithMuscles
import hu.toliver.muskli.data.db.entity.WorkoutEntity
import hu.toliver.muskli.data.db.entity.WorkoutExerciseEntity
import hu.toliver.muskli.data.db.entity.WorkoutSetEntity
import hu.toliver.muskli.data.db.entity.WorkoutWithExercises
import hu.toliver.muskli.domain.calc.WorkoutExerciseRecord
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet

fun ExerciseTemplate.toEntity() = ExerciseTemplateEntity(
    id = id,
    title = title,
    exerciseType = type,
    primaryMuscle = primaryMuscle,
    equipment = equipment,
    isCustom = isCustom,
    archived = archived,
    createdAt = createdAtEpochMs,
    updatedAt = updatedAtEpochMs,
)

fun ExerciseTemplate.secondaryMuscleEntities() =
    secondaryMuscles.map { ExerciseTemplateSecondaryMuscleEntity(id, it) }

fun ExerciseTemplateWithMuscles.toDomain() = ExerciseTemplate(
    id = template.id,
    title = template.title,
    type = template.exerciseType,
    primaryMuscle = template.primaryMuscle,
    secondaryMuscles = secondaryMuscles.map { it.muscle }.toSet(),
    equipment = template.equipment,
    isCustom = template.isCustom,
    archived = template.archived,
    createdAtEpochMs = template.createdAt,
    updatedAtEpochMs = template.updatedAt,
)

/** Rows for [hu.toliver.muskli.data.db.dao.WorkoutDao.save]; list order becomes `position`. */
data class WorkoutRows(
    val workout: WorkoutEntity,
    val exercises: List<WorkoutExerciseEntity>,
    val sets: List<WorkoutSetEntity>,
)

fun Workout.toRows(): WorkoutRows {
    val exerciseRows = exercises.mapIndexed { position, exercise ->
        WorkoutExerciseEntity(
            id = exercise.id,
            workoutId = id,
            position = position,
            exerciseTemplateId = exercise.exerciseTemplateId,
            supersetGroup = exercise.supersetGroup,
            notes = exercise.notes,
            restSeconds = exercise.restSeconds,
        )
    }
    val setRows = exercises.flatMap { exercise ->
        exercise.sets.mapIndexed { position, set ->
            WorkoutSetEntity(
                id = set.id,
                workoutExerciseId = exercise.id,
                position = position,
                setType = set.data.type,
                weightKg = set.data.weightKg,
                reps = set.data.reps,
                distanceM = set.data.distanceM,
                durationS = set.data.durationS,
                completed = set.data.completed,
                completedAt = set.completedAtEpochMs,
                side = set.data.side,
            )
        }
    }
    val workoutRow = WorkoutEntity(
        id = id,
        title = title,
        notes = notes,
        startTime = startedAtEpochMs,
        utcOffsetS = utcOffsetS,
        endTime = finishedAtEpochMs,
        createdAt = createdAtEpochMs,
        updatedAt = updatedAtEpochMs,
        deletedAt = deletedAtEpochMs,
    )
    return WorkoutRows(workoutRow, exerciseRows, setRows)
}

// @Relation does not keep order, so exercises and sets are sorted by position here.
fun WorkoutWithExercises.toDomain() = Workout(
    id = workout.id,
    title = workout.title,
    notes = workout.notes,
    startedAtEpochMs = workout.startTime,
    utcOffsetS = workout.utcOffsetS,
    finishedAtEpochMs = workout.endTime,
    createdAtEpochMs = workout.createdAt,
    updatedAtEpochMs = workout.updatedAt,
    deletedAtEpochMs = workout.deletedAt,
    exercises = exercises.sortedBy { it.exercise.position }.map { row ->
        WorkoutExercise(
            id = row.exercise.id,
            exerciseTemplateId = row.exercise.exerciseTemplateId,
            supersetGroup = row.exercise.supersetGroup,
            notes = row.exercise.notes,
            restSeconds = row.exercise.restSeconds,
            sets = row.sets.sortedBy { it.position }.map { it.toDomain() },
        )
    },
)

private fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = id,
    data = SetData(
        type = setType,
        weightKg = weightKg,
        reps = reps,
        durationS = durationS,
        distanceM = distanceM,
        completed = completed,
        side = side,
    ),
    completedAtEpochMs = completedAt,
)

/** One record per occurrence of [exerciseTemplateId] in the workout, in the shape `previousFor` takes. */
fun WorkoutWithExercises.toExerciseRecords(exerciseTemplateId: String): List<WorkoutExerciseRecord> {
    val domain = toDomain()
    return domain.exercises.withIndex()
        .filter { it.value.exerciseTemplateId == exerciseTemplateId }
        .map { (position, exercise) ->
            WorkoutExerciseRecord(
                workoutId = domain.id,
                workoutStartedAtEpochMs = domain.startedAtEpochMs,
                workoutFinishedAtEpochMs = domain.finishedAtEpochMs,
                workoutDeletedAtEpochMs = domain.deletedAtEpochMs,
                positionInWorkout = position,
                sets = exercise.sets.map { it.data },
            )
        }
}
