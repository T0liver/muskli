package hu.toliver.muskli.ui.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.BackTopBar
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun WorkoutDetailRoute(
    viewModel: WorkoutDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExerciseHistory: (exerciseTemplateId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onDelete = onDelete,
        onExerciseHistory = onExerciseHistory,
        onEvent = viewModel::onEvent,
    )
}

@Composable
fun WorkoutDetailScreen(
    state: WorkoutDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExerciseHistory: (exerciseTemplateId: String) -> Unit,
    onEvent: (WorkoutDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state as? WorkoutDetailUiState.Ready
    val snackbar = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.detail_routine_saved)
    LaunchedEffect(ready?.routineSaved) {
        if (ready?.routineSaved == true) {
            // Marking it shown restarts this effect, so the snackbar runs in the screen's scope instead.
            snackbarScope.launch { snackbar.showSnackbar(savedMessage) }
            onEvent(WorkoutDetailEvent.RoutineSavedShown)
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BackTopBar(
                title = ready?.let { workoutTitle(it.summary.title) }.orEmpty(),
                onBack = onBack,
                actions = {
                    if (ready != null) {
                        TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
                            Text(stringResource(R.string.action_edit))
                        }
                        // No confirmation: the history list offers Undo right after (TC-DATA-03).
                        TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
                            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.cd_workout_menu))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.detail_save_as_routine)) },
                                    onClick = {
                                        menuOpen = false
                                        onEvent(WorkoutDetailEvent.SaveAsRoutineClicked)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            WorkoutDetailUiState.Loading -> Unit
            WorkoutDetailUiState.Missing -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.detail_missing), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is WorkoutDetailUiState.Ready -> LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.l,
                    end = Spacing.l,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                item {
                    Text(state.summary.dateTime, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(workoutStats(state.summary, state.weightUnit), style = MaterialTheme.typography.bodyLarge)
                    state.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.s)) }
                }
                items(state.exercises) { exercise ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ExerciseSummary(exercise = exercise, weightUnit = state.weightUnit, onNameClick = { onExerciseHistory(exercise.templateId) })
                }
            }
        }
    }

    ready?.saveAsRoutine?.let { dialog -> SaveAsRoutineDialog(dialog = dialog, onEvent = onEvent) }
}

@Composable
private fun SaveAsRoutineDialog(dialog: SaveAsRoutineUi, onEvent: (WorkoutDetailEvent) -> Unit) {
    val error = dialog.showError && dialog.title.isBlank()
    AlertDialog(
        onDismissRequest = { onEvent(WorkoutDetailEvent.SaveAsRoutineDismissed) },
        title = { Text(stringResource(R.string.detail_save_as_routine)) },
        text = {
            OutlinedTextField(
                value = dialog.title,
                onValueChange = { onEvent(WorkoutDetailEvent.SaveAsRoutineTitleChanged(it)) },
                label = { Text(stringResource(R.string.routine_name)) },
                isError = error,
                supportingText = if (error) {
                    { Text(stringResource(R.string.routine_name_missing)) }
                } else {
                    null
                },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onEvent(WorkoutDetailEvent.SaveAsRoutineConfirmed) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = { onEvent(WorkoutDetailEvent.SaveAsRoutineDismissed) }) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Preview(showSystemUi = true)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WorkoutDetailScreenPreview() {
    MuskliTheme {
        WorkoutDetailScreen(
            state = WorkoutDetailUiState.Ready(
                summary = WorkoutSummaryUi("w", "Push day", "30 Oct 2026, 17:05", 3_900, "1,360", 3, listOf("Bench Press (Barbell)")),
                notes = "Felt strong.",
                exercises = listOf(
                    ExerciseSummaryUi(
                        templateId = "bench",
                        name = "Bench Press (Barbell)",
                        columns = listOf(SetColumn.WEIGHT, SetColumn.REPS),
                        sets = listOf(
                            SetSummaryUi(1, SetType.WARMUP, listOf("40", "10")),
                            SetSummaryUi(1, SetType.NORMAL, listOf("80", "8")),
                            SetSummaryUi(2, SetType.NORMAL, listOf("80", "8")),
                        ),
                    ),
                ),
                weightUnit = WeightUnit.KG,
            ),
            onBack = {},
            onEdit = {},
            onDelete = {},
            onExerciseHistory = {},
            onEvent = {},
        )
    }
}
