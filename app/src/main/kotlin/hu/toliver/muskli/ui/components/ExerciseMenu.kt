package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hu.toliver.muskli.R
import hu.toliver.muskli.ui.theme.MuskliTheme

/** Per-exercise actions from F-02 AC2, and the exercise's history (F-06 AC4). Anchor it in the same Box as the menu button. */
@Composable
fun ExerciseMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onHistory: () -> Unit,
    onReorder: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text(stringResource(R.string.menu_history)) }, onClick = onHistory)
        DropdownMenuItem(text = { Text(stringResource(R.string.menu_reorder)) }, onClick = onReorder)
        DropdownMenuItem(text = { Text(stringResource(R.string.menu_replace)) }, onClick = onReplace)
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_remove), color = MaterialTheme.colorScheme.error) },
            onClick = onRemove,
        )
    }
}

@Preview(name = "Exercise menu", showBackground = true, heightDp = 240)
@Preview(name = "Exercise menu: dark", showBackground = true, heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExerciseMenuPreview() {
    MuskliTheme {
        Surface {
            Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.TopEnd) {
                ExerciseMenu(expanded = true, onDismiss = {}, onHistory = {}, onReorder = {}, onReplace = {}, onRemove = {})
            }
        }
    }
}
