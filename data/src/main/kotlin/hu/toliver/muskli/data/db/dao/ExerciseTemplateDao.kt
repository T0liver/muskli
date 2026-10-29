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
    @Query("SELECT * FROM exercise_template WHERE id = :id")
    abstract suspend fun get(id: String): ExerciseTemplateWithMuscles?

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
