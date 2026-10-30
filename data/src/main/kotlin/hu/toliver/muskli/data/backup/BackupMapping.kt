package hu.toliver.muskli.data.backup

import hu.toliver.muskli.data.db.entity.ExerciseTemplateEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateSecondaryMuscleEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateWithMuscles
import hu.toliver.muskli.data.db.entity.WorkoutEntity
import hu.toliver.muskli.data.db.entity.WorkoutExerciseEntity
import hu.toliver.muskli.data.db.entity.WorkoutSetEntity
import hu.toliver.muskli.data.db.entity.WorkoutWithExercises
import hu.toliver.muskli.data.db.toEnum
import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side
import hu.toliver.muskli.domain.repo.ImportError
import hu.toliver.muskli.domain.repo.ImportProblem
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit

internal fun ExerciseTemplateWithMuscles.toJson() = ExerciseJson(
    id = template.id,
    title = template.title,
    type = template.exerciseType.lower(),
    primaryMuscle = template.primaryMuscle.lower(),
    secondaryMuscles = secondaryMuscles.map { it.muscle }.sorted().map { it.lower() },
    equipment = template.equipment.lower(),
    isCustom = template.isCustom,
    archived = template.archived,
    createdAtMs = template.createdAt,
    updatedAtMs = template.updatedAt,
)

internal fun WorkoutWithExercises.toJson() = WorkoutJson(
    id = workout.id,
    title = workout.title,
    description = workout.notes,
    startTimeMs = workout.startTime,
    endTimeMs = workout.endTime,
    utcOffsetS = workout.utcOffsetS,
    createdAtMs = workout.createdAt,
    updatedAtMs = workout.updatedAt,
    deletedAtMs = workout.deletedAt,
    restEndsAtMs = workout.restEndsAt,
    restDurationS = workout.restDurationS,
    exercises = exercises.sortedBy { it.exercise.position }.map { (exercise, sets) ->
        WorkoutExerciseJson(
            index = exercise.position,
            id = exercise.id,
            exerciseTemplateId = exercise.exerciseTemplateId,
            supersetId = exercise.supersetGroup,
            notes = exercise.notes,
            restSeconds = exercise.restSeconds,
            sets = sets.sortedBy { it.position }.map { set ->
                SetJson(
                    index = set.position,
                    id = set.id,
                    type = set.setType.lower(),
                    weightKg = set.weightKg,
                    reps = set.reps,
                    distanceMeters = set.distanceM,
                    durationSeconds = set.durationS,
                    completed = set.completed,
                    completedAtMs = set.completedAt,
                    side = set.side.lower(),
                )
            },
        )
    },
)

internal fun Settings.toJson() = SettingsJson(
    weightUnit = weightUnit.lower(),
    defaultRestSeconds = defaultRestSeconds,
    previousMode = previousMode.lower(),
    keepScreenAwake = keepScreenAwake,
    soundEnabled = soundEnabled,
    vibrationEnabled = vibrationEnabled,
    weekStart = weekStart.lower(),
    bodyWeightKg = bodyWeightKg,
)

/** A file that passed [validate], as rows ready to insert. */
internal data class ValidBackup(
    val templates: List<ExerciseTemplateEntity>,
    val secondaryMuscles: List<ExerciseTemplateSecondaryMuscleEntity>,
    val workouts: List<WorkoutEntity>,
    val exercises: List<WorkoutExerciseEntity>,
    val sets: List<WorkoutSetEntity>,
    val settings: Settings?,
)

/** Collects every problem rather than stopping at the first, so one report lists all bad records. */
internal fun BackupFile.validate(): Pair<ValidBackup?, List<ImportError>> {
    val errors = mutableListOf<ImportError>()
    if (schemaVersion > BackupFile.CURRENT_SCHEMA_VERSION) {
        return null to listOf(ImportError(ImportProblem.UNSUPPORTED_VERSION, "schemaVersion"))
    }
    if (schemaVersion < 1) return null to listOf(ImportError(ImportProblem.INVALID_VALUE, "schemaVersion"))

    fun invalid(path: String) {
        errors += ImportError(ImportProblem.INVALID_VALUE, path)
    }

    fun <T> parse(path: String, value: () -> T): T? = runCatching(value).getOrNull().also { if (it == null) invalid(path) }

    fun nonNegative(path: String, value: Number?) {
        if (value != null && !(value.toDouble() >= 0.0 && value.toDouble().isFinite())) invalid(path)
    }

    val templateIds = mutableSetOf<String>()
    val templates = mutableListOf<ExerciseTemplateEntity>()
    val muscles = mutableListOf<ExerciseTemplateSecondaryMuscleEntity>()
    exercises.forEachIndexed { i, json ->
        val path = "exercises[$i]"
        if (json.id.isBlank()) invalid("$path.id")
        if (!templateIds.add(json.id)) errors += ImportError(ImportProblem.DUPLICATE_ID, "$path.id")
        if (json.title.isBlank()) invalid("$path.title")
        val type = parse("$path.type") { json.type.toEnum<ExerciseType>() }
        val primary = parse("$path.primaryMuscle") { json.primaryMuscle.toEnum<MuscleGroup>() }
        val equipment = parse("$path.equipment") { json.equipment.toEnum<Equipment>() }
        val secondary = json.secondaryMuscles.mapIndexedNotNull { m, name ->
            parse("$path.secondaryMuscles[$m]") { name.toEnum<MuscleGroup>() }
        }
        if (type != null && primary != null && equipment != null) {
            templates += ExerciseTemplateEntity(
                id = json.id,
                title = json.title,
                exerciseType = type,
                primaryMuscle = primary,
                equipment = equipment,
                isCustom = json.isCustom,
                archived = json.archived,
                createdAt = json.createdAtMs,
                updatedAt = json.updatedAtMs,
            )
            secondary.toSet().forEach { muscles += ExerciseTemplateSecondaryMuscleEntity(json.id, it) }
        }
    }

    val workoutIds = mutableSetOf<String>()
    val exerciseIds = mutableSetOf<String>()
    val setIds = mutableSetOf<String>()
    val workoutRows = mutableListOf<WorkoutEntity>()
    val exerciseRows = mutableListOf<WorkoutExerciseEntity>()
    val setRows = mutableListOf<WorkoutSetEntity>()
    workouts.forEachIndexed { w, json ->
        val path = "workouts[$w]"
        if (json.id.isBlank()) invalid("$path.id")
        if (!workoutIds.add(json.id)) errors += ImportError(ImportProblem.DUPLICATE_ID, "$path.id")
        if (json.endTimeMs != null && json.endTimeMs < json.startTimeMs) invalid("$path.endTimeMs")
        nonNegative("$path.restDurationS", json.restDurationS)
        if (json.exercises.map { it.index }.let { it.toSet().size != it.size || it.any { i -> i < 0 } }) {
            invalid("$path.exercises.index")
        }
        workoutRows += WorkoutEntity(
            id = json.id,
            title = json.title,
            notes = json.description,
            startTime = json.startTimeMs,
            utcOffsetS = json.utcOffsetS,
            endTime = json.endTimeMs,
            createdAt = json.createdAtMs,
            updatedAt = json.updatedAtMs,
            deletedAt = json.deletedAtMs,
            restEndsAt = json.restEndsAtMs,
            restDurationS = json.restDurationS,
        )
        json.exercises.forEachIndexed { e, exercise ->
            val exercisePath = "$path.exercises[$e]"
            if (exercise.id.isBlank()) invalid("$exercisePath.id")
            if (!exerciseIds.add(exercise.id)) errors += ImportError(ImportProblem.DUPLICATE_ID, "$exercisePath.id")
            if (exercise.exerciseTemplateId !in templateIds) {
                errors += ImportError(ImportProblem.UNKNOWN_EXERCISE, "$exercisePath.exerciseTemplateId")
            }
            nonNegative("$exercisePath.restSeconds", exercise.restSeconds)
            if (exercise.sets.map { it.index }.let { it.toSet().size != it.size || it.any { i -> i < 0 } }) {
                invalid("$exercisePath.sets.index")
            }
            exerciseRows += WorkoutExerciseEntity(
                id = exercise.id,
                workoutId = json.id,
                position = exercise.index,
                exerciseTemplateId = exercise.exerciseTemplateId,
                supersetGroup = exercise.supersetId,
                notes = exercise.notes,
                restSeconds = exercise.restSeconds,
            )
            exercise.sets.forEachIndexed { s, set ->
                val setPath = "$exercisePath.sets[$s]"
                if (set.id.isBlank()) invalid("$setPath.id")
                if (!setIds.add(set.id)) errors += ImportError(ImportProblem.DUPLICATE_ID, "$setPath.id")
                nonNegative("$setPath.weightKg", set.weightKg)
                nonNegative("$setPath.reps", set.reps)
                nonNegative("$setPath.distanceMeters", set.distanceMeters)
                nonNegative("$setPath.durationSeconds", set.durationSeconds)
                val type = parse("$setPath.type") { set.type.toEnum<SetType>() }
                val side = parse("$setPath.side") { set.side.toEnum<Side>() }
                if (type != null && side != null) {
                    setRows += WorkoutSetEntity(
                        id = set.id,
                        workoutExerciseId = exercise.id,
                        position = set.index,
                        setType = type,
                        weightKg = set.weightKg,
                        reps = set.reps,
                        distanceM = set.distanceMeters,
                        durationS = set.durationSeconds,
                        completed = set.completed,
                        completedAt = set.completedAtMs,
                        side = side,
                    )
                }
            }
        }
    }
    if (workouts.count { it.endTimeMs == null && it.deletedAtMs == null } > 1) {
        errors += ImportError(ImportProblem.MULTIPLE_DRAFTS, "workouts")
    }

    val settings = settings?.let { json ->
        val unit = parse("settings.weightUnit") { json.weightUnit.toEnum<WeightUnit>() }
        val mode = parse("settings.previousMode") { json.previousMode.toEnum<PreviousMode>() }
        val weekStart = parse("settings.weekStart") { json.weekStart.toEnum<WeekStartDay>() }
        if (json.defaultRestSeconds != null && json.defaultRestSeconds <= 0) invalid("settings.defaultRestSeconds")
        if (json.bodyWeightKg != null && !(json.bodyWeightKg > 0.0 && json.bodyWeightKg.isFinite())) {
            invalid("settings.bodyWeightKg")
        }
        if (unit == null || mode == null || weekStart == null) {
            null
        } else {
            Settings(
                weightUnit = unit,
                defaultRestSeconds = json.defaultRestSeconds,
                previousMode = mode,
                keepScreenAwake = json.keepScreenAwake,
                soundEnabled = json.soundEnabled,
                vibrationEnabled = json.vibrationEnabled,
                weekStart = weekStart,
                bodyWeightKg = json.bodyWeightKg,
            )
        }
    }

    if (errors.isNotEmpty()) return null to errors
    return ValidBackup(templates, muscles, workoutRows, exerciseRows, setRows, settings) to emptyList()
}

private fun Enum<*>.lower() = name.lowercase()
