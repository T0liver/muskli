package hu.toliver.muskli.domain.importer.hevy

import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.IdGenerator

/**
 * What a Hevy import will write, ready for one transaction (D-044). The counts and the first and
 * last start describe [newWorkouts] only, except [workoutsInFile] and [duplicatesSkipped].
 */
data class HevyImportPlan(
    val newWorkouts: List<Workout>,
    val newTemplates: List<ExerciseTemplate>,
    val workoutsInFile: Int,
    val duplicatesSkipped: Int,
    val setCount: Int,
    val rpeDropped: Int,
    val firstStartEpochMs: Long?,
    val lastStartEpochMs: Long?,
)

private const val MINUTE_MS = 60_000L

/**
 * Turns parsed [workouts] and the user's [resolutions] (one per exercise title) into new rows.
 * [existingStartsEpochMs] must include drafts and soft-deleted workouts, so a deleted import stays
 * deleted. A workout whose start minute equals any of them is a duplicate and skipped (D-044).
 */
fun planHevyImport(
    workouts: List<HevyWorkout>,
    resolutions: Map<String, HevyResolution>,
    existingStartsEpochMs: Collection<Long>,
    ids: IdGenerator,
    clock: Clock,
): HevyImportPlan {
    val existingMinutes = existingStartsEpochMs.mapTo(HashSet()) { Math.floorDiv(it, MINUTE_MS) }
    val imported = workouts.filterNot { Math.floorDiv(it.startedAtEpochMs, MINUTE_MS) in existingMinutes }
    val now = clock.nowEpochMs()

    val setsByTitle = workouts.flatMap { it.exercises }.groupBy({ it.title }, { e -> e.sets.map { it.data } })
    val templateIds = LinkedHashMap<String, String>()
    val newTemplates = mutableListOf<ExerciseTemplate>()
    for (title in imported.flatMap { w -> w.exercises.map { it.title } }.distinct()) {
        templateIds[title] = when (val resolution = requireNotNull(resolutions[title]) { "no resolution for an exercise title" }) {
            is HevyResolution.UseTemplate -> resolution.templateId
            HevyResolution.CreateCustom -> {
                val (type, equipment) = inferCustomExercise(title, setsByTitle.getValue(title).flatten())
                val template = ExerciseTemplate(
                    id = ids.newId(),
                    title = title.trim(),
                    type = type,
                    primaryMuscle = MuscleGroup.OTHER,
                    secondaryMuscles = emptySet(),
                    equipment = equipment,
                    isCustom = true,
                    archived = false,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                )
                newTemplates += template
                template.id
            }
        }
    }

    val newWorkouts = imported.map { hevy ->
        Workout(
            id = ids.newId(),
            title = hevy.title,
            notes = hevy.notes,
            startedAtEpochMs = hevy.startedAtEpochMs,
            utcOffsetS = clock.utcOffsetS(hevy.startedAtEpochMs),
            finishedAtEpochMs = hevy.finishedAtEpochMs,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            deletedAtEpochMs = null,
            exercises = hevy.exercises.map { exercise ->
                WorkoutExercise(
                    id = ids.newId(),
                    exerciseTemplateId = templateIds.getValue(exercise.title),
                    supersetGroup = exercise.supersetGroup,
                    notes = exercise.notes,
                    restSeconds = null,
                    sets = exercise.sets.map { WorkoutSet(id = ids.newId(), data = it.data, completedAtEpochMs = null) },
                )
            },
        )
    }

    val importedSets = imported.flatMap { w -> w.exercises.flatMap { it.sets } }
    return HevyImportPlan(
        newWorkouts = newWorkouts,
        newTemplates = newTemplates,
        workoutsInFile = workouts.size,
        duplicatesSkipped = workouts.size - imported.size,
        setCount = importedSets.size,
        rpeDropped = importedSets.count { it.rpe != null },
        firstStartEpochMs = imported.minOfOrNull { it.startedAtEpochMs },
        lastStartEpochMs = imported.maxOfOrNull { it.startedAtEpochMs },
    )
}
