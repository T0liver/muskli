package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.ui.theme.MuskliTheme

/** Picks a date, then a time of day, starting from [initialEpochMs]; [onPicked] gets the combined local time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialogs(initialEpochMs: Long, onPicked: (epochMs: Long) -> Unit, onDismiss: () -> Unit) {
    var picked by rememberSaveable { mutableLongStateOf(initialEpochMs) }
    var choosingTime by rememberSaveable { mutableStateOf(false) }
    if (!choosingTime) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = pickerDateMillis(initialEpochMs))
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { picked = withPickedDate(picked, it) }
                    choosingTime = true
                }) { Text(stringResource(R.string.action_next)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = dateState) }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = localHour(picked),
            initialMinute = localMinute(picked),
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { onPicked(withTimeOfDay(picked, timeState.hour, timeState.minute)) }) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
            text = { TimePicker(state = timeState) },
        )
    }
}

@Preview(name = "Date then time picker")
@Preview(name = "Date then time picker: dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DateTimePickerDialogsPreview() {
    MuskliTheme {
        DateTimePickerDialogs(initialEpochMs = 1_793_390_000_000L, onPicked = {}, onDismiss = {})
    }
}
