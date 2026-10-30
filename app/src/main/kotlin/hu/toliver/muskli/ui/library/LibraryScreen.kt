package hu.toliver.muskli.ui.library

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.Equipment
import hu.toliver.muskli.domain.model.ExerciseTemplate
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.domain.model.MuscleGroup
import hu.toliver.muskli.ui.components.BackTopBar
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

@Composable
fun LibraryRoute(
    mode: LibraryMode,
    onBack: () -> Unit,
    onPicked: (ids: List<String>) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(state = state, mode = mode, onEvent = viewModel::onEvent, onBack = onBack, onPicked = onPicked)
}

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    mode: LibraryMode,
    onEvent: (LibraryEvent) -> Unit,
    onBack: () -> Unit,
    onPicked: (ids: List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editor = state.editor
    if (editor != null) {
        BackHandler { onEvent(LibraryEvent.EditorDismiss) }
        ExerciseEditorScreen(state = editor, onEvent = onEvent, modifier = modifier)
        return
    }
    val onItemClick: (ExerciseTemplate) -> Unit = { exercise ->
        when (mode) {
            LibraryMode.PICK -> onEvent(LibraryEvent.SelectionToggled(exercise.id))
            LibraryMode.BROWSE -> if (exercise.isCustom) onEvent(LibraryEvent.EditClicked(exercise.id))
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(
                title = stringResource(if (mode == LibraryMode.PICK) R.string.library_pick_title else R.string.library_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { onEvent(LibraryEvent.CreateClicked) }) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.cd_create_exercise))
                    }
                },
            )
        },
        bottomBar = {
            if (mode == LibraryMode.PICK) {
                Surface(tonalElevation = Spacing.xs) {
                    Button(
                        onClick = { onPicked(state.selectedIds) },
                        enabled = state.selectedIds.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(Spacing.l)
                            .heightIn(min = Spacing.minTouchTarget),
                    ) { Text(stringResource(R.string.library_add_selected, state.selectedIds.size)) }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onEvent(LibraryEvent.QueryChanged(it)) },
                placeholder = { Text(stringResource(R.string.library_search)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.l)
                    .testTag(SEARCH_FIELD_TAG),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.xs),
            ) {
                FilterDropdown(
                    label = stringResource(R.string.library_filter_muscle),
                    selected = state.muscle,
                    options = MuscleGroup.entries,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelect = { onEvent(LibraryEvent.MuscleFilterChanged(it)) },
                )
                FilterDropdown(
                    label = stringResource(R.string.library_filter_equipment),
                    selected = state.equipment,
                    options = Equipment.entries,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelect = { onEvent(LibraryEvent.EquipmentFilterChanged(it)) },
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                if (state.recents.isNotEmpty()) {
                    section(R.string.library_recent, state.recents, "recent", mode, state.selectedIds, onItemClick)
                }
                section(R.string.library_all, state.results, "all", mode, state.selectedIds, onItemClick)
                if (state.results.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.library_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.l),
                        )
                    }
                }
            }
        }
    }
}

const val SEARCH_FIELD_TAG = "library_search"

private fun LazyListScope.section(
    @StringRes title: Int,
    exercises: List<ExerciseTemplate>,
    keyPrefix: String,
    mode: LibraryMode,
    selectedIds: List<String>,
    onClick: (ExerciseTemplate) -> Unit,
) {
    item(key = "$keyPrefix-header") {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Spacing.l, end = Spacing.l, top = Spacing.m, bottom = Spacing.xs),
        )
    }
    // The same exercise may be in Recent and All, so keys carry the section.
    items(exercises, key = { "$keyPrefix-${it.id}" }) { exercise ->
        ExerciseRow(exercise, mode, selected = exercise.id in selectedIds, onClick = { onClick(exercise) })
    }
}

@Composable
private fun ExerciseRow(exercise: ExerciseTemplate, mode: LibraryMode, selected: Boolean, onClick: () -> Unit) {
    val details = stringResource(
        R.string.library_item_details,
        stringResource(exercise.primaryMuscle.labelRes()),
        stringResource(exercise.equipment.labelRes()),
    )
    ListItem(
        headlineContent = { Text(exercise.title) },
        supportingContent = {
            Text(if (exercise.isCustom) stringResource(R.string.library_item_details, details, stringResource(R.string.library_custom)) else details)
        },
        trailingContent = if (mode == LibraryMode.PICK) {
            { Checkbox(checked = selected, onCheckedChange = null) }
        } else {
            null
        },
        colors = if (selected && mode == LibraryMode.PICK) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.colors()
        },
        modifier = Modifier
            .heightIn(min = Spacing.minTouchTarget)
            .clickable(enabled = mode == LibraryMode.PICK || exercise.isCustom, onClick = onClick),
    )
}

@Composable
private fun <T : Any> FilterDropdown(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(if (selected == null) label else optionLabel(selected)) },
            modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.library_filter_any)) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
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

private fun previewExercise(id: String, title: String, muscle: MuscleGroup, equipment: Equipment, custom: Boolean = false) =
    ExerciseTemplate(
        id = id,
        title = title,
        type = ExerciseType.WEIGHT_REPS,
        primaryMuscle = muscle,
        secondaryMuscles = emptySet(),
        equipment = equipment,
        isCustom = custom,
        archived = false,
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0,
    )

private val previewState = LibraryUiState(
    recents = listOf(previewExercise("bench-press-barbell", "Bench Press (Barbell)", MuscleGroup.CHEST, Equipment.BARBELL)),
    results = listOf(
        previewExercise("bench-press-barbell", "Bench Press (Barbell)", MuscleGroup.CHEST, Equipment.BARBELL),
        previewExercise("c1", "Cable Fly (Low)", MuscleGroup.CHEST, Equipment.MACHINE, custom = true),
        previewExercise("squat-barbell", "Squat (Barbell)", MuscleGroup.QUADRICEPS, Equipment.BARBELL),
    ),
)

@Preview(name = "Library: browse", showBackground = true)
@Preview(name = "Library: browse dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LibraryBrowsePreview() {
    MuskliTheme {
        LibraryScreen(state = previewState, mode = LibraryMode.BROWSE, onEvent = {}, onBack = {}, onPicked = {})
    }
}

@Preview(name = "Library: pick, filtered", showBackground = true)
@Preview(name = "Library: pick, large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun LibraryPickPreview() {
    MuskliTheme {
        LibraryScreen(
            state = previewState.copy(muscle = MuscleGroup.CHEST, recents = emptyList(), selectedIds = listOf("c1")),
            mode = LibraryMode.PICK,
            onEvent = {},
            onBack = {},
            onPicked = {},
        )
    }
}
