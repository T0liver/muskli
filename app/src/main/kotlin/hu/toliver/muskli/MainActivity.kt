package hu.toliver.muskli

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import hu.toliver.muskli.domain.repo.Clock
import hu.toliver.muskli.ui.finish.FinishOutcome
import hu.toliver.muskli.ui.finish.FinishRoute
import hu.toliver.muskli.ui.finish.FinishViewModel
import hu.toliver.muskli.ui.history.ExerciseHistoryRoute
import hu.toliver.muskli.ui.history.ExerciseHistoryViewModel
import hu.toliver.muskli.ui.history.HistoryEvent
import hu.toliver.muskli.ui.history.HistoryRoute
import hu.toliver.muskli.ui.history.HistoryViewModel
import hu.toliver.muskli.ui.history.WorkoutDetailRoute
import hu.toliver.muskli.ui.history.WorkoutDetailViewModel
import hu.toliver.muskli.ui.home.HomeScreen
import hu.toliver.muskli.ui.library.LibraryMode
import hu.toliver.muskli.ui.library.LibraryRoute
import hu.toliver.muskli.ui.settings.SettingsRoute
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.workout.LiveWorkoutEvent
import hu.toliver.muskli.ui.workout.LiveWorkoutRoute
import hu.toliver.muskli.ui.workout.LiveWorkoutUiState
import hu.toliver.muskli.ui.workout.WorkoutSessionViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var clock: Clock

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MuskliTheme {
                val workout = workoutViewModel(editId = null)
                val history: HistoryViewModel = hiltViewModel()
                val workoutState by workout.uiState.collectAsStateWithLifecycle()
                var stack by rememberSaveable(stateSaver = RouteStackSaver) { mutableStateOf(listOf<Route>(Route.Home)) }
                var startChecked by rememberSaveable { mutableStateOf(false) }
                var finishEntries by rememberSaveable { mutableIntStateOf(0) }
                // If denied, the in-app bar still counts down, but the screen-off alert cannot be shown.
                val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                fun push(route: Route) {
                    stack = stack + route
                }

                fun pop() {
                    if (stack.size > 1) stack = stack.dropLast(1)
                }

                fun openFinish(workoutId: String) = push(Route.Finish(workoutId, finishEntries++))

                // Edits are already stored, so leaving edit mode goes through the save step, which
                // applies D-011; a new past workout left with nothing completed is dropped instead.
                fun leaveEdit(route: Route.EditWorkout) {
                    if (!route.isNew) return openFinish(route.workoutId)
                    history.leaveNewPastWorkout(route.workoutId) { discarded -> if (discarded) pop() else openFinish(route.workoutId) }
                }

                fun finished(outcome: FinishOutcome) {
                    val below = stack.dropLast(1)
                    if (below.lastOrNull() == Route.Workout) {
                        stack = if (outcome is FinishOutcome.Saved) listOf(Route.Home, Route.History) else listOf(Route.Home)
                        return
                    }
                    val withoutEdit = below.dropLastWhile { it is Route.EditWorkout }
                    stack = when (outcome) {
                        is FinishOutcome.Saved ->
                            if (withoutEdit.lastOrNull() == Route.WorkoutDetail(outcome.workoutId)) withoutEdit else withoutEdit + Route.WorkoutDetail(outcome.workoutId)
                        is FinishOutcome.Discarded -> {
                            history.onEvent(HistoryEvent.Deleted(outcome.workoutId))
                            withoutEdit.filterNot { it == Route.WorkoutDetail(outcome.workoutId) }
                        }
                    }
                }

                // A draft at app start means the app was closed or killed mid-workout: go straight back to it (AC-03).
                LaunchedEffect(workoutState) {
                    if (!startChecked && workoutState != LiveWorkoutUiState.Loading) {
                        startChecked = true
                        if (workoutState is LiveWorkoutUiState.Active && stack == listOf(Route.Home)) stack = listOf(Route.Home, Route.Workout)
                    }
                }

                val top = stack.last()
                BackHandler(enabled = stack.size > 1) {
                    if (top is Route.EditWorkout) leaveEdit(top) else pop()
                }
                when (top) {
                    Route.Home -> HomeScreen(
                        workoutInProgress = workoutState is LiveWorkoutUiState.Active,
                        onStartWorkout = {
                            // At workout start, before any set is logged (D-015).
                            if (needsNotificationPermission()) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            workout.onEvent(LiveWorkoutEvent.StartClicked)
                            push(Route.Workout)
                        },
                        onResumeWorkout = { push(Route.Workout) },
                        onOpenHistory = { push(Route.History) },
                        onOpenLibrary = { push(Route.Library) },
                        onOpenSettings = { push(Route.Settings) },
                    )
                    Route.Workout -> LiveWorkoutRoute(
                        viewModel = workout,
                        onAddExercise = { push(Route.AddExercises(editId = null)) },
                        onReplaceExercise = { push(Route.ReplaceExercise(editId = null, workoutExerciseId = it)) },
                        onFinish = ::openFinish,
                        onExerciseHistory = { push(Route.ExerciseHistory(it)) },
                    )
                    is Route.EditWorkout -> LiveWorkoutRoute(
                        viewModel = workoutViewModel(editId = top.workoutId),
                        onAddExercise = { push(Route.AddExercises(editId = top.workoutId)) },
                        onReplaceExercise = { push(Route.ReplaceExercise(editId = top.workoutId, workoutExerciseId = it)) },
                        onFinish = ::openFinish,
                        onExerciseHistory = { push(Route.ExerciseHistory(it)) },
                    )
                    is Route.AddExercises -> {
                        val target = workoutViewModel(top.editId)
                        LibraryRoute(mode = LibraryMode.PICK, onBack = ::pop, onPicked = { ids ->
                            target.onEvent(LiveWorkoutEvent.ExercisesPicked(ids))
                            pop()
                        })
                    }
                    // TODO(T-027): single-select picker; until then the first selected exercise is used.
                    is Route.ReplaceExercise -> {
                        val target = workoutViewModel(top.editId)
                        LibraryRoute(mode = LibraryMode.PICK, onBack = ::pop, onPicked = { ids ->
                            ids.firstOrNull()?.let { target.onEvent(LiveWorkoutEvent.ReplacementPicked(top.workoutExerciseId, it)) }
                            pop()
                        })
                    }
                    is Route.Finish -> FinishRoute(
                        viewModel = hiltViewModel<FinishViewModel, FinishViewModel.Factory>(key = "finish-${top.entry}") { it.create(top.workoutId) },
                        onBack = ::pop,
                        onDone = ::finished,
                    )
                    Route.History -> HistoryRoute(
                        viewModel = history,
                        nowEpochMs = clock::nowEpochMs,
                        onBack = ::pop,
                        onOpenWorkout = { push(Route.WorkoutDetail(it)) },
                        onPastWorkoutCreated = { push(Route.EditWorkout(it, isNew = true)) },
                    )
                    is Route.WorkoutDetail -> WorkoutDetailRoute(
                        viewModel = hiltViewModel<WorkoutDetailViewModel, WorkoutDetailViewModel.Factory>(key = "detail-${top.workoutId}") {
                            it.create(top.workoutId)
                        },
                        onBack = ::pop,
                        onEdit = { push(Route.EditWorkout(top.workoutId, isNew = false)) },
                        onDelete = {
                            history.onEvent(HistoryEvent.DeleteClicked(top.workoutId))
                            pop()
                        },
                        onExerciseHistory = { push(Route.ExerciseHistory(it)) },
                    )
                    is Route.ExerciseHistory -> ExerciseHistoryRoute(
                        viewModel = hiltViewModel<ExerciseHistoryViewModel, ExerciseHistoryViewModel.Factory>(
                            key = "exercise-history-${top.exerciseTemplateId}",
                        ) { it.create(top.exerciseTemplateId) },
                        onBack = ::pop,
                        onOpenWorkout = { push(Route.WorkoutDetail(it)) },
                    )
                    Route.Library -> LibraryRoute(mode = LibraryMode.BROWSE, onBack = ::pop, onPicked = {})
                    Route.Settings -> SettingsRoute(onBack = ::pop)
                }
            }
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
}

/** The live workout for a null [editId], otherwise edit mode for that past workout; one instance each. */
@Composable
private fun workoutViewModel(editId: String?): WorkoutSessionViewModel =
    hiltViewModel<WorkoutSessionViewModel, WorkoutSessionViewModel.Factory>(key = "workout-${editId ?: "live"}") { it.create(editId) }
