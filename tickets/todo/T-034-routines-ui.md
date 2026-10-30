# T-034 — Routines UI: list, editor, start

- **Role:** UI
- **Phase:** 2
- **Depends on:** T-033, T-011
- **Refs:** SPEC §6 (Routines) · docs/ARCHITECTURE.md §6
- **Size:** fits one session

## Goal
The user can see their routines, create and edit one (add exercises from the library, set planned sets and rest), delete one, start a workout from it, and save a finished workout as a routine.

## In scope
- Routines list reachable from Home, with Start on each routine and an empty state.
- Routine editor screen reusing the library picker; planned set rows with weight/reps.
- "Save as routine" from a finished workout's detail screen.
- Previews (light/dark), strings in `strings.xml`, 48dp targets, ViewModel tests.

## Out of scope (do NOT do)
- Folders, reordering by drag (a move up/down menu is fine), any change to the live-workout logging screen.

## Acceptance criteria
- [ ] Create a routine with two exercises, restart the app, start it: the live workout shows those exercises and sets.
- [ ] Edit and delete work; Home's Start workout (empty) still works.

## Verification
`testDebugUnitTest`, `assembleDebug`, manual check on the emulator.

## Notes / decisions needed
Created 2026-10-30 together with T-033.

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
