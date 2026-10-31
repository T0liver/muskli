package hu.toliver.muskli.ui.routine

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.toliver.muskli.R
import hu.toliver.muskli.ui.components.BackTopBar
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

@Composable
fun RoutinesRoute(
    viewModel: RoutinesViewModel,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (routineId: String) -> Unit,
    onStarted: () -> Unit,
    onResumeWorkout: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RoutinesScreen(
        state = state,
        onBack = onBack,
        onCreate = onCreate,
        onEdit = onEdit,
        onStart = { viewModel.startRoutine(it, onStarted) },
        onResumeWorkout = {
            viewModel.onEvent(RoutinesEvent.InProgressDismissed)
            onResumeWorkout()
        },
        onInProgressDismiss = { viewModel.onEvent(RoutinesEvent.InProgressDismissed) },
    )
}

@Composable
fun RoutinesScreen(
    state: RoutinesUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (routineId: String) -> Unit,
    onStart: (routineId: String) -> Unit,
    onResumeWorkout: () -> Unit,
    onInProgressDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(
                title = stringResource(R.string.routines_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onCreate) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.routines_new))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Unit
            state.routines.isEmpty() -> EmptyRoutines(onCreate = onCreate, modifier = Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.l,
                    end = Spacing.l,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                items(state.routines, key = { it.id }) { routine ->
                    RoutineCard(routine = routine, onEdit = { onEdit(routine.id) }, onStart = { onStart(routine.id) })
                }
            }
        }
    }

    if (state.inProgressShown) {
        AlertDialog(
            onDismissRequest = onInProgressDismiss,
            title = { Text(stringResource(R.string.routines_in_progress_title)) },
            text = { Text(stringResource(R.string.routines_in_progress_body)) },
            confirmButton = { TextButton(onClick = onResumeWorkout) { Text(stringResource(R.string.routines_resume)) } },
            dismissButton = { TextButton(onClick = onInProgressDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun EmptyRoutines(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.routines_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.routines_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onCreate, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
            Text(stringResource(R.string.routines_new))
        }
    }
}

/** Tapping the card edits the routine; Start is its own 48dp button. */
@Composable
private fun RoutineCard(routine: RoutineSummaryUi, onEdit: () -> Unit, onStart: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onEdit)
                .heightIn(min = Spacing.minTouchTarget)
                .padding(start = Spacing.l, end = Spacing.s, top = Spacing.s, bottom = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(routine.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = routine.exerciseNames.joinToString().ifEmpty { stringResource(R.string.routines_no_exercises) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val startDescription = stringResource(R.string.cd_start_routine, routine.title)
            Button(
                onClick = onStart,
                modifier = Modifier
                    .padding(start = Spacing.s)
                    .heightIn(min = Spacing.minTouchTarget)
                    .semantics { contentDescription = startDescription },
            ) {
                Text(stringResource(R.string.routines_start))
            }
        }
    }
}

@Preview(showSystemUi = true)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RoutinesScreenPreview() {
    MuskliTheme {
        RoutinesScreen(
            state = RoutinesUiState(
                loading = false,
                routines = listOf(
                    RoutineSummaryUi("a", "Pull day", listOf("Pull-Up", "Barbell Row")),
                    RoutineSummaryUi("b", "Push day", listOf("Barbell Bench Press", "Dumbbell Lateral Raise")),
                ),
            ),
            onBack = {},
            onCreate = {},
            onEdit = {},
            onStart = {},
            onResumeWorkout = {},
            onInProgressDismiss = {},
        )
    }
}
