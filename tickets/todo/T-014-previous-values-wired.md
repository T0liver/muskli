# T-014 — Previous values wired into the live workout

- **Role:** BUILD
- **Phase:** 1
- **Depends on:** T-005, T-013
- **Refs:** SPEC F-04 AC1,3 · TC-PREV-*

## Goal
The PREVIOUS column shows real data, and adding an exercise done before pre-fills sets (count/weight/reps) from last time.

## In scope
- Query last finished workout per exercise; map into UI state; prefill on add (editable, not completed).
- Tests: use case with fake history; one UI test showing previous values.

## Out of scope (do NOT do)
- Routine-scoped mode, settings UI for the mode.

## Acceptance criteria
- [ ] Previous column matches TC-PREV-01/02/04.
- [ ] Prefilled sets start uncompleted.

## Handoff log
