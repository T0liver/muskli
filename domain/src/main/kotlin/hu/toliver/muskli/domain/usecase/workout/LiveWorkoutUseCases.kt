package hu.toliver.muskli.domain.usecase.workout

import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.calc.RestEffect
import hu.toliver.muskli.domain.calc.previousSets
import hu.toliver.muskli.domain.calc.remainingMs
import hu.toliver.muskli.domain.calc.restTimerEffect
import hu.toliver.muskli.domain.model.RestTimer
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.repo.WorkoutRepository

// Live-workout operations over the persisted draft (ARCHITECTURE §4). Each one is a single
// `WorkoutRepository.update` transaction, so every change is stored before it returns. Mutations
// return the resulting workout, or null when the workout does not exist; an unknown exercise or
// set id leaves the workout unchanged and writes nothing.

sealed interface StartWorkoutResult {
    val workout: Workout

    data class Started(override val workout: Workout) : StartWorkoutResult

    /** A draft already existed; it is returned instead of creating a second one. */
    data class Resumed(override val workout: Workout) : StartWorkoutResult
}

/** The value fields of a set, all replaced together by [EditSetValuesUseCase]. Weight in kg. */
data class SetValues(
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationS: Int? = null,
    val distanceM: Double? = null,
)

/** Starts an empty draft. The title stays empty until the workout is finished (T-016). */
class StartWorkoutUseCase(
    private val repository: WorkoutRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(): StartWorkoutResult {
        val now = clock.nowEpochMs()
        val draft = Workout(
            id = ids.newId(),
            title = "",
            notes = null,
            startedAtEpochMs = now,
            utcOffsetS = clock.utcOffsetS(now),
            finishedAtEpochMs = null,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            deletedAtEpochMs = null,
            exercises = emptyList(),
        )
        val active = repository.startDraftUnlessActive(draft)
        return if (active.id == draft.id) StartWorkoutResult.Started(active) else StartWorkoutResult.Resumed(active)
    }
}

/**
 * Appends the templates in the given order. An exercise done before starts with last time's sets
 * (count, types, values), uncompleted and aligned like the PREVIOUS column (D-019); a slot
 * without a previous value becomes an empty normal set. Otherwise one empty normal set (D-029).
 */
class AddExercisesUseCase(
    private val repository: WorkoutRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, templateIds: List<String>): Workout? {
        val added = templateIds.map { templateId ->
            val prefill = repository.lastTime(templateId).map { it?.copy(completed = false) ?: SetData() }
            newExercise(templateId, ids, prefill)
        }
        return repository.edit(workoutId, clock) { it.copy(exercises = it.exercises + added) }
    }
}

/** The PREVIOUS column of one exercise: last time's sets by raw index, null where there is none (D-019). */
class PreviousSetsUseCase(private val repository: WorkoutRepository) {
    suspend operator fun invoke(exerciseTemplateId: String): List<SetData?> = repository.lastTime(exerciseTemplateId)
}

// "Anywhere" only: routine-scoped previous values need routines (T-024).
private suspend fun WorkoutRepository.lastTime(templateId: String): List<SetData?> =
    previousSets(exerciseHistory(templateId), PreviousMode.ANYWHERE, routineId = null)

/**
 * Swaps the template at the same position. Old sets are dropped (their values may not fit the new
 * exercise type) for one empty normal set; notes and rest go back to defaults; the superset stays.
 */
class ReplaceExerciseUseCase(
    private val repository: WorkoutRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, workoutExerciseId: String, newTemplateId: String): Workout? {
        val replacement = newExercise(newTemplateId, ids)
        return repository.edit(workoutId, clock) { workout ->
            workout.copy(
                exercises = workout.exercises.map {
                    if (it.id == workoutExerciseId) replacement.copy(supersetGroup = it.supersetGroup) else it
                },
            )
        }
    }
}

class RemoveExerciseUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, workoutExerciseId: String): Workout? =
        repository.edit(workoutId, clock) { workout ->
            workout.copy(exercises = workout.exercises.filterNot { it.id == workoutExerciseId })
        }
}

/** Moves one exercise, with its sets, to [toIndex] (clamped to the list). */
class ReorderExerciseUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, workoutExerciseId: String, toIndex: Int): Workout? =
        repository.edit(workoutId, clock) { workout ->
            val from = workout.exercises.indexOfFirst { it.id == workoutExerciseId }
            if (from < 0) return@edit workout
            val exercises = workout.exercises.toMutableList()
            val moved = exercises.removeAt(from)
            exercises.add(toIndex.coerceIn(0, exercises.size), moved)
            workout.copy(exercises = exercises)
        }
}

/**
 * Appends one normal, uncompleted set that copies the values and side of the exercise's last set,
 * whatever that set's type; an exercise with no sets gets an empty one (D-050).
 */
class AddSetUseCase(
    private val repository: WorkoutRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, workoutExerciseId: String): Workout? {
        val id = ids.newId()
        return repository.edit(workoutId, clock) { workout ->
            workout.mapExercise(workoutExerciseId) { exercise ->
                val above = exercise.sets.lastOrNull()?.data ?: SetData()
                val set = WorkoutSet(id, above.copy(type = SetType.NORMAL, completed = false), completedAtEpochMs = null)
                exercise.copy(sets = exercise.sets + set)
            }
        }
    }
}

class DeleteSetUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, setId: String): Workout? =
        repository.edit(workoutId, clock) { workout ->
            workout.copy(exercises = workout.exercises.map { e -> e.copy(sets = e.sets.filterNot { it.id == setId }) })
        }
}

/** Any type, any number of times, in any sequence (consecutive drop sets included); values are kept. */
class ChangeSetTypeUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, setId: String, type: SetType): Workout? =
        repository.edit(workoutId, clock) { workout ->
            workout.mapSet(setId) { it.copy(data = it.data.copy(type = type)) }
        }
}

/** Replaces the value fields; type, completion, and side are kept. */
class EditSetValuesUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, setId: String, values: SetValues): Workout? =
        repository.edit(workoutId, clock) { workout ->
            workout.mapSet(setId) {
                it.copy(
                    data = it.data.copy(
                        weightKg = values.weightKg,
                        reps = values.reps,
                        durationS = values.durationS,
                        distanceM = values.distanceM,
                    ),
                )
            }
        }
}

/**
 * Marks the set completed now and returns what the rest timer should do (T-004). The next set is
 * the following set in plain workout order: the rest of this exercise, then the next exercise's
 * first set; superset round order waits for the supersets UI. [defaultRestS] is the settings
 * default. An already completed or unknown set changes nothing and starts no timer.
 *
 * The effect is stored in the same transaction: Start replaces the running rest, None cancels it,
 * so nothing fires during a drop set (D-032).
 */
class CompleteSetUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, setId: String, defaultRestS: Int?): RestEffect {
        var effect: RestEffect = RestEffect.None
        repository.edit(workoutId, clock) { current ->
            val now = clock.nowEpochMs()
            val inOrder = current.exercises.flatMap { exercise -> exercise.sets.map { exercise to it } }
            val index = inOrder.indexOfFirst { (_, set) -> set.id == setId }
            if (index < 0 || inOrder[index].second.data.completed) return@edit current
            val (exercise, set) = inOrder[index]
            val completed = set.data.copy(completed = true)
            effect = restTimerEffect(completed, inOrder.getOrNull(index + 1)?.second?.data, exercise.restSeconds, defaultRestS)
            val rest = (effect as? RestEffect.Start)?.let { RestTimer(now + it.seconds * 1_000L, it.seconds) }
            current.mapSet(setId) { it.copy(data = completed, completedAtEpochMs = now) }.copy(restTimer = rest)
        }
        return effect
    }
}

/**
 * Moves the end of the running rest by [deltaS] (±15 s from the bar or the notification). When
 * nothing is left after a minus, the rest ends silently, like skip (D-032). No running rest, or
 * one already over, changes nothing.
 */
class AdjustRestTimerUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, deltaS: Int): Workout? =
        repository.edit(workoutId, clock) { workout ->
            val rest = workout.restTimer ?: return@edit workout
            val now = clock.nowEpochMs()
            if (rest.remainingMs(now) == 0L) return@edit workout
            val endsAt = rest.endsAtEpochMs + deltaS * 1_000L
            workout.copy(restTimer = if (endsAt <= now) null else RestTimer(endsAt, rest.durationS + deltaS))
        }
}

class SkipRestTimerUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String): Workout? =
        repository.edit(workoutId, clock) { it.copy(restTimer = null) }
}

/**
 * Called by whichever of the service and the alarm fires first for the rest ending at
 * [endsAtEpochMs]. Clears it and returns true only if that rest is still the stored one, so the
 * other path, a moved end, or a skipped rest does not alert again.
 */
class FinishRestTimerUseCase(private val repository: WorkoutRepository) {
    suspend operator fun invoke(workoutId: String, endsAtEpochMs: Long): Boolean {
        var finished = false
        repository.update(workoutId) { workout ->
            if (workout.restTimer?.endsAtEpochMs != endsAtEpochMs) return@update workout
            finished = true
            workout.copy(restTimer = null)
        }
        return finished
    }
}

class UncompleteSetUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, setId: String): Workout? =
        repository.edit(workoutId, clock) { workout ->
            workout.mapSet(setId) { it.copy(data = it.data.copy(completed = false), completedAtEpochMs = null) }
        }
}

sealed interface ChangeStartTimeResult {
    data class Changed(val workout: Workout) : ChangeStartTimeResult

    /** The picked start is after now; nothing was written. */
    data object InFuture : ChangeStartTimeResult
}

/**
 * Moves the start of the live draft, and so its duration (F-02 AC1). The offset is taken at the
 * new start, like [StartWorkoutUseCase]. Sets and the running rest keep their absolute times
 * (D-048). Returns null when the workout is unknown or no longer a draft.
 */
class ChangeStartTimeUseCase(
    private val repository: WorkoutRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(workoutId: String, startedAtEpochMs: Long): ChangeStartTimeResult? {
        var inFuture = false
        val stored = repository.edit(workoutId, clock) { workout ->
            when {
                !workout.isDraft() -> workout
                startedAtEpochMs > clock.nowEpochMs() -> workout.also { inFuture = true }
                // The picker has minute precision, so confirming it unchanged must not drop the seconds.
                sameMinute(startedAtEpochMs, workout.startedAtEpochMs) -> workout
                else -> workout.copy(startedAtEpochMs = startedAtEpochMs, utcOffsetS = clock.utcOffsetS(startedAtEpochMs))
            }
        } ?: return null
        return when {
            !stored.isDraft() -> null
            inFuture -> ChangeStartTimeResult.InFuture
            else -> ChangeStartTimeResult.Changed(stored)
        }
    }

    private fun Workout.isDraft() = finishedAtEpochMs == null && deletedAtEpochMs == null

    private fun sameMinute(a: Long, b: Long) = Math.floorDiv(a, MINUTE_MS) == Math.floorDiv(b, MINUTE_MS)

    private companion object {
        const val MINUTE_MS = 60_000L
    }
}

/** [WorkoutRepository.update] that also stamps `updatedAt` when [edit] changed anything. */
private suspend fun WorkoutRepository.edit(workoutId: String, clock: Clock, edit: (Workout) -> Workout): Workout? =
    update(workoutId) { current ->
        val edited = edit(current)
        if (edited == current) current else edited.copy(updatedAtEpochMs = clock.nowEpochMs())
    }

private fun newExercise(templateId: String, ids: IdGenerator, prefill: List<SetData> = emptyList()): WorkoutExercise {
    val id = ids.newId()
    return WorkoutExercise(
        id = id,
        exerciseTemplateId = templateId,
        supersetGroup = null,
        notes = null,
        restSeconds = null,
        sets = prefill.ifEmpty { listOf(SetData(type = SetType.NORMAL)) }.map { newSet(ids, it) },
    )
}

private fun newSet(ids: IdGenerator, data: SetData) = WorkoutSet(id = ids.newId(), data = data, completedAtEpochMs = null)

private fun Workout.mapExercise(id: String, transform: (WorkoutExercise) -> WorkoutExercise) =
    copy(exercises = exercises.map { if (it.id == id) transform(it) else it })

private fun Workout.mapSet(id: String, transform: (WorkoutSet) -> WorkoutSet) =
    copy(exercises = exercises.map { e -> e.copy(sets = e.sets.map { if (it.id == id) transform(it) else it }) })
