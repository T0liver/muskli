package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

@Composable
fun AddSetButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget),
    ) {
        AddLabel(stringResource(R.string.action_add_set))
    }
}

@Composable
fun AddExerciseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchTarget),
    ) {
        AddLabel(stringResource(R.string.action_add_exercise))
    }
}

@Composable
private fun AddLabel(text: String) {
    Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(Spacing.l + Spacing.xs))
    Spacer(Modifier.width(Spacing.s))
    Text(text = text, style = MaterialTheme.typography.labelLarge)
}

@Preview(name = "Add buttons", showBackground = true)
@Preview(name = "Add buttons: dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddButtonsPreview() {
    MuskliTheme {
        Surface {
            Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                AddSetButton(onClick = {})
                AddExerciseButton(onClick = {})
            }
        }
    }
}
