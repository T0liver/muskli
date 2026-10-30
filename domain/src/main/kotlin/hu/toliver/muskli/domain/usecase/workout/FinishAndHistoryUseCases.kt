package hu.toliver.muskli.domain.usecase.workout

import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.calc.previousSets
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.repo.WorkoutRepository

/** What the finish screen sets; it also saves an edited past workout. [durationS] is from the start. */
data class WorkoutDetails(
    val title: String,
    val notes: String?,
    val startedAtEpochMs: Long,
    val durationS: Long,
)

sealed interface FinishWorkoutResult {
    data class Finished(val workout: Workout) : FinishWorkoutResult

    /** No set is completed: nothing was written; the screen offers to discard the workout. */
    data object NothingCompleted : FinishWorkoutResult
}

/** Sets that finishing would discard, so the screen knows whether to ask first (D-011). */
fun Workout.uncompletedSetCount(): Int = exercises.sumOf { exercise -> exercise.sets.count { !it.data.completed } }

/** Without one, finishing offers to discard the workout, and a new past workout left this way is dropped. */
fun Workout.hasCompletedSet(): Boolean = exercises.any { exercise -> exercise.sets.any { it.data.completed } }

/**
 * Finishes the live draft, or saves an edited past workout, with [details]. Uncompleted sets are
 * discarded (D-011, the screen confirms first) and exercises left without sets are removed. The
 * offset is taken at the new start, since a back-dated start may fall in another DST period.
 * Returns null when the workout does not exist.
 */
class FinishWorkoutUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, details: WorkoutDetails): FinishWorkoutResult? {
        var result: FinishWorkoutResult = FinishWorkoutResult.NothingCompleted
        repository.update(workoutId) { current ->
            if (!current.hasCompletedSet()) return@update current
            val exercises = current.exercises
                .map { exercise -> exercise.copy(sets = exercise.sets.filter { it.data.completed }) }
                .filter { it.sets.isNotEmpty() }
            val start = details.startedAtEpochMs
            current.copy(
                title = details.title.trim(),
                notes = details.notes?.trim()?.ifEmpty { null },
                startedAtEpochMs = start,
                utcOffsetS = clock.utcOffsetS(start),
                finishedAtEpochMs = start + details.durationS.coerceAtLeast(0) * 1_000L,
                updatedAtEpochMs = clock.nowEpochMs(),
                exercises = exercises,
                restTimer = null,
            ).also { result = FinishWorkoutResult.Finished(it) }
        } ?: return null
        return result
    }
}

/** Soft delete (undo through [RestoreWorkoutUseCase]); also how the live draft is discarded. */
class DeleteWorkoutUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String) = repository.softDelete(workoutId, clock.nowEpochMs())
}

class RestoreWorkoutUseCase(private val repository: WorkoutRepository) {
    suspend operator fun invoke(workoutId: String) = repository.restore(workoutId)
}

/**
 * Logs a past workout: an empty finished workout at [startedAtEpochMs], edited like any past
 * workout afterwards. Finished from the start so it never becomes the live draft.
 */
class StartPastWorkoutUseCase(
    private val repository: WorkoutRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(startedAtEpochMs: Long): Workout {
        val now = clock.nowEpochMs()
        val workout = Workout(
            id = ids.newId(),
            title = "",
            notes = null,
            startedAtEpochMs = startedAtEpochMs,
            utcOffsetS = clock.utcOffsetS(startedAtEpochMs),
            finishedAtEpochMs = startedAtEpochMs + DEFAULT_PAST_DURATION_MS,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            deletedAtEpochMs = null,
            exercises = emptyList(),
        )
        repository.save(workout)
        return workout
    }

    private companion object {
        const val DEFAULT_PAST_DURATION_MS = 3_600_000L
    }
}

/**
 * The PREVIOUS column while editing [workout]: the last session started before it, so the
 * workout never shows itself or a later session as "last time".
 */
class PreviousSetsBeforeUseCase(private val repository: WorkoutRepository) {
    suspend operator fun invoke(exerciseTemplateId: String, workout: Workout): List<SetData?> {
        val earlier = repository.exerciseHistory(exerciseTemplateId)
            .filter { it.workoutId != workout.id && it.workoutStartedAtEpochMs < workout.startedAtEpochMs }
        return previousSets(earlier, PreviousMode.ANYWHERE, routineId = null)
    }
}

/** Ticks a set of a past workout being edited. Unlike [CompleteSetUseCase] no rest timer starts. */
class MarkSetCompletedUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, setId: String): Workout? = repository.update(workoutId) { workout ->
        val now = clock.nowEpochMs()
        val edited = workout.copy(
            exercises = workout.exercises.map { e ->
                e.copy(sets = e.sets.map { if (it.id == setId && !it.data.completed) it.copy(data = it.data.copy(completed = true), completedAtEpochMs = now) else it })
            },
        )
        if (edited == workout) workout else edited.copy(updatedAtEpochMs = now)
    }
}
