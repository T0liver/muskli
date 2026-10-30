package hu.toliver.muskli.ui.workout

import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.SetColumn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetFieldsTest {
    private fun parse(column: SetColumn, text: String, unit: WeightUnit = WeightUnit.KG) =
        column.parseInto(SetData(), text, unit)

    @Test
    fun weightsKeepQuarterKiloStepsAndDropTrailingZeros() {
        assertEquals("81.25", SetColumn.WEIGHT.format(SetData(weightKg = 81.25), WeightUnit.KG))
        assertEquals("100", SetColumn.WEIGHT.format(SetData(weightKg = 100.0), WeightUnit.KG))
        assertEquals("220.46", SetColumn.WEIGHT.format(SetData(weightKg = 100.0), WeightUnit.LB))
    }

    @Test
    fun poundsRoundTripThroughKg() {
        val stored = parse(SetColumn.WEIGHT, "225", WeightUnit.LB)
        assertEquals("225", SetColumn.WEIGHT.format(stored, WeightUnit.LB))
    }

    @Test
    fun unparsableOrNegativeInputClearsTheValue() {
        assertNull(parse(SetColumn.WEIGHT, "abc").weightKg)
        assertNull(parse(SetColumn.WEIGHT, "-5").weightKg)
        assertNull(parse(SetColumn.REPS, "").reps)
    }

    @Test
    fun durationAcceptsColonsOrTimerStyleDigits() {
        assertEquals(45, parse(SetColumn.DURATION, "45").durationS)
        assertEquals(90, parse(SetColumn.DURATION, "130").durationS)
        assertEquals(90, parse(SetColumn.DURATION, "1:30").durationS)
        assertEquals(3_723, parse(SetColumn.DURATION, "1:02:03").durationS)
        assertEquals("1:30", SetColumn.DURATION.format(SetData(durationS = 90), WeightUnit.KG))
        assertEquals("1:02:03", SetColumn.DURATION.format(SetData(durationS = 3_723), WeightUnit.KG))
    }

    @Test
    fun distanceIsKilometresForCardioAndMetresForShortDistance() {
        assertEquals(5_000.0, parse(SetColumn.DISTANCE_KM, "5").distanceM)
        assertEquals("2.5", SetColumn.DISTANCE_KM.format(SetData(distanceM = 2_500.0), WeightUnit.KG))
        assertEquals(40.0, parse(SetColumn.DISTANCE_M, "40").distanceM)
    }
}
