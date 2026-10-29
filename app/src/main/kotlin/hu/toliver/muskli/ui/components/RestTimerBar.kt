package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.Spacing

/** Shown only while a rest is running, so it never adds a control to logging itself (F-05 AC4). */
@Composable
fun RestTimerBar(
    timer: RestTimerUi,
    onMinus15: () -> Unit,
    onPlus15: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(
                progress = { timer.progress },
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.rest_timer_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = timer.remaining, style = MaterialTheme.typography.headlineSmall)
                }
                val minusDescription = stringResource(R.string.cd_rest_minus_15)
                OutlinedButton(
                    onClick = onMinus15,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .heightIn(min = Spacing.minTouchTarget)
                        .semantics { contentDescription = minusDescription },
                ) {
                    Text(stringResource(R.string.rest_minus_15))
                }
                val plusDescription = stringResource(R.string.cd_rest_plus_15)
                OutlinedButton(
                    onClick = onPlus15,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .heightIn(min = Spacing.minTouchTarget)
                        .semantics { contentDescription = plusDescription },
                ) {
                    Text(stringResource(R.string.rest_plus_15))
                }
                Button(
                    onClick = onSkip,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
                ) {
                    Text(stringResource(R.string.rest_skip))
                }
            }
        }
    }
}

@Preview(name = "Rest timer bar", showBackground = true)
@Preview(name = "Rest timer bar: dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RestTimerBarPreview() {
    MuskliTheme {
        RestTimerBar(timer = RestTimerUi(remaining = "1:24", progress = 0.6f), onMinus15 = {}, onPlus15 = {}, onSkip = {})
    }
}
