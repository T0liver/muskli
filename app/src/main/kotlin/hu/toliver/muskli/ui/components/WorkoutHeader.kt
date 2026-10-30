package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

/**
 * Title bar, then the stopwatch (tap to edit start time, F-02 AC1), volume and completed sets (F-02 AC4).
 * Editing a past workout swaps the title and the Finish label (T-016).
 */
@Composable
fun WorkoutHeader(
    duration: String,
    volume: String,
    completedSets: Int,
    onDurationClick: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes title: Int = R.string.live_workout_title,
    @StringRes finishLabel: Int = R.string.action_finish,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.minTouchTarget + Spacing.l)
                .padding(horizontal = Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onFinish,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
            ) {
                Text(stringResource(finishLabel), style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.s, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            val durationDescription = stringResource(R.string.cd_edit_duration, duration)
            Stat(
                label = stringResource(R.string.stat_duration),
                value = duration,
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onDurationClick)
                    .clearAndSetSemantics {
                        contentDescription = durationDescription
                        role = Role.Button
                    },
            )
            Stat(label = stringResource(R.string.stat_volume), value = volume, modifier = Modifier.weight(1f))
            Stat(label = stringResource(R.string.stat_sets), value = completedSets.toString(), modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    Column(
        modifier = modifier
            .heightIn(min = Spacing.minTouchTarget)
            .padding(horizontal = Spacing.s, vertical = Spacing.xs),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.headlineSmall, color = valueColor)
    }
}

@Preview(name = "Workout header", showBackground = true)
@Preview(name = "Workout header: dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WorkoutHeaderPreview() {
    MuskliTheme {
        Surface {
            WorkoutHeader(
                duration = "42:17",
                volume = stringResource(R.string.volume_kg, "3,240"),
                completedSets = 9,
                onDurationClick = {},
                onFinish = {},
            )
        }
    }
}
