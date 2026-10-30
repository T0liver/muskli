package hu.toliver.muskli.domain.settings

import hu.toliver.muskli.domain.calc.PreviousMode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsTest {
    @Test
    fun `defaults match D-009 and D-017`() {
        val defaults = Settings()

        assertEquals(WeightUnit.KG, defaults.weightUnit)
        assertEquals(90, defaults.defaultRestSeconds)
        assertEquals(PreviousMode.ANYWHERE, defaults.previousMode)
        assertTrue(defaults.keepScreenAwake)
        assertTrue(defaults.soundEnabled)
        assertTrue(defaults.vibrationEnabled)
        assertEquals(WeekStartDay.MONDAY, defaults.weekStart)
        assertNull(defaults.bodyWeightKg)
    }

    @Test
    fun `week start offers all seven days starting Monday`() {
        assertEquals(7, WeekStartDay.entries.size)
        assertEquals(WeekStartDay.MONDAY, WeekStartDay.entries.first())
    }
}
