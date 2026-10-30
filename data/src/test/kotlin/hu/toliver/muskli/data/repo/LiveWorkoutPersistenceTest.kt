package hu.toliver.muskli.data.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.data.db.MuskliDatabase
import hu.toliver.muskli.data.db.benchPress
import hu.toliver.muskli.data.db.dumbbellRow
import hu.toliver.muskli.data.db.secondaryMuscleEntities
import hu.toliver.muskli.data.db.toEntity
import hu.toliver.muskli.domain.calc.RestEffect
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.usecase.workout.AddExercisesUseCase
import hu.toliver.muskli.domain.usecase.workout.AddSetUseCase
import hu.toliver.muskli.domain.usecase.workout.ChangeSetTypeUseCase
import hu.toliver.muskli.domain.usecase.workout.CompleteSetUseCase
import hu.toliver.muskli.domain.usecase.workout.DeleteSetUseCase
import hu.toliver.muskli.domain.usecase.workout.EditSetValuesUseCase
import hu.toliver.muskli.domain.usecase.workout.RemoveExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.ReorderExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.ReplaceExerciseUseCase
import hu.toliver.muskli.domain.usecase.workout.SetValues
import hu.toliver.muskli.domain.usecase.workout.StartWorkoutResult
import hu.toliver.muskli.domain.usecase.workout.StartWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.UncompleteSetUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/** Uses the file database, closed and reopened between steps to simulate process death (TC-DATA-02). */
@RunWith(AndroidJUnit4::class)
class LiveWorkoutPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var db = MuskliDatabase.build(context)
    private var now = 10_000L
    private val clock = object : Clock {
        override fun nowEpochMs() = now

        override fun utcOffsetS(epochMs: Long) = 3_600
    }
    private val nextId = AtomicInteger()
    private val ids = IdGenerator { "id${nextId.getAndIncrement()}" }
    private val squat = benchPress.copy(id = "squat", title = "Squat", secondaryMuscles = emptySet())

    private val repo get() = RoomWorkoutRepository(db)

    @Before
    fun setUp() = runTest {
        listOf(benchPress, dumbbellRow, squat).forEach { db.exerciseTemplateDao().upsert(it.toEntity(), it.secondaryMuscleEntities()) }
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(MuskliDatabase.NAME)
    }

    private suspend fun draftAfterReopen(): Workout? {
        db.close()
        db = MuskliDatabase.build(context)
        return repo.observeActiveDraft().first()
    }

    /** Runs [mutation], then checks the reopened database holds exactly the workout it returned. */
    private suspend fun persisted(mutation: suspend () -> Workout?): Workout {
        val returned = mutation()!!
        assertEquals(returned, draftAfterReopen())
        return returned
    }

    @Test
    fun `TC-DATA-02 every live-workout mutation survives recreating the database connection`() = runTest {
        val started = (StartWorkoutUseCase(repo, ids, clock)() as StartWorkoutResult.Started).workout
        assertEquals(started, draftAfterReopen())
        val id = started.id

        var workout = persisted { AddExercisesUseCase(repo, ids, clock)(id, listOf(benchPress.id, dumbbellRow.id, squat.id)) }
        val bench = workout.exercises[0].id
        workout = persisted { AddSetUseCase(repo, ids, clock)(id, bench) }
        workout = persisted { AddSetUseCase(repo, ids, clock)(id, bench) }
        val (first, second, third) = workout.exercises[0].sets.map { it.id }
        workout = persisted { EditSetValuesUseCase(repo, clock)(id, first, SetValues(weightKg = 100.0, reps = 5)) }
        workout = persisted { ChangeSetTypeUseCase(repo, clock)(id, second, SetType.DROPSET) }
        workout = persisted { ChangeSetTypeUseCase(repo, clock)(id, third, SetType.DROPSET) }

        now = 12_000L
        assertEquals(RestEffect.None, CompleteSetUseCase(repo, clock)(id, first, defaultRestS = 90))
        val completed = draftAfterReopen()!!.exercises[0].sets[0]
        assertEquals(true, completed.data.completed)
        assertEquals(12_000L, completed.completedAtEpochMs)

        workout = persisted { UncompleteSetUseCase(repo, clock)(id, first) }
        workout = persisted { DeleteSetUseCase(repo, clock)(id, second) }
        workout = persisted { ReorderExerciseUseCase(repo, clock)(id, bench, toIndex = 2) }
        workout = persisted { ReplaceExerciseUseCase(repo, ids, clock)(id, workout.exercises[0].id, squat.id) }
        workout = persisted { RemoveExerciseUseCase(repo, clock)(id, workout.exercises[1].id) }

        assertEquals(listOf(squat.id, benchPress.id), workout.exercises.map { it.exerciseTemplateId })
        assertEquals(listOf(first, third), workout.exercises[1].sets.map { it.id })
    }

    @Test
    fun `TC-SET-03 and TC-SET-04 stored positions are contiguous from 0 and sets keep their exercise`() = runTest {
        val id = (StartWorkoutUseCase(repo, ids, clock)() as StartWorkoutResult.Started).workout.id
        val added = AddExercisesUseCase(repo, ids, clock)(id, listOf(benchPress.id, dumbbellRow.id, squat.id))!!
        val bench = added.exercises[0].id
        repeat(3) { AddSetUseCase(repo, ids, clock)(id, bench) }
        val benchSets = repo.observe(id).first()!!.exercises[0].sets.map { it.id }

        DeleteSetUseCase(repo, clock)(id, benchSets[1])
        ReorderExerciseUseCase(repo, clock)(id, bench, toIndex = 1)
        draftAfterReopen()

        val rows = db.workoutDao().observe(id).first()!!
        assertEquals(listOf(0, 1, 2), rows.exercises.map { it.exercise.position }.sorted())
        val benchRow = rows.exercises.single { it.exercise.id == bench }
        assertEquals(1, benchRow.exercise.position)
        assertEquals(listOf(benchSets[0], benchSets[2], benchSets[3]), benchRow.sets.sortedBy { it.position }.map { it.id })
        assertEquals(listOf(0, 1, 2), benchRow.sets.map { it.position }.sorted())
    }

    @Test
    fun `starting while a draft exists resumes it and keeps one draft`() = runTest {
        val first = (StartWorkoutUseCase(repo, ids, clock)() as StartWorkoutResult.Started).workout
        draftAfterReopen()

        val again = StartWorkoutUseCase(repo, ids, clock)()

        assertEquals(StartWorkoutResult.Resumed(first), again)
        val drafts = withContext(Dispatchers.IO) {
            db.query("SELECT COUNT(*) FROM workout WHERE end_time IS NULL", null).use {
                it.moveToFirst()
                it.getInt(0)
            }
        }
        assertEquals(1, drafts)
    }

    @Test
    fun `concurrent edits are not lost`() = runTest {
        val id = (StartWorkoutUseCase(repo, ids, clock)() as StartWorkoutResult.Started).workout.id
        val bench = AddExercisesUseCase(repo, ids, clock)(id, listOf(benchPress.id))!!.exercises[0].id
        val addSet = AddSetUseCase(repo, ids, clock)

        coroutineScope { repeat(20) { launch(Dispatchers.IO) { addSet(id, bench) } } }

        assertEquals(21, draftAfterReopen()!!.exercises[0].sets.size)
    }
}
