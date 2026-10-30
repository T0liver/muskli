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
- [ ] Applying the fixture to a database with an existing finished workout and a draft leaves both unchanged and adds 2 workouts and 3 custom exercises (TC-HEVY-20).
- [ ] A failure injected during `add` leaves the database unchanged (TC-HEVY-20).
- [ ] Applying the fixture a second time adds nothing and reports 2 duplicates (TC-HEVY-21).
- [ ] Body weight is written only when requested and after the Room commit (TC-HEVY-22).
- [ ] No schema version change; `MigrationTest` untouched.
- [ ] `./gradlew :domain:test testDebugUnitTest assembleDebug` pass.

## Verification
`.\gradlew.bat :domain:test testDebugUnitTest assembleDebug`

## Notes / decisions needed
None open: D-044, D-045. Re-planning in Apply avoids stale ids if the user takes long in the preview or logs a workout meanwhile.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
