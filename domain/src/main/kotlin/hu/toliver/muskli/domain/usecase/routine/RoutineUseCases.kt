package hu.toliver.muskli.domain.usecase.routine

import hu.toliver.muskli.domain.model.PlannedSet
import hu.toliver.muskli.domain.model.Routine
import hu.toliver.muskli.domain.model.RoutineExercise
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.repo.RoutineRepository
import hu.toliver.muskli.domain.repo.WorkoutRepository
import hu.toliver.muskli.domain.usecase.workout.StartWorkoutResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ObserveRoutinesUseCase(private val repository: RoutineRepository) {
    operator fun invoke(): Flow<List<Routine>> = repository.observeAll()
}

class CreateRoutineUseCase(
    private val repository: RoutineRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    /** @throws IllegalArgumentException when the title is blank. */
    suspend operator fun invoke(title: String, exercises: List<RoutineExercise>): Routine {
        val trimmed = routineTitle(title)
        val now = clock.nowEpochMs()
        val routine = Routine(ids.newId(), trimmed, createdAtEpochMs = now, updatedAtEpochMs = now, exercises = exercises)
        repository.save(routine)
        return routine
    }
}

class UpdateRoutineUseCase(
    private val repository: RoutineRepository,
    private val clock: Clock,
) {
    /** Replaces title and exercises. Null when [id] is missing. @throws IllegalArgumentException when the title is blank. */
    suspend operator fun invoke(id: String, title: String, exercises: List<RoutineExercise>): Routine? {
        val trimmed = routineTitle(title)
        val current = repository.get(id) ?: return null
        val updated = current.copy(title = trimmed, exercises = exercises, updatedAtEpochMs = clock.nowEpochMs())
        repository.save(updated)
        return updated
    }
}

/** A hard delete with no undo (D-052); the UI confirms first. */
class DeleteRoutineUseCase(private val repository: RoutineRepository) {
    suspend operator fun invoke(id: String) = repository.delete(id)
}

/**
 * Starts a draft from the routine: its exercises in order with notes, rest, and superset, and the
 * planned sets uncompleted with their type and values; an exercise without sets gets one empty
 * normal set (D-029). The draft takes the routine's title so Finish shows it, and records
 * `routineId`. When a draft already exists it is returned as [StartWorkoutResult.Resumed] and
 * nothing is written, so there is never a second draft (D-052). Null when the routine is missing.
 */
class StartWorkoutFromRoutineUseCase(
    private val routines: RoutineRepository,
    private val workouts: WorkoutRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(routineId: String): StartWorkoutResult? {
        val routine = routines.get(routineId) ?: return null
        val now = clock.nowEpochMs()
        val draft = Workout(
            id = ids.newId(),
            title = routine.title,
            notes = null,
            startedAtEpochMs = now,
            utcOffsetS = clock.utcOffsetS(now),
            finishedAtEpochMs = null,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            deletedAtEpochMs = null,
            exercises = routine.exercises.map { it.toWorkoutExercise() },
            routineId = routine.id,
        )
        val active = workouts.startDraftUnlessActive(draft)
        return if (active.id == draft.id) StartWorkoutResult.Started(active) else StartWorkoutResult.Resumed(active)
    }

    private fun RoutineExercise.toWorkoutExercise(): WorkoutExercise {
        val id = ids.newId()
        val planned = sets.ifEmpty { listOf(PlannedSet()) }
        return WorkoutExercise(
            id = id,
            exerciseTemplateId = exerciseTemplateId,
            supersetGroup = supersetGroup,
            notes = notes,
            restSeconds = restSeconds,
            sets = planned.map { WorkoutSet(ids.newId(), it.toSetData(), completedAtEpochMs = null) },
        )
    }

    private fun PlannedSet.toSetData() =
        SetData(type = type, weightKg = weightKg, reps = reps, durationS = durationS, distanceM = distanceM)
}

/**
 * Saves a finished workout as a new routine: exercises in order with notes, rest, and superset,
 * and every set's type and values. Side and completion are not part of a plan. The workout is
 * not changed. Null when the workout is missing, a draft, or deleted.
 */
class SaveWorkoutAsRoutineUseCase(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    /** @throws IllegalArgumentException when the title is blank. */
    suspend operator fun invoke(workoutId: String, title: String): Routine? {
        val trimmed = routineTitle(title)
        val workout = workouts.observe(workoutId).first()
            ?.takeIf { it.finishedAtEpochMs != null && it.deletedAtEpochMs == null }
            ?: return null
        val now = clock.nowEpochMs()
        val routine = Routine(
            id = ids.newId(),
            title = trimmed,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            exercises = workout.exercises.map { exercise ->
                RoutineExercise(
                    exerciseTemplateId = exercise.exerciseTemplateId,
                    supersetGroup = exercise.supersetGroup,
                    notes = exercise.notes,
                    restSeconds = exercise.restSeconds,
                    sets = exercise.sets.map { it.data.toPlannedSet() },
                )
            },
        )
        routines.save(routine)
        return routine
    }

    private fun SetData.toPlannedSet() =
        PlannedSet(type = type, weightKg = weightKg, reps = reps, durationS = durationS, distanceM = distanceM)
}

private fun routineTitle(title: String): String =
    title.trim().also { require(it.isNotEmpty()) { "Routine title must not be blank" } }
