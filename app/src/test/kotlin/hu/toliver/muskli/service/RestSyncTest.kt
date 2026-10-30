package hu.toliver.muskli.service

import hu.toliver.muskli.domain.model.RestTimer
import org.junit.Assert.assertEquals
import org.junit.Test

class RestSyncTest {
    private val timer = RestTimer(endsAtEpochMs = 100_000L, durationS = 90)

    @Test
    fun aRunningRestArmsTheAlarmAndService() {
        assertEquals(RestSync.Arm("w1", timer), restSync("w1", timer, nowEpochMs = 99_999L))
    }

    @Test
    fun noRestOrNoDraftDisarms() {
        assertEquals(RestSync.Disarm, restSync("w1", timer = null, nowEpochMs = 0L))
        assertEquals(RestSync.Disarm, restSync(workoutId = null, timer = null, nowEpochMs = 0L))
    }

    @Test
    fun aRestThatIsOverButStillStoredIsLeftForTheFiringAlarm() {
        assertEquals(RestSync.Leave, restSync("w1", timer, nowEpochMs = 100_000L))
        assertEquals(RestSync.Leave, restSync("w1", timer, nowEpochMs = 500_000L))
    }
}
