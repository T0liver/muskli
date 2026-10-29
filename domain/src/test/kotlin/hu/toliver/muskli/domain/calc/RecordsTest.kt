package hu.toliver.muskli.domain.calc

import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordsTest {
    private val epley = OneRmFormula.EPLEY

    private fun set(weightKg: Double?, reps: Int?, type: SetType = SetType.NORMAL, durationS: Int? = null) =
        SetData(type = type, weightKg = weightKg, reps = reps, durationS = durationS, completed = true)

    private fun history(workoutId: String, at: Long, vararg sets: SetData) =
        sets.map { HistorySet(workoutId = workoutId, performedAtEpochMs = at, set = it) }

    private val best100x5 = history("w1", 1_000, set(100.0, 5), set(90.0, 5))

    @Test
    fun `TC-PR-01 100x6 over a 100x5 best is a 1RM and reps PR`() {
        val broken = recordsBrokenBy(set(100.0, 6), best100x5, ExerciseType.WEIGHT_REPS, epley)

        assertTrue(RecordType.ESTIMATED_ONE_REP_MAX in broken)
        assertTrue(RecordType.MOST_REPS in broken)
        assertTrue(RecordType.HEAVIEST_WEIGHT !in broken)
    }

    @Test
    fun `TC-PR-01 100x6 sets a new best weight at 6 or more reps`() {
        assertEquals(null, setRecords(best100x5)[6])
        assertEquals(100.0, setRecords(best100x5 + history("w2", 2_000, set(100.0, 6)))[6])
    }

    @Test
    fun `TC-PR-02 105x3 is a heaviest weight PR but not a 1RM PR`() {
        val broken = recordsBrokenBy(set(105.0, 3), best100x5, ExerciseType.WEIGHT_REPS, epley)

        assertEquals(setOf(RecordType.HEAVIEST_WEIGHT), broken)
    }

    @Test
    fun `TC-PR-03 first-ever set breaks every category it qualifies for`() {
        assertEquals(
            setOf(
                RecordType.HEAVIEST_WEIGHT,
                RecordType.ESTIMATED_ONE_REP_MAX,
                RecordType.MOST_REPS,
                RecordType.BEST_SET_VOLUME,
            ),
            recordsBrokenBy(set(60.0, 8), emptyList(), ExerciseType.WEIGHT_REPS, epley),
        )
        assertEquals(
            setOf(RecordType.MOST_REPS),
            recordsBrokenBy(set(null, 12), emptyList(), ExerciseType.BODYWEIGHT_REPS, epley),
        )
        assertEquals(
            setOf(RecordType.BEST_DURATION),
            recordsBrokenBy(set(null, null, durationS = 60), emptyList(), ExerciseType.DURATION, epley),
        )
    }

    @Test
    fun `TC-PR-04 a set equal to the existing best is not a PR`() {
        assertEquals(
            emptySet(),
            recordsBrokenBy(set(100.0, 5), best100x5, ExerciseType.WEIGHT_REPS, epley),
        )
    }

    @Test
    fun `TC-PR-05 removing the workout that holds the PR restores the previous record`() {
        val withPr = best100x5 + history("w2", 2_000, set(110.0, 5))
        assertEquals(
            Record(RecordType.HEAVIEST_WEIGHT, 110.0, "w2"),
            currentRecords(withPr, ExerciseType.WEIGHT_REPS, epley)[RecordType.HEAVIEST_WEIGHT],
        )

        val afterDelete = withPr.filter { it.workoutId != "w2" }
        assertEquals(
            Record(RecordType.HEAVIEST_WEIGHT, 100.0, "w1"),
            currentRecords(afterDelete, ExerciseType.WEIGHT_REPS, epley)[RecordType.HEAVIEST_WEIGHT],
        )
        assertEquals(
            setOf(RecordType.HEAVIEST_WEIGHT, RecordType.ESTIMATED_ONE_REP_MAX, RecordType.BEST_SET_VOLUME),
            recordsBrokenBy(set(105.0, 5), afterDelete, ExerciseType.WEIGHT_REPS, epley),
        )
    }

    @Test
    fun `TC-PR-05 an equal later set does not take over the record`() {
        val tied = best100x5 + history("w2", 2_000, set(100.0, 5))
        assertEquals(
            "w1",
            currentRecords(tied, ExerciseType.WEIGHT_REPS, epley).getValue(RecordType.HEAVIEST_WEIGHT).workoutId,
        )
    }

    @Test
    fun `TC-PR-06 warm-up sets never produce PRs`() {
        assertEquals(
            emptySet(),
            recordsBrokenBy(set(200.0, 20, SetType.WARMUP), best100x5, ExerciseType.WEIGHT_REPS, epley),
        )
        val warmUpHistory = history("w1", 1_000, set(200.0, 20, SetType.WARMUP))
        assertEquals(emptyMap(), currentRecords(warmUpHistory, ExerciseType.WEIGHT_REPS, epley))
        assertEquals(emptyMap(), setRecords(warmUpHistory))
    }

    @Test
    fun `TC-PR-07 set records hold the best weight at N or more reps`() {
        val records = setRecords(history("w1", 1_000, set(80.0, 5), set(100.0, 8), set(100.0, 3)))

        assertEquals(100.0, records[3])
        assertEquals(100.0, records[5])
        assertEquals(100.0, records[8])
        assertEquals((1..8).toSet(), records.keys)
    }

    @Test
    fun `uncompleted sets in history are ignored`() {
        val history = history("w1", 1_000, SetData(weightKg = 150.0, reps = 5, completed = false))
        assertEquals(emptyMap(), currentRecords(history, ExerciseType.WEIGHT_REPS, epley))
    }
}
