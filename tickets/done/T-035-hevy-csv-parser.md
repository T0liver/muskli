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
- **2026-10-31 · VERIFY.** Build `16b8241` on `t-035-hevy-csv-parser`, Windows host, no device: the ticket has no UI and no `AC-*` steps, so verification is the suites plus the fixture row count. **Pass; moved to `done/`.**
  - Fixture count: header + **13** data records (8 + 5); the fixture is identical to `main`'s HEVY-IMPORT §8 block (see REVIEW). TC-HEVY-01 asserts 13 sets, `[5, 4]` occurrences, and `[8, 5]` sets per workout, and it passes.
  - `.\gradlew.bat :domain:test --rerun testDebugUnitTest assembleDebug` gave BUILD SUCCESSFUL: domain 205/0 failures (CsvReaderTest 9/0, HevyParserTest 14/0), data 62/0, app 108/0; debug APK built. Lint from the REVIEW run: 0 errors, 1 existing warning.
  - No failures, so no bug ticket.
- **2026-10-31 · REVIEW.** Diff `main...t-035-hevy-csv-parser` (commit `c78addf`) against this ticket and WORKFLOW §5. **No blockers; moved to `verify/`.**
  - **13 vs 14 sets (D-056): accepted.** I counted the committed fixture myself with a quote-aware record count: 14 records = header + **13 data records** (Push, heavy 3 + 2 + 1 + 1 + 1 = 8; Legs 2 + 1 + 1 + 1 = 5). The committed `workout_data.csv` and `measurement_data.csv` are identical to the `csv` blocks in **`main`'s** HEVY-IMPORT §8 after line-end normalisation (no BOM, CRLF). The branch's doc diff touches only the two "Expected" lines, not the fixture block. So the fixture was not edited to fit the test. The old "14" was most likely the record count including the header, and "9" a slip in the sum. The expectation change is proved by the spec's own fixture.
  - **Checklist:** (1) Scope matches the ticket; no `:data`/UI/matching code, no Gradle or `libs.versions.toml` change. (2) Tests assert the TC-HEVY-01..10 values literally (epoch ms for start/end, DST gap `1774747800000`, overlap `1792888200000`, body weight `1772350200000`); TC-HEVY-05 has one error per problem with record numbers 3..8 and a 13-field record. Only the D-056 count was changed, as justified above. (3) Pure `:domain`, public types exactly as the ticket lists. (4) No I/O; pure text in. (5) No UI. (6) Readable, no dead code, no TODOs. `rg "java\.time|import android" domain/src` finds only two older comments (`Clock.kt`, `Settings.kt`). Only `docs/fixtures/.gitkeep` is tracked (`.gitignore:16`); none of the real export's workout titles appear in the test fixtures.
  - **Logic checked by reading:** occurrence split (title, superset, `set_index` not increasing) means each occurrence's sets are already in rising `set_index` order, so §2's "sort by `set_index`" holds. Superset renumbering is per workout in order of first appearance. `distance_km` is scaled in `BigDecimal`. The §5 probe algorithm matches (gap gives `L − oBefore`, overlap gives the earliest valid candidate). `days_from_civil` uses `Math.floorDiv` (Java 8, fine on minSdk 24).
  - **Should-fix (fixed, `793a750`):** TC-HEVY-01's "`completedAtEpochMs` null" cannot be asserted on `HevySet`, and T-036 (where `WorkoutSet` is built) did not mention it, so the check would have been lost. I added it to T-036's in-scope list.
  - **Nit:** trailing blank lines after the last record (for example `\n\n`) become a one-field record and give FIELD_COUNT. Hevy writes no final newline, so I left it; T-040 will show if a real file does this.
  - **Nit:** the HEVY-IMPORT §2 validation bullet still describes the simpler kind rule ("`weight_kg` with `date` and no `exercise_title`"); D-055 refines it. That is fine as a decision record, and I left the doc unchanged.
  - **Commands:** `$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'`; `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` gave BUILD SUCCESSFUL (mostly up to date). Then `.\gradlew.bat :domain:test --rerun :data:testDebugUnitTest --rerun :app:testDebugUnitTest --rerun assembleDebug lintDebug` gave BUILD SUCCESSFUL: domain **205** tests (CsvReaderTest 9, HevyParserTest 14), data 62, app 108, all with 0 failures; lint 0 errors, 1 warning (the existing `junit-bom` NewerVersionAvailable).
- **2026-10-31 · BUILD.** Done, branch `t-035-hevy-csv-parser`.
  - **Code** (`:domain` `importer/hevy/`): `HevyExport.kt` holds the public types exactly as in this ticket. `CsvReader.kt` is the internal RFC 4180 `readCsv` and `CsvRecord(number, fields)`. `HevyLocalTime.kt` has `parseHevyLocalTime` (strict `d MMM yyyy, HH:mm`, calendar-checked, days-from-civil, no `java.time`) and `localToEpochMs` (HEVY-IMPORT §5 probe algorithm). `HevyParser.kt` has `parseHevyExport`: kind from header, columns by name, all-or-nothing errors, and the §2 mapping (grouping by first appearance, occurrence split, superset renumbering, km to m in `BigDecimal`, completed true, side BOTH, RPE on `HevySet`).
  - **Tests** (written first; all 23 seen failing on `TODO()` stubs, then passing): `CsvReaderTest` covers TC-HEVY-02 syntax. `HevyParserTest` covers TC-HEVY-01, file-level TC-HEVY-02, and TC-HEVY-03..10, with a `SwitchingClock` fake for the DST cases.
  - **Fixtures**: `domain/src/test/resources/hevy/{workout,measurement}_data.csv` were copied from §8 and compared programmatically with the doc blocks (identical after line-end normalisation). Nothing from `docs/fixtures/` is used or committed.
  - **Decisions:** D-055 covers parser details (kind detection so a missing `exercise_title` gives MISSING_COLUMN, required columns, number and date syntax, error granularity, lenient quoting). D-056 fixes the fixture set count: §8 said 9 / 14 sets, but its own breakdown and the verbatim fixture give **8 / 13**. The TC-HEVY-01 count assertion therefore expects 13; HEVY-IMPORT §8, TC-HEVY-01, TC-HEVY-18, and T-038's AC were corrected to match. Reviewer: please confirm this is the intended reading.
  - **Gotchas:** TC-HEVY-01's "`completedAtEpochMs` null" has no field at this level (`HevySet` wraps `SetData` only); it applies when T-036 builds `WorkoutSet`. Workout-level `createdAt`/`updatedAt` and ids are also T-036's job, so `parseHevyExport` uses `Clock` only for offsets.
  - **Commands:** `.\gradlew.bat :domain:test --tests "...importer.hevy.*"` failed first (23/23), then passed. `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` gave BUILD SUCCESSFUL: 205 domain tests, 0 failures; lint 0 errors, 1 warning (the existing JUnit BOM `NewerVersionAvailable`, not new). `rg "java\.time|android\." domain/.../importer` found no matches. No Gradle file changed.
  - **Left:** nothing for this ticket; matching and the plan are T-036.
