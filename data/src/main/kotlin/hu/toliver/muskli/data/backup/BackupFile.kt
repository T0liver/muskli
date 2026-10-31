package hu.toliver.muskli.data.backup

import kotlinx.serialization.Serializable

/**
 * The backup file, version [BackupFile.CURRENT_SCHEMA_VERSION]. Nesting and set fields follow Hevy's public API
 * (research/06) in the repo's camelCase spelling, so a Hevy mapper stays shallow. Times are epoch milliseconds
 * plus the workout's UTC offset, which round-trips exactly. Enums use the database's lowercase names.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAtMs: Long? = null,
    val exercises: List<ExerciseJson>,
    val workouts: List<WorkoutJson>,
    /** Absent means "keep this device's settings". */
    val settings: SettingsJson? = null,
    /** Optional so files written before D-052 still import, with no routines. */
    val routines: List<RoutineJson> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class ExerciseJson(
    val id: String,
    val title: String,
    val type: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val isCustom: Boolean,
    val archived: Boolean,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    /** Optional so files written before D-037 still import; absent means bilateral. */
    val unilateral: Boolean = false,
)

@Serializable
data class WorkoutJson(
    val id: String,
    val title: String,
    val description: String? = null,
    val startTimeMs: Long,
    val endTimeMs: Long? = null,
    val utcOffsetS: Int,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val deletedAtMs: Long? = null,
    val restEndsAtMs: Long? = null,
    val restDurationS: Int? = null,
    /** The id of a routine in the same file, or absent. */
    val routineId: String? = null,
    val exercises: List<WorkoutExerciseJson>,
)

@Serializable
data class WorkoutExerciseJson(
    val index: Int,
    val id: String,
    val exerciseTemplateId: String,
    val supersetId: Int? = null,
    val notes: String? = null,
    val restSeconds: Int? = null,
    val sets: List<SetJson>,
)

@Serializable
data class SetJson(
    val index: Int,
    val id: String,
    val type: String,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val distanceMeters: Double? = null,
    val durationSeconds: Int? = null,
    val completed: Boolean,
    val completedAtMs: Long? = null,
    val side: String,
)

/** Exercises and sets have no ids; `index` is their position, like a workout's (D-052). */
@Serializable
data class RoutineJson(
    val id: String,
    val title: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val exercises: List<RoutineExerciseJson>,
)

@Serializable
data class RoutineExerciseJson(
    val index: Int,
    val exerciseTemplateId: String,
    val supersetId: Int? = null,
    val notes: String? = null,
    val restSeconds: Int? = null,
    val sets: List<RoutineSetJson>,
)

@Serializable
data class RoutineSetJson(
    val index: Int,
    val type: String,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val distanceMeters: Double? = null,
    val durationSeconds: Int? = null,
)

@Serializable
data class SettingsJson(
    val weightUnit: String,
    /** Null means the default rest timer is off. */
    val defaultRestSeconds: Int? = null,
    val previousMode: String,
    val keepScreenAwake: Boolean,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val weekStart: String,
    val bodyWeightKg: Double? = null,
)
