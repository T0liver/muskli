package hu.toliver.muskli.ui.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun ExerciseHistoryRoute(viewModel: ExerciseHistoryViewModel, onBack: () -> Unit, onOpenWorkout: (workoutId: String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseHistoryScreen(state = state, onBack = onBack, onOpenWorkout = onOpenWorkout)
}

@Composable
fun ExerciseHistoryScreen(
    state: ExerciseHistoryUiState,
    onBack: () -> Unit,
    onOpenWorkout: (workoutId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state as? ExerciseHistoryUiState.Ready
    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(title = ready?.name.orEmpty(), onBack = onBack) },
    ) { padding ->
        when {
            ready == null -> Unit
            ready.sessions.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(Spacing.xl), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.exercise_history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.l,
                    end = Spacing.l,
                    top = padding.calculateTopPadding() + Spacing.s,
                    bottom = padding.calculateBottomPadding() + Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                items(ready.sessions, key = { it.workoutId }) { session ->
                    Card(onClick = { onOpenWorkout(session.workoutId) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(workoutTitle(session.workoutTitle), style = MaterialTheme.typography.titleMedium)
                            Text(session.dateTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            session.sets.forEach { SetSummaryRow(it, ready.columns, ready.weightUnit) }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExerciseHistoryScreenPreview() {
    MuskliTheme {
        ExerciseHistoryScreen(
            state = ExerciseHistoryUiState.Ready(
                name = "Bench Press (Barbell)",
                columns = listOf(SetColumn.WEIGHT, SetColumn.REPS),
                sessions = listOf(
                    ExerciseSessionUi(
                        "a",
                        "Push day",
                        "30 Oct 2026, 17:05",
                        listOf(SetSummaryUi(1, SetType.NORMAL, listOf("80", "8")), SetSummaryUi(2, SetType.NORMAL, listOf("80", "7"))),
                    ),
                    ExerciseSessionUi("b", "", "27 Oct 2026, 18:00", listOf(SetSummaryUi(1, SetType.NORMAL, listOf("77.5", "8")))),
                ),
                weightUnit = WeightUnit.KG,
            ),
            onBack = {},
            onOpenWorkout = {},
        )
    }
}
