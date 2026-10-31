package hu.toliver.muskli.ui.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.ui.library.FakeExerciseTemplateRepository
import hu.toliver.muskli.ui.library.LibraryMode
import hu.toliver.muskli.ui.library.LibraryScreen
import hu.toliver.muskli.ui.library.SEARCH_FIELD_TAG
import hu.toliver.muskli.ui.library.libraryViewModel
import hu.toliver.muskli.ui.library.sampleLibrary
import hu.toliver.muskli.ui.settings.FakeSettingsRepository
import hu.toliver.muskli.ui.theme.MuskliTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveWorkoutScreenTest {
    @get:Rule val compose = createComposeRule()

    private val templates = FakeExerciseTemplateRepository(sampleLibrary)
    private val workouts = FakeWorkoutRepository()

    @Test
    fun addExerciseEnterValuesAndCompleteSet() {
        val workout = workoutSessionViewModel(workouts, templates, FakeSettingsRepository())
        val library = libraryViewModel(templates)
        workout.onEvent(LiveWorkoutEvent.StartClicked)
        compose.setContent {
            MuskliTheme {
                var picking by remember { mutableStateOf(false) }
                if (picking) {
                    val state by library.uiState.collectAsStateWithLifecycle()
                    LibraryScreen(state = state, mode = LibraryMode.PICK, onEvent = library::onEvent, onBack = {}, onPicked = {
                        workout.onEvent(LiveWorkoutEvent.ExercisesPicked(it))
                        picking = false
                    })
                } else {
                    LiveWorkoutRoute(
                        viewModel = workout,
                        onAddExercise = { picking = true },
                        onReplaceExercise = {},
                        onFinish = {},
                        onExerciseHistory = {},
                    )
                }
            }
        }

        compose.onNodeWithText("Add exercise").performClick()
        compose.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("squat")
        compose.onNodeWithText("Squat (Barbell)").performClick()
        compose.onNodeWithText("Add 1").performClick()

        compose.onNodeWithText("Squat (Barbell)").assertExists()
        compose.onNodeWithContentDescription("Weight, set 1").performTextInput("100")
        compose.onNodeWithContentDescription("Reps, set 1").performTextInput("5")
        compose.onNodeWithContentDescription("Complete set 1").performClick()

        compose.onNodeWithContentDescription("Complete set 1").assertIsOn()
        compose.onNodeWithText("500 kg").assertExists()
        val stored = workouts.activeDraft!!.exercises.single().sets.single()
        assertEquals(SetData(weightKg = 100.0, reps = 5, completed = true), stored.data)
    }

    @Test
    fun anExerciseDoneBeforeShowsPreviousValuesAndLogsAPrefilledSetInOneTap() {
        workouts.history.value = mapOf("squat-barbell" to listOf(lastTime(done(60.0, 10), done(80.0, 8))))
        val workout = workoutSessionViewModel(workouts, templates, FakeSettingsRepository())
        workout.onEvent(LiveWorkoutEvent.StartClicked)
        workout.onEvent(LiveWorkoutEvent.ExercisesPicked(listOf("squat-barbell")))
        compose.setContent {
            MuskliTheme {
                LiveWorkoutRoute(viewModel = workout, onAddExercise = {}, onReplaceExercise = {}, onFinish = {}, onExerciseHistory = {})
            }
        }

        compose.onNodeWithText("60 × 10").assertExists()
        compose.onNodeWithText("80 × 8").assertExists()
        compose.onNodeWithContentDescription("Complete set 2").assertIsOff().performClick()

        compose.onNodeWithContentDescription("Complete set 2").assertIsOn()
        compose.onNodeWithText("640 kg").assertExists()
    }

    @Test
    fun setFieldsAnnounceTheirValueAndEmptyFieldsOnlyTheirName() {
        workouts.history.value = mapOf("squat-barbell" to listOf(lastTime(done(80.0, 8))))
        val workout = workoutSessionViewModel(workouts, templates, FakeSettingsRepository())
        workout.onEvent(LiveWorkoutEvent.StartClicked)
        workout.onEvent(LiveWorkoutEvent.ExercisesPicked(listOf("squat-barbell", "bench-press-barbell")))
        compose.setContent {
            MuskliTheme {
                LiveWorkoutRoute(viewModel = workout, onAddExercise = {}, onReplaceExercise = {}, onFinish = {}, onExerciseHistory = {})
            }
        }

        compose.onNodeWithContentDescription("Weight, set 1, 80").assertExists()
        compose.onNodeWithContentDescription("Reps, set 1, 8").assertExists()

        compose.onNodeWithContentDescription("Weight, set 1").performTextInput("100")
        compose.onNodeWithContentDescription("Weight, set 1, 100").assertExists()
        compose.onNodeWithContentDescription("Reps, set 1").assertExists()
        compose.onAllNodesWithContentDescription("Complete set 1").assertCountEquals(2)
    }

    @Test
    fun completingASetShowsTheRestBarAndSkipHidesItButNotBeforeADropSet() {
        workouts.history.value = mapOf("squat-barbell" to listOf(lastTime(done(80.0, 8), done(80.0, 8), done(60.0, 8))))
        val workout = workoutSessionViewModel(workouts, templates, FakeSettingsRepository())
        workout.onEvent(LiveWorkoutEvent.StartClicked)
        workout.onEvent(LiveWorkoutEvent.ExercisesPicked(listOf("squat-barbell")))
        workout.onEvent(LiveWorkoutEvent.SetTypeChosen(workouts.activeDraft!!.exercises[0].sets[2].id, SetType.DROPSET))
        compose.setContent {
            MuskliTheme {
                LiveWorkoutRoute(viewModel = workout, onAddExercise = {}, onReplaceExercise = {}, onFinish = {}, onExerciseHistory = {})
            }
        }

        compose.onNodeWithText("Skip").assertDoesNotExist()
        compose.onNodeWithContentDescription("Complete set 1").performClick()
        compose.onNodeWithText("1:30").assertExists()
        compose.onNodeWithText("Skip").performClick()
        compose.onNodeWithText("Skip").assertDoesNotExist()

        compose.onNodeWithContentDescription("Complete set 2").performClick()
        compose.onNodeWithText("Skip").assertDoesNotExist()
    }
}
