package hu.toliver.muskli.domain.usecase.workout

import hu.toliver.muskli.domain.TestClock
import hu.toliver.muskli.domain.calc.RestEffect
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import hu.toliver.muskli.domain.repo.IdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class LiveWorkoutUseCasesTest {
    private val repo = FakeWorkoutRepository()
    private val clock = TestClock(now = 10_000L, offsetS = 3_600)
    private var nextId = 0
    private val ids = IdGenerator { "id${nextId++}" }
    private val start = StartWorkoutUseCase(repo, ids, clock)
    private val addExercises = AddExercisesUseCase(repo, ids, clock)
    private val replaceExercise = ReplaceExerciseUseCase(repo, ids, clock)
    private val removeExercise = RemoveExerciseUseCase(repo, clock)
    private val reorderExercise = ReorderExerciseUseCase(repo, clock)
    private val addSet = AddSetUseCase(repo, ids, clock)
    private val deleteSet = DeleteSetUseCase(repo, clock)
    private val changeSetType = ChangeSetTypeUseCase(repo, clock)
    private val editSetValues = EditSetValuesUseCase(repo, clock)
    private val completeSet = CompleteSetUseCase(repo, clock)
    private val uncompleteSet = UncompleteSetUseCase(repo, clock)

    private fun emptySet(id: String) = WorkoutSet(id, SetData(), completedAtEpochMs = null)

    private suspend fun stored(id: String): Workout = repo.observe(id).first()!!

    private suspend fun startWith(vararg templateIds: String): Workout {
        val draft = (start() as StartWorkoutResult.Started).workout
        return addExercises(draft.id, templateIds.toList())!!
    }

    private fun Workout.setIds(exerciseIndex: Int) = exercises[exerciseIndex].sets.map { it.id }

    @Test
    fun `start creates an empty draft with the clock time and offset`() = runTest {
        val result = start()

        val expected = Workout(
            id = "id0",
            title = "",
            notes = null,
            startedAtEpochMs = 10_000L,
            utcOffsetS = 3_600,
            finishedAtEpochMs = null,
            createdAtEpochMs = 10_000L,
            updatedAtEpochMs = 10_000L,
            deletedAtEpochMs = null,
            exercises = emptyList(),
        )
        assertEquals(StartWorkoutResult.Started(expected), result)
        assertEquals(expected, repo.observeActiveDraft().first())
    }

    @Test
    fun `starting while a draft exists resumes it instead of creating another`() = runTest {
        val first = startWith("bench")
        clock.now = 20_000L

        val result = start()

        assertEquals(StartWorkoutResult.Resumed(first), result)
        assertEquals(1, repo.workouts.value.size)
    }

    @Test
    fun `added exercises start with one empty normal set each, in the order given`() = runTest {
        val workout = startWith("bench", "row")

        assertEquals(
            listOf(
                WorkoutExercise("id1", "bench", supersetGroup = null, notes = null, restSeconds = null, sets = listOf(emptySet("id2"))),
                WorkoutExercise("id3", "row", supersetGroup = null, notes = null, restSeconds = null, sets = listOf(emptySet("id4"))),
            ),
            workout.exercises,
        )
        assertEquals(workout, stored(workout.id))
    }

    @Test
    fun `every mutation bumps updatedAt`() = runTest {
        val workout = startWith("bench")
        clock.now = 11_000L

        val edited = addSet(workout.id, workout.exercises[0].id)!!

        assertEquals(11_000L, edited.updatedAtEpochMs)
        assertEquals(10_000L, edited.createdAtEpochMs)
    }

    @Test
    fun `TC-SET-01 changing the type any number of times persists and keeps the values`() = runTest {
        val workout = startWith("bench")
        val setId = workout.exercises[0].sets[0].id
        editSetValues(workout.id, setId, SetValues(weightKg = 100.0, reps = 5))
        completeSet(workout.id, setId, defaultRestS = 90)

        listOf(SetType.WARMUP, SetType.DROPSET, SetType.FAILURE, SetType.NORMAL, SetType.FAILURE).forEach {
            changeSetType(workout.id, setId, it)
            assertEquals(it, stored(workout.id).exercises[0].sets[0].data.type)
        }

        val set = stored(workout.id).exercises[0].sets[0]
        assertEquals(SetData(SetType.FAILURE, weightKg = 100.0, reps = 5, completed = true), set.data)
        assertEquals(10_000L, set.completedAtEpochMs)
    }

    @Test
    fun `TC-SET-02 multiple consecutive drop sets are allowed`() = runTest {
        val workout = startWith("bench")
        val exerciseId = workout.exercises[0].id
        repeat(3) { addSet(workout.id, exerciseId) }
        val setIds = stored(workout.id).setIds(0)

        setIds.drop(1).forEach { changeSetType(workout.id, it, SetType.DROPSET) }

        assertEquals(
            listOf(SetType.NORMAL, SetType.DROPSET, SetType.DROPSET, SetType.DROPSET),
            stored(workout.id).exercises[0].sets.map { it.data.type },
        )
    }

    @Test
    fun `TC-SET-03 deleting a set keeps the others contiguous and in order`() = runTest {
        val workout = startWith("bench")
        val exerciseId = workout.exercises[0].id
        repeat(3) { addSet(workout.id, exerciseId) }
        val before = stored(workout.id).setIds(0)

        deleteSet(workout.id, before[1])

        assertEquals(listOf(before[0], before[2], before[3]), stored(workout.id).setIds(0))
    }

    @Test
    fun `TC-SET-04 reordering exercises moves them with their sets`() = runTest {
        val workout = startWith("a", "b", "c")
        workout.exercises.forEach { addSet(workout.id, it.id) }
        val before = stored(workout.id)
        val setsByExercise = before.exercises.associate { it.id to it.sets }

        reorderExercise(workout.id, before.exercises[0].id, toIndex = 2)

        val after = stored(workout.id)
        assertEquals(listOf("b", "c", "a"), after.exercises.map { it.exerciseTemplateId })
        after.exercises.forEach { assertEquals(setsByExercise.getValue(it.id), it.sets) }
    }

    @Test
    fun `reorder clamps the target index`() = runTest {
        val workout = startWith("a", "b")

        reorderExercise(workout.id, workout.exercises[1].id, toIndex = -5)

        assertEquals(listOf("b", "a"), stored(workout.id).exercises.map { it.exerciseTemplateId })
    }

    @Test
    fun `remove drops the exercise and its sets`() = runTest {
        val workout = startWith("a", "b")

        removeExercise(workout.id, workout.exercises[0].id)

        assertEquals(listOf("b"), stored(workout.id).exercises.map { it.exerciseTemplateId })
    }

    @Test
    fun `replace swaps the template in place and restarts with one empty set`() = runTest {
        val workout = startWith("a", "b")
        val target = workout.exercises[0]
        editSetValues(workout.id, target.sets[0].id, SetValues(weightKg = 60.0, reps = 8))
        addSet(workout.id, target.id)
        repo.update(workout.id) { w -> w.copy(exercises = w.exercises.map { it.copy(supersetGroup = 1, restSeconds = 120, notes = "x") }) }

        replaceExercise(workout.id, target.id, newTemplateId = "z")

        val replaced = stored(workout.id).exercises[0]
        assertEquals("z", replaced.exerciseTemplateId)
        assertEquals(1, replaced.supersetGroup)
        assertNull(replaced.restSeconds)
        assertNull(replaced.notes)
        assertEquals(listOf(SetData()), replaced.sets.map { it.data })
        assertEquals("b", stored(workout.id).exercises[1].exerciseTemplateId)
    }

    @Test
    fun `edit values keeps type, completion and side`() = runTest {
        val workout = startWith("bench")
        val setId = workout.exercises[0].sets[0].id
        changeSetType(workout.id, setId, SetType.WARMUP)

        editSetValues(workout.id, setId, SetValues(weightKg = 40.0, reps = 10, durationS = 30, distanceM = 5.0))

        assertEquals(
            SetData(SetType.WARMUP, weightKg = 40.0, reps = 10, durationS = 30, distanceM = 5.0),
            stored(workout.id).exercises[0].sets[0].data,
        )
    }

    @Test
    fun `complete marks the set with the clock time and starts the exercise rest before the next set`() = runTest {
        val workout = startWith("bench")
        repo.update(workout.id) { w -> w.copy(exercises = w.exercises.map { it.copy(restSeconds = 150) }) }
        addSet(workout.id, workout.exercises[0].id)
        clock.now = 12_345L
        val setId = workout.exercises[0].sets[0].id

        val effect = completeSet(workout.id, setId, defaultRestS = 90)

        assertEquals(RestEffect.Start(150), effect)
        val set = stored(workout.id).exercises[0].sets[0]
        assertEquals(true, set.data.completed)
        assertEquals(12_345L, set.completedAtEpochMs)
    }

    @Test
    fun `complete on the last set of an exercise looks at the next exercise and uses the default rest`() = runTest {
        val workout = startWith("a", "b")

        assertEquals(RestEffect.Start(90), completeSet(workout.id, workout.exercises[0].sets[0].id, defaultRestS = 90))
    }

    @Test
    fun `complete before a drop set starts no timer`() = runTest {
        val workout = startWith("a")
        addSet(workout.id, workout.exercises[0].id)
        val (first, second) = stored(workout.id).setIds(0)
        changeSetType(workout.id, second, SetType.DROPSET)

        assertEquals(RestEffect.None, completeSet(workout.id, first, defaultRestS = 90))
    }

    @Test
    fun `complete on the last set of the workout starts no timer`() = runTest {
        val workout = startWith("a", "b")

        assertEquals(RestEffect.None, completeSet(workout.id, workout.exercises[1].sets[0].id, defaultRestS = 90))
    }

    @Test
    fun `completing an already completed set changes nothing and starts no timer`() = runTest {
        val workout = startWith("a", "b")
        val setId = workout.exercises[0].sets[0].id
        completeSet(workout.id, setId, defaultRestS = 90)
        clock.now = 99_000L
        val writes = repo.writes

        assertEquals(RestEffect.None, completeSet(workout.id, setId, defaultRestS = 90))
        assertEquals(writes, repo.writes)
        assertEquals(10_000L, stored(workout.id).exercises[0].sets[0].completedAtEpochMs)
    }

    @Test
    fun `uncomplete clears completion and its time`() = runTest {
        val workout = startWith("a")
        val setId = workout.exercises[0].sets[0].id
        completeSet(workout.id, setId, defaultRestS = 90)

        uncompleteSet(workout.id, setId)

        val set = stored(workout.id).exercises[0].sets[0]
        assertEquals(false, set.data.completed)
        assertNull(set.completedAtEpochMs)
    }

    @Test
    fun `unknown ids change nothing`() = runTest {
        val workout = startWith("a")
        val writes = repo.writes

        assertNull(addSet("missing", "x"))
        assertEquals(workout, addSet(workout.id, "missing"))
        assertEquals(workout, deleteSet(workout.id, "missing"))
        assertEquals(RestEffect.None, completeSet(workout.id, "missing", defaultRestS = 90))
        assertEquals(writes, repo.writes)
        assertIs<StartWorkoutResult.Resumed>(start())
    }
}
