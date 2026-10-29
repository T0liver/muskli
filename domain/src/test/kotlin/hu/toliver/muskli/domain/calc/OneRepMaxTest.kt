package hu.toliver.muskli.domain.calc

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OneRepMaxTest {
    private fun assertOneRm(expected: Double, weightKg: Double, reps: Int, formula: OneRmFormula) {
        val actual = estimateOneRepMax(weightKg, reps, formula)
        assertNotNull(actual)
        assertEquals(expected, actual, 0.01)
    }

    @Test
    fun `TC-1RM-01 epley 100x1 is 100`() = assertOneRm(100.0, 100.0, 1, OneRmFormula.EPLEY)

    @Test
    fun `TC-1RM-02 epley 100x10 is 133_33`() = assertOneRm(133.33, 100.0, 10, OneRmFormula.EPLEY)

    @Test
    fun `TC-1RM-03 epley 100x6 is 120`() = assertOneRm(120.0, 100.0, 6, OneRmFormula.EPLEY)

    @Test
    fun `TC-1RM-04 epley 60x5 is 70`() = assertOneRm(70.0, 60.0, 5, OneRmFormula.EPLEY)

    @Test
    fun `TC-1RM-05 brzycki 100x1 is 100`() = assertOneRm(100.0, 100.0, 1, OneRmFormula.BRZYCKI)

    @Test
    fun `TC-1RM-06 brzycki 100x10 is 133_33`() = assertOneRm(133.33, 100.0, 10, OneRmFormula.BRZYCKI)

    @Test
    fun `TC-1RM-07 brzycki 100x6 is 116_13`() = assertOneRm(116.13, 100.0, 6, OneRmFormula.BRZYCKI)

    @Test
    fun `TC-1RM-08 brzycki 60x5 is 67_50`() = assertOneRm(67.50, 60.0, 5, OneRmFormula.BRZYCKI)

    @Test
    fun `TC-1RM-09 zero reps is null for every formula`() {
        OneRmFormula.entries.forEach { assertNull(estimateOneRepMax(100.0, 0, it)) }
    }

    @Test
    fun `TC-1RM-10 brzycki at 37 reps is null instead of dividing by zero`() {
        assertNull(estimateOneRepMax(100.0, 37, OneRmFormula.BRZYCKI))
    }

    @Test
    fun `TC-1RM-11 zero or negative weight is null for every formula`() {
        OneRmFormula.entries.forEach {
            assertNull(estimateOneRepMax(0.0, 5, it))
            assertNull(estimateOneRepMax(-10.0, 5, it))
        }
    }
}
