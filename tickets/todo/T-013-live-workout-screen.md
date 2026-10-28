# T-013 — Live workout screen: sets, set types, completion

- **Role:** UI+BUILD
- **Phase:** 1
- **Depends on:** T-002, T-008, T-012
- **Refs:** SPEC F-02, F-03 · AC-01, AC-03 · docs/ARCHITECTURE.md §4, §6

## Goal
The real live-workout screen wired to the draft: stopwatch, add exercises via the picker, set rows with inputs by exercise type, set-type menu, swipe-delete, check to complete, header counters, keep-awake setting hook.

## In scope
- `WorkoutSessionViewModel` + `LiveWorkoutUiState`; reuse T-008 components; ViewModel tests with fakes; a Compose UI test for the happy path (add exercise, enter values, complete set).
- Resume banner/behavior when a draft exists at app start.
- Unit display kg/lb via T-002 conversions (global setting only; per-exercise toggle later).

## Out of scope (do NOT do)
- Previous values (T-014), rest timer (T-015), finish flow (T-016), supersets, notes, RPE.

## Acceptance criteria
- [ ] Happy path UI test passes; kill/restart restores the draft (AC-03 steps).
- [ ] Only completed sets count in the header volume/sets.
- [ ] No extra taps beyond the design approved in T-008.

## Handoff log
