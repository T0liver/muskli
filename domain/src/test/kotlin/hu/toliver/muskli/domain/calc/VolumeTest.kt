package hu.toliver.muskli.domain.calc

import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VolumeTest {
    private fun done(weightKg: Double?, reps: Int?, type: SetType = SetType.NORMAL, side: Side = Side.BOTH) =
        SetData(type = type, weightKg = weightKg, reps = reps, completed = true, side = side)

    private fun exercise(type: ExerciseType, vararg sets: SetData, unilateral: Boolean = false) =
        ExerciseSets(type = type, sets = sets.toList(), unilateral = unilateral)

    private val warmUpAndTwoWorkSets = exercise(
        ExerciseType.WEIGHT_REPS,
        done(40.0, 10, SetType.WARMUP),
        done(100.0, 5),
        done(100.0, 5),
    )

    @Test
    fun `TC-VOL-01 warm-ups are excluded by default`() {
        assertEquals(1000.0, sessionVolumeKg(listOf(warmUpAndTwoWorkSets), bodyweightKg = null))
    }

    @Test
    fun `TC-VOL-02 warm-ups are counted when included`() {
        assertEquals(
            1400.0,
            sessionVolumeKg(listOf(warmUpAndTwoWorkSets), bodyweightKg = null, includeWarmUps = true),
        )
    }

    @Test
    fun `TC-VOL-03 uncompleted sets do not count`() {
        val sets = exercise(
            ExerciseType.WEIGHT_REPS,
            done(100.0, 5),
            SetData(weightKg = 100.0, reps = 5, completed = false),
        )
        assertEquals(500.0, sessionVolumeKg(listOf(sets), bodyweightKg = null))
    }

    @Test
    fun `TC-VOL-04 null weight or null reps gives 0`() {
        assertEquals(0.0, setVolumeKg(done(null, 5), ExerciseType.WEIGHT_REPS, bodyweightKg = null))
        assertEquals(0.0, setVolumeKg(done(100.0, null), ExerciseType.WEIGHT_REPS, bodyweightKg = null))
        assertEquals(
            0.0,
            sessionVolumeKg(
                listOf(exercise(ExerciseType.WEIGHT_REPS, done(null, 5), done(100.0, null))),
                bodyweightKg = null,
            ),
        )
    }

    @Test
    fun `TC-VOL-05 reps-only exercise has no volume`() {
        assertEquals(0.0, setVolumeKg(done(null, 20), ExerciseType.REPS_ONLY, bodyweightKg = 80.0))
        assertEquals(0.0, setVolumeKg(done(10.0, 20), ExerciseType.REPS_ONLY, bodyweightKg = 80.0))
    }

    @Test
    fun `TC-VOL-06 bodyweight 80 kg with no added weight for 10 reps is 800`() {
        assertEquals(800.0, setVolumeKg(done(0.0, 10), ExerciseType.BODYWEIGHT_REPS, bodyweightKg = 80.0))
        assertEquals(800.0, setVolumeKg(done(null, 10), ExerciseType.BODYWEIGHT_REPS, bodyweightKg = 80.0))
    }

    @Test
    fun `TC-VOL-07 bodyweight 80 kg plus 10 kg added for 8 reps is 720`() {
        assertEquals(720.0, setVolumeKg(done(10.0, 8), ExerciseType.BODYWEIGHT_REPS, bodyweightKg = 80.0))
    }

    @Test
    fun `TC-VOL-08 assisted 80 kg body weight minus 20 kg assistance for 5 reps is 300`() {
        assertEquals(
            300.0,
            setVolumeKg(done(20.0, 5), ExerciseType.BODYWEIGHT_ASSISTED_REPS, bodyweightKg = 80.0),
        )
    }

    @Test
    fun `TC-VOL-08 assistance at or above body weight is clamped to 0`() {
        assertEquals(
            0.0,
            setVolumeKg(done(90.0, 5), ExerciseType.BODYWEIGHT_ASSISTED_REPS, bodyweightKg = 80.0),
        )
    }

    @Test
    fun `TC-VOL-09 unilateral set on both sides counts twice`() {
        assertEquals(
            400.0,
            setVolumeKg(done(20.0, 10, side = Side.BOTH), ExerciseType.WEIGHT_REPS, null, unilateral = true),
        )
    }

    @Test
    fun `TC-VOL-09 unilateral set on one side counts once`() {
        assertEquals(
            200.0,
            setVolumeKg(done(20.0, 10, side = Side.LEFT), ExerciseType.WEIGHT_REPS, null, unilateral = true),
        )
        assertEquals(
            200.0,
            setVolumeKg(done(20.0, 10, side = Side.RIGHT), ExerciseType.WEIGHT_REPS, null, unilateral = true),
        )
    }

    @Test
    fun `TC-VOL-09 default side both on a bilateral exercise is not doubled`() {
        assertEquals(200.0, setVolumeKg(done(20.0, 10), ExerciseType.WEIGHT_REPS, bodyweightKg = null))
    }

    @Test
    fun `TC-VOL-10 unset body weight gives 0 for bodyweight and assisted`() {
        assertEquals(0.0, setVolumeKg(done(10.0, 8), ExerciseType.BODYWEIGHT_REPS, bodyweightKg = null))
        assertEquals(
            0.0,
            setVolumeKg(done(20.0, 5), ExerciseType.BODYWEIGHT_ASSISTED_REPS, bodyweightKg = null),
        )
        assertEquals(
            0.0,
            sessionVolumeKg(
                listOf(exercise(ExerciseType.BODYWEIGHT_REPS, done(0.0, 10))),
                bodyweightKg = null,
            ),
        )
    }
}
