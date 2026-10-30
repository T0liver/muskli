package hu.toliver.muskli.domain.library

import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExerciseSearchTest {
    private fun template(
        id: String,
        title: String,
        primary: MuscleGroup,
        equipment: Equipment,
        secondary: Set<MuscleGroup> = emptySet(),
        archived: Boolean = false,
    ) = ExerciseTemplate(
        id = id,
        title = title,
        type = ExerciseType.WEIGHT_REPS,
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        equipment = equipment,
        isCustom = false,
        archived = archived,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
    )

    private val bench = template("bench", "Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, setOf(MuscleGroup.TRICEPS))
    private val dip = template("dip", "Dip", MuscleGroup.TRICEPS, Equipment.NONE, setOf(MuscleGroup.CHEST))
    private val dbPress = template("db-press", "dumbbell press", MuscleGroup.CHEST, Equipment.DUMBBELL)
    private val curl = template("curl", "Bíceps Cúrl", MuscleGroup.BICEPS, Equipment.DUMBBELL)
    private val oldRow = template("old-row", "Machine Row", MuscleGroup.UPPER_BACK, Equipment.MACHINE, archived = true)
    private val all = listOf(dip, curl, oldRow, bench, dbPress)

    @Test
    fun `no filter returns active exercises sorted by title ignoring case and accents`() {
        assertEquals(listOf(bench, curl, dip, dbPress), searchExercises(all, ExerciseFilter()))
    }

    @Test
    fun `name search ignores case`() {
        assertEquals(listOf(bench, dbPress), searchExercises(all, ExerciseFilter(query = "PRESS")))
    }

    @Test
    fun `name search ignores diacritics on both sides`() {
        assertEquals(listOf(curl), searchExercises(all, ExerciseFilter(query = "biceps curl")))
        assertEquals(listOf(bench), searchExercises(all, ExerciseFilter(query = "bénch")))
    }

    @Test
    fun `blank query matches everything`() {
        assertEquals(searchExercises(all, ExerciseFilter()), searchExercises(all, ExerciseFilter(query = "   ")))
    }

    @Test
    fun `muscle filter matches the primary muscle only`() {
        assertEquals(listOf(bench, dbPress), searchExercises(all, ExerciseFilter(muscle = MuscleGroup.CHEST)))
    }

    @Test
    fun `equipment filter`() {
        assertEquals(listOf(curl, dbPress), searchExercises(all, ExerciseFilter(equipment = Equipment.DUMBBELL)))
    }

    @Test
    fun `filters and query combine with AND`() {
        assertEquals(listOf(dbPress), searchExercises(all, ExerciseFilter(muscle = MuscleGroup.CHEST, equipment = Equipment.DUMBBELL)))
        assertEquals(emptyList(), searchExercises(all, ExerciseFilter(query = "curl", muscle = MuscleGroup.CHEST)))
        assertEquals(listOf(dbPress), searchExercises(all, ExerciseFilter(query = "press", equipment = Equipment.DUMBBELL)))
    }

    @Test
    fun `archived exercises only when asked`() {
        assertEquals(emptyList(), searchExercises(all, ExerciseFilter(query = "row")))
        assertEquals(listOf(oldRow), searchExercises(all, ExerciseFilter(query = "row", includeArchived = true)))
    }
}
