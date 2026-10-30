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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import hu.toliver.muskli.ui.home.HomeScreen
import hu.toliver.muskli.ui.library.LibraryMode
import hu.toliver.muskli.ui.library.LibraryRoute
import hu.toliver.muskli.ui.settings.SettingsRoute
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.workout.LiveWorkoutEvent
import hu.toliver.muskli.ui.workout.LiveWorkoutRoute
import hu.toliver.muskli.ui.workout.LiveWorkoutUiState
import hu.toliver.muskli.ui.workout.WorkoutSessionViewModel

/** No navigation library yet (owner choice, T-017). */
private enum class Screen { HOME, WORKOUT, ADD_EXERCISES, REPLACE_EXERCISE, LIBRARY, SETTINGS }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MuskliTheme {
                val workout: WorkoutSessionViewModel = hiltViewModel()
                val workoutState by workout.uiState.collectAsStateWithLifecycle()
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                var startChecked by rememberSaveable { mutableStateOf(false) }
                var replacingExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
                // If denied, the in-app bar still counts down, but the screen-off alert cannot be shown.
                val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                // A draft at app start means the app was closed or killed mid-workout: go straight back to it (AC-03).
                LaunchedEffect(workoutState) {
                    if (!startChecked && workoutState != LiveWorkoutUiState.Loading) {
                        startChecked = true
                        if (workoutState is LiveWorkoutUiState.Active) screen = Screen.WORKOUT
                    }
                }

                val backToWorkout = { screen = Screen.WORKOUT }
                BackHandler(enabled = screen != Screen.HOME) {
                    screen = if (screen == Screen.ADD_EXERCISES || screen == Screen.REPLACE_EXERCISE) Screen.WORKOUT else Screen.HOME
                }
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        workoutInProgress = workoutState is LiveWorkoutUiState.Active,
                        onStartWorkout = {
                            // At workout start, before any set is logged (D-015).
                            if (needsNotificationPermission()) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            workout.onEvent(LiveWorkoutEvent.StartClicked)
                            screen = Screen.WORKOUT
                        },
                        onResumeWorkout = backToWorkout,
                        onOpenLibrary = { screen = Screen.LIBRARY },
                        onOpenSettings = { screen = Screen.SETTINGS },
                    )
                    Screen.WORKOUT -> LiveWorkoutRoute(
                        viewModel = workout,
                        onAddExercise = { screen = Screen.ADD_EXERCISES },
                        onReplaceExercise = {
                            replacingExerciseId = it
                            screen = Screen.REPLACE_EXERCISE
                        },
                    )
                    Screen.ADD_EXERCISES -> LibraryRoute(mode = LibraryMode.PICK, onBack = backToWorkout, onPicked = { ids ->
                        workout.onEvent(LiveWorkoutEvent.ExercisesPicked(ids))
                        screen = Screen.WORKOUT
                    })
                    // TODO(T-027): single-select picker; until then the first selected exercise is used.
                    Screen.REPLACE_EXERCISE -> LibraryRoute(mode = LibraryMode.PICK, onBack = backToWorkout, onPicked = { ids ->
                        val target = replacingExerciseId
                        val replacement = ids.firstOrNull()
                        if (target != null && replacement != null) {
                            workout.onEvent(LiveWorkoutEvent.ReplacementPicked(target, replacement))
                        }
                        screen = Screen.WORKOUT
                    })
                    Screen.LIBRARY -> LibraryRoute(mode = LibraryMode.BROWSE, onBack = { screen = Screen.HOME }, onPicked = {})
                    Screen.SETTINGS -> SettingsRoute(onBack = { screen = Screen.HOME })
                }
            }
        }
    }

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
}
