package hu.toliver.muskli.ui.library

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.ui.theme.MuskliTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun searchThenSelectReturnsChosenIds() {
        val vm = libraryViewModel(FakeExerciseTemplateRepository(sampleLibrary))
        var picked: List<String>? = null
        compose.setContent {
            MuskliTheme {
                val state by vm.uiState.collectAsStateWithLifecycle()
                LibraryScreen(state = state, mode = LibraryMode.PICK, onEvent = vm::onEvent, onBack = {}, onPicked = { picked = it })
            }
        }
        compose.onNodeWithText("Add 0").assertIsNotEnabled()

        compose.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("squat")
        compose.onNodeWithText("Bench Press (Barbell)").assertDoesNotExist()
        compose.onNodeWithText("Squat (Barbell)").performClick()

        compose.onNodeWithText("Add 1").assertIsEnabled().performClick()
        assertEquals(listOf("squat-barbell"), picked)
    }

    @Test
    fun replaceReturnsTheOneTappedExercise() {
        val picks = mutableListOf<List<String>>()
        showReplace(libraryViewModel(FakeExerciseTemplateRepository(sampleLibrary))) { picks += it }

        compose.onNodeWithText("Add 0").assertDoesNotExist()
        compose.onNodeWithText("Bench Press (Dumbbell)").performClick()

        assertEquals(listOf(listOf("bench-press-dumbbell")), picks)
    }

    @Test
    fun replaceCannotPickTheCurrentExercise() {
        val picks = mutableListOf<List<String>>()
        showReplace(libraryViewModel(FakeExerciseTemplateRepository(sampleLibrary))) { picks += it }

        compose.onNodeWithText("Current").assertExists()
        compose.onNodeWithText("Pull Up").assertIsNotEnabled()
        compose.onNodeWithText("Pull Up").performClick()

        assertEquals(emptyList<List<String>>(), picks)
    }

    @Test
    fun replaceWithANewCustomExerciseReturnsIt() {
        val vm = libraryViewModel(FakeExerciseTemplateRepository(sampleLibrary))
        val picks = mutableListOf<List<String>>()
        showReplace(vm) { picks += it }

        vm.onEvent(LibraryEvent.CreateClicked)
        vm.onEvent(LibraryEvent.EditorTitleChanged("Cable Fly"))
        vm.onEvent(LibraryEvent.EditorPrimaryMuscleChanged(MuscleGroup.CHEST))
        vm.onEvent(LibraryEvent.EditorEquipmentChanged(Equipment.MACHINE))
        vm.onEvent(LibraryEvent.EditorSave)
        compose.waitForIdle()

        assertEquals(listOf(listOf("custom-0")), picks)
    }

    private fun showReplace(vm: LibraryViewModel, onPicked: (List<String>) -> Unit) {
        compose.setContent {
            MuskliTheme {
                val state by vm.uiState.collectAsStateWithLifecycle()
                LibraryScreen(
                    state = state,
                    mode = LibraryMode.REPLACE,
                    onEvent = vm::onEvent,
                    onBack = {},
                    onPicked = onPicked,
                    currentId = "pull-up",
                )
            }
        }
        compose.waitUntil { compose.onAllNodesWithText("Pull Up").fetchSemanticsNodes().isNotEmpty() }
    }
}
