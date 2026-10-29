package hu.toliver.muskli.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side
import hu.toliver.muskli.domain.model.Workout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {
    private lateinit var db: MuskliDatabase

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MuskliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        listOf(benchPress, dumbbellRow).forEach {
            db.exerciseTemplateDao().upsert(it.toEntity(), it.secondaryMuscleEntities())
        }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun save(workout: Workout) {
        val rows = workout.toRows()
        db.workoutDao().save(rows.workout, rows.exercises, rows.sets)
    }

    private fun count(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun `TC-DATA-01 saved workout with nested exercises and sets reads back equal`() = runTest {
        val original = workout()

        save(original)

        assertEquals(original, db.workoutDao().observe(original.id).first()?.toDomain())
    }

    @Test
    fun `saving again replaces exercises and sets instead of adding to them`() = runTest {
        val original = workout()
        save(original)
        val edited = original.copy(exercises = original.exercises.take(1))

        save(edited)

        assertEquals(edited, db.workoutDao().observe(original.id).first()?.toDomain())
        assertEquals(3, count("workout_set"))
    }

    @Test
    fun `active draft is the unfinished workout and history holds finished ones`() = runTest {
        save(workout(id = "done", finishedAt = 20_000L))
        save(workout(id = "draft", startedAt = 30_000L, finishedAt = null))

        assertEquals("draft", db.workoutDao().observeActiveDraft().first()?.workout?.id)
        assertEquals(listOf("done"), db.workoutDao().observeHistory().first().map { it.workout.id })
    }

    @Test
    fun `TC-DATA-03 soft-deleted workout is excluded from history and previous values and restorable`() = runTest {
        save(workout(id = "w1"))
        val dao = db.workoutDao()

        dao.softDelete("w1", deletedAt = 50_000L)

        assertTrue(dao.observeHistory().first().isEmpty())
        assertTrue(dao.finishedWorkoutsWithExercise(benchPress.id).isEmpty())
        assertEquals(50_000L, dao.observe("w1").first()?.workout?.deletedAt)

        dao.restore("w1")

        assertEquals(listOf("w1"), dao.observeHistory().first().map { it.workout.id })
        val records = dao.finishedWorkoutsWithExercise(benchPress.id).flatMap { it.toExerciseRecords(benchPress.id) }
        assertEquals(1, records.size)
        assertEquals(0, records.single().positionInWorkout)
        assertEquals(workout().exercises.first().sets.map { it.data }, records.single().sets)
    }

    @Test
    fun `TC-DATA-07 removing a workout removes its exercises and sets`() = runTest {
        save(workout(id = "w1"))
        save(workout(id = "w2"))

        db.workoutDao().delete("w1")

        assertNull(db.workoutDao().observe("w1").first())
        assertEquals(2, count("workout_exercise"))
        assertEquals(5, count("workout_set"))
    }

    @Test
    fun `D-022 a set row without side or set type defaults to both and normal`() = runTest {
        save(workout(id = "w1"))

        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO workout_set (id, workout_exercise_id, position, weight_kg, reps, distance_m, duration_s, completed_at) " +
                "VALUES ('raw', 'w1-e1', 3, 60.0, 5, NULL, NULL, NULL)",
        )

        val set = db.workoutDao().observe("w1").first()!!.toDomain().exercises.first().sets.last()
        assertEquals("raw", set.id)
        assertEquals(Side.BOTH, set.data.side)
        assertEquals(SetType.NORMAL, set.data.type)
        assertEquals(false, set.data.completed)
    }
}
