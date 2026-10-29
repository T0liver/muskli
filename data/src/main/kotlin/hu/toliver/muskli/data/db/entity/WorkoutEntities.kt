package hu.toliver.muskli.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side

@Entity(tableName = "workout", indices = [Index("start_time")])
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String?,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "utc_offset_s") val utcOffsetS: Int,
    @ColumnInfo(name = "end_time") val endTime: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(
    tableName = "workout_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        // History must keep its exercises; a referenced template is archived, never deleted (D-021).
        ForeignKey(
            entity = ExerciseTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_template_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workout_id"), Index("exercise_template_id")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "workout_id") val workoutId: String,
    val position: Int,
    @ColumnInfo(name = "exercise_template_id") val exerciseTemplateId: String,
    @ColumnInfo(name = "superset_group") val supersetGroup: Int?,
    val notes: String?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int?,
)

@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workout_exercise_id")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "workout_exercise_id") val workoutExerciseId: String,
    val position: Int,
    @ColumnInfo(name = "set_type", defaultValue = "'normal'") val setType: SetType,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    val reps: Int?,
    @ColumnInfo(name = "distance_m") val distanceM: Double?,
    @ColumnInfo(name = "duration_s") val durationS: Int?,
    @ColumnInfo(defaultValue = "0") val completed: Boolean,
    @ColumnInfo(name = "completed_at") val completedAt: Long?,
    @ColumnInfo(defaultValue = "'both'") val side: Side,
)

data class WorkoutExerciseWithSets(
    @Embedded val exercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workout_exercise_id")
    val sets: List<WorkoutSetEntity>,
)

data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(entity = WorkoutExerciseEntity::class, parentColumn = "id", entityColumn = "workout_id")
    val exercises: List<WorkoutExerciseWithSets>,
)
