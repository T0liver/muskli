package hu.toliver.muskli.domain.calc

import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreviousValuesTest {
    private fun set(weightKg: Double, reps: Int, type: SetType = SetType.NORMAL, completed: Boolean = true) =
        SetData(type = type, weightKg = weightKg, reps = reps, completed = completed)

    private fun record(
        workoutId: String,
        startedAt: Long,
        vararg sets: SetData,
        position: Int = 0,
        finished: Boolean = true,
        deleted: Boolean = false,
        routineId: String? = null,
    ) = WorkoutExerciseRecord(
        workoutId = workoutId,
        workoutStartedAtEpochMs = startedAt,
        workoutFinishedAtEpochMs = if (finished) startedAt + 3_600_000 else null,
        workoutDeletedAtEpochMs = if (deleted) startedAt + 7_200_000 else null,
        positionInWorkout = position,
        sets = sets.toList(),
        routineId = routineId,
    )

    private fun anywhere(history: List<WorkoutExerciseRecord>, setIndex: Int) =
        previousFor(history, setIndex, PreviousMode.ANYWHERE, routineId = null)

    @Test
    fun `TC-PREV-01 returns the set at the same index from the last session`() {
        val history = listOf(
            record("old", 1_000, set(50.0, 10), set(70.0, 8)),
            record("last", 2_000, set(60.0, 10), set(80.0, 8)),
        )
        assertEquals(set(80.0, 8), anywhere(history, 1))
    }

    @Test
    fun `TC-PREV-02 index beyond the last session's set count is null`() {
        val history = listOf(
            record("old", 1_000, set(50.0, 10), set(70.0, 8), set(70.0, 8)),
            record("last", 2_000, set(60.0, 10), set(80.0, 8)),
        )
        assertNull(anywhere(history, 2))
    }

    private fun inRoutine(history: List<WorkoutExerciseRecord>, setIndex: Int, routineId: String?) =
        previousFor(history, setIndex, PreviousMode.ROUTINE_SCOPED, routineId)

    private val routinesAThenB = listOf(
        record("a", 1_000, set(80.0, 8), routineId = "routine-a"),
        record("b", 2_000, set(85.0, 6), routineId = "routine-b"),
    )

    @Test
    fun `TC-PREV-03 routine mode returns the last workout of the same routine`() {
        assertEquals(set(80.0, 8), inRoutine(routinesAThenB, 0, "routine-a"))
        assertEquals(set(85.0, 6), inRoutine(routinesAThenB, 0, "routine-b"))
    }

    @Test
    fun `TC-PREV-03 anywhere mode returns the last workout whatever the routine`() {
        assertEquals(set(85.0, 6), previousFor(routinesAThenB, 0, PreviousMode.ANYWHERE, "routine-a"))
        assertEquals(set(85.0, 6), previousFor(routinesAThenB, 0, PreviousMode.ANYWHERE, "routine-b"))
    }

    @Test
    fun `routine mode without a routine falls back to last time anywhere (D-038)`() {
        val history = routinesAThenB + record("free", 3_000, set(90.0, 5))
        assertEquals(set(90.0, 5), inRoutine(history, 0, routineId = null))
    }

    @Test
    fun `routine mode is null when the exercise was never done in that routine (D-053)`() {
        assertNull(inRoutine(routinesAThenB, 0, "routine-c"))
        assertEquals(emptyList(), previousSets(routinesAThenB, PreviousMode.ROUTINE_SCOPED, "routine-c"))
    }

    @Test
    fun `routine mode applies the same draft, delete, and first-occurrence rules`() {
        val history = listOf(
            record("a1", 1_000, set(70.0, 8), routineId = "routine-a"),
            record("a2", 2_000, set(60.0, 12), position = 3, routineId = "routine-a"),
            record("a2", 2_000, set(80.0, 8), position = 0, routineId = "routine-a"),
            record("a3", 3_000, set(90.0, 8), finished = false, routineId = "routine-a"),
            record("a4", 4_000, set(95.0, 8), deleted = true, routineId = "routine-a"),
        )
        assertEquals(set(80.0, 8), inRoutine(history, 0, "routine-a"))
    }

    @Test
    fun `TC-PREV-04 no history is null`() {
        assertNull(anywhere(emptyList(), 0))
    }

    @Test
    fun `TC-PREV-05 aligns by raw index with warm-ups included`() {
        val history = listOf(record("last", 2_000, set(40.0, 10, SetType.WARMUP), set(100.0, 5)))
        assertEquals(set(40.0, 10, SetType.WARMUP), anywhere(history, 0))
        assertEquals(set(100.0, 5), anywhere(history, 1))
    }

    @Test
    fun `TC-PREV-06 drafts are ignored`() {
        val history = listOf(
            record("finished", 1_000, set(80.0, 8)),
            record("draft", 2_000, set(90.0, 8), finished = false),
        )
        assertEquals(set(80.0, 8), anywhere(history, 0))
    }

    @Test
    fun `TC-PREV-06 soft-deleted workouts are ignored`() {
        val history = listOf(
            record("finished", 1_000, set(80.0, 8)),
            record("deleted", 2_000, set(90.0, 8), deleted = true),
        )
        assertEquals(set(80.0, 8), anywhere(history, 0))
    }

    @Test
    fun `TC-PREV-06 an uncompleted set keeps its index and is not returned`() {
        val history = listOf(
            record("last", 2_000, set(40.0, 10, SetType.WARMUP), set(70.0, 8, completed = false), set(100.0, 5)),
        )
        assertEquals(set(40.0, 10, SetType.WARMUP), anywhere(history, 0))
        assertNull(anywhere(history, 1))
        assertEquals(set(100.0, 5), anywhere(history, 2))
    }

    @Test
    fun `TC-PREV-07 an exercise done twice in a workout uses the first occurrence`() {
        val history = listOf(
            record("last", 2_000, set(60.0, 12), position = 4),
            record("last", 2_000, set(80.0, 8), position = 1),
        )
        assertEquals(set(80.0, 8), anywhere(history, 0))
    }

    private fun allAnywhere(history: List<WorkoutExerciseRecord>) =
        previousSets(history, PreviousMode.ANYWHERE, routineId = null)

    @Test
    fun `previousSets lists the last session by raw index, warm-ups included`() {
        val history = listOf(
            record("old", 1_000, set(50.0, 10), set(70.0, 8), set(70.0, 8)),
            record("last", 2_000, set(40.0, 10, SetType.WARMUP), set(100.0, 5)),
        )
        assertEquals(listOf(set(40.0, 10, SetType.WARMUP), set(100.0, 5)), allAnywhere(history))
    }

    @Test
    fun `previousSets keeps an uncompleted slot as null and is empty without history`() {
        val history = listOf(record("last", 2_000, set(60.0, 10), set(70.0, 8, completed = false), set(80.0, 8)))
        assertEquals(listOf(set(60.0, 10), null, set(80.0, 8)), allAnywhere(history))
        assertEquals(emptyList(), allAnywhere(emptyList()))
    }

    @Test
    fun `previousSets ignores drafts and uses the first occurrence`() {
        val history = listOf(
            record("last", 2_000, set(60.0, 12), position = 4),
            record("last", 2_000, set(80.0, 8), position = 1),
            record("draft", 3_000, set(90.0, 8), finished = false),
        )
        assertEquals(listOf(set(80.0, 8)), allAnywhere(history))
    }
}
