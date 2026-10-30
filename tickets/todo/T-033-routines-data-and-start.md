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
- [ ] Starting from a routine creates a draft with its exercises and planned sets (uncompleted) and `routineId` set.
- [ ] Routines survive app restart and backup export/import.
- [ ] Migration test from the previous schema version passes.

## Verification
`./gradlew :domain:test testDebugUnitTest assembleDebug`

## Notes / decisions needed
Created 2026-10-30 by the owner's request so T-024 can be built (D-038).

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
