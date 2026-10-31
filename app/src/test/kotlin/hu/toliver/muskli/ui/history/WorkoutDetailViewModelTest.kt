package hu.toliver.muskli.ui.history

import hu.toliver.muskli.domain.model.PlannedSet
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.usecase.routine.SaveWorkoutAsRoutineUseCase
import hu.toliver.muskli.ui.library.FakeExerciseTemplateRepository
import hu.toliver.muskli.ui.library.sampleLibrary
import hu.toliver.muskli.ui.routine.FakeRoutineRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutDetailViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = TestClock()
    private val bench = sampleLibrary.first()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val workout = Workout(
        id = "w",
        title = "Push day",
        notes = null,
        startedAtEpochMs = 1_000L,
        utcOffsetS = 0,
        finishedAtEpochMs = 3_601_000L,
        createdAtEpochMs = 1_000L,
        updatedAtEpochMs = 1_000L,
        deletedAtEpochMs = null,
        exercises = listOf(
            WorkoutExercise(
                "e",
                bench.id,
                supersetGroup = null,
                notes = null,
                restSeconds = 120,
                sets = listOf(WorkoutSet("s", SetData(weightKg = 80.0, reps = 8, completed = true), null)),
            ),
        ),
    )

    private fun TestScope.subscribed(routines: FakeRoutineRepository): WorkoutDetailViewModel {
        val workouts = FakeWorkoutRepository(listOf(workout))
        val vm = WorkoutDetailViewModel(
            workoutId = "w",
            workouts = workouts,
            templates = FakeExerciseTemplateRepository(sampleLibrary),
            settingsRepository = FakeSettingsRepository(),
            saveWorkoutAsRoutine = SaveWorkoutAsRoutineUseCase(workouts, routines, IdGenerator { "r" }, clock),
        )
        backgroundScope.launch { vm.uiState.collect {} }
        return vm
    }

    private val WorkoutDetailViewModel.ready get() = uiState.value as WorkoutDetailUiState.Ready

    @Test
    fun saveAsRoutineOffersTheWorkoutTitleAndStoresTheRoutine() = runTest(dispatcher) {
        val routines = FakeRoutineRepository()
        val vm = subscribed(routines)

        vm.onEvent(WorkoutDetailEvent.SaveAsRoutineClicked)
        assertEquals("Push day", vm.ready.saveAsRoutine?.title)

        vm.onEvent(WorkoutDetailEvent.SaveAsRoutineTitleChanged("Push A"))
        vm.onEvent(WorkoutDetailEvent.SaveAsRoutineConfirmed)

        assertNull(vm.ready.saveAsRoutine)
        assertTrue(vm.ready.routineSaved)
        val saved = routines.routines.value.getValue("r")
        assertEquals("Push A", saved.title)
        assertEquals(listOf(PlannedSet(weightKg = 80.0, reps = 8)), saved.exercises.single().sets)
        assertEquals(120, saved.exercises.single().restSeconds)

        vm.onEvent(WorkoutDetailEvent.RoutineSavedShown)
        assertFalse(vm.ready.routineSaved)
    }

    @Test
    fun aBlankNameKeepsTheDialogOpenAndSavesNothing() = runTest(dispatcher) {
        val routines = FakeRoutineRepository()
        val vm = subscribed(routines)
        vm.onEvent(WorkoutDetailEvent.SaveAsRoutineClicked)
        vm.onEvent(WorkoutDetailEvent.SaveAsRoutineTitleChanged("  "))

        vm.onEvent(WorkoutDetailEvent.SaveAsRoutineConfirmed)

        assertEquals(true, vm.ready.saveAsRoutine?.showError)
        assertTrue(routines.routines.value.isEmpty())

        vm.onEvent(WorkoutDetailEvent.SaveAsRoutineDismissed)
        assertNull(vm.ready.saveAsRoutine)
    }
}
