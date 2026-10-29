package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.SetValueTextStyle
import hu.toliver.muskli.ui.theme.Spacing

// Header and rows share these so the columns line up at every font scale.
private const val PREVIOUS_WEIGHT = 1.4f
private const val VALUE_WEIGHT = 1f

@Composable
fun SetTableHeader(weightColumnLabel: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderText(stringResource(R.string.column_set), Modifier.size(width = Spacing.minTouchTarget, height = Spacing.xl))
        HeaderText(stringResource(R.string.column_previous), Modifier.weight(PREVIOUS_WEIGHT))
        HeaderText(weightColumnLabel, Modifier.weight(VALUE_WEIGHT))
        HeaderText(stringResource(R.string.column_reps), Modifier.weight(VALUE_WEIGHT))
        Box(Modifier.size(width = Spacing.minTouchTarget, height = Spacing.xl), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.cd_column_completed),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Spacing.l),
            )
        }
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * One set: SET | PREVIOUS | weight | REPS | check. Pre-filled values sit in the fields as real
 * values, so logging a set as planned is a single tap on the check.
 */
@Composable
fun SetRow(
    set: WorkoutSetUi,
    onSetTypeClick: () -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowColor = if (set.completed) MuskliTheme.workoutColors.completedRow else Color.Transparent
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowColor)
            .padding(horizontal = Spacing.l, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetTypeCell(number = set.number, type = set.type, onClick = onSetTypeClick)
        PreviousText(set.previousWeight, set.previousReps)
        SetValueField(
            value = set.weight,
            onValueChange = onWeightChange,
            keyboardType = KeyboardType.Decimal,
            completed = set.completed,
            contentDescription = stringResource(R.string.cd_weight, set.number),
            modifier = Modifier.weight(VALUE_WEIGHT),
        )
        SetValueField(
            value = set.reps,
            onValueChange = onRepsChange,
            keyboardType = KeyboardType.Number,
            completed = set.completed,
            contentDescription = stringResource(R.string.cd_reps, set.number),
            modifier = Modifier.weight(VALUE_WEIGHT),
        )
        CompleteButton(number = set.number, completed = set.completed, onCompletedChange = onCompletedChange)
    }
}

@Composable
private fun SetTypeCell(number: Int, type: SetType, onClick: () -> Unit) {
    val colors = MuskliTheme.workoutColors
    val (label, color) = when (type) {
        SetType.NORMAL -> number.toString() to MaterialTheme.colorScheme.onSurface
        SetType.WARMUP -> stringResource(R.string.set_marker_warmup) to colors.warmup
        SetType.DROPSET -> stringResource(R.string.set_marker_dropset) to colors.dropset
        SetType.FAILURE -> stringResource(R.string.set_marker_failure) to colors.failure
    }
    val typeName = stringResource(
        when (type) {
            SetType.NORMAL -> R.string.set_type_normal
            SetType.WARMUP -> R.string.set_type_warmup
            SetType.DROPSET -> R.string.set_type_dropset
            SetType.FAILURE -> R.string.set_type_failure
        },
    )
    val description = stringResource(R.string.cd_set_type, number, typeName)
    val background = if (type == SetType.NORMAL) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        color.copy(alpha = 0.16f)
    }
    Box(
        modifier = Modifier
            .sizeIn(minWidth = Spacing.minTouchTarget, minHeight = Spacing.minTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = SetValueTextStyle, color = color)
    }
}

@Composable
private fun RowScope.PreviousText(weight: String?, reps: String?) {
    val text = if (weight != null && reps != null) {
        stringResource(R.string.previous_value, weight, reps)
    } else {
        stringResource(R.string.previous_none)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2,
        modifier = Modifier.weight(PREVIOUS_WEIGHT),
    )
}

@Composable
private fun SetValueField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    completed: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val fieldColor = if (completed) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = SetValueTextStyle.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.minTouchTarget)
                    .clip(MaterialTheme.shapes.small)
                    .background(fieldColor)
                    .padding(horizontal = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
private fun CompleteButton(number: Int, completed: Boolean, onCompletedChange: (Boolean) -> Unit) {
    val description = stringResource(R.string.cd_complete_set, number)
    val background = if (completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh
    val tint = if (completed) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(Spacing.minTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .toggleable(value = completed, role = Role.Checkbox, onValueChange = onCompletedChange)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = tint)
    }
}

@Preview(name = "Set rows: types and states", showBackground = true)
@Preview(name = "Set rows: dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetRowPreview() {
    MuskliTheme {
        Surface {
            Column {
                SetTableHeader(weightColumnLabel = stringResource(R.string.column_kg))
                listOf(
                    WorkoutSetUi("1", 1, SetType.WARMUP, "40", "10", "40", "10", completed = true),
                    WorkoutSetUi("2", 1, SetType.NORMAL, "80", "8", "80", "8", completed = true),
                    WorkoutSetUi("3", 2, SetType.NORMAL, "80", "8", "80", "8", completed = false),
                    WorkoutSetUi("4", 3, SetType.DROPSET, "60", "10", "60", "10", completed = false),
                    WorkoutSetUi("5", 4, SetType.FAILURE, null, null, "", "", completed = false),
                ).forEach { set ->
                    SetRow(set = set, onSetTypeClick = {}, onWeightChange = {}, onRepsChange = {}, onCompletedChange = {})
                }
            }
        }
    }
}
