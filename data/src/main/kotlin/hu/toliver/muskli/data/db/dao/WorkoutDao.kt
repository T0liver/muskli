package hu.toliver.muskli.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import hu.toliver.muskli.data.db.entity.WorkoutEntity
import hu.toliver.muskli.data.db.entity.WorkoutExerciseEntity
import hu.toliver.muskli.data.db.entity.WorkoutSetEntity
import hu.toliver.muskli.data.db.entity.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
abstract class WorkoutDao {
    @Transaction
    @Query("SELECT * FROM workout WHERE id = :id")
    abstract fun observe(id: String): Flow<WorkoutWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout WHERE end_time IS NULL AND deleted_at IS NULL ORDER BY start_time DESC LIMIT 1")
    abstract fun observeActiveDraft(): Flow<WorkoutWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout WHERE end_time IS NOT NULL AND deleted_at IS NULL ORDER BY start_time DESC")
    abstract fun observeHistory(): Flow<List<WorkoutWithExercises>>

    @Transaction
    @Query(
        """
        SELECT * FROM workout
        WHERE end_time IS NOT NULL AND deleted_at IS NULL
          AND id IN (SELECT workout_id FROM workout_exercise WHERE exercise_template_id = :exerciseTemplateId)
        """,
    )
    abstract suspend fun finishedWorkoutsWithExercise(exerciseTemplateId: String): List<WorkoutWithExercises>

    /** Replaces the workout's exercises and sets; deleting the exercises cascades to their sets. */
    @Transaction
    open suspend fun save(
        workout: WorkoutEntity,
        exercises: List<WorkoutExerciseEntity>,
        sets: List<WorkoutSetEntity>,
    ) {
        upsertWorkout(workout)
        deleteExercises(workout.id)
        insertExercises(exercises)
        insertSets(sets)
    }

    @Query("UPDATE workout SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    abstract suspend fun softDelete(id: String, deletedAt: Long)

    @Query("UPDATE workout SET deleted_at = NULL WHERE id = :id")
    abstract suspend fun restore(id: String)

    @Query("DELETE FROM workout WHERE id = :id")
    abstract suspend fun delete(id: String)

    @Upsert
    protected abstract suspend fun upsertWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workout_exercise WHERE workout_id = :workoutId")
    protected abstract suspend fun deleteExercises(workoutId: String)

    @Insert
    protected abstract suspend fun insertExercises(exercises: List<WorkoutExerciseEntity>)

    @Insert
    protected abstract suspend fun insertSets(sets: List<WorkoutSetEntity>)
}
