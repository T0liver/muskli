package hu.toliver.muskli.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import hu.toliver.muskli.domain.model.SetType

@Entity(tableName = "routine")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

// A routine is replaced as a whole, so its children are keyed by position instead of ids (D-052).
@Entity(
    tableName = "routine_exercise",
    primaryKeys = ["routine_id", "position"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        // Like workouts, a routine keeps its exercises; referenced templates are archived, not deleted (D-021).
        ForeignKey(
            entity = ExerciseTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_template_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("exercise_template_id")],
)
data class RoutineExerciseEntity(
    @ColumnInfo(name = "routine_id") val routineId: String,
    val position: Int,
    @ColumnInfo(name = "exercise_template_id") val exerciseTemplateId: String,
    @ColumnInfo(name = "superset_group") val supersetGroup: Int?,
    val notes: String?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int?,
)

@Entity(
    tableName = "routine_set",
    primaryKeys = ["routine_id", "exercise_position", "position"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["routine_id", "position"],
            childColumns = ["routine_id", "exercise_position"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RoutineSetEntity(
    @ColumnInfo(name = "routine_id") val routineId: String,
    @ColumnInfo(name = "exercise_position") val exercisePosition: Int,
    val position: Int,
    @ColumnInfo(name = "set_type", defaultValue = "'normal'") val setType: SetType,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    val reps: Int?,
    @ColumnInfo(name = "distance_m") val distanceM: Double?,
    @ColumnInfo(name = "duration_s") val durationS: Int?,
)

data class RoutineWithChildren(
    @Embedded val routine: RoutineEntity,
    @Relation(parentColumn = "id", entityColumn = "routine_id")
    val exercises: List<RoutineExerciseEntity>,
    @Relation(parentColumn = "id", entityColumn = "routine_id")
    val sets: List<RoutineSetEntity>,
)
