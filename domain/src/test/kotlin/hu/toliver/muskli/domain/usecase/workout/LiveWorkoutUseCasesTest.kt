package hu.toliver.muskli.domain.usecase.workout

import hu.toliver.muskli.domain.TestClock
import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.calc.RestEffect
import hu.toliver.muskli.domain.calc.WorkoutExerciseRecord
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side
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
    fun `add set copies the values of the latest set, uncompleted`() = runTest {
        val workout = startWith("bench")
        val exerciseId = workout.exercises[0].id
        val firstId = workout.exercises[0].sets[0].id
        editSetValues(workout.id, firstId, SetValues(weightKg = 100.0, reps = 5))
        completeSet(workout.id, firstId, defaultRestS = 90)

        val added = addSet(workout.id, exerciseId)!!.exercises[0].sets.last()

        assertEquals(SetData(SetType.NORMAL, weightKg = 100.0, reps = 5), added.data)
        assertNull(added.completedAtEpochMs)
    }

    @Test
    fun `add set copies every value field and the side of the set directly above`() = runTest {
        val workout = startWith("bench")
        val exerciseId = workout.exercises[0].id
        val above = SetData(weightKg = 20.0, reps = 8, durationS = 45, distanceM = 300.0, side = Side.LEFT)
        repo.update(workout.id) { w ->
            w.copy(exercises = w.exercises.map { e -> e.copy(sets = e.sets.map { it.copy(data = above) }) })
        }

        val added = addSet(workout.id, exerciseId)!!.exercises[0].sets.last()

        assertEquals(SetData(weightKg = 20.0, reps = 8, durationS = 45, distanceM = 300.0, side = Side.LEFT), added.data)
    }

    @Test
    fun `add set after a warm-up or drop set adds a normal set with its values`() = runTest {
        val workout = startWith("bench")
        val setId = workout.exercises[0].sets[0].id
        editSetValues(workout.id, setId, SetValues(weightKg = 40.0, reps = 12))

        listOf(SetType.WARMUP, SetType.DROPSET, SetType.FAILURE).forEach { type ->
            val aboveId = stored(workout.id).setIds(0).last()
            changeSetType(workout.id, aboveId, type)

            val added = addSet(workout.id, workout.exercises[0].id)!!.exercises[0].sets.last()

            assertEquals(SetData(SetType.NORMAL, weightKg = 40.0, reps = 12), added.data)
        }
    }

    @Test
    fun `add set on an exercise with no sets adds one empty normal set`() = runTest {
        val workout = startWith("bench")
        val exerciseId = workout.exercises[0].id
        deleteSet(workout.id, workout.exercises[0].sets[0].id)

        val sets = addSet(workout.id, exerciseId)!!.exercises[0].sets

        assertEquals(listOf(SetData(SetType.NORMAL)), sets.map { it.data })
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

    private fun lastTime(workoutId: String, startedAt: Long, vararg sets: SetData, finished: Boolean = true) = WorkoutExerciseRecord(
        workoutId = workoutId,
        workoutStartedAtEpochMs = startedAt,
        workoutFinishedAtEpochMs = if (finished) startedAt + 3_600_000 else null,
        workoutDeletedAtEpochMs = null,
        positionInWorkout = 0,
        sets = sets.toList(),
    )

    private fun done(type: SetType, weightKg: Double, reps: Int) = SetData(type, weightKg = weightKg, reps = reps, completed = true)

    @Test
    fun `adding an exercise done before prefills count, types and values from last time, uncompleted`() = runTest {
        repo.history = mapOf(
            "bench" to listOf(
                lastTime("old", 1_000, done(SetType.NORMAL, 50.0, 10)),
                lastTime("last", 2_000, done(SetType.WARMUP, 40.0, 10), done(SetType.NORMAL, 100.0, 5), done(SetType.FAILURE, 100.0, 3)),
            ),
        )

        val workout = startWith("bench", "row")

        assertEquals(
            listOf(
                SetData(SetType.WARMUP, weightKg = 40.0, reps = 10),
                SetData(SetType.NORMAL, weightKg = 100.0, reps = 5),
                SetData(SetType.FAILURE, weightKg = 100.0, reps = 3),
            ),
            workout.exercises[0].sets.map { it.data },
        )
        assertEquals(listOf(null, null, null), workout.exercises[0].sets.map { it.completedAtEpochMs })
        assertEquals(listOf(SetData()), workout.exercises[1].sets.map { it.data })
        assertEquals(workout.exercises.flatMap { it.sets }.map { it.id }.distinct().size, 4)
        assertEquals(workout, stored(workout.id))
    }

    @Test
    fun `prefill skips drafts and keeps an uncompleted slot as an empty set`() = runTest {
        repo.history = mapOf(
            "bench" to listOf(
                lastTime("last", 2_000, done(SetType.NORMAL, 80.0, 8), SetData(weightKg = 85.0, reps = 6)),
                lastTime("draft", 3_000, done(SetType.NORMAL, 90.0, 8), finished = false),
            ),
        )

        val workout = startWith("bench")

        assertEquals(listOf(SetData(weightKg = 80.0, reps = 8), SetData()), workout.exercises[0].sets.map { it.data })
    }

    @Test
    fun `previous sets come from the last finished workout by raw index`() = runTest {
        repo.history = mapOf("bench" to listOf(lastTime("last", 2_000, done(SetType.WARMUP, 40.0, 10), done(SetType.NORMAL, 100.0, 5))))
        val previousSets = PreviousSetsUseCase(repo)

        assertEquals(listOf(done(SetType.WARMUP, 40.0, 10), done(SetType.NORMAL, 100.0, 5)), previousSets("bench"))
        assertEquals(emptyList(), previousSets("row"))
    }

    private val benchInRoutinesAThenB = mapOf(
        "bench" to listOf(
            lastTime("a", 1_000, done(SetType.NORMAL, 80.0, 8)).copy(routineId = "routine-a"),
            lastTime("b", 2_000, done(SetType.NORMAL, 85.0, 6)).copy(routineId = "routine-b"),
        ),
    )

    @Test
    fun `previous sets in routine mode come from the last workout of that routine`() = runTest {
        repo.history = benchInRoutinesAThenB
        val previousSets = PreviousSetsUseCase(repo)

        assertEquals(listOf(done(SetType.NORMAL, 80.0, 8)), previousSets("bench", PreviousMode.ROUTINE_SCOPED, "routine-a"))
        assertEquals(listOf(done(SetType.NORMAL, 85.0, 6)), previousSets("bench", PreviousMode.ROUTINE_SCOPED, null))
        assertEquals(listOf(done(SetType.NORMAL, 85.0, 6)), previousSets("bench", PreviousMode.ANYWHERE, "routine-a"))
    }

    @Test
    fun `prefill in routine mode follows the same routine as the previous column`() = runTest {
        repo.history = benchInRoutinesAThenB
        val draft = (start() as StartWorkoutResult.Started).workout

        val workout = addExercises(draft.id, listOf("bench"), PreviousMode.ROUTINE_SCOPED, "routine-a")!!

        assertEquals(listOf(SetData(weightKg = 80.0, reps = 8)), workout.exercises[0].sets.map { it.data })
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
