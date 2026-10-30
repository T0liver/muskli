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
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.usecase.workout.AddExercisesUseCase
import hu.toliver.muskli.domain.usecase.workout.DeleteWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.EditSetValuesUseCase
import hu.toliver.muskli.domain.usecase.workout.FinishWorkoutResult
import hu.toliver.muskli.domain.usecase.workout.FinishWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.MarkSetCompletedUseCase
import hu.toliver.muskli.domain.usecase.workout.PreviousSetsUseCase
import hu.toliver.muskli.domain.usecase.workout.RestoreWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.SetValues
import hu.toliver.muskli.domain.usecase.workout.StartPastWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.WorkoutDetails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** T-016 history flows against Room: newest first, soft delete with undo, edits feeding previous values, past dates. */
@RunWith(AndroidJUnit4::class)
class WorkoutHistoryTest {
    private lateinit var db: MuskliDatabase
    private val clock = object : Clock {
        override fun nowEpochMs() = 1_000_000L

        override fun utcOffsetS(epochMs: Long) = 3_600
    }
    private var nextId = 0
    private val ids = IdGenerator { "id${nextId++}" }
    private val repo get() = RoomWorkoutRepository(db)

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), MuskliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        listOf(benchPress, dumbbellRow).forEach { db.exerciseTemplateDao().upsert(it.toEntity(), it.secondaryMuscleEntities()) }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `finished workouts are listed newest first, a deleted one hides and undo restores it (TC-DATA-03)`() = runTest {
        repo.save(workout("old", startedAt = 10_000L))
        repo.save(workout("new", startedAt = 30_000L))
        repo.save(workout("draft", startedAt = 40_000L, finishedAt = null))

        assertEquals(listOf("new", "old"), repo.observeHistory().first().map { it.id })

        DeleteWorkoutUseCase(repo, clock)("new")
        assertEquals(listOf("old"), repo.observeHistory().first().map { it.id })
        assertTrue(PreviousSetsUseCase(repo)(benchPress.id).isNotEmpty())

        RestoreWorkoutUseCase(repo)("new")
        assertEquals(listOf("new", "old"), repo.observeHistory().first().map { it.id })
    }

    @Test
    fun `editing a past workout persists and changes previous values for later sessions`() = runTest {
        repo.save(workout("w1"))
        val benchSet = "w1-e1-s2"

        EditSetValuesUseCase(repo, clock)("w1", benchSet, SetValues(weightKg = 90.0, reps = 3))

        assertEquals(SetData(SetType.NORMAL, weightKg = 90.0, reps = 3, completed = true), PreviousSetsUseCase(repo)(benchPress.id)[1])
        assertEquals(90.0, repo.observe("w1").first()!!.exercises[0].sets[1].data.weightKg!!, 0.0)
    }

    @Test
    fun `a past workout logged on a chosen date is stored with that date and sorts by it`() = runTest {
        repo.save(workout("recent", startedAt = 500_000L))
        val past = StartPastWorkoutUseCase(repo, ids, clock)(startedAtEpochMs = 100_000L)
        AddExercisesUseCase(repo, ids, clock)(past.id, listOf(dumbbellRow.id))
        val setId = repo.observe(past.id).first()!!.exercises.single().sets.first().id
        EditSetValuesUseCase(repo, clock)(past.id, setId, SetValues(weightKg = 30.0, reps = 10))
        MarkSetCompletedUseCase(repo, clock)(past.id, setId)

        val result = FinishWorkoutUseCase(repo, clock)(past.id, WorkoutDetails("Back", null, startedAtEpochMs = 100_000L, durationS = 1_800))

        val saved = (result as FinishWorkoutResult.Finished).workout
        assertEquals(saved, repo.observe(past.id).first())
        assertEquals(listOf("recent", past.id), repo.observeHistory().first().map { it.id })
        assertNull(repo.observeActiveDraft().first())
    }
}
