# T-033 — Routines: model, storage, and starting a workout from one

- **Role:** BUILD
- **Phase:** 2
- **Depends on:** T-006, T-012
- **Refs:** SPEC §6 (Routines) · D-019 · D-038 · docs/research/ (routine shape only if needed)
- **Size:** fits one session

## Goal
A routine (name, ordered exercises, planned sets with weight/reps/type, per-exercise rest) can be saved, listed, edited, deleted, and a live workout can be started from it, with `routineId` set on the draft.

## In scope
- `:domain` model `Routine` and `RoutineRepository`; use cases: create/update/delete routine, list routines, start workout from routine (respects "at most one draft", D-029), save a finished workout as a routine.
- `:data` Room tables for routines (schema bump plus migration and migration test; approved by the owner on 2026-10-30), repository implementation, backup JSON includes routines (backward compatible with `schemaVersion` 1 files that have none).
- Workout draft records the `routineId` it was started from.
- Tests first for the `:domain` use cases.

## Out of scope (do NOT do)
- Any UI (T-034). Folders, routine sharing, rep ranges, progression.

## Acceptance criteria
- [x] Starting from a routine creates a draft with its exercises and planned sets (uncompleted) and `routineId` set.
- [x] Routines survive app restart and backup export/import.
- [x] Migration test from the previous schema version passes.

## Verification
`./gradlew :domain:test testDebugUnitTest assembleDebug`

## Notes / decisions needed
Created 2026-10-30 by the owner's request so T-024 can be built (D-038).

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
- 2026-10-31 REVIEW (branch `t-033-routines-data-and-start` at `c437092`, diff `main...`):
  - Checked against the ticket, D-052 (with D-029, D-034, D-038), SPEC §6, ARCHITECTURE §3/§8, WORKFLOW §5.
  - **Blockers:** none. **Should-fix:** none. No fixes made.
  - Confirmed: schema v5 adds only `5.json` (1–4.json untouched); `MIGRATION_4_5` SQL matches `5.json` and `runMigrationsAndValidate(5)` checks it, with a draft and its exercise kept and `routineId` null. Never a second draft: `StartWorkoutFromRoutine` goes through the transactional `startDraftUnlessActive` and returns `Resumed` (TC-ROUTINE-05). `routineId` is set on the draft (TC-ROUTINE-04) and survives reopening the DB. Backup `CURRENT_SCHEMA_VERSION` stays 1; `routines`/`routineId` are optional, absent from files without routines, and such a file imports with no routines; import deletes routines before custom templates (RESTRICT FK), and the only template hard delete in the code is that import path. Existing tests only gained new tests; no expected values changed. No UI (only Hilt providers in `:app`), no new dependencies (no `libs.versions.toml`/`*.kts` change), `:domain` imports only Kotlin/kotlinx. `@Upsert` on `routine` does not cascade-delete children the way REPLACE would.
  - **Nits** (left as they are; none worth a ticket):
    - `validate()` adds an `indicesInvalid` helper for routines, while the workout branch keeps its two inline copies of the same check.
    - `StartWorkoutFromRoutine` draws ids before it knows whether a draft exists; they are discarded on `Resumed`. Harmless with UUIDs.
    - `CreateRoutine`/`UpdateRoutine` do not check that template ids exist, so an unknown id fails at Room's FK on save. T-034 picks exercises from the library, so this cannot happen through the UI.
    - Imported routine titles are checked for blank but not trimmed, the same as workout titles.
  - Commands: `$env:JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStoreType=Windows-ROOT'; .\gradlew.bat --no-build-cache :domain:cleanTest :data:cleanTestDebugUnitTest :app:cleanTestDebugUnitTest :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL; domain 174, data 62, app 89 tests, 0 failures, 0 skipped; lint 0 errors, 1 warning (the existing `NewerVersionAvailable` for junit-bom in `libs.versions.toml`, not from this diff).
  - Result: moved to `verify/`.
- 2026-10-31 BUILD (branch `t-033-routines-data-and-start`, from `main` at `6b027a4`):
  - **Decision D-052** (auto-accepted recommendation, owner-delegated 2026-10-30): planned set = type + weight/reps/duration/distance; routine children keyed by position (no ids); list by title ignoring case; hard delete clears `workout.routine_id`; start copies notes/rest/superset, title = routine title, no last-time prefill; **an existing draft is returned as `Resumed` and nothing is created**; save-as-routine only for finished, non-deleted workouts.
  - `:domain`: `model/Routine.kt` (`Routine`, `RoutineExercise`, `PlannedSet`), `repo/RoutineRepository`, `Workout.routineId` (default null, so no call site changed), `usecase/routine/RoutineUseCases.kt`: `ObserveRoutines`, `CreateRoutine`, `UpdateRoutine`, `DeleteRoutine`, `StartWorkoutFromRoutine` (reuses `StartWorkoutResult` and `startDraftUnlessActive`), `SaveWorkoutAsRoutine`. Blank titles throw `IllegalArgumentException`, like `CreateCustomExerciseUseCase`. Tests written first (`RoutineUseCasesTest`, 12 tests, TC-ROUTINE-01..06 added to TEST-CASES); seen failing (unresolved use cases), then green.
  - `:data`: Room **v5** (`routine`, `routine_exercise`, `routine_set`, `workout.routine_id` + index, no FK, see D-052), `MIGRATION_4_5` with SQL copied from the exported `5.json`, `RoutineDao`, `RoomRoutineRepository`, mappers (`toExerciseRecords` now also fills `routineId` for T-024). Backup: optional `routines` and `workouts[].routineId`, validated all-or-nothing (unknown template, bad type, negative values, duplicate ids/indices, `routineId` not in file); export includes catalogue templates used by routines; import deletes routines before custom templates (RESTRICT FK). `schemaVersion` stays 1; files without routines export byte-identical as before.
  - `:app`: only DI (`DataModule.routineRepository`, routine use cases in `UseCaseModule`). No UI (T-034).
  - Tests added: `MigrationTest` 4→5, `RoomRoutineRepositoryTest` (read-back, replace, order, survives reopen with draft `routineId`, delete clears only its workouts), 3 in `JsonBackupRepositoryTest` (round-trip with routines and `routineId`, old file imports none and replaces device routines, bad routine records reject the file).
  - Docs: D-052, ARCHITECTURE §3/§8, TEST-CASES TC-ROUTINE, `BackupRepository` KDoc.
  - Commands: `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug` → BUILD SUCCESSFUL; domain 174, data 62, app 89 tests, 0 failures; lint 0 errors, 1 warning (pre-existing `NewerVersionAvailable` junit-bom, same as before the change).
  - Left / gotchas: routine-scoped previous values are still T-024 (`previousSets` still `TODO` for ROUTINE_SCOPED). `ImportResult.Imported` does not count routines (no domain API change). No emulator check: nothing visible changed. The draft from a routine has the routine's title, so Finish pre-fills it.
