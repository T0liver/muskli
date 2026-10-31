package hu.toliver.muskli.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import hu.toliver.muskli.data.db.entity.ExerciseTemplateEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateSecondaryMuscleEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateWithMuscles
import hu.toliver.muskli.data.db.entity.RoutineEntity
import hu.toliver.muskli.data.db.entity.RoutineExerciseEntity
import hu.toliver.muskli.data.db.entity.RoutineSetEntity
import hu.toliver.muskli.data.db.entity.RoutineWithChildren
import hu.toliver.muskli.data.db.entity.WorkoutEntity
import hu.toliver.muskli.data.db.entity.WorkoutExerciseEntity
import hu.toliver.muskli.data.db.entity.WorkoutSetEntity
import hu.toliver.muskli.data.db.entity.WorkoutWithExercises

/** Whole-table reads and writes for export/import (T-018); callers wrap writes in one transaction. */
@Dao
abstract class BackupDao {
    @Transaction
    @Query("SELECT * FROM workout ORDER BY start_time, id")
    abstract suspend fun allWorkouts(): List<WorkoutWithExercises>

    @Transaction
    @Query("SELECT * FROM routine ORDER BY id")
    abstract suspend fun allRoutines(): List<RoutineWithChildren>

    /** Custom exercises plus the catalogue exercises any workout or routine uses. */
    @Transaction
    @Query(
        """
        SELECT * FROM exercise_template
        WHERE is_custom = 1
           OR id IN (SELECT exercise_template_id FROM workout_exercise)
           OR id IN (SELECT exercise_template_id FROM routine_exercise)
        ORDER BY id
        """,
    )
    abstract suspend fun exportedTemplates(): List<ExerciseTemplateWithMuscles>

    /** Every workout row, drafts and soft-deleted included (Hevy duplicate check, D-044). */
    @Query("SELECT start_time FROM workout")
    abstract suspend fun allWorkoutStartTimes(): List<Long>

    @Query("SELECT id FROM exercise_template WHERE is_custom = 0")
    abstract suspend fun catalogueTemplateIds(): List<String>

    /** Exercises and sets go with them through the foreign-key cascades. */
    @Query("DELETE FROM workout")
    abstract suspend fun deleteAllWorkouts()

    /** Exercises and sets go with them through the foreign-key cascades. */
    @Query("DELETE FROM routine")
    abstract suspend fun deleteAllRoutines()

    @Query("DELETE FROM exercise_template WHERE is_custom = 1")
    abstract suspend fun deleteCustomTemplates()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTemplatesIfMissing(templates: List<ExerciseTemplateEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSecondaryMusclesIfMissing(muscles: List<ExerciseTemplateSecondaryMuscleEntity>)

    @Insert
    abstract suspend fun insertWorkouts(workouts: List<WorkoutEntity>)

    @Insert
    abstract suspend fun insertExercises(exercises: List<WorkoutExerciseEntity>)

    @Insert
    abstract suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Insert
    abstract suspend fun insertRoutines(routines: List<RoutineEntity>)

    @Insert
    abstract suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Insert
    abstract suspend fun insertRoutineSets(sets: List<RoutineSetEntity>)
}
