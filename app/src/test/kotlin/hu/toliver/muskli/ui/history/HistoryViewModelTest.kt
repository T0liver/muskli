package hu.toliver.muskli.ui.history

import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.domain.usecase.workout.DeleteWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.RestoreWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.StartPastWorkoutUseCase
import hu.toliver.muskli.ui.library.FakeExerciseTemplateRepository
import hu.toliver.muskli.ui.library.sampleLibrary
import hu.toliver.muskli.ui.settings.FakeSettingsRepository
import hu.toliver.muskli.ui.workout.FakeWorkoutRepository
import hu.toliver.muskli.ui.workout.TestClock
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = TestClock()
    private val bench = sampleLibrary.first()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun finished(id: String, startedAt: Long, completed: Boolean = true) = Workout(
        id = id,
        title = id,
        notes = null,
        startedAtEpochMs = startedAt,
        utcOffsetS = 0,
        finishedAtEpochMs = startedAt + 3_600_000L,
        createdAtEpochMs = startedAt,
        updatedAtEpochMs = startedAt,
        deletedAtEpochMs = null,
        exercises = listOf(
            WorkoutExercise(
                "$id-e",
                bench.id,
                supersetGroup = null,
                notes = null,
                restSeconds = null,
                sets = listOf(WorkoutSet("$id-s", SetData(weightKg = 100.0, reps = 5, completed = completed), null)),
            ),
        ),
    )

    private fun TestScope.subscribed(workouts: FakeWorkoutRepository): HistoryViewModel {
        var nextId = 0
        val vm = HistoryViewModel(
            workouts = workouts,
            templates = FakeExerciseTemplateRepository(sampleLibrary),
            settingsRepository = FakeSettingsRepository(),
            deleteWorkout = DeleteWorkoutUseCase(workouts, clock),
            restoreWorkout = RestoreWorkoutUseCase(workouts),
            startPastWorkout = StartPastWorkoutUseCase(workouts, IdGenerator { "past${nextId++}" }, clock),
        )
        backgroundScope.launch { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun finishedWorkoutsAreListedNewestFirstWithTheirStats() = runTest(dispatcher) {
        val vm = subscribed(FakeWorkoutRepository(listOf(finished("old", 1_000L), finished("new", 9_000_000L))))

        val workouts = vm.uiState.value.workouts
        assertEquals(listOf("new", "old"), workouts.map { it.id })
        assertEquals("500", workouts.first().volume)
        assertEquals(1, workouts.first().completedSets)
        assertEquals(3_600L, workouts.first().durationS)
        assertEquals(listOf(bench.title), workouts.first().exerciseNames)
    }

    @Test
    fun summaryVolumeCountsBothSidesOfAUnilateralExerciseOnly() {
        val lunge = bench.copy(id = "lunge", title = "Lunge", unilateral = true)
        val workout = finished("w", 1_000L).let { w ->
            w.copy(
                exercises = w.exercises + WorkoutExercise(
                    "w-e2",
                    lunge.id,
                    supersetGroup = null,
                    notes = null,
                    restSeconds = null,
                    sets = listOf(WorkoutSet("w-s2", SetData(weightKg = 20.0, reps = 10, completed = true), null)),
                ),
            )
        }

        val summary = summarizeWorkout(workout, listOf(bench, lunge).associateBy { it.id }, WeightUnit.KG, bodyWeightKg = null)

        assertEquals("900", summary.volume)
    }

    @Test
    fun deleteHidesTheWorkoutAndUndoRestoresIt() = runTest(dispatcher) {
        val vm = subscribed(FakeWorkoutRepository(listOf(finished("a", 1_000L), finished("b", 2_000L))))

        vm.onEvent(HistoryEvent.DeleteClicked("b"))
        assertEquals(listOf("a"), vm.uiState.value.workouts.map { it.id })
        assertEquals("b", vm.uiState.value.undoWorkoutId)

        vm.onEvent(HistoryEvent.UndoClicked)
        assertEquals(listOf("b", "a"), vm.uiState.value.workouts.map { it.id })
        assertNull(vm.uiState.value.undoWorkoutId)
    }

    @Test
    fun aNewPastWorkoutLeftWithNothingCompletedIsDropped() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository()
        val vm = subscribed(workouts)
        var created: String? = null
        vm.logPastWorkout(startedAtEpochMs = 5_000L) { created = it }
        var discarded: Boolean? = null

        vm.leaveNewPastWorkout(created!!) { discarded = it }

        assertEquals(true, discarded)
        assertEquals(emptyList<String>(), vm.uiState.value.workouts.map { it.id })
    }

    @Test
    fun aNewPastWorkoutWithACompletedSetIsKept() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(listOf(finished("p", 5_000L)))
        val vm = subscribed(workouts)
        var discarded: Boolean? = null

        vm.leaveNewPastWorkout("p") { discarded = it }

        assertEquals(false, discarded)
        assertEquals(listOf("p"), vm.uiState.value.workouts.map { it.id })
    }
}
