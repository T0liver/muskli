package hu.toliver.muskli.data.db

import androidx.room.TypeConverter
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side

/**
 * Enums are stored as lowercase names (`weight_reps`, `upper_back`), the same spelling as the
 * catalogue and export. Room cannot declare CHECK constraints, so an unknown value fails here.
 */
class Converters {
    @TypeConverter fun fromExerciseType(value: ExerciseType): String = value.toDb()

    @TypeConverter fun toExerciseType(value: String): ExerciseType = value.toEnum()

    @TypeConverter fun fromMuscleGroup(value: MuscleGroup): String = value.toDb()

    @TypeConverter fun toMuscleGroup(value: String): MuscleGroup = value.toEnum()

    @TypeConverter fun fromEquipment(value: Equipment): String = value.toDb()

    @TypeConverter fun toEquipment(value: String): Equipment = value.toEnum()

    @TypeConverter fun fromSetType(value: SetType): String = value.toDb()

    @TypeConverter fun toSetType(value: String): SetType = value.toEnum()

    @TypeConverter fun fromSide(value: Side): String = value.toDb()

    @TypeConverter fun toSide(value: String): Side = value.toEnum()
}

private fun Enum<*>.toDb(): String = name.lowercase()

private inline fun <reified E : Enum<E>> String.toEnum(): E =
    enumValues<E>().firstOrNull { it.name.lowercase() == this }
        ?: throw IllegalArgumentException("Unknown ${E::class.simpleName} value '$this'")
