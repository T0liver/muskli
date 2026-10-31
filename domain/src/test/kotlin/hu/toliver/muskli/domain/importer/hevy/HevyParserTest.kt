package hu.toliver.muskli.domain.importer.hevy

import hu.toliver.muskli.domain.TestClock
import hu.toliver.muskli.domain.importer.hevy.HevyProblem.END_BEFORE_START
import hu.toliver.muskli.domain.importer.hevy.HevyProblem.FIELD_COUNT
import hu.toliver.muskli.domain.importer.hevy.HevyProblem.INVALID_DATE
import hu.toliver.muskli.domain.importer.hevy.HevyProblem.INVALID_NUMBER
import hu.toliver.muskli.domain.importer.hevy.HevyProblem.MISSING_COLUMN
import hu.toliver.muskli.domain.importer.hevy.HevyProblem.NEGATIVE_NUMBER
import hu.toliver.muskli.domain.importer.hevy.HevyProblem.NOT_HEVY_EXPORT
import hu.toliver.muskli.domain.importer.hevy.HevyProblem.UNKNOWN_SET_TYPE
import hu.toliver.muskli.domain.model.SetData
import hu.toliver.muskli.domain.model.SetType
import hu.toliver.muskli.domain.model.Side
import hu.toliver.muskli.domain.repo.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HevyParserTest {
    /** Offset [beforeS] until [switchAtMs], then [afterS], like a DST transition. */
    private class SwitchingClock(private val switchAtMs: Long, private val beforeS: Int, private val afterS: Int) : Clock {
        override fun nowEpochMs(): Long = 0L

        override fun utcOffsetS(epochMs: Long): Int = if (epochMs < switchAtMs) beforeS else afterS
    }

    private val clock = TestClock(offsetS = 3600)

    private fun resource(name: String): String =
        requireNotNull(javaClass.getResource("/hevy/$name")) { "missing fixture $name" }.readText()

    private val workoutFixture = resource("workout_data.csv")
    private val measurementFixture = resource("measurement_data.csv")

    private fun csv(rows: List<List<String>>): String =
        rows.joinToString("\r\n") { row -> row.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" } }

    private val header = listOf(
        "title", "start_time", "end_time", "description", "exercise_title", "superset_id", "exercise_notes",
        "set_index", "set_type", "weight_kg", "reps", "distance_km", "duration_seconds", "rpe",
    )

    private fun row(
        start: String = "12 Mar 2026, 07:00",
        end: String = "12 Mar 2026, 07:45",
        exercise: String = "Squat",
        setIndex: String = "0",
        setType: String = "normal",
        weight: String = "100",
        reps: String = "5",
    ) = listOf("W", start, end, "", exercise, "", "", setIndex, setType, weight, reps, "", "", "")

    private fun set(type: SetType = SetType.NORMAL, weightKg: Double? = null, reps: Int? = null, durationS: Int? = null, distanceM: Double? = null, rpe: Double? = null) =
        HevySet(SetData(type, weightKg, reps, durationS, distanceM, completed = true, side = Side.BOTH), rpe)

    private val expectedFixture = HevyParseResult.Workouts(
        listOf(
            HevyWorkout(
                title = "Push, heavy",
                notes = "Felt \"strong\"",
                startedAtEpochMs = 1773507900000,
                finishedAtEpochMs = 1773511920000,
                exercises = listOf(
                    HevyExercise(
                        "Bench Press (Barbell)", null, "Pause reps",
                        listOf(
                            set(SetType.WARMUP, weightKg = 40.0, reps = 10),
                            set(weightKg = 82.5, reps = 5, rpe = 8.5),
                            set(SetType.FAILURE, weightKg = 82.5, reps = 4, rpe = 10.0),
                        ),
                    ),
                    HevyExercise(
                        "Lateral Raise (Dumbbell)", 0, null,
                        listOf(set(weightKg = 10.0, reps = 12), set(SetType.DROPSET, weightKg = 6.0, reps = 10)),
                    ),
                    HevyExercise("Pull Up", 0, null, listOf(set(reps = 8))),
                    HevyExercise("Plank", null, "line one\nline two", listOf(set(durationS = 60))),
                    HevyExercise("Bench Press (Barbell)", null, null, listOf(set(weightKg = 60.0, reps = 12))),
                ),
            ),
            HevyWorkout(
                title = "Legs",
                notes = null,
                startedAtEpochMs = 1773295200000,
                finishedAtEpochMs = 1773297900000,
                exercises = listOf(
                    HevyExercise(
                        "Goblet Squat (Kettlebell)", null, null,
                        listOf(set(weightKg = 24.0, reps = 10), set(weightKg = 24.0, reps = 10)),
                    ),
                    HevyExercise("Assisted Pull Up", null, null, listOf(set(weightKg = 20.0, reps = 6))),
                    HevyExercise("Farmers Walk (Dumbbell)", null, null, listOf(set(weightKg = 32.0, distanceM = 40.0))),
                    HevyExercise("Rowing Machine", null, null, listOf(set(distanceM = 5020.0, durationS = 1260))),
                ),
            ),
        ),
    )

    @Test
    fun `TC-HEVY-01 workout fixture parses to the documented values`() {
        val result = parseHevyExport(workoutFixture, clock)

        assertEquals(expectedFixture, result)
        val sets = assertIs<HevyParseResult.Workouts>(result).workouts.flatMap { w -> w.exercises.flatMap { it.sets } }
        assertEquals(13, sets.size)
        assertEquals(listOf(5, 4), result.workouts.map { it.exercises.size })
        assertEquals(listOf(8, 5), result.workouts.map { w -> w.exercises.sumOf { it.sets.size } })
        assertEquals(true, sets.all { it.data.completed && it.data.side == Side.BOTH })
        assertEquals(2, sets.count { it.rpe != null })
    }

    @Test
    fun `TC-HEVY-02 line ends, BOM, and final newline do not change the result`() {
        val lf = workoutFixture.replace("\r\n", "\n")
        val crlf = lf.replace("\n", "\r\n")

        assertEquals(expectedFixture, parseHevyExport(lf, clock))
        assertEquals(expectedFixture, parseHevyExport(crlf, clock))
        assertEquals(expectedFixture, parseHevyExport("\uFEFF" + crlf, clock))
        assertEquals(expectedFixture, parseHevyExport(crlf.trimEnd('\r', '\n'), clock))
    }

    private val fixtureRows = readCsv(workoutFixture).map { it.fields }

    private fun withColumns(transform: (List<String>) -> List<String>): String =
        csv(fixtureRows.map(transform))

    @Test
    fun `TC-HEVY-03 columns in another order give the same result`() {
        val reversed = withColumns { it.reversed() }

        assertEquals(expectedFixture, parseHevyExport(reversed, clock))
    }

    @Test
    fun `TC-HEVY-03 an extra unknown column is ignored`() {
        var first = true
        val extra = withColumns { fields ->
            val added = if (first) "unknown_column" else "ignored, \"value\""
            first = false
            listOf(added) + fields
        }

        assertEquals(expectedFixture, parseHevyExport(extra, clock))
    }

    @Test
    fun `TC-HEVY-03 a missing exercise_title column rejects the file`() {
        val column = header.indexOf("exercise_title")
        val missing = withColumns { fields -> fields.filterIndexed { i, _ -> i != column } }

        assertEquals(
            HevyParseResult.Rejected(listOf(HevyParseError(MISSING_COLUMN, 1, "exercise_title"))),
            parseHevyExport(missing, clock),
        )
    }

    @Test
    fun `TC-HEVY-04 the same exercise twice in one workout stays two occurrences`() {
        val text = csv(
            listOf(
                header,
                row(exercise = "Squat", setIndex = "0", weight = "100"),
                row(exercise = "Squat", setIndex = "1", weight = "110"),
                row(exercise = "Squat", setIndex = "0", weight = "60"),
                row(exercise = "Squat", setIndex = "0", weight = "50"),
            ),
        )

        val workouts = assertIs<HevyParseResult.Workouts>(parseHevyExport(text, clock)).workouts

        assertEquals(1, workouts.size)
        assertEquals(
            listOf(listOf(100.0, 110.0), listOf(60.0), listOf(50.0)),
            workouts.single().exercises.map { e -> e.sets.map { it.data.weightKg } },
        )
        assertEquals(listOf("Squat", "Squat", "Squat"), workouts.single().exercises.map { it.title })
    }

    @Test
    fun `TC-HEVY-05 every problem is reported with record and column and nothing is returned`() {
        val text = csv(
            listOf(
                header,
                row(),
                row(setType = "superset"),
                row(start = "32 Mar 2026, 07:00"),
                row(weight = "-5"),
                row(reps = "5.5"),
                row(start = "12 Mar 2026, 07:00", end = "12 Mar 2026, 06:59"),
                row().dropLast(1),
            ),
        )

        assertEquals(
            HevyParseResult.Rejected(
                listOf(
                    HevyParseError(UNKNOWN_SET_TYPE, 3, "set_type"),
                    HevyParseError(INVALID_DATE, 4, "start_time"),
                    HevyParseError(NEGATIVE_NUMBER, 5, "weight_kg"),
                    HevyParseError(INVALID_NUMBER, 6, "reps"),
                    HevyParseError(END_BEFORE_START, 7, "end_time"),
                    HevyParseError(FIELD_COUNT, 8, null),
                ),
            ),
            parseHevyExport(text, clock),
        )
    }

    private fun startOf(local: String, clock: Clock): Long {
        val text = csv(listOf(header, row(start = local, end = local)))
        return assertIs<HevyParseResult.Workouts>(parseHevyExport(text, clock)).workouts.single().startedAtEpochMs
    }

    @Test
    fun `TC-HEVY-06 a time in the DST gap moves forward by the gap`() {
        val springForward = SwitchingClock(1774746000000, beforeS = 3600, afterS = 7200)

        assertEquals(1774747800000, startOf("29 Mar 2026, 02:30", springForward))
    }

    @Test
    fun `TC-HEVY-07 a time in the DST overlap takes the earliest instant`() {
        val fallBack = SwitchingClock(1792890000000, beforeS = 7200, afterS = 3600)

        assertEquals(1792888200000, startOf("25 Oct 2026, 02:30", fallBack))
    }

    @Test
    fun `TC-HEVY-08 one and two digit days are equal`() {
        assertEquals(startOf("05 Mar 2026, 07:00", clock), startOf("5 Mar 2026, 07:00", clock))
    }

    @Test
    fun `TC-HEVY-08 localised month and 12 hour time are rejected`() {
        for (bad in listOf("5 Mär 2026, 07:00", "5 Mar 2026, 7:00 PM")) {
            val text = csv(listOf(header, row(start = bad, end = "05 Mar 2026, 08:00")))

            assertEquals(
                HevyParseResult.Rejected(listOf(HevyParseError(INVALID_DATE, 2, "start_time"))),
                parseHevyExport(text, clock),
                bad,
            )
        }
    }

    @Test
    fun `TC-HEVY-09 measurement fixture gives the latest body weight`() {
        assertEquals(
            HevyParseResult.Measurements(HevyBodyWeight(80.4, 1772350200000)),
            parseHevyExport(measurementFixture, clock),
        )
    }

    @Test
    fun `TC-HEVY-09 a measurement file without any weight has no body weight`() {
        val text = csv(listOf(listOf("date", "weight_kg", "waist_cm"), listOf("05 Mar 2026, 00:00", "", "88")))

        assertEquals(HevyParseResult.Measurements(null), parseHevyExport(text, clock))
    }

    @Test
    fun `TC-HEVY-10 file kind comes from the header`() {
        val notHevy = HevyParseResult.Rejected(listOf(HevyParseError(NOT_HEVY_EXPORT, 1, null)))

        assertIs<HevyParseResult.Workouts>(parseHevyExport(workoutFixture, clock))
        assertIs<HevyParseResult.Measurements>(parseHevyExport(measurementFixture, clock))
        assertEquals(
            notHevy,
            parseHevyExport("{\"schemaVersion\":1,\"exportedAtEpochMs\":0,\"exercises\":[],\"workouts\":[]}", clock),
        )
        assertEquals(notHevy, parseHevyExport("", clock))
    }
}
