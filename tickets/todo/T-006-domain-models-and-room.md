# T-006 — Domain models + Room schema

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-001, T-002
- **Refs:** docs/ARCHITECTURE.md §2–3 · research/04 §4 · TC-DATA-01, 02, 03, 04, 07 · D-002

## Goal
Persistent model for exercise templates, workouts (incl. drafts), workout exercises and sets, with DAOs, mappers, schema export and a first migration test setup.

## In scope
- Reuse the types from T-002; add the remaining `:domain` models and repository **interfaces**.
- Room entities/DAOs/mappers in `:data` for: exercise_template (+secondary muscles), workout, workout_exercise, workout_set (incl. `completed`, `completed_at`), app-level IDs via injected `IdGenerator`.
- Soft delete on workouts. Foreign keys with cascade.
- In-memory DAO tests.

## Out of scope (do NOT do)
- Routines, folders, measurements, photos, notes tables, `side`/`partial_reps`/`rpe_target` columns, seed data, UI.
- **Owner approval required for the final schema before merge** (post the entity list in the Handoff log).

## Acceptance criteria
- [ ] TC-DATA-01, 02, 03, 07 pass; migration test harness exists (TC-DATA-04 satisfied once a second schema version exists).
- [ ] Schema JSON exported and committed.
- [ ] Owner approved the schema (note in Handoff log).

## Verification
`./gradlew testDebugUnitTest` (or instrumented per setup chosen in T-001).

## Handoff log
