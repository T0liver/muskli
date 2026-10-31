package hu.toliver.muskli.ui.routine

import hu.toliver.muskli.domain.model.PlannedSet
import hu.toliver.muskli.domain.model.Routine
import hu.toliver.muskli.domain.model.RoutineExercise
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.repo.IdGenerator
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.domain.usecase.routine.CreateRoutineUseCase
import hu.toliver.muskli.domain.usecase.routine.DeleteRoutineUseCase
import hu.toliver.muskli.domain.usecase.routine.UpdateRoutineUseCase
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.library.FakeExerciseTemplateRepository
import hu.toliver.muskli.ui.library.sampleLibrary
import hu.toliver.muskli.ui.settings.FakeSettingsRepository
import hu.toliver.muskli.ui.workout.TestClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditorViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = TestClock()
    private val bench = sampleLibrary[0]
    private val squat = sampleLibrary[2]

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        routines: FakeRoutineRepository,
        routineId: String? = null,
        settings: Settings = Settings(),
    ) = RoutineEditorViewModel(
        routineId = routineId,
        routines = routines,
        templates = FakeExerciseTemplateRepository(sampleLibrary),
        settingsRepository = FakeSettingsRepository(settings),
        createRoutine = CreateRoutineUseCase(routines, IdGenerator { "new" }, clock),
        updateRoutine = UpdateRoutineUseCase(routines, clock),
        deleteRoutine = DeleteRoutineUseCase(routines),
    )

    private val RoutineEditorViewModel.ready get() = uiState.value as RoutineEditorUiState.Ready

    private fun RoutineEditorViewModel.exercise(index: Int) = ready.exercises[index]

    @Test
    fun aNewRoutineWithTwoExercisesSavesTheirPlannedSetsAndRest() = runTest(dispatcher) {
        val routines = FakeRoutineRepository()
        val vm = viewModel(routines)
        vm.onEvent(RoutineEditorEvent.TitleChanged("  Legs "))
        vm.onEvent(RoutineEditorEvent.ExercisesPicked(listOf(squat.id, bench.id)))

        val squatKey = vm.exercise(0).key
        val firstSet = vm.exercise(0).sets.single().key
        vm.onEvent(RoutineEditorEvent.ValueChanged(squatKey, firstSet, SetColumn.WEIGHT, "100"))
        vm.onEvent(RoutineEditorEvent.ValueChanged(squatKey, firstSet, SetColumn.REPS, "5"))
        vm.onEvent(RoutineEditorEvent.AddSetClicked(squatKey))
        vm.onEvent(RoutineEditorEvent.RestChanged(squatKey, "180"))
        vm.onEvent(RoutineEditorEvent.SaveClicked)

        assertEquals(RoutineEditorUiState.Done, vm.uiState.value)
        val saved = routines.routines.value.getValue("new")
        assertEquals("Legs", saved.title)
        assertEquals(listOf(squat.id, bench.id), saved.exercises.map { it.exerciseTemplateId })
        assertEquals(listOf(PlannedSet(weightKg = 100.0, reps = 5), PlannedSet(weightKg = 100.0, reps = 5)), saved.exercises[0].sets)
        assertEquals(180, saved.exercises[0].restSeconds)
        assertEquals(listOf(PlannedSet()), saved.exercises[1].sets)
        assertNull(saved.exercises[1].restSeconds)
    }

    @Test
    fun aBlankNameOrInvalidRestIsNotSaved() = runTest(dispatcher) {
        val routines = FakeRoutineRepository()
        val vm = viewModel(routines)
        vm.onEvent(RoutineEditorEvent.ExercisesPicked(listOf(bench.id)))

        vm.onEvent(RoutineEditorEvent.SaveClicked)
        assertTrue(vm.ready.showErrors)
        assertTrue(vm.ready.titleMissing)

        vm.onEvent(RoutineEditorEvent.TitleChanged("Push"))
        vm.onEvent(RoutineEditorEvent.RestChanged(vm.exercise(0).key, "-5"))
        vm.onEvent(RoutineEditorEvent.SaveClicked)
        assertTrue(vm.exercise(0).restInvalid)
        assertEquals(emptyMap<String, Routine>(), routines.routines.value)
    }

    @Test
    fun editingLoadsTheRoutineInTheDisplayUnitAndKeepsWhatItDoesNotShow() = runTest(dispatcher) {
        val stored = Routine(
            id = "r",
            title = "Push",
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0,
            exercises = listOf(
                RoutineExercise(
                    bench.id,
                    supersetGroup = 1,
                    notes = "Pause at the chest",
                    restSeconds = 0,
                    sets = listOf(PlannedSet(SetType.WARMUP, weightKg = 20.0, reps = 10), PlannedSet(weightKg = 60.0, reps = 8, durationS = 30)),
                ),
            ),
        )
        val routines = FakeRoutineRepository(listOf(stored))
        val vm = viewModel(routines, routineId = "r", settings = Settings(weightUnit = WeightUnit.LB))

        val shown = vm.exercise(0)
        assertEquals(bench.title, shown.name)
        assertEquals("0", shown.rest)
        assertEquals(listOf("44.09", "10"), shown.sets[0].values)
        assertEquals(listOf(SetType.WARMUP to 1, SetType.NORMAL to 1), shown.sets.map { it.type to it.number })

        vm.onEvent(RoutineEditorEvent.ValueChanged(shown.key, shown.sets[1].key, SetColumn.REPS, "6"))
        vm.onEvent(RoutineEditorEvent.SaveClicked)

        val saved = routines.routines.value.getValue("r").exercises.single()
        assertEquals(1, saved.supersetGroup)
        assertEquals("Pause at the chest", saved.notes)
        assertEquals(0, saved.restSeconds)
        assertEquals(PlannedSet(weightKg = 60.0, reps = 6, durationS = 30), saved.sets[1])
        assertEquals(PlannedSet(SetType.WARMUP, weightKg = 20.0, reps = 10), saved.sets[0])
    }

    @Test
    fun exercisesMoveUpAndDownAndCanBeRemoved() = runTest(dispatcher) {
        val vm = viewModel(FakeRoutineRepository())
        vm.onEvent(RoutineEditorEvent.ExercisesPicked(listOf(bench.id, squat.id)))

        vm.onEvent(RoutineEditorEvent.ExerciseMoved(vm.exercise(1).key, 0))
        assertEquals(listOf(squat.title, bench.title), vm.ready.exercises.map { it.name })

        vm.onEvent(RoutineEditorEvent.ExerciseMoved(vm.exercise(0).key, -1))
        assertEquals(listOf(squat.title, bench.title), vm.ready.exercises.map { it.name })

        vm.onEvent(RoutineEditorEvent.ExerciseRemoved(vm.exercise(0).key))
        assertEquals(listOf(bench.title), vm.ready.exercises.map { it.name })
    }

    @Test
    fun setsChangeTypeAndAreDeleted() = runTest(dispatcher) {
        val vm = viewModel(FakeRoutineRepository())
        vm.onEvent(RoutineEditorEvent.ExercisesPicked(listOf(bench.id)))
        val key = vm.exercise(0).key
        vm.onEvent(RoutineEditorEvent.SetTypeChosen(key, vm.exercise(0).sets[0].key, SetType.WARMUP))
        vm.onEvent(RoutineEditorEvent.AddSetClicked(key))

        assertEquals(listOf(SetType.WARMUP, SetType.NORMAL), vm.exercise(0).sets.map { it.type })

        vm.onEvent(RoutineEditorEvent.SetDeleted(key, vm.exercise(0).sets[0].key))
        assertEquals(listOf(SetType.NORMAL to 1), vm.exercise(0).sets.map { it.type to it.number })
    }

    @Test
    fun deleteRemovesTheRoutineOnlyAfterConfirmation() = runTest(dispatcher) {
        val routines = FakeRoutineRepository(listOf(Routine("r", "Push", 0, 0, emptyList())))
        val vm = viewModel(routines, routineId = "r")

        vm.onEvent(RoutineEditorEvent.DeleteClicked)
        assertTrue(vm.ready.confirmDelete)
        vm.onEvent(RoutineEditorEvent.DeleteDismissed)
        assertEquals(setOf("r"), routines.routines.value.keys)

        vm.onEvent(RoutineEditorEvent.DeleteClicked)
        vm.onEvent(RoutineEditorEvent.DeleteConfirmed)
        assertEquals(RoutineEditorUiState.Done, vm.uiState.value)
        assertEquals(emptySet<String>(), routines.routines.value.keys)
    }

    @Test
    fun aMissingRoutineShowsMissing() = runTest(dispatcher) {
        val vm = viewModel(FakeRoutineRepository(), routineId = "gone")

        assertEquals(RoutineEditorUiState.Missing, vm.uiState.value)
    }
}
