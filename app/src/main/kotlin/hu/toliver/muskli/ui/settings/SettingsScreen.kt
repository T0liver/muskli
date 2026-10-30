package hu.toliver.muskli.ui.settings

import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.calc.PreviousMode
import hu.toliver.muskli.domain.settings.WeekStartDay
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.backup.BackupEvent
import hu.toliver.muskli.ui.backup.BackupSection
import hu.toliver.muskli.ui.backup.BackupUiState
import hu.toliver.muskli.ui.backup.BackupViewModel
import hu.toliver.muskli.ui.components.BackTopBar
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val backup by backupViewModel.uiState.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME)) { uri ->
        uri?.let { backupViewModel.onEvent(BackupEvent.ExportTo(it.toString())) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { backupViewModel.onEvent(BackupEvent.ImportPicked(it.toString())) }
    }
    val fileName = stringResource(R.string.backup_file_name, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        backup = backup,
        onExport = { exportLauncher.launch(fileName) },
        // Some file managers label .json files as plain text or as unknown bytes.
        onImport = { importLauncher.launch(arrayOf(JSON_MIME, "text/*", "application/octet-stream")) },
        onBackupEvent = backupViewModel::onEvent,
    )
}

private const val JSON_MIME = "application/json"

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backup: BackupUiState = BackupUiState(),
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    onBackupEvent: (BackupEvent) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.l, vertical = Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            SectionTitle(R.string.settings_units)
            UnitSelector(state.weightUnit) { onEvent(SettingsEvent.WeightUnitChanged(it)) }

            HorizontalDivider()
            SwitchRow(R.string.settings_default_rest, state.restEnabled) { onEvent(SettingsEvent.RestEnabledChanged(it)) }
            if (state.restEnabled) {
                NumberField(
                    value = state.restSecondsText,
                    label = stringResource(R.string.settings_rest_seconds),
                    error = if (state.restSecondsInvalid) stringResource(R.string.settings_rest_invalid) else null,
                    keyboardType = KeyboardType.Number,
                    onValueChange = { onEvent(SettingsEvent.RestSecondsChanged(it)) },
                )
            }

            HorizontalDivider()
            SectionTitle(R.string.settings_previous)
            RadioRow(R.string.settings_previous_anywhere, selected = state.previousMode == PreviousMode.ANYWHERE) {
                onEvent(SettingsEvent.PreviousModeChanged(PreviousMode.ANYWHERE))
            }
            // Routine-scoped mode is stored by the repository but only becomes selectable with routines (T-024).
            RadioRow(
                R.string.settings_previous_routine,
                selected = state.previousMode == PreviousMode.ROUTINE_SCOPED,
                enabled = false,
                supporting = stringResource(R.string.settings_previous_routine_unavailable),
            ) {}

            HorizontalDivider()
            SwitchRow(R.string.settings_keep_awake, state.keepScreenAwake) { onEvent(SettingsEvent.KeepScreenAwakeChanged(it)) }
            SwitchRow(R.string.settings_sound, state.soundEnabled) { onEvent(SettingsEvent.SoundChanged(it)) }
            SwitchRow(R.string.settings_vibration, state.vibrationEnabled) { onEvent(SettingsEvent.VibrationChanged(it)) }

            HorizontalDivider()
            WeekStartRow(state.weekStart) { onEvent(SettingsEvent.WeekStartChanged(it)) }

            HorizontalDivider()
            val unitLabel = stringResource(state.weightUnit.labelRes())
            NumberField(
                value = state.bodyWeightText,
                label = stringResource(R.string.settings_body_weight, unitLabel),
                error = if (state.bodyWeightInvalid) stringResource(R.string.settings_body_weight_invalid) else null,
                supporting = stringResource(R.string.settings_body_weight_hint),
                keyboardType = KeyboardType.Decimal,
                onValueChange = { onEvent(SettingsEvent.BodyWeightChanged(it)) },
            )

            HorizontalDivider()
            SectionTitle(R.string.settings_backup)
            BackupSection(state = backup, onExport = onExport, onImport = onImport, onEvent = onBackupEvent)
        }
    }
}

@Composable
private fun SectionTitle(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = Spacing.s),
    )
}

@Composable
private fun UnitSelector(selected: WeightUnit, onSelect: (WeightUnit) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        WeightUnit.entries.forEachIndexed { index, unit ->
            SegmentedButton(
                selected = unit == selected,
                onClick = { onSelect(unit) },
                shape = SegmentedButtonDefaults.itemShape(index, WeightUnit.entries.size),
                modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
            ) { Text(stringResource(unit.labelRes())) }
        }
    }
}

@Composable
private fun SwitchRow(@StringRes label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    ) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun RadioRow(
    @StringRes label: Int,
    selected: Boolean,
    enabled: Boolean = true,
    supporting: String? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column {
            val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = color)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeekStartRow(selected: WeekStartDay, onSelect: (WeekStartDay) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget),
    ) {
        Text(stringResource(R.string.settings_week_start), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { expanded = true }, modifier = Modifier.heightIn(min = Spacing.minTouchTarget)) {
                Text(stringResource(selected.labelRes()))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                WeekStartDay.entries.forEach { day ->
                    DropdownMenuItem(
                        text = { Text(stringResource(day.labelRes())) },
                        onClick = {
                            expanded = false
                            onSelect(day)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    label: String,
    error: String?,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@StringRes
private fun WeightUnit.labelRes(): Int = when (this) {
    WeightUnit.KG -> R.string.unit_kg
    WeightUnit.LB -> R.string.unit_lb
}

@StringRes
private fun WeekStartDay.labelRes(): Int = when (this) {
    WeekStartDay.MONDAY -> R.string.day_monday
    WeekStartDay.TUESDAY -> R.string.day_tuesday
    WeekStartDay.WEDNESDAY -> R.string.day_wednesday
    WeekStartDay.THURSDAY -> R.string.day_thursday
    WeekStartDay.FRIDAY -> R.string.day_friday
    WeekStartDay.SATURDAY -> R.string.day_saturday
    WeekStartDay.SUNDAY -> R.string.day_sunday
}

@Preview(name = "Settings: defaults", showBackground = true)
@Preview(name = "Settings: dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Settings: large font", showBackground = true, fontScale = 1.5f)
@Composable
private fun SettingsScreenPreview() {
    MuskliTheme {
        SettingsScreen(state = SettingsUiState(), onEvent = {}, onBack = {})
    }
}

@Preview(name = "Settings: lb, rest off, invalid body weight", showBackground = true)
@Composable
private fun SettingsScreenEditedPreview() {
    MuskliTheme {
        SettingsScreen(
            state = SettingsUiState(
                weightUnit = WeightUnit.LB,
                restEnabled = false,
                restSecondsText = "",
                weekStart = WeekStartDay.SUNDAY,
                bodyWeightText = "abc",
                bodyWeightInvalid = true,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
