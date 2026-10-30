package hu.toliver.muskli.ui.workout

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.DateTimePickerDialogs
import hu.toliver.muskli.ui.components.ReorderDialog
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.components.WorkoutExerciseUi
import hu.toliver.muskli.ui.components.WorkoutSetUi
import hu.toliver.muskli.ui.theme.MuskliTheme

/** The live workout, or a past workout in edit mode; [onFinish] opens the finish (or save) details. */
@Composable
fun LiveWorkoutRoute(
    viewModel: WorkoutSessionViewModel,
    onAddExercise: () -> Unit,
    onReplaceExercise: (workoutExerciseId: String) -> Unit,
    onFinish: (workoutId: String) -> Unit,
    onExerciseHistory: (exerciseTemplateId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onEvent(LiveWorkoutEvent.ScreenShown) }
    when (val current = state) {
        is LiveWorkoutUiState.Active -> LiveWorkoutScreen(
            state = current,
            onEvent = viewModel::onEvent,
            onAddExercise = onAddExercise,
            onReplaceExercise = onReplaceExercise,
            onFinish = { onFinish(current.workoutId) },
            onExerciseHistory = onExerciseHistory,
        )
        // Only for the moment between "Start" and the draft's first emission.
        LiveWorkoutUiState.Loading, LiveWorkoutUiState.NoWorkout -> Surface(Modifier.fillMaxSize()) {}
    }
}

@Composable
fun LiveWorkoutScreen(
    state: LiveWorkoutUiState.Active,
    onEvent: (LiveWorkoutEvent) -> Unit,
    onAddExercise: () -> Unit,
    onReplaceExercise: (workoutExerciseId: String) -> Unit,
    onFinish: () -> Unit,
    onExerciseHistory: (exerciseTemplateId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.keepScreenAwake && !state.editing) KeepScreenOn()
    LiveWorkoutContent(
        duration = state.duration,
        volume = stringResource(if (state.weightUnit == WeightUnit.LB) R.string.volume_lb else R.string.volume_kg, state.volume),
        completedSets = state.completedSets,
        exercises = state.exercises,
        weightUnit = state.weightUnit,
        menuExpandedForExerciseId = state.exerciseMenuFor,
        setTypeMenuForSetId = state.setTypeMenuFor,
        restTimer = state.restTimer,
        onDurationClick = { onEvent(LiveWorkoutEvent.DurationClicked) },
        onFinish = onFinish,
        onMenuClick = { onEvent(LiveWorkoutEvent.ExerciseMenuClicked(it)) },
        onMenuDismiss = { onEvent(LiveWorkoutEvent.ExerciseMenuDismissed) },
        onExerciseHistory = {
            onEvent(LiveWorkoutEvent.ExerciseMenuDismissed)
            onExerciseHistory(it)
        },
        onReorder = { onEvent(LiveWorkoutEvent.ReorderClicked) },
        onReplace = {
            onEvent(LiveWorkoutEvent.ExerciseMenuDismissed)
            onReplaceExercise(it)
        },
        onRemove = { onEvent(LiveWorkoutEvent.RemoveClicked(it)) },
        onSetTypeClick = { onEvent(LiveWorkoutEvent.SetTypeClicked(it)) },
        onSetTypeChosen = { setId, type -> onEvent(LiveWorkoutEvent.SetTypeChosen(setId, type)) },
        onSetTypeMenuDismiss = { onEvent(LiveWorkoutEvent.SetTypeMenuDismissed) },
        onValueChange = { setId, column, text -> onEvent(LiveWorkoutEvent.ValueChanged(setId, column, text)) },
        onCompletedChange = { setId, completed -> onEvent(LiveWorkoutEvent.CompletedChanged(setId, completed)) },
        onDeleteSet = { onEvent(LiveWorkoutEvent.SetDeleted(it)) },
        onAddSet = { onEvent(LiveWorkoutEvent.AddSetClicked(it)) },
        onAddExercise = onAddExercise,
        onRestMinus15 = { onEvent(LiveWorkoutEvent.RestMinus15) },
        onRestPlus15 = { onEvent(LiveWorkoutEvent.RestPlus15) },
        onRestSkip = { onEvent(LiveWorkoutEvent.RestSkipped) },
        modifier = modifier,
        editing = state.editing,
    )
    if (state.reorderOpen) {
        ReorderDialog(
            exercises = state.exercises,
            onMove = { id, toIndex -> onEvent(LiveWorkoutEvent.ExerciseMoved(id, toIndex)) },
            onDismiss = { onEvent(LiveWorkoutEvent.ReorderDismissed) },
        )
    }
    if (state.startPickerOpen) {
        DateTimePickerDialogs(
            initialEpochMs = state.startedAtEpochMs,
            onPicked = { onEvent(LiveWorkoutEvent.StartPicked(it)) },
            onDismiss = { onEvent(LiveWorkoutEvent.StartPickerDismissed) },
        )
    }
    if (state.startInFutureShown) {
        StartInFutureDialog(onDismiss = { onEvent(LiveWorkoutEvent.StartInFutureDismissed) })
    }
}

@Composable
private fun StartInFutureDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
        title = { Text(stringResource(R.string.start_in_future_title)) },
        text = { Text(stringResource(R.string.start_in_future_body)) },
    )
}

/** The keep-awake setting (F-02 AC5), applied only while this screen is shown. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LiveWorkoutScreenPreview() {
    MuskliTheme {
        LiveWorkoutScreen(
            state = LiveWorkoutUiState.Active(
                workoutId = "w",
                duration = "12:40",
                volume = "800",
                completedSets = 1,
                weightUnit = WeightUnit.KG,
                exercises = listOf(
                    WorkoutExerciseUi(
                        id = "squat",
                        templateId = "back-squat",
                        name = "Back Squat",
                        columns = listOf(SetColumn.WEIGHT, SetColumn.REPS),
                        sets = listOf(
                            WorkoutSetUi("1", 1, SetType.NORMAL, listOf("100", "8"), listOf("100", "8"), completed = true),
                            WorkoutSetUi("2", 2, SetType.NORMAL, listOf("100", "8"), listOf("100", "8"), completed = false),
                        ),
                    ),
                ),
                keepScreenAwake = false,
            ),
            onEvent = {},
            onAddExercise = {},
            onReplaceExercise = {},
            onFinish = {},
            onExerciseHistory = {},
        )
    }
}

@Preview(name = "Start in the future")
@Preview(name = "Start in the future: dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StartInFutureDialogPreview() {
    MuskliTheme {
        StartInFutureDialog(onDismiss = {})
    }
}
