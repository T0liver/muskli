package hu.toliver.muskli.ui.finish

import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.Workout
import hu.toliver.muskli.domain.model.WorkoutExercise
import hu.toliver.muskli.domain.model.WorkoutSet
import hu.toliver.muskli.domain.usecase.workout.DeleteWorkoutUseCase
import hu.toliver.muskli.domain.usecase.workout.FinishWorkoutUseCase
import hu.toliver.muskli.ui.workout.FakeWorkoutRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinishViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = TestClock(now = TestClock.START_MS + 3_900_000L)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun set(id: String, completed: Boolean) =
        WorkoutSet(id, SetData(weightKg = 80.0, reps = 5, completed = completed), completedAtEpochMs = null)

    private fun draft(vararg sets: WorkoutSet, finishedAt: Long? = null) = Workout(
        id = "w1",
        title = "",
        notes = null,
        startedAtEpochMs = TestClock.START_MS,
        utcOffsetS = 0,
        finishedAtEpochMs = finishedAt,
        createdAtEpochMs = TestClock.START_MS,
        updatedAtEpochMs = TestClock.START_MS,
        deletedAtEpochMs = null,
        exercises = listOf(WorkoutExercise("e1", "bench", supersetGroup = null, notes = null, restSeconds = null, sets = sets.toList())),
    )

    private fun viewModel(workouts: FakeWorkoutRepository) =
        FinishViewModel("w1", workouts, clock, FinishWorkoutUseCase(workouts, clock), DeleteWorkoutUseCase(workouts, clock))

    private val FinishViewModel.ready get() = uiState.value as FinishUiState.Ready

    @Test
    fun liveDurationIsTheTimeSinceTheStart() = runTest(dispatcher) {
        val vm = viewModel(FakeWorkoutRepository(listOf(draft(set("s1", true)))))

        assertEquals("1", vm.ready.hours)
        assertEquals("5", vm.ready.minutes)
        assertEquals(false, vm.ready.editingPast)
    }

    @Test
    fun uncompletedSetsAreDiscardedOnlyAfterConfirmation() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(listOf(draft(set("s1", true), set("s2", false))))
        val vm = viewModel(workouts)
        vm.onEvent(FinishEvent.TitleChanged("Push"))

        vm.onEvent(FinishEvent.SaveClicked)

        assertEquals(FinishDialog.DiscardSets(1), vm.ready.dialog)
        assertNull(workouts.workouts.value.getValue("w1").finishedAtEpochMs)

        vm.onEvent(FinishEvent.DiscardSetsConfirmed)

        assertEquals(FinishUiState.Done(FinishOutcome.Saved("w1")), vm.uiState.value)
        val saved = workouts.workouts.value.getValue("w1")
        assertEquals("Push", saved.title)
        assertEquals(listOf("s1"), saved.exercises.single().sets.map { it.id })
        assertEquals(TestClock.START_MS + 3_900_000L, saved.finishedAtEpochMs)
    }

    @Test
    fun withNothingCompletedItOffersToDiscardTheWorkout() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(listOf(draft(set("s1", false))))
        val vm = viewModel(workouts)

        vm.onEvent(FinishEvent.SaveClicked)
        assertEquals(FinishDialog.DiscardWorkout, vm.ready.dialog)

        vm.onEvent(FinishEvent.DiscardWorkoutConfirmed)

        assertEquals(FinishUiState.Done(FinishOutcome.Discarded("w1")), vm.uiState.value)
        assertNull(workouts.activeDraft)
    }

    @Test
    fun anInvalidDurationBlocksSaving() = runTest(dispatcher) {
        val workouts = FakeWorkoutRepository(listOf(draft(set("s1", true))))
        val vm = viewModel(workouts)

        vm.onEvent(FinishEvent.MinutesChanged("75"))
        vm.onEvent(FinishEvent.SaveClicked)

        assertEquals(false, vm.ready.durationValid)
        assertNull(workouts.workouts.value.getValue("w1").finishedAtEpochMs)
    }

    @Test
    fun aWorkoutUnderAMinuteSavedUneditedKeepsItsSeconds() = runTest(dispatcher) {
        clock.now = TestClock.START_MS + 30_000L
        val workouts = FakeWorkoutRepository(listOf(draft(set("s1", true))))
        val vm = viewModel(workouts)
        assertEquals("0", vm.ready.minutes)

        vm.onEvent(FinishEvent.SaveClicked)

        assertEquals(TestClock.START_MS + 30_000L, workouts.workouts.value.getValue("w1").finishedAtEpochMs)
    }

    @Test
    fun anEditedPastWorkoutKeepsItsDuration() = runTest(dispatcher) {
        val vm = viewModel(FakeWorkoutRepository(listOf(draft(set("s1", true), finishedAt = TestClock.START_MS + 1_800_000L))))

        assertEquals("0", vm.ready.hours)
        assertEquals("30", vm.ready.minutes)
        assertEquals(true, vm.ready.editingPast)
    }
}
