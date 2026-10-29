package hu.toliver.muskli.domain.repo

import hu.toliver.muskli.domain.calc.WorkoutExerciseRecord
import hu.toliver.muskli.domain.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observe(id: String): Flow<Workout?>

    /** The unfinished, non-deleted workout, if any. */
    fun observeActiveDraft(): Flow<Workout?>

    /** Finished, non-deleted workouts, newest start first. */
    fun observeHistory(): Flow<List<Workout>>

    /** Writes the workout with all its exercises and sets, replacing what was stored for it. */
    suspend fun save(workout: Workout)

    suspend fun softDelete(id: String, deletedAtEpochMs: Long)

    suspend fun restore(id: String)

    /** Occurrences of the exercise in finished, non-deleted workouts, input for `previousFor`. */
    suspend fun exerciseHistory(exerciseTemplateId: String): List<WorkoutExerciseRecord>
}
