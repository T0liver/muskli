package hu.toliver.muskli.ui.history

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

/** "1 h 5 min", "42 min", or "30 s" under a minute (D-051). */
@Composable
fun durationText(durationS: Long): String {
    val hours = durationS / 3600
    val minutes = durationS % 3600 / 60
    return when {
        hours > 0 -> stringResource(R.string.duration_h_min, hours, minutes)
        durationS in 1 until 60 -> stringResource(R.string.duration_s, durationS)
        else -> stringResource(R.string.duration_min, minutes)
    }
}

/** "1 h 5 min · 4,320 kg · 14 sets". */
@Composable
fun workoutStats(workout: WorkoutSummaryUi, weightUnit: WeightUnit): String = stringResource(
    R.string.history_stats,
    durationText(workout.durationS),
    stringResource(if (weightUnit == WeightUnit.LB) R.string.volume_lb else R.string.volume_kg, workout.volume),
    pluralStringResource(R.plurals.history_sets, workout.completedSets, workout.completedSets),
)

@Composable
fun workoutTitle(title: String): String = title.ifBlank { stringResource(R.string.workout_untitled) }

/** One read-only set: number or type marker, then the values with their units. */
@Composable
fun SetSummaryRow(set: SetSummaryUi, columns: List<SetColumn>, weightUnit: WeightUnit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(
            text = setMarker(set),
            style = MaterialTheme.typography.bodyMedium,
            color = if (set.type == SetType.NORMAL) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(24.dp),
        )
        val parts = columns.zip(set.values).filter { it.second.isNotEmpty() }.map { (column, value) -> valueWithUnit(column, value, weightUnit) }
        val separator = if (columns.lastOrNull() == SetColumn.REPS) " × " else " · "
        Text(text = parts.joinToString(separator).ifEmpty { stringResource(R.string.previous_none) }, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun setMarker(set: SetSummaryUi): String = when (set.type) {
    SetType.NORMAL -> set.number.toString()
    SetType.WARMUP -> stringResource(R.string.set_marker_warmup)
    SetType.DROPSET -> stringResource(R.string.set_marker_dropset)
    SetType.FAILURE -> stringResource(R.string.set_marker_failure)
}

@Composable
private fun valueWithUnit(column: SetColumn, value: String, unit: WeightUnit): String {
    val lb = unit == WeightUnit.LB
    return when (column) {
        SetColumn.WEIGHT -> stringResource(if (lb) R.string.volume_lb else R.string.volume_kg, value)
        SetColumn.ADDED_WEIGHT -> stringResource(if (lb) R.string.summary_added_lb else R.string.summary_added_kg, value)
        SetColumn.ASSISTANCE -> stringResource(if (lb) R.string.summary_assist_lb else R.string.summary_assist_kg, value)
        SetColumn.REPS, SetColumn.DURATION -> value
        SetColumn.DISTANCE_KM -> stringResource(R.string.summary_km, value)
        SetColumn.DISTANCE_M -> stringResource(R.string.summary_m, value)
    }
}

/** An exercise with its sets; tapping the name opens the exercise history (F-06 AC4). */
@Composable
fun ExerciseSummary(exercise: ExerciseSummaryUi, weightUnit: WeightUnit, onNameClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.minTouchTarget)
                .clickable(onClick = onNameClick)
                .padding(vertical = Spacing.m),
        )
        exercise.sets.forEach { SetSummaryRow(it, exercise.columns, weightUnit, Modifier.padding(vertical = Spacing.xs)) }
    }
}

@Composable
fun WorkoutSummaryCard(workout: WorkoutSummaryUi, weightUnit: WeightUnit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(workoutTitle(workout.title), style = MaterialTheme.typography.titleMedium)
            Text(workout.dateTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(workoutStats(workout, weightUnit), style = MaterialTheme.typography.bodyMedium)
            if (workout.exerciseNames.isNotEmpty()) {
                Text(
                    workout.exerciseNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@Preview(name = "History components", showBackground = true)
@Preview(name = "History components: dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryComponentsPreview() {
    MuskliTheme {
        Surface {
            Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                WorkoutSummaryCard(
                    workout = WorkoutSummaryUi("w", "Push day", "30 Oct 2026, 17:05", 3_900, "4,320", 14, listOf("Barbell Bench Press", "Dips")),
                    weightUnit = WeightUnit.KG,
                    onClick = {},
                )
                ExerciseSummary(
                    exercise = ExerciseSummaryUi(
                        templateId = "bench",
                        name = "Barbell Bench Press",
                        columns = listOf(SetColumn.WEIGHT, SetColumn.REPS),
                        sets = listOf(
                            SetSummaryUi(1, SetType.WARMUP, listOf("40", "10")),
                            SetSummaryUi(1, SetType.NORMAL, listOf("80", "8")),
                            SetSummaryUi(2, SetType.FAILURE, listOf("80", "6")),
                        ),
                    ),
                    weightUnit = WeightUnit.KG,
                    onNameClick = {},
                )
            }
        }
    }
}
