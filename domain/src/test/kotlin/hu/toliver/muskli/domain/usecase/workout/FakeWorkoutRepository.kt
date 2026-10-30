package hu.toliver.muskli.domain.usecase.workout

import hu.toliver.muskli.domain.calc.WorkoutExerciseRecord
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeWorkoutRepository : WorkoutRepository {
    val workouts = MutableStateFlow<Map<String, Workout>>(emptyMap())
    var writes = 0
        private set

    private fun activeDraft() = workouts.value.values.firstOrNull { it.finishedAtEpochMs == null && it.deletedAtEpochMs == null }

    override fun observe(id: String): Flow<Workout?> = workouts.map { it[id] }

    override fun observeActiveDraft(): Flow<Workout?> = workouts.map { activeDraft() }

    override fun observeHistory(): Flow<List<Workout>> = error("not used")

    override suspend fun save(workout: Workout) {
        writes++
        workouts.value = workouts.value + (workout.id to workout)
    }

    override suspend fun softDelete(id: String, deletedAtEpochMs: Long) = error("not used")

    override suspend fun restore(id: String) = error("not used")

    /** Per template id, what [exerciseHistory] returns. */
    var history: Map<String, List<WorkoutExerciseRecord>> = emptyMap()

    override suspend fun exerciseHistory(exerciseTemplateId: String): List<WorkoutExerciseRecord> =
        history[exerciseTemplateId].orEmpty()

    override suspend fun startDraftUnlessActive(draft: Workout): Workout = activeDraft() ?: draft.also { save(it) }

    override suspend fun update(id: String, transform: (Workout) -> Workout): Workout? {
        val current = workouts.value[id] ?: return null
        val updated = transform(current)
        if (updated != current) save(updated)
        return updated
    }
}
