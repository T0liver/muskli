package hu.toliver.muskli.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import hu.toliver.muskli.data.db.dao.BackupDao
import hu.toliver.muskli.data.db.dao.CatalogueMetaDao
import hu.toliver.muskli.data.db.dao.ExerciseTemplateDao
import hu.toliver.muskli.data.db.dao.RoutineDao
import hu.toliver.muskli.data.db.dao.WorkoutDao
import hu.toliver.muskli.data.db.entity.CatalogueMetaEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateEntity
import hu.toliver.muskli.data.db.entity.ExerciseTemplateSecondaryMuscleEntity
import hu.toliver.muskli.data.db.entity.RoutineEntity
import hu.toliver.muskli.data.db.entity.RoutineExerciseEntity
import hu.toliver.muskli.data.db.entity.RoutineSetEntity
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
        CatalogueMetaEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        RoutineSetEntity::class,
    ],
    version = MuskliDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MuskliDatabase : RoomDatabase() {
    abstract fun exerciseTemplateDao(): ExerciseTemplateDao

    abstract fun workoutDao(): WorkoutDao

    abstract fun catalogueMetaDao(): CatalogueMetaDao

    abstract fun backupDao(): BackupDao

    abstract fun routineDao(): RoutineDao

    companion object {
        const val NAME = "muskli.db"
        const val VERSION = 5

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalogue_meta` (`id` INTEGER NOT NULL, `version` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
            }
        }

        /** The live workout's running rest (D-032). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `workout` ADD COLUMN `rest_ends_at` INTEGER")
                connection.execSQL("ALTER TABLE `workout` ADD COLUMN `rest_duration_s` INTEGER")
            }
        }

        /** The unilateral flag (D-037). Existing rows are bilateral until a newer catalogue or the editor says otherwise. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `exercise_template` ADD COLUMN `unilateral` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Routines and the routine a workout was started from (D-052). Existing workouts have none. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE `workout` ADD COLUMN `routine_id` TEXT")
                connection.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_routine_id` ON `workout` (`routine_id`)")
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routine` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                        "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routine_exercise` (`routine_id` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                        "`exercise_template_id` TEXT NOT NULL, `superset_group` INTEGER, `notes` TEXT, `rest_seconds` INTEGER, " +
                        "PRIMARY KEY(`routine_id`, `position`), " +
                        "FOREIGN KEY(`routine_id`) REFERENCES `routine`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                        "FOREIGN KEY(`exercise_template_id`) REFERENCES `exercise_template`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_routine_exercise_exercise_template_id` ON `routine_exercise` (`exercise_template_id`)",
                )
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `routine_set` (`routine_id` TEXT NOT NULL, `exercise_position` INTEGER NOT NULL, " +
                        "`position` INTEGER NOT NULL, `set_type` TEXT NOT NULL DEFAULT 'normal', `weight_kg` REAL, `reps` INTEGER, " +
                        "`distance_m` REAL, `duration_s` INTEGER, PRIMARY KEY(`routine_id`, `exercise_position`, `position`), " +
                        "FOREIGN KEY(`routine_id`, `exercise_position`) REFERENCES `routine_exercise`(`routine_id`, `position`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
            }
        }

        /** Every schema change adds its migration here and a test in `MigrationTest` (TC-DATA-04). */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

        /** Lives in the default database directory so Android Auto Backup includes it (D-024). */
        fun build(context: Context): MuskliDatabase =
            Room.databaseBuilder(context, MuskliDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
