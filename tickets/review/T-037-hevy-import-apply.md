# T-037 — Hevy import: repository and use cases (preview, apply)

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-036
- **Refs:** SPEC F-09 · `docs/HEVY-IMPORT.md` §4, §6 · D-034, D-044, D-045 · TC-HEVY-20..22 · TC-DATA-01
- **Size:** fits one session

## Goal
The app can preview a Hevy file (parse, match, plan summary) and apply it: new custom exercises and workouts are added to Room in one transaction, existing data is untouched, and an optional body weight is written to Settings afterwards.

## In scope
- `:domain` `repo/HevyImportRepository` (new interface):
  `suspend fun existingWorkoutStartsEpochMs(): List<Long>` (every workout row: finished, draft, soft-deleted), `suspend fun allTemplates(): List<ExerciseTemplate>` (archived included), `suspend fun add(templates: List<ExerciseTemplate>, workouts: List<Workout>)` (one transaction, insert only).
- `:domain` use cases in `usecase/importer/`:
  - `PreviewHevyImportUseCase(text: String): HevyPreview` → `Rejected(errors)` from the parser, `Measurements(latestBodyWeight)` for a measurement file, or `Ready(workouts, matches, defaultResolutions, plan)` where `plan` is built with `defaultResolutions`.
  - `ApplyHevyImportUseCase(workouts, resolutions, bodyWeightKg: Double?)`: re-reads existing starts and templates, **re-plans** (fresh ids and duplicate check), calls `add`, then `SettingsRepository.setBodyWeightKg` only when `bodyWeightKg` is non-null and the Room write succeeded. Returns the applied plan's counts.
  - Tests with fake repositories (JUnit 5).
- `:data` `RoomHevyImportRepository`: reuse `BackupDao` insert methods and the `Mappers.kt` functions (`toEntity`, `secondaryMuscleEntities`, `toRows`) inside `db.withTransaction`; add a DAO query for all workout start times. **No schema change**, no new migration.
- Hilt binding in `:app` `di/`.
- `:data` Robolectric tests for TC-HEVY-20 and TC-HEVY-21; `:domain` use-case test for TC-HEVY-22.

## Out of scope (do NOT do)
- Any screen, string, or navigation (T-038). The measurements file picker and checkbox (T-039).
- Replace mode; deleting or editing existing data (D-044).
- Changing `BackupRepository` or the backup JSON format.

## Acceptance criteria
- [x] Applying the fixture to a database with an existing finished workout and a draft leaves both unchanged and adds 2 workouts and 3 custom exercises (TC-HEVY-20).
- [x] A failure injected during `add` leaves the database unchanged (TC-HEVY-20).
- [x] Applying the fixture a second time adds nothing and reports 2 duplicates (TC-HEVY-21).
- [x] Body weight is written only when requested and after the Room commit (TC-HEVY-22).
- [x] No schema version change; `MigrationTest` untouched.
- [x] `./gradlew :domain:test testDebugUnitTest assembleDebug` pass.

## Verification
`.\gradlew.bat :domain:test testDebugUnitTest assembleDebug`

## Notes / decisions needed
None open: D-044, D-045. Re-planning in Apply avoids stale ids if the user takes long in the preview or logs a workout meanwhile.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->

### 2026-10-31 · BUILD
**Done.**
- `:domain` `repo/HevyImportRepository` (the three methods as specified) and `usecase/importer/HevyImportUseCases.kt`: `HevyPreview` (`Rejected`, `Measurements`, `Ready`), `PreviewHevyImportUseCase`, `ApplyHevyImportUseCase`. Tests were written first and seen failing (13/13 on `TODO()` stubs): `HevyImportUseCasesTest` with `FakeHevyImportRepository`. They cover preview, fresh ids on apply, a workout logged after the preview, untrusted template ids, an archived mapping, TC-HEVY-21, TC-HEVY-22 (checked, unchecked, failed add), a non-positive body weight, and a measurements-only apply.
- Review nit fixed: Apply **builds resolutions only from template ids it read**. An unknown `UseTemplate` id or a missing title falls back to the fresh default from re-matching. Recorded as **D-058**, with the body-weight validation, the return value (the applied `HevyImportPlan`), and insert-only behaviour.
- `:data` `repo/RoomHevyImportRepository`: `BackupDao` inserts plus `Mappers.kt` (`toEntity`, `secondaryMuscleEntities`, `toRows`) inside `db.withTransaction`. If an `IGNORE`d template row comes back as -1 it throws, so the transaction rolls back and nothing is replaced. New query `BackupDao.allWorkoutStartTimes()` (a plain `SELECT start_time FROM workout`, so drafts and soft-deleted rows are included). **No schema change** (no entity or version edits, `schemas/` untouched, `MigrationTest` untouched).
- `RoomHevyImportRepositoryTest` (Robolectric, in-memory Room, like `JsonBackupRepositoryTest`) covers:
  - TC-HEVY-20: existing finished workout and draft unchanged, 2 workouts and 3 customs read back equal to the plan.
  - TC-HEVY-20 failure: a foreign-key failure on the second workout's exercises, after the template and workout inserts, leaves the DB unchanged.
  - An id clash never replaces a template.
  - TC-HEVY-21: the second preview and apply give 0 new, 2 duplicates, 0 customs, and Lateral Raise now matches its custom exactly.
  - Drafts and soft-deleted workouts count as duplicates.
- Mutation check: replacing `withTransaction` with `run` makes the TC-HEVY-20 failure test fail.
- Hilt: `DataModule.hevyImportRepository`, and `UseCaseModule.previewHevyImport` / `applyHevyImport`.

**Gotchas.**
- `data/src/test/resources/hevy/workout_data.csv` is a byte copy of the `:domain` fixture, because `:data` tests cannot see `:domain` test resources. Keep the two copies identical.
- The data test seeds only the 5 catalogue exercises the fixture matches, because `CatalogueV1.kt` is `:domain` test code.
- Apply reads starts and templates outside the write transaction (the interface the ticket specifies). A workout started in the same minute between that read and `add` is not detected. This is negligible for one user.
- Preview uses ids from the injected generator for its throwaway plan. Apply always issues new ones.

**Left.** UI (T-038), measurements picker and checkbox (T-039).

**Commands run.**
- `.\gradlew.bat :domain:test --tests "*HevyImportUseCasesTest*"` on stubs: 13 failed (expected). After implementing: pass.
- `.\gradlew.bat :data:testDebugUnitTest --tests "*RoomHevyImportRepositoryTest*"`: 5/5 pass.
- `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`: BUILD SUCCESSFUL, 412 tests, 0 failures. Lint has 1 issue, the pre-existing `NewerVersionAvailable` (junit-bom), same as main. No emulator run (none needed for this ticket).
