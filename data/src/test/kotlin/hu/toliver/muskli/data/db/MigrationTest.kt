package hu.toliver.muskli.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Harness for TC-DATA-04. Each migration N to N+1 gets a test here that creates version N from its
 * exported schema, inserts rows, runs the migration with validation, and checks the rows survived.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // The file-and-driver constructor, because the name-based one rejects Windows paths (Room 2.8.5).
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        context.getDatabasePath(dbName),
        AndroidSQLiteDriver(),
        MuskliDatabase::class,
    )

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `version 1 created from the exported schema opens with the current entities`() = runTest {
        helper.createDatabase(1).apply {
            execSQL(
                "INSERT INTO exercise_template (id, title, exercise_type, primary_muscle, equipment, is_custom, archived, created_at, updated_at) " +
                    "VALUES ('bench-press', 'Bench press', 'weight_reps', 'chest', 'barbell', 0, 0, 1, 1)",
            )
            execSQL(
                "INSERT INTO workout (id, title, notes, start_time, utc_offset_s, end_time, created_at, updated_at, deleted_at) " +
                    "VALUES ('w1', 'Push', NULL, 10, 0, 20, 10, 10, NULL)",
            )
            execSQL(
                "INSERT INTO workout_exercise (id, workout_id, position, exercise_template_id, superset_group, notes, rest_seconds) " +
                    "VALUES ('e1', 'w1', 0, 'bench-press', NULL, NULL, NULL)",
            )
            execSQL(
                "INSERT INTO workout_set (id, workout_exercise_id, position, set_type, weight_kg, reps, distance_m, duration_s, completed, completed_at, side) " +
                    "VALUES ('s1', 'e1', 0, 'normal', 80.0, 5, NULL, NULL, 1, 15, 'both')",
            )
            close()
        }

        helper.runMigrationsAndValidate(MuskliDatabase.VERSION, MuskliDatabase.MIGRATIONS.toList()).close()

        val room = Room.databaseBuilder(context, MuskliDatabase::class.java, dbName)
            .addMigrations(*MuskliDatabase.MIGRATIONS)
            .build()
        try {
            val set = room.workoutDao().observe("w1").first()!!.toDomain().exercises.single().sets.single()
            assertEquals(80.0, set.data.weightKg)
        } finally {
            room.close()
        }
    }

    @Test
    fun `TC-DATA-04 migration 1 to 2 keeps the library and starts with no catalogue version`() = runTest {
        helper.createDatabase(1).apply {
            execSQL(
                "INSERT INTO exercise_template (id, title, exercise_type, primary_muscle, equipment, is_custom, archived, created_at, updated_at) " +
                    "VALUES ('my-fly', 'My fly', 'weight_reps', 'chest', 'machine', 1, 0, 1, 1)",
            )
            close()
        }

        helper.runMigrationsAndValidate(2, listOf(MuskliDatabase.MIGRATION_1_2)).close()

        val room = Room.databaseBuilder(context, MuskliDatabase::class.java, dbName)
            .addMigrations(*MuskliDatabase.MIGRATIONS)
            .build()
        try {
            assertEquals("My fly", room.exerciseTemplateDao().get("my-fly")?.toDomain()?.title)
            assertNull(room.catalogueMetaDao().version())
        } finally {
            room.close()
        }
    }
}
