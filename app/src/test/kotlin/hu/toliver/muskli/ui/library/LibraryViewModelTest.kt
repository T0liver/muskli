package hu.toliver.muskli.ui.library

import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
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
class LibraryViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repo = FakeExerciseTemplateRepository(sampleLibrary)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.subscribed(): LibraryViewModel =
        libraryViewModel(repo).also { vm -> backgroundScope.launch { vm.uiState.collect {} } }

    private val LibraryViewModel.resultIds get() = uiState.value.results.map { it.id }

    @Test
    fun showsWholeLibrarySortedByTitle() = runTest(dispatcher) {
        val vm = subscribed()

        assertEquals(listOf("bench-press-barbell", "bench-press-dumbbell", "pull-up", "squat-barbell"), vm.resultIds)
    }

    @Test
    fun searchIsCaseAndDiacriticInsensitive() = runTest(dispatcher) {
        val vm = subscribed()

        vm.onEvent(LibraryEvent.QueryChanged("BÉNCH"))

        assertEquals("BÉNCH", vm.uiState.value.query)
        assertEquals(listOf("bench-press-barbell", "bench-press-dumbbell"), vm.resultIds)
    }

    @Test
    fun muscleAndEquipmentFiltersCombine() = runTest(dispatcher) {
        val vm = subscribed()

        vm.onEvent(LibraryEvent.MuscleFilterChanged(MuscleGroup.CHEST))
        vm.onEvent(LibraryEvent.EquipmentFilterChanged(Equipment.DUMBBELL))
        assertEquals(listOf("bench-press-dumbbell"), vm.resultIds)

        vm.onEvent(LibraryEvent.EquipmentFilterChanged(null))
        assertEquals(listOf("bench-press-barbell", "bench-press-dumbbell"), vm.resultIds)
    }

    @Test
    fun recentsShowOnlyWithoutQueryOrFilter() = runTest(dispatcher) {
        repo.recentIds.value = listOf("squat-barbell", "pull-up")
        val vm = subscribed()

        assertEquals(listOf("squat-barbell", "pull-up"), vm.uiState.value.recents.map { it.id })

        vm.onEvent(LibraryEvent.QueryChanged("squat"))
        assertTrue(vm.uiState.value.recents.isEmpty())

        vm.onEvent(LibraryEvent.QueryChanged(""))
        vm.onEvent(LibraryEvent.MuscleFilterChanged(MuscleGroup.LATS))
        assertTrue(vm.uiState.value.recents.isEmpty())
    }

    @Test
    fun multiSelectKeepsTapOrderAndToggles() = runTest(dispatcher) {
        val vm = subscribed()

        vm.onEvent(LibraryEvent.SelectionToggled("squat-barbell"))
        vm.onEvent(LibraryEvent.SelectionToggled("bench-press-barbell"))
        vm.onEvent(LibraryEvent.SelectionToggled("pull-up"))
        vm.onEvent(LibraryEvent.SelectionToggled("bench-press-barbell"))

        assertEquals(listOf("squat-barbell", "pull-up"), vm.uiState.value.selectedIds)
    }

    @Test
    fun selectionSurvivesSearchChanges() = runTest(dispatcher) {
        val vm = subscribed()

        vm.onEvent(LibraryEvent.SelectionToggled("pull-up"))
        vm.onEvent(LibraryEvent.QueryChanged("bench"))

        assertEquals(listOf("pull-up"), vm.uiState.value.selectedIds)
    }

    @Test
    fun saveWithMissingFieldsShowsErrorsAndCreatesNothing() = runTest(dispatcher) {
        val vm = subscribed()

        vm.onEvent(LibraryEvent.CreateClicked)
        vm.onEvent(LibraryEvent.EditorTitleChanged("  "))
        vm.onEvent(LibraryEvent.EditorSave)

        val editor = vm.uiState.value.editor!!
        assertTrue(editor.showErrors)
        assertTrue(editor.titleMissing && editor.primaryMuscleMissing && editor.equipmentMissing)
        assertEquals(sampleLibrary.size, repo.templates.value.size)
    }

    @Test
    fun createCustomExerciseAddsItAndSelectsIt() = runTest(dispatcher) {
        val vm = subscribed()

        vm.onEvent(LibraryEvent.CreateClicked)
        vm.onEvent(LibraryEvent.EditorTitleChanged("Landmine Press"))
        vm.onEvent(LibraryEvent.EditorTypeChanged(ExerciseType.WEIGHT_REPS))
        vm.onEvent(LibraryEvent.EditorPrimaryMuscleChanged(MuscleGroup.SHOULDERS))
        vm.onEvent(LibraryEvent.EditorSecondaryMuscleToggled(MuscleGroup.TRICEPS))
        vm.onEvent(LibraryEvent.EditorSecondaryMuscleToggled(MuscleGroup.CHEST))
        vm.onEvent(LibraryEvent.EditorSecondaryMuscleToggled(MuscleGroup.CHEST))
        vm.onEvent(LibraryEvent.EditorEquipmentChanged(Equipment.BARBELL))
        vm.onEvent(LibraryEvent.EditorSave)

        val created = repo.templates.value.getValue("custom-0")
        assertEquals("Landmine Press", created.title)
        assertTrue(created.isCustom)
        assertEquals(MuscleGroup.SHOULDERS, created.primaryMuscle)
        assertEquals(setOf(MuscleGroup.TRICEPS), created.secondaryMuscles)
        assertEquals(Equipment.BARBELL, created.equipment)
        assertNull(vm.uiState.value.editor)
        assertTrue("custom-0" in vm.resultIds)
        assertEquals(listOf("custom-0"), vm.uiState.value.selectedIds)
    }

    @Test
    fun choosingPrimaryMuscleDropsItFromSecondaries() = runTest(dispatcher) {
        val vm = subscribed()

        vm.onEvent(LibraryEvent.CreateClicked)
        vm.onEvent(LibraryEvent.EditorSecondaryMuscleToggled(MuscleGroup.CHEST))
        vm.onEvent(LibraryEvent.EditorPrimaryMuscleChanged(MuscleGroup.CHEST))

        assertTrue(vm.uiState.value.editor!!.secondaryMuscles.isEmpty())
    }

    @Test
    fun editCustomExerciseLoadsAndSavesChanges() = runTest(dispatcher) {
        repo.templates.value += "c1" to template("c1", "Cable Fly", equipment = Equipment.MACHINE, custom = true)
        val vm = subscribed()

        vm.onEvent(LibraryEvent.EditClicked("c1"))
        assertEquals("Cable Fly", vm.uiState.value.editor!!.title)
        assertEquals("c1", vm.uiState.value.editor!!.editingId)

        vm.onEvent(LibraryEvent.EditorTitleChanged("Cable Fly (Low)"))
        vm.onEvent(LibraryEvent.EditorSave)

        assertEquals("Cable Fly (Low)", repo.templates.value.getValue("c1").title)
        assertNull(vm.uiState.value.editor)
    }

    @Test
    fun catalogueExerciseCannotBeEdited() = runTest(dispatcher) {
        val vm = subscribed()

        vm.onEvent(LibraryEvent.EditClicked("bench-press-barbell"))

        assertNull(vm.uiState.value.editor)
    }

    @Test
    fun archiveHidesItAndDropsItFromSelection() = runTest(dispatcher) {
        repo.templates.value += "c1" to template("c1", "Cable Fly", custom = true)
        val vm = subscribed()
        vm.onEvent(LibraryEvent.SelectionToggled("c1"))

        vm.onEvent(LibraryEvent.EditClicked("c1"))
        vm.onEvent(LibraryEvent.EditorArchive)

        assertTrue(repo.templates.value.getValue("c1").archived)
        assertFalse("c1" in vm.resultIds)
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
        assertNull(vm.uiState.value.editor)
    }

    @Test
    fun dismissClosesTheFormWithoutSaving() = runTest(dispatcher) {
        val vm = subscribed()

        vm.onEvent(LibraryEvent.CreateClicked)
        vm.onEvent(LibraryEvent.EditorTitleChanged("Draft"))
        vm.onEvent(LibraryEvent.EditorDismiss)

        assertNull(vm.uiState.value.editor)
        assertEquals(sampleLibrary.size, repo.templates.value.size)
    }
}
