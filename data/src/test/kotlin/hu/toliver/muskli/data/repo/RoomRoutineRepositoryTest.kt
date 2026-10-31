package hu.toliver.muskli.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.db.benchPress
import hu.toliver.muskli.data.db.dumbbellRow
import hu.toliver.muskli.data.db.secondaryMuscleEntities
import hu.toliver.muskli.data.db.toEntity
import hu.toliver.muskli.data.db.workout
import hu.toliver.muskli.domain.model.PlannedSet
import hu.toliver.muskli.domain.model.Routine
import hu.toliver.muskli.domain.model.RoutineExercise
import hu.toliver.muskli.domain.model.SetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRoutineRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databases = mutableListOf<MuskliDatabase>()

    @After
    fun tearDown() {
        databases.forEach { it.close() }
        context.deleteDatabase(MuskliDatabase.NAME)
    }

    private suspend fun MuskliDatabase.withTemplates() = apply {
        listOf(benchPress, dumbbellRow).forEach { exerciseTemplateDao().upsert(it.toEntity(), it.secondaryMuscleEntities()) }
    }

    private suspend fun inMemory(): MuskliDatabase =
        Room.inMemoryDatabaseBuilder(context, MuskliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also { databases += it }
            .withTemplates()

    private fun routine(id: String = "r1", title: String = "Push A") = Routine(
        id = id,
        title = title,
        createdAtEpochMs = 1_000L,
        updatedAtEpochMs = 2_000L,
        exercises = listOf(
            RoutineExercise(
                exerciseTemplateId = benchPress.id,
                supersetGroup = 1,
                notes = "pause at the chest",
                restSeconds = 120,
                sets = listOf(PlannedSet(SetType.WARMUP, weightKg = 40.0, reps = 10), PlannedSet(weightKg = 80.0, reps = 5)),
            ),
            RoutineExercise(exerciseTemplateId = dumbbellRow.id, restSeconds = 0, sets = emptyList()),
            RoutineExercise(exerciseTemplateId = benchPress.id, sets = listOf(PlannedSet(SetType.DROPSET, weightKg = 60.0))),
        ),
    )

    @Test
    fun `a saved routine reads back equal, in order, and saving again replaces its exercises and sets`() = runTest {
        val repo = RoomRoutineRepository(inMemory().routineDao())

        repo.save(routine())
        assertEquals(routine(), repo.get("r1"))
        assertEquals(routine(), repo.observe("r1").first())

        val edited = routine().copy(title = "Push B", exercises = routine().exercises.drop(2))
        repo.save(edited)
        assertEquals(edited, repo.get("r1"))
    }

    @Test
    fun `routines are listed by title ignoring case, then id`() = runTest {
        val repo = RoomRoutineRepository(inMemory().routineDao())
        repo.save(routine("r1", "pull"))
        repo.save(routine("r3", "Legs"))
        repo.save(routine("r2", "Legs"))
        repo.save(routine("r4", "Push"))

        assertEquals(listOf("r2", "r3", "r1", "r4"), repo.observeAll().first().map { it.id })
    }

    @Test
    fun `routines and a draft's routineId survive recreating the database connection`() = runTest {
        MuskliDatabase.build(context).run {
            withTemplates()
            RoomRoutineRepository(routineDao()).save(routine())
            RoomWorkoutRepository(this).save(workout("draft", finishedAt = null).copy(routineId = "r1"))
            close()
        }

        val reopened = MuskliDatabase.build(context).also { databases += it }
        assertEquals(listOf(routine()), RoomRoutineRepository(reopened.routineDao()).observeAll().first())
        assertEquals("r1", RoomWorkoutRepository(reopened).observeActiveDraft().first()?.routineId)
    }

    @Test
    fun `deleting a routine removes its rows and clears routineId on its workouts only`() = runTest {
        val db = inMemory()
        val repo = RoomRoutineRepository(db.routineDao())
        val workouts = RoomWorkoutRepository(db)
        repo.save(routine("r1"))
        repo.save(routine("r2"))
        workouts.save(workout("w1").copy(routineId = "r1"))
        workouts.save(workout("w2", startedAt = 30_000L, finishedAt = 40_000L).copy(routineId = "r2"))

        repo.delete("r1")

        assertNull(repo.get("r1"))
        assertEquals(listOf("r2"), repo.observeAll().first().map { it.id })
        assertNull(workouts.observe("w1").first()!!.routineId)
        assertEquals(workout("w1").exercises, workouts.observe("w1").first()!!.exercises)
        assertEquals("r2", workouts.observe("w2").first()!!.routineId)
    }
}
