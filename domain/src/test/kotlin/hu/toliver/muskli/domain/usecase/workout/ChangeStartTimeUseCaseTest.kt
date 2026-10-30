package hu.toliver.muskli.domain.usecase.workout

import hu.toliver.muskli.domain.TestClock
import hu.toliver.muskli.domain.model.RestTimer
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.IdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChangeStartTimeUseCaseTest {
    private val repo = FakeWorkoutRepository()
    private val clock = TestClock(now = 3_600_000L, offsetS = 3_600)
    private var nextId = 0
    private val ids = IdGenerator { "id${nextId++}" }
    private val start = StartWorkoutUseCase(repo, ids, clock)
    private val addExercises = AddExercisesUseCase(repo, ids, clock)
    private val completeSet = CompleteSetUseCase(repo, clock)
    private val changeStart = ChangeStartTimeUseCase(repo, clock)

    private suspend fun stored(id: String): Workout = repo.observe(id).first()!!

    private suspend fun draft(): Workout = (start() as StartWorkoutResult.Started).workout

    @Test
    fun `an earlier start is stored and bumps updatedAt`() = runTest {
        val workout = draft()
        clock.now = 4_000_000L

        val result = changeStart(workout.id, 1_000_000L)

        val expected = workout.copy(startedAtEpochMs = 1_000_000L, updatedAtEpochMs = 4_000_000L)
        assertEquals(ChangeStartTimeResult.Changed(expected), result)
        assertEquals(expected, stored(workout.id))
    }

    @Test
    fun `a start in the future is rejected and nothing is written`() = runTest {
        val workout = draft()
        val writes = repo.writes

        assertEquals(ChangeStartTimeResult.InFuture, changeStart(workout.id, clock.now + 1))
        assertEquals(writes, repo.writes)
        assertEquals(workout, stored(workout.id))
    }

    @Test
    fun `a start exactly now is accepted`() = runTest {
        val workout = draft()
        clock.now = 5_000_000L

        val result = changeStart(workout.id, 5_000_000L)

        assertEquals(5_000_000L, (result as ChangeStartTimeResult.Changed).workout.startedAtEpochMs)
    }

    @Test
    fun `the offset is taken at the new start`() = runTest {
        val dstClock = object : Clock {
            override fun nowEpochMs() = 10_000_000L

            override fun utcOffsetS(epochMs: Long) = if (epochMs < 5_000_000L) 3_600 else 7_200
        }
        val workout = StartWorkoutUseCase(repo, ids, dstClock)().workout
        assertEquals(7_200, workout.utcOffsetS)

        ChangeStartTimeUseCase(repo, dstClock)(workout.id, 1_000_000L)

        assertEquals(3_600, stored(workout.id).utcOffsetS)
    }

    @Test
    fun `sets and the running rest are kept`() = runTest {
        val workout = addExercises(draft().id, listOf("bench", "row"))!!
        completeSet(workout.id, workout.exercises[0].sets[0].id, defaultRestS = 90)
        val before = stored(workout.id)

        changeStart(workout.id, 1_000L)

        val after = stored(workout.id)
        assertEquals(before.exercises, after.exercises)
        assertEquals(RestTimer(endsAtEpochMs = 3_690_000L, durationS = 90), after.restTimer)
    }

    @Test
    fun `the same start changes nothing`() = runTest {
        val workout = draft()
        val writes = repo.writes

        assertEquals(ChangeStartTimeResult.Changed(workout), changeStart(workout.id, workout.startedAtEpochMs))
        assertEquals(writes, repo.writes)
    }

    @Test
    fun `a finished or unknown workout is not changed`() = runTest {
        val finished = draft().copy(finishedAtEpochMs = 3_600_000L)
        repo.save(finished)
        val writes = repo.writes

        assertNull(changeStart(finished.id, 1_000L))
        assertNull(changeStart("missing", 1_000L))
        assertEquals(writes, repo.writes)
        assertEquals(finished, stored(finished.id))
    }
}
