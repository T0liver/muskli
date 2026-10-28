# T-012 — Live workout: draft persistence + use cases

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-006, T-010
- **Refs:** SPEC F-02 AC2–3, F-03 AC1,3,4 · TC-SET-* · TC-DATA-02 · AC-03 · docs/ARCHITECTURE.md §4

## Goal
All live-workout operations as use cases over a persisted draft: start, add/replace/remove/reorder exercise, add/delete set, change set type, edit values, complete/uncomplete set.

## In scope
- Use cases in `:domain`, repository support in `:data`, tests first for TC-SET-01..04 and TC-DATA-02.
- `CompleteSet` calls `restTimerEffect` (T-004) and returns the effect; no timer implementation here.
- Exactly one active draft; starting while a draft exists returns a clear result (resume).

## Out of scope (do NOT do)
- UI, previous values, rest timer service, finishing (T-016).

## Acceptance criteria
- [ ] TC-SET-01..04, TC-DATA-02 pass.
- [ ] Every mutation is persisted immediately (verified by recreating the DB connection in a test).

## Handoff log
