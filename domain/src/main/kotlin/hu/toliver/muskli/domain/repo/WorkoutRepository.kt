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

    /** In one transaction: the active draft if there is one, otherwise [draft] saved. Keeps drafts to at most one. */
    suspend fun startDraftUnlessActive(draft: Workout): Workout

    /**
     * Reads, transforms, and writes the workout in one transaction, so concurrent edits cannot
     * overwrite each other. Nothing is written when [transform] returns an equal workout.
     * Returns the result, or null when [id] does not exist.
     */
    suspend fun update(id: String, transform: (Workout) -> Workout): Workout?

    suspend fun softDelete(id: String, deletedAtEpochMs: Long)

    suspend fun restore(id: String)

    /** Occurrences of the exercise in finished, non-deleted workouts, input for `previousFor`. */
    suspend fun exerciseHistory(exerciseTemplateId: String): List<WorkoutExerciseRecord>
}
