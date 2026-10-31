package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

@Composable
fun ExerciseCard(
    exercise: WorkoutExerciseUi,
    weightUnit: WeightUnit,
    menuExpanded: Boolean,
    setTypeMenuForSetId: String?,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onHistory: () -> Unit,
    onReorder: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onSetTypeClick: (setId: String) -> Unit,
    onSetTypeChosen: (setId: String, type: SetType) -> Unit,
    onSetTypeMenuDismiss: () -> Unit,
    onValueChange: (setId: String, column: SetColumn, value: String) -> Unit,
    onCompletedChange: (setId: String, completed: Boolean) -> Unit,
    onDeleteSet: (setId: String) -> Unit,
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
                    onHistory = onHistory,
                    onReorder = onReorder,
                    onReplace = onReplace,
                    onRemove = onRemove,
                )
            }
        }
        SetTableHeader(columns = exercise.columns, weightUnit = weightUnit, modifier = Modifier.padding(vertical = Spacing.xs))
        exercise.sets.forEach { set ->
            key(set.id) {
                SwipeToDeleteSet(onDelete = { onDeleteSet(set.id) }) {
                    SetRow(
                        set = set,
                        columns = exercise.columns,
                        typeMenuExpanded = set.id == setTypeMenuForSetId,
                        onSetTypeClick = { onSetTypeClick(set.id) },
                        onSetTypeChosen = { onSetTypeChosen(set.id, it) },
                        onSetTypeMenuDismiss = onSetTypeMenuDismiss,
                        onValueChange = { column, value -> onValueChange(set.id, column, value) },
                        onCompletedChange = { onCompletedChange(set.id, it) },
                    )
                }
            }
        }
        AddSetButton(
            onClick = onAddSet,
            modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
        )
    }
}

/** Swipe from the end deletes (F-03 AC1); the same action is offered to accessibility services. */
@Composable
fun SwipeToDeleteSet(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState()
    val deleteLabel = stringResource(R.string.action_delete_set)
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete() },
        backgroundContent = {
            // Composed only while swiping, so screen readers do not read the label on every row.
            if (state.dismissDirection != SwipeToDismissBoxValue.Settled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = Spacing.l),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(deleteLabel, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        modifier = Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
        },
    ) {
        content()
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
                    templateId = "back-squat",
                    name = "Back Squat",
                    columns = listOf(SetColumn.WEIGHT, SetColumn.REPS),
                    sets = listOf(
                        WorkoutSetUi("1", 1, SetType.WARMUP, listOf("60", "8"), listOf("60", "8"), completed = true),
                        WorkoutSetUi("2", 1, SetType.NORMAL, listOf("100", "5"), listOf("100", "5"), completed = true),
                        WorkoutSetUi("3", 2, SetType.NORMAL, listOf("100", "5"), listOf("100", "5"), completed = false),
                    ),
                ),
                weightUnit = WeightUnit.KG,
                menuExpanded = false,
                setTypeMenuForSetId = null,
                onMenuClick = {},
                onMenuDismiss = {},
                onHistory = {},
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
            )
        }
    }
}
