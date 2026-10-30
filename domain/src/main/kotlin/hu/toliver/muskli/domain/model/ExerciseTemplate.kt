package hu.toliver.muskli.domain.model

enum class MuscleGroup {
    ABDOMINALS,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    FOREARMS,
    QUADRICEPS,
    HAMSTRINGS,
    CALVES,
    GLUTES,
    ABDUCTORS,
    ADDUCTORS,
    LATS,
    UPPER_BACK,
    TRAPS,
    LOWER_BACK,
    CHEST,
    CARDIO,
    NECK,
    FULL_BODY,
    OTHER,
}

enum class Equipment { NONE, BARBELL, DUMBBELL, KETTLEBELL, MACHINE, PLATE, RESISTANCE_BAND, SUSPENSION, OTHER }

/** A library entry. Catalogue ids are slugs (D-025); custom ids come from [hu.toliver.muskli.domain.repo.IdGenerator]. */
data class ExerciseTemplate(
    val id: String,
    val title: String,
    val type: ExerciseType,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: Set<MuscleGroup>,
    val equipment: Equipment,
    val isCustom: Boolean,
    val archived: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    /** Trained one side at a time, so volume counts a `both` set twice (D-014, D-026, D-037). */
    val unilateral: Boolean = false,
)
