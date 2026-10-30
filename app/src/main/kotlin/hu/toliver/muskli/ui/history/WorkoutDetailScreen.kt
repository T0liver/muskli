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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun WorkoutDetailRoute(
    viewModel: WorkoutDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExerciseHistory: (exerciseTemplateId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutDetailScreen(state = state, onBack = onBack, onEdit = onEdit, onDelete = onDelete, onExerciseHistory = onExerciseHistory)
}

@Composable
fun WorkoutDetailScreen(
    state: WorkoutDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExerciseHistory: (exerciseTemplateId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state as? WorkoutDetailUiState.Ready
    Scaffold(
        modifier = modifier,
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
        )
    }
}
