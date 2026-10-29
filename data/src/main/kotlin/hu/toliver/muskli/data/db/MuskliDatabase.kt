package hu.toliver.muskli.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import hu.toliver.muskli.data.db.dao.ExerciseTemplateDao
import hu.toliver.muskli.data.db.dao.WorkoutDao
import hu.toliver.muskli.data.db.entity.ExerciseTemplateEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateSecondaryMuscleEntity
import hu.toliver.muskli.data.db.entity.WorkoutEntity
import hu.toliver.muskli.data.db.entity.WorkoutExerciseEntity
import hu.toliver.muskli.data.db.entity.WorkoutSetEntity

@Database(
    entities = [
        ExerciseTemplateEntity::class,
        ExerciseTemplateSecondaryMuscleEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
    ],
    version = MuskliDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MuskliDatabase : RoomDatabase() {
    abstract fun exerciseTemplateDao(): ExerciseTemplateDao

    abstract fun workoutDao(): WorkoutDao

    companion object {
        const val NAME = "muskli.db"
        const val VERSION = 1

        /** Every schema change adds its migration here and a test in `MigrationTest` (TC-DATA-04). */
        val MIGRATIONS: Array<Migration> = emptyArray()

        /** Lives in the default database directory so Android Auto Backup includes it (D-024). */
        fun build(context: Context): MuskliDatabase =
            Room.databaseBuilder(context, MuskliDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
