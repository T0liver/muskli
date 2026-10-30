package hu.toliver.muskli.data.seed

import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.domain.catalogue.CatalogueExercise
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

// Robolectric, because the JVM unit-test android.jar has only stubs of org.json.
@RunWith(AndroidJUnit4::class)
class CatalogueParserTest {
    private fun file(formatVersion: Int = 1, exercise: String) = """
        {
          "${'$'}schema": "https://example.invalid/schema.json",
          "formatVersion": $formatVersion,
          "version": 7,
          "license": "CC0-1.0",
          "exercises": [$exercise],
          "retiredIds": []
        }
    """.trimIndent()

    private val squat = """
        {
          "id": "barbell-back-squat",
          "title": "Barbell Back Squat",
          "type": "weight_reps",
          "primaryMuscle": "quadriceps",
          "secondaryMuscles": ["glutes", "upper_back"],
          "equipment": "barbell"
        }
    """.trimIndent()

    @Test
    fun `reads versions and exercise fields`() {
        val bundle = CatalogueParser.parse(file(exercise = squat))

        assertEquals(1, bundle.formatVersion)
        assertEquals(7, bundle.version)
        val expected = CatalogueExercise(
            id = "barbell-back-squat",
            title = "Barbell Back Squat",
            type = ExerciseType.WEIGHT_REPS,
            primaryMuscle = MuscleGroup.QUADRICEPS,
            secondaryMuscles = setOf(MuscleGroup.GLUTES, MuscleGroup.UPPER_BACK),
            equipment = Equipment.BARBELL,
        )
        assertEquals(listOf(expected), bundle.exercises)
    }

    @Test
    fun `the optional unilateral flag is read and defaults to false (D-037)`() {
        val lunge = squat.replace("\"equipment\": \"barbell\"", "\"equipment\": \"barbell\",\n  \"unilateral\": true")

        assertEquals(listOf(true), CatalogueParser.parse(file(exercise = lunge)).exercises.map { it.unilateral })
        assertEquals(listOf(false), CatalogueParser.parse(file(exercise = squat)).exercises.map { it.unilateral })
    }

    @Test
    fun `unknown fields are ignored`() {
        val withExtra = squat.replace("\"equipment\"", "\"aliases\": [\"squat\"], \"equipment\"")

        assertEquals(CatalogueParser.parse(file(exercise = squat)), CatalogueParser.parse(file(exercise = withExtra)))
    }

    @Test
    fun `unknown format version is returned without exercises`() {
        val bundle = CatalogueParser.parse(file(formatVersion = 2, exercise = squat.replace("barbell\"", "sled\"")))

        assertEquals(2, bundle.formatVersion)
        assertEquals(emptyList<CatalogueExercise>(), bundle.exercises)
    }

    @Test
    fun `unknown enum value fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            CatalogueParser.parse(file(exercise = squat.replace("\"barbell\"", "\"sled\"")))
        }
    }
}
