package hu.toliver.muskli.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import hu.toliver.muskli.data.db.entity.ExerciseTemplateEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateSecondaryMuscleEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateWithMuscles
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ExerciseTemplateDao {
    @Transaction
    @Query("SELECT * FROM exercise_template ORDER BY title")
    abstract fun observeAll(): Flow<List<ExerciseTemplateWithMuscles>>

    @Transaction
    @Query("SELECT * FROM exercise_template")
    abstract suspend fun getAll(): List<ExerciseTemplateWithMuscles>

    @Transaction
    @Query("SELECT * FROM exercise_template WHERE id = :id")
    abstract suspend fun get(id: String): ExerciseTemplateWithMuscles?

    @Transaction
    @Query(
        """
        SELECT t.* FROM exercise_template t
        JOIN (
            SELECT we.exercise_template_id AS template_id, MAX(w.start_time) AS last_used
            FROM workout_exercise we JOIN workout w ON w.id = we.workout_id
            WHERE w.deleted_at IS NULL
            GROUP BY we.exercise_template_id
        ) recent ON recent.template_id = t.id
        WHERE t.archived = 0
        ORDER BY recent.last_used DESC, t.title
        LIMIT :limit
        """,
    )
    abstract fun observeRecentlyUsed(limit: Int): Flow<List<ExerciseTemplateWithMuscles>>

    /** Templates any workout points at, soft-deleted workouts included, since those can be restored. */
    @Query("SELECT DISTINCT exercise_template_id FROM workout_exercise")
    abstract suspend fun referencedTemplateIds(): List<String>

    @Query("UPDATE exercise_template SET archived = 1, updated_at = :updatedAt WHERE id IN (:ids)")
    abstract suspend fun archive(ids: List<String>, updatedAt: Long)

    @Transaction
    open suspend fun upsert(
        template: ExerciseTemplateEntity,
        secondaryMuscles: List<ExerciseTemplateSecondaryMuscleEntity>,
    ) {
        upsertTemplate(template)
        deleteSecondaryMuscles(template.id)
        insertSecondaryMuscles(secondaryMuscles)
    }

    @Upsert
    protected abstract suspend fun upsertTemplate(template: ExerciseTemplateEntity)

    @Query("DELETE FROM exercise_template_secondary_muscle WHERE exercise_template_id = :templateId")
    protected abstract suspend fun deleteSecondaryMuscles(templateId: String)

    @Insert
    protected abstract suspend fun insertSecondaryMuscles(muscles: List<ExerciseTemplateSecondaryMuscleEntity>)
}
