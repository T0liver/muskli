package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.ui.theme.MuskliTheme

/** Opened from the exercise menu (F-02 AC2), so reordering adds nothing to the logging rows. */
@Composable
fun ReorderDialog(
    exercises: List<WorkoutExerciseUi>,
    onMove: (exerciseId: String, toIndex: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
        title = { Text(stringResource(R.string.reorder_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                exercises.forEachIndexed { index, exercise ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(exercise.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onMove(exercise.id, index - 1) }, enabled = index > 0) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_up),
                                contentDescription = stringResource(R.string.cd_move_up, exercise.name),
                            )
                        }
                        IconButton(onClick = { onMove(exercise.id, index + 1) }, enabled = index < exercises.lastIndex) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_down),
                                contentDescription = stringResource(R.string.cd_move_down, exercise.name),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Preview(name = "Reorder dialog")
@Preview(name = "Reorder dialog: dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReorderDialogPreview() {
    MuskliTheme {
        ReorderDialog(
            exercises = listOf("Bench Press (Barbell)", "Lateral Raise (Dumbbell)", "Bent-Over Row (Barbell)").map {
                WorkoutExerciseUi(id = it, templateId = it, name = it, columns = emptyList(), sets = emptyList())
            },
            onMove = { _, _ -> },
            onDismiss = {},
        )
    }
}
