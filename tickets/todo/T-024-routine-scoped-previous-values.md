# T-024 — Routine-scoped previous values

- **Role:** BUILD
- **Phase:** 2
- **Depends on:** T-005 and the routines ticket (not written yet)
- **Refs:** TC-PREV-03 · SPEC F-04 AC2 · D-019
- **Size:** fits one session

## Goal
`previousFor(..., PreviousMode.ROUTINE_SCOPED, routineId)` returns the set from the last workout of the same routine instead of throwing.

## In scope
- Test first from TC-PREV-03, then replace the `TODO("T-024 ...")` in `:domain/calc/PreviousValues.kt`.
- `WorkoutExerciseRecord.routineId` already exists.

## Out of scope (do NOT do)
- Routines themselves, settings UI, persistence.

## Acceptance criteria
- [ ] TC-PREV-03 passes; the other TC-PREV tests are unchanged and still pass.

## Verification
`./gradlew :domain:test`

## Notes / decisions needed
What should routine mode return for a workout started without a routine (`routineId` null): anywhere, or null? Ask the owner.

## Handoff log
- 2026-10-29 BUILD (T-005): Created. The mode is typed and throws `NotImplementedError` until this ticket.
