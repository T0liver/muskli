package hu.toliver.muskli.ui.library

import androidx.annotation.StringRes
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup

@StringRes
fun MuscleGroup.labelRes(): Int = when (this) {
    MuscleGroup.ABDOMINALS -> R.string.muscle_abdominals
    MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
    MuscleGroup.BICEPS -> R.string.muscle_biceps
    MuscleGroup.TRICEPS -> R.string.muscle_triceps
    MuscleGroup.FOREARMS -> R.string.muscle_forearms
    MuscleGroup.QUADRICEPS -> R.string.muscle_quadriceps
    MuscleGroup.HAMSTRINGS -> R.string.muscle_hamstrings
    MuscleGroup.CALVES -> R.string.muscle_calves
    MuscleGroup.GLUTES -> R.string.muscle_glutes
    MuscleGroup.ABDUCTORS -> R.string.muscle_abductors
    MuscleGroup.ADDUCTORS -> R.string.muscle_adductors
    MuscleGroup.LATS -> R.string.muscle_lats
    MuscleGroup.UPPER_BACK -> R.string.muscle_upper_back
    MuscleGroup.TRAPS -> R.string.muscle_traps
    MuscleGroup.LOWER_BACK -> R.string.muscle_lower_back
    MuscleGroup.CHEST -> R.string.muscle_chest
    MuscleGroup.CARDIO -> R.string.muscle_cardio
    MuscleGroup.NECK -> R.string.muscle_neck
    MuscleGroup.FULL_BODY -> R.string.muscle_full_body
    MuscleGroup.OTHER -> R.string.muscle_other
}

@StringRes
fun Equipment.labelRes(): Int = when (this) {
    Equipment.NONE -> R.string.equipment_none
    Equipment.BARBELL -> R.string.equipment_barbell
    Equipment.DUMBBELL -> R.string.equipment_dumbbell
    Equipment.KETTLEBELL -> R.string.equipment_kettlebell
    Equipment.MACHINE -> R.string.equipment_machine
    Equipment.PLATE -> R.string.equipment_plate
    Equipment.RESISTANCE_BAND -> R.string.equipment_resistance_band
    Equipment.SUSPENSION -> R.string.equipment_suspension
    Equipment.OTHER -> R.string.equipment_other
}

@StringRes
fun ExerciseType.labelRes(): Int = when (this) {
    ExerciseType.WEIGHT_REPS -> R.string.type_weight_reps
    ExerciseType.REPS_ONLY -> R.string.type_reps_only
    ExerciseType.BODYWEIGHT_REPS -> R.string.type_bodyweight_reps
    ExerciseType.BODYWEIGHT_ASSISTED_REPS -> R.string.type_bodyweight_assisted_reps
    ExerciseType.DURATION -> R.string.type_duration
    ExerciseType.WEIGHT_DURATION -> R.string.type_weight_duration
    ExerciseType.DISTANCE_DURATION -> R.string.type_distance_duration
    ExerciseType.SHORT_DISTANCE_WEIGHT -> R.string.type_short_distance_weight
}
