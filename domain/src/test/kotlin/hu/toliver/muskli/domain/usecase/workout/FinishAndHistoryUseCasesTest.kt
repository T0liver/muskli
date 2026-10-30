package hu.toliver.muskli.domain.usecase.workout

import hu.toliver.muskli.domain.TestClock
import hu.toliver.muskli.domain.calc.WorkoutExerciseRecord
import hu.toliver.muskli.domain.model.RestTimer
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import hu.toliver.muskli.domain.repo.IdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FinishAndHistoryUseCasesTest {
    private val repo = FakeWorkoutRepository()
    private val clock = TestClock(now = 5_000_000L, offsetS = 7_200)
    private var nextId = 0
    private val ids = IdGenerator { "id${nextId++}" }
    private val finish = FinishWorkoutUseCase(repo, clock)
    private val delete = DeleteWorkoutUseCase(repo, clock)
    private val restore = RestoreWorkoutUseCase(repo)
    private val startPast = StartPastWorkoutUseCase(repo, ids, clock)
    private val markCompleted = MarkSetCompletedUseCase(repo, clock)

    private fun set(id: String, completed: Boolean, weightKg: Double = 100.0) =
        WorkoutSet(id, SetData(weightKg = weightKg, reps = 5, completed = completed), completedAtEpochMs = if (completed) 1_000L else null)

    private fun exercise(id: String, vararg sets: WorkoutSet) =
        WorkoutExercise(id, "t-$id", supersetGroup = null, notes = null, restSeconds = null, sets = sets.toList())

    private fun draft(vararg exercises: WorkoutExercise, finishedAt: Long? = null) = Workout(
        id = "w",
        title = "",
        notes = null,
        startedAtEpochMs = 1_000_000L,
        utcOffsetS = 0,
        finishedAtEpochMs = finishedAt,
        createdAtEpochMs = 1_000_000L,
        updatedAtEpochMs = 1_000_000L,
        deletedAtEpochMs = null,
        exercises = exercises.toList(),
        restTimer = RestTimer(endsAtEpochMs = 1_090_000L, durationS = 90),
    )

    private val details = WorkoutDetails(title = " Push day ", notes = " felt strong ", startedAtEpochMs = 900_000L, durationS = 3_000)

    private suspend fun stored(): Workout = repo.observe("w").first()!!

    @Test
    fun `uncompleted sets are counted for the confirmation`() {
        val workout = draft(exercise("a", set("a1", true), set("a2", false)), exercise("b", set("b1", false)))

        assertEquals(2, workout.uncompletedSetCount())
        assertEquals(true, workout.hasCompletedSet())
        assertEquals(false, draft(exercise("b", set("b1", false))).hasCompletedSet())
        assertEquals(false, draft().hasCompletedSet())
    }

    @Test
    fun `finishing keeps only completed sets and drops exercises left empty (D-011)`() = runTest {
        repo.save(draft(exercise("a", set("a1", true), set("a2", false), set("a3", true)), exercise("b", set("b1", false))))

        val result = finish("w", details)

        val finished = assertIs<FinishWorkoutResult.Finished>(result).workout
        assertEquals(listOf(exercise("a", set("a1", true), set("a3", true))), finished.exercises)
        assertEquals(finished, stored())
    }

    @Test
    fun `finishing applies the details, recomputes the offset for the new start, and clears the rest`() = runTest {
        repo.save(draft(exercise("a", set("a1", true))))

        val finished = (finish("w", details) as FinishWorkoutResult.Finished).workout

        assertEquals("Push day", finished.title)
        assertEquals("felt strong", finished.notes)
        assertEquals(900_000L, finished.startedAtEpochMs)
        assertEquals(900_000L + 3_000_000L, finished.finishedAtEpochMs)
        assertEquals(7_200, finished.utcOffsetS)
        assertEquals(5_000_000L, finished.updatedAtEpochMs)
        assertNull(finished.restTimer)
    }

    @Test
    fun `a blank note is stored as none`() = runTest {
        repo.save(draft(exercise("a", set("a1", true))))

        val finished = (finish("w", details.copy(notes = "  ")) as FinishWorkoutResult.Finished).workout

        assertNull(finished.notes)
    }

    @Test
    fun `with no completed set nothing is written and the result asks about discarding`() = runTest {
        val workout = draft(exercise("a", set("a1", false)))
        repo.save(workout)
        val writes = repo.writes

        val result = finish("w", details)

        assertEquals(FinishWorkoutResult.NothingCompleted, result)
        assertEquals(writes, repo.writes)
        assertEquals(workout, stored())
    }

    @Test
    fun `saving an edited finished workout uses the same rules`() = runTest {
        repo.save(draft(exercise("a", set("a1", true), set("a2", false)), finishedAt = 4_000_000L))

        val finished = (finish("w", details) as FinishWorkoutResult.Finished).workout

        assertEquals(listOf(set("a1", true)), finished.exercises.single().sets)
        assertEquals(3_900_000L, finished.finishedAtEpochMs)
    }

    @Test
    fun `finishing an unknown workout returns null`() = runTest {
        assertNull(finish("missing", details))
    }

    @Test
    fun `delete hides the workout from history and restore brings it back (TC-DATA-03)`() = runTest {
        repo.save(draft(exercise("a", set("a1", true)), finishedAt = 4_000_000L))

        delete("w")
        assertEquals(5_000_000L, stored().deletedAtEpochMs)
        assertEquals(emptyList(), repo.observeHistory().first())

        restore("w")
        assertNull(stored().deletedAtEpochMs)
        assertEquals(listOf("w"), repo.observeHistory().first().map { it.id })
    }

    @Test
    fun `discarding the live draft is a delete, so no draft is left`() = runTest {
        repo.save(draft(exercise("a", set("a1", false))))

        delete("w")

        assertNull(repo.observeActiveDraft().first())
    }

    @Test
    fun `a past workout starts finished at the chosen time, one hour long, and empty`() = runTest {
        val workout = startPast(startedAtEpochMs = 100_000L)

        val expected = Workout(
            id = "id0",
            title = "",
            notes = null,
            startedAtEpochMs = 100_000L,
            utcOffsetS = 7_200,
            finishedAtEpochMs = 100_000L + 3_600_000L,
            createdAtEpochMs = 5_000_000L,
            updatedAtEpochMs = 5_000_000L,
            deletedAtEpochMs = null,
            exercises = emptyList(),
        )
        assertEquals(expected, workout)
        assertEquals(expected, repo.observe("id0").first())
        assertNull(repo.observeActiveDraft().first())
    }

    @Test
    fun `previous values while editing come from the last session before the edited workout`() = runTest {
        fun record(workoutId: String, startedAt: Long, weightKg: Double) = WorkoutExerciseRecord(
            workoutId = workoutId,
            workoutStartedAtEpochMs = startedAt,
            workoutFinishedAtEpochMs = startedAt + 1,
            workoutDeletedAtEpochMs = null,
            positionInWorkout = 0,
            sets = listOf(SetData(weightKg = weightKg, reps = 5, completed = true)),
        )
        repo.history = mapOf(
            "t-a" to listOf(record("before", 500_000L, 80.0), record("w", 1_000_000L, 90.0), record("after", 2_000_000L, 100.0)),
        )
        val edited = draft(exercise("a", set("a1", true)), finishedAt = 4_000_000L)

        val previous = PreviousSetsBeforeUseCase(repo)("t-a", edited)

        assertEquals(listOf(SetData(weightKg = 80.0, reps = 5, completed = true)), previous)
    }

    @Test
    fun `ticking a set in a past workout completes it without touching the rest timer`() = runTest {
        repo.save(draft(exercise("a", set("a1", false)), finishedAt = 4_000_000L).copy(restTimer = null))

        markCompleted("w", "a1")

        val set = stored().exercises.single().sets.single()
        assertEquals(true, set.data.completed)
        assertEquals(5_000_000L, set.completedAtEpochMs)
        assertNull(stored().restTimer)
    }
}
