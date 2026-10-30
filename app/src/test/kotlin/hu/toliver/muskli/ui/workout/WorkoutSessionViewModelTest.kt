package hu.toliver.muskli.ui.workout

import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.library.FakeExerciseTemplateRepository
import hu.toliver.muskli.ui.library.sampleLibrary
import hu.toliver.muskli.ui.settings.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.NumberFormat

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val templates = FakeExerciseTemplateRepository(
        sampleLibrary + sampleLibrary.first().copy(id = "plank", title = "Plank", type = ExerciseType.DURATION),
    )
    private val settings = FakeSettingsRepository()
    private val clock = TestClock()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.subscribed(workouts: FakeWorkoutRepository): WorkoutSessionViewModel =
        workoutSessionViewModel(workouts, templates, settings, clock).also { vm -> backgroundScope.launch { vm.uiState.collect {} } }

    private val WorkoutSessionViewModel.active get() = uiState.value as LiveWorkoutUiState.Active

    private fun set(id: String, data: SetData = SetData()) = WorkoutSet(id, data, completedAtEpochMs = null)

    private fun draft(vararg exercises: WorkoutExercise) = Workout(
        id = "w1",
        title = "",
        notes = null,
        startedAtEpochMs = TestClock.START_MS,
        utcOffsetS = 0,
        finishedAtEpochMs = null,
        createdAtEpochMs = TestClock.START_MS,
        updatedAtEpochMs = TestClock.START_MS,
        deletedAtEpochMs = null,
        exercises = exercises.toList(),
    )

    private fun exercise(id: String, templateId: String, vararg sets: WorkoutSet) =
        WorkoutExercise(id, templateId, supersetGroup = null, notes = null, restSeconds = null, sets = sets.toList())

    private fun FakeWorkoutRepository.storedSet(exerciseIndex: Int = 0, setIndex: Int = 0) =
        activeDraft!!.exercises[exerciseIndex].sets[setIndex]

    @Test
    fun withoutDraftThereIsNoWorkoutUntilStarted() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository()
        val vm = subscribed(workouts)
        assertEquals(LiveWorkoutUiState.NoWorkout, vm.uiState.value)

        vm.onEvent(LiveWorkoutEvent.StartClicked)

        assertTrue(vm.active.exercises.isEmpty())
        assertEquals("0:00", vm.active.duration)
    }

    @Test
    fun aDraftStoredBeforeARestartIsShownAsItWas() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(
            listOf(
                draft(
                    exercise(
                        "e1",
                        "squat-barbell",
                        set("s1", SetData(weightKg = 100.0, reps = 5, completed = true)),
                        set("s2", SetData(weightKg = 102.5, reps = 3)),
                    ),
                ),
            ),
        )
        clock.now = TestClock.START_MS + 754_000

        val vm = subscribed(workouts)

        val exercise = vm.active.exercises.single()
        assertEquals("Squat (Barbell)", exercise.name)
        assertEquals(listOf(listOf("100", "5"), listOf("102.5", "3")), exercise.sets.map { it.values })
        assertEquals(listOf(true, false), exercise.sets.map { it.completed })
        assertEquals("12:34", vm.active.duration)
    }

    @Test
    fun headerCountsOnlyCompletedSets() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(
            listOf(
                draft(
                    exercise(
                        "e1",
                        "squat-barbell",
                        set("s1", SetData(type = SetType.WARMUP, weightKg = 60.0, reps = 5, completed = true)),
                        set("s2", SetData(weightKg = 100.0, reps = 5, completed = true)),
                        set("s3", SetData(weightKg = 100.0, reps = 5)),
                    ),
                ),
            ),
        )
        val vm = subscribed(workouts)
        assertEquals(1, vm.active.completedSets)
        assertEquals("500", vm.active.volume)

        vm.onEvent(LiveWorkoutEvent.CompletedChanged("s3", completed = true))
        assertEquals(2, vm.active.completedSets)
        assertEquals(NumberFormat.getIntegerInstance().format(1000), vm.active.volume)

        vm.onEvent(LiveWorkoutEvent.CompletedChanged("s2", completed = false))
        assertEquals(1, vm.active.completedSets)
        assertNull(workouts.storedSet(setIndex = 1).completedAtEpochMs)
    }

    @Test
    fun typedValuesAreStoredInKgAndShownAsTyped() = runTest(dispatcher) {
        settings.settings.value = Settings(weightUnit = WeightUnit.LB)
        val workouts = FakeWorkoutRepository(listOf(draft(exercise("e1", "squat-barbell", set("s1")))))
        val vm = subscribed(workouts)

        vm.onEvent(LiveWorkoutEvent.ValueChanged("s1", SetColumn.WEIGHT, "225."))
        vm.onEvent(LiveWorkoutEvent.ValueChanged("s1", SetColumn.REPS, "5"))

        assertEquals(listOf("225.", "5"), vm.active.exercises[0].sets[0].values)
        val stored = workouts.storedSet().data
        assertEquals(102.058, stored.weightKg!!, 0.001)
        assertEquals(5, stored.reps)
    }

    @Test
    fun aDecimalCommaIsAccepted() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(listOf(draft(exercise("e1", "squat-barbell", set("s1")))))
        val vm = subscribed(workouts)

        vm.onEvent(LiveWorkoutEvent.ValueChanged("s1", SetColumn.WEIGHT, "82,5"))

        assertEquals(82.5, workouts.storedSet().data.weightKg)
    }

    @Test
    fun inputColumnsFollowTheExerciseType() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(listOf(draft(exercise("e1", "plank", set("s1")))))
        val vm = subscribed(workouts)
        assertEquals(listOf(SetColumn.DURATION), vm.active.exercises[0].columns)

        vm.onEvent(LiveWorkoutEvent.ValueChanged("s1", SetColumn.DURATION, "130"))

        assertEquals(90, workouts.storedSet().data.durationS)
    }

    @Test
    fun setTypeMenuChangesTheTypeAndCloses() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(listOf(draft(exercise("e1", "squat-barbell", set("s1"), set("s2")))))
        val vm = subscribed(workouts)

        vm.onEvent(LiveWorkoutEvent.SetTypeClicked("s1"))
        assertEquals("s1", vm.active.setTypeMenuFor)
        vm.onEvent(LiveWorkoutEvent.SetTypeChosen("s1", SetType.WARMUP))

        assertNull(vm.active.setTypeMenuFor)
        assertEquals(SetType.WARMUP, workouts.storedSet().data.type)
        // The warm-up takes no number, so the first working set is still set 1.
        assertEquals(listOf(1, 1), vm.active.exercises[0].sets.map { it.number })
    }

    @Test
    fun addAndDeleteSets() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(listOf(draft(exercise("e1", "squat-barbell", set("s1")))))
        val vm = subscribed(workouts)

        vm.onEvent(LiveWorkoutEvent.AddSetClicked("e1"))
        assertEquals(2, vm.active.exercises[0].sets.size)

        vm.onEvent(LiveWorkoutEvent.SetDeleted("s1"))
        assertEquals(1, vm.active.exercises[0].sets.size)
        assertEquals(1, vm.active.exercises[0].sets[0].number)
    }

    @Test
    fun exercisesAreAddedReplacedReorderedAndRemoved() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(listOf(draft()))
        val vm = subscribed(workouts)
        val names = { vm.active.exercises.map { it.name } }

        vm.onEvent(LiveWorkoutEvent.ExercisesPicked(listOf("squat-barbell", "pull-up")))
        assertEquals(listOf("Squat (Barbell)", "Pull Up"), names())

        val squat = vm.active.exercises[0].id
        vm.onEvent(LiveWorkoutEvent.ReplacementPicked(squat, "bench-press-barbell"))
        assertEquals(listOf("Bench Press (Barbell)", "Pull Up"), names())

        vm.onEvent(LiveWorkoutEvent.ReorderClicked)
        assertTrue(vm.active.reorderOpen)
        vm.onEvent(LiveWorkoutEvent.ExerciseMoved(vm.active.exercises[1].id, 0))
        assertEquals(listOf("Pull Up", "Bench Press (Barbell)"), names())

        vm.onEvent(LiveWorkoutEvent.ExerciseMenuClicked(vm.active.exercises[0].id))
        vm.onEvent(LiveWorkoutEvent.RemoveClicked(vm.active.exercises[0].id))
        assertNull(vm.active.exerciseMenuFor)
        assertEquals(listOf("Bench Press (Barbell)"), names())
    }

    @Test
    fun keepAwakeFollowsTheSetting() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(listOf(draft()))
        val vm = subscribed(workouts)
        assertTrue(vm.active.keepScreenAwake)

        settings.setKeepScreenAwake(false)

        assertEquals(false, vm.active.keepScreenAwake)
    }
}
