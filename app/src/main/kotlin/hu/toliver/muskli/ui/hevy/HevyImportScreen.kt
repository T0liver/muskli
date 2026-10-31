package hu.toliver.muskli.ui.hevy

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.importer.hevy.HevyParseError
import hu.toliver.muskli.domain.importer.hevy.HevyProblem
import hu.toliver.muskli.domain.model.ExerciseType
import hu.toliver.muskli.ui.components.BackTopBar
import hu.toliver.muskli.ui.library.labelRes
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date

@Composable
fun HevyImportRoute(viewModel: HevyImportViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onEvent(HevyImportEvent.FilePicked(it.toString())) }
    }
    HevyImportScreen(
        state = state,
        onEvent = viewModel::onEvent,
        // Any type: file managers label CSV inconsistently, and the parser decides (D-040).
        onChooseFile = { picker.launch(arrayOf("*/*")) },
        onBack = onBack,
    )
}

@Composable
fun HevyImportScreen(
    state: HevyImportUiState,
    onEvent: (HevyImportEvent) -> Unit,
    onChooseFile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(title = stringResource(R.string.hevy_title), onBack = onBack) },
        bottomBar = {
            if (state is HevyImportUiState.Preview) PreviewButtons(state, onImport = { onEvent(HevyImportEvent.ImportClicked) }, onCancel = onBack)
        },
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding)
        when (state) {
            HevyImportUiState.Pick -> Message(content, text = stringResource(R.string.hevy_pick_hint)) {
                PrimaryAction(stringResource(R.string.hevy_choose_file), onChooseFile)
            }
            HevyImportUiState.Busy -> Box(content, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is HevyImportUiState.Rejected -> Rejected(content, state.reason, onChooseFile)
            is HevyImportUiState.Preview -> PreviewContent(content, state, onEvent)
            is HevyImportUiState.Done -> Message(content, text = doneText(state)) {
                PrimaryAction(stringResource(R.string.hevy_back_to_settings), onBack)
            }
            HevyImportUiState.Failed -> Message(content, text = stringResource(R.string.hevy_failed)) {
                PrimaryAction(stringResource(R.string.hevy_choose_another), onChooseFile)
            }
        }
    }
    (state as? HevyImportUiState.Preview)?.mapping?.let { MappingDialog(it, onEvent) }
}

@Composable
private fun Message(modifier: Modifier, text: String, action: @Composable () -> Unit) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        action()
    }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouchTarget)) { Text(label) }
}

@Composable
private fun Rejected(modifier: Modifier, reason: HevyRejection, onChooseFile: () -> Unit) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Text(stringResource(R.string.hevy_rejected_title), style = MaterialTheme.typography.titleMedium)
        when (reason) {
            HevyRejection.MeasurementsFile -> Text(stringResource(R.string.hevy_measurements_file))
            HevyRejection.ReadFailed -> Text(stringResource(R.string.hevy_read_failed))
            is HevyRejection.Errors -> {
                Text(stringResource(R.string.hevy_rejected_errors))
                reason.errors.take(MAX_ERRORS_SHOWN).forEach { error ->
                    Text(errorText(error), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = Spacing.s))
                }
                val hidden = reason.errors.size - MAX_ERRORS_SHOWN
                if (hidden > 0) Text(pluralStringResource(R.plurals.hevy_more_errors, hidden, hidden))
            }
        }
        PrimaryAction(stringResource(R.string.hevy_choose_another), onChooseFile)
    }
}

@Composable
private fun errorText(error: HevyParseError): String {
    val problem = stringResource(
        when (error.problem) {
            HevyProblem.NOT_HEVY_EXPORT -> R.string.hevy_problem_not_hevy
            HevyProblem.MISSING_COLUMN -> R.string.hevy_problem_missing_column
            HevyProblem.FIELD_COUNT -> R.string.hevy_problem_field_count
            HevyProblem.INVALID_DATE -> R.string.hevy_problem_invalid_date
            HevyProblem.INVALID_NUMBER -> R.string.hevy_problem_invalid_number
            HevyProblem.NEGATIVE_NUMBER -> R.string.hevy_problem_negative_number
            HevyProblem.UNKNOWN_SET_TYPE -> R.string.hevy_problem_unknown_set_type
            HevyProblem.END_BEFORE_START -> R.string.hevy_problem_end_before_start
        },
    )
    return error.column?.let { stringResource(R.string.hevy_error_at_column, error.record, it, problem) }
        ?: stringResource(R.string.hevy_error_at_record, error.record, problem)
}

@Composable
private fun PreviewContent(modifier: Modifier, state: HevyImportUiState.Preview, onEvent: (HevyImportEvent) -> Unit) {
    LazyColumn(modifier = modifier) {
        item { Summary(state.summary) }
        item {
            Column(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s)) {
                Text(stringResource(R.string.hevy_exercises), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.hevy_exercises_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.exercises, key = { it.hevyTitle }) { row ->
            ExerciseRow(row, onClick = { onEvent(HevyImportEvent.RowClicked(row.hevyTitle)) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun Summary(summary: HevyImportSummary) {
    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(pluralStringResource(R.plurals.hevy_summary_in_file, summary.workoutsInFile, summary.workoutsInFile), style = MaterialTheme.typography.titleMedium)
        Text(pluralStringResource(R.plurals.hevy_summary_new, summary.newWorkouts, summary.newWorkouts))
        Text(pluralStringResource(R.plurals.hevy_summary_duplicates, summary.duplicatesSkipped, summary.duplicatesSkipped))
        Text(pluralStringResource(R.plurals.hevy_summary_sets, summary.sets, summary.sets))
        Text(pluralStringResource(R.plurals.hevy_summary_new_customs, summary.newCustomExercises, summary.newCustomExercises))
        if (summary.firstStartEpochMs != null && summary.lastStartEpochMs != null) {
            Text(stringResource(R.string.hevy_summary_dates, formatDate(summary.firstStartEpochMs), formatDate(summary.lastStartEpochMs)))
        }
        if (summary.rpeDropped > 0) {
            Text(
                pluralStringResource(R.plurals.hevy_summary_rpe, summary.rpeDropped, summary.rpeDropped),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDate(epochMs: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMs))

@Composable
private fun ExerciseRow(row: HevyExerciseRow, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        Text(row.hevyTitle, style = MaterialTheme.typography.bodyLarge)
        if (row.unmatched) {
            Text(stringResource(R.string.hevy_unmatched), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Text(resolutionText(row.resolution), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun resolutionText(resolution: HevyRowResolution): String = when (resolution) {
    is HevyRowResolution.Library ->
        stringResource(if (resolution.check) R.string.hevy_resolution_library_check else R.string.hevy_resolution_library, resolution.title)
    is HevyRowResolution.NewCustom -> stringResource(R.string.hevy_resolution_custom, stringResource(resolution.type.labelRes()))
}

@Composable
private fun PreviewButtons(state: HevyImportUiState.Preview, onImport: () -> Unit, onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = Spacing.minTouchTarget)) {
            Text(stringResource(R.string.action_cancel))
        }
        val count = state.summary.newWorkouts
        Button(onClick = onImport, enabled = state.canImport, modifier = Modifier.weight(1f).heightIn(min = Spacing.minTouchTarget)) {
            Text(pluralStringResource(R.plurals.hevy_import_action, count, count))
        }
    }
}

@Composable
private fun MappingDialog(dialog: HevyMappingDialog, onEvent: (HevyImportEvent) -> Unit) {
    val dismiss = { onEvent(HevyImportEvent.MappingDismissed) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(dialog.hevyTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                OptionRow(stringResource(R.string.hevy_map_create_custom), dialog.createCustomSelected) {
                    onEvent(HevyImportEvent.CreateCustomChosen)
                }
                OutlinedTextField(
                    value = dialog.query,
                    onValueChange = { onEvent(HevyImportEvent.QueryChanged(it)) },
                    label = { Text(stringResource(R.string.hevy_map_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn {
                    if (dialog.candidates.isNotEmpty()) {
                        item { OptionHeader(stringResource(R.string.hevy_map_candidates)) }
                        items(dialog.candidates, key = { "candidate-${it.templateId}" }) { option ->
                            OptionRow(option.title, option.selected) { onEvent(HevyImportEvent.TemplateChosen(option.templateId)) }
                        }
                        item { OptionHeader(stringResource(R.string.hevy_map_library)) }
                    }
                    items(dialog.results, key = { it.templateId }) { option ->
                        OptionRow(option.title, option.selected) { onEvent(HevyImportEvent.TemplateChosen(option.templateId)) }
                    }
                    if (dialog.results.isEmpty()) item { Text(stringResource(R.string.hevy_map_no_results), style = MaterialTheme.typography.bodySmall) }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun OptionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = Spacing.xs))
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun doneText(state: HevyImportUiState.Done): String = stringResource(
    R.string.hevy_done,
    pluralStringResource(R.plurals.hevy_done_workouts, state.workouts, state.workouts),
    pluralStringResource(R.plurals.hevy_done_customs, state.newCustomExercises, state.newCustomExercises),
    pluralStringResource(R.plurals.hevy_done_duplicates, state.duplicatesSkipped, state.duplicatesSkipped),
)

private const val MAX_ERRORS_SHOWN = 20

private val sampleSummary = HevyImportSummary(
    workoutsInFile = 2,
    newWorkouts = 2,
    duplicatesSkipped = 0,
    sets = 13,
    rpeDropped = 2,
    firstStartEpochMs = 1_773_295_200_000L,
    lastStartEpochMs = 1_773_507_900_000L,
    newCustomExercises = 3,
)

private val samplePreview = HevyImportUiState.Preview(
    summary = sampleSummary,
    exercises = listOf(
        HevyExerciseRow("Side Raise, cable", HevyRowResolution.NewCustom(ExerciseType.WEIGHT_REPS), unmatched = true),
        HevyExerciseRow("Hanging Knee Raise", HevyRowResolution.NewCustom(ExerciseType.REPS_ONLY), unmatched = true),
        HevyExerciseRow("Bench Press, barbell", HevyRowResolution.Library("Barbell Bench Press", check = true), unmatched = false),
        HevyExerciseRow("Plank", HevyRowResolution.Library("Plank", check = false), unmatched = false),
    ),
)

@Composable
private fun ScreenPreview(state: HevyImportUiState) {
    MuskliTheme { HevyImportScreen(state = state, onEvent = {}, onChooseFile = {}, onBack = {}) }
}

@Preview(name = "Hevy import: pick", showBackground = true)
@Preview(name = "Hevy import: pick, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PickPreview() = ScreenPreview(HevyImportUiState.Pick)

@Preview(name = "Hevy import: busy", showBackground = true)
@Preview(name = "Hevy import: busy, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BusyPreview() = ScreenPreview(HevyImportUiState.Busy)

@Preview(name = "Hevy import: rejected", showBackground = true)
@Preview(name = "Hevy import: rejected, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RejectedPreview() = ScreenPreview(
    HevyImportUiState.Rejected(
        HevyRejection.Errors(
            listOf(
                HevyParseError(HevyProblem.INVALID_DATE, record = 4, column = "start_time"),
                HevyParseError(HevyProblem.FIELD_COUNT, record = 9, column = null),
            ),
        ),
    ),
)

@Preview(name = "Hevy import: measurements file", showBackground = true)
@Preview(name = "Hevy import: measurements file, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MeasurementsPreview() = ScreenPreview(HevyImportUiState.Rejected(HevyRejection.MeasurementsFile))

@Preview(name = "Hevy import: preview", showBackground = true)
@Preview(name = "Hevy import: preview, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewPreview() = ScreenPreview(samplePreview)

@Preview(name = "Hevy import: nothing new", showBackground = true)
@Preview(name = "Hevy import: nothing new, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NothingNewPreview() = ScreenPreview(
    samplePreview.copy(
        summary = sampleSummary.copy(newWorkouts = 0, duplicatesSkipped = 2, sets = 0, rpeDropped = 0, firstStartEpochMs = null, lastStartEpochMs = null, newCustomExercises = 0),
    ),
)

@Preview(name = "Hevy import: mapping", showBackground = true)
@Preview(name = "Hevy import: mapping, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MappingPreview() = ScreenPreview(
    samplePreview.copy(
        mapping = HevyMappingDialog(
            hevyTitle = "Side Raise, cable",
            query = "raise",
            createCustomSelected = true,
            candidates = listOf(HevyMappingOption("a", "Cable Lateral Raise", selected = false)),
            results = listOf(
                HevyMappingOption("b", "Dumbbell Lateral Raise", selected = false),
                HevyMappingOption("c", "Front Raise", selected = false),
            ),
        ),
    ),
)

@Preview(name = "Hevy import: done", showBackground = true)
@Preview(name = "Hevy import: done, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DonePreview() = ScreenPreview(HevyImportUiState.Done(workouts = 2, newCustomExercises = 3, duplicatesSkipped = 0))

@Preview(name = "Hevy import: failed", showBackground = true)
@Preview(name = "Hevy import: failed, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FailedPreview() = ScreenPreview(HevyImportUiState.Failed)
