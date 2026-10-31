package hu.toliver.muskli.ui.routine

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.components.AddExerciseButton
import hu.toliver.muskli.ui.components.AddSetButton
import hu.toliver.muskli.ui.components.BackTopBar
import hu.toliver.muskli.ui.components.PlannedSetRow
import hu.toliver.muskli.ui.components.PlannedSetTableHeader
import hu.toliver.muskli.ui.components.SetColumn
import hu.toliver.muskli.ui.components.SwipeToDeleteSet
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

@Composable
fun RoutineEditorRoute(
    viewModel: RoutineEditorViewModel,
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    onDone: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        RoutineEditorUiState.Done -> LaunchedEffect(current) { onDone() }
        else -> RoutineEditorScreen(state = current, onEvent = viewModel::onEvent, onBack = onBack, onAddExercise = onAddExercise)
    }
}

@Composable
fun RoutineEditorScreen(
    state: RoutineEditorUiState,
    onEvent: (RoutineEditorEvent) -> Unit,
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state as? RoutineEditorUiState.Ready
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(
                title = stringResource(if (ready?.isNew != false) R.string.routine_editor_new_title else R.string.routine_editor_edit_title),
                onBack = onBack,
                actions = {
                    if (ready != null) {
                        if (!ready.isNew) {
                            TextButton(onClick = { onEvent(RoutineEditorEvent.DeleteClicked) }, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
                                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(onClick = { onEvent(RoutineEditorEvent.SaveClicked) }, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            RoutineEditorUiState.Missing -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.routine_missing), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is RoutineEditorUiState.Ready -> EditorContent(state = state, onEvent = onEvent, onAddExercise = onAddExercise, padding = padding)
            else -> Unit
        }
    }

    if (ready?.confirmDelete == true) {
        AlertDialog(
            onDismissRequest = { onEvent(RoutineEditorEvent.DeleteDismissed) },
            title = { Text(stringResource(R.string.routine_delete_title)) },
            text = { Text(stringResource(R.string.routine_delete_body)) },
            confirmButton = {
                TextButton(onClick = { onEvent(RoutineEditorEvent.DeleteConfirmed) }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { onEvent(RoutineEditorEvent.DeleteDismissed) }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun EditorContent(
    state: RoutineEditorUiState.Ready,
    onEvent: (RoutineEditorEvent) -> Unit,
    onAddExercise: () -> Unit,
    padding: PaddingValues,
) {
    // Which dropdown is open is screen-only and not worth surviving a rotation.
    var menuFor by remember { mutableStateOf<String?>(null) }
    var typeMenuFor by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        item {
            val titleError = state.showErrors && state.titleMissing
            OutlinedTextField(
                value = state.title,
                onValueChange = { onEvent(RoutineEditorEvent.TitleChanged(it)) },
                label = { Text(stringResource(R.string.routine_name)) },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(stringResource(R.string.routine_name_missing)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.l),
            )
        }
        if (state.exercises.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.routine_editor_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
                )
            }
        }
        itemsIndexed(state.exercises, key = { _, exercise -> exercise.key }) { index, exercise ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RoutineExerciseCard(
                exercise = exercise,
                weightUnit = state.weightUnit,
                showErrors = state.showErrors,
                canMoveUp = index > 0,
                canMoveDown = index < state.exercises.lastIndex,
                menuExpanded = menuFor == exercise.key,
                typeMenuForSetKey = typeMenuFor,
                onMenuClick = { menuFor = exercise.key },
                onMenuDismiss = { menuFor = null },
                onMoveUp = {
                    menuFor = null
                    onEvent(RoutineEditorEvent.ExerciseMoved(exercise.key, index - 1))
                },
                onMoveDown = {
                    menuFor = null
                    onEvent(RoutineEditorEvent.ExerciseMoved(exercise.key, index + 1))
                },
                onRemove = {
                    menuFor = null
                    onEvent(RoutineEditorEvent.ExerciseRemoved(exercise.key))
                },
                onRestChange = { onEvent(RoutineEditorEvent.RestChanged(exercise.key, it)) },
                onSetTypeClick = { typeMenuFor = it },
                onSetTypeChosen = { setKey, type ->
                    typeMenuFor = null
                    onEvent(RoutineEditorEvent.SetTypeChosen(exercise.key, setKey, type))
                },
                onSetTypeMenuDismiss = { typeMenuFor = null },
                onValueChange = { setKey, column, text -> onEvent(RoutineEditorEvent.ValueChanged(exercise.key, setKey, column, text)) },
                onDeleteSet = { onEvent(RoutineEditorEvent.SetDeleted(exercise.key, it)) },
                onAddSet = { onEvent(RoutineEditorEvent.AddSetClicked(exercise.key)) },
            )
        }
        item {
            AddExerciseButton(onClick = onAddExercise, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s))
        }
    }
}

@Composable
private fun RoutineExerciseCard(
    exercise: RoutineExerciseUi,
    weightUnit: WeightUnit,
    showErrors: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    menuExpanded: Boolean,
    typeMenuForSetKey: String?,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onRestChange: (String) -> Unit,
    onSetTypeClick: (setKey: String) -> Unit,
    onSetTypeChosen: (setKey: String, type: SetType) -> Unit,
    onSetTypeMenuDismiss: () -> Unit,
    onValueChange: (setKey: String, column: SetColumn, text: String) -> Unit,
    onDeleteSet: (setKey: String) -> Unit,
    onAddSet: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
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
                DropdownMenu(expanded = menuExpanded, onDismissRequest = onMenuDismiss) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_move_up)) },
                        enabled = canMoveUp,
                        onClick = onMoveUp,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_move_down)) },
                        enabled = canMoveDown,
                        onClick = onMoveDown,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_remove), color = MaterialTheme.colorScheme.error) },
                        onClick = onRemove,
                    )
                }
            }
        }
        val restError = showErrors && exercise.restInvalid
        OutlinedTextField(
            value = exercise.rest,
            onValueChange = onRestChange,
            label = { Text(stringResource(R.string.routine_rest)) },
            placeholder = { Text(stringResource(R.string.routine_rest_default)) },
            isError = restError,
            supportingText = if (restError) {
                { Text(stringResource(R.string.routine_rest_invalid)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.l, vertical = Spacing.xs),
        )
        PlannedSetTableHeader(columns = exercise.columns, weightUnit = weightUnit, modifier = Modifier.padding(vertical = Spacing.xs))
        exercise.sets.forEach { set ->
            key(set.key) {
                SwipeToDeleteSet(onDelete = { onDeleteSet(set.key) }) {
                    PlannedSetRow(
                        number = set.number,
                        type = set.type,
                        values = set.values,
                        columns = exercise.columns,
                        typeMenuExpanded = set.key == typeMenuForSetKey,
                        onSetTypeClick = { onSetTypeClick(set.key) },
                        onSetTypeChosen = { onSetTypeChosen(set.key, it) },
                        onSetTypeMenuDismiss = onSetTypeMenuDismiss,
                        onValueChange = { column, text -> onValueChange(set.key, column, text) },
                    )
                }
            }
        }
        AddSetButton(onClick = onAddSet, modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s))
    }
}

@Preview(showSystemUi = true)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RoutineEditorScreenPreview() {
    MuskliTheme {
        RoutineEditorScreen(
            state = RoutineEditorUiState.Ready(
                isNew = false,
                title = "Push day",
                exercises = listOf(
                    RoutineExerciseUi(
                        key = "e1",
                        name = "Barbell Bench Press",
                        columns = listOf(SetColumn.WEIGHT, SetColumn.REPS),
                        rest = "120",
                        restInvalid = false,
                        sets = listOf(
                            PlannedSetUi("s1", 1, SetType.WARMUP, listOf("40", "10")),
                            PlannedSetUi("s2", 1, SetType.NORMAL, listOf("80", "8")),
                            PlannedSetUi("s3", 2, SetType.NORMAL, listOf("80", "8")),
                        ),
                    ),
                    RoutineExerciseUi(
                        key = "e2",
                        name = "Plank",
                        columns = listOf(SetColumn.DURATION),
                        rest = "",
                        restInvalid = false,
                        sets = listOf(PlannedSetUi("s4", 1, SetType.NORMAL, listOf("1:00"))),
                    ),
                ),
                weightUnit = WeightUnit.KG,
            ),
            onEvent = {},
            onBack = {},
            onAddExercise = {},
        )
    }
}
