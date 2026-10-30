package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import androidx.annotation.StringRes
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import hu.toliver.muskli.domain.settings.WeightUnit
import hu.toliver.muskli.ui.theme.MuskliTheme
import hu.toliver.muskli.ui.theme.SetValueTextStyle
import hu.toliver.muskli.ui.theme.Spacing

// Header and rows share these so the columns line up at every font scale.
private const val PREVIOUS_WEIGHT = 1.4f
private const val VALUE_WEIGHT = 1f

@Composable
fun SetTableHeader(columns: List<SetColumn>, weightUnit: WeightUnit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.l),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderText(stringResource(R.string.column_set), Modifier.size(width = Spacing.minTouchTarget, height = Spacing.xl))
        HeaderText(stringResource(R.string.column_previous), Modifier.weight(PREVIOUS_WEIGHT))
        columns.forEach { HeaderText(stringResource(it.labelRes(weightUnit)), Modifier.weight(VALUE_WEIGHT)) }
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
 * One set: SET | PREVIOUS | the exercise's value columns | check. Pre-filled values sit in the
 * fields as real values, so logging a set as planned is a single tap on the check. Tapping the
 * set number opens the type menu (F-03 AC3).
 */
@Composable
fun SetRow(
    set: WorkoutSetUi,
    columns: List<SetColumn>,
    typeMenuExpanded: Boolean,
    onSetTypeClick: () -> Unit,
    onSetTypeChosen: (SetType) -> Unit,
    onSetTypeMenuDismiss: () -> Unit,
    onValueChange: (column: SetColumn, value: String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowColor = if (set.completed) MuskliTheme.workoutColors.completedRow else MaterialTheme.colorScheme.surface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowColor)
            .padding(horizontal = Spacing.l, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            SetTypeCell(number = set.number, type = set.type, onClick = onSetTypeClick)
            SetTypeMenu(expanded = typeMenuExpanded, onChosen = onSetTypeChosen, onDismiss = onSetTypeMenuDismiss)
        }
        PreviousText(set.previous)
        columns.forEachIndexed { index, column ->
            SetValueField(
                value = set.values.getOrElse(index) { "" },
                onValueChange = { onValueChange(column, it) },
                keyboardType = if (column == SetColumn.REPS || column == SetColumn.DURATION) KeyboardType.Number else KeyboardType.Decimal,
                completed = set.completed,
                contentDescription = stringResource(column.descriptionRes(), set.number),
                modifier = Modifier.weight(VALUE_WEIGHT),
            )
        }
        CompleteButton(number = set.number, completed = set.completed, onCompletedChange = onCompletedChange)
    }
}

@StringRes
private fun SetColumn.labelRes(unit: WeightUnit): Int {
    val lb = unit == WeightUnit.LB
    return when (this) {
        SetColumn.WEIGHT -> if (lb) R.string.column_lb else R.string.column_kg
        SetColumn.ADDED_WEIGHT -> if (lb) R.string.column_plus_lb else R.string.column_plus_kg
        SetColumn.ASSISTANCE -> if (lb) R.string.column_minus_lb else R.string.column_minus_kg
        SetColumn.REPS -> R.string.column_reps
        SetColumn.DURATION -> R.string.column_time
        SetColumn.DISTANCE_KM -> R.string.column_km
        SetColumn.DISTANCE_M -> R.string.column_m
    }
}

@StringRes
private fun SetColumn.descriptionRes(): Int = when (this) {
    SetColumn.WEIGHT -> R.string.cd_weight
    SetColumn.ADDED_WEIGHT -> R.string.cd_added_weight
    SetColumn.ASSISTANCE -> R.string.cd_assistance
    SetColumn.REPS -> R.string.cd_reps
    SetColumn.DURATION -> R.string.cd_duration
    SetColumn.DISTANCE_KM, SetColumn.DISTANCE_M -> R.string.cd_distance
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
private fun SetTypeMenu(expanded: Boolean, onChosen: (SetType) -> Unit, onDismiss: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        listOf(
            SetType.NORMAL to R.string.set_type_menu_normal,
            SetType.WARMUP to R.string.set_type_menu_warmup,
            SetType.DROPSET to R.string.set_type_menu_dropset,
            SetType.FAILURE to R.string.set_type_menu_failure,
        ).forEach { (type, label) ->
            DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { onChosen(type) })
        }
    }
}

@Composable
private fun RowScope.PreviousText(previous: List<String>?) {
    val none = stringResource(R.string.previous_none)
    val parts = previous?.map { it.ifEmpty { none } }
    val text = when {
        parts.isNullOrEmpty() -> none
        parts.size == 2 -> stringResource(R.string.previous_value, parts[0], parts[1])
        else -> parts.first()
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
    val weightReps = listOf(SetColumn.WEIGHT, SetColumn.REPS)
    MuskliTheme {
        Surface {
            Column {
                SetTableHeader(columns = weightReps, weightUnit = WeightUnit.KG)
                listOf(
                    WorkoutSetUi("1", 1, SetType.WARMUP, listOf("40", "10"), listOf("40", "10"), completed = true),
                    WorkoutSetUi("2", 1, SetType.NORMAL, listOf("80", "8"), listOf("80", "8"), completed = true),
                    WorkoutSetUi("3", 2, SetType.NORMAL, listOf("80", "8"), listOf("80", "8"), completed = false),
                    WorkoutSetUi("4", 3, SetType.DROPSET, listOf("60", "10"), listOf("60", "10"), completed = false),
                    WorkoutSetUi("5", 4, SetType.FAILURE, null, listOf("", ""), completed = false),
                ).forEach { set ->
                    SetRow(
                        set = set,
                        columns = weightReps,
                        typeMenuExpanded = false,
                        onSetTypeClick = {},
                        onSetTypeChosen = {},
                        onSetTypeMenuDismiss = {},
                        onValueChange = { _, _ -> },
                        onCompletedChange = {},
                    )
                }
                SetTableHeader(columns = listOf(SetColumn.DURATION), weightUnit = WeightUnit.KG)
                SetRow(
                    set = WorkoutSetUi("6", 1, SetType.NORMAL, listOf("1:30"), listOf("1:30"), completed = false),
                    columns = listOf(SetColumn.DURATION),
                    typeMenuExpanded = false,
                    onSetTypeClick = {},
                    onSetTypeChosen = {},
                    onSetTypeMenuDismiss = {},
                    onValueChange = { _, _ -> },
                    onCompletedChange = {},
                )
            }
        }
    }
}
