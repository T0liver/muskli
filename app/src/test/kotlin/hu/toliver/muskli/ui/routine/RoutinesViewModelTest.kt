package hu.toliver.muskli.ui.routine

import hu.toliver.muskli.domain.model.PlannedSet
import hu.toliver.muskli.domain.model.Routine
import hu.toliver.muskli.domain.model.RoutineExercise
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.usecase.routine.ObserveRoutinesUseCase
import hu.toliver.muskli.domain.usecase.routine.StartWorkoutFromRoutineUseCase
import hu.toliver.muskli.domain.usecase.workout.StartWorkoutUseCase
import hu.toliver.muskli.ui.library.FakeExerciseTemplateRepository
import hu.toliver.muskli.ui.library.sampleLibrary
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = TestClock()
    private val bench = sampleLibrary[0]
    private val squat = sampleLibrary[2]

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun routine(id: String, title: String, vararg templateIds: String) = Routine(
        id = id,
        title = title,
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0,
        exercises = templateIds.map { RoutineExercise(it, sets = listOf(PlannedSet(weightKg = 60.0, reps = 5))) },
    )

    private fun TestScope.subscribed(routines: FakeRoutineRepository, workouts: FakeWorkoutRepository): RoutinesViewModel {
        var nextId = 0
        val vm = RoutinesViewModel(
            observeRoutines = ObserveRoutinesUseCase(routines),
            templates = FakeExerciseTemplateRepository(sampleLibrary),
            startWorkoutFromRoutine = StartWorkoutFromRoutineUseCase(routines, workouts, IdGenerator { "id${nextId++}" }, clock),
        )
        backgroundScope.launch { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun routinesAreListedByTitleWithTheirExerciseNames() = runTest(dispatcher) {
        val routines = FakeRoutineRepository(listOf(routine("b", "Push", bench.id), routine("a", "legs", squat.id, bench.id)))
        val vm = subscribed(routines, FakeWorkoutRepository())

        val state = vm.uiState.value
        assertFalse(state.loading)
        assertEquals(listOf("legs", "Push"), state.routines.map { it.title })
        assertEquals(listOf(squat.title, bench.title), state.routines.first().exerciseNames)
    }

    @Test
    fun noRoutinesIsAnEmptyLoadedList() = runTest(dispatcher) {
        val vm = subscribed(FakeRoutineRepository(), FakeWorkoutRepository())

        assertFalse(vm.uiState.value.loading)
        assertEquals(emptyList<RoutineSummaryUi>(), vm.uiState.value.routines)
    }

    @Test
    fun startCreatesADraftFromTheRoutineAndReportsIt() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository()
        val vm = subscribed(FakeRoutineRepository(listOf(routine("r", "Legs", squat.id, bench.id))), workouts)
        var started = false

        vm.startRoutine("r") { started = true }

        assertTrue(started)
        val draft = workouts.activeDraft!!
        assertEquals("r", draft.routineId)
        assertEquals(listOf(squat.id, bench.id), draft.exercises.map { it.exerciseTemplateId })
        assertEquals(60.0, draft.exercises.first().sets.single().data.weightKg)
        assertFalse(vm.uiState.value.inProgressShown)
    }

    @Test
    fun startWithADraftRunningCreatesNothingAndSaysSo() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository()
        StartWorkoutUseCase(workouts, IdGenerator { "running" }, clock)()
        val vm = subscribed(FakeRoutineRepository(listOf(routine("r", "Legs", squat.id))), workouts)
        var started = false

        vm.startRoutine("r") { started = true }

        assertFalse(started)
        assertTrue(vm.uiState.value.inProgressShown)
        assertEquals(listOf("running"), workouts.workouts.value.keys.toList())

        vm.onEvent(RoutinesEvent.InProgressDismissed)
        assertFalse(vm.uiState.value.inProgressShown)
    }
}
