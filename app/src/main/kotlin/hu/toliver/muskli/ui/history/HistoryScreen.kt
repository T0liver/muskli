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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.toliver.muskli.R
import hu.toliver.muskli.ui.components.BackTopBar
import hu.toliver.muskli.ui.components.DateTimePickerDialogs
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    nowEpochMs: () -> Long,
    onBack: () -> Unit,
    onOpenWorkout: (workoutId: String) -> Unit,
    onPastWorkoutCreated: (workoutId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        nowEpochMs = nowEpochMs,
        onBack = onBack,
        onOpenWorkout = onOpenWorkout,
        onLogPast = { viewModel.logPastWorkout(it, onPastWorkoutCreated) },
    )
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onEvent: (HistoryEvent) -> Unit,
    nowEpochMs: () -> Long,
    onBack: () -> Unit,
    onOpenWorkout: (workoutId: String) -> Unit,
    onLogPast: (startedAtEpochMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    var pickingPast by rememberSaveable { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.history_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(state.undoWorkoutId) {
        if (state.undoWorkoutId == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(deletedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Long)
        onEvent(if (result == SnackbarResult.ActionPerformed) HistoryEvent.UndoClicked else HistoryEvent.UndoDismissed)
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(
                title = stringResource(R.string.history_title),
                onBack = onBack,
                actions = {
                    TextButton(onClick = { pickingPast = true }, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
                        Text(stringResource(R.string.history_log_past))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!state.loading && state.workouts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(Spacing.xl), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.l,
                    end = Spacing.l,
                    top = padding.calculateTopPadding() + Spacing.s,
                    bottom = padding.calculateBottomPadding() + Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                items(state.workouts, key = { it.id }) { workout ->
                    WorkoutSummaryCard(workout = workout, weightUnit = state.weightUnit, onClick = { onOpenWorkout(workout.id) })
                }
            }
        }
    }
    if (pickingPast) {
        DateTimePickerDialogs(
            initialEpochMs = nowEpochMs(),
            onPicked = {
                pickingPast = false
                onLogPast(it)
            },
            onDismiss = { pickingPast = false },
        )
    }
}

@Preview(showSystemUi = true)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryScreenPreview() {
    MuskliTheme {
        HistoryScreen(
            state = HistoryUiState(
                loading = false,
                workouts = listOf(
                    WorkoutSummaryUi("a", "Push day", "30 Oct 2026, 17:05", 3_900, "4,320", 14, listOf("Bench Press (Barbell)", "Dips")),
                    WorkoutSummaryUi("b", "", "28 Oct 2026, 18:10", 2_700, "6,100", 12, listOf("Back Squat", "Romanian Deadlift")),
                ),
            ),
            onEvent = {},
            nowEpochMs = { 0L },
            onBack = {},
            onOpenWorkout = {},
            onLogPast = {},
        )
    }
}
