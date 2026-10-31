package hu.toliver.muskli.domain.importer.hevy

import hu.toliver.muskli.domain.TestClock
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.repo.IdGenerator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HevyImportPlanTest {
    private val now = 1_800_000_000_000L
    private val clock = TestClock(now = now, offsetS = 3600)

    private class CountingIds : IdGenerator {
        val issued = mutableListOf<String>()

        override fun newId(): String = "id-${issued.size + 1}".also { issued += it }
    }

    private val fixture: List<HevyWorkout> by lazy {
        val text = requireNotNull(javaClass.getResource("/hevy/workout_data.csv")).readText()
        assertIs<HevyParseResult.Workouts>(parseHevyExport(text, clock)).workouts
    }

    private val pushStart = 1773507900000L
    private val legsStart = 1773295200000L

    private fun defaults(templates: List<ExerciseTemplate> = catalogueV1): Map<String, HevyResolution> =
        defaultResolutions(matchHevyExercises(fixture.flatMap { w -> w.exercises.map { it.title } }.toSet(), templates))

    private fun templateIdsOf(workout: Workout): List<String> = workout.exercises.map { it.exerciseTemplateId }

    private fun assertEverySetReferences(plan: HevyImportPlan, templates: List<ExerciseTemplate>) {
        val known = (templates + plan.newTemplates).map { it.id }.toSet()
        val used = plan.newWorkouts.flatMap { w -> w.exercises.map { it.exerciseTemplateId } }
        assertTrue(used.all { it in known }, "unknown template ids: ${used.filterNot { it in known }}")
    }

    @Test
    fun `TC-HEVY-15 every unmatched title becomes a new custom exercise`() {
        val ids = CountingIds()

        val plan = planHevyImport(fixture, defaults(), emptyList(), ids, clock)

        val custom = { id: String, title: String, type: ExerciseType, equipment: Equipment ->
            ExerciseTemplate(id, title, type, MuscleGroup.OTHER, emptySet(), equipment, isCustom = true, archived = false, now, now)
        }
        val byTitle = plan.newTemplates.associateBy { it.title }
        assertEquals(
            setOf(
                custom(byTitle.getValue("Lateral Raise (Dumbbell)").id, "Lateral Raise (Dumbbell)", ExerciseType.WEIGHT_REPS, Equipment.DUMBBELL),
                custom(byTitle.getValue("Goblet Squat (Kettlebell)").id, "Goblet Squat (Kettlebell)", ExerciseType.WEIGHT_REPS, Equipment.KETTLEBELL),
                custom(byTitle.getValue("Assisted Pull Up").id, "Assisted Pull Up", ExerciseType.BODYWEIGHT_ASSISTED_REPS, Equipment.OTHER),
            ),
            plan.newTemplates.toSet(),
        )
        assertEquals(3, plan.newTemplates.size)
        assertTrue(plan.newTemplates.all { it.id in ids.issued })
        assertEverySetReferences(plan, catalogueV1)

        val lateral = byTitle.getValue("Lateral Raise (Dumbbell)").id
        val goblet = byTitle.getValue("Goblet Squat (Kettlebell)").id
        val assisted = byTitle.getValue("Assisted Pull Up").id
        assertEquals(
            listOf(
                listOf("barbell-bench-press", lateral, "pull-up", "plank", "barbell-bench-press"),
                listOf(goblet, assisted, "dumbbell-farmers-walk", "rowing-machine"),
            ),
            plan.newWorkouts.map(::templateIdsOf),
        )
    }

    @Test
    fun `TC-HEVY-15 new workouts carry the parsed values, new ids, and no completion times`() {
        val ids = CountingIds()

        val plan = planHevyImport(fixture, defaults(), emptyList(), ids, clock)

        assertEquals(fixture.size, plan.newWorkouts.size)
        for ((workout, hevy) in plan.newWorkouts.zip(fixture)) {
            assertEquals(hevy.title, workout.title)
            assertEquals(hevy.notes, workout.notes)
            assertEquals(hevy.startedAtEpochMs, workout.startedAtEpochMs)
            assertEquals(hevy.finishedAtEpochMs, workout.finishedAtEpochMs)
            assertEquals(3600, workout.utcOffsetS)
            assertEquals(now, workout.createdAtEpochMs)
            assertEquals(now, workout.updatedAtEpochMs)
            assertEquals(null, workout.deletedAtEpochMs)
            assertEquals(null, workout.restTimer)
            assertEquals(null, workout.routineId)
            assertEquals(hevy.exercises.map { it.supersetGroup }, workout.exercises.map { it.supersetGroup })
            assertEquals(hevy.exercises.map { it.notes }, workout.exercises.map { it.notes })
            assertTrue(workout.exercises.all { it.restSeconds == null })
            assertEquals(hevy.exercises.map { e -> e.sets.map { it.data } }, workout.exercises.map { e -> e.sets.map { it.data } })
            assertTrue(workout.exercises.flatMap { it.sets }.all { it.completedAtEpochMs == null })
        }
        val allIds = plan.newWorkouts.flatMap { w -> listOf(w.id) + w.exercises.flatMap { e -> listOf(e.id) + e.sets.map { it.id } } } +
            plan.newTemplates.map { it.id }
        assertEquals(allIds.size, allIds.toSet().size)
        assertEquals(ids.issued.toSet(), allIds.toSet())
    }

    @Test
    fun `TC-HEVY-16 a mapped title gets no custom and sets use the chosen templates`() {
        val myLateral = template("my-lateral", "Side Raise", isCustom = true)
        val templates = catalogueV1 + myLateral
        val resolutions = defaults(templates) +
            mapOf(
                "Lateral Raise (Dumbbell)" to HevyResolution.UseTemplate("my-lateral"),
                "Pull Up" to HevyResolution.UseTemplate("machine-assisted-pull-up"),
            )

        val plan = planHevyImport(fixture, resolutions, emptyList(), CountingIds(), clock)

        assertEquals(setOf("Goblet Squat (Kettlebell)", "Assisted Pull Up"), plan.newTemplates.map { it.title }.toSet())
        assertEquals(
            listOf("barbell-bench-press", "my-lateral", "machine-assisted-pull-up", "plank", "barbell-bench-press"),
            templateIdsOf(plan.newWorkouts.first()),
        )
        assertEverySetReferences(plan, templates)
    }

    @Test
    fun `TC-HEVY-17 existing starts in the same minute skip both workouts`() {
        val plan = planHevyImport(fixture, defaults(), listOf(pushStart + 37000, legsStart), CountingIds(), clock)

        assertEquals(emptyList(), plan.newWorkouts)
        assertEquals(2, plan.duplicatesSkipped)
        assertEquals(2, plan.workoutsInFile)
    }

    @Test
    fun `TC-HEVY-17 an existing start one minute later does not make a duplicate`() {
        val plan = planHevyImport(fixture, defaults(), listOf(pushStart + 60000, legsStart), CountingIds(), clock)

        assertEquals(listOf("Push, heavy"), plan.newWorkouts.map { it.title })
        assertEquals(1, plan.duplicatesSkipped)
    }

    @Test
    fun `D-044 two workouts in the file with the same start minute are both imported`() {
        val twin = fixture.first().copy(title = "Twin", startedAtEpochMs = pushStart + 30000)

        val plan = planHevyImport(fixture + twin, defaults(), emptyList(), CountingIds(), clock)

        assertEquals(listOf("Push, heavy", "Legs", "Twin"), plan.newWorkouts.map { it.title })
        assertEquals(0, plan.duplicatesSkipped)
    }

    @Test
    fun `TC-HEVY-18 summary for the fixture on an empty phone`() {
        val plan = planHevyImport(fixture, defaults(), emptyList(), CountingIds(), clock)

        assertEquals(2, plan.workoutsInFile)
        assertEquals(2, plan.newWorkouts.size)
        assertEquals(0, plan.duplicatesSkipped)
        assertEquals(13, plan.setCount)
        assertEquals(2, plan.rpeDropped)
        assertEquals(3, plan.newTemplates.size)
        assertEquals(legsStart, plan.firstStartEpochMs)
        assertEquals(pushStart, plan.lastStartEpochMs)
    }

    @Test
    fun `TC-HEVY-18 summary counts only the workouts that will be imported`() {
        val plan = planHevyImport(fixture, defaults(), listOf(pushStart), CountingIds(), clock)

        assertEquals(2, plan.workoutsInFile)
        assertEquals(1, plan.duplicatesSkipped)
        assertEquals(5, plan.setCount)
        assertEquals(0, plan.rpeDropped)
        assertEquals(legsStart, plan.firstStartEpochMs)
        assertEquals(legsStart, plan.lastStartEpochMs)

        val none = planHevyImport(fixture, defaults(), listOf(pushStart, legsStart), CountingIds(), clock)
        assertEquals(0, none.setCount)
        assertEquals(null, none.firstStartEpochMs)
        assertEquals(null, none.lastStartEpochMs)
    }

    @Test
    fun `TC-HEVY-19 a title used only by duplicate workouts gets no custom exercise`() {
        val plan = planHevyImport(fixture, defaults(), listOf(legsStart), CountingIds(), clock)

        assertEquals(listOf("Lateral Raise (Dumbbell)"), plan.newTemplates.map { it.title })
        assertEverySetReferences(plan, catalogueV1)
    }
}
