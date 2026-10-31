package hu.toliver.muskli.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.ui.theme.MuskliTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun lastTimeInThisRoutineCanBeSelected() {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        compose.setContent {
            MuskliTheme {
                val state by vm.uiState.collectAsStateWithLifecycle()
                SettingsScreen(state = state, onEvent = vm::onEvent, onBack = {})
            }
        }
        compose.onNodeWithText("Last time anywhere").assertIsSelected()

        compose.onNodeWithText("Last time in this routine").performClick()

        compose.onNodeWithText("Last time in this routine").assertIsSelected()
        compose.onNodeWithText("Last time anywhere").assertIsNotSelected()
        assertEquals(PreviousMode.ROUTINE_SCOPED, repo.settings.value.previousMode)
    }
}
