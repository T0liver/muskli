package hu.toliver.muskli.domain.calc

import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RestTimerTest {
    private val completed = SetData(weightKg = 100.0, reps = 5, completed = true)
    private val nextNormal = SetData(type = SetType.NORMAL, weightKg = 100.0, reps = 5)

    @Test
    fun `TC-REST-01 exercise rest 90 before a normal set starts 90`() {
        assertEquals(RestEffect.Start(90), restTimerEffect(completed, nextNormal, exerciseRestS = 90, defaultRestS = 120))
    }

    @Test
    fun `TC-REST-02 no timer when the next set is a drop set`() {
        val nextDrop = nextNormal.copy(type = SetType.DROPSET)
        assertEquals(RestEffect.None, restTimerEffect(completed, nextDrop, exerciseRestS = 90, defaultRestS = 120))
    }

    @Test
    fun `TC-REST-03 unset exercise rest falls back to the default`() {
        assertEquals(
            RestEffect.Start(120),
            restTimerEffect(completed, nextNormal, exerciseRestS = null, defaultRestS = 120),
        )
    }

    @Test
    fun `TC-REST-04 exercise rest 0 is off even with a default`() {
        assertEquals(RestEffect.None, restTimerEffect(completed, nextNormal, exerciseRestS = 0, defaultRestS = 120))
    }

    @Test
    fun `TC-REST-05 no exercise rest and no default gives no timer`() {
        assertEquals(
            RestEffect.None,
            restTimerEffect(completed, nextNormal, exerciseRestS = null, defaultRestS = null),
        )
    }

    @Test
    fun `TC-REST-06 last set of an exercise uses the completed exercise's rest`() {
        val otherExerciseFirstSet = SetData(type = SetType.NORMAL, weightKg = 40.0, reps = 12)
        assertEquals(
            RestEffect.Start(150),
            restTimerEffect(completed, otherExerciseFirstSet, exerciseRestS = 150, defaultRestS = 90),
        )
    }

    @Test
    fun `TC-REST-07 last set of the whole workout gives no timer`() {
        assertEquals(RestEffect.None, restTimerEffect(completed, next = null, exerciseRestS = 90, defaultRestS = 120))
    }

    @Test
    fun `TC-REST-08 superset uses the rest of the exercise just completed`() {
        val supersetBNext = SetData(type = SetType.NORMAL, weightKg = 30.0, reps = 10)
        val exerciseARestS = 90
        assertEquals(
            RestEffect.Start(90),
            restTimerEffect(completed, supersetBNext, exerciseRestS = exerciseARestS, defaultRestS = 120),
        )
    }

    @Test
    fun `a set that is not completed starts no timer`() {
        assertEquals(
            RestEffect.None,
            restTimerEffect(completed.copy(completed = false), nextNormal, exerciseRestS = 90, defaultRestS = 120),
        )
    }
}
