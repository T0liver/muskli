package hu.toliver.muskli.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup

@Entity(tableName = "exercise_template")
data class ExerciseTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "exercise_type") val exerciseType: ExerciseType,
    @ColumnInfo(name = "primary_muscle") val primaryMuscle: MuscleGroup,
    val equipment: Equipment,
    @ColumnInfo(name = "is_custom", defaultValue = "0") val isCustom: Boolean,
    @ColumnInfo(defaultValue = "0") val archived: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "exercise_template_secondary_muscle",
    primaryKeys = ["exercise_template_id", "muscle"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExerciseTemplateSecondaryMuscleEntity(
    @ColumnInfo(name = "exercise_template_id") val exerciseTemplateId: String,
    val muscle: MuscleGroup,
)

data class ExerciseTemplateWithMuscles(
    @Embedded val template: ExerciseTemplateEntity,
    @Relation(parentColumn = "id", entityColumn = "exercise_template_id")
    val secondaryMuscles: List<ExerciseTemplateSecondaryMuscleEntity>,
)
