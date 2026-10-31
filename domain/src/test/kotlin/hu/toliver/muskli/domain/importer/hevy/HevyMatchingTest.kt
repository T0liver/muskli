package hu.toliver.muskli.domain.importer.hevy

import hu.toliver.muskli.domain.TestClock
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.SetData
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HevyMatchingTest {
    private val fixtureTitles: Set<String> by lazy {
        val text = requireNotNull(javaClass.getResource("/hevy/workout_data.csv")).readText()
        val workouts = assertIs<HevyParseResult.Workouts>(parseHevyExport(text, TestClock(offsetS = 3600))).workouts
        workouts.flatMap { w -> w.exercises.map { it.title } }.toSet()
    }

    @Test
    fun `TC-HEVY-11 fixture titles against catalogue version 1`() {
        assertEquals(
            mapOf(
                "Plank" to HevyMatch.Exact("plank"),
                "Rowing Machine" to HevyMatch.Exact("rowing-machine"),
                "Bench Press (Barbell)" to HevyMatch.Normalised("barbell-bench-press"),
                "Pull Up" to HevyMatch.Normalised("pull-up"),
                "Farmers Walk (Dumbbell)" to HevyMatch.Normalised("dumbbell-farmers-walk"),
                "Lateral Raise (Dumbbell)" to HevyMatch.Unmatched(emptyList()),
                "Goblet Squat (Kettlebell)" to HevyMatch.Unmatched(emptyList()),
                "Assisted Pull Up" to HevyMatch.Unmatched(emptyList()),
            ),
            matchHevyExercises(fixtureTitles, catalogueV1),
        )
    }

    @Test
    fun `TC-HEVY-12 two non-archived templates with the same tokens leave the title unmatched with both`() {
        val templates = listOf(
            template("a", "Seated Cable Row"),
            template("b", "Cable Row (Seated)", isCustom = true),
        )

        assertEquals(
            mapOf("Row Seated Cable" to HevyMatch.Unmatched(listOf("a", "b"))),
            matchHevyExercises(listOf("Row Seated Cable"), templates),
        )
    }

    @Test
    fun `TC-HEVY-12 an archived and a non-archived template with the same tokens match the non-archived one`() {
        val templates = listOf(
            template("old", "Seated Cable Row", archived = true),
            template("new", "Cable Row (Seated)", isCustom = true),
        )

        assertEquals(
            mapOf("Row Seated Cable" to HevyMatch.Normalised("new")),
            matchHevyExercises(listOf("Row Seated Cable"), templates),
        )
    }

    @Test
    fun `TC-HEVY-12 a tie in the exact step is not broken by the normalised step`() {
        val templates = listOf(
            template("a", "Row"),
            template("b", "row", isCustom = true),
            template("c", "Row."),
        )

        assertEquals(mapOf("ROW" to HevyMatch.Unmatched(listOf("a", "b"))), matchHevyExercises(listOf("ROW"), templates))
    }

    @Test
    fun `TC-HEVY-12 a single archived exact match still matches`() {
        val templates = listOf(template("old", "Pull-Up", archived = true))

        assertEquals(mapOf("pull-up" to HevyMatch.Exact("old")), matchHevyExercises(listOf("pull-up"), templates))
    }

    @Test
    fun `TC-HEVY-13 an exact custom title wins over a normalised catalogue title`() {
        val templates = catalogueV1 + template("custom-1", "Pull Up", isCustom = true)

        assertEquals(mapOf("pull up" to HevyMatch.Exact("custom-1")), matchHevyExercises(listOf("pull up"), templates))
    }

    @Test
    fun `default resolutions use matched templates and create customs for the rest`() {
        val matches = mapOf(
            "A" to HevyMatch.Exact("a"),
            "B" to HevyMatch.Normalised("b"),
            "C" to HevyMatch.Unmatched(listOf("x", "y")),
            "D" to HevyMatch.Unmatched(emptyList()),
        )

        assertEquals(
            mapOf(
                "A" to HevyResolution.UseTemplate("a"),
                "B" to HevyResolution.UseTemplate("b"),
                "C" to HevyResolution.CreateCustom,
                "D" to HevyResolution.CreateCustom,
            ),
            defaultResolutions(matches),
        )
    }

    private val weight = SetData(weightKg = 20.0, reps = 5)

    @Test
    fun `TC-HEVY-14 type inference follows rules 1 to 8 in order`() {
        val cases = listOf(
            listOf(SetData(weightKg = 32.0), SetData(distanceM = 40.0)) to ExerciseType.SHORT_DISTANCE_WEIGHT,
            listOf(SetData(distanceM = 5000.0, durationS = 1200)) to ExerciseType.DISTANCE_DURATION,
            listOf(SetData(weightKg = 10.0, durationS = 60)) to ExerciseType.WEIGHT_DURATION,
            listOf(SetData(durationS = 60)) to ExerciseType.DURATION,
            listOf(SetData(weightKg = 20.0, reps = 6)) to ExerciseType.WEIGHT_REPS,
            listOf(SetData(reps = 8)) to ExerciseType.REPS_ONLY,
            listOf(SetData()) to ExerciseType.WEIGHT_REPS,
            emptyList<SetData>() to ExerciseType.WEIGHT_REPS,
        )
        for ((sets, type) in cases) {
            assertEquals(type, inferCustomExercise("Some Exercise", sets).first, sets.toString())
        }
        assertEquals(ExerciseType.BODYWEIGHT_ASSISTED_REPS, inferCustomExercise("Assisted Dip", listOf(SetData(weightKg = 20.0, reps = 6))).first)
        assertEquals(ExerciseType.REPS_ONLY, inferCustomExercise("Assisted Dip", listOf(SetData(reps = 6))).first)
        assertEquals(ExerciseType.WEIGHT_REPS, inferCustomExercise("Unassisted Dip", listOf(SetData(weightKg = 20.0, reps = 6))).first)
    }

    @Test
    fun `TC-HEVY-14 equipment comes from title tokens, else from whether any set has a weight`() {
        assertEquals(Equipment.BARBELL, inferCustomExercise("Row (Barbell)", listOf(weight)).second)
        assertEquals(Equipment.MACHINE, inferCustomExercise("Fly (Cable)", listOf(weight)).second)
        assertEquals(Equipment.RESISTANCE_BAND, inferCustomExercise("Pull Apart (Band)", listOf(weight)).second)
        assertEquals(Equipment.OTHER, inferCustomExercise("Some Exercise", listOf(SetData(reps = 5), weight)).second)
        assertEquals(Equipment.NONE, inferCustomExercise("Some Exercise", listOf(SetData(reps = 5))).second)
    }

    @Test
    fun `TC-HEVY-14 every equipment token`() {
        val cases = mapOf(
            "barbell" to Equipment.BARBELL,
            "dumbbell" to Equipment.DUMBBELL,
            "kettlebell" to Equipment.KETTLEBELL,
            "machine" to Equipment.MACHINE,
            "cable" to Equipment.MACHINE,
            "smith" to Equipment.MACHINE,
            "band" to Equipment.RESISTANCE_BAND,
            "plate" to Equipment.PLATE,
            "suspension" to Equipment.SUSPENSION,
            "trx" to Equipment.SUSPENSION,
            "bodyweight" to Equipment.NONE,
        )
        for ((token, equipment) in cases) {
            assertEquals(equipment, inferCustomExercise("Thing ($token)", listOf(weight)).second, token)
        }
        assertEquals(Equipment.OTHER, inferCustomExercise("Bandit Row", listOf(weight)).second)
    }
}
