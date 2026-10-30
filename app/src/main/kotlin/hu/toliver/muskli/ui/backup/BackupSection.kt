package hu.toliver.muskli.ui.backup

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.repo.ImportError
import hu.toliver.muskli.domain.repo.ImportProblem
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

/** Export/import buttons plus their dialogs; the caller owns the system file pickers. */
@Composable
fun BackupSection(
    state: BackupUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onEvent: (BackupEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(
            text = stringResource(R.string.backup_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onExport,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouchTarget),
        ) { Text(stringResource(R.string.backup_export)) }
        OutlinedButton(
            onClick = onImport,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouchTarget),
        ) { Text(stringResource(R.string.backup_import)) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }

    if (state.pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { onEvent(BackupEvent.ImportCancelled) },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { onEvent(BackupEvent.ImportConfirmed) }) {
                    Text(stringResource(R.string.backup_import_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(BackupEvent.ImportCancelled) }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    state.message?.let { message ->
        val dismiss = { onEvent(BackupEvent.MessageShown) }
        AlertDialog(
            onDismissRequest = dismiss,
            title = (message as? BackupMessage.Rejected)?.let { { Text(stringResource(R.string.backup_rejected_title)) } },
            text = { MessageText(message) },
            confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

@Composable
private fun MessageText(message: BackupMessage) {
    when (message) {
        BackupMessage.Exported -> Text(stringResource(R.string.backup_exported))
        BackupMessage.ExportFailed -> Text(stringResource(R.string.backup_export_failed))
        BackupMessage.ReadFailed -> Text(stringResource(R.string.backup_read_failed))
        is BackupMessage.Imported ->
            Text(pluralStringResource(R.plurals.backup_imported, message.workouts, message.workouts))
        is BackupMessage.Rejected -> Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(stringResource(R.string.backup_rejected_text))
            message.errors.take(MAX_ERRORS_SHOWN).forEach { error ->
                Text(
                    text = errorText(error),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = Spacing.s),
                )
            }
            val hidden = message.errors.size - MAX_ERRORS_SHOWN
            if (hidden > 0) Text(pluralStringResource(R.plurals.backup_more_errors, hidden, hidden))
        }
    }
}

@Composable
private fun errorText(error: ImportError): String = when (error.problem) {
    ImportProblem.MALFORMED -> stringResource(R.string.backup_problem_malformed, error.where)
    ImportProblem.UNSUPPORTED_VERSION -> stringResource(R.string.backup_problem_unsupported_version)
    ImportProblem.INVALID_VALUE -> stringResource(R.string.backup_problem_invalid_value, error.where)
    ImportProblem.DUPLICATE_ID -> stringResource(R.string.backup_problem_duplicate_id, error.where)
    ImportProblem.UNKNOWN_EXERCISE -> stringResource(R.string.backup_problem_unknown_exercise, error.where)
    ImportProblem.MULTIPLE_DRAFTS -> stringResource(R.string.backup_problem_multiple_drafts)
    ImportProblem.ID_CONFLICT -> stringResource(R.string.backup_problem_id_conflict, error.where)
}

private const val MAX_ERRORS_SHOWN = 20

@Preview(name = "Backup: rejected file", showBackground = true)
@Preview(name = "Backup: dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BackupSectionPreview() {
    MuskliTheme {
        BackupSection(
            state = BackupUiState(
                message = BackupMessage.Rejected(
                    listOf(
                        ImportError(ImportProblem.INVALID_VALUE, "workouts[0].exercises[0].sets[0].type"),
                        ImportError(ImportProblem.UNKNOWN_EXERCISE, "workouts[0].exercises[1].exerciseTemplateId"),
                    ),
                ),
            ),
            onExport = {},
            onImport = {},
            onEvent = {},
        )
    }
}
