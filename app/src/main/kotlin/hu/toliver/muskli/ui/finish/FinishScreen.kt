package hu.toliver.muskli.ui.finish

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.toliver.muskli.R
import hu.toliver.muskli.ui.components.BackTopBar
import hu.toliver.muskli.ui.components.DateTimePickerDialogs
import hu.toliver.muskli.ui.components.formatDateTime
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

@Composable
fun FinishRoute(viewModel: FinishViewModel, onBack: () -> Unit, onDone: (FinishOutcome) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        FinishUiState.Loading -> Unit
        is FinishUiState.Ready -> FinishScreen(state = current, onEvent = viewModel::onEvent, onBack = onBack)
        is FinishUiState.Done -> LaunchedEffect(current) { onDone(current.outcome) }
    }
}

@Composable
fun FinishScreen(
    state: FinishUiState.Ready,
    onEvent: (FinishEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickingStart by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(
                title = stringResource(if (state.editingPast) R.string.finish_title_edit else R.string.finish_title),
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = { onEvent(FinishEvent.SaveClicked) },
                        enabled = state.durationValid,
                        modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
                    ) { Text(stringResource(R.string.action_save)) }
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
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = { onEvent(FinishEvent.TitleChanged(it)) },
                label = { Text(stringResource(R.string.finish_name)) },
                placeholder = { Text(stringResource(R.string.workout_untitled)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.finish_start), style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { pickingStart = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.minTouchTarget),
            ) { Text(formatDateTime(state.startedAtEpochMs)) }
            Text(stringResource(R.string.finish_duration), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                DurationField(state.hours, R.string.finish_hours, state.durationValid, Modifier.weight(1f)) {
                    onEvent(FinishEvent.HoursChanged(it))
                }
                DurationField(state.minutes, R.string.finish_minutes, state.durationValid, Modifier.weight(1f)) {
                    onEvent(FinishEvent.MinutesChanged(it))
                }
            }
            if (!state.durationValid) {
                Text(
                    stringResource(R.string.finish_duration_invalid),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onEvent(FinishEvent.NotesChanged(it)) },
                label = { Text(stringResource(R.string.finish_notes)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (pickingStart) {
        DateTimePickerDialogs(
            initialEpochMs = state.startedAtEpochMs,
            onPicked = {
                onEvent(FinishEvent.StartChanged(it))
                pickingStart = false
            },
            onDismiss = { pickingStart = false },
        )
    }

    when (val dialog = state.dialog) {
        is FinishDialog.DiscardSets -> AlertDialog(
            onDismissRequest = { onEvent(FinishEvent.DialogDismissed) },
            title = { Text(pluralStringResource(R.plurals.finish_discard_sets_title, dialog.count, dialog.count)) },
            text = { Text(stringResource(R.string.finish_discard_sets_body)) },
            confirmButton = {
                TextButton(onClick = { onEvent(FinishEvent.DiscardSetsConfirmed) }) { Text(stringResource(R.string.finish_discard_sets_confirm)) }
            },
            dismissButton = { TextButton(onClick = { onEvent(FinishEvent.DialogDismissed) }) { Text(stringResource(R.string.action_cancel)) } },
        )
        FinishDialog.DiscardWorkout -> AlertDialog(
            onDismissRequest = { onEvent(FinishEvent.DialogDismissed) },
            title = { Text(stringResource(R.string.finish_nothing_title)) },
            text = { Text(stringResource(R.string.finish_nothing_body)) },
            confirmButton = {
                TextButton(onClick = { onEvent(FinishEvent.DiscardWorkoutConfirmed) }) {
                    Text(stringResource(R.string.finish_discard_workout), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { onEvent(FinishEvent.DialogDismissed) }) { Text(stringResource(R.string.finish_keep)) } },
        )
        null -> Unit
    }
}

@Composable
private fun DurationField(value: String, @StringRes label: Int, valid: Boolean, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        isError = !valid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Preview(showSystemUi = true)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FinishScreenPreview() {
    MuskliTheme {
        FinishScreen(
            state = FinishUiState.Ready(
                title = "Push day",
                notes = "",
                startedAtEpochMs = 1_793_390_000_000L,
                hours = "1",
                minutes = "5",
                durationValid = true,
                editingPast = false,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
