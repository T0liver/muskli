# T-041 — Enable the "Last time in this routine" setting

- **Role:** UI
- **Phase:** 2
- **Depends on:** T-024, T-034
- **Refs:** SPEC F-04 AC2 · TC-PREV-03 · D-038 · D-053
- **Size:** fits one session

## Goal
The Settings radio "Last time in this routine" can be selected, now that workouts can be started from a routine (T-034) and the live workout follows the stored mode (T-024).

## In scope
- `SettingsScreen`: make the routine-scoped radio selectable (sends `SettingsEvent.PreviousModeChanged(ROUTINE_SCOPED)`), drop the "Available once routines exist" supporting text and its string.
- Previews and the Settings ViewModel/UI test for the new selection.

## Out of scope (do NOT do)
- Any change to the lookup rules (T-024, D-053) or to the live-workout screen.

## Acceptance criteria
- [ ] Selecting "Last time in this routine" persists and survives an app restart.
- [ ] On the emulator: finish a workout from Routine A (80x8) and one from Routine B (85x6) for the same exercise; starting Routine A shows 80x8 in PREVIOUS, Routine B shows 85x6, an empty workout shows 85x6 (TC-PREV-03, D-038).

## Verification
`testDebugUnitTest`, `assembleDebug`, manual check on the emulator.

## Notes / decisions needed
Created 2026-10-31 by T-024: the radio stays disabled there because the ticket excludes settings UI and routines cannot be started from the UI yet (D-053).

## Handoff log
<!-- Newest first. Each entry: date, role, what was done, what's left, gotchas, commands run + results. -->
