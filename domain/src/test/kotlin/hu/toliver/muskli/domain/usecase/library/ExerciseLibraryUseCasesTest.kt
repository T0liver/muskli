package hu.toliver.muskli.domain.usecase.library

import hu.toliver.muskli.domain.library.ExerciseFilter
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.domain.repo.IdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExerciseLibraryUseCasesTest {
    private var now = 1_000L
    private val clock = Clock { now }
    private var nextId = 0
    private val ids = IdGenerator { "custom-${nextId++}" }

    private val published = ExerciseTemplate(
        id = "plank",
        title = "Plank",
        type = ExerciseType.DURATION,
        primaryMuscle = MuscleGroup.ABDOMINALS,
        secondaryMuscles = emptySet(),
        equipment = Equipment.NONE,
        isCustom = false,
        archived = false,
        createdAtEpochMs = 0L,
        updatedAtEpochMs = 0L,
    )
    private val repo = FakeExerciseTemplateRepository(listOf(published))
    private val create = CreateCustomExerciseUseCase(repo, ids, clock)
    private val edit = EditCustomExerciseUseCase(repo, clock)
    private val archive = ArchiveCustomExerciseUseCase(repo, clock)

    private val input = CustomExerciseInput(
        title = "  Cable Fly  ",
        type = ExerciseType.WEIGHT_REPS,
        primaryMuscle = MuscleGroup.CHEST,
        secondaryMuscles = setOf(MuscleGroup.SHOULDERS, MuscleGroup.CHEST),
        equipment = Equipment.MACHINE,
    )

    @Test
    fun `create stores a custom exercise with a generated id and the clock time`() = runTest {
        val created = create(input)

        val expected = ExerciseTemplate(
            id = "custom-0",
            title = "Cable Fly",
            type = ExerciseType.WEIGHT_REPS,
            primaryMuscle = MuscleGroup.CHEST,
            secondaryMuscles = setOf(MuscleGroup.SHOULDERS),
            equipment = Equipment.MACHINE,
            isCustom = true,
            archived = false,
            createdAtEpochMs = 1_000L,
            updatedAtEpochMs = 1_000L,
        )
        assertEquals(expected, created)
        assertEquals(expected, repo.get("custom-0"))
    }

    @Test
    fun `create rejects a blank title`() = runTest {
        assertThrows<IllegalArgumentException> { create(input.copy(title = "   ")) }
    }

    @Test
    fun `custom exercises are unlimited`() = runTest {
        repeat(500) { create(input.copy(title = "Custom $it")) }

        assertEquals(500, repo.observeAll().first().count { it.isCustom })
    }

    @Test
    fun `edit updates a custom exercise and keeps its creation time`() = runTest {
        create(input)
        now = 5_000L

        val edited = edit("custom-0", input.copy(title = "Pec Deck", type = ExerciseType.REPS_ONLY, secondaryMuscles = emptySet()))

        assertEquals("Pec Deck", edited?.title)
        assertEquals(ExerciseType.REPS_ONLY, edited?.type)
        assertEquals(1_000L, edited?.createdAtEpochMs)
        assertEquals(5_000L, edited?.updatedAtEpochMs)
        assertEquals(edited, repo.get("custom-0"))
    }

    @Test
    fun `edit refuses a published exercise`() = runTest {
        assertNull(edit(published.id, input))
        assertEquals(published, repo.get(published.id))
    }

    @Test
    fun `archive hides a custom exercise without deleting it`() = runTest {
        create(input)
        now = 7_000L

        assertTrue(archive("custom-0"))

        val stored = repo.get("custom-0")!!
        assertTrue(stored.archived)
        assertEquals(7_000L, stored.updatedAtEpochMs)
        assertEquals(emptyList(), SearchExercisesUseCase(repo)(ExerciseFilter(query = "cable")).first())
    }

    @Test
    fun `archive refuses a published or missing exercise`() = runTest {
        assertFalse(archive(published.id))
        assertFalse(archive("missing"))
        assertEquals(published, repo.get(published.id))
    }

    @Test
    fun `search use case filters the repository contents`() = runTest {
        create(input)

        val found = SearchExercisesUseCase(repo)(ExerciseFilter(query = "cable", equipment = Equipment.MACHINE)).first()

        assertEquals(listOf("custom-0"), found.map { it.id })
    }
}
