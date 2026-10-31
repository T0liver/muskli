package hu.toliver.muskli.domain.importer.hevy

import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.model.MuscleGroup.ABDOMINALS
import hu.toliver.muskli.domain.model.MuscleGroup.BICEPS
import hu.toliver.muskli.domain.model.MuscleGroup.CARDIO
import hu.toliver.muskli.domain.model.MuscleGroup.CHEST
import hu.toliver.muskli.domain.model.MuscleGroup.FOREARMS
import hu.toliver.muskli.domain.model.MuscleGroup.GLUTES
import hu.toliver.muskli.domain.model.MuscleGroup.HAMSTRINGS
import hu.toliver.muskli.domain.model.MuscleGroup.LATS
import hu.toliver.muskli.domain.model.MuscleGroup.LOWER_BACK
import hu.toliver.muskli.domain.model.MuscleGroup.QUADRICEPS
import hu.toliver.muskli.domain.model.MuscleGroup.SHOULDERS
import hu.toliver.muskli.domain.model.MuscleGroup.TRAPS
import hu.toliver.muskli.domain.model.MuscleGroup.TRICEPS
import hu.toliver.muskli.domain.model.MuscleGroup.UPPER_BACK

internal fun template(
    id: String,
    title: String,
    type: ExerciseType = ExerciseType.WEIGHT_REPS,
    primary: MuscleGroup = MuscleGroup.OTHER,
    secondary: Set<MuscleGroup> = emptySet(),
    equipment: Equipment = Equipment.NONE,
    isCustom: Boolean = false,
    archived: Boolean = false,
) = ExerciseTemplate(id, title, type, primary, secondary, equipment, isCustom, archived, 0L, 0L)

/** The 10 exercises of catalogue version 1, copied so that catalogue growth cannot break the Hevy tests. */
internal val catalogueV1 = listOf(
    template("barbell-back-squat", "Barbell Back Squat", ExerciseType.WEIGHT_REPS, QUADRICEPS, setOf(GLUTES, HAMSTRINGS), Equipment.BARBELL),
    template("barbell-bench-press", "Barbell Bench Press", ExerciseType.WEIGHT_REPS, CHEST, setOf(SHOULDERS, TRICEPS), Equipment.BARBELL),
    template("barbell-deadlift", "Barbell Deadlift", ExerciseType.WEIGHT_REPS, HAMSTRINGS, setOf(GLUTES, LOWER_BACK, FOREARMS), Equipment.BARBELL),
    template("dumbbell-farmers-walk", "Dumbbell Farmer's Walk", ExerciseType.SHORT_DISTANCE_WEIGHT, FOREARMS, setOf(TRAPS, ABDOMINALS), Equipment.DUMBBELL),
    template("hanging-knee-raise", "Hanging Knee Raise", ExerciseType.REPS_ONLY, ABDOMINALS),
    template("machine-assisted-pull-up", "Machine Assisted Pull-Up", ExerciseType.BODYWEIGHT_ASSISTED_REPS, LATS, setOf(BICEPS), Equipment.MACHINE),
    template("plank", "Plank", ExerciseType.DURATION, ABDOMINALS),
    template("pull-up", "Pull-Up", ExerciseType.BODYWEIGHT_REPS, LATS, setOf(BICEPS, UPPER_BACK)),
    template("rowing-machine", "Rowing Machine", ExerciseType.DISTANCE_DURATION, CARDIO, equipment = Equipment.MACHINE),
    template("weighted-plank", "Weighted Plank", ExerciseType.WEIGHT_DURATION, ABDOMINALS, equipment = Equipment.PLATE),
)
