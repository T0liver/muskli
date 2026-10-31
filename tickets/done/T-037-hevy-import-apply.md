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

### 2026-10-31 · VERIFY
**Pass. Move to `done/`.** This ticket has no UI and no `AC-*` device steps (the UI is T-038), so verification is the test suites. I did not install on a device; there is nothing to see there.
- Build: commit `f88fcbc` on `t-037-hevy-import-apply`, Windows, JDK from `gradlew`, Robolectric for `:data`.
- `.\gradlew.bat :domain:cleanTest :domain:test :data:cleanTestDebugUnitTest :app:cleanTestDebugUnitTest testDebugUnitTest assembleDebug lintDebug --no-build-cache` gave BUILD SUCCESSFUL, 412 tests, 0 failures, 0 errors. Lint: 0 errors, 1 warning (pre-existing `NewerVersionAvailable`).
- `RoomHevyImportRepositoryTest` 5/5 pass:
  - TC-HEVY-20 adds 2 workouts and 3 customs, existing rows unchanged.
  - TC-HEVY-20 rollback (a foreign-key failure leaves the DB unchanged; it was shown to fail without the transaction in REVIEW).
  - No template replace.
  - TC-HEVY-21 second import: 0 new, 2 duplicates, 0 customs, DB unchanged.
  - Drafts and soft-deleted workouts count as duplicates.
- `HevyImportUseCasesTest` 13/13 pass, including TC-HEVY-21 and TC-HEVY-22 (written after `add`, unchanged when not requested, not written on failure).
- Every acceptance criterion holds. No new tickets.

### 2026-10-31 · REVIEW
**Verdict: no Blockers, no Should-fix. Move to `verify/`.** Reviewed `git diff main...t-037-hevy-import-apply` (commit `e9338fc`) against the ticket, D-044, D-058 and WORKFLOW §5.
- **Scope:** only the interface, the two use cases, `RoomHevyImportRepository`, one read query in `BackupDao`, Hilt bindings, tests, the fixture copy, and one D-058 line. No UI, no strings, no navigation. `BackupRepository` and the backup format are untouched.
- **Add only (D-044):** `add` uses `insertTemplatesIfMissing` (IGNORE, with `-1` row ids checked so a clash fails instead of being skipped silently) and plain `@Insert` (ABORT) for workouts, exercises and sets. There is no update, replace or delete path. The test `add never replaces an existing template` covers it.
- **Duplicates:** `SELECT start_time FROM workout` has no filter, so drafts and soft-deleted rows count. The same-minute rule comes from the `planHevyImport` code that T-036 verified. Both the Room test (drafts and soft-deleted) and the domain test (`+59 s`) cover it.
- **One transaction:** everything runs inside `db.withTransaction`, and `check` throws inside it. The TC-HEVY-20 failure test is a real one: I replaced `withTransaction` with `run` and it fails (`5 tests completed, 1 failed`), then I reverted.
- **Untrusted template ids (D-058):** `UseTemplate` is kept only when its id is in `allTemplates()` from the same call. Otherwise, or when no resolution is given, it uses the fresh default. `CreateCustom` is kept. Tests cover unknown ids, a missing title, and archived ids that were read.
- **Body weight:** it is validated before any write and written only after `add` returns. The tests check the order (`add` then `setBodyWeightKg`) and that nothing is written when `add` throws.
- **No schema change, no new dependencies:** no edits to entities, `MuskliDatabase`, `schemas/`, `MigrationTest`, `libs.versions.toml` or `build.gradle.kts`. No existing test file is modified (`--diff-filter=M` on `*Test*.kt` is empty).
- **Layers:** the logic is in `:domain` with fakes. `:data` is a thin mapper. Everything is suspend Room, so there is no main-thread I/O. Time and ids are injected.

**Nits (not fixed, no ticket needed):**
- Apply reads starts and templates outside the write transaction (this is the ticket's interface, and the BUILD log already notes it). A same-minute workout logged in the few milliseconds between them would not be detected. This is acceptable for one user.
- `data/src/test/resources/hevy/workout_data.csv` must stay byte-identical to the `:domain` fixture. I checked: the SHA-256 hashes match. Nothing enforces it.
- The Room rollback test calls `repository.add` with a corrupted plan rather than going through Apply. That is fine, because the domain test covers Apply's failure path.

**Commands run.**
- `.\gradlew.bat :domain:cleanTest :domain:test :data:cleanTestDebugUnitTest :app:cleanTestDebugUnitTest testDebugUnitTest assembleDebug lintDebug --no-build-cache` gave BUILD SUCCESSFUL, 412 tests, 0 failures, 0 errors, 0 skipped. Lint: 0 errors, 1 warning, the pre-existing `NewerVersionAvailable` (junit-bom).
- Mutation run (`withTransaction` changed to `run`) of `:data:testDebugUnitTest --tests "*RoomHevyImportRepositoryTest*"`: the TC-HEVY-20 failure test FAILED as expected. Reverted, then `:data:testDebugUnitTest assembleDebug` gave BUILD SUCCESSFUL.

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
