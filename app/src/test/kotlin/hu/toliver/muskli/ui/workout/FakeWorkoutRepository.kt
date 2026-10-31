package hu.toliver.muskli.ui.workout

import hu.toliver.muskli.domain.calc.WorkoutExerciseRecord
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.ExerciseTemplateRepository
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.repo.SettingsRepository
import hu.toliver.muskli.domain.repo.WorkoutRepository
import hu.toliver.muskli.domain.usecase.workout.AddExercisesUseCase
import hu.toliver.muskli.domain.usecase.workout.AddSetUseCase
import hu.toliver.muskli.domain.usecase.workout.AdjustRestTimerUseCase
import hu.toliver.muskli.domain.usecase.workout.ChangeSetTypeUseCase
import hu.toliver.muskli.domain.usecase.workout.ChangeStartTimeUseCase
import hu.toliver.muskli.domain.usecase.workout.CompleteSetUseCase
import hu.toliver.muskli.domain.usecase.workout.DeleteSetUseCase
import hu.toliver.muskli.domain.usecase.workout.EditSetValuesUseCase
import hu.toliver.muskli.domain.usecase.workout.MarkSetCompletedUseCase
import hu.toliver.muskli.domain.usecase.workout.PreviousSetsBeforeUseCase
import hu.toliver.muskli.domain.usecase.workout.PreviousSetsUseCase
import hu.toliver.muskli.domain.usecase.workout.RemoveExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.ReorderExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.ReplaceExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.SkipRestTimerUseCase
import hu.toliver.muskli.domain.usecase.workout.StartWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.UncompleteSetUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWorkoutRepository(initial: List<Workout> = emptyList()) : WorkoutRepository {
    val workouts = MutableStateFlow(initial.associateBy { it.id })

    /** Per template id, what [exerciseHistory] returns. */
    val history = MutableStateFlow<Map<String, List<WorkoutExerciseRecord>>>(emptyMap())

    /** Each [exerciseHistory] call waits for the next gate in line, if any, so tests can order lookups. */
    val historyGates = ArrayDeque<CompletableDeferred<Unit>>()

    val activeDraft: Workout? get() = workouts.value.values.firstOrNull { it.finishedAtEpochMs == null && it.deletedAtEpochMs == null }

    override fun observe(id: String): Flow<Workout?> = workouts.map { it[id] }

    override fun observeActiveDraft(): Flow<Workout?> = workouts.map { activeDraft }

    override fun observeHistory(): Flow<List<Workout>> = workouts.map { all ->
        all.values.filter { it.finishedAtEpochMs != null && it.deletedAtEpochMs == null }.sortedByDescending { it.startedAtEpochMs }
    }

    override suspend fun save(workout: Workout) {
        workouts.value = workouts.value + (workout.id to workout)
    }

    override suspend fun startDraftUnlessActive(draft: Workout): Workout = activeDraft ?: draft.also { save(it) }

    override suspend fun update(id: String, transform: (Workout) -> Workout): Workout? {
        val current = workouts.value[id] ?: return null
        val updated = transform(current)
        if (updated != current) save(updated)
        return updated
    }

    override suspend fun softDelete(id: String, deletedAtEpochMs: Long) {
        workouts.value[id]?.let { save(it.copy(deletedAtEpochMs = deletedAtEpochMs)) }
    }

    override suspend fun restore(id: String) {
        workouts.value[id]?.let { save(it.copy(deletedAtEpochMs = null)) }
    }

    override suspend fun exerciseHistory(exerciseTemplateId: String): List<WorkoutExerciseRecord> {
        historyGates.removeFirstOrNull()?.await()
        return history.value[exerciseTemplateId].orEmpty()
    }
}

/** One finished past workout in which the exercise had [sets]. */
fun lastTime(vararg sets: SetData, workoutId: String = "past", startedAt: Long = 1_000L) = WorkoutExerciseRecord(
    workoutId = workoutId,
    workoutStartedAtEpochMs = startedAt,
    workoutFinishedAtEpochMs = startedAt + 3_600_000,
    workoutDeletedAtEpochMs = null,
    positionInWorkout = 0,
    sets = sets.toList(),
)

fun done(weightKg: Double, reps: Int, type: SetType = SetType.NORMAL) =
    SetData(type = type, weightKg = weightKg, reps = reps, completed = true)

class TestClock(var now: Long = START_MS) : Clock {
    override fun nowEpochMs() = now

    override fun utcOffsetS(epochMs: Long) = 0

    companion object {
        const val START_MS = 1_000_000L
    }
}

fun workoutSessionViewModel(
    workouts: FakeWorkoutRepository,
    templates: ExerciseTemplateRepository,
    settings: SettingsRepository,
    clock: Clock = TestClock(),
    editWorkoutId: String? = null,
): WorkoutSessionViewModel {
    var nextId = 0
    val ids = IdGenerator { "id${nextId++}" }
    return WorkoutSessionViewModel(
        editWorkoutId = editWorkoutId,
        workouts = workouts,
        templates = templates,
        settingsRepository = settings,
        clock = clock,
        startWorkout = StartWorkoutUseCase(workouts, ids, clock),
        addExercises = AddExercisesUseCase(workouts, ids, clock),
        replaceExercise = ReplaceExerciseUseCase(workouts, ids, clock),
        removeExercise = RemoveExerciseUseCase(workouts, clock),
        reorderExercise = ReorderExerciseUseCase(workouts, clock),
        addSet = AddSetUseCase(workouts, ids, clock),
        deleteSet = DeleteSetUseCase(workouts, clock),
        changeSetType = ChangeSetTypeUseCase(workouts, clock),
        editSetValues = EditSetValuesUseCase(workouts, clock),
        completeSet = CompleteSetUseCase(workouts, clock),
        uncompleteSet = UncompleteSetUseCase(workouts, clock),
        previousSets = PreviousSetsUseCase(workouts),
        adjustRest = AdjustRestTimerUseCase(workouts, clock),
        skipRest = SkipRestTimerUseCase(workouts, clock),
        markSetCompleted = MarkSetCompletedUseCase(workouts, clock),
        previousSetsBefore = PreviousSetsBeforeUseCase(workouts),
        changeStartTime = ChangeStartTimeUseCase(workouts, clock),
    )
}
