package hu.toliver.muskli.ui.library

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.ui.components.BackTopBar
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditorScreen(state: ExerciseEditorState, onEvent: (LibraryEvent) -> Unit, modifier: Modifier = Modifier) {
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(
                title = stringResource(if (state.editingId == null) R.string.editor_create_title else R.string.editor_edit_title),
                onBack = { onEvent(LibraryEvent.EditorDismiss) },
                actions = {
                    TextButton(onClick = { onEvent(LibraryEvent.EditorSave) }, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
                        Text(stringResource(R.string.editor_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            val titleError = state.showErrors && state.titleMissing
            OutlinedTextField(
                value = state.title,
                onValueChange = { onEvent(LibraryEvent.EditorTitleChanged(it)) },
                label = { Text(stringResource(R.string.editor_name)) },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(stringResource(R.string.editor_name_missing)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            ChoiceRow(
                label = stringResource(R.string.editor_type),
                selected = state.type,
                options = ExerciseType.entries,
                optionLabel = { stringResource(it.labelRes()) },
                error = false,
                onSelect = { onEvent(LibraryEvent.EditorTypeChanged(it)) },
            )
            ChoiceRow(
                label = stringResource(R.string.editor_primary_muscle),
                selected = state.primaryMuscle,
                options = MuscleGroup.entries,
                optionLabel = { stringResource(it.labelRes()) },
                error = state.showErrors && state.primaryMuscleMissing,
                onSelect = { onEvent(LibraryEvent.EditorPrimaryMuscleChanged(it)) },
            )
            ChoiceRow(
                label = stringResource(R.string.editor_equipment),
                selected = state.equipment,
                options = Equipment.entries,
                optionLabel = { stringResource(it.labelRes()) },
                error = state.showErrors && state.equipmentMissing,
                onSelect = { onEvent(LibraryEvent.EditorEquipmentChanged(it)) },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.minTouchTarget)
                    .toggleable(
                        value = state.unilateral,
                        role = Role.Switch,
                        onValueChange = { onEvent(LibraryEvent.EditorUnilateralChanged(it)) },
                    ),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.editor_unilateral), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.editor_unilateral_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.unilateral, onCheckedChange = null)
            }
            HorizontalDivider()
            Text(stringResource(R.string.editor_secondary_muscles), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                MuscleGroup.entries.filter { it != state.primaryMuscle }.forEach { muscle ->
                    FilterChip(
                        selected = muscle in state.secondaryMuscles,
                        onClick = { onEvent(LibraryEvent.EditorSecondaryMuscleToggled(muscle)) },
                        label = { Text(stringResource(muscle.labelRes())) },
                        modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
                    )
                }
            }
            if (state.editingId != null) {
                HorizontalDivider()
                OutlinedButton(
                    onClick = { confirmArchive = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.minTouchTarget),
                ) { Text(stringResource(R.string.editor_archive)) }
            }
        }
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(stringResource(R.string.editor_archive_confirm_title)) },
            text = { Text(stringResource(R.string.editor_archive_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmArchive = false
                    onEvent(LibraryEvent.EditorArchive)
                }) { Text(stringResource(R.string.editor_archive_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun <T : Any> ChoiceRow(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    error: Boolean,
    onSelect: (T) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (error) {
                Text(stringResource(R.string.editor_required), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Box {
            TextButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
                Text(if (selected == null) stringResource(R.string.editor_choose) else optionLabel(selected))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
    }
}

@Preview(name = "Editor: new, errors", showBackground = true)
@Preview(name = "Editor: new, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExerciseEditorNewPreview() {
    MuskliTheme {
        ExerciseEditorScreen(state = ExerciseEditorState(showErrors = true), onEvent = {})
    }
}

@Preview(name = "Editor: edit, large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun ExerciseEditorEditPreview() {
    MuskliTheme {
        ExerciseEditorScreen(
            state = ExerciseEditorState(
                editingId = "c1",
                title = "Cable Fly (Low)",
                primaryMuscle = MuscleGroup.CHEST,
                secondaryMuscles = setOf(MuscleGroup.SHOULDERS),
                equipment = Equipment.MACHINE,
                unilateral = true,
            ),
            onEvent = {},
        )
    }
}
