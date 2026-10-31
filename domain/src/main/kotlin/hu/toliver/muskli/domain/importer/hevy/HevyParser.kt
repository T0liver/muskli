package hu.toliver.muskli.domain.importer.hevy

import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side
import hu.toliver.muskli.domain.repo.Clock
import java.math.BigDecimal

private const val TITLE = "title"
private const val START_TIME = "start_time"
private const val END_TIME = "end_time"
private const val DESCRIPTION = "description"
private const val EXERCISE_TITLE = "exercise_title"
private const val SUPERSET_ID = "superset_id"
private const val EXERCISE_NOTES = "exercise_notes"
private const val SET_INDEX = "set_index"
private const val SET_TYPE = "set_type"
private const val WEIGHT_KG = "weight_kg"
private const val REPS = "reps"
private const val DISTANCE_KM = "distance_km"
private const val DURATION_SECONDS = "duration_seconds"
private const val RPE = "rpe"
private const val DATE = "date"

private val WORKOUT_COLUMNS = listOf(
    TITLE, START_TIME, END_TIME, DESCRIPTION, EXERCISE_TITLE, SUPERSET_ID, EXERCISE_NOTES,
    SET_INDEX, SET_TYPE, WEIGHT_KG, REPS, DISTANCE_KM, DURATION_SECONDS, RPE,
)

/** `weight_kg` is shared with the measurement file, so it does not identify a workout file. */
private val WORKOUT_ONLY_COLUMNS = WORKOUT_COLUMNS - WEIGHT_KG
private val MEASUREMENT_COLUMNS = listOf(DATE, WEIGHT_KG)

private val SET_TYPES = mapOf(
    "normal" to SetType.NORMAL,
    "warmup" to SetType.WARMUP,
    "dropset" to SetType.DROPSET,
    "failure" to SetType.FAILURE,
)

private val PLAIN_NUMBER = Regex("""-?\d+(\.\d+)?""")

private const val HEADER_RECORD = 1

/**
 * Reads the text of a Hevy export (docs/HEVY-IMPORT.md §1, §2, §5, §6). The file kind comes from the
 * header (D-055); local times are resolved with [clock]'s offsets (D-041). All or nothing (D-040): any
 * problem rejects the file with every error found.
 */
fun parseHevyExport(text: String, clock: Clock): HevyParseResult {
    val records = readCsv(text)
    val header = records.firstOrNull()?.fields?.map { it.trim() }
    val rows = records.drop(1)
    return when {
        header == null -> notHevyExport()
        WORKOUT_ONLY_COLUMNS.any { it in header } -> parseWorkouts(header, rows, clock)
        DATE in header -> parseMeasurements(header, rows, clock)
        else -> notHevyExport()
    }
}

private fun notHevyExport() = HevyParseResult.Rejected(listOf(HevyParseError(HevyProblem.NOT_HEVY_EXPORT, HEADER_RECORD, null)))

private fun missingColumns(header: List<String>, required: List<String>): HevyParseResult.Rejected? =
    required.filter { it !in header }
        .map { HevyParseError(HevyProblem.MISSING_COLUMN, HEADER_RECORD, it) }
        .takeIf { it.isNotEmpty() }
        ?.let { HevyParseResult.Rejected(it) }

private class WorkoutRow(
    val workoutKey: Triple<String, String, String>,
    val title: String,
    val notes: String?,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val exerciseTitle: String,
    val supersetId: Int?,
    val exerciseNotes: String?,
    val setIndex: Int,
    val set: HevySet,
)

private fun parseWorkouts(header: List<String>, records: List<CsvRecord>, clock: Clock): HevyParseResult {
    missingColumns(header, WORKOUT_COLUMNS)?.let { return it }
    val errors = mutableListOf<HevyParseError>()
    val rows = records.mapNotNull { record -> RowReader(header, record, clock, errors).readWorkoutRow() }
    if (errors.isNotEmpty()) return HevyParseResult.Rejected(errors)
    val workouts = rows.groupBy { it.workoutKey }.values.map(::toWorkout)
    return HevyParseResult.Workouts(workouts)
}

private fun toWorkout(rows: List<WorkoutRow>): HevyWorkout {
    val supersetGroups = rows.mapNotNull { it.supersetId }.distinct().withIndex().associate { (index, id) -> id to index }
    val occurrences = mutableListOf<MutableList<WorkoutRow>>()
    var previous: WorkoutRow? = null
    for (row in rows) {
        val startsOccurrence = previous == null ||
            row.exerciseTitle != previous.exerciseTitle ||
            row.supersetId != previous.supersetId ||
            row.setIndex <= previous.setIndex
        if (startsOccurrence) occurrences += mutableListOf(row) else occurrences.last() += row
        previous = row
    }
    val first = rows.first()
    return HevyWorkout(
        title = first.title,
        notes = first.notes,
        startedAtEpochMs = first.startedAtEpochMs,
        finishedAtEpochMs = first.finishedAtEpochMs,
        exercises = occurrences.map { sets ->
            HevyExercise(
                title = sets.first().exerciseTitle,
                supersetGroup = sets.first().supersetId?.let(supersetGroups::getValue),
                notes = sets.first().exerciseNotes,
                sets = sets.map { it.set },
            )
        },
    )
}

private fun parseMeasurements(header: List<String>, records: List<CsvRecord>, clock: Clock): HevyParseResult {
    missingColumns(header, MEASUREMENT_COLUMNS)?.let { return it }
    val errors = mutableListOf<HevyParseError>()
    val weights = records.mapNotNull { record -> RowReader(header, record, clock, errors).readBodyWeight() }
    if (errors.isNotEmpty()) return HevyParseResult.Rejected(errors)
    return HevyParseResult.Measurements(weights.maxByOrNull { it.measuredAtEpochMs })
}

/** Reads one data record; every problem is added to [errors] and makes the read return null. */
private class RowReader(
    private val header: List<String>,
    private val record: CsvRecord,
    private val clock: Clock,
    private val errors: MutableList<HevyParseError>,
) {
    private val errorsBefore = errors.size

    private fun fieldCountOk(): Boolean {
        if (record.fields.size == header.size) return true
        errors += HevyParseError(HevyProblem.FIELD_COUNT, record.number, null)
        return false
    }

    private fun hasErrors() = errors.size > errorsBefore

    private fun fail(problem: HevyProblem, column: String): Nothing? {
        errors += HevyParseError(problem, record.number, column)
        return null
    }

    private fun text(column: String): String = record.fields[header.indexOf(column)]

    private fun textOrNull(column: String): String? = text(column).ifEmpty { null }

    private fun date(column: String): Long? =
        parseHevyLocalTime(text(column))?.let { localToEpochMs(it, clock) } ?: fail(HevyProblem.INVALID_DATE, column)

    /** Empty gives null; otherwise a plain non-negative decimal with a `.` point. */
    private fun number(column: String): BigDecimal? {
        val value = text(column).trim().ifEmpty { return null }
        if (!PLAIN_NUMBER.matches(value)) return fail(HevyProblem.INVALID_NUMBER, column)
        val number = BigDecimal(value)
        return if (number.signum() < 0) fail(HevyProblem.NEGATIVE_NUMBER, column) else number
    }

    private fun integer(column: String): Int? {
        val number = number(column) ?: return null
        return runCatching { number.intValueExact() }.getOrNull() ?: fail(HevyProblem.INVALID_NUMBER, column)
    }

    private fun requiredInteger(column: String): Int? =
        if (text(column).isBlank()) fail(HevyProblem.INVALID_NUMBER, column) else integer(column)

    private fun setType(): SetType? = SET_TYPES[text(SET_TYPE)] ?: fail(HevyProblem.UNKNOWN_SET_TYPE, SET_TYPE)

    fun readWorkoutRow(): WorkoutRow? {
        if (!fieldCountOk()) return null
        val start = date(START_TIME)
        val end = date(END_TIME)
        if (start != null && end != null && end < start) fail(HevyProblem.END_BEFORE_START, END_TIME)
        val supersetId = integer(SUPERSET_ID)
        val setIndex = requiredInteger(SET_INDEX)
        val type = setType()
        val weightKg = number(WEIGHT_KG)?.toDouble()
        val reps = integer(REPS)
        val distanceM = number(DISTANCE_KM)?.movePointRight(3)?.toDouble()
        val durationS = integer(DURATION_SECONDS)
        val rpe = number(RPE)?.toDouble()
        if (hasErrors() || start == null || end == null || setIndex == null || type == null) return null
        return WorkoutRow(
            workoutKey = Triple(text(TITLE), text(START_TIME), text(END_TIME)),
            title = text(TITLE).trim(),
            notes = textOrNull(DESCRIPTION),
            startedAtEpochMs = start,
            finishedAtEpochMs = end,
            exerciseTitle = text(EXERCISE_TITLE),
            supersetId = supersetId,
            exerciseNotes = textOrNull(EXERCISE_NOTES),
            setIndex = setIndex,
            set = HevySet(
                data = SetData(
                    type = type,
                    weightKg = weightKg,
                    reps = reps,
                    durationS = durationS,
                    distanceM = distanceM,
                    completed = true,
                    side = Side.BOTH,
                ),
                rpe = rpe,
            ),
        )
    }

    fun readBodyWeight(): HevyBodyWeight? {
        if (!fieldCountOk()) return null
        val measuredAt = date(DATE)
        val weightKg = number(WEIGHT_KG)?.toDouble()
        if (hasErrors() || measuredAt == null || weightKg == null) return null
        return HevyBodyWeight(weightKg, measuredAt)
    }
}
