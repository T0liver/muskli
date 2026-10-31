# T-035 — Hevy import: CSV reader and parser in `:domain`

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-020
- **Refs:** SPEC F-09 · `docs/HEVY-IMPORT.md` §1, §2, §5, §6, §8 · D-040, D-041, D-042, D-045 · TC-HEVY-01..10
- **Size:** fits one session

## Goal
A pure `:domain` function turns the text of a Hevy export file into parsed workouts (or the latest body weight from a measurement file), or rejects it with a full error list. Nothing is matched, stored, or shown yet.

## In scope
- New package `hu.toliver.muskli.domain.importer.hevy` (D-040):
  - An internal RFC 4180 reader: quoted fields, `""` escapes, commas and line breaks inside quotes (normalised to `\n`), CRLF and LF record ends, optional UTF-8 BOM, no final newline. Records carry their 1-based record number.
  - `fun parseHevyExport(text: String, clock: Clock): HevyParseResult` with
    `sealed interface HevyParseResult { data class Workouts(val workouts: List<HevyWorkout>); data class Measurements(val latestBodyWeight: HevyBodyWeight?); data class Rejected(val errors: List<HevyParseError>) }`.
  - `data class HevyWorkout(val title: String, val notes: String?, val startedAtEpochMs: Long, val finishedAtEpochMs: Long, val exercises: List<HevyExercise>)`, `data class HevyExercise(val title: String, val supersetGroup: Int?, val notes: String?, val sets: List<HevySet>)`, `data class HevySet(val data: SetData, val rpe: Double?)`, `data class HevyBodyWeight(val weightKg: Double, val measuredAtEpochMs: Long)`.
  - `data class HevyParseError(val problem: HevyProblem, val record: Int, val column: String?)`, `enum class HevyProblem { NOT_HEVY_EXPORT, MISSING_COLUMN, FIELD_COUNT, INVALID_DATE, INVALID_NUMBER, NEGATIVE_NUMBER, UNKNOWN_SET_TYPE, END_BEFORE_START }`.
  - Mapping exactly as HEVY-IMPORT §2 (grouping, occurrence split, superset renumbering, km to m, completed true, RPE kept on `HevySet` only).
  - Local time to epoch ms per HEVY-IMPORT §5 using only `Clock.utcOffsetS` (no `java.time`: minSdk 24).
- Fixtures: create `domain/src/test/resources/hevy/workout_data.csv` and `measurement_data.csv` **verbatim** from HEVY-IMPORT §8.
- Tests first (JUnit 5) for TC-HEVY-01..10, with a fake `Clock` whose offset can switch at a given instant.

## Out of scope (do NOT do)
- Exercise matching, import plan, duplicate detection (T-036). Any Room, `:data`, or UI code.
- Using the real files in `docs/fixtures/` in tests or committing them (git-ignored personal data).
- Adding any dependency (no CSV library).

## Acceptance criteria
- [x] Fixture files exist under `domain/src/test/resources/hevy/` and match HEVY-IMPORT §8 byte for byte apart from line ends.
- [x] TC-HEVY-01..10 are implemented as tests, were seen failing first, and pass.
- [x] No `java.time`, no Android import, no new dependency in `:domain`.
- [x] `./gradlew :domain:test assembleDebug` pass.

## Verification
`.\gradlew.bat :domain:test assembleDebug`

## Notes / decisions needed
None open: D-040, D-041, D-042, D-045. The parser reads the whole text (the owner's history is small); streaming is not needed unless T-040 measures a problem.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- **2026-10-31 · BUILD.** Done, branch `t-035-hevy-csv-parser`.
  - **Code** (`:domain` `importer/hevy/`): `HevyExport.kt` holds the public types exactly as in this ticket. `CsvReader.kt` is the internal RFC 4180 `readCsv` and `CsvRecord(number, fields)`. `HevyLocalTime.kt` has `parseHevyLocalTime` (strict `d MMM yyyy, HH:mm`, calendar-checked, days-from-civil, no `java.time`) and `localToEpochMs` (HEVY-IMPORT §5 probe algorithm). `HevyParser.kt` has `parseHevyExport`: kind from header, columns by name, all-or-nothing errors, and the §2 mapping (grouping by first appearance, occurrence split, superset renumbering, km to m in `BigDecimal`, completed true, side BOTH, RPE on `HevySet`).
  - **Tests** (written first; all 23 seen failing on `TODO()` stubs, then passing): `CsvReaderTest` covers TC-HEVY-02 syntax. `HevyParserTest` covers TC-HEVY-01, file-level TC-HEVY-02, and TC-HEVY-03..10, with a `SwitchingClock` fake for the DST cases.
  - **Fixtures**: `domain/src/test/resources/hevy/{workout,measurement}_data.csv` were copied from §8 and compared programmatically with the doc blocks (identical after line-end normalisation). Nothing from `docs/fixtures/` is used or committed.
  - **Decisions:** D-055 covers parser details (kind detection so a missing `exercise_title` gives MISSING_COLUMN, required columns, number and date syntax, error granularity, lenient quoting). D-056 fixes the fixture set count: §8 said 9 / 14 sets, but its own breakdown and the verbatim fixture give **8 / 13**. The TC-HEVY-01 count assertion therefore expects 13; HEVY-IMPORT §8, TC-HEVY-01, TC-HEVY-18, and T-038's AC were corrected to match. Reviewer: please confirm this is the intended reading.
  - **Gotchas:** TC-HEVY-01's "`completedAtEpochMs` null" has no field at this level (`HevySet` wraps `SetData` only); it applies when T-036 builds `WorkoutSet`. Workout-level `createdAt`/`updatedAt` and ids are also T-036's job, so `parseHevyExport` uses `Clock` only for offsets.
  - **Commands:** `.\gradlew.bat :domain:test --tests "...importer.hevy.*"` failed first (23/23), then passed. `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` gave BUILD SUCCESSFUL: 205 domain tests, 0 failures; lint 0 errors, 1 warning (the existing JUnit BOM `NewerVersionAvailable`, not new). `rg "java\.time|android\." domain/.../importer` found no matches. No Gradle file changed.
  - **Left:** nothing for this ticket; matching and the plan are T-036.
