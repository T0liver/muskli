package hu.toliver.muskli.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import hu.toliver.muskli.data.db.entity.RoutineEntity
import hu.toliver.muskli.data.db.entity.RoutineExerciseEntity
import hu.toliver.muskli.data.db.entity.RoutineSetEntity
import hu.toliver.muskli.data.db.entity.RoutineWithChildren
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RoutineDao {
    @Transaction
    @Query("SELECT * FROM routine ORDER BY title COLLATE NOCASE, id")
    abstract fun observeAll(): Flow<List<RoutineWithChildren>>

    @Transaction
    @Query("SELECT * FROM routine WHERE id = :id")
    abstract fun observe(id: String): Flow<RoutineWithChildren?>

    @Transaction
    @Query("SELECT * FROM routine WHERE id = :id")
    abstract suspend fun get(id: String): RoutineWithChildren?

    /** Replaces the routine's exercises and sets; deleting the exercises cascades to their sets. */
    @Transaction
    open suspend fun save(
        routine: RoutineEntity,
        exercises: List<RoutineExerciseEntity>,
        sets: List<RoutineSetEntity>,
    ) {
        upsertRoutine(routine)
        deleteExercises(routine.id)
        insertExercises(exercises)
        insertSets(sets)
    }

    /** Workouts have no foreign key to routines, so their reference is cleared here. */
    @Transaction
    open suspend fun delete(id: String) {
        clearWorkoutRoutine(id)
        deleteRoutine(id)
    }

    @Upsert
    protected abstract suspend fun upsertRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routine_exercise WHERE routine_id = :routineId")
    protected abstract suspend fun deleteExercises(routineId: String)

    @Insert
    protected abstract suspend fun insertExercises(exercises: List<RoutineExerciseEntity>)

    @Insert
    protected abstract suspend fun insertSets(sets: List<RoutineSetEntity>)

    @Query("UPDATE workout SET routine_id = NULL WHERE routine_id = :routineId")
    protected abstract suspend fun clearWorkoutRoutine(routineId: String)

    @Query("DELETE FROM routine WHERE id = :id")
    protected abstract suspend fun deleteRoutine(id: String)
}
