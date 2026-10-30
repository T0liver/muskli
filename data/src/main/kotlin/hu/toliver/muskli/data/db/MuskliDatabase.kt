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
import hu.toliver.muskli.data.db.dao.WorkoutDao
import hu.toliver.muskli.data.db.entity.CatalogueMetaEntity
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
        CatalogueMetaEntity::class,
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

    companion object {
        const val NAME = "muskli.db"
        const val VERSION = 3

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

        /** Every schema change adds its migration here and a test in `MigrationTest` (TC-DATA-04). */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

        /** Lives in the default database directory so Android Auto Backup includes it (D-024). */
        fun build(context: Context): MuskliDatabase =
            Room.databaseBuilder(context, MuskliDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
