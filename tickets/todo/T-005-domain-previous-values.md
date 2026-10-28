# T-005 — Domain: previous-values lookup

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-002
- **Refs:** TC-PREV-01, 02, 04, 06, 07 · SPEC F-04

## Goal
Pure function that returns the previous set for (exercise, set position) from finished-workout history, mode "anywhere".

## In scope
- Tests first. Align by raw set index (proposed for TC-PREV-05; flag for confirmation).
- Ignore drafts and soft-deleted workouts; use the most recent finished workout containing the exercise; first occurrence if repeated (TC-PREV-07).
- Leave a `PreviousMode.RoutineScoped` variant **unimplemented but typed** (TC-PREV-03 arrives with routines).

## Out of scope (do NOT do)
- Routines, persistence, UI, prefill-on-add (that's T-014).

## Acceptance criteria
- [ ] Listed TC tests pass; TC-PREV-03 and -05 documented as pending/proposed in the Handoff log.

## Verification
`./gradlew :domain:test`

## Handoff log
