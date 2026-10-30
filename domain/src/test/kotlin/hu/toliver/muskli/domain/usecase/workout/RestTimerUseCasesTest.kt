package hu.toliver.muskli.domain.usecase.workout

import hu.toliver.muskli.domain.TestClock
import hu.toliver.muskli.domain.calc.RestEffect
import hu.toliver.muskli.domain.model.RestTimer
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.IdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestTimerUseCasesTest {
    private val repo = FakeWorkoutRepository()
    private val clock = TestClock(now = 10_000L)
    private var nextId = 0
    private val ids = IdGenerator { "id${nextId++}" }
    private val start = StartWorkoutUseCase(repo, ids, clock)
    private val addExercises = AddExercisesUseCase(repo, ids, clock)
    private val addSet = AddSetUseCase(repo, ids, clock)
    private val changeSetType = ChangeSetTypeUseCase(repo, clock)
    private val completeSet = CompleteSetUseCase(repo, clock)
    private val uncompleteSet = UncompleteSetUseCase(repo, clock)
    private val adjustRest = AdjustRestTimerUseCase(repo, clock)
    private val skipRest = SkipRestTimerUseCase(repo, clock)
    private val finishRest = FinishRestTimerUseCase(repo)

    private suspend fun stored(id: String): Workout = repo.observe(id).first()!!

    /** One exercise per template id, each with [setsEach] normal sets. */
    private suspend fun startWith(vararg templateIds: String, setsEach: Int = 1): Workout {
        val draft = (start() as StartWorkoutResult.Started).workout
        val workout = addExercises(draft.id, templateIds.toList())!!
        workout.exercises.forEach { exercise -> repeat(setsEach - 1) { addSet(workout.id, exercise.id) } }
        return stored(workout.id)
    }

    private fun Workout.setId(exercise: Int, set: Int) = exercises[exercise].sets[set].id

    @Test
    fun `completing a set that starts a rest persists when it ends and its length`() = runTest {
        val workout = startWith("bench", setsEach = 2)
        clock.now = 50_000L

        assertEquals(RestEffect.Start(90), completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90))

        assertEquals(RestTimer(endsAtEpochMs = 140_000L, durationS = 90), stored(workout.id).restTimer)
    }

    @Test
    fun `AC-05 completing the set before a drop set persists no rest`() = runTest {
        val workout = startWith("bench", setsEach = 2)
        changeSetType(workout.id, workout.setId(0, 1), SetType.DROPSET)

        assertEquals(RestEffect.None, completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90))

        assertNull(stored(workout.id).restTimer)
    }

    @Test
    fun `AC-05 a running rest is cancelled when the set before a drop set is completed (D-032)`() = runTest {
        val workout = startWith("bench", setsEach = 3)
        changeSetType(workout.id, workout.setId(0, 2), SetType.DROPSET)
        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)

        assertEquals(RestEffect.None, completeSet(workout.id, workout.setId(0, 1), defaultRestS = 90))

        assertNull(stored(workout.id).restTimer)
    }

    @Test
    fun `TC-REST-07 completing the last set of the workout persists no rest and cancels a running one`() = runTest {
        val workout = startWith("bench", "row")
        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)

        assertEquals(RestEffect.None, completeSet(workout.id, workout.setId(1, 0), defaultRestS = 90))

        assertNull(stored(workout.id).restTimer)
    }

    @Test
    fun `a new rest replaces the running one`() = runTest {
        val workout = startWith("bench", setsEach = 3)
        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)
        clock.now = 30_000L

        completeSet(workout.id, workout.setId(0, 1), defaultRestS = 60)

        assertEquals(RestTimer(endsAtEpochMs = 90_000L, durationS = 60), stored(workout.id).restTimer)
    }

    @Test
    fun `re-completing an already completed set leaves the running rest alone`() = runTest {
        val workout = startWith("bench", setsEach = 3)
        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)
        val running = stored(workout.id).restTimer
        clock.now = 30_000L

        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)

        assertEquals(running, stored(workout.id).restTimer)
    }

    @Test
    fun `uncompleting the set keeps the running rest (D-032)`() = runTest {
        val workout = startWith("bench", setsEach = 2)
        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)

        uncompleteSet(workout.id, workout.setId(0, 0))

        assertEquals(RestTimer(endsAtEpochMs = 100_000L, durationS = 90), stored(workout.id).restTimer)
    }

    @Test
    fun `plus and minus 15 s move the end and the length`() = runTest {
        val workout = startWith("bench", setsEach = 2)
        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)

        adjustRest(workout.id, deltaS = 15)
        assertEquals(RestTimer(endsAtEpochMs = 115_000L, durationS = 105), stored(workout.id).restTimer)

        adjustRest(workout.id, deltaS = -15)
        adjustRest(workout.id, deltaS = -15)
        assertEquals(RestTimer(endsAtEpochMs = 85_000L, durationS = 75), stored(workout.id).restTimer)
    }

    @Test
    fun `minus 15 s with 15 s or less left ends the rest (D-032)`() = runTest {
        val workout = startWith("bench", setsEach = 2)
        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)
        clock.now = 85_000L

        adjustRest(workout.id, deltaS = -15)

        assertNull(stored(workout.id).restTimer)
    }

    @Test
    fun `adjusting with no rest running or after it ended changes nothing`() = runTest {
        val workout = startWith("bench", setsEach = 2)
        val writes = repo.writes
        adjustRest(workout.id, deltaS = 15)
        assertEquals(writes, repo.writes)

        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)
        val ended = stored(workout.id).restTimer
        clock.now = 100_000L
        adjustRest(workout.id, deltaS = 15)

        assertEquals(ended, stored(workout.id).restTimer)
    }

    @Test
    fun `skip clears the rest`() = runTest {
        val workout = startWith("bench", setsEach = 2)
        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)

        skipRest(workout.id)

        assertNull(stored(workout.id).restTimer)
    }

    @Test
    fun `finish clears the rest once, so only the first of service and alarm alerts`() = runTest {
        val workout = startWith("bench", setsEach = 2)
        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)

        assertTrue(finishRest(workout.id, endsAtEpochMs = 100_000L))
        assertFalse(finishRest(workout.id, endsAtEpochMs = 100_000L))
        assertNull(stored(workout.id).restTimer)
    }

    @Test
    fun `finish for an end that was moved or skipped does nothing`() = runTest {
        val workout = startWith("bench", setsEach = 2)
        completeSet(workout.id, workout.setId(0, 0), defaultRestS = 90)
        adjustRest(workout.id, deltaS = 15)

        assertFalse(finishRest(workout.id, endsAtEpochMs = 100_000L))
        assertEquals(115_000L, stored(workout.id).restTimer?.endsAtEpochMs)

        skipRest(workout.id)
        assertFalse(finishRest(workout.id, endsAtEpochMs = 115_000L))
        assertFalse(finishRest("missing", endsAtEpochMs = 115_000L))
    }
}
