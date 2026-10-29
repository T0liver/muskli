# T-005 — Domain: previous-values lookup

- **Role:** BUILD
- **Phase:** 0
- **Depends on:** T-002
- **Refs:** TC-PREV-01, 02, 04, 05, 06, 07 · SPEC F-04 · D-019

## Goal
Pure function that returns the previous set for (exercise, set position) from finished-workout history, mode "anywhere".

## In scope
- Tests first. Align by raw set index, warm-ups included (D-019, TC-PREV-05).
- Ignore drafts and soft-deleted workouts; use the most recent finished workout containing the exercise; first occurrence if repeated (TC-PREV-07, D-019).
- Leave a `PreviousMode.RoutineScoped` variant **unimplemented but typed** (TC-PREV-03 arrives with routines).

## Out of scope (do NOT do)
- Routines, persistence, UI, prefill-on-add (that's T-014).

## Acceptance criteria
- [ ] Listed TC tests pass, including TC-PREV-05 (raw index). TC-PREV-03 stays unimplemented (Phase 2) and is noted in the Handoff log.

## Verification
`./gradlew :domain:test`

## Handoff log
- 2026-10-28 DOCS: Owner pinned D-019. Raw index is decided; only TC-PREV-03 stays pending.
