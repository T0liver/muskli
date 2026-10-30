package hu.toliver.muskli.ui.settings

import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.settings.Settings
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
class SettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun defaultsShowD009AndUnsetBodyWeight() = runTest(dispatcher) {
        val vm = SettingsViewModel(FakeSettingsRepository())
        backgroundScope.launch { vm.uiState.collect {} }

        assertEquals(SettingsUiState(), vm.uiState.value)
        assertEquals("90", vm.uiState.value.restSecondsText)
        assertEquals("", vm.uiState.value.bodyWeightText)
    }

    @Test
    fun togglesAndChoicesAreSaved() = runTest(dispatcher) {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }

        vm.onEvent(SettingsEvent.WeightUnitChanged(WeightUnit.LB))
        vm.onEvent(SettingsEvent.KeepScreenAwakeChanged(false))
        vm.onEvent(SettingsEvent.SoundChanged(false))
        vm.onEvent(SettingsEvent.VibrationChanged(false))
        vm.onEvent(SettingsEvent.WeekStartChanged(WeekStartDay.SATURDAY))
        vm.onEvent(SettingsEvent.PreviousModeChanged(PreviousMode.ANYWHERE))

        assertEquals(
            Settings(
                weightUnit = WeightUnit.LB,
                keepScreenAwake = false,
                soundEnabled = false,
                vibrationEnabled = false,
                weekStart = WeekStartDay.SATURDAY,
            ),
            repo.settings.value,
        )
        assertEquals(WeekStartDay.SATURDAY, vm.uiState.value.weekStart)
    }

    @Test
    fun restOffAndOnAgainRestoresDefault() = runTest(dispatcher) {
        val repo = FakeSettingsRepository(Settings(defaultRestSeconds = 120))
        val vm = SettingsViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }

        vm.onEvent(SettingsEvent.RestEnabledChanged(false))
        assertNull(repo.settings.value.defaultRestSeconds)
        assertFalse(vm.uiState.value.restEnabled)

        vm.onEvent(SettingsEvent.RestEnabledChanged(true))
        assertEquals(90, repo.settings.value.defaultRestSeconds)
    }

    @Test
    fun invalidRestIsShownButNotSaved() = runTest(dispatcher) {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }

        vm.onEvent(SettingsEvent.RestSecondsChanged("0"))
        assertTrue(vm.uiState.value.restSecondsInvalid)
        assertEquals("0", vm.uiState.value.restSecondsText)
        assertEquals(90, repo.settings.value.defaultRestSeconds)

        vm.onEvent(SettingsEvent.RestSecondsChanged("135"))
        assertFalse(vm.uiState.value.restSecondsInvalid)
        assertEquals(135, repo.settings.value.defaultRestSeconds)
    }

    @Test
    fun bodyWeightIsStoredInKgWhateverTheUnit() = runTest(dispatcher) {
        val repo = FakeSettingsRepository(Settings(weightUnit = WeightUnit.LB))
        val vm = SettingsViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }

        vm.onEvent(SettingsEvent.BodyWeightChanged("220,46"))

        assertEquals(100.0, repo.settings.value.bodyWeightKg!!, 0.01)
        assertEquals("220,46", vm.uiState.value.bodyWeightText)

        vm.onEvent(SettingsEvent.WeightUnitChanged(WeightUnit.KG))
        assertEquals("100", vm.uiState.value.bodyWeightText)
    }

    @Test
    fun clearingBodyWeightUnsetsIt() = runTest(dispatcher) {
        val repo = FakeSettingsRepository(Settings(bodyWeightKg = 80.0))
        val vm = SettingsViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }
        assertEquals("80", vm.uiState.value.bodyWeightText)

        vm.onEvent(SettingsEvent.BodyWeightChanged(""))

        assertNull(repo.settings.value.bodyWeightKg)
        assertFalse(vm.uiState.value.bodyWeightInvalid)
    }

    @Test
    fun invalidBodyWeightIsShownButNotSaved() = runTest(dispatcher) {
        val repo = FakeSettingsRepository(Settings(bodyWeightKg = 80.0))
        val vm = SettingsViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }

        vm.onEvent(SettingsEvent.BodyWeightChanged("-5"))

        assertTrue(vm.uiState.value.bodyWeightInvalid)
        assertEquals(80.0, repo.settings.value.bodyWeightKg!!, 0.0)
    }

    @Test
    fun externalChangesFlowIntoState() = runTest(dispatcher) {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }

        repo.setBodyWeightKg(72.25)

        assertEquals("72.3", vm.uiState.value.bodyWeightText)
    }
}
