package hu.toliver.muskli.domain.calc

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UnitsTest {
    @Test
    fun `TC-UNIT-01 100 kg is 220_462262 lb`() {
        assertEquals(220.462262, kgToLb(100.0), 1e-9)
    }

    @Test
    fun `TC-UNIT-02 225 lb is 102_0583 kg`() {
        assertEquals(102.0583, lbToKg(225.0), 0.00005)
    }

    @Test
    fun `TC-UNIT-03 45 lb is 20_4117 kg`() {
        assertEquals(20.4117, lbToKg(45.0), 0.00005)
    }

    @Test
    fun `TC-UNIT-04 82_5 kg survives a kg to lb to kg round trip`() {
        assertEquals(82.5, lbToKg(kgToLb(82.5)), 1e-9)
    }
}
