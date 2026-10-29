# T-006 — Domain models + Room schema

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-001, T-002
- **Refs:** docs/ARCHITECTURE.md §2–3 · research/04 §4 · TC-DATA-01, 02, 03, 04, 07 · D-002, D-014, D-022, D-024

## Goal
Persistent model for exercise templates, workouts (incl. drafts), workout exercises and sets, with DAOs, mappers, schema export and a first migration test setup.

## In scope
- Reuse the types from T-002; add the remaining `:domain` models and repository **interfaces**.
- Room entities/DAOs/mappers in `:data` for: exercise_template (+secondary muscles), workout, workout_exercise, workout_set (incl. `completed`, `completed_at`, and `side`), app-level IDs via injected `IdGenerator`.
- `workout_set.side`: non-null, default `both`, values `left` | `right` | `both` (D-022). No left/right UI.
- Soft delete on workouts. Foreign keys with cascade.
- Room database in the default database directory so Android Auto Backup includes it (D-024). No `noBackup` directory and no backup-exclusion rules.
- In-memory DAO tests.

## Out of scope (do NOT do)
- Routines, folders, measurements, photos, notes tables, `partial_reps`/`rpe_target` columns, seed data, UI.
- **Owner approval required for the final schema before merge** (post the entity list in the Handoff log).

## Acceptance criteria
- [x] TC-DATA-01, 02, 03, 07 pass; migration test harness exists (TC-DATA-04 satisfied once a second schema version exists).
- [x] Schema JSON exported and committed.
- [x] Owner approved the schema (note in Handoff log).

## Verification
`./gradlew testDebugUnitTest` (or instrumented per setup chosen in T-001).

## Handoff log
- 2026-10-28 DOCS: Owner pinned D-022. `side` is in schema v1 (default `both`). The rest of the entity list still needs owner approval before merge.
- 2026-10-29 DOCS: Owner pinned D-024. Keep the database in the default directory so Auto Backup includes workouts.
- 2026-10-29 BUILD: Started at the owner's request although T-001 is still in `verify/`, not `done/`.
- 2026-10-29 BUILD: **Owner approved schema v1 as posted**, with no optional columns (`default_rest_s`, `workout.routine_id`, `rpe` stay out), and approved the new dependencies: Room 2.8.5 (runtime, ktx, compiler, testing, Gradle plugin), kotlinx-coroutines 1.11.0 (core in `:domain`, test), Robolectric 4.17, androidx.test core 1.7.0 and ext-junit 1.3.0. DAO tests run on the JVM with Robolectric.
  - Schema v1 (`muskli.db`, default database dir): times are epoch ms UTC, enums are lowercase strings, ids are TEXT.
    - `exercise_template`: id PK, title, exercise_type, primary_muscle, equipment, is_custom (default 0), archived (default 0), created_at, updated_at.
    - `exercise_template_secondary_muscle`: PK (exercise_template_id, muscle); FK to template, cascade.
    - `workout`: id PK, title, notes?, start_time (indexed), utc_offset_s, end_time? (null = draft), created_at, updated_at, deleted_at? (soft delete).
    - `workout_exercise`: id PK, workout_id (FK, cascade, indexed), position, exercise_template_id (FK, RESTRICT, indexed), superset_group?, notes?, rest_seconds? (null = default, 0 = off).
    - `workout_set`: id PK, workout_exercise_id (FK, cascade, indexed), position, set_type (default 'normal'), weight_kg?, reps?, distance_m?, duration_s?, completed (default 0), completed_at?, side (NOT NULL, default 'both').
  - Done: `:domain` models (`MuscleGroup`, `Equipment`, `ExerciseTemplate`, `Workout`, `WorkoutExercise`, `WorkoutSet`) and interfaces (`IdGenerator`, `ExerciseTemplateRepository`, `WorkoutRepository`). `:data` has the entities, `Converters`, `ExerciseTemplateDao`, `WorkoutDao`, `MuskliDatabase` (with `MIGRATIONS` and `build(context)`), the mappers, `UuidIdGenerator`, and the schema at `data/schemas/.../1.json`.
  - Tests: `WorkoutDaoTest` (TC-DATA-01, 03, 07, save replaces children, draft/history queries, D-022 defaults), `ExerciseTemplateDaoTest`, `DraftPersistenceTest` (TC-DATA-02, file DB closed and reopened), `MigrationTest` (harness: create v1 from exported schema, validate, open with Room).
  - Left: repository implementations are T-010 / T-012. "At most one draft" is enforced by use cases in T-012 (Room has no partial unique index).
  - Gotchas: Robolectric's SDK 36 sandbox needs `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED` (set in `data/build.gradle.kts`). The name-based `MigrationTestHelper` constructor fails on Windows because Room's `SupportSQLiteDriver` compares names with `substringAfterLast('/')`; the harness uses the file + `AndroidSQLiteDriver` constructor instead.
  - Commands: `.\gradlew.bat :domain:test testDebugUnitTest assembleDebug lintDebug`, all green; `:data` 11 tests, 0 failures. Not run: `connectedDebugAndroidTest` (no instrumented tests added).
