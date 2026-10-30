package hu.toliver.muskli.domain.catalogue

import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogueUpdateTest {
    private val now = 50_000L

    private val squat = CatalogueExercise(
        id = "barbell-back-squat",
        title = "Barbell Back Squat",
        type = ExerciseType.WEIGHT_REPS,
        primaryMuscle = MuscleGroup.QUADRICEPS,
        secondaryMuscles = setOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
        equipment = Equipment.BARBELL,
    )
    private val plank = CatalogueExercise(
        id = "plank",
        title = "Plank",
        type = ExerciseType.DURATION,
        primaryMuscle = MuscleGroup.ABDOMINALS,
        secondaryMuscles = emptySet(),
        equipment = Equipment.NONE,
    )

    private fun bundle(version: Int, vararg exercises: CatalogueExercise, formatVersion: Int = 1) =
        CatalogueBundle(formatVersion = formatVersion, version = version, exercises = exercises.toList())

    private fun stored(exercise: CatalogueExercise, at: Long = 1_000L, archived: Boolean = false) =
        exercise.toTemplate(nowEpochMs = at).copy(archived = archived)

    private val custom = ExerciseTemplate(
        id = "6f1c2a9e-0000-4000-8000-000000000001",
        title = "My cable thing",
        type = ExerciseType.WEIGHT_REPS,
        primaryMuscle = MuscleGroup.CHEST,
        secondaryMuscles = emptySet(),
        equipment = Equipment.MACHINE,
        isCustom = true,
        archived = false,
        createdAtEpochMs = 2_000L,
        updatedAtEpochMs = 2_000L,
    )

    @Test
    fun `empty library takes every exercise of the bundle`() {
        val update = planCatalogueUpdate(bundle(1, squat, plank), storedVersion = null, existing = emptyList(), referencedIds = emptySet(), nowEpochMs = now)!!

        assertEquals(1, update.version)
        assertEquals(listOf(squat.toTemplate(now), plank.toTemplate(now)), update.upserts)
        assertEquals(emptyList(), update.archiveIds)
        assertTrue(update.upserts.none { it.isCustom || it.archived })
    }

    @Test
    fun `equal stored version is not applied`() {
        assertNull(planCatalogueUpdate(bundle(3, squat), storedVersion = 3, existing = listOf(stored(squat)), referencedIds = emptySet(), nowEpochMs = now))
    }

    @Test
    fun `older bundle is not applied`() {
        assertNull(planCatalogueUpdate(bundle(2, squat), storedVersion = 3, existing = listOf(stored(squat)), referencedIds = emptySet(), nowEpochMs = now))
    }

    @Test
    fun `unknown format version is not applied`() {
        assertNull(planCatalogueUpdate(bundle(9, squat, formatVersion = 2), storedVersion = 1, existing = emptyList(), referencedIds = emptySet(), nowEpochMs = now))
    }

    @Test
    fun `newer bundle upserts changed exercises by id and keeps their creation time`() {
        val renamed = squat.copy(title = "Back Squat", secondaryMuscles = setOf(MuscleGroup.GLUTES))

        val update = planCatalogueUpdate(bundle(2, renamed, plank), storedVersion = 1, existing = listOf(stored(squat), stored(plank)), referencedIds = emptySet(), nowEpochMs = now)!!

        assertEquals(listOf(renamed.toTemplate(now).copy(createdAtEpochMs = 1_000L)), update.upserts)
        assertEquals(2, update.version)
    }

    @Test
    fun `unchanged exercises are not rewritten`() {
        val update = planCatalogueUpdate(bundle(2, squat), storedVersion = 1, existing = listOf(stored(squat)), referencedIds = emptySet(), nowEpochMs = now)!!

        assertEquals(emptyList(), update.upserts)
    }

    @Test
    fun `custom exercises are never touched`() {
        val collidingId = squat.copy(id = custom.id)

        val update = planCatalogueUpdate(bundle(2, collidingId), storedVersion = 1, existing = listOf(custom), referencedIds = emptySet(), nowEpochMs = now)!!

        assertEquals(emptyList(), update.upserts)
        assertEquals(emptyList(), update.archiveIds)
    }

    @Test
    fun `unreferenced published exercise missing from the bundle is archived`() {
        val update = planCatalogueUpdate(bundle(2, squat), storedVersion = 1, existing = listOf(stored(squat), stored(plank)), referencedIds = emptySet(), nowEpochMs = now)!!

        assertEquals(listOf(plank.id), update.archiveIds)
    }

    @Test
    fun `referenced published exercise missing from the bundle is kept`() {
        val update = planCatalogueUpdate(bundle(2, squat), storedVersion = 1, existing = listOf(stored(squat), stored(plank)), referencedIds = setOf(plank.id), nowEpochMs = now)!!

        assertEquals(emptyList(), update.archiveIds)
        assertEquals(emptyList(), update.upserts)
    }

    @Test
    fun `already archived exercise is not archived again`() {
        val update = planCatalogueUpdate(bundle(2, squat), storedVersion = 1, existing = listOf(stored(squat), stored(plank, archived = true)), referencedIds = emptySet(), nowEpochMs = now)!!

        assertEquals(emptyList(), update.archiveIds)
    }

    @Test
    fun `archived exercise back in the bundle is restored`() {
        val update = planCatalogueUpdate(bundle(3, plank), storedVersion = 2, existing = listOf(stored(plank, archived = true)), referencedIds = emptySet(), nowEpochMs = now)!!

        assertEquals(listOf(plank.toTemplate(now).copy(createdAtEpochMs = 1_000L)), update.upserts)
    }
}
