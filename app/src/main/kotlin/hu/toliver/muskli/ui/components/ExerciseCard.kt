package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

@Composable
fun ExerciseCard(
    exercise: WorkoutExerciseUi,
    weightColumnLabel: String,
    menuExpanded: Boolean,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onReorder: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onSetTypeClick: (setId: String) -> Unit,
    onWeightChange: (setId: String, value: String) -> Unit,
    onRepsChange: (setId: String, value: String) -> Unit,
    onCompletedChange: (setId: String, completed: Boolean) -> Unit,
    onAddSet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.l, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = stringResource(R.string.cd_exercise_menu, exercise.name),
                    )
                }
                ExerciseMenu(
                    expanded = menuExpanded,
                    onDismiss = onMenuDismiss,
                    onReorder = onReorder,
                    onReplace = onReplace,
                    onRemove = onRemove,
                )
            }
        }
        SetTableHeader(weightColumnLabel = weightColumnLabel, modifier = Modifier.padding(vertical = Spacing.xs))
        exercise.sets.forEach { set ->
            SetRow(
                set = set,
                onSetTypeClick = { onSetTypeClick(set.id) },
                onWeightChange = { onWeightChange(set.id, it) },
                onRepsChange = { onRepsChange(set.id, it) },
                onCompletedChange = { onCompletedChange(set.id, it) },
            )
        }
        AddSetButton(
            onClick = onAddSet,
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
        )
    }
}

@Preview(name = "Exercise card", showBackground = true)
@Preview(name = "Exercise card: dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExerciseCardPreview() {
    MuskliTheme {
        Surface {
            ExerciseCard(
                exercise = WorkoutExerciseUi(
                    id = "squat",
                    name = "Back Squat",
                    sets = listOf(
                        WorkoutSetUi("1", 1, SetType.WARMUP, "60", "8", "60", "8", completed = true),
                        WorkoutSetUi("2", 1, SetType.NORMAL, "100", "5", "100", "5", completed = true),
                        WorkoutSetUi("3", 2, SetType.NORMAL, "100", "5", "100", "5", completed = false),
                    ),
                ),
                weightColumnLabel = stringResource(R.string.column_kg),
                menuExpanded = false,
                onMenuClick = {},
                onMenuDismiss = {},
                onReorder = {},
                onReplace = {},
                onRemove = {},
                onSetTypeClick = {},
                onWeightChange = { _, _ -> },
                onRepsChange = { _, _ -> },
                onCompletedChange = { _, _ -> },
                onAddSet = {},
            )
        }
    }
}
