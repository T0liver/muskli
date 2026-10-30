package hu.toliver.muskli.ui.workout

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.AddExerciseButton
import hu.toliver.muskli.ui.components.ExerciseCard
import hu.toliver.muskli.ui.components.RestTimerBar
import hu.toliver.muskli.ui.components.RestTimerUi
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.components.WorkoutExerciseUi
import hu.toliver.muskli.ui.components.WorkoutHeader
import hu.toliver.muskli.ui.components.WorkoutSetUi
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

/** Layout of the live-workout screen; [LiveWorkoutScreen] wires it to the ViewModel state. */
@Composable
fun LiveWorkoutContent(
    duration: String,
    volume: String,
    completedSets: Int,
    exercises: List<WorkoutExerciseUi>,
    weightUnit: WeightUnit,
    menuExpandedForExerciseId: String?,
    setTypeMenuForSetId: String?,
    restTimer: RestTimerUi?,
    onDurationClick: () -> Unit,
    onFinish: () -> Unit,
    onMenuClick: (exerciseId: String) -> Unit,
    onMenuDismiss: () -> Unit,
    onExerciseHistory: (exerciseTemplateId: String) -> Unit,
    onReorder: (exerciseId: String) -> Unit,
    onReplace: (exerciseId: String) -> Unit,
    onRemove: (exerciseId: String) -> Unit,
    onSetTypeClick: (setId: String) -> Unit,
    onSetTypeChosen: (setId: String, type: SetType) -> Unit,
    onSetTypeMenuDismiss: () -> Unit,
    onValueChange: (setId: String, column: SetColumn, value: String) -> Unit,
    onCompletedChange: (setId: String, completed: Boolean) -> Unit,
    onDeleteSet: (setId: String) -> Unit,
    onAddSet: (exerciseId: String) -> Unit,
    onAddExercise: () -> Unit,
    onRestMinus15: () -> Unit,
    onRestPlus15: () -> Unit,
    onRestSkip: () -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            WorkoutHeader(
                duration = duration,
                volume = volume,
                completedSets = completedSets,
                onDurationClick = onDurationClick,
                onFinish = onFinish,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(top = Spacing.xs),
                title = if (editing) R.string.edit_workout_title else R.string.live_workout_title,
                finishLabel = if (editing) R.string.action_save else R.string.action_finish,
            )
        },
        bottomBar = {
            if (restTimer != null) {
                RestTimerBar(timer = restTimer, onMinus15 = onRestMinus15, onPlus15 = onRestPlus15, onSkip = onRestSkip)
            }
        },
    ) { padding ->
        if (exercises.isEmpty()) {
            EmptyWorkout(onAddExercise = onAddExercise, modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + Spacing.m,
                    bottom = padding.calculateBottomPadding() + Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        weightUnit = weightUnit,
                        menuExpanded = exercise.id == menuExpandedForExerciseId,
                        setTypeMenuForSetId = setTypeMenuForSetId,
                        onMenuClick = { onMenuClick(exercise.id) },
                        onMenuDismiss = onMenuDismiss,
                        onHistory = { onExerciseHistory(exercise.templateId) },
                        onReorder = { onReorder(exercise.id) },
                        onReplace = { onReplace(exercise.id) },
                        onRemove = { onRemove(exercise.id) },
                        onSetTypeClick = onSetTypeClick,
                        onSetTypeChosen = onSetTypeChosen,
                        onSetTypeMenuDismiss = onSetTypeMenuDismiss,
                        onValueChange = onValueChange,
                        onCompletedChange = onCompletedChange,
                        onDeleteSet = onDeleteSet,
                        onAddSet = { onAddSet(exercise.id) },
                    )
                }
                item {
                    AddExerciseButton(onClick = onAddExercise, modifier = Modifier.padding(horizontal = Spacing.l))
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkout(onAddExercise: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.live_workout_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.live_workout_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        AddExerciseButton(onClick = onAddExercise, modifier = Modifier.padding(top = Spacing.l))
    }
}

private val weightReps = listOf(SetColumn.WEIGHT, SetColumn.REPS)

private fun previewExercises(completed: Boolean): List<WorkoutExerciseUi> = listOf(
    WorkoutExerciseUi(
        id = "bench",
        templateId = "bench",
        name = "Bench Press (Barbell)",
        columns = weightReps,
        sets = listOf(
            WorkoutSetUi("b1", 1, SetType.WARMUP, listOf("40", "10"), listOf("40", "10"), completed = completed),
            WorkoutSetUi("b2", 1, SetType.NORMAL, listOf("80", "8"), listOf("82.5", "8"), completed = completed),
            WorkoutSetUi("b3", 2, SetType.NORMAL, listOf("80", "8"), listOf("82.5", "8"), completed = false),
            WorkoutSetUi("b4", 3, SetType.FAILURE, listOf("80", "6"), listOf("82.5", "6"), completed = false),
        ),
    ),
    WorkoutExerciseUi(
        id = "raise",
        templateId = "raise",
        name = "Lateral Raise (Dumbbell)",
        columns = weightReps,
        sets = listOf(
            WorkoutSetUi("l1", 1, SetType.NORMAL, listOf("12", "12"), listOf("12", "12"), completed = false),
            WorkoutSetUi("l2", 2, SetType.DROPSET, listOf("8", "10"), listOf("8", "10"), completed = false),
            WorkoutSetUi("l3", 3, SetType.DROPSET, null, listOf("", ""), completed = false),
        ),
    ),
    WorkoutExerciseUi(
        id = "row",
        templateId = "row",
        name = "Bent-Over Row (Barbell)",
        columns = weightReps,
        sets = listOf(
            WorkoutSetUi("r1", 1, SetType.NORMAL, listOf("70", "10"), listOf("70", "10"), completed = completed),
            WorkoutSetUi("r2", 2, SetType.NORMAL, listOf("70", "10"), listOf("70", "10"), completed = false),
        ),
    ),
)

@Composable
private fun LiveWorkoutPreview(
    exercises: List<WorkoutExerciseUi>,
    completedSets: Int,
    volume: String,
    restTimer: RestTimerUi?,
) {
    MuskliTheme {
        LiveWorkoutContent(
            duration = "24:05",
            volume = stringResource(R.string.volume_kg, volume),
            completedSets = completedSets,
            exercises = exercises,
            weightUnit = WeightUnit.KG,
            menuExpandedForExerciseId = null,
            setTypeMenuForSetId = null,
            restTimer = restTimer,
            onDurationClick = {},
            onFinish = {},
            onMenuClick = {},
            onMenuDismiss = {},
            onExerciseHistory = {},
            onReorder = {},
            onReplace = {},
            onRemove = {},
            onSetTypeClick = {},
            onSetTypeChosen = { _, _ -> },
            onSetTypeMenuDismiss = {},
            onValueChange = { _, _, _ -> },
            onCompletedChange = { _, _ -> },
            onDeleteSet = {},
            onAddSet = {},
            onAddExercise = {},
            onRestMinus15 = {},
            onRestPlus15 = {},
            onRestSkip = {},
        )
    }
}

@Preview(name = "1. Empty workout", showSystemUi = true)
@Composable
private fun LiveWorkoutEmptyPreview() {
    LiveWorkoutPreview(exercises = emptyList(), completedSets = 0, volume = "0", restTimer = null)
}

@Preview(name = "2. Three exercises, all set types, pre-filled", showSystemUi = true)
@Composable
private fun LiveWorkoutAllSetTypesPreview() {
    LiveWorkoutPreview(exercises = previewExercises(completed = false), completedSets = 0, volume = "0", restTimer = null)
}

@Preview(name = "3. Checked and unchecked sets, resting", showSystemUi = true)
@Composable
private fun LiveWorkoutCheckedPreview() {
    LiveWorkoutPreview(
        exercises = previewExercises(completed = true),
        completedSets = 2,
        volume = "1,360",
        restTimer = RestTimerUi(remaining = "1:24", progress = 0.6f),
    )
}

@Preview(name = "4. Dark mode", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LiveWorkoutDarkPreview() {
    LiveWorkoutPreview(
        exercises = previewExercises(completed = true),
        completedSets = 2,
        volume = "1,360",
        restTimer = RestTimerUi(remaining = "1:24", progress = 0.6f),
    )
}

@Preview(name = "5. Large font (1.5x)", showSystemUi = true, fontScale = 1.5f)
@Composable
private fun LiveWorkoutLargeFontPreview() {
    LiveWorkoutPreview(
        exercises = previewExercises(completed = true),
        completedSets = 2,
        volume = "1,360",
        restTimer = RestTimerUi(remaining = "1:24", progress = 0.6f),
    )
}
